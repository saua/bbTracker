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

	private int currentPointIndex = -1;

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

	public int getCurrentPointIndex() {
		return currentPointIndex;
	}

	public boolean changeCurrentPoint(final int offset) {
		if (track == null) {
			return false;
		}

		int newValue;
		if (currentPoint == null) {
			if (offset >= 0) {
				newValue = offset;
			} else {
				newValue = track.getPointCount() + 1 + offset;
			}
		} else {
			newValue = currentPointIndex + offset;
		}
		if (newValue < 0) {
			newValue = 0;
		} else if (newValue >= track.getPointCount()) {
			newValue = track.getPointCount() - 1;
		}

		final boolean changed = (newValue != currentPointIndex);
		if (changed) {
			currentPointIndex = newValue;
			currentPoint = track.getPoint(currentPointIndex);
			fireCurrentPointChanged();
		}
		return changed;
	}

	public Track getTrack() {
		return track;
	}

	public void locationUpdated(final LocationProvider provider, final Location location) {
		if (state == STATE_STATIC) {
			return;
		}
		if (location.isValid()) {
			boolean boundsChanged = false;
			boolean newSegment = false;
			if (trackInterrupted == true) {
				newSegment = true;
				trackInterrupted = false;
			}
			final QualifiedCoordinates coordinates = location.getQualifiedCoordinates();
			final TrackPoint trackPoint = new TrackPoint(location.getTimestamp(), coordinates.getLatitude(),
					coordinates.getLongitude(), coordinates.getAltitude(), location.getSpeed(), location.getCourse(),
					false);
			if (track != null) {
				final int pointCount = track.getPointCount();
				if (currentPointIndex == pointCount - 1) {
					// activate the new point only, when the last point is currently selected.
					currentPointIndex = pointCount;
					currentPoint = trackPoint;
				}
				boundsChanged = track.addPoint(trackPoint);
			} else {
				currentPoint = trackPoint;
			}
			fireNewPoint(currentPoint, boundsChanged, newSegment);
			fireCurrentPointChanged();
			providerStateChanged(provider, provider.getState());
		} else {
			fireNewPoint(null, false, false);
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

	private void fireCurrentPointChanged() {
		if (listeners == null) {
			return;
		}

		final Enumeration e = listeners.elements();
		while (e.hasMoreElements()) {
			((TrackListener) e.nextElement()).currentPointChanged(currentPoint, currentPointIndex);
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

		currentPointIndex = -1;
		currentPoint = null;
		fireStateChanged();
		fireCurrentPointChanged();
	}

	public void updateSampleInterval() {
		final int sampleInterval = Preferences.getInstance().getSampleInterval();
		try {
			provider.setLocationListener(TrackManager.this, sampleInterval, sampleInterval, -1);
		} catch (final IllegalArgumentException e) {
			provider.setLocationListener(TrackManager.this, -1, -1, -1);
			BBTracker.log(e);
		}
	}

	public void setTrack(final Track newTrack) {
		maybeSafeTrack();
		state = STATE_STATIC;
		track = newTrack;
		if (track == null || track.getPointCount() == 0) {
			currentPoint = null;
			currentPointIndex = -1;
		} else {
			currentPoint = track.getPoint(0);
			currentPointIndex = 0;
		}
		fireStateChanged();
		fireCurrentPointChanged();
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
		if (track != null && state != STATE_STATIC && track.getPointCount() > 0) {
			try {
				TrackStore.getInstance().store(track);
			} catch (final RecordStoreException e) {
				BBTracker.log(e);
			}
		}
	}
}