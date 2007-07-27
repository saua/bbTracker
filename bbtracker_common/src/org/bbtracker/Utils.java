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

	private static final float MS_TO_KMH_FACTOR = 3.6f;

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
		buf.append(c);
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

	/**
	 * @param speed
	 *            the speed in m/s
	 * @return a human readable String containing the speed in km/h.
	 */
	public static String speedToString(final float speed) {
		final float value = speed * MS_TO_KMH_FACTOR;
		return String.valueOf(((int) (value * 10)) / 10f) + " km/h";
	}

	public static String courseToString(final float course) {
		if (Float.isNaN(course)) {
			return "???" + DEGREE;
		} else {
			return String.valueOf((int) (Math.floor(course + 0.5d))) + DEGREE;
		}
	}

	public static String dateToString(final Date date) {
		final String orig = date.toString();
		final int size = orig.length();
		// luckily enough Date.toString() is well-defined, so we can cut the TimeZone info out
		return orig.substring(0, 20) + orig.substring(size - 4, size);
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

	public static String elevationToString(final float elevation) {
		return ((int) elevation) + "m";
	}
}