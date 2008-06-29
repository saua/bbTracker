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

/**
 * Converts the values and units as handled internally by bbTracker to
 * displayable strings.
 * 
 * @author Joachim Sauer
 */
public abstract class UnitConverter {
	/**
	 * @return template used to compute the width of the speed widget
	 */
	public abstract String getSpeedTemplate();

	/**
	 * @return template used to compute the width of the elevation widget
	 */
	public abstract String getElevationTemplate();

	/**
	 * @return template used to compute the width of the distance widget
	 */
	public abstract String getDistanceTemplate();

	/**
	 * @return template used to compute the width of the longitude and latitude
	 *         widget
	 */
	public String getCoordinateTemplate() {
		return "N99\u00B099\'99.99\"";
	}

	/**
	 * @return template used to compute the width of the longitude and latitude
	 *         widget
	 */
	public String getHeartRateTemplate() {
		return "999";
	}

	/**
	 * @param speed
	 *            the speed in m/s
	 * @return a human readable String containing the speed including a unit.
	 */
	public abstract String speedToString(final float speed);

	/**
	 * @param timeOffsetValue
	 *            timeOffset in ms
	 * @param lengthValue
	 *            distance in meter
	 * @return a human readable String containing the speed including a unit.
	 */
	public String speedToString(final long timeOffsetValue, final double lengthValue) {
		return speedToString((float) (lengthValue / (timeOffsetValue / 1000)));
	}

	/**
	 * @param elevation
	 *            the elevation in meters
	 * @return a human readable String containing the elevation including a
	 *         unit.
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

	public final ScaleConfiguration getScaleHeartRate(final int min, final int max) {
		return getScaleConfiguration("hr", min, max);
	}

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
		int ticks = -1;
		final ScaleConfiguration conf = new ScaleConfiguration();
		// calculate approximate tick size (find correct power of ten)
		double tickSize = 0;
		if (diff != 0) {
			conf.unit = unit;
			conf.lengthInSourceUnits = diff;

			// I wanted to use log10 to find the correct magnitude, but that's
			// not
			// available in CLDC 1.1
			tickSize = 1;
			if (tickSize > diff) {
				while (tickSize > diff && tickSize > 1) {
					tickSize /= 10;
				}
			} else {
				while (tickSize < diff / 10 && tickSize < 1000000) {
					tickSize *= 10;
				}
			}

			// adjust to get a reasonable number of ticks (usually between 3 and
			// 6)
			ticks = (int) (diff / tickSize);
			if (ticks > 5) {
				tickSize *= 2;
			}

			// might be one to big, but that's better than one to small
			ticks = (int) (diff / tickSize) + 1;
		}

		if (ticks < 1) {
			conf.unit = unit;
			conf.lengthInSourceUnits = diff;
			conf.labelLocation = new float[] { (float) min };
			conf.labelValue = new float[] { 0.0f };
			return conf;
		}

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
			// adding a multiple results in smaller numerical errors than adding
			// each iteration
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
		 * Any value might be {@link Float#NaN} to indicate an unused entry
		 * (this is used to avoid unnecessary re-allocation of a smaller array).
		 */
		public float[] labelValue;

		/**
		 * The relative label positions ranging from 0 (minimum) to 1 (maximum).
		 * 
		 * Any value might be {@link Float#NaN} just as in {@link #labelValue}.
		 */
		public float[] labelLocation;

		/**
		 * The length in source units (meter for distance and elevation, m/s for
		 * speed)
		 */
		public double lengthInSourceUnits;
	}
}