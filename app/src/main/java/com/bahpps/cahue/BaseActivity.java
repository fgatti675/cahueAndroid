package com.bahpps.cahue;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;

import android.os.UserManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.bahpps.cahue.util.Util;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.model.people.Person;

import java.io.IOException;
import java.io.InputStream;


/**
 * A base class to wrap communication with the Google Play Services PlusClient.
 */
public abstract class BaseActivity
        extends ActionBarActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private final static String TAG = BaseActivity.class.getSimpleName();

    // Profile pic image size in pixels
    private final static int PROFILE_PIC_SIZE = 400;

    // A magic number we will use to know that our sign-in error resolution activity has completed
    private final static int OUR_REQUEST_CODE = 49404;

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private final static String SCOPE = "oauth2:https://www.googleapis.com/auth/plus.login https://www.googleapis.com/auth/userinfo.email";

    /**
     * A flag indicating that a PendingIntent is in progress and prevents us
     * from starting further intents.
     */
    private boolean mIntentInProgress;

    private boolean mSigningIn;

    // This is the helper object that connects to Google Play Services.
    private GoogleApiClient mGoogleApiClient;

    // The saved result from {@link #onConnectionFailed(ConnectionResult)}.  If a connection
    // attempt has been made, this is non-null.
    // If this IS null, then the connect method is still running.
    private ConnectionResult mConnectionResult;

    private String mLoggedEmail;
    private String mAuthToken;

    /**
     * Called when the PlusClient is successfully connected.
     */
    protected abstract void onPlusClientSignIn();

    /**
     * Called when the {@link PlusClient} is disconnected.
     */
    protected abstract void onPlusClientSignOut();

    /**
     * Called when there is a change in connection state.  If you have "Sign in"/ "Connect",
     * "Sign out"/ "Disconnect", or "Revoke access" buttons, this lets you know when their states
     * need to be updated.
     */
    protected abstract void onConnectingStatusChange(boolean connecting);

    /**
     * Called when signing in is required
     */
    protected abstract void onSignInRequired();

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    protected boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (checkPlayServices()) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState != null) {
                mIntentInProgress = savedInstanceState.getBoolean("mIntentInProgress");
            }
            Log.d(TAG, "mGoogleApiClient initialized");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putBoolean("mIntentInProgress", mIntentInProgress);

    }

    private void setUpLocationClientIfNeeded() {

        if (mGoogleApiClient == null) {

            // Initialize the PlusClient connection.
            // Scopes indicate the information about the user your application will be able to access.
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Plus.API)
                    .addApi(LocationServices.API)
                    .addApi(ActivityRecognition.API)
                    .addScope(Plus.SCOPE_PLUS_LOGIN)
                    .addScope(new Scope("https://www.googleapis.com/auth/userinfo.email"))
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

        }
    }

    /**
     * Try to sign in the user.
     */
    public void signIn() {
        if (!mGoogleApiClient.isConnecting()) {
            // Show the dialog as we are now signing in.
            mSigningIn = true;
            resolveSignInError();
            onConnectingStatusChange(true);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        setUpLocationClientIfNeeded();
        Log.d(TAG, "mGoogleApiClient connecting");
        mGoogleApiClient.connect();
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Sign out the user (so they can switch to another account).
     */
    public void signOut() {

        // We only want to sign out if we're connected.
        if (mGoogleApiClient.isConnected()) {
            // Clear the default account in order to allow the user to potentially choose a
            // different account from the account chooser.

            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            // Disconnect from Google Play Services, then reconnect in order to restart the
            // process from scratch.
            mGoogleApiClient.disconnect();
            mGoogleApiClient.connect();

            Log.v(TAG, "Sign out successful!");
        }

        Util.saveOAuthToken(BaseActivity.this, null);

        onConnectingStatusChange(false);
        onPlusClientSignOut();
    }


    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {

        if (requestCode == OUR_REQUEST_CODE) {
            if (responseCode != RESULT_OK) {
                mSigningIn = false;
            }

            mIntentInProgress = false;

            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        }
    }


    /**
     * Successfully connected
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected");
        mSigningIn = false;
        onPlusClientSignIn();
        getProfileInformation();
        requestOauthToken();
        onConnectingStatusChange(false);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
    }


    /**
     * Connection failed for some reason
     * Try and resolve the result.  Failure here is usually not an indication of a serious error,
     * just that the user's input is needed.
     *
     * @see #onActivityResult(int, int, Intent)
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {

        if (!result.hasResolution()) {
            GooglePlayServicesUtil
                    .getErrorDialog(result.getErrorCode(), this, 0)
                    .show();
            return;
        }

        if (result.getErrorCode() == ConnectionResult.SIGN_IN_REQUIRED) {
            onSignInRequired();
        }

        if (!mIntentInProgress) {
            // Store the ConnectionResult for later usage
            mConnectionResult = result;

            if (mSigningIn) {
                // The user has already clicked 'sign-in' so we attempt to
                // resolve all
                // errors until the user is signed in, or they cancel.
                resolveSignInError();
            }
        }

        onConnectingStatusChange(false);
    }

    /**
     * Method to resolve any signin errors
     */
    private void resolveSignInError() {
        if (mConnectionResult.hasResolution()) {
            try {
                mIntentInProgress = true;
                mConnectionResult.startResolutionForResult(this, OUR_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    private void requestOauthToken() {

        Log.i(TAG, "requestOauthToken");

        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void[] objects) {
                try {
                    mAuthToken = GoogleAuthUtil.getToken(BaseActivity.this, mLoggedEmail, SCOPE);
                    if (mAuthToken != null)
                        Util.saveOAuthToken(BaseActivity.this, mAuthToken);
                } catch (UserRecoverableAuthException userRecoverableException) {
                    // GooglePlayServices.apk is either old, disabled, or not present
                    // so we need to show the user some UI in the activity to recover.
                    onSignInRequired();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (GoogleAuthException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "Auth token: " + mAuthToken);
                return mAuthToken;
            }

            @Override
            protected void onPostExecute(String authToken) {
                onAuthTokenSet(authToken);
            }

        }.execute();


    }

    protected void onAuthTokenSet(String authToken) {

    }

    public String getAuthToken() {
        return mAuthToken;
    }

    /**
     * Fetching user's information name, mLoggedEmail, profile pic
     */
    private void getProfileInformation() {
        try {
            if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
                Person currentPerson = Plus.PeopleApi
                        .getCurrentPerson(mGoogleApiClient);
                String personName = currentPerson.getDisplayName();
                String personPhotoUrl = currentPerson.getImage().getUrl();
                String personGooglePlusProfile = currentPerson.getUrl();
                mLoggedEmail = Plus.AccountApi.getAccountName(mGoogleApiClient);

                Log.e(TAG, "Name: " + personName + ", plusProfile: "
                        + personGooglePlusProfile + ", mLoggedEmail: " + mLoggedEmail
                        + ", Image: " + personPhotoUrl);

//                txtName.setText(personName);
//                txtEmail.setText(mLoggedEmail);

                // by default the profile url gives 50x50 px image only
                // we can replace the value with whatever dimension we want by
                // replacing sz=X
                personPhotoUrl = personPhotoUrl.substring(0,
                        personPhotoUrl.length() - 2)
                        + PROFILE_PIC_SIZE;

//                new LoadProfileImage(imgProfilePic).execute(personPhotoUrl);

            } else {
                Toast.makeText(getApplicationContext(),
                        "Person information is null", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Background Async task to load user profile picture from url
     */
    private class LoadProfileImage extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public LoadProfileImage(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

}
