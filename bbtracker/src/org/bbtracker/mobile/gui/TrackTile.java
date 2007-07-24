package org.bbtracker.mobile.gui;

import java.util.Enumeration;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;
import org.bbtracker.TrackSegment;
import org.bbtracker.Utils;

public class TrackTile extends Tile {
	private static final int MARKED_POINT_COLOR = 0x00bb0000;

	private static final int LAST_POINT_COLOR = 0x00555555;

	private static final int LAST_MARKED_POINT_COLOR = 0x00550000;

	private static final int LINK_COLOR = 0x00003300;

	private static final int SEGMENT_LINK_COLOR = 0x00aaaaaa;

	private static final int FONT_COLOR = 0x00000000;

	private static final double SMALL_STEP = 1 / (60 * 4);

	private static final double MARGIN = 0.025;

	private static final int SCALE_HEIGTH = 5;

	private double minimumLongitude, minimumLatitude;

	private double scale;

	private final Track track;

	private TrackPoint currentPoint;

	private int scaleSizeInPixel;

	private int scaleSize;

	public TrackTile(final Track track) {
		this.track = track;

		minimumLongitude = minimumLatitude = Double.NaN;
		scale = Float.NaN;
	}

	public void setCurrentPoint(final TrackPoint currentPoint) {
		this.currentPoint = currentPoint;
	}

	public TrackPoint getCurrentPoint() {
		return currentPoint;
	}

	protected void recalculateBounds() {
		final double marginFactor = (2 * MARGIN) + 1;

		final double maxLon = track.getMaxLongitude();
		final double minLon = track.getMinLongitude();
		final double maxLat = track.getMaxLatitude();
		final double minLat = track.getMinLatitude();

		double lonDiff = (maxLon - minLon) * marginFactor;
		double latDiff = (maxLat - minLat) * marginFactor;

		if (lonDiff == 0) {
			lonDiff = SMALL_STEP;
		} else if (latDiff == 0) {
			latDiff = SMALL_STEP;
		}

		final double xScale = lonDiff / width;
		final double yScale = latDiff / height;

		scale = Math.max(xScale, yScale);

		minimumLongitude = ((maxLon + minLon) - (width * scale)) / 2;
		minimumLatitude = ((maxLat + minLat) - (height * scale)) / 2;
		final double maximumLongitude = minimumLongitude + (width * scale);

		// calculate the size of the scale
		final double widthInMeter = Utils
				.distance(minimumLatitude, minimumLongitude, minimumLatitude, maximumLongitude);
		if (widthInMeter < 1) {
			scaleSize = 0;
			return;
		}

		final int metersAvailableForScale = (int) (widthInMeter * 0.9);

		scaleSize = 1;
		while (scaleSize * 10 < metersAvailableForScale) {
			scaleSize = scaleSize * 10;
		}
		if (scaleSize * 5 < metersAvailableForScale) {
			scaleSize = scaleSize * 5;
		}
		scaleSizeInPixel = (int) (scaleSize * (width / widthInMeter));
	}

	protected void onResize() {
		minimumLongitude = Double.NaN;
	}

	protected void doPaint(final Graphics g) {
		g.setColor(0x00ffffff);
		g.fillRect(xOffset, yOffset, width, height);
		if (track == null) {
			g.drawString("No Track ...", 2, 2, Graphics.TOP | Graphics.RIGHT);
			return;
		}
		final int segmentCount = track.getSegmentCount();
		if (segmentCount < 1 || track.getSegment(0).getPointCount() == 0) {
			return;
		}
		if (Double.isNaN(minimumLongitude)) {
			recalculateBounds();
		}

		drawScale(g);
		drawTrack(g);
	}

	private void drawTrack(final Graphics g) {
		g.setColor(FONT_COLOR);
		g.setColor(LINK_COLOR);

		int prevX = -1;
		int prevY = -1;
		final boolean prevIsWaypoint = false;
		final Enumeration segments = track.getSegments();
		while (segments.hasMoreElements()) {
			boolean newSegment = (prevX != -1);
			final TrackSegment segment = (TrackSegment) segments.nextElement();
			final Enumeration points = segment.getPoints();
			TrackPoint prev = null;
			while (points.hasMoreElements()) {
				final TrackPoint point = (TrackPoint) points.nextElement();
				final int curX = (int) ((point.getLongitude() - minimumLongitude) / scale);
				final int curY = height - (int) ((point.getLatitude() - minimumLatitude) / scale);
				if (prevX != -1) {

					g.drawLine(prevX, prevY, curX, curY);
					drawPoint(g, prevX, prevY, prevIsWaypoint, prev == currentPoint);
					if (newSegment) {
						g.setColor(SEGMENT_LINK_COLOR);
						newSegment = false;
					} else {
						g.setColor(LINK_COLOR);
					}
				}
				prevX = curX;
				prevY = curY;
				prev = point;
			}
			drawPoint(g, prevX, prevY, prevIsWaypoint, prev == currentPoint);
		}
	}

	private void drawPoint(final Graphics g, final int x, final int y, final boolean waypoint, final boolean current) {
		if (!(waypoint || current)) {
			return;
		}
		if (current) {
			g.setColor(waypoint ? LAST_MARKED_POINT_COLOR : LAST_POINT_COLOR);
			g.drawLine(x, y - 3, x + 3, y);
			g.drawLine(x + 3, y, x, y + 3);
			g.drawLine(x, y + 3, x - 3, y);
			g.drawLine(x - 3, y, x, y - 3);

		} else {
			g.setColor(MARKED_POINT_COLOR);
			g.drawLine(x - 2, y - 2, x + 2, y + 2);
			g.drawLine(x - 2, y + 2, x + 2, y - 2);
		}
	}

	private void drawScale(final Graphics g) {
		if (scaleSize == 0) {
			return;
		}

		String unit;
		int displayScaleSize;

		if (scaleSize >= 10000) {
			unit = "km";
			displayScaleSize = scaleSize / 1000;
		} else {
			unit = "m";
			displayScaleSize = scaleSize;
		}

		final String leftLabel = "0";
		final String middleLabel = displayScaleSize == 5 ? "2.5" : String.valueOf(displayScaleSize / 2);
		final String rightLabel = displayScaleSize + " " + unit;

		final Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
		g.setFont(font);
		g.setColor(0x00000000);
		g.drawRect(2, height - 2 - SCALE_HEIGTH, scaleSizeInPixel, SCALE_HEIGTH);
		g.fillRect(2, height - 2 - SCALE_HEIGTH, scaleSizeInPixel / 2, SCALE_HEIGTH);

		final int textBottom = height - 4 - SCALE_HEIGTH;
		g.drawString(leftLabel, 2, textBottom, Graphics.BOTTOM | Graphics.HCENTER);
		g.drawString(middleLabel, 2 + (scaleSizeInPixel / 2), textBottom, Graphics.BOTTOM | Graphics.HCENTER);
		g.drawString(rightLabel, 2 + scaleSizeInPixel, textBottom, Graphics.BOTTOM | Graphics.HCENTER);
	}
}
