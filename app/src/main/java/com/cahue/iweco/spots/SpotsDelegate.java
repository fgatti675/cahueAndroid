package com.cahue.iweco.spots;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.cahue.iweco.AbstractMarkerDelegate;
import com.cahue.iweco.CameraUpdateRequester;
import com.cahue.iweco.spots.query.AreaSpotsQuery;
import com.cahue.iweco.spots.query.ParkingSpotsQuery;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Delegate in charge of querying and drawing parking spots in the map.
 * <p/>
 * Created by Francesco on 21/10/2014.
 */
public class SpotsDelegate extends AbstractMarkerDelegate implements ParkingSpotsQuery.ParkingSpotsUpdateListener,
        CameraUpdateRequester {

    private final static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    private static final String TAG = "SpotsDelegate";
    private static final String QUERY_TAG = "SpotsDelegateQuery";

    // Earth’s radius, sphere
    private final static double EARTH_RADIUS = 6378137;

    // time after we consider the query is outdated and need to repeat
    private final static long TIMEOUT_MS = 60000;

    // number of spots being retrieved on nearby spots query
    private final static int CLOSEST_LOCATIONS = 200;

    // max number of spots displayed at once.
    private static final int MARKERS_LIMIT = 100;

    // distance we are adding to the bounds query on each one of the 4 sides to get also results outside the screen
    public static final int OFFSET_METERS = 2000;
    public static final String FRAGMENT_TAG = "SPOTS_DELEGATE";

    private final Handler handler = new Handler();

    /**
     * If zoom is more far than this, we don't display the markers
     */
    public final static float MAX_ZOOM = 0F;

    private Set<ParkingSpot> spots;
    private Map<ParkingSpot, Marker> spotMarkersMap;
    private Map<Marker, ParkingSpot> markerSpotsMap;

    private GoogleMap mMap;

    private SpotSelectedListener spotSelectedListener;

    // In the next spots update, clear the previous state
    private boolean shouldBeReset = false;

    private List<LatLngBounds> queriedBounds;

    private LatLngBounds viewBounds;
    private LatLngBounds extendedViewBounds;

    private Date lastResetTaskRequestTime;
    private ScheduledFuture scheduledResetTask;

    /**
     * If markers shouldn't be displayed (like zoom is too far)
     */
    private boolean markersDisplayed = false;

    // location used as a center fos nearby spots query
//    private LatLng userQueryLocation;
//    private ParkingSpotsQuery nearbyQuery;

//    private Date lastNearbyQuery;

    private ParkingSpot selectedSpot;

    public static SpotsDelegate newInstance() {
        SpotsDelegate fragment = new SpotsDelegate();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {

            setRetainInstance(true);

            queriedBounds = new ArrayList<>();
            spots = new HashSet<>();
            lastResetTaskRequestTime = new Date();

            spotMarkersMap = new HashMap<>();
            markerSpotsMap = new HashMap<>();
        }
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            this.spotSelectedListener = (SpotSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement SpotSelectedListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        setUpResetTask();
        if (mMap != null)
            doDraw();
    }

    public void setUpResetTask() {

        Log.v(TAG, "Setting up repetitive reset task");

        long timeFromLastTimeout = System.currentTimeMillis() - lastResetTaskRequestTime.getTime();
        long nextTimeOut = TIMEOUT_MS - timeFromLastTimeout;
        if (nextTimeOut < 0) nextTimeOut = 0;

        Log.d(TAG, "Next time out (ms): " + nextTimeOut);

        scheduledResetTask = scheduledExecutorService.scheduleAtFixedRate(
                new Runnable() {

                    public void run() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "scheduledResetTask run");
                                lastResetTaskRequestTime = new Date();
                                shouldBeReset = true;
                                repeatLastQuery();
                            }
                        });
                    }
                },
                nextTimeOut,
                TIMEOUT_MS,
                TimeUnit.MILLISECONDS);

    }

    private void reset(boolean clearSpots) {
        Log.d(TAG, "Spots reset");
        for (Marker marker : markerSpotsMap.keySet()) {
            marker.remove();
        }
        queriedBounds.clear();
        if (clearSpots) spots.clear();
        markerSpotsMap.clear();
        spotMarkersMap.clear();
    }

    private boolean repeatLastQuery() {
        return queryCameraView();
    }

