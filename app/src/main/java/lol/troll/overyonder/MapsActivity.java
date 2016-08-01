package lol.troll.overyonder;

import android.app.FragmentManager;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.TravelMode;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String LOCATION_KEY = "LAST_KNOWN_LOCATION";

    // I'm not a fan of member variables for everything...
    private GoogleApiClient googleApiClient;
    private GoogleMap googleMap;
    private Location lastLoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        ensureGoogleApiClientExists();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = this.getMapFragment();
        mapFragment.getMapAsync(this);

        updateValuesFromBundle(savedInstanceState);
    }

    private SupportMapFragment getMapFragment() {
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        SupportMapFragment supportMapFragment = (SupportMapFragment) fragmentManager
                .findFragmentById(R.id.map);
        return supportMapFragment;
//        return (MapFragment) getFragmentManager().findFragmentById(R.id.map);
    }

    private void ensureGoogleApiClientExists() {
        // Create an instance of GoogleAPIClient.
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    /**
     * Implementation of GoogleApiClient.ConnectionCallbacks
     * @param bundle
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startGettingLocationUpdates();
    }

    private void startGettingLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    private EncodedPolyline getPathToLocation(com.google.maps.model.LatLng start, com.google.maps.model.LatLng destination) throws Exception {
        GeoApiContext context = new GeoApiContext()
                .setApiKey(this.getResources().getString(R.string.google_maps_key));

        DirectionsResult result = com.google.maps.DirectionsApi.newRequest(context)
                .mode(TravelMode.WALKING)
                .origin(start)
                .destination(destination)
                .await();

        EncodedPolyline thingToDrawOnMap = result.routes[0].overviewPolyline;
        return thingToDrawOnMap;
    }

    @Override
    public void onLocationChanged(Location loc) {
        Toast.makeText(this, "Got location " + System.currentTimeMillis(), Toast.LENGTH_SHORT);

        lastLoc = loc;
        centerMapAtLastKnownLocation();
    }

    private Marker lastPaintedPos;
    private void centerMapAtLastKnownLocation() {
        if (lastPaintedPos != null) {
            lastPaintedPos.remove();
        }
        LatLng pos = new LatLng(lastLoc.getLatitude(), lastLoc.getLongitude());

        lastPaintedPos = googleMap.addMarker(new MarkerOptions().position(pos).title("Current pos"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 12f));


        com.google.maps.model.LatLng source = new com.google.maps.model.LatLng(lastLoc.getLatitude(), lastLoc.getLongitude());
        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(lastLoc.getLatitude(), lastLoc.getLongitude() + 0.05);
        tryPaintLineBetweenLocs(source, destination);
    }

    private void tryPaintLineBetweenLocs(com.google.maps.model.LatLng source, com.google.maps.model.LatLng destination) {
        try {
            PolylineOptions line = getPolyLineFormLocs(source, destination);
            googleMap.addPolyline(line);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NonNull
    private PolylineOptions getPolyLineFormLocs(com.google.maps.model.LatLng source, com.google.maps.model.LatLng destination) throws Exception {
        PolylineOptions line = new PolylineOptions();
        for(com.google.maps.model.LatLng pointOnLine : getPathToLocation(source, destination).decodePath()) {
            line.add(new LatLng(pointOnLine.lat, pointOnLine.lng));
        }
        return line;
    }

    /**
     * Implementation of GoogleApiClient.ConnectionCallbacks
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {
    }

    /**
     * Implementation of GoogleApiClient.OnConnectionFailedListener
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Failed connecting to the locations services. Cannot tell where you are...\n" + connectionResult.getErrorMessage(), Toast.LENGTH_LONG);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(LOCATION_KEY, lastLoc);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of lastLoc from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // since LOCATION_KEY was found in the Bundle, we can be sure that
                // lastLoc not null.
                lastLoc = savedInstanceState.getParcelable(LOCATION_KEY);
            }
            centerMapAtLastKnownLocation();
        }
    }
}
