package com.whereismycar.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;

import com.whereismycar.OnCarClickedListener;
import com.whereismycar.R;
import com.whereismycar.model.Car;

import java.util.List;

/**
 * Adapter for generating buttons from a list of cars
 */
public class CarButtonAdapter extends BaseAdapter {

    private final OnCarClickedListener carSelectedListener;
    private final List<Car> cars;

    public CarButtonAdapter(OnCarClickedListener carSelectedListener, List<Car> cars) {
        this.carSelectedListener = carSelectedListener;
        this.cars = cars;
    }

    public int getCount() {
        return cars.size();
    }

    public Object getItem(int position) {
        return cars.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    // create a new Button for each item referenced by the Adapter
    @Nullable
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Button button;
        final Car car = cars.get(position);
        if (convertView == null) {
            button = (Button) LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.button_car,
                            parent,
                            false);
        } else {
            button = (Button) convertView;
        }

        if (car.isOther()) button.setText(R.string.other);
        else button.setText(car.name);

        button.setOnClickListener(view -> carSelectedListener.onCarSelected(car));
        return button;
    }
}
