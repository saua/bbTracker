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
package org.bbtracker.mobile.gui;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;
import org.bbtracker.UnitConverter;
import org.bbtracker.Utils;
import org.bbtracker.mobile.Log;
import org.bbtracker.mobile.Preferences;
import org.bbtracker.mobile.TrackManager;

public class StatusTile extends Tile {

	/*
	 * The MAX_* constants represent the string values that each field can take that take up the maximum amount of
	 * horizontal space.
	 */
	private static final String MAX_DEGREE_STRING = "99" + Utils.DEGREE + "99" + Utils.MINUTE + "99.99" + Utils.SECOND +
			"W";

	private static final String MAX_COURSE_STRING = "359" + Utils.DEGREE + " NW";

	private static final String MAX_SPEED_STRING = "999.9km/h";

	private static final String MAX_ELEVATION_STRING = "9999m";

	private static final String MAX_LENGTH_STRING = "9999.9km";

	private static final String MAX_POINT_STRING = "9999/9999";

	private static final String MAX_TIME_STRING = "99:99:99";

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

	private int timeWidth;

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
		timeWidth = font.stringWidth(MAX_TIME_STRING);
	}

	protected void onResize() {
		getPreferredHeight(width);
	}

	protected boolean fitsLayout(final int width) {
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
		long timeValue = -1;
		if (p != null) {
			lonValue = p.getLongitude();
			latValue = p.getLatitude();
			speedValue = p.getSpeed();
			courseValue = p.getCourse();
			elevationValue = p.getElevation();
			if (track != null) {
				timeValue = track.getPointOffset(p);
			}
		}
		if (track != null) {
			lengthValue = track.getLength();
		}

		final String lon = Utils.longitudeToString(lonValue);
		final String lat = Utils.latitudeToString(latValue);
		final String course = Utils.courseToString(courseValue) + " " + Utils.courseToHeadingString(courseValue);

		final UnitConverter unit = Preferences.getInstance().getUnitsConverter();
		final String speed = unit.speedToString(speedValue);
		final String elevation = unit.elevationToString(elevationValue);
		final String length = unit.distanceToString(lengthValue);
		final String time = timeValue == -1 ? "-" : Utils.durationToString(timeValue);

		final int line1 = MARGIN;
		final int line2 = line1 + font.getHeight();
		final int line3 = line2 + font.getHeight();

		final int right = width - MARGIN;
		int x;
		int gapWidth;

		// the space available for text
		final int availableWidth = width - 2 * MARGIN;
		final int topRight = Graphics.TOP | Graphics.RIGHT;

		// longitude / latitude
		x = width / 2;
		g.drawString(lon, x, line1, topRight);
		x = right;
		g.drawString(lat, x, line1, topRight);

		gapWidth = (availableWidth - (speedWidth + elevationWidth + courseWidth)) / 3;

		// speed / course / elevation
		x = MARGIN + speedWidth + gapWidth;
		g.drawString(speed, x, line2, topRight);
		x += courseWidth + gapWidth;
		g.drawString(course, x, line2, topRight);
		x = right;
		g.drawString(elevation, x, line2, topRight);

		gapWidth = (availableWidth - (timeWidth + lengthWidth + pointWidth)) / 3;

		// track length / point number
		x = MARGIN + timeWidth + gapWidth;
		g.drawString(time, x, line3, topRight);
		x += lengthWidth + gapWidth;
		g.drawString(length, x, line3, topRight);
		x = right;
		g.drawString(point, x, line3, topRight);
	}

	public void showNotify() {
		// nothing to do
	}

	public int getPreferredHeight(final int width) {
		setFontSize(Preferences.getInstance().getStatusFontSize());
		if (!fitsLayout(width)) {
			Log.log(this, "getPreferredHeight: Setting Font size to medium, because layout doesn't fit!");
			setFontSize(Font.SIZE_MEDIUM);
			if (!fitsLayout(width)) {
				Log.log(this, "getPreferredHeight: Setting Font size to small, because layout still doesn't fit!");
				setFontSize(Font.SIZE_SMALL);
			}
		}
		return MARGIN + font.getHeight() * 3 + MARGIN;
	}
}