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
package org.bbtracker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

public class TrackSegment {
	private final Vector points;

	transient double minLatitude = Double.POSITIVE_INFINITY;

	transient double maxLatitude = Double.NEGATIVE_INFINITY;

	transient double minLongitude = Double.POSITIVE_INFINITY;

	transient double maxLongitude = Double.NEGATIVE_INFINITY;

	transient double length = 0;

	TrackSegment() {
		points = new Vector();
	}

	TrackSegment(final int size) {
		points = new Vector(size);
	}

	boolean addPoint(final TrackPoint point) {
		boolean boundsChanged = false;
		final double lat = point.getLatitude();
		final double lon = point.getLongitude();
		if (lat < minLatitude) {
			minLatitude = lat;
			boundsChanged = true;
		}
		if (lat > maxLatitude) {
			maxLatitude = lat;
			boundsChanged = true;
		}
		if (lon < minLongitude) {
			minLongitude = lon;
			boundsChanged = true;
		}
		if (lon > maxLongitude) {
			maxLongitude = lon;
			boundsChanged = true;
		}
		if (points.size() > 0) {
			final TrackPoint prevPoint = (TrackPoint) points.elementAt(points.size() - 1);
			final double dist = Utils.distance(prevPoint.getLatitude(), prevPoint.getLongitude(), lat, lon);
			length += dist;
		}
		points.addElement(point);
		return boundsChanged;
	}

	public TrackPoint getPoint(final int i) {
		return (TrackPoint) points.elementAt(i);
	}

	public int getPointCount() {
		return points.size();
	}

	public double getLength() {
		return length;
	}

	public Enumeration getPoints() {
		return points.elements();
	}

	public void writeToStream(final DataOutputStream out) throws IOException {
		final int pointCount = points.size();
		out.writeInt(pointCount);
		for (int i = 0; i < pointCount; i++) {
			final TrackPoint point = (TrackPoint) points.elementAt(i);
			point.writeToStream(out);
		}
	}

	public static TrackSegment readFromStream(final DataInputStream in) throws IOException {
		final int size = in.readInt();
		final TrackSegment segment = new TrackSegment(size);
		for (int i = 0; i < size; i++) {
			final TrackPoint point = TrackPoint.readFromStream(in);
			segment.addPoint(point);
		}
		return segment;
	}
}
