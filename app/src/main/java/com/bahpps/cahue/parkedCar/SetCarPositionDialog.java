package com.bahpps.cahue.parkedCar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.bahpps.cahue.R;
import com.bahpps.cahue.parkedCar.CarLocationManager;


/**
 * This class is used as a dialog to ask the user if he is sure to store the location in
 * the indicated place.
 * @author Francesco
 *
 */
public class SetCarPositionDialog extends DialogFragment  {

    Location location;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setIcon(R.drawable.ic_action_help)
                .setTitle(R.string.car_dialog_text)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.w("CAR_DIALOG", location.toString());

                        // If ok, we just send and intent and leave the location receivers to do all the work
                        CarLocationManager.saveLocation(getActivity(), location);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}