package com.rishabc.gasapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback
{

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Location lastLocation;
    private boolean hasLocationPermission = false;
    private JSONArray stationList;
    private WebRequestCallback stationLocationCallback;
    private ArrayList<LatLng> gasStationLocs;
    private Geocoder geocoder;
    private List<StationLocator> stationLocatorList;
    private FloatingActionButton listButton = null;
    private StationRetriever retriever;

    private static final int API_AVAILABILITY_REQUEST = 1;
    private static final int LOCATION_PERMISSION_REQUEST = 2;

    static ArrayList<GasStation> gasStations;
    static StationListAdapter stationListAdapter;

    private int requests_processed = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        listButton = findViewById(R.id.list_view_fab);

        //Obtain FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if(gasStations == null || stationListAdapter == null)
        {
            gasStations = new ArrayList<GasStation>();
            stationListAdapter = new StationListAdapter(getApplicationContext(),gasStations);
        }
        gasStationLocs = new ArrayList<LatLng>();
        geocoder = new Geocoder(this, Locale.getDefault());
        stationLocatorList = new ArrayList<>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        listButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(v.getContext(),StationListActivity.class);
                intent.putParcelableArrayListExtra("GasStations", gasStations);
                startActivityForResult(intent,0);
            }
        });

        stationLocationCallback = new WebRequestCallback()
        {
            @Override
            public void callBack(JSONObject response)
            {
                if (response != null)
                {
                    Iterator<String> iter = response.keys();
                    String key,name,searchName,address;
                    JSONArray jsonArray;
                    JSONObject jsonObject;
                    LatLng loc;

                    int listpos = 0;

                    if (iter.hasNext())
                    {
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
                                if (searchName.equals("brands") || name.toLowerCase().contains(searchName))
                                {
                                    JSONObject locationObject = jsonObject.optJSONObject("location");
                                    JSONObject geocodeObject = jsonObject.optJSONObject("geocodes").optJSONObject("main");

                                    loc = getImprovedLatLng(locationObject,geocodeObject);

                                    if (!isDuplicate(loc))
                                    {
                                        if (!locationObject.optString("address").equals(""))
                                            address = locationObject.optString("address") + "," + locationObject.optString("locality") + ","
                                                    + locationObject.optString("region");
                                        else
                                            address = null;
                                        GasStation station = new GasStation(name, loc, lastLocation, address);
                                        listpos = getListPos(0, gasStations.size()-1,station);
                                        gasStations.add(listpos,station);
                                        stationListAdapter.notifyDataSetChanged();
                                        gasStationLocs.add(loc);

                                        mMap.addMarker(new MarkerOptions().position(loc).title(name)).setTag(station);
                                    }
                                }
                            }
                        }
                    }
                }
                requests_processed++;

                if (requests_processed == stationLocatorList.size())
                {
                    if (listButton != null)
                        listButton.show();

                    Toast.makeText(getApplicationContext(), getString(R.string.LocatedMsg), Toast.LENGTH_SHORT).show();
                    Log.v("DATA_ADD","All Stations added");
                }
            }
        };
    }

    private int getListPos(int left, int right, GasStation target)
    {
        if(right < left)
            return left;

        int mid = (left+right)/2;
        if (gasStations.get(mid).getDistance() < target.getDistance())
            return getListPos(mid+1, right, target);
        else
            return getListPos(left, mid-1,target);
    }

    private LatLng getImprovedLatLng(JSONObject locationObject,JSONObject geocodeObject)
    {
        if(locationObject.optString("address").equals(""))
        {
            String address = locationObject.optString("address") + "," + locationObject.optString("locality") + ","
                    + locationObject.optString("region") + " " + locationObject.optString("postcode");

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

        return new LatLng(geocodeObject.optDouble("latitude"), geocodeObject.optDouble("longitude"));
    }

    private boolean isDuplicate(LatLng loc)
    {
        return gasStationLocs.contains(loc);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        //Make sure Google Play services is installed, enabled, and up to date
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
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

        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        mMap.setInfoWindowAdapter(new StationInfoWindowAdapter(getLayoutInflater()));

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener()
        {
            @Override
            public void onInfoWindowClick(Marker marker)
            {
                GasStation station = (GasStation)marker.getTag();
                LatLng loc = station.getLocation();

                String intentString = "";
                if (station.getAddress() != null)
                    intentString = String.format(getString(R.string.mapsIntent_Address), loc.latitude, loc.longitude, Uri.encode(station.getAddress()));
                else
                    intentString = String.format(getString(R.string.mapsIntent_LatLng), loc.latitude, loc.longitude);

                // Create a Uri from an intent string. Use the result to create an Intent.
                Uri gmmIntentUri = Uri.parse(intentString);

                // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                // Make the Intent explicit by setting the Google Maps package
                mapIntent.setPackage("com.google.android.apps.maps");

                // Attempt to start an activity that can handle the Intent
                startActivity(mapIntent);
            }
        });

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
            FloatingActionButton locateButton = findViewById(R.id.locateMeButton);
            locateButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    updateLocation();
                }
            });


            if (hasLocationPermission)
            {
                mMap.setMyLocationEnabled(true);
            }
            else
            {
                mMap.setMyLocationEnabled(false);
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

                            prepareToLocateStations();

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

    private void prepareToLocateStations()
    {
        String country = "";
        if(lastLocation == null)
            return;

        if(retriever != null && !retriever.getStatus().equals(AsyncTask.Status.FINISHED))
            retriever.cancel(true);

        for(StationLocator task:stationLocatorList)
        {
            if(!task.getStatus().equals(AsyncTask.Status.FINISHED))
                task.cancel(true);
        }

        mMap.clear();
        gasStations.clear();
        stationListAdapter.notifyDataSetChanged();
        gasStationLocs.clear();
        stationLocatorList.clear();
        requests_processed = 0;

        if (listButton != null)
            listButton.hide();

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

        if(stationList != null && stationList.optJSONObject(0).optString("location","").equalsIgnoreCase(country))
            locateNearbyStations();
        else
        {
            retriever = new StationRetriever(new WebRequestCallback()
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
            });

            retriever.execute(new Object[]{getString(R.string.stations_url) + country, getString(R.string.X_API_KEY)});
        }
    }

    private void locateNearbyStations()
    {

        String urlString = String.format(getString(R.string.nearby_names_url), lastLocation.getLatitude(),
                lastLocation.getLongitude());

        JSONObject station = null;

        Toast.makeText(getApplicationContext(), getString(R.string.LocatingMsg), Toast.LENGTH_SHORT).show();
        StringBuilder brandUrlBuilder = new StringBuilder();
        String brandId = "";

        String request = "";
        Log.v("DATA_ADD", "Start station queries");
        StationLocator stationLocator;
        for(int i = 0; i < stationList.length(); i++)
        {
            station = stationList.optJSONObject(i);

            brandId = station.optString("brand_id","");
            if(!brandId.isEmpty())
            {
                if(brandUrlBuilder.length() > 0)
                    brandUrlBuilder.append(',');

                brandUrlBuilder.append(brandId);
            }
            else
            {
                try
                {
                    request = urlString + URLEncoder.encode(station.optString("name"),"UTF-8");

                    stationLocator = new StationLocator(stationLocationCallback);
                    stationLocatorList.add(stationLocator);

                    stationLocator.execute(request,station.optString("name"), getString(R.string.foursquare_api_key));
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                }
            }
        }
        stationLocator = new StationLocator(stationLocationCallback);
        stationLocatorList.add(stationLocator);

        urlString = String.format(getString(R.string.nearby_brands_url), lastLocation.getLatitude(),
                lastLocation.getLongitude());

        brandUrlBuilder.insert(0,urlString);

        stationLocator.execute(brandUrlBuilder.toString(),"brands",getString(R.string.foursquare_api_key));
    }
}
