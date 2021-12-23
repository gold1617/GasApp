package com.rishabc.gasapp;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StationLocator extends AsyncTask<Object, Void, JSONObject>
{
    private WebRequestCallback delegate = null;
    private boolean isLast = false;

    public StationLocator(WebRequestCallback stationLocationCallback)
    {
        delegate = stationLocationCallback;
    }

    @Override
    protected JSONObject doInBackground(Object[] objects)
    {
        String request = (String) objects[0];
        String station = (String) objects[1];
        String authorization = (String) objects[2];

        JSONObject response = new JSONObject();

        try
        {
            JSONObject singleResponse = URLHelper.simpleGet(request,authorization);

            if (singleResponse != null)
            {
                JSONArray temp = singleResponse.getJSONArray("results");
                response.put(station, temp);
                response.put("isLast",isLast);
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

    public void setLast(boolean last)
    {
        isLast = last;
    }
}
