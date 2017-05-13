package com.daijian.gpsapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static Intent locService;
    private NotificationManager notificationMgr;
    //private static final int distanceNotifyId = 0;
    private static final int goalNotifyId = 1;
    private static final float shortGoal = 3f;
    private float totalDistance = 0f;
    private float toLastReachGoal = 0f;
    private boolean serviceStarted;
    //Views
    private TextView tvLoc;
    private TextView tvLog;
    private Button btnStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Find views
        tvLoc = (TextView)findViewById(R.id.tvLoc);
        tvLog = (TextView)findViewById(R.id.tvLog);
        btnStart = (Button)findViewById(R.id.btnStart);
        btnStart.setOnClickListener(ctlServiceClick);
        locService = new Intent(MainActivity.this, LocationService.class);
        //Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(LocationService.ACTION_DISTANCE_UPDATE);
        getApplicationContext().registerReceiver(distanceUpdateReceiver, filter);
        //Get notification service
        notificationMgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        Messenger.defaultOne.setLogListener(new Messenger.LogListener() {
            @Override
            public void onNewLogAppend(String text) {
                tvLog.append(text);
            }
        });
    }

    //Ensure permission granted
    private void ensurePermissionGranted() {
        if (!(isGranted(this, Manifest.permission.ACCESS_FINE_LOCATION)
                && isGranted(this,  Manifest.permission.ACCESS_COARSE_LOCATION))) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },0);//Zero : requestCode, useless so not special.
        }
    }
    private boolean isGranted(Context context, String permission){
        return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private View.OnClickListener ctlServiceClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(serviceStarted){
                stopService(locService);
                btnStart.setText(R.string.start);
                serviceStarted = false;
            }else{
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    ensurePermissionGranted();
                }
                //Check if system location service is opened
                LocationManager manager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
                if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    //Go to settings
                    Toast.makeText(MainActivity.this, "Please open gps.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }else{
                    //Start location service
                    startService(locService);
                    btnStart.setText(R.string.stop);
                    serviceStarted = true;
                }
            }
        }
    };

    //BroadcastReceiver for receiver distance update info
    public BroadcastReceiver distanceUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null){
                if(intent.getAction().equals(LocationService.ACTION_DISTANCE_UPDATE)){
                    Bundle bundle = intent.getExtras();
                    float distance = bundle.getFloat(LocationService.UPDATED_DISTANCE, 0f);
                    if(distance != 0){
                        totalDistance += distance;
                        toLastReachGoal += distance;
                        //Km -> m, distance * 1000
                        //Accurate to the second decimal place, Math.round(distance * 100) / 100f
                        float decimalTotal = Math.round(totalDistance * 100000) / 100f;
                        tvLoc.setText(String.valueOf(decimalTotal));
                        //Log option
                        Messenger.logln("Distance update : " + distance + "km");
                        Messenger.logln("Total distance : " + totalDistance + "km");
                        //Notify when user reach 3km
                        if(toLastReachGoal > shortGoal){
                            //But something need to solve is when the distance change is too large
                            Messenger.logln("To last reach : " + toLastReachGoal + "km");
                            toLastReachGoal -= shortGoal;
                            Messenger.logln("Reach " + shortGoal + "km.");
                            Toast.makeText(MainActivity.this, "Reach " + shortGoal + "km.", Toast.LENGTH_SHORT).show();
                            //Notify goal if in need
                            showNotification(goalNotifyId, "Congratulations", "Reach " + shortGoal + "km.");
                        }
                        //Notify if in need
                        //showNotification(distanceNotifyId, "Distance updated", decimalTotal + "m");
                    }
                }
            }
        }
    };

    //Show simple notification
    private void showNotification(int id, String title, String content){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            Intent intent = new Intent(this, MainActivity.class);
            //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
            Notification.Builder builder = new Notification.Builder(this);
            Notification notification = builder.setTicker("Ticker")
                    .setSmallIcon(R.drawable.notification_sicon)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(title)
                    .setContentText(content).setContentIntent(pendingIntent)
                    .setDefaults(Notification.DEFAULT_ALL).build();
            notificationMgr.notify(id, notification);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        getApplicationContext().unregisterReceiver(distanceUpdateReceiver);
        if(serviceStarted){
            stopService(locService);
            serviceStarted = false;
        }
    }
}
