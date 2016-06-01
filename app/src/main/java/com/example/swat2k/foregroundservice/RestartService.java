package com.example.swat2k.foregroundservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RestartService extends BroadcastReceiver {

    static final String ACTION_RESTART_PERSISTENT_SERVICE = "ACTION_RESTART_PERSISTENT_SERVICE";

    public RestartService() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_RESTART_PERSISTENT_SERVICE)) {
            Intent serviceIntent = new Intent(context, GeoLocationService.class);
            context.startService(serviceIntent);
        }
    }
}
