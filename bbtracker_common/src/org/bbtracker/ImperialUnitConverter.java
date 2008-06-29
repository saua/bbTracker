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

public class ImperialUnitConverter extends UnitConverter {
	public static final float METER_TO_FOOT_FACTOR = 1 / 0.3048f;

	public static final int FEET_IN_A_MILE = 5280;

	public static final float METER_TO_MILE_FACTOR = METER_TO_FOOT_FACTOR / FEET_IN_A_MILE;

	public static final float MS_TO_MPH_FACTOR = METER_TO_MILE_FACTOR * 3600;

	public String speedToString(final float speed) {
		if (Float.isNaN(speed)) {
			return "-mph";
		}
		final float mph = speed * MS_TO_MPH_FACTOR;
		return Utils.floatToString(mph, false) + "mph";
	}

	/**
	 * @return template used to compute the width of the speed widget
	 */
	public String getSpeedTemplate() {
		return "999.9mph";
	}

	/**
	 * @return template used to compute the width of the elevation widget
	 */
	public String getElevationTemplate() {
		return "99999ft";
	}

	/**
	 * @return template used to compute the width of the distance widget
	 */
	public String getDistanceTemplate() {
		return "999.9ft";
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
			return "-mi";
		}
		final double miles = length * METER_TO_MILE_FACTOR;
		if (miles < 1) {
			final int feet = (int) (length * METER_TO_FOOT_FACTOR);
			return feet + "ft";
		} else {
			return Utils.doubleToString(miles, false) + "mi";
		}
	}

	public ScaleConfiguration getScaleDistance(final double lengthInMeter) {
		final double lengthInFoot = lengthInMeter * METER_TO_FOOT_FACTOR;
		double factor = METER_TO_FOOT_FACTOR;
		final ScaleConfiguration conf = new ScaleConfiguration();
		int available;
		if (lengthInFoot < FEET_IN_A_MILE) {
			// feet
			conf.unit = "ft";
			available = (int) lengthInFoot;
		} else {
			// miles
			conf.unit = "mi";
			factor = factor / FEET_IN_A_MILE;
			available = (int) (lengthInFoot / FEET_IN_A_MILE);
		}

		final int lengthInUnits = getRoundScaleSize(available);
		conf.lengthInSourceUnits = lengthInUnits / factor;
		conf.labelLocation = new float[] { 0.0f, 0.5f, 1.0f };
		conf.labelValue = new float[] { 0f, lengthInUnits / 2, lengthInUnits };
		return conf;
	}

	public ScaleConfiguration getScaleElevation(final int min, final int max) {
		return getScaleConfiguration("ft", min * METER_TO_FOOT_FACTOR, max * METER_TO_FOOT_FACTOR);
	}

	public ScaleConfiguration getScaleSpeed(final double maxSpeed) {
		return getScaleConfiguration("mph", 0, (float) (maxSpeed * MS_TO_MPH_FACTOR));
	}
}