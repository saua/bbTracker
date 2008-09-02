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
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;

import org.bbtracker.ImperialUnitConverter;
import org.bbtracker.MetricUnitConverter;
import org.bbtracker.NauticalUnitConverter;
import org.bbtracker.UnitConverter;
import org.bbtracker.mobile.config.ConfigFile;

public class Preferences {

	private static final String RECORD_STORE_NAME = "PreferencesV2";
	private static final String OLD_RECORD_STORE_NAME = "Preferences";

	private static final String CONFIGURATION_VERSION = "configurationVersion";

	private static final String START_ACTION = "startAction";
	private static final String SAMPLE_INTERVAL = "sampleInterval";
	private static final String STRING = "units";
	private static final String TRACK_DIRECTORY = "trackDirectory";
	private static final String EXPORT_DIRECTORY = "exportDirectory";
	private static final String MAP_DIRECTORY = "mapDirectory";
	private static final String EXPORT_FORMATS_KEY = "exportFormats";
	private static final String TRACK_NUMBER = "trackNumber";
	private static final String BLUETOOTH_NAME = "bluetoothName";
	private static final String BLUETOOTH_URL = "bluetoothUrl";
	private static final String DETAILS_FONT_SIZE = "detailsFontSize";
	private static final String STATUS_FONT_SIZE = "statusFontSize";
	private static final String HEARTRATE_ENABLED = "heartrateEnabled";

	public static final int START_ACTION_SHOW_OPTIONS = -1;

	public static final int START_ACTION_NOTHING = 0;

	public static final int START_ACTION_INIT_GPS = 1;

	public static final int START_ACTION_NEWTRACK = 2;

	public static final int START_ACTION_TRACKS_SCREEN = 3;

	public static final int START_ACTION_NEW_VERSION = 4;

	public static final int DEFAULT_START_ACTION = START_ACTION_INIT_GPS;

	public static String[] START_ACTIONS = new String[] { "Do nothing", "Initialize GPS", "Start new track",
			"Open Track Screen" };

	public static final int EXPORT_KML = 0;

	public static final int EXPORT_GPX = 1;

	public static String[] EXPORT_FORMATS = new String[] { "KML (Google Earth)", "GPX" };

	public static final int UNITS_METRIC = 0;

	public static final int UNITS_IMPERIAL = 1;

	public static final int UNITS_NAUTICAL = 2;

	public static String[] UNITS = new String[] { "Metric (km/h, km, m)", "Imperial (mph, miles, feet)",
			"Nautical (nm/h, nm, feet)" };

	public static final int LOCATION_JSR179 = 0;

	public static final int LOCATION_BLUETOOTH = 1;

	public static final int LOCATION_NONE = 2;

	public static final int DEFAULT_LOCATION_PROVIDER = LOCATION_JSR179;

	public static String[] LOCATION_ACCESS = new String[] { "Location API (JSR-179)", "Bluetooth API", "No GPS" };

	private static Preferences instance;

	public static synchronized Preferences getInstance() {
		if (instance == null) {
			instance = new Preferences();
			instance.load();
		}
		return instance;
	}

	private boolean newVersion = true;
	private ConfigFile conf;

	// cached values
	private transient UnitConverter unitConverter;
	private transient Font statusFont;
	private transient Font detailsFont;

	private Preferences() {
	}

	public int getLocationProvider() {
		return conf.getInteger("locationProvider", DEFAULT_LOCATION_PROVIDER);
	}

	public void setLocationProvider(final int locationProvider) {
		switch (locationProvider) {
		case LOCATION_BLUETOOTH:
		case LOCATION_JSR179:
		case LOCATION_NONE:
			conf.put("locationProvider", locationProvider);
			break;
		default:
			throw new IllegalArgumentException("Illegal LocationProvider value: " + locationProvider);
		}
	}

	public int getSampleInterval() {
		return conf.getInteger(SAMPLE_INTERVAL, 5);
	}

	public void setSampleInterval(final int sampleInterval) {
		conf.put(SAMPLE_INTERVAL, sampleInterval);
	}

	public int getStartAction() {
		return conf.getInteger(START_ACTION, DEFAULT_START_ACTION);
	}

	public void setStartAction(final int startAction) {
		conf.put(START_ACTION, startAction);
	}

