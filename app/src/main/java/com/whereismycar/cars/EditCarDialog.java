package com.whereismycar.cars;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.whereismycar.R;
import com.whereismycar.model.Car;


/**
 * This class is used as a dialog to ask the user to edit properties like nake or color of the car.
 *
 * @author Francesco
 */
public class EditCarDialog extends DialogFragment {

    private final static String TAG = EditCarDialog.class.getSimpleName();
    private static final String ARG_CAR = "arg_car";
    private static final String ARG_NEW_CAR = "arg_new_car";

    boolean newCar;

    @Nullable
    private Car car;

    private CarEditedListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param car Car being edited
     * @return A new instance of fragment MarkerDetailsFragment.
     */
    @NonNull
    public static EditCarDialog newInstance(Car car, boolean newCar) {
        EditCarDialog fragment = new EditCarDialog();
        Bundle args = new Bundle();
        args.putParcelable(ARG_CAR, car);
        args.putBoolean(ARG_NEW_CAR, newCar);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the Listener so we can send events to the host
            mListener = (CarEditedListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement CarEditedListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            car = getArguments().getParcelable(ARG_CAR);
            newCar = getArguments().getBoolean(ARG_NEW_CAR);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        /**
         * Get fields
         */
        View view = View.inflate(getActivity(), R.layout.fragment_car_edit, null);
        final EditText name = (EditText) view.findViewById(R.id.name_edit);
        final Spinner spinner = (Spinner) view.findViewById(R.id.color_spinner);
        final ColorSpinnerAdapter spinnerAdapter = new ColorSpinnerAdapter(getActivity());
        spinner.setAdapter(spinnerAdapter);

        /**
         * Bind values
         */
        name.setText(car.name);
        name.requestFocus();
        int colorPosition = spinnerAdapter.getPositionOf(car.color);
        spinner.setSelection(colorPosition);

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setTitle(R.string.car)
                .setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        car.name = name.getText().toString();
                        car.color = (Integer) spinner.getSelectedItem();
                        mListener.onCarEdited(car, newCar);
                    }
                })
                .setNegativeButton(R.string.cancel, null);


        // Create the AlertDialog object and return it
        return builder.create();
    }


    public interface CarEditedListener {
        void onCarEdited(Car car, boolean newCar);
    }

}
