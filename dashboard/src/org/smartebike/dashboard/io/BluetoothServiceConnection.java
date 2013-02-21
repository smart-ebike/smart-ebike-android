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

import org.smartebike.dashboard.IPostListener;
import org.smartebike.dashboard.IPostMonitor;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Service connection for BluetoothService.
 */
public class BluetoothServiceConnection implements ServiceConnection {

	private static final String TAG = "BluetoothService";

	private IPostMonitor service = null;
	private IPostListener listener = null;

	public void onServiceConnected(ComponentName name, IBinder binder) {
		service = (IPostMonitor) binder;
		service.setListener(listener);
	}

	public void onServiceDisconnected(ComponentName name) {
		service = null;
		Log.d(TAG, "Service is disconnected.");
	}

	/**
	 * @return true if service is running, false otherwise.
	 */
	public boolean isRunning() {
		return service == null ? false : service.isRunning();
	}

	/**
	 * Sets a callback in the service.
	 * 
	 * @param listener
	 */
	public void setServiceListener(IPostListener listener) {
		this.listener = listener;
	}

}