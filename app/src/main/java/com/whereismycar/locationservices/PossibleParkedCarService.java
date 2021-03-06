package com.whereismycar.locationservices;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import android.util.Log;

import com.whereismycar.BuildConfig;
import com.whereismycar.Constants;
import com.whereismycar.MapsActivity;
import com.whereismycar.R;
import com.whereismycar.activityrecognition.SaveCarRequestReceiver;
import com.whereismycar.cars.database.CarDatabase;
import com.whereismycar.model.Car;
import com.whereismycar.model.PossibleSpot;
import com.whereismycar.util.FetchAddressDelegate;
import com.whereismycar.util.PreferencesUtil;
import com.whereismycar.util.Tracking;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Date;
import java.util.List;

import static com.whereismycar.util.NotificationChannelsUtils.ACT_RECOG_CHANNEL_ID;

/**
 * This service fetches the current location and ask the user if the car was set there.
 * It is started from the activity recognition service, when it detects that the user steps out of
 * a vehicle.
 * <p>
 * When the location is retrieved the user g    ets a notification asking him whan car he has parked.
 *
 * @author Francesco
 */
public class PossibleParkedCarService extends AbstractLocationUpdatesService {

    public static final String ACTION = BuildConfig.APPLICATION_ID + ".POSSIBLE_PARKED_CAR_ACTION";

    public static final int NOTIFICATION_ID = 875644;

    private final static String TAG = PossibleParkedCarService.class.getSimpleName();

    private static final int ACCURACY_THRESHOLD_M = 50;

    private CarDatabase carDatabase;


    @Override
    protected void  onPreciseFixPolled(Location location, String carId, Date startTime) {

        Log.i(TAG, "Received : " + location);

        carDatabase = CarDatabase.getInstance();

        Log.d(TAG, "Fetching address");
        FetchAddressDelegate fetchAddressDelegate = new FetchAddressDelegate();
        fetchAddressDelegate.fetch(this, location, new FetchAddressDelegate.Callbacks() {
            @Override
            public void onAddressFetched(String address) {
                PossibleParkedCarService.this.onAddressFetched(location, address, startTime);
            }

            @Override
            public void onError(String error) {
            }
        });


        Tracking.sendEvent(Tracking.CATEGORY_PARKING, Tracking.ACTION_POSSIBLE_SPOT_DETECTED);

        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();
        bundle.putDouble("accuracy", location.getAccuracy());
        firebaseAnalytics.logEvent("ar_possible_spot_detected", bundle);
    }


    /**
     * When the address is fetched, we display a notification asking the user to save the location,
     * assigning it to a car
     *
     * @param address
     */
    private void onAddressFetched(Location location, String address, Date startTime) {

        PossibleSpot possibleParkingSpot = new PossibleSpot(location, address, startTime, false, PossibleSpot.RECENT);
        carDatabase.addPossibleParkingSpot(this, possibleParkingSpot);

        if (!PreferencesUtil.isMovementRecognitionNotificationEnabled(this))
            return;

        long[] pattern = {0, 100, 1000};

        // Intent to start the activity and show a just parked dialog
        Intent intent = new Intent(this, MapsActivity.class);
        intent.setAction(Constants.ACTION_POSSIBLE_PARKED_CAR);
        intent.putExtra(Constants.EXTRA_SPOT, possibleParkingSpot);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 345345, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManagerCompat mNotifyMgr = NotificationManagerCompat.from(this);
        Notification.Builder mBuilder =
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, ACT_RECOG_CHANNEL_ID) : new Notification.Builder(this))
                        .setVibrate(pattern)
                        .setContentIntent(pendingIntent)
                        .setColor(this.getResources().getColor(R.color.theme_primary))
                        .setSmallIcon(R.drawable.ic_car_white_48dp)
                        .setContentTitle(this.getString(R.string.ask_just_parked))
                        .setContentText(address);

        carDatabase.retrieveCars(new CarDatabase.CarsRetrieveListener() {
            @Override
            public void onCarsRetrieved(List<Car> cars) {
                int numberActions = Math.min(cars.size(), 3);
                for (int i = 0; i < numberActions; i++) {
                    Car car = cars.get(i);
                    Notification.Action saveAction = createCarSaveAction(PossibleParkedCarService.this, car, possibleParkingSpot, i);
                    mBuilder.addAction(saveAction);
                }
                mBuilder.setDeleteIntent(createDeleteIntent(PossibleParkedCarService.this));
                mNotifyMgr.notify(null, NOTIFICATION_ID, mBuilder.build());

//                FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(context);
//                Bundle bundle = new Bundle();
//                firebaseAnalytics.logEvent("ar_notification_displayed", bundle);
            }

            @Override
            public void onCarsRetrievedError() {

            }
        });
    }


    private PendingIntent createDeleteIntent(Context context) {
        Intent intent = new Intent();
        intent.setAction("com.whereismycar.DELETE_AR_NOTIFICATION");
        return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT
        );
    }

    @NonNull
    private Notification.Action createCarSaveAction(Context context, @NonNull Car car, PossibleSpot possibleSpot, int index) {

        Intent intent = new Intent(context, SaveCarRequestReceiver.class);
        intent.putExtra(Constants.EXTRA_CAR_ID, car.id);
        intent.putExtra(Constants.EXTRA_SPOT, possibleSpot);

        PendingIntent pIntent = PendingIntent.getBroadcast(context, 782982 + index, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        String name = car.isOther() ? context.getResources().getString(R.string.other) : car.name;
        return new Notification.Action(R.drawable.ic_car_white_24dp, name, pIntent);
    }



    public static class NotificationBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action == null || !action.equals("com.whereismycar.DELETE_AR_NOTIFICATION")) {
                return;
            }

            Tracking.sendEvent(Tracking.CATEGORY_NOTIFICATION_ACT_RECOG, Tracking.ACTION_NOTIFICATION_REMOVED);
            FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(context);
            Bundle bundle = new Bundle();
            firebaseAnalytics.logEvent("ar_notification_removed", bundle);
            Log.i(TAG, "Notification removed");
        }
    }
}
