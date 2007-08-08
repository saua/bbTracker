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

import java.util.Enumeration;

import javax.microedition.lcdui.Graphics;

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;
import org.bbtracker.TrackSegment;
import org.bbtracker.mobile.TrackManager;

public abstract class PlotterTile extends Tile {
	public static final int DEFAULT_MARGIN = 5;

	private static final int LINK_COLOR = 0x00003300;

	private static final int SEGMENT_LINK_COLOR = 0x00aaaaaa;

	private static final int CURRENT_POINT_COLOR = 0x00555555;

	private static final int WAYPOINT_COLOR = 0x00bb0000;

	private static final int CURRENT_POINT_SIZE = 4;

	private static final int WAYPOINT_SIZE = 3;

	private final TrackManager manager;

	private Track track;

	private final AxisConfiguration xAxis = new AxisConfiguration();

	private final AxisConfiguration yAxis = new AxisConfiguration();

	private final DataProvider xData;

	private final DataProvider yData;

	private final boolean linkedScale;

	private int marginLeft = DEFAULT_MARGIN;

	private int marginRight = DEFAULT_MARGIN;

	private int marginTop = DEFAULT_MARGIN;

	private int marginBottom = DEFAULT_MARGIN;

	public PlotterTile(final TrackManager manager, final DataProvider xData, final DataProvider yData,
			final boolean linkedScale) {
		this.manager = manager;
		this.xData = xData;
		this.yData = yData;
		this.linkedScale = linkedScale;
		track = manager.getTrack();
		manager.addPointListener(this);
		updateScale();
	}

	protected int getMarginLeft() {
		return marginLeft;
	}

	protected void setMarginLeft(final int marginLeft) {
		this.marginLeft = marginLeft;
		onScaleChanged();
	}

	protected int getMarginRight() {
		return marginRight;
	}

	protected void setMarginRight(final int marginRight) {
		this.marginRight = marginRight;
		onScaleChanged();
	}

	protected int getMarginTop() {
		return marginTop;
	}

	protected void setMarginTop(final int marginTop) {
		this.marginTop = marginTop;
		onScaleChanged();
	}

	protected int getMarginBottom() {
		return marginBottom;
	}

	protected void setMarginBottom(final int marginBottom) {
		this.marginBottom = marginBottom;
		onScaleChanged();
	}

	protected void setMargins(final int left, final int right, final int top, final int bottom) {
		marginLeft = left;
		marginRight = right;
		marginTop = top;
		marginBottom = bottom;
		onScaleChanged();
	}

	protected void doPaint(final Graphics g) {
		g.setColor(0x00ffffff);
		g.fillRect(xOffset, yOffset, width, height);
		g.setColor(0x00000000);

		if (Double.isNaN(xAxis.scale) || Double.isNaN(yAxis.scale)) {
			doPaintNoScale(g);
			return;
		}

		doPaintPlot(g);
		doPaintAxis(g);
	}

