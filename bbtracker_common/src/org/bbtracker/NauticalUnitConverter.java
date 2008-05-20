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

public class NauticalUnitConverter extends UnitConverter {
	public static final float METER_TO_FOOT_FACTOR = 1f / 0.3048f;

	public static final float METER_TO_NAUTICAL_MILE_FACTOR = 1f / 1852f;

	public static final float MS_TO_NMH_FACTOR = METER_TO_NAUTICAL_MILE_FACTOR * 3600;

	public String speedToString(final float speed) {
		if (Float.isNaN(speed)) {
			return "-nm/h";
		}
		final float nmh = speed * MS_TO_NMH_FACTOR;
		return Utils.floatToString(nmh, false) + "nm/h";
	}

	public String elevationToString(final float elevation) {
		if (Float.isNaN(elevation)) {
			return "-ft";
		}
		final int feet = (int) (elevation * METER_TO_FOOT_FACTOR);
		return feet + "ft";
	}

	public String distanceToString(final double length) {
		if (Double.isNaN(length)) {
			return "-nm";
		}
		final double nauticalMiles = length * METER_TO_NAUTICAL_MILE_FACTOR;
		return Utils.doubleToString(nauticalMiles, false) + "nm";
	}

	public ScaleConfiguration getScaleDistance(final double lengthInMeter) {
		final double lengthInNauticalMiles = lengthInMeter * METER_TO_NAUTICAL_MILE_FACTOR;
		final int lengthInUnits = getRoundScaleSize((int) lengthInNauticalMiles);

		final ScaleConfiguration conf = new ScaleConfiguration();
		conf.unit = "nm";
		conf.lengthInSourceUnits = lengthInUnits / METER_TO_NAUTICAL_MILE_FACTOR;
		conf.labelLocation = new float[] { 0.0f, 0.5f, 1.0f };
		conf.labelValue = new float[] { 0f, lengthInUnits / 2, lengthInUnits };
		return conf;
	}

	public ScaleConfiguration getScaleElevation(final int min, final int max) {
		return getScaleConfiguration("ft", min * METER_TO_FOOT_FACTOR, max * METER_TO_FOOT_FACTOR);
	}

	public ScaleConfiguration getScaleSpeed(final double maxSpeed) {
		return getScaleConfiguration("nm/h", 0, (float) (maxSpeed * MS_TO_NMH_FACTOR));
	}
}