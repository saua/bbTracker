// J2ME GPS Track
// Copyright (C) 2006 Dana Peters
// http://www.qcontinuum.org/gpstrack

package org.bbtracker.mobile.gps;

public class GpsHorizontalPosition {

	private float mAzimuth;

	private float mElevation;

	int mNumber;

	int mSnr;

	boolean mFix;

	public GpsHorizontalPosition() {
		mNumber = 0;
		mSnr = 0;
		mFix = false;
		mAzimuth = 0f;
		mElevation = 0f;
	}

	public float getAzimuth() {
		return mAzimuth;
	}

	public float getElevation() {
		return mElevation;
	}

	public void setAzimuth(final float azimuth) {
		mAzimuth = azimuth;
	}

	public void setElevation(final float elevation) {
		mElevation = elevation;
	}

	public void setNumber(final int number) {
		mNumber = number;
	}

	public void setSnr(final int snr) {
		mSnr = snr;
	}

	public void setFix(final boolean fix) {
		mFix = fix;
	}

	public int getNumber() {
		return mNumber;
	}

	public int getSnr() {
		return mSnr;
	}

	public boolean getFix() {
		return mFix;
	}

}
