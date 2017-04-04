package com.cahue.iweco.util;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.cahue.iweco.Constants;
import com.cahue.iweco.activityrecognition.ActivityRecognitionService;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.locationservices.CarMovedService;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.parkedcar.ParkedCarService;

import java.util.Set;

/**
 * This receiver is in charge of detecting BT disconnection or connection, as declared on the manifest
 */
public class BluetoothDetector extends BroadcastReceiver {

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {

        if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            if(state == BluetoothAdapter.STATE_ON) {
                onBtTurnedOn(context);
            } else if(state == BluetoothAdapter.STATE_OFF) {
                onBtTurnedOff(context);
            }
        } else if (intent.getAction().equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
                || intent.getAction().equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            String address = device.getAddress();
            String name = device.getName();

            Log.d("Bluetooth", "Bluetooth: " + intent.getAction());
            Log.d("Bluetooth", device.getName() + " " + address);

            // we need to get which BT device the user chose as the one of his car

            CarDatabase carDatabase = CarDatabase.getInstance();
            Set<String> storedAddress = carDatabase.getPairedBTAddresses(context);

            // If the device we just disconnected from is our chosen one
            if (storedAddress.contains(address)) {

                Log.d("Bluetooth", "storedAddress matched: " + storedAddress);

                Car car = carDatabase.findCarByBTAddress(context, address);
                int state = intent.getExtras().getInt(BluetoothProfile.EXTRA_STATE);
                if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    onBtDisconnectedFromCar(context, car);
                } else if (state == BluetoothProfile.STATE_CONNECTED) {
                    onBtConnectedToCar(context, car);
                }
            }
        }
    }

    private void onBtTurnedOn(Context context) {
    }

    private void onBtTurnedOff(Context context) {
        /**
         * Start activity recognition
         */
        ActivityRecognitionService.startIfEnabled(context);
    }

    private void onBtConnectedToCar(@NonNull Context context, @NonNull Car car) {

        /**
         * Stop activity recognition
         */
        ActivityRecognitionService.stop(context);

        Log.d("Bluetooth", "onBtConnectedToCar");

        // we create an intent to start the location poller service, as declared in manifest
        Intent intent = new Intent();
        intent.setClass(context, CarMovedService.class);
        intent.putExtra(Constants.EXTRA_CAR_ID, car.id);
        context.startService(intent);

    }

    private void onBtDisconnectedFromCar(@NonNull Context context, @NonNull Car car) {

        Log.d("Bluetooth", "onBtDisconnectedFromCar");

        // we create an intent to start the location poller service, as declared in manifest
        Intent intent = new Intent();
        intent.setClass(context, ParkedCarService.class);
        intent.putExtra(Constants.EXTRA_CAR_ID, car.id);
        context.startService(intent);

        /**
         * Start activity recognition if required
         */
        ActivityRecognitionService.startIfEnabled(context);
    }
}
