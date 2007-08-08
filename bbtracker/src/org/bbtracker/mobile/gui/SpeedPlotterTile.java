package org.bbtracker.mobile.gui;

import org.bbtracker.UnitConverter.ScaleConfiguration;
import org.bbtracker.mobile.Preferences;
import org.bbtracker.mobile.TrackManager;

public class SpeedPlotterTile extends AxisPlotterTile {
	public SpeedPlotterTile(final TrackManager manager, final DataProvider xAxis) {
		super(manager, xAxis, DataProvider.SPEED);
	}

	protected ScaleConfiguration newScaleConfiguration() {
		final int max = (int) getYAxis().maxValue;
		return Preferences.getInstance().getUnitsConverter().getScaleSpeed(max);
	}

}
