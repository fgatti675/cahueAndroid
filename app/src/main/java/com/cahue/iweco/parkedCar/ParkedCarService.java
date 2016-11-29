package com.cahue.iweco.parkedcar;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.widget.Toast;

import com.cahue.iweco.Constants;
import com.cahue.iweco.MapsActivity;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.locationservices.GeofenceCarService;
import com.cahue.iweco.locationservices.LocationPollerService;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.util.FetchAddressIntentService;
import com.cahue.iweco.util.PreferencesUtil;
import com.cahue.iweco.util.Util;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.Date;

import static android.content.Intent.ACTION_VIEW;
import static android.support.v4.app.NotificationCompat.CATEGORY_SERVICE;
import static android.support.v4.app.NotificationCompat.PRIORITY_MIN;

/**
 * This class is in charge of receiving a location fix when the user parks his car.
 * Triggered when BT is disconnected
 *
 * @author Francesco
 */
public class ParkedCarService extends LocationPollerService {

    public static final int NOTIFICATION_ID = 4833;

    private final static String TAG = ParkedCarService.class.getSimpleName();

    private CarDatabase carDatabase;
    private Car car;

    @Override
    public void onCreate() {
        super.onCreate();
        carDatabase = CarDatabase.getInstance();
    }

    @Override
    protected boolean checkPreconditions(Car car) {
        return true;
    }

    @Override
    public void onPreciseFixPolled(@NonNull Context context, @NonNull Location location, @NonNull Car car, Date startTime, GoogleApiClient googleApiClient) {

        this.car = car;

        Log.i(TAG, "Received : " + location);

        car.spotId = null;
        car.location = location;
        car.address = null;
        car.time = new Date();

        CarsSync.storeCar(carDatabase, context, car);

        /**
         * If the location of the car is good enough we can set a geofence afterwards.
         */
        if (car.location.getAccuracy() < Constants.ACCURACY_THRESHOLD_M) {
            GeofenceCarService.startDelayedGeofenceService(context, car.id);
        }

        fetchAddress(this, car);
    }

    private void notifyUser() {
        if (PreferencesUtil.isDisplayParkedNotificationEnabled(this)) {

            NotificationManagerCompat mNotifyMgr = NotificationManagerCompat.from(this);

            // Intent to start the activity and show a just parked dialog
            Intent intent = new Intent(this, MapsActivity.class);
            intent.setAction(ACTION_VIEW);
            intent.putExtra(Constants.EXTRA_CAR_ID, car.id);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 79243, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            CharSequence contentText;
            if (car.name != null) {
                Spannable sb = new SpannableString(car.name + " - " + car.address);
                sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, car.name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                contentText = sb;
            } else {
                contentText = car.address;
            }

            long[] pattern = {0, 100, 1000};
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setVibrate(pattern)
                            .setContentIntent(pendingIntent)
                            .setColor(getResources().getColor(R.color.theme_primary))
                            .setSmallIcon(R.drawable.ic_car_white_48dp)
                            .setContentTitle(getString(R.string.just_parked))
                            .setCategory(CATEGORY_SERVICE)
                            .setPriority(PRIORITY_MIN)
                            .setContentText(contentText);


            mNotifyMgr.notify(car.id, NOTIFICATION_ID, mBuilder.build());

        } else {
            Util.showBlueToastWithLogo(ParkedCarService.this, getString(R.string.car_location_registered, car.name), Toast.LENGTH_SHORT);
        }
    }

    private void fetchAddress(@NonNull Context context, @NonNull Car car) {
        Log.d(TAG, "Fetching address");
        Intent fetchAddressIntent = new Intent(context, FetchAddressIntentService.class);
        fetchAddressIntent.putExtra(FetchAddressIntentService.RECEIVER, new ParkedCarService.AddressResultReceiver());
        fetchAddressIntent.putExtra(FetchAddressIntentService.LOCATION_DATA_EXTRA, car.location);
        context.startService(fetchAddressIntent);
    }

    @SuppressLint("ParcelCreator")
    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver() {
            super(new Handler());
        }

        @Override
        protected void onReceiveResult(int resultCode, @NonNull Bundle resultData) {

            if (resultCode != FetchAddressIntentService.SUCCESS_RESULT)
                return;

            // Display the address string
            // or an error message sent from the intent service.
            car.address = resultData.getString(FetchAddressIntentService.RESULT_DATA_KEY);
            carDatabase.updateAddress(ParkedCarService.this, car);

            Log.d(TAG, "Sending car update broadcast");

            Intent intent = new Intent(Constants.INTENT_ADDRESS_UPDATE);
            intent.putExtra(Constants.EXTRA_CAR_ID, car.id);
            intent.putExtra(Constants.EXTRA_CAR_ADDRESS, car.address);
            sendBroadcast(intent);

            notifyUser();

        }
    }


}
