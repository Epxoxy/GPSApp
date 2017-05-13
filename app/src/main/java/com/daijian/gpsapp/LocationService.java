package com.daijian.gpsapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.RequiresApi;

public class LocationService extends Service {
    public static final String ACTION_DISTANCE_UPDATE = "action.distance.update";
    public static final String UPDATED_DISTANCE = "updated.distance";
    private static final int LOCATION_INTERVAL = 1000;
    private static final float MIN_DISTANCE = 1f;
    private LocationManager locMgr = null;
    private LocationListener[] locListeners;

    public LocationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onCreate(){
        //Get location service
        locMgr = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        locListeners = new LocationListener[] {
                new LocationListener(LocationManager.GPS_PROVIDER),
                new LocationListener(LocationManager.NETWORK_PROVIDER)
        };
        //Check if network is enabled.
        try {
            boolean network_enabled;
            network_enabled = locMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            Messenger.logln("isProviderEnabled, " +  network_enabled);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        //Request location updates of network provider
        try {
            locMgr.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, MIN_DISTANCE,
                    locListeners[1]);
            Location lastKnow = locMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if(lastKnow != null)
                locListeners[1].lastLocation = lastKnow;
        } catch (java.lang.SecurityException ex) {
            Messenger.logln("fail to request location update, " +  ex);
        } catch (IllegalArgumentException ex) {
            Messenger.logln("network provider does not exist, " + ex.getMessage());
        }
        //Request location updates of GPS provider
        try {
            locMgr.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, MIN_DISTANCE,
                    locListeners[0]);
            Location lastKnow = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(lastKnow != null)
                locListeners[0].lastLocation = lastKnow;
        } catch (java.lang.SecurityException ex) {
            Messenger.logln("fail to request location update, ignore" +  ex);
        } catch (IllegalArgumentException ex) {
            Messenger.logln("gps provider does not exist " + ex.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Messenger.logln("onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        Messenger.logln("onDestroy");
        super.onDestroy();
        if (locMgr != null && locListeners != null) {
            for (LocationListener locationListener : locListeners) {
                try {
                    locMgr.removeUpdates(locationListener);
                } catch (Exception ex) {
                    Messenger.logln("fail to remove location listners, ignore" + ex);
                }
            }
        }
        locMgr = null;
    }

    private class LocationListener implements  android.location.LocationListener{
        private Location lastLocation;
        private String provider;

        LocationListener(String provider){
            Messenger.logln("Create LocationListener : " + provider);
            this.provider = provider;
        }

        @Override
        public void onLocationChanged(Location location) {
            Messenger.logln("onLocationChanged : " + location.getLongitude() + "," + location.getLatitude());
            if(lastLocation == null){
                lastLocation = new Location(provider);
            }else{
                checkDistance(location);
            }
            lastLocation.set(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Messenger.logln("onStatusChanged : " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Messenger.logln("onProviderEnabled : " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Messenger.logln("onProviderDisabled : " + provider);
        }

        private void checkDistance(Location location){
            if(location != null && lastLocation != null){
                float[] result = new float[1];
                Location.distanceBetween(
                        lastLocation.getLatitude(),
                        lastLocation.getLongitude(),
                        location.getLatitude(),
                        location.getLongitude(),
                        result);
                float distance = result[0] / 1000;//km
                Intent intent = new Intent();
                intent.setAction(ACTION_DISTANCE_UPDATE);
                intent.putExtra(UPDATED_DISTANCE, distance);
                sendBroadcast(intent);
            }
        }
    }

}
