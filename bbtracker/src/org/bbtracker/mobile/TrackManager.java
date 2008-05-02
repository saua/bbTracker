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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Displayable;

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;
import org.bbtracker.Utils;
import org.bbtracker.mobile.TrackStore.TrackStoreEntry;
import org.bbtracker.mobile.TrackStore.TrackStoreException;
import org.bbtracker.mobile.exporter.GpxTrackExporter;
import org.bbtracker.mobile.exporter.KmlTrackExporter;
import org.bbtracker.mobile.exporter.TrackExporter;
import org.bbtracker.mobile.gps.LocationException;
import org.bbtracker.mobile.gps.LocationListener;
import org.bbtracker.mobile.gps.LocationProvider;

public class TrackManager {

	public static final int STATE_NOT_INITIALIZED = 0;

	public static final int STATE_NO_TRACK = 1;

	public static final int STATE_TRACKING = 2;

	public static final int STATE_STATIC = 3;

	private int state;

	// true if no new points are recorded in TRACKING state.
	private boolean paused;

	protected LocationProvider provider;

	private TrackPoint currentPoint;

	private int currentPointIndex = -1;

	private Track track;

	private TrackStore[] trackStores;

	private final Vector listeners = new Vector();

	private boolean trackInterrupted = false;

	private final LocationListener locationListener = new LocationListener() {
		public void locationUpdated(final TrackPoint location) {
			if (state == STATE_STATIC) {
				return;
			}
			if (location != null) {
				boolean boundsChanged = false;
				boolean newSegment = false;
				if (trackInterrupted == true) {
					newSegment = true;
					trackInterrupted = false;
				}
				boolean currentPointChanged;
				if (track != null) {
					currentPointChanged = false;
					if (!paused) {
						final int pointCount = track.getPointCount();
						if (currentPointIndex == pointCount - 1) {
							// activate the new point only, when the last point is currently selected.
							currentPointIndex = pointCount;
							currentPoint = location;
							currentPointChanged = true;
						}
						if (newSegment) {
							track.newSegment();
						}
						boundsChanged = track.addPoint(location);
					}
				} else {
					currentPoint = location;
					currentPointChanged = true;
				}
				fireNewPoint(location, boundsChanged, newSegment);
				if (currentPointChanged) {
					fireCurrentPointChanged();
				}
			} else {
				fireNewPoint(null, false, false);
			}
		}

		public void providerStateChanged(final LocationProvider provider, final int newState) {
			// noop
		}
	};

	public TrackManager() {
		state = STATE_NOT_INITIALIZED;
	}

	void initialize(final LocationProvider provider, final TrackStore[] trackStores) {
		if (state != STATE_NOT_INITIALIZED) {
			throw new IllegalStateException("Trackmanager has already been initialized!");
		}
		if (provider == null) {
			throw new IllegalArgumentException("Must set a valid location provider.");
		}
		if (trackStores == null) {
			throw new IllegalArgumentException("Must set valid trackstores.");
		}
		this.trackStores = trackStores;
		this.provider = provider;
		this.provider.setLocationListener(locationListener);

		state = STATE_NO_TRACK;
		fireStateChanged();
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

	public void pauseTracking() {
		if (state != STATE_TRACKING) {
			throw new IllegalStateException("Not in tracking state! Can't pause tracking!");
		}
		paused = true;
		final int pc = track.getPointCount();
		if (track != null && pc > 0) {
			final TrackPoint p = track.getPoint(pc - 1);
			if (p.getName() == null) {
				p.setName("paused");
			}
		}
	}

	public void continueTracking() {
		if (state != STATE_TRACKING) {
			throw new IllegalStateException("Not in tracking state! Can't continue tracking!");
		}
		trackInterrupted = true;
		paused = false;
	}

	public boolean isPaused() {
		return paused;
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
			throw new LocationException("No location provider is currently configured.");
		}
		updateSampleInterval();

		track = new Track(name);
		state = STATE_TRACKING;
		paused = false;

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
				Log.log(this, e, "saving track");
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
		provider.setUpdateInterval(sampleInterval);
	}

	public int getState() {
		return state;
	}

	public String getStateString() {
		switch (state) {
		case STATE_NOT_INITIALIZED:
			return "Not Initialized";
		case STATE_NO_TRACK:
			return "No Track";
		case STATE_TRACKING:
			return "Tracking";
		case STATE_STATIC:
			return "Static Track";
		default:
			return null;
		}
	}

	public void shutdown() {
		if (provider != null) {
			provider.setLocationListener(null);
			provider = null;
		}
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

		Utils.quicksort(result, TrackStore.TSE_COMPARATOR);

		return result;
	}

	// #ifndef AVOID_FILE_API
	public static int exportTrack(final Track track) throws IOException {
		final Preferences pref = Preferences.getInstance();
		final String dir = pref.getEffectiveExportDirectory();
		int exportCount = 0;
		if (pref.getExportFormat(0)) {
			export(dir, track, new KmlTrackExporter());
			exportCount++;
		}
		if (pref.getExportFormat(1)) {
			export(dir, track, new GpxTrackExporter());
			exportCount++;
		}
		return exportCount;
	}

	private static void export(final String dir, final Track track, final TrackExporter exporter) throws IOException {
		javax.microedition.io.file.FileConnection connection = null;
		OutputStream out = null;
		try {
			connection = FileUtil.createFile(dir, track.getName(), exporter.getExtension());
			out = connection.openOutputStream();
			exporter.export(out, track);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (final IOException ignored) {
					// ignore
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (final IOException ignored) {
					// ignore
				}
			}
		}
	}

	// #endif

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
}