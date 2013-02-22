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

import org.smartebike.dashboard.R;
import org.smartebike.dashboard.io.BluetoothService;
import org.smartebike.dashboard.message.Message;
import org.smartebike.dashboard.message.MessageHandler;
import org.smartebike.dashboard.message.MessageKey;
import org.smartebike.dashboard.message.MessageType;

import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
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
public class MainActivity extends RoboActivity implements MessageHandler {

	private static final String TAG = "MainActivity";

	private static final int NO_BLUETOOTH_ID = 0;
	private static final int BLUETOOTH_DISABLED = 1;
	private static final int START_LIVE_DATA = 3;
	private static final int STOP_LIVE_DATA = 4;
	private static final int SETTINGS = 5;

	private boolean isServiceBound;

	@Inject
	BluetoothService btService;

	private ServiceConnection serviceConn = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(TAG, "BluetoothService is bound");
			// register this activity as a service listener
			btService.registerListener(MainActivity.this);
			isServiceBound = true;
		}

		public void onServiceDisconnected(ComponentName className) {
			Log.d(TAG, "BluetoothService is unbound");
			isServiceBound = false;
		}
	};

	@Inject
	private SharedPreferences prefs;
	@Inject
	private PowerManager powerManager;
	private PowerManager.WakeLock wakeLock;

	@InjectView(R.id.tvMotorSpeed)
	private TextView tvMotorSpeed;

	private boolean preRequisites = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

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
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "Destroying..");
		try {
			doUnbindService();
		} catch (Throwable t) {
			Log.e(TAG, "Failed to unbind from the service", t);
		}
		releaseWakeLockIfHeld();
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "Starting..");

		// start service
		if (!isServiceBound)
			doBindService();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "Resuming..");

		if (isServiceBound)
			Log.d(TAG, "BluetoothService is bound...");

		// get wakelock
		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
		        "Smart EBike");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "Pausing..");

		if (isServiceBound)
			Log.d(TAG, "BluetoothService is bound...");

		releaseWakeLockIfHeld();
	}

	private void doBindService() {
		if (!isServiceBound) {
			Log.d(TAG, "Binding BluetoothService..");
			Intent serviceIntent = new Intent(this, BluetoothService.class);
			bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
		}
	}

	private void doUnbindService() {
		if (isServiceBound) {
			Log.d(TAG, "Unbinding BluetoothService..");
			unbindService(serviceConn);
		}
	}

	/**
	 * If lock is held, release. Lock will be held when the service is running.
	 */
	private void releaseWakeLockIfHeld() {
		if (wakeLock.isHeld())
			wakeLock.release();
	}

	/**
	 * Starts bluetooth connection and reads incoming data
	 */
	private void startLiveData() {
		Log.d(TAG, "Starting live data..");
		if (isServiceBound)
			if (btService != null)
				btService
				        .handleMessage(new Message(MessageType.START_LIVE_DATA));

		// screen won't turn off until wakeLock.release()
		wakeLock.acquire();
	}

	/**
	 * Stops bluetooth connection
	 */
	private void stopLiveData() {
		Log.d(TAG, "Stopping live data..");

		if (isServiceBound)
			if (btService != null)
				btService
				        .handleMessage(new Message(MessageType.STOP_LIVE_DATA));

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

	private void updateConfig() {
		Intent configIntent = new Intent(this, ConfigActivity.class);
		startActivity(configIntent);
	}

	/**
	 * Handles incoming {@link Message}.
	 */
	@Override
	public void handleMessage(Message message) {
		switch (message.getMessageType()) {
		case UPDATE_MOTOR_SPEED:
			tvMotorSpeed
			        .setText(message.getExtra(MessageKey.MOTOR_SPEED_VALUE));
			break;
		default:
			break;
		}
	}

	@Override
	public void registerListener(MessageHandler listener) {
		// not needed
	}

}