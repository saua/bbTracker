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

	private transient double minLatitude = Double.MAX_VALUE;

	private transient double maxLatitude = Double.MIN_VALUE;

	private transient double minLongitude = Double.MAX_VALUE;

	private transient double maxLongitude = Double.MIN_VALUE;

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

	public boolean addPoint(final TrackPoint point) {
		final int segmentCount = segments.size();
		final TrackSegment currentSegment;
		if (segmentCount == 0) {
			currentSegment = newSegment();
		} else {
			currentSegment = (TrackSegment) segments.elementAt(segmentCount - 1);
		}
		final boolean boundsChanged = currentSegment.addPoint(point);
		if (boundsChanged) {
			final double lat = point.getLatitude();
			final double lon = point.getLongitude();
			if (lat < minLatitude) {
				minLatitude = lat;
			}
			if (lat > maxLatitude) {
				maxLatitude = lat;
			}
			if (lon < minLongitude) {
				minLongitude = lon;
			}
			if (lon > maxLongitude) {
				maxLongitude = lon;
			}
		}
		pointCount++;
		return boundsChanged;
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
		for (int i = 0; i < segmentCount; i++) {
			final TrackSegment segment = TrackSegment.readFromStream(in);
			track.segments.addElement(segment);
			track.pointCount += segment.getPointCount();
			if (segment.minLatitude < track.minLatitude) {
				track.minLatitude = segment.minLatitude;
			}
			if (segment.maxLatitude > track.maxLatitude) {
				track.maxLatitude = segment.maxLatitude;
			}
			if (segment.minLongitude < track.minLongitude) {
				track.minLongitude = segment.minLongitude;
			}
			if (segment.maxLongitude > track.maxLongitude) {
				track.maxLongitude = segment.maxLongitude;
			}
		}
		track.creationDate = creationDate;
		return track;
	}

	public static String readDescriptionFromStream(final DataInputStream in) throws IOException {
		final int version = in.readInt();
		if (version != streamVersion) {
			return "Wrong version! Got " + version + " instead of " + streamVersion + "!";
		}
		final String name = in.readUTF();
		final Date creationDate = new Date(in.readLong());

		return name + " (" + creationDate + ")";
	}
}
