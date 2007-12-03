package org.bbtracker.mobile.gps;

import org.bbtracker.mobile.Log;
import org.bbtracker.mobile.Preferences;

public class SerialLocationProvider extends LocationProvider {
	private final Gps gps;

	public SerialLocationProvider() {
		gps = new Gps();
		setState(UNINITIALIZED);
		final String bluetoothAddress = Preferences.getInstance().getBluetoothUrl();
		final String url = bluetoothAddress;
		if (url == null || url.length() == 0) {
			Log.log(this, "No URL configured!");
			setState(OUT_OF_SERVICE);
		} else {
			try {
				gps.open(url);
				setState(AVAILABLE);
			} catch (final LocationException e) {
				Log.log(this, e, "Failed to open connection to GPS at " + url);
			}
		}
	}

	public void setUpdateInterval(final int updateInterval) {
		super.setUpdateInterval(updateInterval);

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
}
