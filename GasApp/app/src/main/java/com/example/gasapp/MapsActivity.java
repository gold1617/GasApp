package com.example.gasapp;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private GoogleApiAvailability googleApiAvailability;
    private Location lastLocation;
    private boolean hasLocationPermission = false;
    public JSONArray stationList;

    private static final int API_AVAILABILITY_REQUEST = 1;
    private static final int LOCATION_PERMISSION_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //Obtain FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        WebRequestCallback stationsCallback = new WebRequestCallback() {
            @Override
            public void callBack(JSONObject response) {
                if (response != null)
                {
                    try {
                        stationList = response.getJSONArray("stations");
                    } catch (JSONException e) {
                        Log.e("NETWORK","Error in stationList Callback" + e.getMessage());
                        e.printStackTrace();
                    }
                }
                else
                    stationList = null;

            }
        };

        StationRetriever retriever = new StationRetriever(stationsCallback);
        retriever.execute(new Object[]{getString(R.string.stations_url),getString(R.string.X_API_KEY)});
    }

    @Override
    public void onResume(){
        super.onResume();
        //Make sure Google Play services is installed, enabled, and up to date
        googleApiAvailability = GoogleApiAvailability.getInstance();
        int apiAvailability = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability != ConnectionResult.SUCCESS)
            googleApiAvailability.getErrorDialog(this,apiAvailability,API_AVAILABILITY_REQUEST).show();

        //If play services is available check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        else
            hasLocationPermission = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults)
    {
        switch (requestCode)
        {
            case LOCATION_PERMISSION_REQUEST:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasLocationPermission = true;
                }
                else
                {
                    hasLocationPermission = false;
//                    AlertDialog alert = new AlertDialog.Builder(this).
//                            setMessage("Location permission is required for finding nearby gas stations").setCancelable(false)
//                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int id) {
//                                    dialog.cancel();
//                                }
//                            }).show();
                }
                updateLocation();
                break;
            // other 'case' lines to check for other
            // permissions this app might request.
        }
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

        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {

            @Override
            public boolean onMyLocationButtonClick() {
                updateLocation();
                return true;
            }
        });

        updateLocationUI();

        updateLocation();
    }

    private void updateLocationUI()
    {
        if (mMap == null) {
            return;
        }
        try {
            if (hasLocationPermission) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                lastLocation = null;
            }
        } catch (SecurityException e)  {
            Log.e("LOCATION", e.getMessage());
        }
    }

    private void updateLocation()
    {
        try {
            if (hasLocationPermission) {
                fusedLocationClient.getLastLocation().addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful())
                        {
                            lastLocation = (Location) task.getResult();
                            LatLng myLoc = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                            CameraPosition myPosition = new CameraPosition.Builder().target(myLoc).zoom(12).build();
                            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(myPosition));
                        }
                        else
                        {
                            Log.e("LOCATION", "Current location is null");
                            Log.e("LOCATION", task.getException().getMessage());
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("LOCATION", e.getMessage());
        }
    }
}
