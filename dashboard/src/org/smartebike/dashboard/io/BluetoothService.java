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
package org.smartebike.dashboard.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.smartebike.dashboard.R;
import org.smartebike.dashboard.activity.ConfigActivity;
import org.smartebike.dashboard.activity.MainActivity;
import org.smartebike.dashboard.message.Message;
import org.smartebike.dashboard.message.MessageHandler;
import org.smartebike.dashboard.message.MessageKey;
import org.smartebike.dashboard.message.MessageType;

import roboguice.service.RoboService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.inject.Inject;

/**
 * This service is primarily responsible for establishing and maintaining a
 * permanent connection between the device where the application runs and a
 * Bluetooth Smart EBike controller.
 */
public class BluetoothService extends RoboService implements MessageHandler {

	private static final String TAG = "BluetoothService";

	private final IBinder binder = new BluetoothServiceBinder();
	private MessageHandler listener = null;
	private boolean isRunning = false;

	@Inject
	private NotificationManager notificationManager;
	@Inject
	private SharedPreferences prefs;

	private BluetoothDevice btDevice = null;
	private BluetoothSocket btSocket = null;
	/*
	 * http://developer.android.com/reference/android/bluetooth/BluetoothDevice.html
	 * #createRfcommSocketToServiceRecord(java.util.UUID)
	 * 
	 * "Hint: If you are connecting to a Bluetooth serial board then try using
	 * the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB. However if
	 * you are connecting to an Android peer then please generate your own
	 * unique UUID."
	 */
	private static final UUID MY_UUID = UUID
	        .fromString("00001101-0000-1000-8000-00805F9B34FB");

	@Override
	public void onCreate() {
		super.onCreate();
		showNotification();
		Log.d(TAG, "Service started.");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "Destroying BluetoothService...");
		stopLiveData();
		clearNotification();
		listener = null;
		Log.d(TAG, "BluetoothService destroyed.");
	}

	private void startLiveData() {
		Log.d(TAG, "Starting live data..");

		// let's get the remote Bluetooth device
		String remoteDevice = prefs.getString(
		        ConfigActivity.BLUETOOTH_LIST_KEY, null);
		if (remoteDevice == null || "".equals(remoteDevice)) {
			Toast.makeText(this, "No Bluetooth device selected",
			        Toast.LENGTH_LONG).show();

			// log error
			Log.e(TAG, "No Bluetooth device has been selected.");

			// TODO kill this service gracefully
			stopLiveData();
		}

		final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		btDevice = btAdapter.getRemoteDevice(remoteDevice);

		boolean imperialUnits = prefs.getBoolean(
		        ConfigActivity.IMPERIAL_UNITS_KEY, false);

		/*
		 * Establish Bluetooth connection
		 * 
		 * Because discovery is a heavyweight procedure for the Bluetooth
		 * adapter, this method should always be called before attempting to
		 * connect to a remote device with connect(). Discovery is not managed
		 * by the Activity, but is run as a system service, so an application
		 * should always call cancel discovery even if it did not directly
		 * request a discovery, just to be sure. If Bluetooth state is not
		 * STATE_ON, this API will return false.
		 * 
		 * see
		 * http://developer.android.com/reference/android/bluetooth/BluetoothAdapter
		 * .html#cancelDiscovery()
		 */
		Log.d(TAG, "Stopping Bluetooth discovery.");
		btAdapter.cancelDiscovery();

		try {
			startConnection();
		} catch (Exception e) {
			Log.e(TAG, "There was an error while establishing connection. -> "
			        + e.getMessage());

			// in case of failure, stop this service.
			stopLiveData();
		}
	}

	/**
	 * Start and configure the connection to the Smart EBike interface.
	 * 
	 * @throws IOException
	 */
	private void startConnection() throws IOException {
		Log.d(TAG, "Starting Bluetooth connection..");

		// Instantiate a BluetoothSocket for the remote device and connect it.
		btSocket = btDevice.createRfcommSocketToServiceRecord(MY_UUID);
		btSocket.connect();

		isRunning = true;

		// listen socket
		try {
			readBluetoothSocketInput(btSocket.getInputStream());
		} catch (Exception e) {
			Log.e(TAG,
			        "There was an error reading Bluetooth socket inputstream: ",
			        e);
			stopLiveData();
		}
	}

	/**
	 * @throws IOException
	 * 
	 */
	private void readBluetoothSocketInput(InputStream in) throws IOException {
		byte b = 0;
		StringBuilder motorSpeed = new StringBuilder();
		// read until '\n' arrives
		while ((char) (b = (byte) in.read()) != '\n')
			if ((char) b != ' ')
				motorSpeed.append((char) b);

		// received \n so we need to update UI
		Message updateMotorSpeedMessage = new Message(
		        MessageType.UPDATE_MOTOR_SPEED);
		updateMotorSpeedMessage.putExtra(MessageKey.MOTOR_SPEED_VALUE,
		        motorSpeed.toString());
		listener.handleMessage(updateMotorSpeedMessage);

		// restart method
		readBluetoothSocketInput(in);
	}

	/**
	 * Stop Bluetooth connection.
	 */
	public void stopLiveData() {
		if (isRunning) {
			Log.d(TAG, "Stopping live data..");
			// close socket
			try {
				btSocket.close();
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}
			isRunning = false;
		}
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification() {
		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.ic_launcher,
		        getText(R.string.service_started), System.currentTimeMillis());

		// Launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
		        new Intent(this, MainActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this,
		        getText(R.string.notification_label),
		        getText(R.string.service_started), contentIntent);

		// Send the notification.
		notificationManager.notify(R.string.service_started, notification);
	}

	/**
	 * Clear notification.
	 */
	private void clearNotification() {
		notificationManager.cancel(R.string.service_started);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public class BluetoothServiceBinder extends Binder {
		public BluetoothService getService() {
			return BluetoothService.this;
		}
	}

	@Override
	public void registerListener(MessageHandler listener) {
		this.listener = listener;
	}

	/**
	 * Handles received messages.
	 */
	@Override
	public void handleMessage(Message message) {
		switch (message.getMessageType()) {
		case START_LIVE_DATA:
			startLiveData();
			break;
		case STOP_LIVE_DATA:
			stopLiveData();
			break;
		default:
			break;
		}
	}

}