package org.bbtracker.mobile.gui;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;
import org.bbtracker.Utils;
import org.bbtracker.mobile.TrackManager;

public class StatusTile extends Tile {
	public static final String MAX_DEGREE_STRING = "99" + Utils.DEGREE + "99" + Utils.MINUTE + "99.99" + Utils.SECOND +
			"W";

	private static final int MARGIN = 2;

	private static final int GAP = 5;

	private final TrackManager manager;

	private final Font font;

	private final int latWidth;

	public StatusTile(final TrackManager manager) {
		this.manager = manager;
		font = Font.getFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL);
		latWidth = font.stringWidth(MAX_DEGREE_STRING);
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

		int y = MARGIN;

		final int leftColumn = MARGIN + latWidth;
		final int middleColumn = MARGIN + latWidth * 2 + GAP;
		final int rightBorder = width - MARGIN;

		g.drawString(lon, leftColumn, y, Graphics.TOP | Graphics.RIGHT);
		g.drawString(lat, middleColumn, y, Graphics.TOP | Graphics.RIGHT);
		g.drawString(elevation, rightBorder, y, Graphics.TOP | Graphics.RIGHT);
		y += font.getHeight();
		g.drawString(speed, leftColumn, y, Graphics.TOP | Graphics.RIGHT);
		g.drawString(course, middleColumn, y, Graphics.TOP | Graphics.RIGHT);
		g.drawString(point, rightBorder, y, Graphics.TOP | Graphics.RIGHT);
	}

	public int getPreferredHeight() {
		return (MARGIN + font.getHeight()) * 2;
	}
}
