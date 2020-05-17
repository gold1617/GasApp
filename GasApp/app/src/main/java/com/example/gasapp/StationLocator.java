package com.example.gasapp;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StationLocator extends AsyncTask<Object, Void, JSONObject>
{
    WebRequestCallback delegate = null;

    public StationLocator(WebRequestCallback stationLocationCallback)
    {
        delegate = stationLocationCallback;

    }

    @Override
    protected JSONObject doInBackground(Object[] objects)
    {
        String request = (String) objects[0];
        String station = (String) objects[1];

        JSONObject response = new JSONObject();

        try
        {
            JSONObject singleResponse = URLHelper.simpleGet(request);

            if (singleResponse != null)
            {
                JSONArray temp = singleResponse.getJSONObject("response").getJSONArray("venues");
                response.put(station, temp);
                Log.d("NETWORK",response.toString());
            }
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
        if (delegate != null && !isCancelled())
            delegate.callBack(jsonObject);
    }
}
