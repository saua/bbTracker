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
