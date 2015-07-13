package com.cahue.iweco.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.cahue.iweco.Constants;
import com.cahue.iweco.activityRecognition.ActivityRecognitionService;
import com.cahue.iweco.locationServices.CarMovedService;
import com.cahue.iweco.parkedCar.ParkedCarService;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.database.CarDatabase;

import java.util.Set;

public class BluetoothDetector extends BroadcastReceiver {


    /**
     * This receiver is in charge of detecting BT disconnection or connection, as declared on the manifest
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            if(state == BluetoothAdapter.STATE_ON) {
                onBtTurnedOn(context);
            } else if(state == BluetoothAdapter.STATE_OFF) {
                onBtTurnedOff(context);
            }
        } else {

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            String address = device.getAddress();
            String name = device.getName();

            Log.d("Bluetooth", "Bluetooth: " + intent.getAction());
            Log.d("Bluetooth", device.getName() + " " + address);

            // we need to get which BT device the user chose as the one of his car

            CarDatabase carDatabase = CarDatabase.getInstance(context);
            Set<String> storedAddress = carDatabase.getPairedBTAddresses();

            // If the device we just disconnected from is our chosen one
            if (storedAddress.contains(address)) {

                Log.d("Bluetooth", "storedAddress matched: " + storedAddress);

                Car car = carDatabase.findByBTAddress(address);

                if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                    onBtDisconnected(context, car);
                } else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                    onBtConnected(context, car);
                }
            }
        }
    }

    private void onBtTurnedOn(Context context) {
        /**
         * Stop activity recognition
         */
        Intent intent = new Intent(context, ActivityRecognitionService.class);
        intent.setAction(Constants.ACTION_STOP_ACTIVITY_RECOGNITION);
        context.startService(intent);
    }

    private void onBtTurnedOff(Context context) {
        /**
         * Start activity recognition
         */

        ActivityRecognitionService.startIfNecessary(context);
    }

    public void onBtConnected(Context context, Car car) {

        Log.d("Bluetooth", "onBtConnected");

        // we create an intent to start the location poller service, as declared in manifest
        Intent intent = new Intent();
        intent.setClass(context, CarMovedService.class);
        intent.putExtra(Constants.INTENT_CAR_EXTRA_ID, car.id);
        context.startService(intent);

    }

    public void onBtDisconnected(Context context, Car car) {

        Log.d("Bluetooth", "onBtDisconnected");

        // we create an intent to start the location poller service, as declared in manifest
        Intent intent = new Intent();
        intent.setClass(context, ParkedCarService.class);
        intent.putExtra(Constants.INTENT_CAR_EXTRA_ID, car.id);
        context.startService(intent);

    }
}
