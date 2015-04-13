package com.cahue.iweco.spots;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.cahue.iweco.R;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.util.Util;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;

/**
 * Created by Francesco on 11/10/2014.
 */
public class ParkingSpot implements Parcelable {

    private static long GREEN_TIME_THRESHOLD_MS = 5 * 60 * 1000;
    private static long YELLOW_TIME_THRESHOLD_MS = 10 * 60 * 1000;

    public final Long id;

    public final LatLng position;

    public final float accuracy;

    public final Date time;

    public final boolean future;

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
        id = parcel.readLong();
        position = parcel.readParcelable(LatLng.class.getClassLoader());
        accuracy = parcel.readFloat();
        time = (Date) parcel.readSerializable();
        future = parcel.readByte() != 0;
    }

    public ParkingSpot(Long id, LatLng location, float accuracy, Date time, boolean future) {
        this.id = id;
        this.position = location;
        this.accuracy = accuracy;
        this.time = time;
        this.future = future;
    }

    public static ParkingSpot fromJSON(JSONObject jsonObject) throws JSONException {
        try {
            return new ParkingSpot(
                    jsonObject.getLong("id"),
                    new LatLng(jsonObject.getDouble("latitude"), jsonObject.getDouble("longitude")),
                    Float.parseFloat(jsonObject.getString("accuracy")),
                    Util.DATE_FORMAT.parse(jsonObject.getString("time")),
                    jsonObject.optBoolean("future"));
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeParcelable(position, i);
        parcel.writeFloat(accuracy);
        parcel.writeSerializable(time);
        parcel.writeByte((byte) (future ? 1 : 0));
    }

    public JSONObject toJSON(Car car) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("car", car.id);
            if (car.spotId != null)
                obj.put("id", car.spotId);
            obj.put("latitude", position.latitude);
            obj.put("longitude", position.longitude);
            obj.put("accuracy", accuracy);
            obj.put("future", future);
            return obj;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        return "ParkingSpot{" +
                "id='" + id + '\'' +
                ", position=" + position +
                ", time=" + time +
                ", future=" + future +
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

    /**
     * Get the marker time based on the time it was created
     *
     * @return
     */
    public Type getMarkerType() {
        long timeSinceSpotWasFree_ms = System.currentTimeMillis() - time.getTime();
        if (timeSinceSpotWasFree_ms < GREEN_TIME_THRESHOLD_MS)
            return Type.green;
        else if (timeSinceSpotWasFree_ms < YELLOW_TIME_THRESHOLD_MS)
            return Type.yellow;
        else
            return Type.red;
    }

    /**
     * Created by Francesco on 19.11.2014.
     */
    public static enum Type {

        red(0.02F, R.color.marker_red, R.dimen.marker_diameter_red, true),
        yellow(0.03F, R.color.marker_yellow, R.dimen.marker_diameter_yellow, false),
        green(0.06F, R.color.marker_green, R.dimen.marker_diameter_green, false);

        /**
         * Difference in alpha values per frame when the marker is included in the map
         */
        public final float dAlpha;

        /**
         * Resource color representing
         */
        public final int colorId;

        /**
         * Resource id indicating the dimension of the spot marker
         */
        public final int diameterId;

        /**
         * The marker is displayed flat and centered
         */
        public boolean flatAndCentered;

        Type(float dAlpha, int colorId, int diameterId, boolean flatAndCentered) {
            this.dAlpha = dAlpha;
            this.colorId = colorId;
            this.diameterId = diameterId;
            this.flatAndCentered = flatAndCentered;
        }

    }
}