//    private synchronized boolean queryClosestSpots(LatLng userLocation) {
//
//        this.userQueryLocation = userLocation;
//
//        if (nearbyQuery != null && nearbyQuery.getStatus() == AsyncTask.Status.RUNNING) {
//            return false;
//        }
//
//        nearbyQuery = new NearestSpotsQuery(mContext, userLocation, CLOSEST_LOCATIONS, this);
//
//        Log.v(QUERY_TAG, "Starting query for closest spots to: " + userLocation);
//        nearbyQuery.execute();
//
//        return true;
//    }

    /**
     * Set the bounds where the camera is currently looking.
     * A query is done
     *
     * @return
     */
    private synchronized boolean queryCameraView() {


        // What the user is actually seeing right now

        setUpViewBounds();

//        if (nearbyQuery != null && nearbyQuery.getStatus() == AsyncTask.Status.RUNNING && viewBounds.contains(userQueryLocation)) {
//            Log.d(QUERY_TAG, "Abort camera query because view contains user");
//            return false;
//        }

        // A broader space we want to query so that data is there when we move the camera
        this.extendedViewBounds = LatLngBounds.builder()
                .include(getOffsetLatLng(viewBounds.northeast, OFFSET_METERS, OFFSET_METERS))
                .include(getOffsetLatLng(viewBounds.southwest, -OFFSET_METERS, -OFFSET_METERS))
                .build();

        /**
         * Check if this query is already contained in another one
         */
        if (!shouldBeReset) {
            for (LatLngBounds latLngBounds : queriedBounds) {
                if (latLngBounds.contains(viewBounds.northeast) && latLngBounds.contains(viewBounds.southwest)) {
                    Log.v(QUERY_TAG, "No need to query again camera");
                    return false;
                }
            }
        }

        // we keep a reference of the current query to prevent repeating it
        queriedBounds.add(extendedViewBounds);

        ParkingSpotsQuery areaQuery = new AreaSpotsQuery(getActivity(), extendedViewBounds, this);

        Log.v(QUERY_TAG, "Starting query for queryBounds: " + extendedViewBounds);
        areaQuery.execute();

        return true;

    }

    private void setUpViewBounds() {
        this.viewBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
    }

    /**
     * Called when new parking spots are received
     *
     * @param parkingSpots
     */
    @Override
    public synchronized void onSpotsUpdate(ParkingSpotsQuery query, Set<ParkingSpot> parkingSpots) {

//        if (query == nearbyQuery)
//            lastNearbyQuery = new Date();

        if (shouldBeReset) {
            reset(true);
            shouldBeReset = false;
        }

        /**
         * We can consider that after an update, all
         */
        if (!parkingSpots.isEmpty()) {
            LatLngBounds.Builder builder = LatLngBounds.builder();
            for (ParkingSpot spot : parkingSpots) {
                builder.include(spot.position);
            }
            queriedBounds.add(builder.build());
        }

        spots.addAll(parkingSpots);

        doDraw();
    }

    /**
     * On getting an error when retrieving spots
     *
     * @param statusCode
     * @param reasonPhrase
     */
    @Override
    public void onServerError(ParkingSpotsQuery query, int statusCode, String reasonPhrase) {
        Toast.makeText(getActivity(), "Error: " + reasonPhrase, Toast.LENGTH_SHORT).show();
        repeatLastQuery();
    }

    @Override
    public void onIOError() {
        Toast.makeText(getActivity(), "Check internet connection", Toast.LENGTH_SHORT).show();
    }

    int displayedMarkers;

    public void doDraw() {

        Log.d(TAG, "Drawing spots");

        setUpViewBounds();

        // hideMarkers first
        hideMarkers();

        if (!markersDisplayed) {
            Log.d(TAG, "Abort drawing spots. Markers are hidden");
            return;
        }

        for (final ParkingSpot parkingSpot : spots) {

            if (displayedMarkers > MARKERS_LIMIT) {
                Log.v(TAG, "Marker display limit reached");
                return;
            }

            LatLng spotPosition = parkingSpot.position;

            Marker marker = spotMarkersMap.get(parkingSpot);

            // if there is no marker we create it
            if (marker == null) {
                marker = mMap.addMarker(MarkerFactory.getMarker(parkingSpot, getActivity(), parkingSpot.equals(selectedSpot)));
                marker.setVisible(false);
                spotMarkersMap.put(parkingSpot, marker);
                markerSpotsMap.put(marker, parkingSpot);
            }

            // else we may need to update it
            else if (true) { // TODO
                updateMarker(parkingSpot, marker, parkingSpot.equals(selectedSpot));
            }


            if (!marker.isVisible() && viewBounds.contains(spotPosition)) {
                makeMarkerVisible(marker, parkingSpot);
                displayedMarkers++;
            }

        }
    }

    private void updateMarker(ParkingSpot parkingSpot, Marker marker, boolean selected) {
        MarkerOptions markerOptions = MarkerFactory.getMarker(parkingSpot, getActivity(), selected);
        marker.setIcon(markerOptions.getIcon());
    }


    /**
     * Hide non visible markers (outside of viewport)
     */
    private void hideMarkers() {

        displayedMarkers = 0;

        for (final Marker marker : markerSpotsMap.keySet()) {
            if (!markersDisplayed || !viewBounds.contains(marker.getPosition()))
                marker.setVisible(false);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        clearSelectedSpot();

        // apply new style and tell listener
        selectedSpot = markerSpotsMap.get(marker);
        if (selectedSpot != null) {
            Marker selectedMarker = spotMarkersMap.get(selectedSpot);

            updateMarker(selectedSpot, selectedMarker, true);
            selectedMarker.showInfoWindow();

            spotSelectedListener.onSpotClicked(selectedSpot);
        }
        return true;
    }

    /**
     * Clear previously selected spot
     */
    private void clearSelectedSpot() {
        // clear previous selection
        if (selectedSpot != null) {
            Marker previousMarker = spotMarkersMap.get(selectedSpot);

            // we may have restored the selected spot but it may not have been drawn (like on device rotation)
            if (previousMarker != null) {
                updateMarker(selectedSpot, previousMarker, false);
            }
        }
        selectedSpot = null;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        Log.d(TAG, "onMapReady");
        this.mMap = map;
        reset(false);
        doDraw();
    }


    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "scheduledResetTask canceled");
        scheduledResetTask.cancel(true);
    }

    @Override
    public void onDetailsClosed() {
        clearSelectedSpot();
    }

    Random random = new Random();

    private void makeMarkerVisible(final Marker marker, ParkingSpot spot) {

        marker.setVisible(true);
        final float dAlpha = 0.03F;
        marker.setAlpha(0);

        handler.post(new Runnable() {
            @Override
            public void run() {

                float alpha = marker.getAlpha() + dAlpha;
                if (alpha < 1) {
                    // Post again 12ms later.
                    marker.setAlpha(alpha);
                    handler.postDelayed(this, 12);
                } else {
                    marker.setAlpha(1);
                    // animation ended
                }
            }
        });

    }


    @Override
    public void onLocationChanged(Location userLocation) {
        LatLng userLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());

