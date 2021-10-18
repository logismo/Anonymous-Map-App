/*
 * TASE
 * Copyright (C) 2017
 *
 * TASE is free software, licensed under version 3 of the GNU Affero General Public License.
 *
 */

package lbr.tase;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

public class Client {

    private static Client instance;
    Tor tor;
    Database db;

    Context context;
    AtomicInteger counter = new AtomicInteger();
    StatusListener statusListener;

    public Client(Context c) {
        context = c;
        tor = Tor.getInstance(context);
        db = Database.getInstance(context);
    }

    public static Client getInstance(Context context) {
        if (instance == null)
            instance = new Client(context.getApplicationContext());
        return instance;
    }

    private void log(String s) {
        if (!BuildConfig.DEBUG) return;
        Log.i("Client", s);
    }

    private Sock connect(String address) {
        log("connect to " + address);
        Sock sock = new Sock(context, address + ".onion", Tor.getHiddenServicePort());
        return sock;
    }

    public void askForLocations() {
        final String receiver = context.getString(R.string.seed_node);
        start(new Runnable() {
            @Override
            public void run() {
                doAskForLocations(receiver);
            }
        });
    }

    public void doAskForLocations(String receiver) {
        String sender  = tor.getID();
        log("ask for locations");
        String cmd = "askloc" + " " + receiver + " " + sender;
        connect(receiver).queryAndClose(
                cmd,
                Utils.base64encode(tor.pubkey()),
                Utils.base64encode(tor.sign(cmd.getBytes()))
        );
    }

    public void startSendLocations(final String receiver, final String hash) {
        log("start send locations");
        if (receiver.equals(tor.getID())) { return; }
        start(new Runnable() {
            @Override
            public void run() {
                doSendLocations(receiver, hash);
            }
        });
    }

    private void doSendLocations(String receiver, String hash) {
        log("do send locations");
        Cursor cur;
        if (hash == "") { cur = db.getReadableDatabase().query("locations", null, "time>? AND author!=? AND deleted=0",
                new String[]{""+(System.currentTimeMillis()-1000*60*60), receiver}, null, null, null); }
        else { cur = db.getReadableDatabase().query("locations", null, "time>? AND author!=? AND hash=? AND deleted=0",
                new String[]{""+(System.currentTimeMillis()-1000*60*60), receiver, hash}, null, null, null); }

        log(""+cur.getCount());
        if (cur.getCount() > 0) {
            String sender = tor.getID();
            String locations = "";

            while (cur.moveToNext()) {
                locations +=
                        cur.getDouble(cur.getColumnIndex("latitude")) + ";" +
                        cur.getDouble(cur.getColumnIndex("longitude")) + ";" +
                        cur.getString(cur.getColumnIndex("type")) + ";" +
                                Utils.base64encode(cur.getString(cur.getColumnIndex("text")).getBytes(Charset.forName("UTF-8"))) + ";" +
                        cur.getString(cur.getColumnIndex("time")) + ";" +
                        cur.getString(cur.getColumnIndex("hash")) + "~";
            }

            if (locations.length()>0) locations.substring(0, locations.length()-1);

            String cmd = "loc " + receiver + " " + sender + " " + locations;

            connect(receiver).queryAndClose(
                    cmd,
                    Utils.base64encode(tor.pubkey()),
                    Utils.base64encode(tor.sign(cmd.getBytes()))
            );
        }
        cur.close();
    }

    private void start(final Runnable runnable) {
        new Thread() {
            @Override
            public void run() {
                {
                    int n = counter.incrementAndGet();
                    StatusListener l = statusListener;
                    if (l != null) l.onStatusChange(n > 0);
                }
                try {
                    runnable.run();
                } finally {
                    int n = counter.decrementAndGet();
                    StatusListener l = statusListener;
                    if (l != null) l.onStatusChange(n > 0);
                }
            }
        }.start();
    }

    public interface StatusListener {
        void onStatusChange(boolean loading);
    }


    public boolean testIfServerIsUp() {
        Sock sock = connect(tor.getID());
        boolean ret = sock.isClosed() == false;
        sock.close();
        return ret;
    }
}
