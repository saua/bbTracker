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
package org.bbtracker.mobile;

import java.util.Enumeration;
import java.util.TimerTask;
import java.util.Vector;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Displayable;
import javax.microedition.location.Criteria;
import javax.microedition.location.Location;
import javax.microedition.location.LocationException;
import javax.microedition.location.LocationListener;
import javax.microedition.location.LocationProvider;
import javax.microedition.location.QualifiedCoordinates;

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;
import org.bbtracker.mobile.TrackStore.TrackStoreEntry;
import org.bbtracker.mobile.TrackStore.TrackStoreException;

public class TrackManager {

	public static final int STATE_NOT_INITIALIZED = 1;

	public static final int STATE_INITIALIZED = 2;

	public static final int STATE_TRACKING = 3;

	public static final int STATE_STATIC = 4;

	private int state;

	protected LocationProvider provider;

	private TrackPoint currentPoint;

	private int currentPointIndex = -1;

	private boolean trackInterrupted;

	private Track track;

	private final TrackStore[] trackStores;

	private final Vector listeners = new Vector();

	private int gpsRecoveryEscalation = 0;

	private final TimerTask gpsRecoveryTask = new GpsRecoveryTask();

	private final LocationListener locationListener = new LocationListener() {
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
						coordinates.getLongitude(), coordinates.getAltitude(), location.getSpeed(), location
								.getCourse(), false);
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
			if (newState == LocationProvider.AVAILABLE) {
				gpsRecoveryTask.cancel();
				gpsRecoveryEscalation = 0;
			} else if (newState == LocationProvider.TEMPORARILY_UNAVAILABLE) {
				BBTracker.getTimer().schedule(gpsRecoveryTask, 100);
			}
			switch (newState) {
			case LocationProvider.OUT_OF_SERVICE:
				trackInterrupted = true;
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				trackInterrupted = true;
				break;
			default:
				return;
			}
		}
	};

	public TrackManager() {
		state = STATE_NOT_INITIALIZED;
		trackStores = new TrackStore[] { new FileTrackStore(), new RMSTrackStore() };
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

	public boolean changeCurrentPoint(final int offset) {
		if (track == null) {
			return false;
		}

		int newValue;
		final int pointCount = track.getPointCount();
		if (pointCount == 0) {
			return false;
		}

		if (currentPoint == null) {
			if (offset >= 0) {
				newValue = offset;
			} else {
				newValue = pointCount + 1 + offset;
			}
		} else {
			newValue = currentPointIndex + offset;
		}
		if (newValue < 0) {
			newValue = 0;
		} else if (newValue >= pointCount) {
			newValue = pointCount - 1;
		}

		return setCurrentPoint(newValue);
	}

	public boolean changeToLastPoint() {
		if (track == null) {
			return false;
		}

		final int pointCount = track.getPointCount();
		if (pointCount == 0) {
			return false;
		}

		return setCurrentPoint(pointCount - 1);
	}

	public boolean changeToFirstPoint() {
		if (track == null || track.getPointCount() == 0) {
			return false;
		}

		return setCurrentPoint(0);
	}

	private boolean setCurrentPoint(final int newValue) {
		final boolean changed = (newValue != currentPointIndex);
		if (changed) {
			currentPointIndex = newValue;
			currentPoint = track.getPoint(currentPointIndex);
			fireCurrentPointChanged();
		}
		return changed;
	}

	public void addPointListener(final TrackListener listener) {
		if (!listeners.contains(listener)) {
			listeners.addElement(listener);
		}
	}

	public void removePointListener(final TrackListener listener) {
		listeners.removeElement(listener);
	}

	public TrackPoint getCurrentPoint() {
		return currentPoint;
	}

	public int getCurrentPointIndex() {
		return currentPointIndex;
	}

	public Track getTrack() {
		return track;
	}

	/**
	 * Start a new track.
	 * 
	 * @throws LocationException
	 * @throws IllegalStateException
	 *             if the {@link #state} is {@link #STATE_TRACKING}.
	 */
	public void newTrack(final String name) throws IllegalStateException, LocationException {
		if (state == STATE_TRACKING) {
			throw new IllegalStateException("Can't start new track, when in STATE_TRACKING");
		}
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

	/**
	 * Sets the current track. This also sets the state to STATE_TRACKING.
	 * 
	 * @throws IllegalStateException
	 *             if the {@link #state} is {@link #STATE_TRACKING}.
	 */
	public void setTrack(final Track newTrack) throws IllegalStateException {
		if (state == STATE_TRACKING) {
			throw new IllegalStateException("Can't set track, when in STATE_TRACKING");
		}
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

	/**
	 * Saves the currently recording track, if any. This is different from {@link #saveTrack()} in that it is a no-op if
	 * {@link #state} is not {@link #STATE_TRACKING}.
	 * 
	 * @throws TrackStoreException
	 *             if saving fails
	 * 
	 */
	public void maybeSaveTrack() throws TrackStoreException {
		if (state != STATE_TRACKING) {
			return;
		}

		saveTrack();
	}

	/**
	 * Saves the currently recording track.
	 * 
	 * @throws IllegalStateException
	 *             if {@link #state} is not {@link #STATE_TRACKING}.
	 * @throws TrackStoreException
	 *             if saving fails
	 */
	public void saveTrack() throws IllegalStateException, TrackStoreException {
		if (state != STATE_TRACKING) {
			throw new IllegalStateException("Can't save a track, when not in STATE_TRACKING!");
		}

		String error = null;
		boolean success = false;
		for (int i = 0; i < trackStores.length; i++) {
			try {
				trackStores[i].saveTrack(track);
				success = true;
				break;
			} catch (final TrackStoreException e) {
				BBTracker.log(this, e, "saving track");
				final String msg = e.getMessage();
				error = error == null ? msg : error + "\n" + msg;
			}
		}

		if (success) {
			state = STATE_STATIC;
			fireStateChanged();
		} else {
			throw new TrackStoreException(error);
		}
	}

	public static void showSaveFailedAlert(final TrackStoreException e, final Displayable next) {
		BBTracker.alert(new Alert("Failed to save track!", "Failed to safe track:\n" + e.getMessage(), null,
				AlertType.ERROR), next);
	}

	public void updateSampleInterval() {
		if (provider == null) {
			return;
		}
		final int sampleInterval = Preferences.getInstance().getSampleInterval();
		try {
			provider.setLocationListener(locationListener, sampleInterval, sampleInterval, -1);
		} catch (final IllegalArgumentException e) {
			provider.setLocationListener(locationListener, -1, -1, -1);
			BBTracker.log(this, e);
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
			return "Tracking";
		case STATE_STATIC:
			return "Static Track";
		default:
			return null;
		}
	}

	public void shutdown() {
		provider = null;
	}

	public TrackStoreEntry[] getEntries() throws TrackStoreException {
		final TrackStoreEntry[][] entries = new TrackStoreEntry[trackStores.length][];
		int count = 0;
		for (int i = 0; i < trackStores.length; i++) {
			entries[i] = trackStores[i].getEntries();
			count += entries[i].length;
		}
		final TrackStoreEntry[] result = new TrackStoreEntry[count];
		int offset = 0;
		for (int i = 0; i < trackStores.length; i++) {
			System.arraycopy(entries[i], 0, result, offset, entries[i].length);
			offset += entries[i].length;
		}

		quicksort(result, 0, result.length - 1);

		return result;
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

	// Wee! A QuickSort implementation!
	// with inspiration from Wikipedia, I was to lazy to implement it myself
	private static void quicksort(final TrackStoreEntry[] array, final int left, final int right) {
		if (right > left) {
			int pivotIndex = left;
			final TrackStoreEntry pivotValue = array[pivotIndex];
			swap(array, pivotIndex, right);
			int storeIndex = left - 1;
			for (int i = left; i < right - 1; i++) {
				if (array[i].compareTo(pivotValue) < 0) {
					storeIndex++;
					swap(array, storeIndex, i);
				}
			}
			swap(array, right, storeIndex + 1);
			pivotIndex = storeIndex + 1;

			quicksort(array, left, pivotIndex - 1);
			quicksort(array, pivotIndex + 1, right);
		}
	}

	private static void swap(final TrackStoreEntry[] array, final int i1, final int i2) {
		final TrackStoreEntry e = array[i1];
		array[i1] = array[i2];
		array[i2] = e;
	}

	private class GpsRecoveryTask extends TimerTask {
		private static final long FIRST_DELAY = 2 * 60;

		private static final long DELAY_PER_LEVEL = 2 * 60;

		public void run() {
			gpsRecoveryEscalation++;
			if (gpsRecoveryEscalation == 1) {
				provider.reset();
				updateSampleInterval();
				BBTracker.getTimer().schedule(this, FIRST_DELAY);
			} else {
				provider.setLocationListener(null, -1, -1, -1);
				provider = null;
				try {
					final int oldState = state;
					state = STATE_NOT_INITIALIZED;
					initLocationProvider();
					state = oldState;
				} catch (final LocationException e) {
					BBTracker.log(this, e);
					BBTracker.getTimer().schedule(this, DELAY_PER_LEVEL * gpsRecoveryEscalation);
					fireStateChanged();
				}
			}
		}
	}
}