package com.cahue.iweco.debug;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.cahue.iweco.cars.Car;
import com.cahue.iweco.locationServices.ParkedCarService;

/**
* Created by Francesco on 17/10/2014.
*/
public class DebugParkedCarService extends ParkedCarService {

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    ServiceListener serviceListener;

    @Override
    public void onFirstPreciseFixPolled(Context context, Location spotLocation, Car car) {
        car = new Car();
        super.onFirstPreciseFixPolled(context, spotLocation, car);
        serviceListener.onNewLocation(spotLocation);
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        DebugParkedCarService getService() {
            // Return this instance of LocalService so clients can call public methods
            return DebugParkedCarService.this;
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


    public interface ServiceListener{
        public void onNewLocation(Location location);
    }
}