	public String getMapDirectory() {
		return conf.get(MAP_DIRECTORY);
	}

	public String getTrackDirectory() {
		return conf.get(TRACK_DIRECTORY);
	}

	public String getExportDirectory() {
		return conf.get(EXPORT_DIRECTORY);
	}

	public String getEffectiveExportDirectory() {
		String dir = getExportDirectory();
		if (dir == null) {
			dir = getTrackDirectory();
		}
		return dir;
	}

	public void setMapDirectory(final String mapDirectory) {
		setDir(MAP_DIRECTORY, mapDirectory);
	}

	public void setTrackDirectory(final String trackDirectory) {
		setDir(TRACK_DIRECTORY, trackDirectory);
	}

	public void setExportDirectory(final String exportDirectory) {
		setDir(EXPORT_DIRECTORY, exportDirectory);
	}

	private void setDir(final String key, final String value) {
		String dir;
		if (value == null || value.length() == 0) {
			dir = null;
		} else {
			dir = value;
			if (!dir.endsWith("/")) {
				dir += "/";
			}
		}
		conf.put(key, dir);
	}

	public void setExportFormat(final int index, final boolean value) {
		if (index >= EXPORT_FORMATS.length || index < 0) {
			throw new IllegalArgumentException();
		}
		int f = getExportFormats();
		if (value) {
			f |= 1 << index;
		} else {
			f &= ~(1 << index);
		}
		setExportFormats(f);
	}

	public boolean getExportFormat(final int index) {
		return (getExportFormats() & (1 << index)) != 0;
	}

	private int getExportFormats() {
		// by default export format 0 and 1 are set
		return conf.getInteger(EXPORT_FORMATS_KEY, 0x03);
	}

	private void setExportFormats(final int exportFormats) {
		conf.put(EXPORT_FORMATS_KEY, exportFormats);
	}

	public int getUnits() {
		return conf.getInteger(STRING, UNITS_METRIC);
	}

	public void setUnits(final int units) {
		if (units != UNITS_METRIC && units != UNITS_IMPERIAL && units != UNITS_NAUTICAL) {
			throw new IllegalArgumentException();
		}
		conf.put(STRING, units);
		unitConverter = null;
	}

	public UnitConverter getUnitsConverter() {
		if (unitConverter == null) {
			switch (getUnits()) {
			case UNITS_METRIC:
				unitConverter = new MetricUnitConverter();
				break;
			case UNITS_IMPERIAL:
				unitConverter = new ImperialUnitConverter();
				break;
			case UNITS_NAUTICAL:
				unitConverter = new NauticalUnitConverter();
				break;
			default:
				throw new IllegalStateException();
			}
		}
		return unitConverter;
	}

	public int getStatusFontSize() {
		return conf.getInteger(STATUS_FONT_SIZE, Font.SIZE_MEDIUM);
	}

	public void setStatusFontSize(final int statusFontSize) {
		conf.put(STATUS_FONT_SIZE, statusFontSize);
		statusFont = null;
	}

