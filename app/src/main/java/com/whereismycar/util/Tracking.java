package com.whereismycar.util;

import androidx.annotation.Nullable;
import android.util.Log;

import com.whereismycar.BuildConfig;
import com.whereismycar.ParkifyApp;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;

public class Tracking {

    public static final String CATEGORY_TUTORIAL = "Tutorial";

    public static final String CATEGORY_LOGIN = "Login";

    public static final String CATEGORY_MAP = "Map";

    public static final String CATEGORY_CAR_MANAGER = "Car Manager";

    public static final String CATEGORY_PARKING = "Parking";

    public static final String CATEGORY_NAVIGATION_DRAWER = "Navigation drawer";

    public static final String CATEGORY_RATING_DIALOG = "Rating Dialog";

    public static final String CATEGORY_DONATION_DIALOG = "Donation Dialog";

    public static final String CATEGORY_ADVERTISING = "Advertising";

    public static final String CATEGORY_NOTIFICATION_ACT_RECOG = "Notification act. recognition";


    public static final String ACTION_POSSIBLE_SPOT_DETECTED = "Possible spot detected";

    public static final String ACTION_BLUETOOTH_PARKING = "Bluetooth parking detected";

    public static final String ACTION_BLUETOOTH_FREED_SPOT = "Bluetooth freed parking spot";

    public static final String ACTION_DO_LOGIN = "Do login";

    public static final String ACTION_SKIP_LOGIN = "Skip login";

    public static final String ACTION_CAR_SELECTED = "Car selected";

    public static final String ACTION_POSSIBLE_CAR_SELECTED = "Possible car selected";

    public static final String ACTION_CAR_LOCATION_SHARED = "Car location shared";

    public static final String ACTION_NOTIFICATION_REMOVED = "Notification removed";

    public static final String ACTION_FREE_SPOT_SELECTED = "Spot selected";

    public static final String ACTION_PARKING_SELECTED = "Parking selected";

    public static final String ACTION_CAR_EDIT = "Car edited";

    public static final String ACTION_AD_CLICKED = "Ad clicked";

    public static final String ACTION_AD_IMPRESSION = "Ad impression";

    public static final String LABEL_FACEBOOK_LOGIN = "Facebook login";

    public static final String LABEL_GOOGLE_LOGIN = "Google login";

    public static final String LABEL_SELECTED_FROM_MARKER = "Selected from marker";

    public static final String LABEL_SELECTED_FROM_DRAWER = "Selected from drawer";

    public static final String ACTION_CAR_MANAGER_CLICK = "Car manager click";
    public static final String ACTION_SETTINGS_CLICK = "Settings click";
    public static final String ACTION_DONATION_CLICK = "Donation click";
    public static final String ACTION_SIGN_OUT = "Sign out";
    public static final String ACTION_NAVIGATION_TOGGLE = "Navigation drawer toggle click";

    public static final String ACTION_ACCEPT = "Accepted";
    public static final String ACTION_DISMISS = "Dismissed";

    public static final String ACTION_PURCHASE_ERROR = "Purchase error";
    public static final String ACTION_PURCHASE_SUCCESSFUL = "Purchase successful";
    public static final String ACTION_PURCHASE_CANCELLED = "Started purchase cancelled";


    public static void sendView(String screenName) {

        Log.i("Tracking", "View: " + screenName);

        if (BuildConfig.DEBUG) return;

        getTracker().setScreenName(screenName);
        HitBuilders.ScreenViewBuilder builder = new HitBuilders.ScreenViewBuilder();
        getTracker().send(builder.build());

    }

    public static void sendEvent(String category, String action) {
        sendEvent(category, action, null, false);
    }

    public static void sendEvent(String category, String action, boolean nonInteractionEvent) {
        sendEvent(category, action, null, nonInteractionEvent);
    }

    public static void sendEvent(String category, String action, String label) {
        sendEvent(category, action, label, false);
    }

    public static void sendEvent(String category, String action, @Nullable String label, boolean nonInteractionEvent) {

        Log.i("Tracking", "Event: " + category + " / " + action + " / " + label);

        if (BuildConfig.DEBUG) return;

        HitBuilders.EventBuilder builder = new HitBuilders.EventBuilder();
        builder.setAction(action);
        builder.setCategory(category);
        builder.setNonInteraction(nonInteractionEvent);
        if (label != null) builder.setLabel(label);

        getTracker().send(builder.build());


    }

    public static void sendException(String location, String message, boolean fatal) {

        Log.i("Tracking", "Event: " + location + " " + message);

        if (BuildConfig.DEBUG) return;

        // Build and send exception.
        getTracker().send(new HitBuilders.ExceptionBuilder()
                .setDescription(location + " : " + message)
                .setFatal(fatal)
                .build());

    }

    public static void setTrackerUserId(String trackerUserId) {
        ParkifyApp.getParkifyApp().setTrackerUserId(trackerUserId);
        getFirebaseAnalytics().setUserId(trackerUserId);
    }

    private static Tracker getTracker() {
        return ParkifyApp.getParkifyApp().getTracker();
    }

    private static FirebaseAnalytics getFirebaseAnalytics() {
        return ParkifyApp.getParkifyApp().getFirebaseAnalytics();
    }
}
