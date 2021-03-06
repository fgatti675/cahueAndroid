package com.whereismycar.locationservices;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.whereismycar.BuildConfig;
import com.whereismycar.R;
import com.whereismycar.cars.database.CarDatabase;
import com.whereismycar.spots.ParkingSpotSender;
import com.whereismycar.util.Tracking;
import com.whereismycar.util.Util;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;

import java.util.Collections;
import java.util.Date;

/**
 * This class is in charge of uploading the location of the car to the server when BT connects
 * and the car starts moving.
 *
 * @author Francesco
 */
public class CarMovedService extends AbstractLocationUpdatesService {

    public static final String ACTION = BuildConfig.APPLICATION_ID + ".CAR_MOVED_ACTION";

    private final static String TAG = CarMovedService.class.getSimpleName();

    private void clearGeofence(Context context, @NonNull final String carId) {
        GeofencingClient mGeofencingClient = LocationServices.getGeofencingClient(context);

        // remove the geofence we just entered
        mGeofencingClient.removeGeofences(
                Collections.singletonList(carId)
        );
        Log.d(TAG, "Geofence removed");
    }

    @Override
    protected void onPreciseFixPolled(Location location, String carId, Date startTime) {

        CarDatabase carDatabase = CarDatabase.getInstance();

        Util.showToast(this, R.string.thanks_free_spot, Toast.LENGTH_SHORT);

        carDatabase.removeCarLocation(carId);
        clearGeofence(this, carId);

        Tracking.sendEvent(Tracking.CATEGORY_PARKING, Tracking.ACTION_BLUETOOTH_FREED_SPOT);

        ParkingSpotSender.fetchAddressAndSave(this, location, startTime, carId, false);

    }
}
