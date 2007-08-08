package org.bbtracker.mobile.gui;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;
import org.bbtracker.UnitConverter;
import org.bbtracker.Utils;
import org.bbtracker.mobile.BBTracker;
import org.bbtracker.mobile.Preferences;
import org.bbtracker.mobile.TrackManager;

public class StatusTile extends Tile {

	/*
	 * The MAX_* constants represent the string values that each field can take that take up the maximum amount of
	 * horizontal space.
	 */
	private static final String MAX_DEGREE_STRING = "99" + Utils.DEGREE + "99" + Utils.MINUTE + "99.99" + Utils.SECOND +
			"W";

	private static final String MAX_COURSE_STRING = "399" + Utils.DEGREE;

	private static final String MAX_SPEED_STRING = "999.9km/h";

	private static final String MAX_ELEVATION_STRING = "9999m";

	private static final String MAX_LENGTH_STRING = "9999.9km";

	private static final String MAX_POINT_STRING = "9999/9999";

	private static final int MARGIN = 2;

	private static final int MINIMAL_GAP = 5;

	private final TrackManager manager;

	private Font font;

	private int latWidth;

	private int courseWidth;

	private int speedWidth;

	private int elevationWidth;

	private int lengthWidth;

	private int pointWidth;

	private boolean twoLineLayout = true;

	public StatusTile(final TrackManager manager) {
		this.manager = manager;
		setFontSize(Font.SIZE_MEDIUM);
	}

	private void setFontSize(final int fontSize) {
		font = Font.getFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, fontSize);
		latWidth = font.stringWidth(MAX_DEGREE_STRING);
		courseWidth = font.stringWidth(MAX_COURSE_STRING);
		speedWidth = font.stringWidth(MAX_SPEED_STRING);
		elevationWidth = font.stringWidth(MAX_ELEVATION_STRING);
		lengthWidth = font.stringWidth(MAX_LENGTH_STRING);
		pointWidth = font.stringWidth(MAX_POINT_STRING);
		twoLineLayout = fitsTwoLineLayout(width);
	}

	protected void onResize() {
		twoLineLayout = fitsTwoLineLayout(width);
		if (!twoLineLayout && width < ((MARGIN + latWidth) * 2 + MINIMAL_GAP)) {
			BBTracker.log("onResize: Setting Font size to small, because even three lines overlap!");
			setFontSize(Font.SIZE_SMALL);
		}
	}

	protected boolean fitsTwoLineLayout(final int width) {
		return width >= (MARGIN + MINIMAL_GAP + latWidth) * 2 + lengthWidth;
	}

	protected boolean fitsThreeLineLayout(final int width) {
		return width >= (MARGIN + latWidth) * 2 + MINIMAL_GAP;
	}

	protected void doPaint(final Graphics g) {
		final Track track = manager.getTrack();
		final TrackPoint p = manager.getCurrentPoint();
		final int pi = manager.getCurrentPointIndex();

		g.setColor(0x00ffffff);
		g.fillRect(0, 0, width, height);
		g.setColor(0x00000000);
		g.setFont(font);

		final String point;
		if (pi == -1) {
			point = "-";
		} else {
			point = (pi + 1) + "/" + track.getPointCount();
		}
		double lonValue = Double.NaN;
		double latValue = Double.NaN;
		float speedValue = Float.NaN;
		float courseValue = Float.NaN;
		float elevationValue = Float.NaN;
		double lengthValue = Double.NaN;
		if (p != null) {
			lonValue = p.getLongitude();
			latValue = p.getLatitude();
			speedValue = p.getSpeed();
			courseValue = p.getCourse();
			elevationValue = p.getElevation();
		}
		if (track != null) {
			lengthValue = track.getLength();
		}

		final String lon = Utils.longitudeToString(lonValue);
		final String lat = Utils.latitudeToString(latValue);
		final String course = Utils.courseToString(courseValue);

		final UnitConverter unit = Preferences.getInstance().getUnitsConverter();
		final String speed = unit.speedToString(speedValue);
		final String elevation = unit.elevationToString(elevationValue);
		final String length = unit.distanceToString(lengthValue);

		final int line1 = MARGIN;
		final int line2 = line1 + font.getHeight();
		final int latLonWidth; // the space available for latitude and longitude combined, it's also used for
		// speed/course/elevation
		if (twoLineLayout) {
			final int spareSpace = width - (MARGIN * 2 + latWidth * 2 + Math.max(lengthWidth, pointWidth));
			latLonWidth = latWidth * 2 + spareSpace / 2;
		} else {
			latLonWidth = width - 2 * MARGIN;
		}

		// longitude / latitude (always on line 1)
		g.drawString(lon, MARGIN + latLonWidth / 2, line1, Graphics.TOP | Graphics.RIGHT);
		g.drawString(lat, MARGIN + latLonWidth, line1, Graphics.TOP | Graphics.RIGHT);

		final int spaceForCourse = latLonWidth - speedWidth - elevationWidth;
		final int courseX = MARGIN + speedWidth + (spaceForCourse + courseWidth) / 2;

		// speed / course / elevation (always on line 2)
		g.drawString(speed, MARGIN + speedWidth, line2, Graphics.TOP | Graphics.RIGHT);
		g.drawString(course, courseX, line2, Graphics.TOP | Graphics.RIGHT);
		g.drawString(elevation, MARGIN + latLonWidth, line2, Graphics.TOP | Graphics.RIGHT);

		final int lengthX;
		int lengthY;
		final int pointX;
		int pointY;

		if (twoLineLayout) {
			lengthX = width - MARGIN;
			lengthY = line1;
			pointX = lengthX;
			pointY = line2;
		} else {
			lengthX = width / 2;
			lengthY = line2 + font.getHeight();
			pointX = width - MARGIN;
			pointY = lengthY;
		}
		g.drawString(length, lengthX, lengthY, Graphics.TOP | Graphics.RIGHT);
		g.drawString(point, pointX, pointY, Graphics.TOP | Graphics.RIGHT);
	}

	public void showNotify() {
		// nothing to do
	}

	public int getPreferredHeight(final int width) {
		final int lineCount;
		if (fitsTwoLineLayout(width)) {
			lineCount = 2;
		} else {
			lineCount = 3;
			if (!fitsThreeLineLayout(width)) {
				BBTracker.log("getPreferredHeight: Setting Font size to small, because even three lines overlap!");
				setFontSize(Font.SIZE_SMALL);
			}
		}
		return MARGIN + font.getHeight() * lineCount + MARGIN;
	}
}