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
		assertEquals("+ 1° 6'39.60\"", Utils.degreesToString(1.111, '+', '-'));
	}

	public void testDegreesToStringNegative() throws Exception {
		assertEquals("- 1° 6'39.60\"", Utils.degreesToString(-1.111, '+', '-'));
	}

	public void testDegreesToStringDecimalSecondsLessThan0_1() throws Exception {
		assertEquals("+ 1° 0' 0.04\"", Utils.degreesToString(1.00001, '+', '-'));
	}

	public void testLatitudeToStringPositive() throws Exception {
		assertEquals("N 1° 0' 0.00\"", Utils.latitudeToString(1d));
	}

	public void testLatitudeToStringNegative() throws Exception {
		assertEquals("S 1° 0' 0.00\"", Utils.latitudeToString(-1d));
	}

	public void testLongitudeToStringPositive() throws Exception {
		assertEquals("E 1° 0' 0.00\"", Utils.longitudeToString(1d));
	}

	public void testLongitudeToStringNegative() throws Exception {
		assertEquals("W 1° 0' 0.00\"", Utils.longitudeToString(-1d));
	}

// public void testSpeedToString0() throws Exception {
// assertEquals("0.0 km/h", Utils.speedToString(0));
// }
//
// public void testSpeedToString1() throws Exception {
// assertEquals("3.6 km/h", Utils.speedToString(1));
// }
//
// public void testSpeedToString5() throws Exception {
// assertEquals("18.0 km/h", Utils.speedToString(5));
// }

	// Test case from http://www.movable-type.co.uk/scripts/latlong-vincenty.html
	public void testDistanceVincenty() throws Exception {
		assertEquals(54972.271, Utils.distance(-37.951033, 144.424868, -37.652821, 143.926496), 0.1d);
	}
}
