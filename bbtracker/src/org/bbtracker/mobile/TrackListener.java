package org.bbtracker.mobile;

import org.bbtracker.TrackPoint;

public interface TrackListener {
	public void newPoint(TrackPoint newPoint, boolean boundsChanged, boolean newSegment);

	public void stateChanged(int newState);
}
