package com.whereismycar.locationservices;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.ResultReceiver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.whereismycar.BuildConfig;
import com.whereismycar.Constants;
import com.whereismycar.R;
import com.whereismycar.model.Car;
import com.whereismycar.spots.ParkingSpotSender;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.Date;

import static com.whereismycar.util.NotificationChannelsUtils.DEBUG_CHANNEL_ID;

/**
 * This service is in charge of detecting if the user is far away enough after parking, and if so,
 * setting a geofence around it.
 */
public class GeofenceCarService extends AbstractLocationUpdatesService {

    public static final String ACTION = BuildConfig.APPLICATION_ID + ".GEOFENCE_ACTIVATED_ACTION";

    private static final String TAG = GeofenceCarService.class.getSimpleName();

    private PendingIntent mGeofencePendingIntent;

    /**
     * x
     * Start the GeoFence service after 2 minutes
     *
     * @param context
     * @param carId
     */
    public static void startDelayedGeofenceService(@NonNull Context context, String carId) {

        if (carId == null) return;

        PersistableBundle bundle = new PersistableBundle();
        bundle.putString(Constants.EXTRA_CAR_ID, carId);

        ComponentName serviceComponent = new ComponentName(context, DelayedGeofenceJob.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        builder.setMinimumLatency(2 * 60 * 1000); // wait at least
        builder.setExtras(bundle);
        //builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED); // require unmetered network
        //builder.setRequiresDeviceIdle(true); // device should be idle
        //builder.setRequiresCharging(false); // we don't care if the device is charging or not
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());

        Log.i(TAG, "Starting delayed geofence service");
    }

    @Override
    protected void onPreciseFixPolled(Location location, String carId, Date startTime) {

        if (location == null) return;

        DocumentReference documentReference = FirebaseFirestore.getInstance().collection("cars").document(carId);
        documentReference.get().addOnSuccessListener(documentSnapshot -> {
            Car car = Car.fromFirestore(documentSnapshot);

            if (car.location == null) return;

            if (car.location.getAccuracy() > Constants.ACCURACY_THRESHOLD_M
                    || location.getAccuracy() > Constants.ACCURACY_THRESHOLD_M) {
                String msg = "Geofence not added because accuracy is not good enough: Car " + car.location.getAccuracy() + " / User " + location.getAccuracy();
                Log.d(TAG, msg);
                if (BuildConfig.DEBUG) {
                    notifyGeofenceError(this, car, msg);
                }
                return;
            }

            float distanceTo = car.location.distanceTo(location);
            if (distanceTo < Constants.PARKED_DISTANCE_THRESHOLD) {
                String msg = "GF Error: Too close to car: " + distanceTo;
                Log.d(TAG, msg);
                if (BuildConfig.DEBUG) {
                    notifyGeofenceError(this, car, msg);
                }
                return;
            }

            addGeofence(this, car);

        });


    }

    private void addGeofence(final Context context, @NonNull final Car car) {

        if (car == null) return;

        /**
         * Create a geofence around the car
         */
        Geofence geofence = new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this geofence.
                .setRequestId(car.id)
                .setCircularRegion(
                        car.location.getLatitude(),
                        car.location.getLongitude(),
                        Constants.GEOFENCE_RADIUS_IN_METERS
                )
                .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(2000)
                .build();

        final GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        GeofencingClient mGeofencingClient = LocationServices.getGeofencingClient(context);
        mGeofencingClient.removeGeofences(Arrays.asList(car.id));

        try {
            mGeofencingClient.addGeofences(
                    geofencingRequest,
                    getGeofencePendingIntent(context, car)
            );
        } catch (SecurityException s) {
        }

        if (BuildConfig.DEBUG) {
            notifyGeofenceAdded(context, car);
        }


        Log.i(TAG, "Geofence added");
    }

    private PendingIntent getGeofencePendingIntent(final Context context, @NonNull final Car car) {

        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }

        Intent intent = new Intent(context, GeofenceTransitionsIntentService.class);
        intent.putExtra(Constants.EXTRA_CAR_ID, car.id);
        intent.putExtra(GeofenceTransitionsIntentService.RECEIVER, new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, @NonNull Bundle resultData) {

                Log.d(TAG, "Geofence result received");

                if (resultCode == GeofenceTransitionsIntentService.SUCCESS_RESULT) {
                    Log.d(TAG, "Geofence result SUCCESS");
                    Location location = resultData.getParcelable(GeofenceTransitionsIntentService.GEOFENCE_TRIGGERING_LOCATION_KEY);

                    if (BuildConfig.DEBUG)
                        notifyApproachingCar(context, location, car);

                    ParkingSpotSender.fetchAddressAndSave(context, car.location, new Date(), car.id, true);
                }

                GeofencingClient mGeofencingClient = LocationServices.getGeofencingClient(context);

                // remove the geofence we just entered
                mGeofencingClient.removeGeofences(
                        mGeofencePendingIntent
                );

                Log.i(TAG, "Geofence removed");

            }
        });

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getService(context, car.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    private void notifyApproachingCar(Context context, @Nullable Location location, @NonNull Car car) {

        long[] pattern = {0, 1000, 200, 1000};
        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder mBuilder =
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(context, DEBUG_CHANNEL_ID) : new Notification.Builder(context))
                        .setVibrate(pattern)
                        .setSmallIcon(R.drawable.ic_car_white_48dp)
                        .setContentTitle("Approaching " + car.name);

        if (location != null)
            mBuilder.setContentText(String.format("Distance: %f meters", location.distanceTo(car.location)));

        int id = (int) (Math.random() * 10000);
        mNotifyMgr.notify("" + id, id, mBuilder.build());
    }

    private void notifyGeofenceError(Context context, @NonNull Car car, String error) {

        long[] pattern = {0, 110, 1000};
        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder mBuilder =
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(context, DEBUG_CHANNEL_ID) : new Notification.Builder(context))
                        .setVibrate(pattern)
                        .setSmallIcon(R.drawable.crosshairs_gps)
                        .setContentTitle("Geofence ERROR for " + car.name)
                        .setContentText(error);

        int id = (int) (Math.random() * 10000);
        mNotifyMgr.notify("" + id, id, mBuilder.build());
    }

    private void notifyGeofenceAdded(Context context, @NonNull Car car) {

        long[] pattern = {0, 1000, 200, 1000};
        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder mBuilder =
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(context, DEBUG_CHANNEL_ID) : new Notification.Builder(context))
                        .setVibrate(pattern)
                        .setSmallIcon(R.drawable.crosshairs_gps)
                        .setContentTitle("Geofence set for " + car.name);

        int id = (int) (Math.random() * 10000);
        mNotifyMgr.notify("" + id, id, mBuilder.build());
    }

}
