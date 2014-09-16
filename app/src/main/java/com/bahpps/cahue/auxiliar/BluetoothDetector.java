package com.bahpps.cahue.auxiliar;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.util.Log;

import com.bahpps.cahue.R;
import com.bahpps.cahue.location.LocationPoller;

import java.util.GregorianCalendar;

public class BluetoothDetector extends BroadcastReceiver {

    public static final String PREF_BT_DEVICE_ADDRESS = "PREF_BT_DEVICE_ADDRESS";
    public static final String PREF_BT_CONNECTION_TIME = "PREF_BT_CONNECTION_TIME";
    public static final String PREF_BT_DISCONNECTION_TIME = "PREF_BT_DISCONNECTION_TIME";


    SharedPreferences prefs;
    BluetoothDevice device;

    /**
     * This receiver is in charge of detecting BT disconnection, as declared on the manifest
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        Log.d("Bluetooth", "Bluetooth: " + intent.getAction());
        Log.d("Bluetooth", device.getName() + " " + device.getAddress());

        // we need to get which BT device the user chose as the one of his car
        prefs = Util.getSharedPreferences(context);
        String storedAddress = prefs.getString(PREF_BT_DEVICE_ADDRESS, "");

        // If the device we just disconnected from is our chosen one
        if (device.getAddress().equals(storedAddress)) {

            Log.d("Bluetooth", "storedAddress matched: " + storedAddress);

            if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                onBtDisconnected(context);
            } else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                onBtConnected(context);
            }
        }
    }

    private void onBtConnected(Context context) {

        // we store the time the bt device was disconnected
        prefs.edit().putLong(PREF_BT_CONNECTION_TIME, (new GregorianCalendar().getTimeInMillis())).apply();

        Intent i = new Intent(context, CarLocationManager.class);
        context.sendBroadcast(i);

    }

    private void onBtDisconnected(Context context) {

        // we store the time the bt device was disconnected
        prefs.edit().putLong(PREF_BT_DISCONNECTION_TIME, (new GregorianCalendar().getTimeInMillis())).apply();


        // we create an intent to start the location poller service, as declared in manifest
        Intent i = new Intent(context, LocationPoller.class);
        i.putExtra(LocationPoller.EXTRA_INTENT, new Intent(context.getString(R.string.intent_car_parked)));
        i.putExtra(LocationPoller.EXTRA_PROVIDER, LocationManager.GPS_PROVIDER);
        context.sendBroadcast(i);

        // we send it twice, for gps and network provider
        i.putExtra(LocationPoller.EXTRA_PROVIDER, LocationManager.NETWORK_PROVIDER);
        context.sendBroadcast(i);

    }
}
