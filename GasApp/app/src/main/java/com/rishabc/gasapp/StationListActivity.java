package com.rishabc.gasapp;

import android.os.Bundle;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class StationListActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_list);

        ListView listView = findViewById(R.id.list_view);
        if(MapsActivity.stationListAdapter != null)
            listView.setAdapter(MapsActivity.stationListAdapter);
    }

    @Override
    public void onBackPressed()
    {
        finish();
    }
}