	public Font getStatusFont() {
		if (statusFont == null) {
			statusFont = Font.getFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, getStatusFontSize());
		}
		return statusFont;
	}

	public int getDetailsFontSize() {
		return conf.getInteger(DETAILS_FONT_SIZE, Font.SIZE_MEDIUM);
	}

	public void setDetailsFontSize(final int detailsFontSize) {
		conf.put(DETAILS_FONT_SIZE, detailsFontSize);
		detailsFont = null;
	}

	public Font getDetailsFont() {
		if (detailsFont == null) {
			detailsFont = Font.getFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, getDetailsFontSize());
		}
		return detailsFont;
	}

	public String getBluetoothUrl() {
		return conf.get(BLUETOOTH_URL, "");
	}

	public void setBluetoothUrl(final String bluetoothUrl) {
		conf.put(BLUETOOTH_URL, bluetoothUrl == null ? "" : bluetoothUrl);
	}

	public String getBluetoothName() {
		return conf.get(BLUETOOTH_NAME, "");
	}

	public void setBluetoothName(final String bluetoothName) {
		conf.put(BLUETOOTH_NAME, bluetoothName == null ? "" : bluetoothName);
	}

	public boolean isHeartRateEnabled() {
		return conf.getBoolean(HEARTRATE_ENABLED, false);
	}

	public void setHeartRateEnabled(final boolean enabled) {
		conf.put(HEARTRATE_ENABLED, enabled);
	}

	public int getTrackNumber() {
		return conf.getInteger(TRACK_NUMBER, 1);
	}

	public void setTrackNumber(final int trackNumber) {
		conf.put(TRACK_NUMBER, trackNumber);
	}

	public int getNextTrackNumber() {
		final int result = getTrackNumber() + 1;
		setTrackNumber(result);
		return result;
	}

	public boolean isNewVersion() {
		return newVersion;
	}

	private void load() {
		if (!loadPreferences()) {
			conf = ConfigFile.createEmtpyConfig();
			loadOldPreferences();
			newVersion = true;
		}
		if (newVersion) {
			Log.log(this, "New bbTracker Version!");
			setStartAction(START_ACTION_SHOW_OPTIONS);
			conf.put(CONFIGURATION_VERSION, BBTracker.getVersion());
		}
	}

	private boolean loadPreferences() {
		final byte[] data = loadRecordStore(RECORD_STORE_NAME);
		if (data == null) {
			Log.log(this, "No preferences Record Store found.");
			return false;
		}
		try {
			Log.log(this, "Loading preferences from Record Store.");
			conf = ConfigFile.openCSVConfig(new ByteArrayInputStream(data));
			final String s = conf.get(CONFIGURATION_VERSION);
			newVersion = !BBTracker.getVersion().equals(s);
			return true;
		} catch (final IOException e) {
			Log.log(this, e, "loading preferences");
			return false;
		}
	}

	private byte[] loadRecordStore(final String rsName) {
		RecordStore rs = null;
		try {
			rs = RecordStore.openRecordStore(rsName, false);

			byte[] data = null;
			;
			final RecordEnumeration enumerateRecords = rs.enumerateRecords(null, null, false);
			if (enumerateRecords.hasNextElement()) {
				data = enumerateRecords.nextRecord();
			}
			enumerateRecords.destroy();
			return data;
		} catch (final RecordStoreNotFoundException e) {
			return null;
		} catch (final RecordStoreException e) {
			Log.log(this, e, "loading RecordStore " + rsName);
			return null;
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

	private void loadOldPreferences() {
		final byte[] data = loadRecordStore(OLD_RECORD_STORE_NAME);
		if (data == null) {
			Log.log(this, "No old Preferences Record Store found!");
			return;
		}

		Log.log(this, "Loading old Preferences from Record store.");
		final DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		try {
			short s = in.readShort();
			if (s == -1) {
				// new options format begins with -1 followed by string
				// containing version that saved the options
				in.readUTF();
				s = in.readShort();
				// old options format began with startAction
			}
			setSampleInterval(in.readInt());
			setTrackNumber(in.readInt());
			final byte dirFlags = in.readByte();
			if ((dirFlags & 1) != 0) {
				setTrackDirectory(in.readUTF());
			}
			if ((dirFlags & 2) != 0) {
				setExportDirectory(in.readUTF());
			}
			if ((dirFlags & 4) != 0) {
				setMapDirectory(in.readUTF());
			}
			setExportFormats(in.readInt());
			setUnits(in.readInt());
			setStatusFontSize(in.readInt());
			setDetailsFontSize(in.readInt());
			setLocationProvider(in.readInt());
			setBluetoothUrl(in.readUTF());
			setBluetoothName(in.readUTF());
		} catch (final IOException e) {
			Log.log(this, e, "loading old preferences");
		} finally {
			try {
				in.close();
			} catch (final IOException ignored) {
				// ignore
			}
		}
	}

	public void store() throws RecordStoreException {
		RecordStore rs = null;
		try {
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final DataOutputStream out = new DataOutputStream(baos);
			conf.saveCSVConfig(out);
			final byte[] data = baos.toByteArray();

			rs = RecordStore.openRecordStore(RECORD_STORE_NAME, true);
			final RecordEnumeration enumerateRecords = rs.enumerateRecords(null, null, false);
			if (enumerateRecords.hasNextElement()) {
				final int recordIndex = enumerateRecords.nextRecordId();
				rs.setRecord(recordIndex, data, 0, data.length);
			} else {
				rs.addRecord(data, 0, data.length);
			}
			enumerateRecords.destroy();
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
