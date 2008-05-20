package org.bbtracker.mobile.gps;

import junit.framework.TestCase;

public class Jsr179LocationProviderTest extends TestCase {
	public void testGetSatellitesNull() throws Exception {
		assertEquals(-1, Jsr179LocationProvider.getNrOfSatellites(null));
	}

	public void testGetSatellitesEmpty() throws Exception {
		assertEquals(-1, Jsr179LocationProvider.getNrOfSatellites(""));
	}

	public void testGetSatellitesNonGPGGA() throws Exception {
		assertEquals(-1, Jsr179LocationProvider.getNrOfSatellites("$GPGLL,40:00.00000,N,10:00.00000,E,1209493212828,A"));
	}

	public void testGetSatellitesMissing() throws Exception {
		assertEquals(-1, Jsr179LocationProvider
				.getNrOfSatellites("$GPGGA,1209493212828,40:00.00000,N,10:00.00000,E,1,,,310.0,M,,"));
	}

	public void testGetSatellitesNonNumeric() throws Exception {
		assertEquals(-1, Jsr179LocationProvider
				.getNrOfSatellites("$GPGGA,1209493212828,40:00.00000,N,10:00.00000,E,1,A,,310.0,M,,"));
	}

	public void testGetSatellitesTruncated1() throws Exception {
		assertEquals(-1, Jsr179LocationProvider.getNrOfSatellites("$GPGGA,1"));
	}

	public void testGetSatellitesTruncated2() throws Exception {
		assertEquals(-1, Jsr179LocationProvider.getNrOfSatellites("$GPGGA,"));
	}

	public void testGetSatellitesCorrect() throws Exception {
		assertEquals(7, Jsr179LocationProvider
				.getNrOfSatellites("$GPGGA,1209493212828,40:00.00000,N,10:00.00000,E,1,7,,310.0,M,,"));
	}
}