package org.bbtracker.mobile.gui;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;
import org.bbtracker.UnitConverter;
import org.bbtracker.Utils;
import org.bbtracker.mobile.Preferences;
import org.bbtracker.mobile.TrackManager;

public class StatusTile extends Tile {
	private static final String MAX_DEGREE_STRING = "99" + Utils.DEGREE + "99" + Utils.MINUTE + "99.99" + Utils.SECOND +
			"W";

	private static final String MAX_COURSE_STRING = "399" + Utils.DEGREE;

	private static final int MARGIN = 2;

	private static final int GAP = 5;

	private final TrackManager manager;

	private final Font font;

	private final int latWidth;

	private final int courseWidth;

	public StatusTile(final TrackManager manager) {
		this.manager = manager;
		font = Font.getFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL);
		latWidth = font.stringWidth(MAX_DEGREE_STRING);
		courseWidth = font.stringWidth(MAX_COURSE_STRING);
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

		int y = MARGIN;

		final int lonPos = MARGIN + latWidth;
		final int latPos = MARGIN + latWidth * 2 + GAP;
		final int rightPos = width - MARGIN;

		g.drawString(lon, lonPos, y, Graphics.TOP | Graphics.RIGHT);
		g.drawString(lat, latPos, y, Graphics.TOP | Graphics.RIGHT);
		g.drawString(length, rightPos, y, Graphics.TOP | Graphics.RIGHT);
		y += font.getHeight();
		g.drawString(speed, lonPos, y, Graphics.TOP | Graphics.RIGHT);
		g.drawString(course, lonPos + GAP + GAP + courseWidth, y, Graphics.TOP | Graphics.RIGHT);
		g.drawString(elevation, latPos, y, Graphics.TOP | Graphics.RIGHT);
		g.drawString(point, rightPos, y, Graphics.TOP | Graphics.RIGHT);
	}

	public int getPreferredHeight() {
		return (MARGIN + font.getHeight()) * 2;
	}
}
