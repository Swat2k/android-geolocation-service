package com.example.swat2k.foregroundservice;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

public class GeoLocationService extends Service {

    private ServiceHandler serviceHandler;
    private GoogleApiLocationProvider googleApiLocationProvider;
    private Notification.Builder notificationBuilder;

    public GeoLocationService() {
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                googleApiLocationProvider.Start();
                Thread.sleep(Environment.TimeToRefresh);
                sendEmptyMessage(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, START_STICKY, startId);
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        registerRestartAlarm();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Intent intent = new Intent(this,this.getClass());
        startService(intent);
    }

    @Override
    public void onCreate() {
        Log.i("Test", "Service: onCreate");

        notificationBuilder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.NotifyTitle))
                .setContentText(getString(R.string.NotifyText));
        Notification notification;
        if (Build.VERSION.SDK_INT < 16)
            notification = notificationBuilder.getNotification();
        else
            notification = notificationBuilder.build();
        startForeground(777, notification);

        googleApiLocationProvider = new GoogleApiLocationProvider(this, notificationBuilder);
        HandlerThread thread = new HandlerThread("ServiceStartArguments", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        Looper mServiceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(mServiceLooper);

        unregisterRestartAlarm();
        super.onCreate();
    }

    void registerRestartAlarm() {
        Intent intent = new Intent(GeoLocationService.this, RestartService.class);
        intent.setAction(RestartService.ACTION_RESTART_PERSISTENT_SERVICE);
        PendingIntent sender = PendingIntent.getBroadcast(GeoLocationService.this, 0, intent, 0);
        long firstTime = SystemClock.elapsedRealtime();
        firstTime += 1 * 1000;
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,firstTime, 10 * 1000, sender);
    }

    void unregisterRestartAlarm() {
        Intent intent = new Intent(GeoLocationService.this, RestartService.class);
        intent.setAction(RestartService.ACTION_RESTART_PERSISTENT_SERVICE);
        PendingIntent sender = PendingIntent.getBroadcast(GeoLocationService.this, 0, intent, 0);
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        am.cancel(sender);
    }
}
