package com.bahpps.cahue;

import android.app.Fragment;
import android.location.Location;

/**
 * Created by francesco on 26.11.2014.
 */
public abstract class DetailsFragment extends Fragment {
    public abstract void setUserLocation(Location userLocation);
}
