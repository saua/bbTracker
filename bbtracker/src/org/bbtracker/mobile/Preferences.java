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
package org.bbtracker.mobile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.microedition.lcdui.Font;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;

import org.bbtracker.ImperialUnitConverter;
import org.bbtracker.MetricUnitConverter;
import org.bbtracker.UnitConverter;

public class Preferences {
	private static final String RECORD_STORE_NAME = "Preferences";

	public static final int START_ACTION_SHOW_OPTIONS = -1;

	public static final int START_ACTION_NOTHING = 0;

	public static final int START_ACTION_INIT_GPS = 1;

	public static final int START_ACTION_NEWTRACK = 2;

	public static final int START_ACTION_TRACKS_SCREEN = 3;

	public static final int DEFAULT_START_ACTION = START_ACTION_INIT_GPS;

	public static String[] START_ACTIONS = new String[] { "Do nothing", "Initialize GPS", "Start new track",
			"Open Track Screen" };

	public static final int EXPORT_KML = 0;

	public static final int EXPORT_GPX = 1;

	public static String[] EXPORT_FORMATS = new String[] { "KML (Google Earth)", "GPX" };

	public static final int UNITS_METRIC = 0;

	public static final int UNITS_IMPERIAL = 1;

	public static String[] UNITS = new String[] { "Metric (km/h, km, m)", "Imperial (mph, miles, feet)" };

	private static Preferences instance;

	public static synchronized Preferences getInstance() {
		if (instance == null) {
			instance = new Preferences();
			try {
				instance.load();
			} catch (final RecordStoreException ignored) {
				// ignore
			}
		}
		return instance;
	}

	private int recordIndex = -1;

	private int sampleInterval = 5;

	private int startAction = DEFAULT_START_ACTION;

	private int trackNumber = 1;

	private int exportFormats = 0x03; // export format 0 and 1 are set

	private int units = UNITS_METRIC;

	private int statusFontSize = Font.SIZE_MEDIUM;

	private int detailsFontSize = Font.SIZE_LARGE;

	private String trackDirectory;

	private UnitConverter unitConverter;

	private Preferences() {
	}

	public int getSampleInterval() {
		return sampleInterval;
	}

	public void setSampleInterval(final int sampleInterval) {
		this.sampleInterval = sampleInterval;
	}

	public int getStartAction() {
		return startAction;
	}

	public void setStartAction(final int startAction) {
		this.startAction = startAction;
	}

	public String getTrackDirectory() {
		return trackDirectory;
	}

	public void setTrackDirectory(final String trackDirectory) {
		if (trackDirectory == null || trackDirectory.length() == 0) {
			this.trackDirectory = null;
		} else {
			this.trackDirectory = trackDirectory;
			if (!this.trackDirectory.endsWith("/")) {
				this.trackDirectory += "/";
			}
		}
	}

	public void setExportFormat(final int index, final boolean value) {
		if (index >= EXPORT_FORMATS.length || index < 0) {
			throw new IllegalArgumentException();
		}
		if (value) {
			exportFormats |= 1 << index;
		} else {
			exportFormats &= ~(1 << index);
		}
	}

	public boolean getExportFormat(final int index) {
		return (exportFormats & (1 << index)) != 0;
	}

	public int getUnits() {
		return units;
	}

	public void setUnits(final int units) {
		if (units != UNITS_METRIC && units != UNITS_IMPERIAL) {
			throw new IllegalArgumentException();
		}
		this.units = units;
		unitConverter = null;
	}

	public UnitConverter getUnitsConverter() {
		if (unitConverter == null) {
			switch (units) {
			case UNITS_METRIC:
				unitConverter = new MetricUnitConverter();
				break;
			case UNITS_IMPERIAL:
				unitConverter = new ImperialUnitConverter();
				break;
			default:
				throw new IllegalStateException();
			}
		}
		return unitConverter;
	}

	public int getStatusFontSize() {
		return statusFontSize;
	}

	public void setStatusFontSize(final int statusFontSize) {
		this.statusFontSize = statusFontSize;
	}

	public int getDetailsFontSize() {
		return detailsFontSize;
	}

	public void setDetailsFontSize(final int detailsFontSize) {
		this.detailsFontSize = detailsFontSize;
	}

	public int getNextTrackNumber() {
		return trackNumber++;
	}

	private void load() throws RecordStoreException {
		RecordStore rs = null;
		try {
			rs = RecordStore.openRecordStore(RECORD_STORE_NAME, false);

			final byte[] data;
			if (recordIndex != -1) {
				data = rs.getRecord(recordIndex);
			} else {
				final RecordEnumeration enumerateRecords = rs.enumerateRecords(null, null, false);
				if (enumerateRecords.hasNextElement()) {
					data = enumerateRecords.nextRecord();
					enumerateRecords.destroy();
				} else {
					enumerateRecords.destroy();
					return;
				}
			}

			final DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));

			try {
				startAction = in.readShort();
				sampleInterval = in.readInt();
				trackNumber = in.readInt();
				if (in.readByte() != 0) {
					trackDirectory = in.readUTF();
				} else {
					trackDirectory = null;
				}
				exportFormats = in.readInt();
				units = in.readInt();
				statusFontSize = in.readInt();
				detailsFontSize = in.readInt();
			} finally {
				try {
					in.close();
				} catch (final IOException ignored) {
					// ignore
				}
			}

		} catch (final RecordStoreNotFoundException e) {
			// ignore, don't load anything, but show options screen
			startAction = START_ACTION_SHOW_OPTIONS;
		} catch (final InvalidRecordIDException e) {
			// ignore, don't load anything, but show options screen
			startAction = START_ACTION_SHOW_OPTIONS;
		} catch (final IOException e) {
			startAction = START_ACTION_SHOW_OPTIONS;
			throw new RecordStoreException(e.getMessage());
		} finally {
			if (rs != null) {
				try {
					rs.closeRecordStore();
				} catch (final RecordStoreException e) {
					// ignore
				}
			}
		}
	}

	public void store() throws RecordStoreException {
		RecordStore rs = null;
		try {
			rs = RecordStore.openRecordStore(RECORD_STORE_NAME, true);

			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final DataOutputStream out = new DataOutputStream(baos);

			out.writeShort(startAction);
			out.writeInt(sampleInterval);
			out.writeInt(trackNumber);
			if (trackDirectory == null) {
				out.writeByte(0);
			} else {
				out.writeByte(1);
				out.writeUTF(trackDirectory);
			}
			out.writeInt(exportFormats);
			out.writeInt(units);
			out.writeInt(statusFontSize);
			out.writeInt(detailsFontSize);

			out.close();
			final byte[] data = baos.toByteArray();

			if (recordIndex != -1) {
				rs.setRecord(recordIndex, data, 0, data.length);
			} else {
				final RecordEnumeration enumerateRecords = rs.enumerateRecords(null, null, false);
				if (enumerateRecords.hasNextElement()) {
					recordIndex = enumerateRecords.nextRecordId();
					rs.setRecord(recordIndex, data, 0, data.length);
				} else {
					recordIndex = rs.addRecord(data, 0, data.length);
				}
				enumerateRecords.destroy();
			}
		} catch (final IOException e) {
			throw new RecordStoreException(e.getMessage());
		} finally {
			if (rs != null) {
				try {
					rs.closeRecordStore();
				} catch (final RecordStoreException e) {
					// ignore
				}
			}
		}
	}
}
