package it.gufoe.myapplication;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import static android.database.sqlite.SQLiteDatabase.openDatabase;

public class LocationService extends Service {
    private static final String TAG = "BOOMBOOMTESTGPS";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 10f;
    private WifiManager mWifi;
    SQLiteDatabase db;

    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        Log.e(TAG, "onCreate");
        db = openDatabase(getDatabasePath("datalogger.sqlite").getPath(), null, 0);
        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            if (Utils.settings(getApplicationContext()).getBoolean("track_gps", true))
                mLocationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                        mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
    }

    @Override
    public void onDestroy()
    {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
        startService(new Intent(this, this.getClass()));
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        public LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);

            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);

            boolean track_wifi = Utils.settings(getApplicationContext()).getBoolean("track_wifi", true);
            boolean track_reti = Utils.settings(getApplicationContext()).getBoolean("track_reti", true);

            Log.e(TAG, track_reti?"reti si":"reti no");
            Log.e(TAG, track_wifi?"wifi si":"wifi no");

            if (track_wifi)
                logWifi();
            if (track_reti)
                logCellular();
        }

        private String getTime() {
            java.util.Date dt = new java.util.Date();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(dt);
        }

        private void logWifi() {
            mWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            if (!mWifi.isWifiEnabled()) {
                return;
            }

            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent intent) {
                    List<ScanResult> results = mWifi.getScanResults();
                    int free = 0;
                    for (ScanResult sr : results) {
                        String cap = sr.capabilities;
                        if (!cap.contains("WPA") && !cap.contains("WEP"))
                            free++;
                    }

                    if (Utils.settings(getApplicationContext()).getBoolean("realtime_log", true))
                        Toast.makeText(getApplicationContext(), "Reti wifi " + free + " libere su " + results.size(), Toast.LENGTH_SHORT).show();

                    String sql = "INSERT INTO data (time, type, lat, lon, signal) VALUES (?,?,?,?,?)";
                    db.execSQL(sql, new Object[]{
                            getTime(),
                            "wifi",
                            mLastLocation.getLatitude(),
                            mLastLocation.getLongitude(),
                            results.size(),
                    });
                    db.execSQL(sql, new Object[]{
                            getTime(),
                            "free-wifi",
                            mLastLocation.getLatitude(),
                            mLastLocation.getLongitude(),
                            free
                    });
                }
            }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        }

        void logCellular() {
            TelephonyManager tm = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);

            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            List<CellInfo> a = tm.getAllCellInfo();
            String sql = "INSERT INTO data (time, type, lat, lon, signal) VALUES (?,?,?,?,?)";

            for (CellInfo ci : a) {
                String type = null;
                int signal = 0;

                try {
                    signal = ((CellInfoLte) ci).getCellSignalStrength().getLevel();
                    type = "lte";
                } catch (Exception e) {}
                try {
                    signal = ((CellInfoGsm) ci).getCellSignalStrength().getLevel();
                    type = "gsm";
                } catch (Exception e) {}
                try {
                    signal = ((CellInfoWcdma) ci).getCellSignalStrength().getLevel();
                    type = "wcdma";
                } catch (Exception e) {}
                try {
                    signal = ((CellInfoCdma) ci).getCellSignalStrength().getLevel();
                    type = "cdma";
                } catch (Exception e) {}

                if (type != null) {
                    if (Utils.settings(getApplicationContext()).getBoolean("realtime_log", true))
                        Toast.makeText(getApplicationContext(), "Rete " + type + ": " + signal + "/5", Toast.LENGTH_SHORT).show();
                    db.execSQL(sql, new Object[] {
                            getTime(),
                            type,
                            mLastLocation.getLatitude(),
                            mLastLocation.getLongitude(),
                            signal
                    });
                }
            }
        }

        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

}
