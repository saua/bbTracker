/*
 * Copyright (C) 2006 Dana Peters
 * Parts Copyright 2007 Joachim Sauer
 * 
 * This file was originally part of J2ME GPS Track
 * http://www.qcontinuum.org/gpstrack
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
package org.bbtracker.mobile.gps;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import org.bbtracker.mobile.Log;

public class Gps {

	private Thread mThread;

	private float mHeading, mSpeed, mAltitude;

	private float mLatitude;

	private float mLongitude;

	private boolean mFix;

	private int mHour, mMinute, mSecond;

	private int mDay, mMonth, mYear;

	private int mNmeaCount;

	private int mAllSatellites, mFixSatellites;

	private final GpsHorizontalPosition mGpsSatellites[] = new GpsHorizontalPosition[12];

	public Gps() {
		for (int i = 0; i < 12; i++) {
			mGpsSatellites[i] = new GpsHorizontalPosition();
		}
		mNmeaCount = 0;
	}

	public boolean isOpen() {
		return mThread != null;
	}

	public int getNmeaCount() {
		return mNmeaCount;
	}

	public GpsHorizontalPosition[] getSatellites() {
		return mGpsSatellites;
	}

	public float getLatitude() {
		return mLatitude;
	}

	public float getLongitude() {
		return mLongitude;
	}

	public float getHeading() {
		return mHeading;
	}

	public float getSpeed() {
		return mSpeed * 1.852f;
	}

	public float getAltitude() {
		return mAltitude;
	}

	public int getSatelliteCount() {
		return mFixSatellites;
	}

	public boolean getFix() {
		return mFix;
	}

	public void open(final String url) throws LocationException {
		close();

		StreamConnection streamConnection = null;
		InputStream inputStream = null;
		try {
			streamConnection = (StreamConnection) Connector.open(url);
			inputStream = streamConnection.openInputStream();
		} catch (final IOException ex) {
			Log.log(this, ex, "Error opening connection to " + url);
			throw new LocationException("Error while opening Bluetooth connection: " + ex.getMessage());
		}
		mThread = new Thread(new DataReader(inputStream, streamConnection));
		mThread.start();
	}

	public void close() {
		mNmeaCount = 0;
		if (mThread != null) {
			final Thread thread = mThread;
			mThread = null;
			try {
				thread.join();
			} catch (final InterruptedException ex) {
			}
		}
	}

	private void extractData(final String[] param, final int a, final int b, final int c, final int d, final int e) {
		int degree, minute, fraction;
		float latitude = Float.NaN;
		float longitude = Float.NaN;
		if (param[a].length() > 8 && param[b].length() == 1) {
			degree = Integer.parseInt(param[a].substring(0, 2));
			minute = Integer.parseInt(param[a].substring(2, 4));
			fraction = Integer.parseInt(param[a].substring(5, 9).concat("0000").substring(0, 4));
			latitude = degree + (minute / 60) + (fraction / 600000);
			if (param[b].charAt(0) == 'S') {
				latitude = -latitude;
			}
		}
		if (param[c].length() > 9 && param[d].length() == 1) {
			degree = Integer.parseInt(param[c].substring(0, 3));
			minute = Integer.parseInt(param[c].substring(3, 5));
			fraction = Integer.parseInt(param[c].substring(6, 10).concat("0000").substring(0, 4));
			longitude = degree + (minute / 60) + (fraction / 600000);
			if (param[d].charAt(0) == 'W') {
				longitude = -longitude;
			}
		}
		if (param[e].length() > 5) {
			mHour = Integer.parseInt(param[e].substring(0, 2));
			mMinute = Integer.parseInt(param[e].substring(2, 4));
			mSecond = Integer.parseInt(param[e].substring(4, 6));
		}
		if (!Float.isNaN(latitude) && !Float.isNaN(longitude)) {
			mLatitude = latitude;
			mLongitude = longitude;
		}
	}

	private void receiveNmea(final String nmea) {
		final int starIndex = nmea.indexOf('*');
		if (starIndex == -1) {
			return;
		}
		final String[] param = splitString(nmea.substring(0, starIndex), ",");
		if (param[0].equals("$GPGSV")) {
			int i, j;
			mNmeaCount++;
			mAllSatellites = Integer.parseInt(param[3]);
			j = (Integer.parseInt(param[2]) - 1) * 4;
			for (i = 4; i < 17 && j < 12; i += 4, j++) {
				mGpsSatellites[j].setNumber(Integer.parseInt(param[i]));
				mGpsSatellites[j].setElevation(Integer.parseInt(param[i + 1]));
				mGpsSatellites[j].setAzimuth(Integer.parseInt(param[i + 2]));
				mGpsSatellites[j].setSnr(param[i + 3].length() > 0 ? Integer.parseInt(param[i + 3]) : 0);
			}
		} else if (param[0].equals("$GPGLL")) {
			mNmeaCount++;
			extractData(param, 1, 2, 3, 4, 5);
			// qual = param[6].charAt(0); // 'A'
			mFix = (param[6].charAt(0) == 'A');
		} else if (param[0].equals("$GPRMC")) {
			mNmeaCount++;
			// qual = param[2].charAt(0); // 'A'
			extractData(param, 3, 4, 5, 6, 1);
			mFix = (param[2].charAt(0) == 'A');
			mDay = Integer.parseInt(param[9].substring(0, 2));
			mMonth = Integer.parseInt(param[9].substring(2, 4));
			mYear = 2000 + Integer.parseInt(param[9].substring(4, 6));
			mSpeed = Float.parseFloat(param[7]);
			if (param[8].length() > 0) {
				mHeading = Float.parseFloat(param[8]);
			}
		} else if (param[0].equals("$GPGGA")) {
			mNmeaCount++;
			extractData(param, 2, 3, 4, 5, 1);
			// qual2 = param[2].charAt(5); // '1'
			// fix = (qual2 > '0');
			mFixSatellites = Integer.parseInt(param[7]);
			if (param[9].length() > 0) {
				mAltitude = Float.parseFloat(param[9]);
				// altunit = param[10].charAt(0);
			}
		} else if (param[0].equals("$GPGSA")) {
			int i, j, k;
			mNmeaCount++;
			for (i = 0; i < 12; i++) {
				mGpsSatellites[i].setFix(false);
			}
			for (j = 0; j < 12; j++) {
				if (param[j + 3].length() > 0) {
					if ((k = Integer.parseInt(param[j + 3])) != 0) {
						for (i = 0; i < mAllSatellites; i++) {
							if (mGpsSatellites[i].getNumber() == k) {
								mGpsSatellites[i].setFix(true);
								break;
							}
						}
					}
				}
			}
		}
	}

	// my private poor StringTokenizer
	private static String[] splitString(final String string, final String seperator) {
		final Vector str = new Vector(10, 5);
		int s = 0;
		int e = string.indexOf(seperator);
		while (e != -1) {
			str.addElement(string.substring(s, e));
			s = e + seperator.length();
			e = string.indexOf(seperator, s);
		}
		str.addElement(string.substring(s));
		final String[] result = new String[str.size()];
		str.copyInto(result);
		return result;
	}

	private class DataReader implements Runnable {
		private final InputStream stream;

		private final StreamConnection connection;

		DataReader(final InputStream stream, final StreamConnection connection) {
			this.stream = stream;
			this.connection = connection;
		}

		public void run() {
			while (mThread != null) {
				String s;
				try {
					final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
					int ch = 0;
					while ((ch = stream.read()) != '\n') {
						byteArrayOutputStream.write(ch);
					}
					byteArrayOutputStream.flush();
					final byte[] b = byteArrayOutputStream.toByteArray();
					s = new String(b);
					byteArrayOutputStream.close();
				} catch (final IOException ex) {
					mThread = null;
					Log.log(this, ex, "Error while receiving NMEA string");
					continue;
				}

				try {
					receiveNmea(s);
				} catch (final Exception ex) {
					Log.log(this, ex, "Failed to parse NMEA String <" + s + ">");
				}
			}
			try {
				if (stream != null) {
					stream.close();
				}
				if (connection != null) {
					connection.close();
				}
			} catch (final IOException ex) {

			}
		}
	}
}
