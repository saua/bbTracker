/*
 * Copyright 2007 SIB
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

import java.util.Date;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;
import org.bbtracker.UnitConverter;
import org.bbtracker.Utils;
import org.bbtracker.mobile.Preferences;
import org.bbtracker.mobile.TrackManager;

public class DetailsTile extends Tile {

	private static final String COURSE_LABEL = "Course: ";

	private static final String ELEVATION_LABEL = "Elevation: ";

	private static final String LAT_LABEL = "Latitude: ";

	private static final String LON_LABEL = "Longitude: ";

	private static final String TIME_LABEL = "Time: ";

	private static final String DISTANCE_LABEL = "Distance: ";

	private static final String SPEED_LABEL = "Speed: ";

	private static final String POINT_LABEL = "Point: ";

	private static final String NAME_LABEL = "Name: ";

	private static final String SATELLITES_LABEL = "Satellites: ";

	private static final int MARGIN = 2;

	private final TrackManager manager;

	private Font font;

	private int labelWidth;

	public DetailsTile(final TrackManager manager) {
		this.manager = manager;
		setFontSize(Preferences.getInstance().getDetailsFontSize());
	}

	private void setFontSize(final int fontSize) {
		font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, fontSize);
		labelWidth = 0;
		updateLabelWidth(ELEVATION_LABEL);
		updateLabelWidth(LAT_LABEL);
		updateLabelWidth(LON_LABEL);
		updateLabelWidth(TIME_LABEL);
		updateLabelWidth(DISTANCE_LABEL);
		updateLabelWidth(SPEED_LABEL);
		updateLabelWidth(POINT_LABEL);
		updateLabelWidth(NAME_LABEL);
		updateLabelWidth(SATELLITES_LABEL);
	}

	private void updateLabelWidth(final String label) {
		final int w = font.stringWidth(label);
		if (w > labelWidth) {
			labelWidth = w;
		}
	}

	protected void doPaint(final Graphics g) {
		final Track track = manager.getTrack();
		final TrackPoint p = manager.getCurrentPoint();

		g.setColor(0x00ffffff);
		g.fillRect(0, 0, width, height);
		g.setColor(0x00000000);
		g.setFont(font);

		final int pi = manager.getCurrentPointIndex();
		double lonValue = Double.NaN;
		double latValue = Double.NaN;
		float speedValue = Float.NaN;
		float courseValue = Float.NaN;
		float elevationValue = Float.NaN;
		double lengthValue = Double.NaN;
		String pointTime = null;
		byte satellitesValue = -1;
		String time = "-";
		if (p != null) {
			lonValue = p.getLongitude();
			latValue = p.getLatitude();
			speedValue = p.getSpeed();
			courseValue = p.getCourse();
			elevationValue = p.getElevation();
			pointTime = new Date(p.getTimestamp()).toString().substring(11, 19);
			satellitesValue = p.getSatellites();
		}
		if (track != null) {
			if (p != null) {
				lengthValue = p.getDistance();
				final long offset = track.getPointOffset(p);
				time = Utils.durationToString(offset) + " (" + pointTime + ")";
			}
		} else if (p != null) {
			time = pointTime;
		}

		final String trackName;
		if (track == null) {
			trackName = "-";
		} else {
			if (manager.getState() == TrackManager.STATE_STATIC) {
				trackName = track.getName() + " (static)";
			} else {
				trackName = track.getName();
			}
		}
		String point;
		if (pi == -1) {
			point = "-";
		} else {
			point = (pi + 1) + "/" + track.getPointCount();
			if (p.getName() != null) {
				point += " " + p.getName();
			}
		}

		final String lon = Utils.longitudeToString(lonValue).trim();
		final String lat = Utils.latitudeToString(latValue).trim();
		final String course = Utils.courseToHeadingString(courseValue) + " (" + Utils.courseToString(courseValue) + ")";

		final UnitConverter unit = Preferences.getInstance().getUnitsConverter();
		final String speed = unit.speedToString(speedValue);
		final String elevation = unit.elevationToString(elevationValue);
		final String length = unit.distanceToString(lengthValue);
		final String satellites = satellitesValue > 0 ? String.valueOf(satellitesValue) : "-";

		final int fontHeight = font.getHeight();
		final int x = MARGIN + labelWidth;
		int y = MARGIN;

		g.drawString(NAME_LABEL, MARGIN, y, Graphics.TOP | Graphics.LEFT);
		g.drawString(trackName, x, y, Graphics.TOP | Graphics.LEFT);
		y += fontHeight;
		g.drawString(POINT_LABEL, MARGIN, y, Graphics.TOP | Graphics.LEFT);
		g.drawString(point, x, y, Graphics.TOP | Graphics.LEFT);
		y += fontHeight;
		g.drawString(SPEED_LABEL, MARGIN, y, Graphics.TOP | Graphics.LEFT);
		g.drawString(speed, x, y, Graphics.TOP | Graphics.LEFT);
		y += fontHeight;
		g.drawString(DISTANCE_LABEL, MARGIN, y, Graphics.TOP | Graphics.LEFT);
		g.drawString(length, x, y, Graphics.TOP | Graphics.LEFT);
		y += fontHeight;
		g.drawString(TIME_LABEL, MARGIN, y, Graphics.TOP | Graphics.LEFT);
		g.drawString(time, x, y, Graphics.TOP | Graphics.LEFT);
		y += fontHeight * 2;
		g.drawString(LON_LABEL, MARGIN, y, Graphics.TOP | Graphics.LEFT);
		g.drawString(lon, x, y, Graphics.TOP | Graphics.LEFT);
		y += fontHeight;
		g.drawString(LAT_LABEL, MARGIN, y, Graphics.TOP | Graphics.LEFT);
		g.drawString(lat, x, y, Graphics.TOP | Graphics.LEFT);
		y += fontHeight;
		g.drawString(ELEVATION_LABEL, MARGIN, y, Graphics.TOP | Graphics.LEFT);
		g.drawString(elevation, x, y, Graphics.TOP | Graphics.LEFT);
		y += fontHeight;
		g.drawString(COURSE_LABEL, MARGIN, y, Graphics.TOP | Graphics.LEFT);
		g.drawString(course, x, y, Graphics.TOP | Graphics.LEFT);
		y += fontHeight;
		g.drawString(SATELLITES_LABEL, MARGIN, y, Graphics.TOP | Graphics.LEFT);
		g.drawString(satellites, x, y, Graphics.TOP | Graphics.LEFT);
	}

	public void showNotify() {
		setFontSize(Preferences.getInstance().getDetailsFontSize());
	}
}