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

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;
import org.bbtracker.UnitConverter;
import org.bbtracker.Utils;
import org.bbtracker.mobile.Preferences;
import org.bbtracker.mobile.TrackManager;

public class StatusTile extends Tile {
	private static final int ONE_KILOMETER = 1000;

	private final TrackManager manager;

	/** Widget. */
	private final MiniWidget widgetLatitude = new MiniWidget();
	/** Widget. */
	private final MiniWidget widgetLongitude = new MiniWidget();
	/** Widget. */
	private final MiniWidget widgetSpeed = new MiniWidget();
	/** Widget. */
	private final MiniWidget widgetHeartRate = new MiniWidget();
	/** Widget. */
	private final MiniWidget widgetDistance = new MiniWidget();
	/** Widget. */
	private final MiniWidget widgetElevation = new MiniWidget();
	/** Widget. */
	private final MiniWidget widgetOrientation = new MiniWidget();
	/** Widget. */
	private final MiniWidget widgetOrientationArrow = new MiniWidget();
	/** Widget. */
	private final MiniWidget widgetTime = new MiniWidget();
	/** Widget. */
	private final MiniWidget widgetPointIndex = new MiniWidget();
	/** Widget. */
	private final MiniWidget widgetAverageTotal = new MiniWidget();
	/** Widget. */
	private final MiniWidget widgetAverageLastKm = new MiniWidget();
	/** Widget. */
	private final MiniWidget widgetInclinationInstant = new MiniWidget();
	/** Widget. */
	private final MiniWidget widgetInclinationLastKm = new MiniWidget();

	private final MiniWidget[] allWidgets = new MiniWidget[] { widgetLatitude, widgetLongitude, widgetSpeed,
			widgetHeartRate, widgetDistance, widgetElevation, widgetOrientation, widgetOrientationArrow, widgetTime,
			widgetPointIndex, widgetAverageLastKm, widgetAverageTotal, widgetInclinationInstant,
			widgetInclinationLastKm };

	public StatusTile(final TrackManager manager) {
		this.manager = manager;
		setFont(Preferences.getInstance().getStatusFont());
	}

	private void setFont(final Font font) {
		for (int i = 0; i < allWidgets.length; i++) {
			allWidgets[i].setFont(font);
		}
	}

	protected void onResize() {
		getPreferredHeight(width);
	}

	private int layout(final int width) {
		final UnitConverter unit = Preferences.getInstance().getUnitsConverter();

		widgetLatitude.setDimensionsForString(unit.getCoordinateTemplate());
		widgetLongitude.setDimensionsForString(unit.getCoordinateTemplate());
		widgetSpeed.setDimensionsForString(unit.getSpeedTemplate());
		widgetHeartRate.setDimensionsForString(unit.getHeartRateTemplate());
		widgetDistance.setDimensionsForString(unit.getDistanceTemplate());
		widgetElevation.setDimensionsForString(unit.getElevationTemplate());
		widgetSpeed.setDimensionsForString(unit.getSpeedTemplate());
		widgetOrientation.setDimensionsForString("99X XX");
		widgetPointIndex.setDimensionsForString("9999/9999");
		widgetTime.setDimensionsForString("9:99:99");
		widgetOrientationArrow.setOrientationWidth();
		widgetAverageLastKm.setDimensionsForString(unit.getSpeedTemplate());
		widgetAverageTotal.setDimensionsForString(unit.getSpeedTemplate());
		widgetInclinationInstant.setDimensionsForString(Utils.getInclinationTemplate());
		widgetInclinationLastKm.setDimensionsForString(Utils.getInclinationTemplate());

		// TODO: Do customization through preferences
		final WidgetLayouter layouter = new WidgetLayouter();
		layouter.setTopToBottom(true);
		layouter.setY(0);
		layouter.setWidth(width);
		// notused:
		layouter.addLine(new Widget[] { widgetLatitude, widgetLongitude });
		layouter.addLine(new Widget[] { widgetSpeed, widgetOrientationArrow, widgetElevation, widgetHeartRate,
				widgetInclinationLastKm });
		layouter.addLine(new Widget[] { widgetTime, widgetDistance, widgetPointIndex, widgetAverageTotal });
		// layouter.addLine(new Widget[] { widgetAverageTotal,
		// widgetAverageLastKm, widgetInclinationLastKm,
		// widgetInclinationInstant, });

		return layouter.getY();
	}