//        if (lastNearbyQuery == null || System.currentTimeMillis() - lastNearbyQuery.getTime() > TIMEOUT_MS)
//            queryClosestSpots(userLatLng);
//        else
//            Log.v(TAG, "No need to query for closest points again");
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition, CameraUpdateRequester requester) {
        float zoom = mMap.getCameraPosition().zoom;
        Log.v(TAG, "zoom: " + zoom);

        /**
         * Query for current camera position
         */
        if (zoom >= MAX_ZOOM) {
            Log.d(TAG, "Querying because we are close enough");

            queryCameraView();

            markersDisplayed = true;
        }
        /**
         * Too far
         */
        else {
            Log.d(TAG, "Too far to query locations");
            markersDisplayed = false;
        }

        doDraw();

    }


    public LatLng getOffsetLatLng(LatLng original, double offsetNorth, double offsetEast) {

        // Coordinate offsets in radians
        double dLat = offsetNorth / EARTH_RADIUS;
        double dLon = offsetEast / (EARTH_RADIUS * Math.cos(Math.PI * original.latitude / 180));

        // OffsetPosition, decimal degrees
        double nLat = original.latitude + dLat * 180 / Math.PI;
        double nLon = original.longitude + dLon * 180 / Math.PI;

        return new LatLng(nLat, nLon);
    }

    public interface SpotSelectedListener {
        void onSpotClicked(ParkingSpot spot);
    }

}