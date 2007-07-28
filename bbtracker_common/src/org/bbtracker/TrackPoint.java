package org.bbtracker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TrackPoint {
	private long timestamp;

	private double latitude;

	private double longitude;

	private float elevation;

	private float speed;

	private float course;

	private String name;

	private boolean interpolated;

	private boolean standing = false;

	public TrackPoint(final long timestamp, final double latitude, final double longitude, final float elevation,
			final float speed, final float course, final boolean interpolated) {
		this.timestamp = timestamp;
		this.latitude = latitude;
		this.longitude = longitude;
		this.elevation = elevation;
		this.speed = speed;
		this.course = course;
		this.interpolated = interpolated;
		name = null;
	}

	public String getName() {
		return name;
	}

	void setName(final String name) {
		this.name = name;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(final long timestamp) {
		this.timestamp = timestamp;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(final double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(final double longitude) {
		this.longitude = longitude;
	}

	public float getElevation() {
		return elevation;
	}

	public void setElevation(final float elevation) {
		this.elevation = elevation;
	}

	public float getSpeed() {
		return speed;
	}

	public void setSpeed(final float speed) {
		this.speed = speed;
	}

	public float getCourse() {
		return course;
	}

	public void setCourse(final float course) {
		this.course = course;
	}

	public boolean isInterpolated() {
		return interpolated;
	}

	public void setInterpolated(final boolean interpolated) {
		this.interpolated = interpolated;
	}

	public boolean isStanding() {
		return standing;
	}

	public void setStanding(final boolean standing) {
		this.standing = standing;
	}

	public void writeToStream(final DataOutputStream out) throws IOException {
		out.writeLong(timestamp);
		out.writeDouble(latitude);
		out.writeDouble(longitude);
		out.writeFloat(elevation);
		out.writeFloat(speed);
		out.writeFloat(course);
		out.writeBoolean(interpolated);
		out.writeBoolean(standing);
		out.writeUTF(name == null ? "" : name);
	}

	public static TrackPoint readFromStream(final DataInputStream in) throws IOException {
		final long timestamp = in.readLong();
		final double latitude = in.readDouble();
		final double longitude = in.readDouble();
		final float elevation = in.readFloat();
		final float speed = in.readFloat();
		final float course = in.readFloat();
		final boolean interpolated = in.readBoolean();
		final boolean standing = in.readBoolean();
		final String name = in.readUTF();
		final TrackPoint point = new TrackPoint(timestamp, latitude, longitude, elevation, speed, course, interpolated);
		point.setName(name.length() == 0 ? null : name);
		point.setStanding(standing);
		return point;
	}

	public double distance(final TrackPoint point) {

		final double lat1 = Math.toRadians(getLatitude());
		final double lon1 = Math.toRadians(getLongitude());
		final double lat2 = Math.toRadians(point.getLatitude());
		final double lon2 = Math.toRadians(point.getLongitude());

		return Utils.distance(lat1, lon1, lat2, lon2);
	}

}
