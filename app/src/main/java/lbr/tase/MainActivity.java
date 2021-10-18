/*
 * TASE
 * Copyright (C) 2017
 *
 * TASE is free software, licensed under version 3 of the GNU Affero General Public License.
 *
 */

package lbr.tase;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import rx.functions.Action1;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener,
GoogleMap.OnMarkerClickListener {

    Tor tor;
    Database db;
    Client client;
    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Cursor locationCursor;
    int marker_limit;
    Timer timer;
    String seed_node;
    float factor;
    int icon_size = 100;
    float zIndex = 0;
    int tor_listener = 0;
    boolean tor_ready = false;
    boolean ask_location_timeout = false;

    //Subscription subscription;
    //private ReactiveLocationProvider reactiveLocationProvider;

    //boolean followUser = false;
    boolean followUser = true;

    HashMap<String, MarkerHolder> markerMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Server.getInstance(this).setTorListener((new Server.TorListener() {
            @Override
            public void onChange() {
                tor_listener++;
                if (tor_listener > 1) {
                    torReady();
                }
            }
        }));

        seed_node = getString(R.string.seed_node);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        factor = (float) metrics.widthPixels / 1080;

        //Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        //setSupportActionBar(myToolbar);


        db = Database.getInstance(this);
        tor = Tor.getInstance(this);
        client = client.getInstance(this);

        /*
        LocationRequest request = LocationRequest.create() //standard GMS LocationRequest
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(5)
                .setInterval(100);

        reactiveLocationProvider = new ReactiveLocationProvider(this);
        subscription = reactiveLocationProvider.getUpdatedLocation(request)

        .subscribe(new Subscriber<Location>() {
            @Override
            public void onNext(Location location) { animateMapToLocation(location); }

            @Override
            public void onCompleted() { }

            @Override
            public void onError(Throwable e) { }
        });


        //clearGeofence();
        */
    }

    /*
    private View.OnTouchListener handleTouch = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            int x = (int) event.getX();
            int y = (int) event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.i("TAG", "touched down");
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.i("TAG", "moving: (" + x + ", " + y + ")");
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i("TAG", "touched up");
                    break;
            }

            return true;
        }
    };
    */

    /*
    public boolean onMyLocationButtonClick() {
        followUser = true;
        return true;
    }
    */

    /*
    public void animateMapToLocation(Location location) {
        subscription.unsubscribe();
        if (mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13),
                    new GoogleMap.CancelableCallback() {
                        @Override
                        public void onFinish() {
                            // do something
                            //dlg.show();
                        }

                        @Override
                        public void onCancel() {
                            // do something
                        }
                    });
        }
    }
    */

    @Override
    protected void onStart() {
        super.onStart();
        RxPermissions
                .getInstance(this)
                .request(android.Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean granted) {
                        if (granted) {
                            onLocationPermissionGranted();
                        } else {
                            //Toast.makeText(BaseActivity.this, "Sorry, no demo without permission...", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void onLocationPermissionGranted() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /** Called when the user clicks the Send button */
    /*
    public void copButton(View view) {
        RadioGroup typeGroup = (RadioGroup) findViewById(R.id.typegroup);
        typeGroup.check(0);
    }
    */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {
            case R.id.action_askloc:
                if (ask_location_timeout) { break; }
                ask_location_timeout = true;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ask_location_timeout = false;
                    }
                }, 1000 * 15);
                client.askForLocations();
                break;

            case R.id.action_about:
                final View v = getLayoutInflater().inflate(R.layout.about, null);
                final Dialog dlg = new AlertDialog.Builder(MainActivity.this)
                        .setView(v)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
                break;

        }


        return super.onOptionsItemSelected(item);
    }

    private void dropPinEffect(final Marker marker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final long duration = 1500;

        final Interpolator interpolator = new BounceInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = Math.max(
                        1 - interpolator.getInterpolation((float) elapsed / duration), 0);
                marker.setAnchor(0.5f, 1.0f + 2 * t);

                if (t > 0.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 2);
                }
            }
        });

        /*handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = Math.max(
                        1 - interpolator.getInterpolation((float) elapsed
                                / duration), 0);
                marker.setAnchor(0.5f, 1.0f + 14 * t);

                if (t > 0.0) {
                    // Post again 15ms later.
                    handler.postDelayed(this, 5);
                } else {
                    marker.showInfoWindow();

                }
            }
        });*/
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        zIndex += 1;
        marker.setZIndex(zIndex);
        final MarkerHolder markerHolder = markerMap.get(marker.getTitle());

        final View v = getLayoutInflater().inflate(R.layout.marker_details, null);
        final Dialog dlg = new AlertDialog.Builder(MainActivity.this)
                .setView(v)
                .setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        marker_limit--;
                        markerHolder.onDestroy();
                    }
                })
                .create();

        Window window = dlg.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();

        wlp.gravity = Gravity.TOP;
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setAttributes(wlp);

        TextView description = (TextView) v.findViewById(R.id.marker_details_description);
        description.setText(markerHolder.description);
        TextView author = (TextView) v.findViewById(R.id.marker_details_author);
        author.setText(markerHolder.author);

        TextView timeview = (TextView) v.findViewById(R.id.marker_details_time);
        long timesince = System.currentTimeMillis() - markerHolder.time;
        String timestring;
        if (timesince < 1000) { timestring = "1s ago"; }
        else if (timesince < 1000 * 60) { timestring = DateFormat.format("s", timesince) + "s ago"; }
        else if (timesince < 1000*60*60) { timestring = DateFormat.format("m", timesince) + "m ago"; }
        else { timestring = DateFormat.format("h", timesince) + "h ago"; }
        //if (timestring.substring(0,1).equals("0")) { timestring = timestring.substring(1); }
        timeview.setText(timestring);
        //timeview.setText(DateFormat.format("dd/MM/yyyy hh:mm:ss", markerHolder.time));

        TextView address = (TextView) v.findViewById(R.id.marker_details_address);
        address.setText(markerHolder.address);
        String type = "";
        switch (markerHolder.markerType.ordinal()) {
            case 0:
                type = "POLICE";
                break;
            case 1:
                type = "CAMERA";
                break;
            case 2:
                type = "CRIME";
                break;
            case 3:
                type = "ACCIDENT";
                break;
        }
        TextView title = (TextView) v.findViewById(R.id.marker_details_title);
        title.setText(type);

        mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()), 200,
                new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        // do something
                        dlg.show();
                    }

                    @Override
                    public void onCancel() {
                        // do something
                    }
                });

        return true;
    }

    public Bitmap resizeMapIcons(String iconName, int width, int height){
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier(iconName, "mipmap", getPackageName()));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, Math.round(width*factor), Math.round(height*factor), false);
        return resizedBitmap;
    }

    @Override
    public void onMapLongClick(final LatLng point) {
        if (!tor_ready) { return; }
        if (marker_limit >= 5) {
            toast(getString(R.string.alert_limit));
            return;
        }
        Long time = System.currentTimeMillis();
        final MarkerHolder markerHolder = new MarkerHolder(this, point, tor.getID(), time);
        //mMap.setPadding(0,500,0,0);
        //markerMap.put(MarkerHolder.getHash(), MarkerHolder);

        //CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
        //map.animateCamera(cameraUpdate);

        final View v = getLayoutInflater().inflate(R.layout.marker_popup, null);
        final Dialog dlg = new AlertDialog.Builder(MainActivity.this)
                .setView(v)
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        markerMap.put(markerHolder.getHash(), markerHolder);

                        EditText editText = (EditText) v.findViewById(R.id.marker_description);
                        String description = editText.getText().toString();
                        Long time = System.currentTimeMillis();
                        markerHolder.description = description;
                        MarkerOptions markerOptions = new MarkerOptions().position(point).title(markerMap.get(markerHolder.getHash()).getHash());

                        RadioGroup typeGroup = (RadioGroup) v.findViewById(R.id.typegroup);
                        int icon_size = 100;
                        switch (typeGroup.getCheckedRadioButtonId()) {
                            case R.id.radio_police:
                                markerHolder.markerType = MarkerType.POLICE;
                                //markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                                //markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.cop_s));
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("cop_marker", icon_size, icon_size)));
                                break;
                            case R.id.radio_camera:
                                markerHolder.markerType = MarkerType.CAMERA;
                                //markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                                //markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.camera_s));
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("camera_marker", icon_size, icon_size)));
                                break;
                            case R.id.radio_crime:
                                markerHolder.markerType = MarkerType.CRIME;
                                //markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                                //markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.gun_s));
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("crime_marker", icon_size, icon_size)));
                                break;
                            case R.id.radio_accident:
                                markerHolder.markerType = MarkerType.ACCIDENT;
                                //markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                                //markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.warning_s));
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("warning_marker", icon_size, icon_size)));
                                break;
                        }

                        Log.i("daew", "description:: "+description);

                        Marker marker = mMap.addMarker(markerOptions);
                        zIndex += 1;
                        marker.setZIndex(zIndex);
                        marker.hideInfoWindow();
                        markerHolder.marker = marker;
                        //addGeofence(marker);
                        markerHolder.address = generateAddress(markerHolder.latLng.latitude, markerHolder.latLng.longitude).replace("null", "").trim();
                        Log.i("daew", "ADDRESS:: " + markerHolder.address);
                        dropPinEffect(markerHolder.marker);
                        db.addLocation(point.latitude, point.longitude, markerHolder.markerType.ordinal(), description, time, markerHolder.getHash(), tor.getID());
                        client.startSendLocations(seed_node, markerHolder.getHash());
                        marker_limit++;
                    }
                })

                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .create();
        //dlg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        Window window = dlg.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();

        wlp.gravity = Gravity.TOP;
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setAttributes(wlp);

        mMap.animateCamera(CameraUpdateFactory.newLatLng(point), 200, new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                // do something
                dlg.show();
            }

            @Override
            public void onCancel() {
                // do something
            }
        });

        //mMap.addMarker(new MarkerOptions().position(point).title("clik"));
        return;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //addGeofence();
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);
        updateMap();
        //mMap.setOnMyLocationButtonClickListener(this);


        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle_dark));
    }
     /*

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            double lat = mLastLocation.getLatitude();
            double lng = mLastLocation.getLongitude();

            LatLng loc = new LatLng(lat, lng);
            mMap.addMarker(new MarkerOptions().position(loc).title("New Marker"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {} */

    void update() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mMap != null) {
                    updateMap();
                }
            }
        });
    }

    void torReady() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tor_ready = true;
                toast(getString(R.string.press_hold));
            }
        });
    }

    void updateMap() {
        marker_limit = 0;
        locationCursor = db.getReadableDatabase().query("locations", null, "deleted=?", new String[]{""+0}, null, null, null);
        Log.i("Main", "updateMap " + locationCursor.getCount());
        //addGeofence();

        if (locationCursor.getCount() > 0) {
            while (locationCursor.moveToNext()) {
                long time = locationCursor.getLong(locationCursor.getColumnIndex("time"));
                String hash = locationCursor.getString(locationCursor.getColumnIndex("hash"));
                if (System.currentTimeMillis() - time < 1000 * 60 * 60) {
                    Log.i("daew", "hash:: " + hash);
                    String author = locationCursor.getString(locationCursor.getColumnIndex("author"));
                    Log.i("AQUIOWW::::: ", author.toString()+ " " + tor.getID() + " " +(author.equals(tor.getID())));
                    if (author.equals(tor.getID())) { marker_limit++; }
                    if (!markerMap.containsKey(hash)) {
                    /*placeMarker(locationCursor.getLong(locationCursor.getColumnIndex("latitude")),
                            locationCursor.getLong(locationCursor.getColumnIndex("longitude")),
                            locationCursor.getString(locationCursor.getColumnIndex("author")),
                            locationCursor.getString(locationCursor.getColumnIndex("text")),
                            locationCursor.getInt(locationCursor.getColumnIndex("time")),
                            locationCursor.getInt(locationCursor.getColumnIndex("type")),
                            hash
                    );*/


                        double latitude = locationCursor.getDouble(locationCursor.getColumnIndex("latitude"));
                        double longitude = locationCursor.getDouble(locationCursor.getColumnIndex("longitude"));
                        String description = locationCursor.getString(locationCursor.getColumnIndex("text"));

                        MarkerHolder markerHolder = new MarkerHolder(this, new LatLng(latitude, longitude), author, time, hash);

                        MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(latitude, longitude))
                                .title(hash);
                        switch (locationCursor.getInt(locationCursor.getColumnIndex("type"))) {
                            case 0:
                                //markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("cop_marker", icon_size, icon_size)));
                                markerHolder.markerType = MarkerType.POLICE;
                                break;
                            case 1:
                                //markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("camera_marker", icon_size, icon_size)));
                                markerHolder.markerType = MarkerType.CAMERA;
                                break;
                            case 2:
                                //markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("crime_marker", icon_size, icon_size)));
                                markerHolder.markerType = MarkerType.CRIME;
                                break;
                            case 3:
                                //markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("warning_marker", icon_size, icon_size)));
                                markerHolder.markerType = MarkerType.ACCIDENT;
                                break;
                        }

                        markerHolder.description = description;
                        Marker marker = mMap.addMarker(markerOptions);

                        marker.hideInfoWindow();
                        markerHolder.marker = marker;
                        markerHolder.address = generateAddress(markerHolder.latLng.latitude, markerHolder.latLng.longitude).replace("null", "").trim();
                        dropPinEffect(markerHolder.marker);
                        //addGeofence(marker);

                        //Log.i("daew", "hash:: "+hash);
                        markerMap.put(hash, markerHolder);
                    } else {
                        Log.i("daew", "already in map");
                    }
                } else {
                    if (markerMap.containsKey(hash)) {
                        markerMap.get(hash).onDestroy();
                    }
                    db.removeLocation(hash);
                    Log.i("", "Old location, hash:: "+hash);
                }
            }
        }

        findViewById(R.id.mapload).setVisibility(View.GONE);
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //client.askForLocations();

        Tor.getInstance(this).setListener(new Tor.Listener() {
            @Override
            public void onChange() {
                update();
                //send();
            }
        });
        Server.getInstance(this).setListener(new Server.Listener() {
            @Override
            public void onChange() {
                update();
            }
        });
        //update();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                Log.i("","update on resume");
                client.askForLocations();
                update();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //findViewById(R.id.mapload).setVisibility(View.VISIBLE);
                    }
                });
            }
        }, 0, 1000 * 60 * 5);

        //send();

        //Notifier.getInstance(this).onResumeActivity();

        //((TorStatusView) findViewById(R.id.torStatusView)).update();

        //startService(new Intent(this, HostService.class));
    }

    @Override
    protected void onPause() {
        //Notifier.getInstance(this).onPauseActivity();
        Tor.getInstance(this).setListener(null);
        Server.getInstance(this).setListener(null);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //timer.cancel();
        //timer.purge();
        //clearGeofence();
        //subscription.unsubscribe();
    }

    public String generateAddress(double latitude, double longitude) {
        try {
            Geocoder geo = new Geocoder(this.getApplicationContext(), Locale.getDefault());
            List<Address> addresses = geo.getFromLocation(latitude, longitude, 1);
            if (addresses.size() > 0) {
                    return addresses.get(0).getAddressLine(0) + "\n" + addresses.get(0).getLocality();
                    //Toast.makeText(getApplicationContext(), "Address:- " + addresses.get(0).getFeatureName() + addresses.get(0).getAdminArea() + addresses.get(0).getLocality(), Toast.LENGTH_LONG).show();
                }
        }
        catch (Exception e) {
            e.printStackTrace(); // getFromLocation() may sometimes fail
        }
        return "";
    }
}

