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

import org.smartebike.dashboard.IPostListener;
import org.smartebike.dashboard.R;
import org.smartebike.dashboard.io.BluetoothService;
import org.smartebike.dashboard.io.BluetoothServiceConnection;

import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectResource;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.inject.Inject;

/**
 * The main activity.
 */
@ContentView(R.layout.activity_main)
public class MainActivity extends RoboActivity {

	private static final String TAG = "MainActivity";

	private static final int NO_BLUETOOTH_ID = 0;
	private static final int BLUETOOTH_DISABLED = 1;
	private static final int START_LIVE_DATA = 3;
	private static final int STOP_LIVE_DATA = 4;
	private static final int SETTINGS = 5;

	// callback for service to update UI.
	private IPostListener mListener = null;
	private Intent mServiceIntent = null;
	private BluetoothServiceConnection mServiceConnection = null;

	@Inject
	private SensorManager sensorManager;
	@Inject
	private SharedPreferences prefs;
	@Inject
	private PowerManager powerManager;
	private PowerManager.WakeLock wakeLock;

	@InjectResource(R.id.tvMotorSpeed)
	private TextView tvMotorSpeed;

	private boolean preRequisites = true;

	public void updateTextView(final TextView view, final String txt) {
		new Handler().post(new Runnable() {
			public void run() {
				view.setText(txt);
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mListener = new IPostListener() {
			@Override
			public void stateUpdate(String value) {
				updateTextView(tvMotorSpeed, value);
			}
		};

		// Bluetooth device exists?
		final BluetoothAdapter mBtAdapter = BluetoothAdapter
		        .getDefaultAdapter();
		if (mBtAdapter == null) {
			preRequisites = false;
			showDialog(NO_BLUETOOTH_ID);
		} else {
			// Bluetooth device is enabled?
			if (!mBtAdapter.isEnabled()) {
				preRequisites = false;
				showDialog(BLUETOOTH_DISABLED);
			}
		}

		// validate app pre-requisites
		if (preRequisites) {
			// prepare service and its connection
			mServiceIntent = new Intent(this, BluetoothService.class);
			mServiceConnection = new BluetoothServiceConnection();
			mServiceConnection.setServiceListener(mListener);

			// bind service
			Log.d(TAG, "Binding service..");
			bindService(mServiceIntent, mServiceConnection,
			        Context.BIND_AUTO_CREATE);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		releaseWakeLockIfHeld();
		mServiceIntent = null;
		mServiceConnection = null;
		mListener = null;

	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "Pausing..");
		releaseWakeLockIfHeld();
	}

	/**
	 * If lock is held, release. Lock will be held when the service is running.
	 */
	private void releaseWakeLockIfHeld() {
		if (wakeLock.isHeld()) {
			wakeLock.release();
		}
	}

	protected void onResume() {
		super.onResume();
		Log.d(TAG, "Resuming..");
		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
		        "ObdReader");
	}

	private void updateConfig() {
		Intent configIntent = new Intent(this, ConfigActivity.class);
		startActivity(configIntent);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, START_LIVE_DATA, 0, "Start Live Data");
		menu.add(0, STOP_LIVE_DATA, 0, "Stop");
		menu.add(0, SETTINGS, 0, "Settings");
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case START_LIVE_DATA:
			startLiveData();
			return true;
		case STOP_LIVE_DATA:
			stopLiveData();
			return true;
		case SETTINGS:
			updateConfig();
			return true;
		}
		return false;
	}

	private void startLiveData() {
		Log.d(TAG, "Starting live data..");

		if (!mServiceConnection.isRunning()) {
			Log.d(TAG, "Service is not running. Going to start it..");
			startService(mServiceIntent);
		}

		// screen won't turn off until wakeLock.release()
		wakeLock.acquire();
	}

	private void stopLiveData() {
		Log.d(TAG, "Stopping live data..");

		if (mServiceConnection.isRunning())
			stopService(mServiceIntent);

		releaseWakeLockIfHeld();
	}

	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder build = new AlertDialog.Builder(this);
		switch (id) {
		case NO_BLUETOOTH_ID:
			build.setMessage("Sorry, your device doesn't support Bluetooth.");
			return build.create();
		case BLUETOOTH_DISABLED:
			build.setMessage("You have Bluetooth disabled. Please enable it!");
			return build.create();
		}
		return null;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem startItem = menu.findItem(START_LIVE_DATA);
		MenuItem stopItem = menu.findItem(STOP_LIVE_DATA);
		MenuItem settingsItem = menu.findItem(SETTINGS);

		// validate if preRequisites are satisfied.
		if (preRequisites) {
			if (mServiceConnection.isRunning()) {
				startItem.setEnabled(false);
				stopItem.setEnabled(true);
				settingsItem.setEnabled(false);
			} else {
				stopItem.setEnabled(false);
				startItem.setEnabled(true);
				settingsItem.setEnabled(true);
			}
		} else {
			startItem.setEnabled(false);
			stopItem.setEnabled(false);
			settingsItem.setEnabled(false);
		}

		return true;
	}

}