package com.example.gasapp;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class StationInfoWindowAdapter implements GoogleMap.InfoWindowAdapter
{
    private LayoutInflater layoutInflater;
    final private double  METER_TO_MILE = 0.00062137;

    public StationInfoWindowAdapter(LayoutInflater inflater)
    {
        super();
        layoutInflater = inflater;
    }

    @Override
    public View getInfoWindow(Marker marker)
    {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker)
    {

        View view = layoutInflater.inflate(R.layout.station_marker_info,null);

        TextView nameView = (TextView) view.findViewById(R.id.station_name);
        nameView.setText(marker.getTitle());

        GasStation station = (GasStation) marker.getTag();

        TextView distanceView = (TextView) view.findViewById(R.id.station_distance);
        double dist = METER_TO_MILE * station.getDistance();
        String distString = String.format(" %.1fmi",dist);
        distanceView.setText(distString);

        return view;
    }
}
