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

import javax.microedition.location.Criteria;
import javax.microedition.location.Location;
import javax.microedition.location.LocationException;
import javax.microedition.location.QualifiedCoordinates;

import org.bbtracker.TrackPoint;
import org.bbtracker.mobile.Log;

public class Jsr179LocationProvider extends LocationProvider {
	private static final int RECOVERY_DELAY_PER_LEVEL = 2 * 60;

	private javax.microedition.location.LocationProvider provider;

	private final javax.microedition.location.LocationListener locationListener = new javax.microedition.location.LocationListener() {
		public void locationUpdated(javax.microedition.location.LocationProvider provider, Location location) {
			if (location.isValid()) {
				final QualifiedCoordinates coordinates = location.getQualifiedCoordinates();
				final TrackPoint trackPoint = new TrackPoint(location.getTimestamp(), coordinates.getLatitude(),
						coordinates.getLongitude(), coordinates.getAltitude(), location.getSpeed(), location
								.getCourse(), false);
				fireLocationUpdated(trackPoint);
			} else {
				fireLocationUpdated(null);
			}
		}

		public void providerStateChanged(final javax.microedition.location.LocationProvider provider, final int newState) {
			switch (newState) {
			case javax.microedition.location.LocationProvider.AVAILABLE:
				setState(AVAILABLE);
				break;
			case javax.microedition.location.LocationProvider.OUT_OF_SERVICE:
				setState(OUT_OF_SERVICE);
				break;
			case javax.microedition.location.LocationProvider.TEMPORARILY_UNAVAILABLE:
				setState(TEMPORARILY_UNAVAILABLE);
				break;
			default:
				Log.log(this, "Unknown state from LocationProvider: " + newState);
				return;
			}
		}
	};

	public void init() throws org.bbtracker.mobile.gps.LocationException {
		if (provider != null) {
			return;
		}
		final Criteria criteria = new Criteria();
		criteria.setAltitudeRequired(true);
		try {
			provider = javax.microedition.location.LocationProvider.getInstance(criteria);
			applyUpdateInterval();
			setState(AVAILABLE);
		} catch (final LocationException e) {
			Log.log(this, e);
			throw new org.bbtracker.mobile.gps.LocationException("Failed to initialized GPS: " + e.getMessage());
		}
	}

	public void applyUpdateInterval() {
		if (provider == null) {
			return;
		}
		final int updateInterval = getUpdateInterval();
		try {
			Log.log(this, "Setting locationListener with interval " + updateInterval);
			provider.setLocationListener(locationListener, updateInterval, updateInterval, -1);
		} catch (final IllegalArgumentException e) {
			provider.setLocationListener(locationListener, -1, -1, -1);
			Log.log(this, e, "Failed to set updateInterval " + updateInterval + ", using provider default");
		}
	}

	public void setUpdateInterval(final int seconds) {
		super.setUpdateInterval(seconds);
		applyUpdateInterval();
	}

	public int tryRecover(final int escalationLevel) {
		if (escalationLevel == 1) {
			provider.reset();
			applyUpdateInterval();
			return RECOVERY_DELAY_PER_LEVEL;
		} else {
			provider.setLocationListener(null, -1, -1, -1);
			provider = null;
			try {
				init();
				return 0;
			} catch (final org.bbtracker.mobile.gps.LocationException e) {
				Log.log(this, e);
				setState(UNINITIALIZED);
				fireProviderStateChanged();
				return RECOVERY_DELAY_PER_LEVEL * escalationLevel;
			}
		}
	}
}
