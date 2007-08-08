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
import org.bbtracker.mobile.Preferences;
import org.bbtracker.mobile.TrackManager;

public class TrackTile extends PlotterTile {
	private static final int SCALE_HEIGTH = 5;

	private int scaleSizeInPixel;

	private ScaleConfiguration scaleConfiguration;

	public TrackTile(final TrackManager manager) {
		super(manager, DataProvider.LONGITUDE, DataProvider.LATITUDE, true);
	}

	protected void onScaleChanged() {
		super.onScaleChanged();
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
		if (scaleSizeInPixel == 0) {
			return;
		}

		final Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
		g.setFont(font);

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
			g.drawString(label, left + (int) (scaleSizeInPixel * location), textBottom, Graphics.BOTTOM |
					Graphics.HCENTER);
		}
		g.setColor(0x00000000);
		g.drawRect(left, height - 2 - SCALE_HEIGTH, scaleSizeInPixel, SCALE_HEIGTH);
		g.fillRect(left, height - 2 - SCALE_HEIGTH, scaleSizeInPixel / 2, SCALE_HEIGTH);
	}
}
