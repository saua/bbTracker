package org.bbtracker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

public class TrackSegment {
	private final Vector points;

	transient double minLatitude = Double.MAX_VALUE;

	transient double maxLatitude = Double.MIN_VALUE;

	transient double minLongitude = Double.MAX_VALUE;

	transient double maxLongitude = Double.MIN_VALUE;

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
		points.addElement(point);
		return boundsChanged;
	}

	public TrackPoint getPoint(final int i) {
		return (TrackPoint) points.elementAt(i);
	}

	public int getPointCount() {
		return points.size();
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
