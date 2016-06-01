package com.example.swat2k.foregroundservice;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;
import com.example.swat2k.foregroundservice.dto.Coordinate;
import com.example.swat2k.foregroundservice.rpc.CoordinateService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GoogleApiLocationProvider implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleApiClient       googleApiClient;
    private Location              location;
    private LocationRequest       locationRequest;
    private TelephonyManager      telephonyManager;
    private CoordinateService     coordinateService;
    private Notification.Builder  notificationBuilder;
    private NotificationManager   notificationManager;
    private String                deviceUuid;
    private Context               context;
    private PowerManager          powerManager;
    private PowerManager.WakeLock wakeLock;

    public GoogleApiLocationProvider(Context context, Notification.Builder mainNotificationBuilder){
        this.context = context;

        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // configure retrofit library
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://demo.it-serv.ru/mobileservice/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        coordinateService = retrofit.create(CoordinateService.class);
        telephonyManager = (TelephonyManager) this.context.getSystemService(context.TELEPHONY_SERVICE);
        deviceUuid = telephonyManager.getDeviceId();
        notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = mainNotificationBuilder;
        powerManager = (PowerManager) this.context.getSystemService(Context.POWER_SERVICE);
    }

    public void Start() {
        if (checkGooglePlayServices()) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
            wakeLock.acquire();
            googleApiClient.connect();
        }
    }

    public void Stop() {
        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
            wakeLock.release();
        }
    }

    private boolean checkPermission(){
        return ActivityCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    protected void startLocationUpdates() {
        // Create the location request
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY )
                .setInterval(Environment.UPDATE_INTERVAL)
                .setFastestInterval(Environment.FASTEST_INTERVAL);

        // Request location updates
        if (!checkPermission()) {
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,
                locationRequest, this);

        // Schedule a runnable to unregister location listeners
        Executors.newScheduledThreadPool(1).schedule(new Runnable() {
            @Override
            public void run() { Stop(); }
        }, Environment.TimeToRefresh, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (!checkPermission()) return;

        Log.i("LocationProvider", "event onConnected()!");

        location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if(location == null){
            Toast.makeText(context, "startLocationUpdates", Toast.LENGTH_SHORT).show();
            startLocationUpdates();
        }

        if (location != null) {
            SendGeoData(new Coordinate(location.getLatitude(), location.getLongitude(), deviceUuid));
        } else {
            Toast.makeText(context, "Location not Detected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i("LocationProvider", "event onLocationChanged()!");
        SendGeoData(new Coordinate(location.getLatitude(), location.getLongitude(), deviceUuid));
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i("LocationProvider", "Connection failed. Error: " + connectionResult.getErrorMessage());
    }

    private void SendGeoData(Coordinate coordinate) {
        Call<Void> call = coordinateService.Send(coordinate);

        call.enqueue(new retrofit2.Callback<Void>() {
                         @Override
                         public void onResponse(Call<Void> call, Response<Void> response) {
                             notificationBuilder.setContentText("Обновлено в " + getCurrentTime());
                             Notification notification;
                             if (Build.VERSION.SDK_INT < 16)
                                 notification = notificationBuilder.getNotification();
                             else
                                 notification = notificationBuilder.build();
                             notificationManager.notify(777, notification);
                             Stop();
                         }

                         @Override
                         public void onFailure(Call<Void> call, Throwable t) {
                             Stop();
                         }
                     }
        );
    }

    private boolean checkGooglePlayServices(){
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(context);
        return result == ConnectionResult.SUCCESS;
    }

    private String getCurrentTime() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss yyyy-MM-dd");
        return sdf.format(c.getTime());
    }

    private boolean isInteractiveMode() {
        boolean isInteractive;
        if (Build.VERSION.SDK_INT < 20)
            isInteractive = powerManager.isScreenOn();
        else
            isInteractive = powerManager.isInteractive();

        return isInteractive;
    }

}
