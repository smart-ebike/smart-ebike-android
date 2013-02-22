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
package org.smartebike.dashboard.message;

/**
 * This interface shall be implemented by Activities and Services that need to
 * be able to send and receive {@link Message} from each other.
 * <p>
 * This only works with Services sharing the same thread where the Activity is
 * running.
 */
public interface MessageHandler {

	/**
	 * Handle received message.
	 * 
	 * @param message
	 *            - the received message to handle.
	 */
	void handleMessage(Message message);

	/**
	 * Register an implementation of {@link MessageHandler} as a listener for
	 * sent messages.
	 * 
	 * @param handler
	 */
	void registerListener(MessageHandler listener);

}