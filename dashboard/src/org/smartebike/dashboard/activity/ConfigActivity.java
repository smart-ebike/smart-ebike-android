/**
 * This file is part of Smart-EBike.
 *
 * Smart-EBike is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Smart-EBike is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License version 3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Smart-EBike.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.smartebike.dashboard.activity;

import java.util.ArrayList;
import java.util.Set;

import org.smartebike.dashboard.R;

import roboguice.activity.RoboPreferenceActivity;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

/**
 * Configuration activity.
 */
public class ConfigActivity extends RoboPreferenceActivity implements
        OnPreferenceChangeListener {

	public static final String BLUETOOTH_LIST_KEY = "bluetooth_list_preference";
	public static final String IMPERIAL_UNITS_KEY = "imperial_units_preference";
	public static final String ENABLE_GPS_KEY = "enable_gps_preference";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// read preferences resources available at res/xml/preferences.xml
		addPreferencesFromResource(R.xml.preferences);

		ArrayList<CharSequence> pairedDeviceStrings = new ArrayList<CharSequence>();
		ArrayList<CharSequence> vals = new ArrayList<CharSequence>();
		ListPreference listBtDevices = (ListPreference) getPreferenceScreen()
		        .findPreference(BLUETOOTH_LIST_KEY);

		/*
		 * Let's use this device Bluetooth adapter to select which paired
		 * controller we'll interact with.
		 */
		final BluetoothAdapter mBtAdapter = BluetoothAdapter
		        .getDefaultAdapter();
		if (mBtAdapter == null) {
			listBtDevices.setEntries(pairedDeviceStrings
			        .toArray(new CharSequence[0]));
			listBtDevices.setEntryValues(vals.toArray(new CharSequence[0]));

			// we shouldn't get here, still warn user
			Toast.makeText(this, "This device does not support Bluetooth.",
			        Toast.LENGTH_LONG).show();

			return;
		}

		// listen for preferences click.
		final Activity thisActivity = this;
		listBtDevices.setEntries(new CharSequence[1]);
		listBtDevices.setEntryValues(new CharSequence[1]);
		listBtDevices
		        .setOnPreferenceClickListener(new OnPreferenceClickListener() {
			        public boolean onPreferenceClick(Preference preference) {
				        if (mBtAdapter == null || !mBtAdapter.isEnabled()) {
					        Toast.makeText(
					                thisActivity,
					                "This device does not support Bluetooth or it is disabled.",
					                Toast.LENGTH_LONG).show();
					        return false;
				        }
				        return true;
			        }
		        });

		// get paired devices and populate preference list.
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				pairedDeviceStrings.add(device.getName() + "\n"
				        + device.getAddress());
				vals.add(device.getAddress());
			}
		}
		listBtDevices.setEntries(pairedDeviceStrings
		        .toArray(new CharSequence[0]));
		listBtDevices.setEntryValues(vals.toArray(new CharSequence[0]));
	}

	@Override
	public boolean onPreferenceChange(Preference arg0, Object arg1) {
		// TODO Auto-generated method stub
		return false;
	}

}