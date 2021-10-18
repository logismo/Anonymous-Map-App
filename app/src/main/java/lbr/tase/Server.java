/*
 * TASE
 * Copyright (C) 2017
 *
 * TASE is free software, licensed under version 3 of the GNU Affero General Public License.
 *
 */

package lbr.tase;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

public class Server {

    private static Server instance;
    String socketName;
    private Context context;
    private String TAG = "Server";
    private Listener listener = null;
    private TorListener torListener = null;
    private LocalServerSocket serverSocket;
    private LocalSocket ls;
    private String status = "";
    private LogListener logListener;


    public Server(Context c) {
        context = c;

        log("start listening");
        try {
            socketName = new File(context.getFilesDir(), "socket").getAbsolutePath();
            ls = new LocalSocket();
            ls.bind(new LocalSocketAddress(socketName, LocalSocketAddress.Namespace.FILESYSTEM));
            serverSocket = new LocalServerSocket(ls.getFileDescriptor());
            socketName = "unix:" + socketName;
            log(socketName);
        } catch (Exception ex) {
            throw new Error(ex);
        }
        log("started listening");
        status = context.getString(R.string.registering_tor);
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    LocalServerSocket ss = serverSocket;
                    if (ss == null) break;
                    log("waiting for connection");
                    final LocalSocket ls;
                    try {
                        ls = ss.accept();
                        if (BuildConfig.DEBUG) log("accept");
                    } catch (IOException ex) {
                        throw new Error(ex);
                    }
                    if (ls == null) {
                        log("no socket");
                        continue;
                    }
                    log("new connection");
                    new Thread() {
                        @Override
                        public void run() {
                            handle(ls);
                            try {
                                ls.close();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }.start();
                }
            }
        }.start();
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    return;
                }
                Tor tor = Tor.getInstance(context);
                for(int i = 0; i < 20 && !tor.isReady(); i++) {
                    log("Tor not ready");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }
                log("Tor ready");
                final Client client = Client.getInstance(context);
                //client.startSendPendingFriends();
                for(int i = 0; i < 20 && !client.testIfServerIsUp(); i++) {
                    log("Hidden server descriptors not yet propagated");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }
                log("Hidden service registered");
                status = "Tor Hidden Service registered!";
                if (torListener != null) torListener.onChange();
                LogListener l = logListener;
                if (l != null) {
                    l.onLog();
                }
                //client.askForPeers();
                client.askForLocations();
                //client.askForNewMessages();
            }
        }.start();
    }

    public static Server getInstance(Context context) {
        if (instance == null) {
            instance = new Server(context.getApplicationContext());
        }
        return instance;
    }

    private void log(String s) {
        if (!BuildConfig.DEBUG) return;
        Log.i(TAG, s);
    }

    public void setListener(Listener l) {
        listener = l;
        if (listener != null)
            listener.onChange();
    }

    public void setTorListener(TorListener l) {
        torListener = l;
        if (torListener != null)
            torListener.onChange();
    }

    String handle(String request) throws Exception {

        if (BuildConfig.DEBUG) log("accept");

        Database db = Database.getInstance(context);
        Tor tor = Tor.getInstance(context);
        //Notifier notifier = Notifier.getInstance(context);

        log("request " + request);

        String[] tokens = request.split(" ");
        if (tokens.length == 0)
            return "";

        log("toks " + tokens.length);

        if ("askloc".equals(tokens[0]) && tokens.length == 5) {

            String op = tokens[0];
            String receiver = tokens[1];
            String sender = tokens[2];
            String pubkey = tokens[3];
            String signature = tokens[4];

            if (!receiver.equals(tor.getID())) {
                log("add wrong target");
                return "";
            }
            log("askloc target ok");

            if (!tor.checksig(
                    sender,
                    Utils.base64decode(pubkey),
                    Utils.base64decode(signature),
                    (op + " " + receiver + " " + sender).getBytes())) {
                log("aksloc invalid signature");
                return "";
            }
            log("askloc signature ok");

            log("askloc ok");

            Client.getInstance(context).startSendLocations(sender, "");

            return "1";

        }

        if ("loc".equals(tokens[0]) && tokens.length == 6) {

            String op = tokens[0];
            String receiver = tokens[1];
            String sender = tokens[2];
            String locations = tokens[3];
            String pubkey = tokens[4];
            String signature = tokens[5];

            if (!receiver.equals(tor.getID()) || sender.equals(tor.getID())) {
                log("add wrong target");
                return "";
            }
            log("add target ok");

            if (!tor.checksig(
                    sender,
                    Utils.base64decode(pubkey),
                    Utils.base64decode(signature),
                    (op + " " + receiver + " " + sender + " " + locations).getBytes())) {
                log("add invalid signature");
                return "";
            }
            log("add signature ok");

            String[] locationSplit = locations.split("~");
            for (String s: locationSplit) {
                log(s);
                String[] loc = s.split(";");
                log(""+ (System.currentTimeMillis()- Long.parseLong(loc[4])));
                if (Long.parseLong(loc[4]) > System.currentTimeMillis() ||
                        System.currentTimeMillis() - Long.parseLong(loc[4]) > 1000*60*60) { return ""; }
                String description = new String(Utils.base64decode(loc[3]), Charset.forName("UTF-8"));
                db.addLocation(Double.parseDouble(loc[0]), Double.parseDouble(loc[1]),
                        Integer.parseInt(loc[2]), description, Long.parseLong(loc[4]), loc[5], tokens[2]);
            }

            //db.addLocation(Long.parseLong(latitude), Long.parseLong(longitude));
            //db.addContact(sender, false, true, new String(Utils.base64decode(sendername), Charset.forName("UTF-8")));
            if (listener != null) listener.onChange();

            log("add ok");

            return "1";

        }

        return "";
    }

    private void handle(InputStream is, OutputStream os) throws Exception {
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os));
        while (true) {
            String request = r.readLine();
            if (request == null) break;
            request = request.trim();
            if (request.equals("")) break;
            String response = "";
            try {
                response = handle(request);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            w.write(response + "\r\n");
            w.flush();
        }
        r.close();
        w.close();
    }

    private void handle(LocalSocket s) {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = s.getInputStream();
        } catch (IOException ex) {
        }
        try {
            os = s.getOutputStream();
        } catch (IOException ex) {
        }
        if (is != null && os != null) {
            try {
                handle(is, os);
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
        if (is != null) {
            try {
                is.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if (os != null) {
            try {
                os.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public String getSocketName() {
        return socketName;
    }

    public interface Listener {
        void onChange();
    }

    public interface TorListener {
        void onChange();
    }

    public String getStatus() {
        return status;
    }

    public interface LogListener {
        void onLog();
    }

    public void setLogListener(LogListener l) {
        logListener = l;
    }
}