	protected void doPaintPlot(final Graphics g) {
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
				final int x = getMarginLeft() + xAxis.getPosition(xValue);
				final int y = height - (getMarginBottom() + yAxis.getPosition(yValue));

				paintConnection(g, prevPoint, prevX, prevY, point, x, y, newSegment);

				prevPoint = point;
				prevX = x;
				prevY = y;
				newSegment = false;
			}
		}
		paintConnection(g, prevPoint, prevX, prevY, null, -1, -1, false);
	}

	protected abstract void doPaintAxis(final Graphics g);

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
			g.setColor(newSegment ? SEGMENT_LINK_COLOR : LINK_COLOR);
			g.drawLine(x1, y1, x2, y2);
		}

		final String name = point1.getName();
		if (name != null && name.length() > 0) {
			g.setColor(WAYPOINT_COLOR);
			g.drawLine(x1 - WAYPOINT_SIZE, y1 - WAYPOINT_SIZE, x1 + WAYPOINT_SIZE, y1 + WAYPOINT_SIZE);
			g.drawLine(x1 - WAYPOINT_SIZE, y1 + WAYPOINT_SIZE, x1 + WAYPOINT_SIZE, y1 - WAYPOINT_SIZE);
		}

		if (point1 == manager.getCurrentPoint()) {
			g.setColor(CURRENT_POINT_COLOR);
			g.drawLine(x1, y1 - CURRENT_POINT_SIZE, x1 + CURRENT_POINT_SIZE, y1);
			g.drawLine(x1 + CURRENT_POINT_SIZE, y1, x1, y1 + CURRENT_POINT_SIZE);
			g.drawLine(x1, y1 + CURRENT_POINT_SIZE, x1 - CURRENT_POINT_SIZE, y1);
			g.drawLine(x1 - CURRENT_POINT_SIZE, y1, x1, y1 - CURRENT_POINT_SIZE);
		}

	}

	protected void doPaintNoScale(final Graphics g) {
		g.drawString("No Track or no points yet", width - 2, 2, Graphics.TOP | Graphics.RIGHT);
	}

	protected void onResize() {
		onScaleChanged();
	}

	private void updateScale() {
		if (track == null || track.getPointCount() == 0) {
			xAxis.scale = Double.NaN;
			yAxis.scale = Double.NaN;
			return;
		}
		final boolean xChanged = xAxis.updateMinMax(xData, track);
		final boolean yChanged = yAxis.updateMinMax(yData, track);
		if (xChanged || yChanged) {
			onScaleChanged();
		}
	}

	protected void onScaleChanged() {
		if (!(xAxis.hasMinMax() && yAxis.hasMinMax())) {
			xAxis.scale = Double.NaN;
			yAxis.scale = Double.NaN;
			return;
		}

		final int spaceX = width - (getMarginLeft() + getMarginRight());
		final int spaceY = height - (getMarginTop() + getMarginBottom());

		xAxis.calculateScale(spaceX, xData.getSmallDelta());
		yAxis.calculateScale(spaceY, yData.getSmallDelta());

		if (linkedScale) {
			xAxis.scale = yAxis.scale = Math.max(xAxis.scale, yAxis.scale);
		}

		xAxis.calculateOffset(spaceX);
		yAxis.calculateOffset(spaceY);
	}

	public void currentPointChanged(final TrackPoint newPoint, final int newIndex) {
		if (manager.getTrack() != track) {
			track = manager.getTrack();
			updateScale();
		}
	}

	public void newPoint(final TrackPoint newPoint, final boolean boundsChanged, final boolean newSegment) {
		updateScale();
	}

	public void showNotify() {
		if (manager.getTrack() != track) {
			track = manager.getTrack();
		}
		updateScale();
	}

	protected AxisConfiguration getXAxis() {
		return xAxis;
	}

	protected AxisConfiguration getYAxis() {
		return yAxis;
	}

	public DataProvider getXData() {
		return xData;
	}

	public DataProvider getYData() {
		return yData;
	}

	protected static class AxisConfiguration {
		double minValue = Double.NaN;

		double maxValue = Double.NaN;

		double scale;

		double offset;

		boolean hasMinMax() {
			return !(Double.isNaN(minValue) || Double.isNaN(maxValue));
		}

		boolean updateMinMax(final DataProvider data, final Track track) {
			final double newMin = data.getMinValue(track);
			final double newMax = data.getMaxValue(track);
			if (newMin != minValue || newMax != maxValue) {
				minValue = newMin;
				maxValue = newMax;
				return true;
			} else {
				return false;
			}
		}

		/**
		 * Calculates a scale factor for this axis.
		 * 
		 * @param space
		 *            the number of display units available for this axis
		 * @param smallDelta
		 *            a "small delta" for the values on this axis, describes a minimum range.
		 */
		void calculateScale(final int space, final double smallDelta) {
			double range = maxValue - minValue;
			if (range < smallDelta && range > -smallDelta) {
				range = (range < 0) ? -smallDelta : smallDelta;
			}
			scale = range / space;
		}

		void calculateOffset(final int space) {
			offset = ((maxValue + minValue) - (space * scale)) / 2;
		}

		int getPosition(final double value) {
			return (int) ((value - offset) / scale);
		}
	}
}