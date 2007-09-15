/*
 * Copyright 2007 Joachim Sauer
 * Copyright 2002-2006 Chriss Veness (vincenty formula in distance())
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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public final class Utils {

	// U+00B0 = Degree Sign
	public static final char DEGREE = '\u00B0';

	// U+2032 = Prime
	public static final char MINUTE = '\u2032';

	// U+2033 = Double Prime
	public static final char SECOND = '\u2033';

	private static final double WGS84_A = 6378137;

	private static final double WGS84_B = 6356752.3142;

	private static final double WGS84_F = 1 / 298.257223563;

	private Utils() {
		// don't Instantiate
	}

	public static String longitudeToString(final double longitude) {
		return degreesToString(longitude, 'E', 'W');
	}

	public static String latitudeToString(final double latitude) {
		return degreesToString(latitude, 'N', 'S');
	}

	public static String degreesToString(final double value, final char positiveChar, final char negativeChar) {
		if (Double.isNaN(value)) {
			return "-";
		}
		char c;
		double d;
		if (value < 0) {
			d = -value;
			c = negativeChar;
		} else {
			d = value;
			c = positiveChar;
		}
		final StringBuffer buf = new StringBuffer(13);
		buf.append(c);
		final int degrees = (int) Math.floor(d);
		d = (d - degrees) * 60;
		final int minutes = (int) Math.floor(d);
		d = (d - minutes) * 60;
		final int seconds = (int) Math.floor(d);
		d = (d - seconds) * 100;
		final int hundrethSeconds = (int) Math.floor(d + 0.5d);

		appendTwoDigits(buf, degrees, ' ').append(DEGREE);
		appendTwoDigits(buf, minutes, ' ').append(MINUTE);
		appendTwoDigits(buf, seconds, ' ').append('.');
		appendTwoDigits(buf, hundrethSeconds, '0').append(SECOND);
		return buf.toString();
	}

	private static StringBuffer appendTwoDigits(final StringBuffer buf, final int value, final char c) {
		if (value < 10) {
			buf.append(c);
		}
		buf.append(value);
		return buf;
	}

	/**
	 * Taken and converted to Java from http://www.movable-type.co.uk/scripts/latlong-vincenty.html
	 * 
	 * All parameters are interpreted as degrees.
	 * 
	 * @return great circle distance in meters
	 */
	public static double distance(final double lat1, final double lon1, final double lat2, final double lon2) {

		final double L = Math.toRadians(lon2 - lon1);
		final double U1 = MMath.atan((1 - WGS84_F) * Math.tan(Math.toRadians(lat1)));
		final double U2 = MMath.atan((1 - WGS84_F) * Math.tan(Math.toRadians(lat2)));
		final double sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);
		final double sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);

		double lambda = L, lambdaP = 2 * Math.PI;
		double cosSqAlpha = 0d;
		double sinSigma = 0d;
		double cos2SigmaM = 0d;
		double cosSigma = 0d;
		double sigma = 0d;
		int iterLimit = 20;
		while (Math.abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0) {
			final double sinLambda = Math.sin(lambda), cosLambda = Math.cos(lambda);
			sinSigma = Math.sqrt((cosU2 * sinLambda) * (cosU2 * sinLambda) +
					(cosU1 * sinU2 - sinU1 * cosU2 * cosLambda) * (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda));
			if (sinSigma == 0) {
				return 0; // coincident points
			}
			cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
			sigma = MMath.atan2(sinSigma, cosSigma);
			final double sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
			cosSqAlpha = 1 - sinAlpha * sinAlpha;
			cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha;
			if (Double.isNaN(cos2SigmaM)) {
				cos2SigmaM = 0; // equatorial line: cosSqAlpha=0 (§6)
			}
			final double C = WGS84_F / 16 * cosSqAlpha * (4 + WGS84_F * (4 - 3 * cosSqAlpha));
			lambdaP = lambda;
			lambda = L + (1 - C) * WGS84_F * sinAlpha *
					(sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)));
		}
		if (iterLimit == 0) {
			return Double.NaN; // formula failed to converge
		}

		final double uSq = cosSqAlpha * (WGS84_A * WGS84_A - WGS84_B * WGS84_B) / (WGS84_B * WGS84_B);
		final double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
		final double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));
		final double deltaSigma = B *
				sinSigma *
				(cos2SigmaM + B /
						4 *
						(cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) - B / 6 * cos2SigmaM *
								(-3 + 4 * sinSigma * sinSigma) * (-3 + 4 * cos2SigmaM * cos2SigmaM)));
		final double s = WGS84_B * A * (sigma - deltaSigma);

		return s;
	}

	public static String dateToString(final Date date) {
		final String orig = date.toString();
		final int size = orig.length();
		// luckily enough Date.toString() is well-defined, so we can cut the TimeZone info out
		return orig.substring(0, 20) + orig.substring(size - 4, size);
	}

	public static String calendarToCompactString(final Calendar calendar) {
		final StringBuffer sb = new StringBuffer(15);
		sb.append(calendar.get(Calendar.YEAR));
		appendTwoDigits(sb, calendar.get(Calendar.MONTH) + 1, '0');
		appendTwoDigits(sb, calendar.get(Calendar.DAY_OF_MONTH), '0');
		sb.append('_');
		appendTwoDigits(sb, calendar.get(Calendar.HOUR_OF_DAY), '0');
		appendTwoDigits(sb, calendar.get(Calendar.MINUTE), '0');
		appendTwoDigits(sb, calendar.get(Calendar.SECOND), '0');
		return sb.toString();
	}

	/**
	 * Calculates a valid xsd:dateTime value from a given date.
	 * 
	 * The XML Schema standard defines a dateTime roughly as "YYYY-MM-DDThh:mm:ss(.s+)? (zzzzzz)?"
	 * 
	 * @param date
	 * @return
	 */
	public static String dateToXmlDateTime(final Date date) {
		final TimeZone utc = TimeZone.getTimeZone("GMT");
		final Calendar c = Calendar.getInstance(utc);
		c.setTime(date);
		final StringBuffer result = new StringBuffer(24);
		result.append(c.get(Calendar.YEAR)).append('-');
		appendTwoDigits(result, c.get(Calendar.MONTH) + 1, '0').append('-');
		appendTwoDigits(result, c.get(Calendar.DATE), '0').append('T');
		appendTwoDigits(result, c.get(Calendar.HOUR_OF_DAY), '0').append(':');
		appendTwoDigits(result, c.get(Calendar.MINUTE), '0').append(':');
		appendTwoDigits(result, c.get(Calendar.SECOND), '0').append('.');
		final int millisecond = c.get(Calendar.MILLISECOND);
		if (millisecond < 100) {
			result.append('0');
		}
		appendTwoDigits(result, millisecond, '0').append('Z');

		return result.toString();
	}

	public static String escapeXml(final String xml) {
		final StringBuffer escaped = new StringBuffer(xml.length() + 4);
		final char[] chars = xml.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			final char c = chars[i];
			switch (c) {
			case '<':
				escaped.append("&lt;");
				break;
			case '&':
				escaped.append("&amp;");
				break;
			default:
				escaped.append(c);
			}
		}
		return escaped.toString();
	}

	public static String courseToString(final float course) {
		if (Float.isNaN(course)) {
			return "???" + DEGREE;
		} else {
			return String.valueOf((int) (Math.floor(course + 0.5d))) + DEGREE;
		}
	}

	public static String courseToHeadingString(final float course) {
		if (Float.isNaN(course)) {
			return "??";
		} else {
			final int courseInt = (int) Math.floor(course + 0.5d);
			String courseString = "??";
			if (courseInt < 31 || courseInt >= 331) {
				courseString = "N";
			} else if (courseInt < 61) {
				courseString = "NE";
			} else if (courseInt < 121) {
				courseString = "E";
			} else if (courseInt < 151) {
				courseString = "SE";
			} else if (courseInt < 211) {
				courseString = "S";
			} else if (courseInt < 241) {
				courseString = "SW";
			} else if (courseInt < 301) {
				courseString = "W";
			} else {
				courseString = "NW";
			}
			return courseString;

		}
	}

	/**
	 * Converts a given double value to a String with a single digit after that decimal point and optionally strips ".0"
	 * if present.
	 */
	public static String doubleToString(final double value, final boolean stripDotZero) {
		return fixedPointToString((long) (value * 10), stripDotZero);
	}

	/**
	 * Converts a given float value to a String with a single digit after that decimal point and optionally strips ".0"
	 * if present.
	 */
	public static String floatToString(final float value, final boolean stripDotZero) {
		return fixedPointToString((long) (value * 10), stripDotZero);
	}

	private static String fixedPointToString(final long value, final boolean stripDotZero) {
		final String string = String.valueOf(value);
		final int stringLength = string.length();
		final StringBuffer result = new StringBuffer(stringLength + 1);
		result.append(string.substring(0, stringLength - 1));
		if (result.length() == 0) {
			result.append("0");
		}
		if (!(stripDotZero && string.endsWith("0"))) {
			result.append('.');
			result.append(string.charAt(stringLength - 1));
		}
		return result.toString();
	}

	public static String durationToString(final long msec) {
		// i do hope no one uses bbTracker do record tracks that
		// are longer than Integer.MAX_VALUE seconds.
		final StringBuffer sb = new StringBuffer(8);
		int sec = (int) (msec / 1000);
		if (sec > 60 * 60) {
			final int hours = sec / (60 * 60);
			sb.append(hours);
			sec -= hours * (60 * 60);
			sb.append(':');
		}

		final int minutes = sec / 60;
		if (sb.length() > 0) {
			appendTwoDigits(sb, minutes, '0');
		} else {
			sb.append(minutes);
		}
		sb.append(':');
		sec -= minutes * 60;

		appendTwoDigits(sb, sec, '0');

		return sb.toString();
	}
}