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
package org.bbtracker.mobile.gui;

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;

public abstract class DataProvider {
	public static final DataProvider TIME = new DataProvider() {
		public String getName() {
			return "time";
		}

		public double getMaxValue(Track track) {
			return getValue(track.getPoint(track.getPointCount() - 1));
		}

		public double getMinValue(Track track) {
			return getValue(track.getPoint(0));
		}

		public double getValue(TrackPoint point) {
			return point.getTimestamp();
		}

		public double getSmallDelta() {
			// one second should be sufficiently small
			return 1000d;
		}
	};

	public static final DataProvider SPEED = new DataProvider() {
		public String getName() {
			return "speed";
		}

		public double getMaxValue(Track track) {
			return track.getMaxSpeed();
		}

		public double getMinValue(Track track) {
			return 0d;
		}

		public double getValue(TrackPoint point) {
			return point.getSpeed();
		}

		public double getSmallDelta() {
			// 1 km/h is reasonably small
			return 1 / 3.6d;
		}

	};

	public static final DataProvider ELEVATION = new DataProvider() {
		public String getName() {
			return "elevation";
		}

		public double getMaxValue(Track track) {
			return track.getMaxElevation();
		}

		public double getMinValue(Track track) {
			return track.getMinElevation();
		}

		public double getValue(TrackPoint point) {
			return point.getElevation();
		}

		public double getSmallDelta() {
			// 1 m is reasonably small
			return 1;
		}

	};

	public static final DataProvider LONGITUDE = new DataProvider() {
		public String getName() {
			return "longitude";
		}

		public double getMaxValue(Track track) {
			return track.getMaxLongitude();
		}

		public double getMinValue(Track track) {
			return track.getMinLongitude();
		}

		public double getValue(TrackPoint point) {
			return point.getLongitude();
		}

		public double getSmallDelta() {
			// 0.25 seconds should be fine
			return (1 / 60) / 4;
		}
	};

	public static final DataProvider LATITUDE = new DataProvider() {
		public String getName() {
			return "latitude";
		}

		public double getMaxValue(Track track) {
			return track.getMaxLatitude();
		}

		public double getMinValue(Track track) {
			return track.getMinLatitude();
		}

		public double getValue(TrackPoint point) {
			return point.getLatitude();
		}

		public double getSmallDelta() {
			// 0.25 seconds should be fine
			return (1 / 60) / 4;
		}
	};

	/**
	 * Returns the name of this data provider.
	 */
	public abstract String getName();

	/**
	 * Returns the biggest value found in the track.
	 */
	public abstract double getMaxValue(final Track track);

	/**
	 * Returns the smallest value found in the track.
	 */
	public abstract double getMinValue(final Track track);

	/**
	 * Returns the value of the specified point.
	 */
	public abstract double getValue(final TrackPoint point);

	/**
	 * Returns a "small delta" for the value. This should represent a reasonable default unit that can be used to define
	 * the minimal range to be viewed or similar constraints.
	 */
	public abstract double getSmallDelta();
}