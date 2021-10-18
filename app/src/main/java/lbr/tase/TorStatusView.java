/*
 * TASE
 * Copyright (C) 2017
 *
 * TASE is free software, licensed under version 3 of the GNU Affero General Public License.
 *
 */

package lbr.tase;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TorStatusView extends LinearLayout {

    public TorStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void update() {
        Tor tor = Tor.getInstance(getContext());
        Server server = Server.getInstance(getContext());
        boolean torReady = false;

        //setVisibility(!tor.isReady() ? View.VISIBLE : View.GONE);

        TextView view = (TextView) findViewById(R.id.status);

        if (tor.isReady()) { torReady = true; }

        if (!torReady) {

            String status = tor.getStatus();
            int i = status.indexOf(']');
            if (i >= 0) status = status.substring(i + 1);
            status = status.trim();


            String prefix = "Bootstrapped";
            if (status.contains("%") && status.length() > prefix.length() && status.startsWith(prefix)) {
                status = status.substring(prefix.length());
                status = status.trim();
                view.setText(status);
            } else if (view.length() == 0) {
                view.setText("Starting...");
            }
        } else {
            String status = server.getStatus();
            view.setText(status);
            if (status.contains("registered")) { new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    setVisibility(View.GONE);
                }
            }, 1000 * 3);
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {

        super.onAttachedToWindow();

        if (!isInEditMode()) {
            Tor tor = Tor.getInstance(getContext());
            tor.setLogListener(new Tor.LogListener() {
                @Override
                public void onLog() {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            update();
                        }
                    });
                }
            });
            Server server = Server.getInstance(getContext());
            server.setLogListener(new Server.LogListener() {
                @Override
                public void onLog() {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            update();
                        }
                    });
                }
            });
            update();
        }

    }

    @Override
    protected void onDetachedFromWindow() {

        Tor tor = Tor.getInstance(getContext());
        tor.setLogListener(null);
        Server server = Server.getInstance(getContext());
        server.setLogListener(null);

        if (!isInEditMode()) {
            super.onDetachedFromWindow();
        }

    }
}
