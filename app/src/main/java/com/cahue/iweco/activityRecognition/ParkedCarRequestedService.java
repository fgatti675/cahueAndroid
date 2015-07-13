package com.cahue.iweco.activityRecognition;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.cahue.iweco.Constants;
import com.cahue.iweco.MapsActivity;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.locationServices.LocationPollerService;
import com.cahue.iweco.util.FetchAddressIntentService;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.Date;
import java.util.List;

/**
 * This service fetches the current location and ask the user if the car was set there.
 * It is started from the activity recognition service, when it detects that the user steps out of
 * a vehicle.
 *
 * When the location is retrieved the user gets a notification asking him whan car he has parked.
 *
 * @author Francesco
 */
public class ParkedCarRequestedService extends LocationPollerService {

    public static final int NOTIFICATION_ID = 875644;

    private final static String TAG = ParkedCarRequestedService.class.getSimpleName();

    private CarDatabase carDatabase;

    private Location location;

    private Date time;

    @Override
    protected boolean checkPreconditions(Car car) {
        return true;
    }

    @Override
    public void onPreciseFixPolled(Context context, Location location, Car car, Date startTime, GoogleApiClient googleApiClient) {
        this.location = location;
        this.time = startTime;

        Log.i(TAG, "Received : " + location);

        carDatabase = CarDatabase.getInstance(context);

        /**
         * Fetch address
         */
        Intent fetchAddressIntent = new Intent(context, FetchAddressIntentService.class);
        fetchAddressIntent.putExtra(FetchAddressIntentService.RECEIVER, new AddressResultReceiver());
        fetchAddressIntent.putExtra(FetchAddressIntentService.LOCATION_DATA_EXTRA, location);
        context.startService(fetchAddressIntent);

    }


    /**
     * When the address is fetched, we display a notification asking the user to save the location,
     * assigning it to a car
     *
     * @param address
     */
    private void onAddressFetched(String address) {

        long[] pattern = {0, 100, 1000};

        // Intent to start the activity and show a just parked dialog
        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra(Constants.INTENT_CAR_EXTRA_UPDATE_REQUEST, true);
        intent.putExtra(Constants.INTENT_CAR_EXTRA_LOCATION, location);
        intent.putExtra(Constants.INTENT_CAR_EXTRA_ADDRESS, address);
        intent.putExtra(Constants.INTENT_CAR_EXTRA_TIME, time.getTime());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 345345, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManagerCompat mNotifyMgr = NotificationManagerCompat.from(this);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setVibrate(pattern)
                        .setContentIntent(pendingIntent)
                        .setColor(getResources().getColor(R.color.theme_primary))
                        .setSmallIcon(R.drawable.ic_car_white_48dp)
                        .setContentTitle(getString(R.string.ask_just_parked))
                        .setContentText(address);

        List<Car> cars = carDatabase.retrieveCars(false);
        int numberActions = Math.min(cars.size(), 3);
        for (int i = 0; i < numberActions; i++) {
            Car car = cars.get(i);
            NotificationCompat.Action saveAction = createCarSaveAction(car, address, i);
            mBuilder.addAction(saveAction);
        }

        mNotifyMgr.notify(null, NOTIFICATION_ID, mBuilder.build());
    }


    private NotificationCompat.Action createCarSaveAction(Car car, String address, int index) {

        Intent intent = new Intent(Constants.INTENT_SAVE_CAR_REQUEST + "." + index);
        intent.putExtra(Constants.INTENT_CAR_EXTRA_ID, car.id);
        intent.putExtra(Constants.INTENT_CAR_EXTRA_LOCATION, location);
        intent.putExtra(Constants.INTENT_CAR_EXTRA_ADDRESS, address);
        intent.putExtra(Constants.INTENT_CAR_EXTRA_TIME, System.currentTimeMillis());

        PendingIntent pIntent = PendingIntent.getBroadcast(this, 782982, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Action(R.drawable.ic_car_white_24dp, car.name, pIntent);
    }


    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver() {
            super(new Handler());
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            if (resultCode != FetchAddressIntentService.SUCCESS_RESULT)
                return;

            String address = resultData.getString(FetchAddressIntentService.RESULT_DATA_KEY);

            onAddressFetched(address);

        }
    }

}