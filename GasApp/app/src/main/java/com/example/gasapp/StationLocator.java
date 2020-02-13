package com.example.gasapp;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class StationLocator extends AsyncTask<Object, Void, JSONObject>
{
    WebRequestCallback delegate = null;
    private Geocoder geocoder;
    private String country;

    public StationLocator(WebRequestCallback stationLocationCallback, Context context)
    {
        delegate = stationLocationCallback;
        geocoder = new Geocoder(context, Locale.getDefault());
        country = context.getResources().getConfiguration().locale.getDisplayCountry();

    }

    @Override
    protected JSONObject doInBackground(Object[] objects)
    {
        String baseRequest = (String) objects[0];
        JSONArray stationList = (JSONArray) objects[1];
        Location lastLocation = (Location) objects[2];
        JSONObject response = new JSONObject();

        try
        {
            List<Address> addresses = geocoder.getFromLocation(lastLocation.getLatitude(),lastLocation.getLongitude(),1);

            if (addresses != null && !addresses.isEmpty())
            {
                Address tempAddress = addresses.get(0);
                country = tempAddress.getCountryName();
            }

            if(country.equalsIgnoreCase("United States"))
                country = "USA";

            JSONObject station = null;
            for(int i =0;i < stationList.length();i++)
            {
                if (isCancelled())
                    return response;


                station = stationList.getJSONObject(i);

                if (station.optString("location").equalsIgnoreCase(country))
                {
                    String request = baseRequest + station.getString("name").replace(" ", "%20");
                    JSONObject singleResponse = URLHelper.simpleGet(request);

                    if (singleResponse != null)
                    {
                        JSONArray temp = singleResponse.getJSONObject("response").getJSONArray("venues");
                        response.put(station.getString("name"), temp);
                    }
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (JSONException e)
        {
            Log.e("Locate stations", "Error searching for stations" + e.getMessage());
        }


        return response;
    }

    @Override
    protected void onPostExecute(JSONObject jsonObject)
    {
        if (delegate != null)
            delegate.callBack(jsonObject);
    }
}
