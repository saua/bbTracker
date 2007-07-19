package org.bbtracker.mobile;

import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.location.Criteria;
import javax.microedition.location.Location;
import javax.microedition.location.LocationException;
import javax.microedition.location.LocationListener;
import javax.microedition.location.LocationProvider;
import javax.microedition.location.QualifiedCoordinates;
import javax.microedition.rms.RecordStoreException;

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;
import org.bbtracker.TrackSegment;

public class TrackManager implements LocationListener {

	public static final int STATE_NOT_INITIALIZED = 1;

	public static final int STATE_INITIALIZED = 2;

	public static final int STATE_TRACKING = 3;

	public static final int STATE_OUT_OF_SERVICE = 4;

	public static final int STATE_TEMPORARILY_UNAVAILABLE = 5;

	public static final int STATE_STATIC = 6;

	private int state;

	protected LocationProvider provider;

	private TrackPoint currentPoint;

	private boolean trackInterrupted;

	private Track track;

	private final Vector listeners = new Vector();

	public TrackManager() {
		state = STATE_NOT_INITIALIZED;
	}

	public void initLocationProvider() throws LocationException {
		if (provider != null) {
			return;
		}
		final Criteria criteria = new Criteria();
		criteria.setAltitudeRequired(true);
		provider = LocationProvider.getInstance(criteria);
		updateSampleInterval();
		state = STATE_INITIALIZED;
	}

	public TrackPoint getCurrentPoint() {
		return currentPoint;
	}

	public Track getTrack() {
		return track;
	}

	public void locationUpdated(final LocationProvider provider, final Location location) {
		if (state == STATE_STATIC) {
			return;
		}
		boolean boundsChanged = false;
		boolean newSegment = false;
		if (location.isValid()) {
			if (trackInterrupted == true) {
				newSegment = true;
				trackInterrupted = false;
			}
			final QualifiedCoordinates coordinates = location.getQualifiedCoordinates();
			final TrackPoint trackPoint = new TrackPoint(location.getTimestamp(), coordinates.getLatitude(),
					coordinates.getLongitude(), coordinates.getAltitude(), location.getSpeed(), location.getCourse(),
					false);
			if (track != null && state != STATE_STATIC) {
				boundsChanged = track.addPoint(trackPoint);
			}
			currentPoint = trackPoint;
			fireNewPoint(currentPoint, boundsChanged, newSegment);
			providerStateChanged(provider, provider.getState());
		} else {
			fireNewPoint(null, boundsChanged, newSegment);
		}
	}

	public void providerStateChanged(final LocationProvider provider, final int newState) {
		final int oldState = state;
		switch (newState) {
		case LocationProvider.AVAILABLE:
			state = STATE_TRACKING;
			break;
		case LocationProvider.OUT_OF_SERVICE:
			state = STATE_OUT_OF_SERVICE;
			trackInterrupted = true;
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			state = STATE_TEMPORARILY_UNAVAILABLE;
			trackInterrupted = true;
			break;
		default:
			return;
		}
		if (state != oldState) {
			fireStateChanged();
		}
	}

	public void addPointListener(final TrackListener listener) {
		listeners.addElement(listener);
	}

	public void removePointListener(final TrackListener listener) {
		listeners.removeElement(listener);
	}

	private void fireNewPoint(final TrackPoint newPoint, final boolean boundsChanged, final boolean newSegment) {
		if (listeners == null) {
			return;
		}

		final Enumeration e = listeners.elements();
		while (e.hasMoreElements()) {
			((TrackListener) e.nextElement()).newPoint(newPoint, boundsChanged, newSegment);
		}
	}

	private void fireStateChanged() {
		if (listeners == null) {
			return;
		}

		final Enumeration e = listeners.elements();
		while (e.hasMoreElements()) {
			((TrackListener) e.nextElement()).stateChanged(state);
		}
	}

	/**
	 * Start a new track.
	 * 
	 * @throws LocationException
	 */
	public void newTrack(final String name) throws LocationException {
		maybeSafeTrack();
		if (provider == null) {
			initLocationProvider();
		}
		updateSampleInterval();

		track = new Track(name);
		state = STATE_TRACKING;
	}

	public void updateSampleInterval() {
		final int sampleInterval = Preferences.getInstance().getSampleInterval();
		try {
			provider.setLocationListener(TrackManager.this, sampleInterval, sampleInterval, -1);
		} catch (final IllegalArgumentException e) {
			provider.setLocationListener(TrackManager.this, -1, -1, -1);
			BBTracker.nonFatal(e, "Trying to set sample interval " + sampleInterval);
		}
	}

	public void setTrack(final Track newTrack) {
		maybeSafeTrack();
		state = STATE_STATIC;
		track = newTrack;
		if (track == null) {
			currentPoint = null;
		} else {
			if (track.getSegmentCount() > 0) {
				final TrackSegment seg = track.getSegment(0);
				if (seg.getPointCount() > 0) {
					currentPoint = seg.getPoint(0);
				}
			}
		}
	}

	public int getState() {
		return state;
	}

	public String getStateString() {
		switch (state) {
		case STATE_NOT_INITIALIZED:
			return "Not Initialized";
		case STATE_INITIALIZED:
			return "Initialized";
		case STATE_TRACKING:
			return "Ok";
		case STATE_OUT_OF_SERVICE:
			return "Out of Service";
		case STATE_TEMPORARILY_UNAVAILABLE:
			return "Temporarily Unavailable";
		case STATE_STATIC:
			return "Static Track";
		default:
			return null;
		}

	}

	public void shutdown() {
		maybeSafeTrack();
		provider = null;
	}

	public void maybeSafeTrack() {
		if (track != null && state != STATE_STATIC) {
			try {
				TrackStore.getInstance().store(track);
			} catch (final RecordStoreException e) {
				BBTracker.nonFatal(e, "Storing track");
			}
		}
	}
}