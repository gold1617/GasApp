package com.example.gasapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

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
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback
{

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private GoogleApiAvailability googleApiAvailability;
    private Location lastLocation;
    private boolean hasLocationPermission = false;
    private JSONArray stationList;
    private WebRequestCallback stationLocationCallback;
    private StationLocator stationLocator;
    private ArrayList<GasStation> gasStations;
    private Geocoder geocoder;
    private List<StationLocator> stationLocatorList;

    private static final int API_AVAILABILITY_REQUEST = 1;
    private static final int LOCATION_PERMISSION_REQUEST = 2;

    //TODO: Change look of location and maps/navigate buttons

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //Obtain FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        gasStations = new ArrayList<GasStation>();
        geocoder = new Geocoder(this, Locale.getDefault());
        stationLocatorList = new ArrayList<>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        WebRequestCallback stationsListCallback = new WebRequestCallback()
        {
            @Override
            public void callBack(JSONObject response)
            {
                if (response != null)
                {
                    try
                    {
                        stationList = response.getJSONArray("stations");
                        Log.d("Stations", stationList.toString());
                        locateNearbyStations();
                    }
                    catch (JSONException e)
                    {
                        Log.e("NETWORK", "Error in stationList Callback" + e.getMessage());
                        e.printStackTrace();
                    }
                }
                else
                    stationList = null;

            }
        };

        StationRetriever retriever = new StationRetriever(stationsListCallback);
        retriever.execute(new Object[]{getString(R.string.stations_url), getString(R.string.X_API_KEY)});

        stationLocationCallback = new WebRequestCallback()
        {
            @Override
            public void callBack(JSONObject response)
            {
                if (response != null)
                {
                    Iterator<String> iter = response.keys();
                    String key,name,searchName;
                    JSONArray jsonArray;
                    JSONObject jsonObject;
                    LatLng loc;

                    key = iter.next();
                    searchName = key.toLowerCase().contains("costco") ? "costco" : key.toLowerCase();
                    jsonArray = response.optJSONArray(key);

                    if (jsonArray != null)
                    {
                        for (int i = 0; i < jsonArray.length(); i++)
                        {
                            jsonObject = jsonArray.optJSONObject(i);
                            name = jsonObject.optString("name", "");

                            //Filter closely named location returned by FourSquare Api
                            if (name.toLowerCase().contains(searchName))
                            {
                                jsonObject = jsonObject.optJSONObject("location");

                                loc = getImprovedLatLng(jsonObject);

                                if(!isDuplicate(loc))
                                {
                                    GasStation station = new GasStation(name,loc,lastLocation);
                                    gasStations.add(station);

                                    //TODO: Add List View
                                    mMap.addMarker(new MarkerOptions().position(loc).title(name));
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    private LatLng getImprovedLatLng(JSONObject jsonObject)
    {
        if(jsonObject.optString("address") != "")
        {
            String address = jsonObject.optString("address") + "," + jsonObject.optString("city") + ","
                    + jsonObject.optString("state") + " " + jsonObject.optString("postalCode");

            try
            {
                List<Address> addresses = geocoder.getFromLocationName(address,1);

                if (addresses != null && !addresses.isEmpty())
                    return new LatLng(addresses.get(0).getLatitude(),addresses.get(0).getLongitude());
            }
            catch (Exception e)
            {
                Log.e("ImproveLocation",e.getStackTrace().toString());
            }

        }

        return new LatLng(jsonObject.optDouble("lat"), jsonObject.optDouble("lng"));
    }

    private boolean isDuplicate(LatLng loc)
    {
        for (GasStation g: gasStations)
        {
            if(loc.equals(g.getLocation()))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        //Make sure Google Play services is installed, enabled, and up to date
        googleApiAvailability = GoogleApiAvailability.getInstance();
        int apiAvailability = googleApiAvailability.isGooglePlayServicesAvailable(this);

        if (apiAvailability != ConnectionResult.SUCCESS)
            googleApiAvailability.getErrorDialog(this, apiAvailability, API_AVAILABILITY_REQUEST).show();

        //If play services is available check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        }
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
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    hasLocationPermission = true;
                }
                else
                {
                    hasLocationPermission = false;
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
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;

        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener()
        {

            @Override
            public boolean onMyLocationButtonClick()
            {
                updateLocation();
                return true;
            }
        });

        mMap.setInfoWindowAdapter(new StationInfoWindowAdapter(getLayoutInflater()));

        updateLocationUI();

        updateLocation();
    }

    private void updateLocationUI()
    {
        if (mMap == null)
        {
            return;
        }
        try
        {
            if (hasLocationPermission)
            {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            }
            else
            {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                lastLocation = null;
            }
        }
        catch (SecurityException e)
        {
            Log.e("LOCATION", e.getMessage());
        }
    }

    private void updateLocation()
    {
        try
        {
            if (hasLocationPermission)
            {
                fusedLocationClient.getLastLocation().addOnCompleteListener(this, new OnCompleteListener<Location>()
                {
                    @Override
                    public void onComplete(@NonNull Task task)
                    {
                        if (task.isSuccessful())
                        {
                            lastLocation = (Location) task.getResult();
                            if(lastLocation == null)
                                return;

                            LatLng myLoc = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                            CameraPosition myPosition = new CameraPosition.Builder().target(myLoc).zoom(12).build();
                            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(myPosition));

                            Log.d("Stations","Location updated");
                            if (stationList != null)
                            {
                                locateNearbyStations();
                            }
                        }
                        else
                        {
                            Log.e("LOCATION", "Current location is null");
                            Log.e("LOCATION", task.getException().getMessage());
                        }
                    }
                });
            }
        }
        catch (SecurityException e)
        {
            Log.e("LOCATION", e.getMessage());
        }
    }

    private void locateNearbyStations()
    {
        String country = "";
        if(lastLocation == null)
            return;

        for(StationLocator task:stationLocatorList)
        {
            if(task.getStatus().equals(AsyncTask.Status.RUNNING))
                task.cancel(true);
        }

        mMap.clear();
        gasStations.clear();
        stationLocatorList.clear();

        try
        {
            List<Address> addresses  = geocoder.getFromLocation(lastLocation.getLatitude(),lastLocation.getLongitude(),1);
            if (addresses != null && !addresses.isEmpty())
            {
                Address tempAddress = addresses.get(0);
                country = tempAddress.getCountryName();
            }

        }
        catch (IOException e)
        {
            country = this.getResources().getConfiguration().locale.getDisplayCountry();
        }


        if(country.equalsIgnoreCase("United States"))
            country = "USA";

        String urlString = String.format(getString(R.string.nearby_url), lastLocation.getLatitude(),
                lastLocation.getLongitude(),
                getString(R.string.foursquare_client_id),
                getString(R.string.foursquare_client_secret));

        JSONObject station = null;
        for(int i =0;i < stationList.length();i++)
        {
            station = stationList.optJSONObject(i);

            if (country.equalsIgnoreCase(station.optString("location")))
            {
                String request = urlString + station.optString("name").replace(" ", "%20");

                stationLocator = new StationLocator(stationLocationCallback);
                stationLocatorList.add(stationLocator);

                stationLocator.execute(request,station.optString("name"));
            }
        }

    }
}
