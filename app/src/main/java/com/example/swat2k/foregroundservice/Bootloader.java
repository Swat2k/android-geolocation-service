package com.example.swat2k.foregroundservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Bootloader extends BroadcastReceiver {
    public Bootloader() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, GeoLocationService.class);
        context.startService(serviceIntent);
    }
}
