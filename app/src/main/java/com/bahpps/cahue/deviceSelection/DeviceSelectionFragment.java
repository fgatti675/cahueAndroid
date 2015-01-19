package com.bahpps.cahue.deviceSelection;


import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;

import com.bahpps.cahue.R;
import com.bahpps.cahue.util.Util;

import java.util.ArrayList;
import java.util.Set;

/**
 * This Activity appears as a dialog. It lists any paired devices and devices detected in the area after discovery. When
 * a device is chosen by the user, the MAC address of the device is sent back to the parent Activity in the result
 * Intent.
 */
public class DeviceSelectionFragment extends Fragment {

    // Debugging
    private static final String TAG = DeviceSelectionFragment.class.getSimpleName();

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private DeviceListAdapter mDevicesArrayAdapter;

    private Button enableBTButton;
    private ListView pairedListView;

    private Set<String> selectedDeviceAddresses;

    private DeviceSelectionLoadingListener mListener;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CarDetailsFragment.
     */
    public static DeviceSelectionFragment newInstance() {
        DeviceSelectionFragment fragment = new DeviceSelectionFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_device_list, container, false);

        enableBTButton = (Button) view.findViewById(R.id.enable_bt);
        enableBTButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            }
        });

        // Initialize array adapter
        mDevicesArrayAdapter = new DeviceListAdapter();

        // Find and set up the ListView for paired devices
        pairedListView = (ListView) view.findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mDevicesArrayAdapter);

        return view;
    }

    private void setBondedDevices() {
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (!pairedDevices.isEmpty()) {
            mDevicesArrayAdapter.setDevices(pairedDevices);
            mDevicesArrayAdapter.notifyDataSetChanged();
        }
    }

    private void updateUI() {
        if (mBtAdapter.isEnabled()) {
            enableBTButton.setVisibility(View.GONE);
            pairedListView.setVisibility(View.VISIBLE);
        } else {
            enableBTButton.setVisibility(View.VISIBLE);
            pairedListView.setVisibility(View.GONE);
        }
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (DeviceSelectionLoadingListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement DeviceSelectionLoadingListener");
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        selectedDeviceAddresses = Util.getPairedDevices(getActivity());

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        getActivity().registerReceiver(mReceiver, filter);

        // Register for broadcasts when bt has been disconnected or disconnected
        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

    }

    @Override
    public void onResume() {
        super.onResume();
        updateUIAndDoDiscovery();
    }

    private void updateUIAndDoDiscovery() {
        updateUI();

        setBondedDevices();

        if(mBtAdapter.isEnabled()){
            doDiscovery();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister broadcast listeners
        getActivity().unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {

        if (mBtAdapter.isEnabled()) {
            Log.d(TAG, "doDiscovery");

            // Indicate scanning in the title
            mListener.devicesBeingLoaded(true);

            // If we're already discovering, stop it
            if (mBtAdapter.isDiscovering()) {
                mBtAdapter.cancelDiscovery();
            }

            // Request discover from BluetoothAdapter
            mBtAdapter.startDiscovery();
        }
    }


    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed
                // already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mDevicesArrayAdapter.add(device);
                    mDevicesArrayAdapter.notifyDataSetChanged();
                }

            }

            // When discovery is finished, remove progress bar
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

                mListener.devicesBeingLoaded(false);

                if (mDevicesArrayAdapter.getCount() == 0) {
                    // String noDevices = getResources().getText(
                    // R.string.none_found).toString();
                    // mNewDevicesArrayAdapter.add(noDevices);
                }
            }

            // Update UI and discovery if BT state changed
            else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {

                updateUIAndDoDiscovery();

            }
        }
    };


    private class DeviceListAdapter extends BaseAdapter {

        ArrayList<BluetoothDevice> mDevices;

        DeviceListAdapter() {
            mDevices = new ArrayList<BluetoothDevice>();
        }

        public void setDevices(Set<BluetoothDevice> devices) {
            mDevices = new ArrayList<BluetoothDevice>(devices);
        }

        public void add(BluetoothDevice device) {
            mDevices.add(device);
        }

        public int getCount() {
            return mDevices.size();
        }

        public BluetoothDevice getItem(int position) {
            return mDevices.get(position);
        }

        public long getItemId(int arg0) {
            return arg0;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {

            final View view;

            // Code for recycling views
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.list_device_checkbox, null);
            } else {
                view = convertView;
            }

            final BluetoothDevice device = mDevices.get(position);

            // We set the visibility of the check image
            final CheckBox checkBox = (CheckBox) view.findViewById(R.id.device);
            boolean contains = selectedDeviceAddresses.contains(device.getAddress());
            checkBox.setChecked(contains);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    checkBox.callOnClick();

                }
            });

            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    String name = device.getName();
                    String address = device.getAddress();

                    if (checkBox.isChecked()) {
                        selectedDeviceAddresses.add(address);
                        Log.d(TAG, "Added: " + name + " " + address);
                    } else {
                        selectedDeviceAddresses.remove(address);
                        Log.d(TAG, "Removed: " + name + " " + address);
                    }

                    Util.setPairedDevices(getActivity(), selectedDeviceAddresses);

                }
            });

            checkBox.setText(device.getName());

            return view;
        }
    }

    public interface DeviceSelectionLoadingListener {

        public void devicesBeingLoaded(boolean loading);

    }

}