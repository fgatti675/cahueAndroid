package com.bahpps.cahue.spots;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.util.Date;

/**
 * Created by Francesco on 11/10/2014.
 */
public class ParkingSpot implements Parcelable, ClusterItem {

    private String id;

    private LatLng position;

    private Date time;

    public static final Parcelable.Creator<ParkingSpot> CREATOR =
            new Parcelable.Creator<ParkingSpot>() {
                @Override
                public ParkingSpot createFromParcel(Parcel parcel) {
                    return new ParkingSpot(parcel);
                }

                @Override
                public ParkingSpot[] newArray(int size) {
                    return new ParkingSpot[size];
                }
            };

    public ParkingSpot(Parcel parcel) {
        id = parcel.readString();
        position = parcel.readParcelable(LatLng.class.getClassLoader());
        time = (Date) parcel.readSerializable();
    }

    public ParkingSpot(String id, LatLng location, Date time) {
        this.id = id;
        this.position = location;
        this.time = time;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeParcelable(position, 0);
        parcel.writeSerializable(time);
    }

    @Override
    public String toString() {
        return "ParkingSpot{" +
                "id='" + id + '\'' +
                ", position=" + position +
                ", time=" + time +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParkingSpot that = (ParkingSpot) o;

        if (!id.equals(that.id)) return false;
        if (!position.equals(that.position)) return false;
        if (!time.equals(that.time)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + position.hashCode();
        result = 31 * result + time.hashCode();
        return result;
    }

    @Override
    public LatLng getPosition() {
        return position;
    }
}
