/*
 * Copyright 2008 Sebastien Chauvin
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

import java.util.Enumeration;

import javax.microedition.lcdui.Graphics;

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;
import org.bbtracker.TrackSegment;
import org.bbtracker.mobile.gui.PlotterTile.AxisConfiguration;

public class TrackPlotter {
	private Track track;

	/** Track style. */
	public static final int SINGLE = 0;

	/** Track style. */
	public static final int WIDE = 1;
	
	private static final int LINK_COLOR = 0x00003300;

	private static final int SEGMENT_LINK_COLOR = 0x00aaaaaa;

	private static final int CURRENT_POINT_COLOR = 0x00ff1010;

	private static final int WAYPOINT_COLOR = 0x00bb0000;

	private static final int CURRENT_POINT_SIZE = 6;

	private static final int WAYPOINT_SIZE = 3;

	private int trackColor = 0;
	
	private int trackStyle = SINGLE;
	
	public TrackPlotter() {
	}

	public void paint(final Graphics g, final DataProvider xData,
			final DataProvider yData, final AxisConfiguration xAxis, final AxisConfiguration yAxis,
			final int offsetX, final int offsetY, final int height) {
		if (track == null) {
			return;
		}
			 
		TrackPoint prevPoint = null;

		int prevX = -1;
		int prevY = -1;
		final Enumeration segments = track.getSegments();
		while (segments.hasMoreElements()) {
			final TrackSegment segment = (TrackSegment) segments.nextElement();
			final Enumeration points = segment.getPoints();
			boolean newSegment = true;
			while (points.hasMoreElements()) {
				final TrackPoint point = (TrackPoint) points.nextElement();
				final double xValue = xData.getValue(point);
				final double yValue = yData.getValue(point);
				final int x = offsetX + xAxis.getPosition(xValue);
				final int y = height - (offsetY + yAxis.getPosition(yValue));

				paintConnection(g, prevPoint, prevX, prevY, point, x, y, newSegment);

				prevPoint = point;
				prevX = x;
				prevY = y;
				newSegment = false;
			}
		}
		paintConnection(g, prevPoint, prevX, prevY, null, -1, -1, false);
	}

	/**
	 * Draws a connection between two points. The first time this method is called for any given redraw operation
	 * <code>point1</code> will be null (and <code>x1</code> and <code>y1</code> will be -1). The last time it is
	 * called the same is true for <code>point2</code>, <code>x2</code> and <code>y2</code>.
	 * 
	 * This is so that every point will always occur once in position 1 and once in position 2.
	 * 
	 * @param g
	 *            The Graphics object to draw on
	 * @param point1
	 *            the first point
	 * @param x1
	 *            the x-Coordinate of the first point on screen
	 * @param y1
	 *            the y-Coordinate of the first point on screen
	 * @param point2
	 *            the second point
	 * @param x2
	 *            the x-Coordinate of the second point on screen
	 * @param y2
	 *            the y-Coordinate of the second point on screen
	 * @param newSegment
	 *            <code>true</code> iff the two points are not in the same segment.
	 */
	protected void paintConnection(final Graphics g, final TrackPoint point1, final int x1, final int y1,
			final TrackPoint point2, final int x2, final int y2, final boolean newSegment) {
		if (point1 == null) {
			return;
		}

		if (point2 != null) {
			g.setColor(trackColor);
			g.drawLine(x1, y1, x2, y2);
			if (trackStyle == WIDE) {
				g.drawLine(x1 + 1, y1 + 1, x2 - 1, y2 - 1);
				g.drawLine(x1 - 1, y1 - 1, x2 + 1, y2 + 1);
			}
		}

		final String name = point1.getName();
		if (name != null && name.length() > 0) {
			g.setColor(WAYPOINT_COLOR);
			g.drawLine(x1 - WAYPOINT_SIZE, y1 - WAYPOINT_SIZE, x1 + WAYPOINT_SIZE, y1 + WAYPOINT_SIZE);
			g.drawLine(x1 - WAYPOINT_SIZE, y1 + WAYPOINT_SIZE, x1 + WAYPOINT_SIZE, y1 - WAYPOINT_SIZE);
		}
	}

	public void paintCurrentPoint(final Graphics g, TrackPoint point, final DataProvider xData,
			final DataProvider yData, final AxisConfiguration xAxis, final AxisConfiguration yAxis,
			final int offsetX, final int offsetY, final int height) {
		final double xValue = xData.getValue(point);
		final double yValue = yData.getValue(point);
		final int x1 = offsetX + xAxis.getPosition(xValue);
		final int y1 = height - (offsetY + yAxis.getPosition(yValue));

		g.setColor(trackColor);
		g.fillRect(x1 - 2, y1 - 2, 4, 4);
		g.setColor(CURRENT_POINT_COLOR);
		g.drawLine(x1, y1 - CURRENT_POINT_SIZE, x1 + CURRENT_POINT_SIZE, y1);
		g.drawLine(x1 + CURRENT_POINT_SIZE, y1, x1, y1 + CURRENT_POINT_SIZE);
		g.drawLine(x1, y1 + CURRENT_POINT_SIZE, x1 - CURRENT_POINT_SIZE, y1);
		g.drawLine(x1 - CURRENT_POINT_SIZE, y1, x1, y1 - CURRENT_POINT_SIZE);
	}
	
	public Track getTrack() {
		return track;
	}
	
	public void setTrack(final Track track) {
		this.track = track;
	}
	
	public void setTrackColor(final int color) {
		this.trackColor = color;
	}
	
	public void setTrackStyle(final int style) {
		this.trackStyle = style;
	}
}
