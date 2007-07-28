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

	public abstract ScaleConfiguration getScaleConfiguration(final double availableMeter);

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

	public static class ScaleConfiguration {
		public String unit;

		public int lengthInUnits;

		public double lengthInMeter;
	}
}