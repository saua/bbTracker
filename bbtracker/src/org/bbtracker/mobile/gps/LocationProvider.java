/*
 * Copyright 2007 Joachim Sauer
 * 
 * This file is part of bbTracker.
 * 
 * bbTracker is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * bbTracker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.bbtracker.mobile.gps;

import java.util.TimerTask;

import org.bbtracker.TrackPoint;
import org.bbtracker.mobile.BBTracker;

/**
 * This abstract class is a stripped-down copy of the javax.microedition.location.LocationProvider interface.
 * 
 * It is build so that bbTracker can run on devices without a Location API implementation.
 */
public abstract class LocationProvider {
	public static final int UNINITIALIZED = 0;

	public static final int AVAILABLE = 1;

	public static final int TEMPORARILY_UNAVAILABLE = 2;

	public static final int OUT_OF_SERVICE = 3;

	private int state;

	private LocationListener listener;

	private int updateInterval;

	private int recoveryEscalationLevel = 0;

	private GpsRecoveryTask recoveryTask = new GpsRecoveryTask();

	public int getState() {
		return state;
	}

	public void setLocationListener(final LocationListener listener) {
		this.listener = listener;
		fireProviderStateChanged();
	}

	protected void setState(final int state) {
		if (this.state != state) {
			this.state = state;
			fireProviderStateChanged();
		}
	}

	protected void fireProviderStateChanged() {
		if (listener != null) {
			listener.providerStateChanged(this, state);
		}
		if (state == LocationProvider.AVAILABLE) {
			recoveryTask.cancel();
			// re-scheduling a task seem not to work, when it was canceled before. Create a new one
			recoveryTask = new GpsRecoveryTask();
			recoveryEscalationLevel = 0;
		} else if (state == LocationProvider.TEMPORARILY_UNAVAILABLE && !recoveryTask.running) {
			BBTracker.getTimer().schedule(recoveryTask, 100);
		}
	}

	protected void fireLocationUpdated(final TrackPoint location) {
		if (listener != null) {
			listener.locationUpdated(location);
		}
	}

	public void setUpdateInterval(final int updateInterval) {
		this.updateInterval = updateInterval;
	}

	public int getUpdateInterval() {
		return updateInterval;
	}

	/**
	 * This method is called immediately after the provider is switched to {@link #TEMPORARILY_UNAVAILABLE} from a
	 * Timer.
	 * 
	 * It should attempt to recover Location acquisition and return the number of milliseconds to wait for the next
	 * escalation level. Alternately it can return <code>0</code> to indicate that it should not be called again until
	 * the state enters {@link #TEMPORARILY_UNAVAILABLE} again.
	 * 
	 * @return the number of milliseconds to wait for the next time this will be called or <code>0</code> to indicate
	 *         to never call this again for this loss of connectivity.
	 */
	public abstract int tryRecover(int escalationLevel);

	private class GpsRecoveryTask extends TimerTask {
		boolean running;

		public void run() {
			running = true;
			try {
				recoveryEscalationLevel++;
				final int delay = tryRecover(recoveryEscalationLevel);
				if (delay > 0) {
					BBTracker.getTimer().schedule(this, delay);
				}
			} finally {
				running = false;
			}
		}
	}
}