	private void update() {
		final Track track = manager.getTrack();
		final TrackPoint p = manager.getCurrentPoint();
		final int pi = manager.getCurrentPointIndex();
		final UnitConverter unit = Preferences.getInstance().getUnitsConverter();

		final String point;
		if (pi == -1) {
			point = "-";
		} else {
			point = (pi + 1) + "/" + track.getPointCount();
		}
		double lonValue = Double.NaN;
		double latValue = Double.NaN;
		float speedValue = Float.NaN;
		float courseValue = Float.NaN;
		float elevationValue = Float.NaN;
		int heartRate = manager.getHeartRateProvider().getHeartRate();
		double lengthValue = Double.NaN;
		long timeValue = -1;
		if (p != null) {
			lonValue = p.getLongitude();
			latValue = p.getLatitude();
			speedValue = p.getSpeed();
			courseValue = p.getCourse();
			elevationValue = p.getElevation();
			heartRate = p.getHeartRate();
			if (track != null) {
				timeValue = track.getPointOffset(p);
				lengthValue = p.getDistance();
			}
		}
		TrackPoint lastP = null;
		TrackPoint lastKmP = null;
		if (track != null) {
			try {
				lastP = track.getPoint(pi - 1);
				for (int i = pi - 1; i > 0; i--) {
					lastKmP = track.getPoint(i);
					if (lengthValue - lastKmP.getDistance() >= ONE_KILOMETER) {
						break;
					}
				}
			} catch (final IndexOutOfBoundsException e) {
				// ignore
			}
		}
		final StringBuffer speedBuffer = new StringBuffer();
		if (timeValue > 0 && lengthValue > 0) {
			speedBuffer.append(" (avg:");
			speedBuffer.append(unit.speedToString(timeValue, lengthValue));
			speedBuffer.append(")");
		}
		widgetInclinationInstant.setText(Utils.inclinationToString(getInclination(elevationValue, lengthValue, lastP)));
		widgetInclinationLastKm
				.setText(Utils.inclinationToString(getInclination(elevationValue, lengthValue, lastKmP)));
		String speedLastKm;
		if (lastKmP != null) {
			speedLastKm = unit.speedToString(timeValue - track.getPointOffset(lastKmP), lengthValue
					- lastKmP.getDistance());
		} else {
			speedLastKm = "-km/h";
		}
		widgetAverageLastKm.setText(speedLastKm);
		widgetAverageTotal.setText(unit.speedToString(timeValue, lengthValue));
		widgetLongitude.setText(Utils.longitudeToString(lonValue));
		widgetLatitude.setText(Utils.latitudeToString(latValue));
		widgetOrientation.setText(Utils.courseToString(courseValue) + " " + Utils.courseToHeadingString(courseValue));
		widgetOrientationArrow.setOrientation((int) courseValue);

		widgetSpeed.setText(unit.speedToString(speedValue));
		widgetHeartRate.setText(Utils.heartRateToString(heartRate));
		widgetElevation.setText(unit.elevationToString(elevationValue));
		widgetDistance.setText(unit.distanceToString(lengthValue));
		widgetTime.setText(timeValue == -1 ? "-" : Utils.durationToString(timeValue));
		widgetPointIndex.setText(point);
	}

	private double getInclination(final float elevationValue, final double lengthValue, final TrackPoint lastP) {
		if (lastP != null) {
			return (elevationValue - lastP.getElevation()) / (lengthValue - lastP.getDistance());
		} else {
			return Double.NaN;
		}
	}

	protected void doPaint(final Graphics g) {
		g.setColor(0x00ffffff);
		g.fillRect(0, 0, width, height);

		update();

		for (int i = 0; i < allWidgets.length; i++) {
			allWidgets[i].paint(g);
		}
	}

	public void showNotify() {
		// nothing to do
	}

	public int getPreferredHeight(final int width) {
		final Preferences pref = Preferences.getInstance();
		setFont(pref.getStatusFont());

		return layout(width);
	}
}