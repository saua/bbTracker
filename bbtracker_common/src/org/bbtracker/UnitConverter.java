package org.bbtracker;

/**
 * Converts the values and units as handled internally by bbTracker to displayable strings.
 * 
 * @author Joachim Sauer
 */
public abstract class UnitConverter {
	/**
	 * @param speed
	 *            the speed in m/s
	 * @return a human readable String containing the speed including a unit.
	 */
	public abstract String speedToString(final float speed);

	/**
	 * @param elevation
	 *            the elevation in meters
	 * @return a human readable String containing the elevation including a unit.
	 */
	public abstract String elevationToString(final float elevation);

	/**
	 * @param distance
	 *            the distance in meter
	 * @return a human readable String containing the distance including a unit.
	 */
	public abstract String distanceToString(final double distance);

	public abstract ScaleConfiguration getScaleDistance(final double availableMeter);

	public abstract ScaleConfiguration getScaleElevation(final int min, final int max);

	public abstract ScaleConfiguration getScaleSpeed(final double maxSpeed);

	/**
	 * Finds a "round" number that's less than or equal to the boundary.
	 */
	protected int getRoundScaleSize(final int boundary) {
		int scaleSize = 1;

		// find the biggest power of ten that matches
		while (scaleSize < boundary / 10) {
			scaleSize = scaleSize * 10;
		}

		if (scaleSize * 5 < boundary) {
			// see if 5, 50, 500, ... matches
			scaleSize = scaleSize * 5;
		} else if (scaleSize * 2 < boundary) {
			// or at least 2, 20, 200, ... matches
			scaleSize = scaleSize * 2;
		}
		return scaleSize;
	}

	protected static ScaleConfiguration getScaleConfiguration(final String unit, final double min, final double max) {
		final double diff = max - min;
		if (diff == 0) {
			final ScaleConfiguration conf = new ScaleConfiguration();
			conf.unit = unit;
			conf.lengthInSourceUnits = diff;
			conf.labelLocation = new float[] { (float) min };
			conf.labelValue = new float[] { 0.0f };
			return conf;
		}

		final ScaleConfiguration conf = new ScaleConfiguration();
		conf.unit = unit;
		conf.lengthInSourceUnits = diff;

		// calculate approximate tick size (find correct power of ten)
		double tickSize;
		// I wanted to use log10 to find the correct magnitude, but that's not available in CLDC 1.1
		tickSize = 1;
		if (tickSize > diff) {
			while (tickSize > diff) {
				tickSize /= 10;
			}
		} else {
			while (tickSize < diff / 10) {
				tickSize *= 10;
			}
		}

		// adjust to get a reasonable number of ticks (usually between 3 and 6)
		int ticks = (int) (diff / tickSize);
		if (ticks > 5) {
			tickSize *= 2;
		}

		// might be one to big, but that's better than one to small
		ticks = (int) (diff / tickSize) + 1;

		conf.labelLocation = new float[ticks];
		conf.labelValue = new float[ticks];

		// find first tick
		double tMin = (int) (min / tickSize) * tickSize;
		if (tMin < min) {
			tMin += tickSize;
		}

		double t = tMin;
		int i = 0;
		do {
			// adding a multiple results in smaller numerical errors than adding each iteration
			conf.labelValue[i] = (float) t;
			conf.labelLocation[i] = (float) ((t - min) / diff);
			t = tMin + (++i) * tickSize;
		} while (t <= max);

		if (i < ticks) {
			conf.labelLocation[i] = Float.NaN;
			conf.labelValue[i] = Float.NaN;
		}
		return conf;
	}

	public static class ScaleConfiguration {
		ScaleConfiguration() {
		}

		public String unit;

		/**
		 * The values for the labels.
		 * 
		 * Any value might be {@link Float#NaN} to indicate an unused entry (this is used to avoid unnecessary
		 * re-allocation of a smaller array).
		 */
		public float[] labelValue;

		/**
		 * The relative label positions ranging from 0 (minimum) to 1 (maximum).
		 * 
		 * Any value might be {@link Float#NaN} just as in {@link #labelValue}.
		 */
		public float[] labelLocation;

		/**
		 * The length in source units (meter for distance and elevation, m/s for speed)
		 */
		public double lengthInSourceUnits;
	}
}