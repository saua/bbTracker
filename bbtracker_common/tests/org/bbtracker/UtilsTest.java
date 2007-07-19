package org.bbtracker;

import junit.framework.TestCase;

public class UtilsTest extends TestCase {
	public void testCourseToString3_4() throws Exception {
		assertEquals("3°", Utils.courseToString(3.4f));
	}

	public void testCourseToString3_6() throws Exception {
		assertEquals("4°", Utils.courseToString(3.6f));
	}

	public void testCourseToStringNaN() throws Exception {
		assertEquals("???°", Utils.courseToString(Float.NaN));
	}

	public void testDegreesToStringPositive() throws Exception {
		assertEquals(" 1° 6\u203239.60\u2033+", Utils.degreesToString(1.111, '+', '-'));
	}

	public void testDegreesToStringNegative() throws Exception {
		assertEquals(" 1° 6\u203239.60\u2033-", Utils.degreesToString(-1.111, '+', '-'));
	}

	public void testDegreesToStringDecimalSecondsLessThan0_1() throws Exception {
		assertEquals(" 1° 0\u2032 0.04\u2033+", Utils.degreesToString(1.00001, '+', '-'));
	}

	public void testLatitudeToStringPositive() throws Exception {
		assertEquals(" 1° 0\u2032 0.00\u2033N", Utils.latitudeToString(1d));
	}

	public void testLatitudeToStringNegative() throws Exception {
		assertEquals(" 1° 0\u2032 0.00\u2033S", Utils.latitudeToString(-1d));
	}

	public void testLongitudeToStringPositive() throws Exception {
		assertEquals(" 1° 0\u2032 0.00\u2033E", Utils.longitudeToString(1d));
	}

	public void testLongitudeToStringNegative() throws Exception {
		assertEquals(" 1° 0\u2032 0.00\u2033W", Utils.longitudeToString(-1d));
	}

	public void testSpeedToString0() throws Exception {
		assertEquals("0.0 km/h", Utils.speedToString(0));
	}

	public void testSpeedToString1() throws Exception {
		assertEquals("3.6 km/h", Utils.speedToString(1));
	}

	public void testSpeedToString5() throws Exception {
		assertEquals("18.0 km/h", Utils.speedToString(5));
	}

	// Test case from http://www.movable-type.co.uk/scripts/latlong-vincenty.html
	public void testDistanceVincenty() throws Exception {
		assertEquals(54972.271, Utils.distance(-37.951033, 144.424868, -37.652821, 143.926496), 0.1d);
	}
}
