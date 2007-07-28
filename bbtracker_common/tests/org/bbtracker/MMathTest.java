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
