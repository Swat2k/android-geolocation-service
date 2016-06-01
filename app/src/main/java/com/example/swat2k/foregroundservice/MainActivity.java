package com.example.swat2k.foregroundservice;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i("Test", "Activity: onCreate");
        Intent serviceIntent = new Intent(this, GeoLocationService.class);
        startService(serviceIntent);
    }
}
