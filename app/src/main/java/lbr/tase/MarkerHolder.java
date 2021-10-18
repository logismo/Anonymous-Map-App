/*
 * TASE
 * Copyright (C) 2017
 *
 * TASE is free software, licensed under version 3 of the GNU Affero General Public License.
 *
 */

package lbr.tase;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class MarkerHolder {

    MarkerType markerType;
    Marker marker;
    LatLng latLng;
    String author;
    long time;
    String description;
    private String hash;
    private Context context;
    String address = "";

    public MarkerHolder(Context c, LatLng point, String author, long time) {
        this.context = c;
        this.latLng = point;
        this.author = author;
        this.time = time;
        this.hash = createHash();
    }

    public MarkerHolder(Context c, LatLng point, String author, long time, String hash) {
        this.context = c;
        this.latLng = point;
        this.author = author;
        this.time = time;
        this.hash = hash;

    }

    public String createHash() {
        Tor tor = Tor.getInstance(context);
        String cmd = latLng + author + time;
        return Utils.base64encode(tor.sign(cmd.getBytes())).substring(0,10);
    }

    public void onDestroy() {
        if (marker != null) { marker.remove(); }
        Database db = Database.getInstance(context);
        db.deleteLocation(hash);
        this.latLng = null;
        this.author = null;
        this.description = null;
        this.hash = null;
        this.context = null;

    }

    public String getHash() { return hash; }
}