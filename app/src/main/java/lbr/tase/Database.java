/*
 * TASE
 * Copyright (C) 2017
 *
 * TASE is free software, licensed under version 3 of the GNU Affero General Public License.
 *
 */

package lbr.tase;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Database extends SQLiteOpenHelper {

    private static Database instance;
    private Context context;

    public Database(Context context) {
        super(context, "cdb", null, 1);
        this.context = context;
    }

    public static Database getInstance(Context context) {
        if (instance == null)
            instance = new Database(context.getApplicationContext());
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //db.execSQL("CREATE TABLE peers ( _id INTEGER PRIMARY KEY, address TEXT UNIQUE)");
        db.execSQL("CREATE TABLE locations ( _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, latitude DECIMAL(9,6), longitude DECIMAL(9,6), type INTEGER, author TEXT, time INTEGER, text TEXT, hash TEXT UNIQUE, deleted INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }


    // messages
    // TODO: add time
    public synchronized long addLocation(double latitude, double longitude, int type, String text, long time, String hash, String author) {
        ContentValues v = new ContentValues();
        v.put("latitude", latitude);
        v.put("longitude", longitude);
        v.put("text", text);
        v.put("time", time);
        v.put("type", type);
        v.put("author", author);
        v.put("hash", hash);
        return getReadableDatabase().insert("locations", null, v);

    }

    /*
    public synchronized long clearLocations() {
        return getReadableDatabase().delete("locations", "_id > 0", null);
    }

    */

    public synchronized void deleteLocation(String hash) {
        ContentValues v = new ContentValues();
        v.put("deleted", true);
        getReadableDatabase().update("locations", v, "hash=?", new String[]{hash});
    }

    public synchronized void removeLocation(String hash) {
        getReadableDatabase().delete("locations", "hash=?", new String[]{hash});
    }
}
