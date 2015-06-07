package com.cahue.iweco;

import android.location.Location;

import com.google.android.gms.maps.model.CameraPosition;

/**
 * Implemented by components that can make camera update requests.
 * Requests are made calling {@link com.cahue.iweco.CameraManager#onCameraUpdateRequest(com.google.android.gms.maps.CameraUpdate, CameraUpdateRequester)}
 */
public interface CameraUpdateRequester {

    /**
     * Indicate this delegate that the camera of the map has changed.
     * This method need to implement the logic to load any new data if necessary (asynchronously).
     *
     * @param cameraPosition new position of the camera
     * @param requester the requester of the camera update, null if the camera was moved by the user.
     */
    void onCameraChange(CameraPosition cameraPosition, CameraUpdateRequester requester);

    /**
     * Tell this camera updater to follow or not
     */
    void setCameraFollowing(boolean following);

    /**
     * Called when the dimensions of the map change (like when details are displayed).
     * If the component is following it should trigger an update
     */
    void onMapResized();
}
