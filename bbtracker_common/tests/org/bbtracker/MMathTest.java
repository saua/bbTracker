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

public class MMathTest extends TestCase {
	private static final double DELTA = 1 / 1000d;

	public void testAcos1() throws Exception {
		assertEquals(0d, MMath.acos(1), DELTA);
	}

	public void testAcos0() throws Exception {
		assertEquals(1.57079633d, MMath.acos(0), DELTA);
	}

	public void testAcos0_54() throws Exception {
		assertEquals(1, MMath.acos(0.540302306d), DELTA);
	}
}
