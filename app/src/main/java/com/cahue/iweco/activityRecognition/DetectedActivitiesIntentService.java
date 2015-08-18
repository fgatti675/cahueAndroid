/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cahue.iweco.activityRecognition;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.R;
import com.cahue.iweco.util.PreferencesUtil;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
 * IntentService for handling incoming intents that are generated as a result of requesting
 * activity updates using
 * {@link com.google.android.gms.location.ActivityRecognitionApi#requestActivityUpdates}.
 */
public class DetectedActivitiesIntentService extends IntentService {

    protected static final String TAG = DetectedActivitiesIntentService.class.getSimpleName();

    private static final String PREF_PREVIOUS_ACTIVITY_TYPE = "PREF_PREVIOUS_ACTIVITY_CONFIDENCE";
    private static final String PREF_PREVIOUS_ACTIVITY_CONFIDENCE = "PREF_PREVIOUS_ACTIVITY_CONFIDENCE";


    private static DetectedActivity previousActivity;
    private static DetectedActivity previousPreviousActivity;

    // How many times the user has been still in a row
    private static int stillCounter = 0;

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public DetectedActivitiesIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (previousActivity == null)
            previousActivity = getStoredDetectedActivity();
    }

    /**
     * Handles incoming intents.
     *
     * @param intent The Intent is provided (inside a PendingIntent) when requestActivityUpdates()
     *               is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities(result);
        }
    }


    private void handleDetectedActivities(ActivityRecognitionResult result) {

        DetectedActivity mostProbableActivity = result.getMostProbableActivity();

        // Check if still
        if (isStill(mostProbableActivity)) {
            stillCounter++;
            if(stillCounter > 5) {
                // reset if still for too long
                previousActivity = null;
                previousPreviousActivity = null;
            }
        }else {
            stillCounter = 0;
        }

        // If not on foot or in vehicle we are not interested
        if (!isOnFoot(mostProbableActivity) && !isVehicleRelated(mostProbableActivity))
            return;

        // Log each activity.
        Log.d(TAG, "Activities detected");
        for (DetectedActivity da : result.getProbableActivities()) {
            Log.v(TAG, da.toString());
        }

        if ((previousActivity == null || mostProbableActivity.getType() != previousActivity.getType())
                && mostProbableActivity.getConfidence() == 100) {

            if (BuildConfig.DEBUG) {
                showDebugNotification(result, mostProbableActivity);
            }

            if (previousActivity != null) {
                // If switched to on foot, previously in vehicle
                if (isOnFoot(mostProbableActivity) && isVehicleRelated(previousActivity)) {
                    handleVehicleToFoot();
                } else if (isOnFoot(previousActivity) && isVehicleRelated(mostProbableActivity)) {
                    handleFootToVehicle();
                }
            }

            previousPreviousActivity = previousActivity;
            previousActivity = mostProbableActivity;

            savePreviousActivity(previousActivity);
        }

    }
    private void handleVehicleToFoot() {
        // we create an intent to start the location poller service, as declared in manifest
        Intent intent = new Intent(this, ParkedCarRequestedService.class);
        this.startService(intent);
    }

    private void handleFootToVehicle() {
        if (PreferencesUtil.isBtOnEnteringVehicleEnabled(this)) {
            BluetoothAdapter.getDefaultAdapter().enable();
            ActivityRecognitionService.stop(this);
        }
    }


    private void showDebugNotification(ActivityRecognitionResult result, DetectedActivity mostProbableActivity) {
        String previousText = previousActivity != null ?
                "Previous: " + previousActivity.toString() + "\n" :
                "Previous unknown\n";

        StringBuilder stringBuilder = new StringBuilder(previousText);
        for (DetectedActivity detectedActivity : result.getProbableActivities()) {
            stringBuilder.append(detectedActivity.toString() + "\n");
        }

        long[] pattern = {0, 100, 1000};
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder mBuilder =
                new Notification.Builder(this)
                        .setVibrate(pattern)
                        .setSmallIcon(R.drawable.ic_navigation_cancel)
                        .setContentTitle(mostProbableActivity.toString())
                        .setStyle(new Notification.BigTextStyle().bigText(stringBuilder.toString()))
                        .setContentText(previousText);

        mNotifyMgr.notify(null, 7908772, mBuilder.build());
    }

    private boolean isInteresting(DetectedActivity detectedActivity) {
        return (detectedActivity.getType() == DetectedActivity.IN_VEHICLE && detectedActivity.getConfidence() > 95)
                || (detectedActivity.getType() == DetectedActivity.ON_FOOT && detectedActivity.getConfidence() > 100);
    }


    private boolean isStill(DetectedActivity detectedActivity) {
        return detectedActivity.getType() == DetectedActivity.STILL && detectedActivity.getConfidence() > 90;
    }


    private boolean isVehicleRelated(DetectedActivity detectedActivity) {
        if (BuildConfig.DEBUG && detectedActivity.getType() == DetectedActivity.ON_BICYCLE)
            return true;
        return detectedActivity.getType() == DetectedActivity.IN_VEHICLE;
    }

    private boolean isOnFoot(DetectedActivity detectedActivity) {
        return detectedActivity.getType() == DetectedActivity.ON_FOOT;
    }


    private void savePreviousActivity(DetectedActivity previousActivity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit()
                .putInt(PREF_PREVIOUS_ACTIVITY_TYPE, previousActivity.getType())
                .putInt(PREF_PREVIOUS_ACTIVITY_CONFIDENCE, previousActivity.getConfidence())
                .apply();
    }

    private DetectedActivity getStoredDetectedActivity() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int type = prefs.getInt(PREF_PREVIOUS_ACTIVITY_TYPE, DetectedActivity.UNKNOWN);
        int confidence = prefs.getInt(PREF_PREVIOUS_ACTIVITY_CONFIDENCE, 0);
        return new DetectedActivity(type, confidence);
    }


}