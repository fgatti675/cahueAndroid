package com.cahue.iweco.debug;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.locationServices.CarMovedService;
import com.cahue.iweco.cars.Car;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

/**
* Created by Francesco on 17/10/2014.
*/
public class DebugCarMovedService extends CarMovedService {

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    ServiceListener serviceListener;

    @Override
    public void onFirstPreciseFixPolled(Context context, Location spotLocation, Car car) {
        car = CarDatabase.getInstance(context).retrieveCars(false).iterator().next(); // TODO
        super.onFirstPreciseFixPolled(context, spotLocation, car);
        serviceListener.onNewLocation(spotLocation);
    }

    @Override
    public void onActivitiesDetected(Context context, List<DetectedActivity> detectedActivities, Location lastLocation, Car car) {

    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        DebugCarMovedService getService() {
            // Return this instance of LocalService so clients can call public methods
            return DebugCarMovedService.this;
        }
    }

    public void setServiceListener(ServiceListener serviceListener) {
        this.serviceListener = serviceListener;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("debug", "onBind");
        start();
        return mBinder;
    }

    @Override
    protected boolean checkPreconditions(Car car) {
        return true;
    }

    @Override
    public void onLocationPost(){
        serviceListener.onLocationPost();
    }

    public interface ServiceListener{
        public void onNewLocation(Location location);
        public void onLocationPost();
    }
}
