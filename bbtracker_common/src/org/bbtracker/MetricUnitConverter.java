package org.bbtracker;

public class MetricUnitConverter extends UnitConverter {
	public static final float MS_TO_KMH_FACTOR = 3.6f;

	// while I'm at putting magic values into constants ...
	public static final int METER_TO_KM_FACTOR = 1000;

	public String speedToString(final float speed) {
		if (Float.isNaN(speed)) {
			return "- km/h";
		}
		final float value = speed * MS_TO_KMH_FACTOR;
		return String.valueOf(((int) (value * 10)) / 10f) + " km/h";
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
			return String.valueOf(((int) (length / 100)) / 10f) + "km";
		}
	}

	public ScaleConfiguration getScaleConfiguration(final double lengthInMeter) {
		final int scaleSize = getRoundScaleSize((int) lengthInMeter);
		final ScaleConfiguration conf = new ScaleConfiguration();
		if (scaleSize >= 1000) {
			conf.unit = "km";
			conf.lengthInUnits = scaleSize / 1000;
		} else {
			conf.unit = "m";
			conf.lengthInUnits = scaleSize;
		}
		conf.lengthInMeter = scaleSize;
		return conf;
	}
}
