package org.bbtracker.mobile.gps;

import java.util.TimerTask;

import org.bbtracker.TrackPoint;
import org.bbtracker.mobile.BBTracker;
import org.bbtracker.mobile.Log;
import org.bbtracker.mobile.Preferences;

public class SerialLocationProvider extends LocationProvider {
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
		gps.open(url);
		setUpdateInterval(pref.getSampleInterval());
		setState(AVAILABLE);
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

		public void run() {
			final int nmeaCount = gps.getNmeaCount();
			TrackPoint p;
			Log.log(this, "Update. Fix: " + gps.getFix() + " nmeaCount: " + nmeaCount);
			if (!gps.getFix() || nmeaCount == lastNmeaCount) {
				p = null;
			} else {
				lastNmeaCount = nmeaCount;
				p = new TrackPoint(gps.getTimestamp(), gps.getLatitude(), gps.getLongitude(), gps.getAltitude(), gps
						.getSpeed(), gps.getHeading(), false);
			}
			fireLocationUpdated(p);
		}

	}
}
