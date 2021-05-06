package com.rishabc.gasapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class StationListAdapter extends ArrayAdapter<GasStation> implements View.OnClickListener
{
    final private double  METER_TO_MILE = 0.00062137;

    public StationListAdapter(Context context, ArrayList<GasStation> data)
    {
        super(context, R.layout.station_row, data);
    }


    public View getView(int position, View convertView, ViewGroup parent)
    {
        final GasStation station = getItem(position);

        if(convertView == null)
        {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.station_row,parent, false);
        }

        Button button = convertView.findViewById(R.id.navigate_button);
        button.setOnClickListener(this);
        button.setTag(position);

        TextView stationNameTextView = convertView.findViewById(R.id.row_station_name);
        stationNameTextView.setText(station.getName());

        TextView distanceTextView = convertView.findViewById(R.id.row_station_distance);
        double dist = METER_TO_MILE * station.getDistance();
        String distString = String.format(" %.1fmi",dist);
        distanceTextView.setText(distString);

        return convertView;
    }

    @Override
    public void onClick(View v)
    {
        int pos = (int) v.getTag();
        GasStation station = getItem(pos);
        LatLng loc = station.getLocation();

        Log.v("LISTVIEW", "Clicked: " + station.getName());


        String intentString = "";
        if (station.getAddress() != null)
            intentString = String.format(getContext().getString(R.string.mapsIntent_Address), loc.latitude, loc.longitude, Uri.encode(station.getAddress()));
        else
            intentString = String.format(getContext().getString(R.string.mapsIntent_LatLng), loc.latitude, loc.longitude);

        // Create a Uri from an intent string. Use the result to create an Intent.
        Uri gmmIntentUri = Uri.parse(intentString);

        // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Make the Intent explicit by setting the Google Maps package
        mapIntent.setPackage("com.google.android.apps.maps");

        // Attempt to start an activity that can handle the Intent
        getContext().startActivity(mapIntent);
    }
}
