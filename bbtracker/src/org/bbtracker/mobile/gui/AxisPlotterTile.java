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

import org.bbtracker.Utils;
import org.bbtracker.UnitConverter.ScaleConfiguration;
import org.bbtracker.mobile.TrackManager;

public abstract class AxisPlotterTile extends PlotterTile {
	private static final int AXIS_COLOR = 0x00000000;

	private int xAxisLabelWidth = -1;

	private ScaleConfiguration xAxisScale;

	private final Font axisLabelFont;

	AxisPlotterTile(final TrackManager manager, final DataProvider xData, final DataProvider yData) {
		super(manager, xData, yData, false);
		axisLabelFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
	}

	protected void onScaleChanged() {
		super.onScaleChanged();
		xAxisScale = newScaleConfiguration();
		int maxLabelWidth = 0;
		for (int i = 0; i < xAxisScale.labelValue.length; i++) {
			final float label = xAxisScale.labelValue[i];
			if (Float.isNaN(label)) {
				continue;
			}
			maxLabelWidth = Math.max(maxLabelWidth, axisLabelFont.stringWidth(Utils.floatToString(label, true)));
		}
		if (xAxisLabelWidth != maxLabelWidth) {
			xAxisLabelWidth = maxLabelWidth;
			// 2 left of the text, 2 right, 3 for the axis, 2 space
			setMarginLeft(xAxisLabelWidth + 9);
		}
	}

	protected abstract ScaleConfiguration newScaleConfiguration();

	protected void doPaintAxis(final Graphics g) {
		final int axisX = getMarginLeft() - 1;
		final int top = getMarginTop();
		final int bottom = height - getMarginBottom();
		final int diff = bottom - top;
		g.setColor(AXIS_COLOR);
		g.drawLine(axisX, top, axisX, bottom);
		g.drawLine(axisX, top, axisX - 5, top + 5);
		g.drawLine(axisX, top, axisX + 5, top + 5);
		g.setFont(axisLabelFont);
		for (int i = xAxisScale.labelValue.length - 1; i >= 0; i--) {
			final float location = xAxisScale.labelLocation[i];
			if (Float.isNaN(location)) {
				continue;
			}
			final float value = xAxisScale.labelValue[i];
			final int y = bottom - (int) (location * diff);
			if (y - 5 <= top) {
				// don't draw the top notch, if it's too far up the scale (it would collide with the arrow)
				continue;
			}
			g.drawLine(axisX - 2, y, axisX, y);
			g.drawString(Utils.floatToString(value, true), axisX - 4, y - (axisLabelFont.getHeight() / 2),
					Graphics.TOP | Graphics.RIGHT);
		}
		g.drawString(getYData().getName() + " in " + xAxisScale.unit, axisX + 7, top, Graphics.TOP | Graphics.LEFT);
	}
}
