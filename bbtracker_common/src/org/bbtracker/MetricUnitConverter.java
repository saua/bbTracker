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

public class MetricUnitConverter extends UnitConverter {
	public static final float MS_TO_KMH_FACTOR = 3.6f;

	// while I'm at putting magic values into constants ...
	public static final int METER_TO_KM_FACTOR = 1000;

	public String speedToString(final float speed) {
		if (Float.isNaN(speed)) {
			return "-km/h";
		}
		final float value = speed * MS_TO_KMH_FACTOR;
		return Utils.floatToString(value, false) + "km/h";
	}

	public String elevationToString(final float elevation) {
		if (Float.isNaN(elevation)) {
			return "-m";
		}
		return ((int) elevation) + "m";
	}

	public String distanceToString(final double length) {
		if (Double.isNaN(length)) {
			return "-km";
		} else if (length < METER_TO_KM_FACTOR) {
			return ((int) length) + "m";
		} else {
			return Utils.doubleToString(length / 1000, false) + "km";
		}
	}

	public ScaleConfiguration getScaleDistance(final double lengthInMeter) {
		final int scaleSize = getRoundScaleSize((int) lengthInMeter);
		final ScaleConfiguration conf = new ScaleConfiguration();
		int lengthInUnits;
		if (scaleSize >= 1000) {
			conf.unit = "km";
			lengthInUnits = scaleSize / 1000;
		} else {
			conf.unit = "m";
			lengthInUnits = scaleSize;
		}
		conf.lengthInSourceUnits = scaleSize;
		conf.labelLocation = new float[] { 0.0f, 0.5f, 1.0f };
		conf.labelValue = new float[] { 0f, lengthInUnits / 2f, lengthInUnits };
		return conf;
	}

	public ScaleConfiguration getScaleElevation(final int min, final int max) {
		return getScaleConfiguration("m", min, max);
	}

	public ScaleConfiguration getScaleSpeed(final double maxSpeed) {
		return getScaleConfiguration("km/h", 0f, (float) (maxSpeed * MS_TO_KMH_FACTOR));
	}
}
