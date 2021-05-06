package com.rishabc.gasapp;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

public class GasStation implements Parcelable
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

    public float getDistance() { return distance; }

    public GasStation(Parcel in) {
        location = in.readParcelable(LatLng.class.getClassLoader());
        name = in.readString();
        distance = in.readFloat();
        address = in.readString();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeParcelable(location,flags);
        dest.writeString(name);
        dest.writeFloat(distance);
        dest.writeString(address);
    }

    public static final Creator<GasStation> CREATOR = new Creator<GasStation>() {
        @Override
        public GasStation createFromParcel(Parcel in) {
            return new GasStation(in);
        }

        @Override
        public GasStation[] newArray(int size) {
            return new GasStation[size];
        }
    };
}
