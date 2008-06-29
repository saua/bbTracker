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

public class TrackPoint {
	private long timestamp;

	private double latitude;

	private double longitude;

	private float elevation;

	private float speed;

	private float course;

	private int heartRate;

	private double distance;

	private String name;

	private final byte satellites;

	private boolean standing = false;

	private transient int index;

	public TrackPoint(final long timestamp, final double latitude, final double longitude, final float elevation,
			final float speed, final float course, final byte satellites) {
		this.timestamp = timestamp;
		this.latitude = latitude;
		this.longitude = longitude;
		this.elevation = elevation;
		this.speed = speed;
		this.course = course;
		this.satellites = satellites;
		name = null;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
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

	public boolean isStanding() {
		return standing;
	}

	public void setStanding(final boolean standing) {
		this.standing = standing;
	}

	void setIndex(final int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	void setDistance(final double distance) {
		this.distance = distance;
	}

	public double getDistance() {
		return distance;
	}

	public byte getSatellites() {
		return satellites;
	}

	public void writeToStream(final DataOutputStream out) throws IOException {
		out.writeLong(timestamp);
		out.writeDouble(latitude);
		out.writeDouble(longitude);
		out.writeFloat(elevation);
		out.writeFloat(speed);
		out.writeFloat(course);
		// we're now writing the number of satellites instead of the (unused)
		// interpolated property
		// since interpolated was always 0 this shouldn't be a problem.
		out.writeByte((byte) heartRate);
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
		final byte hrByte = in.readByte();
		// final byte satellites = in.readByte();
		final int heartRate;
		if (hrByte < 0) {
			heartRate = hrByte + 256;
		} else {
			heartRate = hrByte;
		}
		final byte satellites = 0;
		final boolean standing = in.readBoolean();
		final String name = in.readUTF();
		final TrackPoint point = new TrackPoint(timestamp, latitude, longitude, elevation, speed, course, satellites);
		point.setName(name.length() == 0 ? null : name);
		point.setStanding(standing);
		point.setHeartRate(heartRate);
		return point;
	}

	public double distance(final TrackPoint point) {

		final double lat1 = Math.toRadians(getLatitude());
		final double lon1 = Math.toRadians(getLongitude());
		final double lat2 = Math.toRadians(point.getLatitude());
		final double lon2 = Math.toRadians(point.getLongitude());

		return Utils.distance(lat1, lon1, lat2, lon2);
	}

	public int getHeartRate() {
		return heartRate;
	}

	public void setHeartRate(final int heartRate) {
		this.heartRate = heartRate;
	}
}
