package hg3n.maps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.EditText;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.lang.reflect.Array;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager mManager;
    private LocationListener mListener;
    private int numberOfMarkers = 0;
    private EditText mTextField;
    private Marker mUserMarker;

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

        mTextField = (EditText)this.findViewById(R.id.markerName);

        mUserMarker = null;
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
                mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(markerName)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

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
}
