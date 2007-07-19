package org.bbtracker.mobile.gui;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;
import org.bbtracker.Utils;
import org.bbtracker.mobile.TrackManager;

public class StatusTile extends Tile {

	private static final int TOP_LEFT = Graphics.TOP | Graphics.LEFT;

	private static final String LABEL_LONGITUDE = "Long.: ";

	private static final String LABEL_LATITUDE = "Lat.: ";

	private static final String LABEL_POINTS = "Points: ";

	private static final String LABEL_AGE = "Age: ";

	private static final String LABEL_SPEED = "Speed: ";

	private static final String LABEL_COURSE = "Course: ";

	private static final int MARGIN = 2;

	private final TrackManager manager;

	private final Font labelFont;

	private final Font valueFont;

	private final int lineHeight;

	private final int leftLabelWidth;

	private final int rightLabelWidth;

	private final int rightValueWidth;

	public StatusTile(final TrackManager manager) {
		this.manager = manager;
		labelFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_SMALL);
		valueFont = Font.getFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL);
		lineHeight = Math.max(labelFont.getHeight(), valueFont.getHeight());

		int w;

		w = maxWidth(labelFont, LABEL_LATITUDE, -1);
		w = maxWidth(labelFont, LABEL_LONGITUDE, w);
		w = maxWidth(labelFont, LABEL_SPEED, w);
		leftLabelWidth = w;

		w = maxWidth(labelFont, LABEL_POINTS, -1);
		w = maxWidth(labelFont, LABEL_AGE, w);
		w = maxWidth(labelFont, LABEL_COURSE, w);
		rightLabelWidth = w;

		rightValueWidth = valueFont.stringWidth("9999");
	}

	private static final int maxWidth(final Font f, final String s, final int prevMax) {
		final int w = f.stringWidth(s);
		return Math.max(prevMax, w);
	}

	protected void doPaint(final Graphics g) {
		final Track track = manager.getTrack();
		final TrackPoint p = manager.getCurrentPoint();

		g.setColor(0x00ffffff);
		g.fillRect(0, 0, width, height);
		g.setColor(0x00000000);
		g.setFont(labelFont);

		final String points = track == null ? "-" : String.valueOf(track.getPointCount());
		final String lon;
		final String lat;
		final String age;
		final String speed;
		final String course;
		if (p != null) {
			lon = Utils.longitudeToString(p.getLongitude());
			lat = Utils.latitudeToString(p.getLatitude());
			speed = Utils.speedToString(p.getSpeed());
			course = Utils.courseToString(p.getCourse());
			age = String.valueOf(((int) (System.currentTimeMillis() - p.getTimestamp()) / 100) / 10f) + 's';
		} else {
			lon = "-";
			lat = "-";
			speed = "-";
			course = "-";
			age = "-";
		}

		int y = MARGIN;
		int left = MARGIN;
		int right = width - rightLabelWidth - rightValueWidth - MARGIN;
		g.setFont(labelFont);
		g.drawString(LABEL_LONGITUDE, left, y, TOP_LEFT);
		g.drawString(LABEL_POINTS, right, y, TOP_LEFT);
		y += lineHeight;
		g.drawString(LABEL_LATITUDE, left, y, TOP_LEFT);
		g.drawString(LABEL_AGE, right, y, TOP_LEFT);
		y += lineHeight;
		g.drawString(LABEL_SPEED, left, y, TOP_LEFT);
		g.drawString(LABEL_COURSE, right, y, TOP_LEFT);

		left += leftLabelWidth;
		right += rightLabelWidth;
		y = MARGIN;
		g.drawString(lon, left, y, TOP_LEFT);
		g.drawString(points, right, y, TOP_LEFT);
		y += lineHeight;
		g.drawString(lat, left, y, TOP_LEFT);
		g.drawString(age, right, y, TOP_LEFT);
		y += lineHeight;
		g.drawString(speed, left, y, TOP_LEFT);
		g.drawString(course, right, y, TOP_LEFT);
	}

	public int getPreferredHeight() {
		return MARGIN + lineHeight * 3 + MARGIN;
	}
}
