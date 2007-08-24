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
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

public class Track {
	private final Vector segments;

	private String name;

	private Date creationDate;

	private transient int pointCount;

	private transient double minLatitude = Double.POSITIVE_INFINITY;

	private transient double maxLatitude = Double.NEGATIVE_INFINITY;

	private transient double minLongitude = Double.POSITIVE_INFINITY;

	private transient double maxLongitude = Double.NEGATIVE_INFINITY;

	private transient float maxSpeed = Float.NaN;

	private transient float minElevation = Float.NaN;

	private transient float maxElevation = Float.NaN;

	public Track(final String name) {
		this.name = name;
		creationDate = new Date();
		segments = new Vector();
		pointCount = 0;
	}

	private Track(final String name, final int size) {
		this.name = name;
		creationDate = new Date();
		segments = new Vector(size);
		pointCount = 0;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public int getPointCount() {
		return pointCount;
	}

	public TrackPoint getPoint(final int nr) {
		if (nr < 0 || nr >= pointCount) {
			throw new IndexOutOfBoundsException("no such point: " + nr + ", must be >= 0 and < " + pointCount);
		}
		int i = nr;
		final Enumeration segs = segments.elements();
		while (segs.hasMoreElements()) {
			final TrackSegment seg = (TrackSegment) segs.nextElement();
			final int count = seg.getPointCount();
			if (i < count) {
				return seg.getPoint(i);
			} else {
				i -= count;
			}
		}
		throw new IllegalStateException("pointCount corrupt!");
	}

	public long getPointOffset(final TrackPoint point) {
		if (getPointCount() == 0) {
			throw new IllegalStateException("No points yet, can't calculate point offset.");
		}
		return point.getTimestamp() - getPoint(0).getTimestamp();
	}

	public int getSegmentCount() {
		return segments.size();
	}

	public TrackSegment getSegment(final int i) {
		return (TrackSegment) segments.elementAt(i);
	}

	public Enumeration getSegments() {
		return segments.elements();
	}

	public double getMaxLatitude() {
		return maxLatitude;
	}

	public double getMinLatitude() {
		return minLatitude;
	}

	public double getMaxLongitude() {
		return maxLongitude;
	}

	public double getMinLongitude() {
		return minLongitude;
	}

	public double getLength() {
		final Enumeration e = segments.elements();
		double length = 0d;
		while (e.hasMoreElements()) {
			length += ((TrackSegment) e.nextElement()).getLength();
		}
		return length;
	}

	public float getMaxSpeed() {
		return maxSpeed;
	}

	public float getMaxElevation() {
		return maxElevation;
	}

	public float getMinElevation() {
		return minElevation;
	}

	public boolean addPoint(final TrackPoint point) {
		final int segmentCount = segments.size();
		final TrackSegment currentSegment;
		if (segmentCount == 0) {
			currentSegment = newSegment();
		} else {
			currentSegment = (TrackSegment) segments.elementAt(segmentCount - 1);
		}
		point.setIndex(pointCount);
		final boolean boundsChanged = currentSegment.addPoint(point);
		if (boundsChanged) {
			final double lat = point.getLatitude();
			final double lon = point.getLongitude();
			updateMinCoordinates(lat, lon);
			updateMaxCoordinates(lat, lon);
		}
		updateSpeedAndElevation(point.getSpeed(), point.getElevation());
		pointCount++;
		return boundsChanged;
	}

	private void updateMinCoordinates(final double lat, final double lon) {
		if (lat < minLatitude) {
			minLatitude = lat;
		}
		if (lon < minLongitude) {
			minLongitude = lon;
		}
	}

	private void updateMaxCoordinates(final double lat, final double lon) {
		if (lat > maxLatitude) {
			maxLatitude = lat;
		}
		if (lon > maxLongitude) {
			maxLongitude = lon;
		}
	}

	private void updateSpeedAndElevation(final float speed, final float elevation) {
		if (!Float.isNaN(speed) && (Float.isNaN(maxSpeed) || speed > maxSpeed)) {
			maxSpeed = speed;
		}
		if (!Float.isNaN(elevation)) {
			if (Float.isNaN(minElevation) || elevation < minElevation) {
				minElevation = elevation;
			}
			if (Float.isNaN(maxElevation) || elevation > maxElevation) {
				maxElevation = elevation;
			}

		}

	}

	public TrackSegment newSegment() {
		if (segments.size() != 0) {
			final TrackSegment lastSegment = (TrackSegment) segments.elementAt(segments.size());
			if (lastSegment.getPointCount() == 0) {
				return lastSegment;
			}
		}
		final TrackSegment newSegment = new TrackSegment();
		segments.addElement(newSegment);
		return newSegment;
	}

	private static final int streamVersion = 1;

	public void writeToStream(final DataOutputStream out) throws IOException {
		out.writeInt(streamVersion);
		out.writeUTF(name == null ? "" : name);
		out.writeLong(creationDate.getTime());
		out.writeInt(segments.size());
		for (int i = 0; i < segments.size(); i++) {
			final TrackSegment segment = (TrackSegment) segments.elementAt(i);
			segment.writeToStream(out);
		}
	}

	public static Track readFromStream(final DataInputStream in) throws IOException {
		final int version = in.readInt();
		if (version != streamVersion) {
			return null;
		}
		final String name = in.readUTF();
		final Date creationDate = new Date(in.readLong());
		final int segmentCount = in.readInt();
		final Track track = new Track(name.length() == 0 ? null : name, segmentCount);
		int p = 0;
		for (int i = 0; i < segmentCount; i++) {
			final TrackSegment segment = TrackSegment.readFromStream(in);
			track.segments.addElement(segment);
			track.pointCount += segment.getPointCount();
			track.updateMinCoordinates(segment.minLatitude, segment.minLongitude);
			track.updateMaxCoordinates(segment.maxLatitude, segment.maxLongitude);
			final Enumeration points = segment.getPoints();
			while (points.hasMoreElements()) {
				final TrackPoint point = (TrackPoint) points.nextElement();
				point.setIndex(p++);
				track.updateSpeedAndElevation(point.getSpeed(), point.getElevation());
			}
		}
		track.creationDate = creationDate;
		return track;
	}

	public static String readNameFromStream(final DataInputStream in) throws IOException {
		final int version = in.readInt();
		if (version != streamVersion) {
			return "Wrong version! Got " + version + " instead of " + streamVersion + "!";
		}
		final String name = in.readUTF();

		return name;
	}

	/**
	 * Can only be used immediately after {@link #readDateFromStream(DataInputStream)}
	 */
	public static Date readDateFromStream(final DataInputStream in) throws IOException {
		final Date creationDate = new Date(in.readLong());
		return creationDate;
	}
}
