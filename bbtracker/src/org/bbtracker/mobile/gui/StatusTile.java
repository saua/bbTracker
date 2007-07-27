package org.bbtracker.mobile.gui;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;
import org.bbtracker.Utils;
import org.bbtracker.mobile.TrackManager;

public class StatusTile extends Tile {
	private static final String MAX_DEGREE_STRING = "99" + Utils.DEGREE + "99" + Utils.MINUTE + "99.99" + Utils.SECOND +
			"W";

	private static final String MAX_SPEED_STRING = "888.8 km/h";

	private static final String MAX_COURSE_STRING = "399" + Utils.DEGREE;

	private static final String MAX_ELEVATION_STRING = "8888m";

	private static final int MARGIN = 2;

	private static final int GAP = 5;

	private final TrackManager manager;

	private final Font font;

	private final int latWidth;

	private final int speedWidth;

	private final int courseWidth;

	private final int elevationWidth;

	public StatusTile(final TrackManager manager) {
		this.manager = manager;
		font = Font.getFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL);
		latWidth = font.stringWidth(MAX_DEGREE_STRING);
		System.out.println(latWidth);
		speedWidth = font.stringWidth(MAX_SPEED_STRING);
		System.out.println(speedWidth);
		courseWidth = font.stringWidth(MAX_COURSE_STRING);
		System.out.println(courseWidth);
		elevationWidth = font.stringWidth(MAX_ELEVATION_STRING);
		System.out.println(elevationWidth);
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
		final String lon;
		final String lat;
		final String speed;
		final String course;
		final String elevation;
		if (p != null) {
			lon = Utils.longitudeToString(p.getLongitude());
			lat = Utils.latitudeToString(p.getLatitude());
			speed = Utils.speedToString(p.getSpeed());
			course = Utils.courseToString(p.getCourse());
			elevation = Utils.elevationToString(p.getElevation());
		} else {
			lon = "-";
			lat = "-";
			speed = "- km/h";
			course = "-" + Utils.DEGREE;
			elevation = "-m";
		}
		final String length;
		if (track != null) {
			length = Utils.distanceToString(track.getLength());
		} else {
			length = "-m";
		}

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
