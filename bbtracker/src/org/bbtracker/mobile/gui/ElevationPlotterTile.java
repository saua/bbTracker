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

import org.bbtracker.UnitConverter.ScaleConfiguration;
import org.bbtracker.mobile.Preferences;
import org.bbtracker.mobile.TrackManager;

public class ElevationPlotterTile extends AxisPlotterTile {
	public ElevationPlotterTile(final TrackManager manager, final DataProvider xAxis) {
		super(manager, xAxis, DataProvider.ELEVATION);
	}

	protected ScaleConfiguration newScaleConfiguration() {
		final int min = (int) getYAxis().minValue;
		final int max = (int) getYAxis().maxValue;
		return Preferences.getInstance().getUnitsConverter().getScaleElevation(min, max);
	}
}
