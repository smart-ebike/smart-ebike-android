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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.smartebike.dashboard.IPostListener;
import org.smartebike.dashboard.IPostMonitor;
import org.smartebike.dashboard.R;
import org.smartebike.dashboard.activity.ConfigActivity;
import org.smartebike.dashboard.activity.MainActivity;

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
public class BluetoothService extends RoboService {

	private static final String TAG = "BluetoothService";

	private IPostListener listener = null;
	private AtomicBoolean isRunning = new AtomicBoolean(false);

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

	/**
	 * As long as the service is bound to another component, say an Activity, it
	 * will remain alive.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return new LocalBinder();
	}

	@Override
	public void onCreate() {
		showNotification();
	}

	@Override
	public void onDestroy() {
		stopService();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "Received start id " + startId + ": " + intent);
		startService();

		// service will die if activity dies
		return START_NOT_STICKY;
	}

	private void startService() {
		Log.d(TAG, "Starting service..");

		/*
		 * Let's get the remote Bluetooth device
		 */
		String remoteDevice = prefs.getString(
		        ConfigActivity.BLUETOOTH_LIST_KEY, null);
		if (remoteDevice == null || "".equals(remoteDevice)) {
			Toast.makeText(this, "No Bluetooth device selected",
			        Toast.LENGTH_LONG).show();

			// log error
			Log.e(TAG, "No Bluetooth device has been selected.");

			// TODO kill this service gracefully
			stopService();
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

		Toast.makeText(this, "Starting Bluetooth connection..",
		        Toast.LENGTH_SHORT).show();

		try {
			startConnection();
		} catch (Exception e) {
			Log.e(TAG, "There was an error while establishing connection. -> "
			        + e.getMessage());

			// in case of failure, stop this service.
			stopService();
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

		// TODO register listener for socket input
	}

	/**
	 * 
	 */
	private void readBluetoothSocketInput() {
	}

	/**
	 * Stop Bluetooth connection.
	 */
	public void stopService() {
		Log.d(TAG, "Stopping service..");

		clearNotification();
		listener = null;
		isRunning.set(false);

		// close socket
		try {
			btSocket.close();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}

		// kill service
		stopSelf();
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

	/**
	 * TODO put description
	 */
	public class LocalBinder extends Binder implements IPostMonitor {
		public void setListener(IPostListener callback) {
			listener = callback;
		}

		public boolean isRunning() {
			return isRunning.get();
		}

	}

}