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

import javax.microedition.lcdui.Graphics;

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;
import org.bbtracker.mobile.Preferences;
import org.bbtracker.mobile.TrackManager;

public abstract class PlotterTile extends Tile {
	public static final int DEFAULT_MARGIN = 5;

	private final TrackManager manager;

	private final AxisConfiguration xAxis = new AxisConfiguration();

	private final AxisConfiguration yAxis = new AxisConfiguration();

	private final boolean linkedScale;

	private int marginLeft = DEFAULT_MARGIN;

	private int marginRight = DEFAULT_MARGIN;

	private int marginTop = DEFAULT_MARGIN;

	private int marginBottom = DEFAULT_MARGIN;

	private final DataProvider xData;

	private final DataProvider yData;

	private final TrackPlotter mainTrackPlotter;

	private final TrackPlotter extraTrackPlotter;

	public PlotterTile(final TrackManager manager, final DataProvider xData, final DataProvider yData,
			final boolean linkedScale) {
		this.manager = manager;
		this.linkedScale = linkedScale;
		this.xData = xData;
		this.yData = yData;
		mainTrackPlotter = new TrackPlotter();
		mainTrackPlotter.setTrackColor(0);
		extraTrackPlotter = new TrackPlotter();
		extraTrackPlotter.setTrackColor(0x801010);
		extraTrackPlotter.setTrackStyle(TrackPlotter.WIDE);
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

		if (isInvalidAxis()) {
			doPaintNoScale(g);
			return;
		}

		doPaintBackground(g);
		doPaintPlot(g);
		doPaintAxis(g);
	}

	private boolean isInvalidAxis() {
		return Double.isNaN(xAxis.scale) || Double.isNaN(yAxis.scale);
	}

	protected void doPaintPlot(final Graphics g) {
		final Track extraTrack = manager.getExtraTrack();
		if (extraTrack != null) {
			extraTrackPlotter.paint(g, xData, yData, xAxis, yAxis, getMarginLeft(), getMarginTop(), height, extraTrack);
		}
		mainTrackPlotter.paint(g, xData, yData, xAxis, yAxis, getMarginLeft(), getMarginTop(), height, manager
				.getTrack());
		final TrackPoint currentPoint = manager.getCurrentPoint();
		if (currentPoint != null) {
			mainTrackPlotter.paintCurrentPoint(g, currentPoint, xData, yData, xAxis, yAxis, getMarginLeft(),
					getMarginTop(), height);
		}
	}

	protected void doPaintBackground(final Graphics g) {
	}

	protected abstract void doPaintAxis(final Graphics g);

	protected void doPaintNoScale(final Graphics g) {
		g.setFont(Preferences.getInstance().getStatusFont());
		g.drawString("nothing to plot, yet", width / 2, height / 2, Graphics.BASELINE | Graphics.HCENTER);
	}

	protected void onResize() {
		onScaleChanged();
	}

	private void updateScale() {
		final Track track = manager.getTrack();
		if (track != null) {
			resetScale();
			updateScale(track);
			updateScale(manager.getExtraTrack());
			onScaleChanged();
		}
	}

	private void resetScale() {
		xAxis.resetMinMax();
		yAxis.resetMinMax();
	}

	public void stateChanged(final int newState) {
		// do nothing
	}

	private boolean updateScale(final Track track) {
		if (track != null && track.getLength() > 0) {
			final boolean xChanged = xAxis.updateMinMax(xData, track);
			final boolean yChanged = yAxis.updateMinMax(yData, track);
			return xChanged || yChanged;
		} else {
			return false;
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

	protected void onScaleChanged(final double scaleX, final double scaleY, final double centerX, final double centerY) {
		if (!(xAxis.hasMinMax() && yAxis.hasMinMax())) {
			xAxis.scale = Double.NaN;
			yAxis.scale = Double.NaN;
			return;
		}

		final int spaceX = width - (getMarginLeft() + getMarginRight());
		final int spaceY = height - (getMarginTop() + getMarginBottom());

		xAxis.scale = scaleX;
		yAxis.scale = scaleY;

		xAxis.calculateOffset(centerX, spaceX);
		yAxis.calculateOffset(centerY, spaceY);
	}

	public void currentPointChanged(final TrackPoint newPoint, final int newIndex) {
		// pool the track as long as no point is present
		if (isInvalidAxis()) {
			updateScale();
		}
	}

	public void newPoint(final TrackPoint newPoint, final boolean boundsChanged, final boolean newSegment) {
		updateScale();
	}

	public void showNotify() {
		if (isInvalidAxis()) {
			updateScale();
		}
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
		double minValue = Double.MAX_VALUE;

		double maxValue = Double.MIN_VALUE;

		double scale;

		double offset;

		boolean hasMinMax() {
			return !(minValue > maxValue);
		}

		void resetMinMax() {
			minValue = Double.MAX_VALUE;
			maxValue = Double.MIN_VALUE;
		}

		boolean updateMinMax(final DataProvider data, final Track track) {
			final double newMin = data.getMinValue(track);
			final double newMax = data.getMaxValue(track);
			boolean update = false;
			if (newMin < minValue) {
				minValue = newMin;
				update = true;
			}
			if (newMax > maxValue) {
				maxValue = newMax;
				update = true;
			}
			return update;
		}

		/**
		 * Calculates a scale factor for this axis.
		 * 
		 * @param space
		 *            the number of display units available for this axis
		 * @param smallDelta
		 *            a "small delta" for the values on this axis, describes a
		 *            minimum range.
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

		void calculateOffset(final double value, final int space) {
			offset = value - space * scale / 2;
		}

		int getPosition(final double value) {
			return (int) ((value - offset) / scale);
		}
	}
}