package com.bahpps.cahue.locationServices;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.bahpps.cahue.parkedCar.Car;
import com.bahpps.cahue.parkedCar.CarDatabase;
import com.bahpps.cahue.parkedCar.CarManager;

import java.util.Date;

/**
 * This class is in charge of receiving location updates, after and store it as the cars position.
 * Triggered when BT is disconnected
 *
 * @author Francesco
 */
public class ParkedCarService extends LocationPollerService {

    private final static String TAG = "ParkedCarService";

    @Override
    protected boolean checkPreconditions(Car car) {
        return true;
    }

    @Override
    public void onLocationPolled(Context context, Location location, Car car) {
        Log.i(TAG, "Received : " + location);
        car.location = location;
        car.time = new Date();

        CarDatabase carDatabase = new CarDatabase(context);
        carDatabase.saveCar(car);
    }


}
