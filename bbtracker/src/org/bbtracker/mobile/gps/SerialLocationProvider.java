package org.bbtracker.mobile.gps;

import java.util.TimerTask;

import org.bbtracker.TrackPoint;
import org.bbtracker.mobile.BBTracker;
import org.bbtracker.mobile.Log;
import org.bbtracker.mobile.Preferences;

public class SerialLocationProvider extends LocationProvider {
	// wait at most 60 seconds for new NMEA sentences, before attempting recovery
	private static final int NO_NMEA_TIMEOUT = 60 * 1000;

	private final Gps gps;

	private TimerTask updateTask;

	private int lastNmeaCount;

	public SerialLocationProvider() {
		gps = new Gps();
		setState(UNINITIALIZED);
		final String url = Preferences.getInstance().getBluetoothUrl();
		if (url == null || url.length() == 0) {
			Log.log(this, "No URL configured!");
			setState(OUT_OF_SERVICE);
		}
	}

	public void init() throws LocationException {
		final Preferences pref = Preferences.getInstance();
		final String url = pref.getBluetoothUrl();
		if (url == null || url.length() == 0) {
			Log.log(this, "No URL configured!");
			setState(OUT_OF_SERVICE);
		} else {
			gps.open(url);
			setUpdateInterval(pref.getSampleInterval());
			setState(AVAILABLE);
		}
	}

	public void setUpdateInterval(final int updateInterval) {
		super.setUpdateInterval(updateInterval);
		if (updateTask != null) {
			updateTask.cancel();
		}
		updateTask = new UpdateTask();
		BBTracker.getTimer().schedule(updateTask, 0, updateInterval * 1000);
	}

	public int tryRecover(final int escalationLevel) {
		if (gps.isOpen()) {
			// all is well
			return 0;
		} else {
			final String url = Preferences.getInstance().getBluetoothUrl();
			try {
				gps.open(url);
				setState(AVAILABLE);
				return 0;
			} catch (final LocationException e) {
				Log.log(this, e, "Failed to re-open connection to GPS at " + url);
				return escalationLevel * 1000; // wait a second more each level
			}
		}
	}

	private class UpdateTask extends TimerTask {
		private long lastNmeaChangeTimestamp = System.currentTimeMillis();

		public void run() {
			final int nmeaCount = gps.getNmeaCount();
			TrackPoint p;
			final boolean open = gps.isOpen();
			if (!gps.getFix() || nmeaCount == lastNmeaCount || !open) {
				final long lastChange = lastNmeaChangeTimestamp - System.currentTimeMillis();
				Log.log(this, "No new GPS data. Fix: " + gps.getFix() + " nmeaCount: " + nmeaCount +
						" lastNmeaCount: " + lastNmeaCount + " last NMEA change " + lastChange + "ms ago, GPS open? " +
						open);
				if (lastChange >= NO_NMEA_TIMEOUT || !open) {
					setState(TEMPORARILY_UNAVAILABLE);
					return;
				} else {
					p = null;
				}
			} else {
				lastNmeaCount = nmeaCount;
				lastNmeaChangeTimestamp = System.currentTimeMillis();
				p = new TrackPoint(gps.getTimestamp(), gps.getLatitude(), gps.getLongitude(), gps.getAltitude(), gps
						.getSpeed(), gps.getHeading(), false);
			}
			fireLocationUpdated(p);
		}
	}
}