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

import org.bbtracker.TrackPoint;
import org.bbtracker.Utils;
import org.bbtracker.UnitConverter.ScaleConfiguration;
import org.bbtracker.mobile.Preferences;
import org.bbtracker.mobile.TrackManager;

public class TrackTile extends PlotterTile {
	private static final int SCALE_HEIGTH = 5;

	private int scaleSizeInPixel;

	private ScaleConfiguration scaleConfiguration;

	private TrackPoint currentPoint;

	private MapBackground mapBackground;

	private MainCanvas mainCanvas;

	/** Pan offset of the map in longitude. */
	private double panLong;

	/** Pan offset of the map in latitude. */
	private double panLatitude;

	public TrackTile(final TrackManager manager) {
		super(manager, DataProvider.LONGITUDE, DataProvider.LATITUDE, true);
	}

	protected void onScaleChanged() {
		if (mapBackground != null && currentPoint != null) {
			final double latitude = DataProvider.LATITUDE.getValue(currentPoint) + panLatitude;
			final double longitude = DataProvider.LONGITUDE.getValue(currentPoint) + panLong;
			super.onScaleChanged(mapBackground.getScaleX(), mapBackground.getScaleY(latitude), longitude, latitude);
		} else {
			super.onScaleChanged();
		}

		final double widthInMeter = Utils.distance(getYAxis().minValue, getXAxis().maxValue, getYAxis().maxValue,
				getXAxis().maxValue);
		if (widthInMeter < 1) {
			scaleSizeInPixel = 0;
			return;
		}

		final double availableLengthInMeter = widthInMeter * 0.9;

		scaleConfiguration = Preferences.getInstance().getUnitsConverter().getScaleDistance(availableLengthInMeter);

		scaleSizeInPixel = (int) ((scaleConfiguration.lengthInSourceUnits / widthInMeter) * width);
	}

	protected void doPaintAxis(final Graphics g) {
		if (scaleSizeInPixel == 0 || mapBackground != null) {
			return;
		}

		final Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
		g.setFont(font);
		g.setColor(0x00000000);

		final int textBottom = height - 4 - SCALE_HEIGTH;
		int left = 0;
		for (int i = 0; i < scaleConfiguration.labelLocation.length; i++) {
			final float location = scaleConfiguration.labelLocation[i];
			final float value = scaleConfiguration.labelValue[i];
			String label = Utils.floatToString(value, true);
			if (location == 0) {
				left = (font.stringWidth(label) / 2) + 2;
			} else if (location == 1) {
				label += " " + scaleConfiguration.unit;
			}
			g.drawString(label, left + (int) (scaleSizeInPixel * location), textBottom, Graphics.BOTTOM
					| Graphics.HCENTER);
		}
		g.drawRect(left, height - 2 - SCALE_HEIGTH, scaleSizeInPixel, SCALE_HEIGTH);
		g.fillRect(left, height - 2 - SCALE_HEIGTH, scaleSizeInPixel / 2, SCALE_HEIGTH);
	}

	public void currentPointChanged(final TrackPoint newPoint, final int newIndex) {
		super.currentPointChanged(newPoint, newIndex);
		currentPoint = newPoint;
		if (mapBackground != null) {
			// In map background mode, we need to readjust all axis everytime
			// the current point changes as the current point stays in the
			// center of the screen.
			onScaleChanged();
		}
	}

	protected void doPaintBackground(final Graphics g) {
		if (mapBackground != null) {
			mapBackground.paint(g, DataProvider.LONGITUDE.getValue(currentPoint) + panLong, DataProvider.LATITUDE
					.getValue(currentPoint)
					+ panLatitude, (width - getMarginLeft() - getMarginRight()) / 2 + getMarginLeft(), (height
					- getMarginTop() - getMarginBottom())
					/ 2 + getMarginTop());
		}
	}

	public void setMapBackground(final MapBackground background) {
		if (mapBackground != null) {
			mapBackground.stop();
		}
		if (background != null) {
			background.setMainCanvas(mainCanvas);
			background.start();
		}
		mapBackground = background;
		onScaleChanged();
	}

	public MapBackground getBackground() {
		return mapBackground;
	}

	public void setMainCanvas(final MainCanvas canvas) {
		mainCanvas = canvas;
	}

	public MapBackground getMapBackground() {
		return mapBackground;
	}

	/**
	 * Pan.
	 * 
	 * @param dx
	 *            position delta in pixels
	 * @param dy
	 *            position delta in pixels
	 */
	public void panPosition(final int dx, final int dy) {
		panLong += dx * getXAxis().scale;
		panLatitude -= dy * getYAxis().scale;
	}

	/**
	 * Reset pan.
	 * 
	 */
	public void resetMapPan() {
		panLong = 0;
		panLatitude = 0;
	}
}
