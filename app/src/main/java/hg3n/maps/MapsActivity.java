package hg3n.maps;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.EditText;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager mManager;
    private LocationListener mListener;
    private int numberOfMarkers = 0;
    private EditText mTextField;
    private Marker mUserMarker;
    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mPrefEditor;
    private String markerKey = "com.maps.com.markers";
    private HashMap<Marker, Circle> mMarkerHalos;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // initialize location manager and location service
        getSystemService(Context.LOCATION_SERVICE);

        // init text field by id
        mTextField = (EditText)this.findViewById(R.id.markerName);

        // init user marker as null
        mUserMarker = null;

        // init shared preferences
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefEditor = mSharedPref.edit();

        mMarkerHalos = new HashMap<Marker, Circle>();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                updateHalos(cameraPosition);
            }
        });

        int permission_check = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        if (permission_check == PackageManager.PERMISSION_GRANTED) {
            // instanciate location manager to retrieve positions
            mManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            // retrieve best provider for position tracking
            String provider = mManager.getBestProvider(new Criteria(), true);

            // get current location
            Location location = mManager.getLastKnownLocation(provider);

            mListener =  new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    //redraw marker when getLocation updates
                    drawUserMarker(location);
                }

                @Override
                public void onProviderEnabled(String p) {
                }

                @Override
                public void onProviderDisabled(String p) {
                }

                @Override
                public void onStatusChanged(String p, int status, Bundle extras) {
                }
            };

            // place initial marker if current position retrieved
            if(location != null) {
                drawUserMarker(location);
            }

            // register location updates with given parameters
            mManager.requestLocationUpdates(provider, 1000, 0, mListener);
        } else {
            // Add a marker in Sydney and move the camera
            LatLng sydney = new LatLng(-34, 151);
            mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        }

        // implement event listener for long map clicks
        GoogleMap.OnMapLongClickListener onMapLongClick = new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {

                // get marker name from text field
                String markerName = mTextField.getText().toString();

                // check if a marker name is set
                if(markerName.isEmpty()) {
                    markerName = "Marker" + Integer.toString(numberOfMarkers);
                }

                // add marker
                Marker marker = mMap.addMarker(
                        new MarkerOptions()
                                .position(latLng)
                                .title(markerName)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                );

                // add halo
                Circle halo = mMap.addCircle(new CircleOptions()
                                .center(latLng)
                                .radius(100.0)
                                .strokeColor(Color.RED)
                                .visible(false)
                );

                // add marker and halo to tracked markers
                mMarkerHalos.put(marker, halo);

                // lat and long to separate values
                double markerLat = latLng.latitude;
                double markerLong = latLng.longitude;

                // save marker to shared preferences
                Set<String> positionSet = new HashSet<String>();
                positionSet.add(Double.toString(markerLat));
                positionSet.add(Double.toString(markerLong));
                mPrefEditor.putStringSet(markerName, positionSet);
                mPrefEditor.apply();

                // clear text field
                mTextField.setText("");

                // increase marker counter
                ++numberOfMarkers;
            }
        };

        // assign event listener to map
        mMap.setOnMapLongClickListener(onMapLongClick);
    }

    private void drawUserMarker(Location location) {
        // TODO: replace clear with one single moving marker
        LatLng current_position = new LatLng(location.getLatitude(), location.getLongitude());
        if(mUserMarker == null) {
            mUserMarker = mMap.addMarker(
                    new MarkerOptions()
                            .position(current_position)
                            .snippet("Lat:" + location.getLatitude() + " Lng:" + location.getLongitude())
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            .title("My Position")
            );
        }
        else {
            mUserMarker.setPosition(current_position);
            mUserMarker.setSnippet("Lat:" + location.getLatitude() + " Lng:" + location.getLongitude());
        }
    }

    private void updateHalos(CameraPosition cameraPosition) {
        Projection proj = mMap.getProjection();
        LatLngBounds bounds = proj.getVisibleRegion().latLngBounds;
        LatLng center = bounds.getCenter();

        for(Marker m : mMarkerHalos.keySet()) {

            if(bounds.contains(m.getPosition())) {
                // hide halo if marker visible
                mMarkerHalos.get(m).setVisible(false);
            }
            else {
                // show halo if marker visible
                mMarkerHalos.get(m).setVisible(true);

                // get LatLonBounds min, max and marker in screen coords
                Point min = proj.toScreenLocation(bounds.southwest);
                Point max = proj.toScreenLocation(bounds.northeast);
                max.set(Math.max(max.x, min.x),Math.max(max.y, min.y));
                min.set(0,0);

                Point p = proj.toScreenLocation(m.getPosition());

                // compute direction to box boundary
                int x = Math.max(Math.max(min.x-p.x, p.x-max.x), 0);
                int y = Math.max(Math.max(min.y-p.y, p.y-max.y), 0);

                // compute transformed marker position into screen bounds
                int pix_off = 60;
                double dist = Math.sqrt(x*x + y*y);
                int t_x = (int) Math.round(x + pix_off*(x/dist));
                int t_y = (int) Math.round(y + pix_off*(y/dist));

                // compute point in screen bounds
                Point p_in_box = new Point(p.x+t_x, p.y+t_y);

                // compute radius as meter distance between (LatLon) point in screen bounds and marker
                double r = distance(proj.fromScreenLocation(p_in_box), m.getPosition());
                mMarkerHalos.get(m).setRadius(r);
            }
        }
    }

    private double distance(LatLng p1, LatLng p2) {
        Location p1_loc = new Location("p1 location");
        p1_loc.setLatitude(p1.latitude);
        p1_loc.setLongitude(p1.longitude);

        Location p2_loc = new Location("p2 location");
        p2_loc.setLatitude(p2.latitude);
        p2_loc.setLongitude(p2.longitude);

        return p1_loc.distanceTo(p2_loc);
    }
}
