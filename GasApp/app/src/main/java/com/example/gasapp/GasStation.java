package com.example.gasapp;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

public class GasStation
{
    private LatLng location;
    private String name;
    private float distance = Float.POSITIVE_INFINITY;
    private String address;

    public GasStation(String stationName,LatLng stationLocation,Location currentLocation,String currentAddress)
    {
        name = stationName;
        location = stationLocation;
        address = currentAddress;
        if(currentLocation != null && location != null)
        {
            float[] dist = new float[1];
            Location.distanceBetween(currentLocation.getLatitude(),currentLocation.getLongitude(),
                    location.latitude,location.longitude,dist);
            distance = dist[0];
        }
    }

    public String getName()
    {
        return name;
    }


    public LatLng getLocation()
    {
        return location;
    }

    public String getAddress()
    {
        return address;
    }
}
