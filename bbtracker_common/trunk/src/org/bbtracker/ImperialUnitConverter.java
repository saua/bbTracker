package org.bbtracker;

public class ImperialUnitConverter extends UnitConverter {
	public static final float METER_TO_FOOT_FACTOR = 1 / 0.3048f;

	public static final int FEET_IN_A_MILE = 5280;

	public static final float METER_TO_MILE_FACTOR = METER_TO_FOOT_FACTOR / FEET_IN_A_MILE;

	public static final float MS_TO_MPH_FACTOR = METER_TO_MILE_FACTOR * 3600;

	public String speedToString(final float speed) {
		if (Float.isNaN(speed)) {
			return "- mph";
		}
		final float mph = speed * MS_TO_MPH_FACTOR;
		return String.valueOf(((int) (mph * 10)) / 10f) + " mph";
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
			return String.valueOf(((int) (miles * 10)) / 10f) + "mi";
		}
	}

	public ScaleConfiguration getScaleConfiguration(final double lengthInMeter) {
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

		conf.lengthInUnits = getRoundScaleSize(available);
		conf.lengthInMeter = conf.lengthInUnits / factor;
		return conf;
	}
}
