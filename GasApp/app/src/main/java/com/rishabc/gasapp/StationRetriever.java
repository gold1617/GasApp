package com.rishabc.gasapp;

import android.os.AsyncTask;

import org.json.JSONObject;

public class StationRetriever extends AsyncTask<Object, Void, JSONObject>
{

    private WebRequestCallback delegate = null;

    public StationRetriever(WebRequestCallback stationsCallback)
    {
        delegate = stationsCallback;
    }

    @Override
    protected JSONObject doInBackground(Object[] objects)
    {
        String url = (String) objects[0];
        String authKey = (String) objects[1];
        JSONObject stations = URLHelper.myAPIGet(url, authKey);

        return stations;
    }

    @Override
    protected void onPostExecute(JSONObject jsonObject)
    {
        if (delegate != null)
            delegate.callBack(jsonObject);
    }
}
