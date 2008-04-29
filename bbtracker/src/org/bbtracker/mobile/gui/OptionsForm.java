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

import java.io.IOException;

import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.TextField;
import javax.microedition.rms.RecordStoreException;

import net.benhui.btgallery.bluelet.BLUElet;

import org.bbtracker.mobile.BBTracker;
import org.bbtracker.mobile.IconManager;
import org.bbtracker.mobile.Log;
import org.bbtracker.mobile.Preferences;
import org.bbtracker.mobile.TrackManager;

public class OptionsForm extends Form implements CommandListener, ItemCommandListener {
	private static final String[] FONT_SIZE_NAMES = new String[] { "Small", "Medium", "Large" };

	private final TrackManager trackManager;

	// #ifndef AVOID_FILE_API
	private final Command browseTrackCommand;

	private final Command browseExportCommand;

	// #endif

	private final Command selectBluetoothDeviceCommand;

	private final ChoiceGroup locationProviderGroup;

	private final TextField bluetoothNameField;

	private String bluetoothUrl;

	private final TextField sampleField;

	private final ChoiceGroup startTypeGroup;

	// #ifndef AVOID_FILE_API
	private final TextField trackDirectoryField;

	private final TextField exportDirectoryField;

	private final ChoiceGroup exportFormatGroup;

	// #endif

	private final ChoiceGroup unitsGroup;

	private final ChoiceGroup statusFontSizeGroup;

	private final ChoiceGroup detailsFontSizeGroup;

	public OptionsForm(final TrackManager trackManager) {
		super("Options");

		this.trackManager = trackManager;

		final Preferences pref = Preferences.getInstance();

		final IconManager iconManager = IconManager.getInstance();
		final Image[] locationImages = new Image[] {
				iconManager.getChoiceGroupImage(BBTracker.isJsr179Available() ? "yes" : "no"),
				iconManager.getChoiceGroupImage(BBTracker.isBluetoothAvailable() ? "yes" : "no"),
				iconManager.getChoiceGroupImage("yes"), };
		locationProviderGroup = new ChoiceGroup("Location/GPS: ", Choice.POPUP, Preferences.LOCATION_ACCESS,
				locationImages);
		locationProviderGroup.setSelectedIndex(pref.getLocationProvider(), true);

		bluetoothUrl = pref.getBluetoothUrl();
		selectBluetoothDeviceCommand = new Command("Select GPS device", Command.ITEM, 0);
		bluetoothNameField = new TextField("GPS device: ", pref.getBluetoothName(), 20, TextField.ANY |
				TextField.UNEDITABLE);
		bluetoothNameField.setDefaultCommand(selectBluetoothDeviceCommand);
		bluetoothNameField.setItemCommandListener(this);

		sampleField = new TextField("Sample Interval in seconds: ", String.valueOf(pref.getSampleInterval()), 5,
				TextField.NUMERIC);

		unitsGroup = new ChoiceGroup("Units: ", Choice.POPUP, Preferences.UNITS, null);
		unitsGroup.setSelectedIndex(pref.getUnits(), true);

		statusFontSizeGroup = new ChoiceGroup("Status text size: ", Choice.POPUP, FONT_SIZE_NAMES, null);
		final int statusFontSizeIndex = getSelectedFontIndex(pref.getStatusFontSize());
		statusFontSizeGroup.setSelectedIndex(statusFontSizeIndex, true);

		detailsFontSizeGroup = new ChoiceGroup("Details text size: ", Choice.POPUP, FONT_SIZE_NAMES, null);
		final int detailsFontSizeIndex = getSelectedFontIndex(pref.getDetailsFontSize());
		detailsFontSizeGroup.setSelectedIndex(detailsFontSizeIndex, true);

		startTypeGroup = new ChoiceGroup("Startup action: ", Choice.POPUP, Preferences.START_ACTIONS, null);
		int startAction = pref.getStartAction();
		if (startAction == Preferences.START_ACTION_SHOW_OPTIONS) {
			startAction = Preferences.DEFAULT_START_ACTION;
		}
		startTypeGroup.setSelectedIndex(startAction, true);

		// #ifndef AVOID_FILE_API
		trackDirectoryField = new TextField("Track directory: ", pref.getTrackDirectory(), 100, TextField.URL);
		browseTrackCommand = new Command("Browse", Command.ITEM, 1);
		trackDirectoryField.setDefaultCommand(browseTrackCommand);
		trackDirectoryField.setItemCommandListener(this);

		exportDirectoryField = new TextField("Export directory (defaults to track directory): ", pref
				.getExportDirectory(), 100, TextField.URL);
		browseExportCommand = new Command("Browse", Command.ITEM, 1);
		exportDirectoryField.setDefaultCommand(browseExportCommand);
		exportDirectoryField.setItemCommandListener(this);

		exportFormatGroup = new ChoiceGroup("Export to: ", Choice.MULTIPLE, Preferences.EXPORT_FORMATS, null);
		for (int i = 0; i < Preferences.EXPORT_FORMATS.length; i++) {
			exportFormatGroup.setSelectedIndex(i, pref.getExportFormat(i));
		}
		// #endif

		append(locationProviderGroup);
		if (BBTracker.isBluetoothAvailable()) {
			append(bluetoothNameField);
		}
		append(sampleField);
		append(unitsGroup);
		append(statusFontSizeGroup);
		append(detailsFontSizeGroup);
		append(startTypeGroup);
		// #ifndef AVOID_FILE_API
		append(trackDirectoryField);
		append(exportDirectoryField);
		append(exportFormatGroup);
		// #endif

		addCommand(GuiUtils.OK_COMMAND);
		addCommand(GuiUtils.CANCEL_COMMAND);
		setCommandListener(this);
	}

	private int getSelectedFontIndex(final int fontSize) {
		int selectedIndex;
		switch (fontSize) {
		case Font.SIZE_SMALL:
			selectedIndex = 0;
			break;
		case Font.SIZE_MEDIUM:
			selectedIndex = 1;
			break;
		case Font.SIZE_LARGE:
			selectedIndex = 2;
			break;
		default:
			selectedIndex = 1;
		}
		return selectedIndex;
	}

	private int getFontSize(final int selectedIndex) {
		int fontSize;
		switch (selectedIndex) {
		case 0:
			fontSize = Font.SIZE_SMALL;
			break;
		case 1:
			fontSize = Font.SIZE_MEDIUM;
			break;
		case 2:
			fontSize = Font.SIZE_LARGE;
			break;
		default:
			throw new IllegalStateException();
		}
		return fontSize;
	}

	public void commandAction(final Command command, final Displayable source) {
		if (command == GuiUtils.OK_COMMAND) {
			final String message = validatePreferences();
			if (message == null) {
				savePreferences();
				BBTracker.getInstance().showMainCanvas();
			} else {
				final Alert alert = new Alert("Validate Preferences!", message, null, AlertType.CONFIRMATION);
				final Command continueCommand = new Command("Continue", "Continue and ignore warnings", Command.OK, 1);
				alert.addCommand(GuiUtils.CANCEL_COMMAND);
				alert.addCommand(continueCommand);
				alert.setCommandListener(new CommandListener() {
					public void commandAction(final Command cmd, final Displayable displayable) {
						if (cmd == continueCommand) {
							savePreferences();
							BBTracker.getInstance().showMainCanvas();
						} else {
							BBTracker.getDisplay().setCurrent(OptionsForm.this);
						}
					}
				});
				BBTracker.alert(alert, null);
			}
		} else if (command == GuiUtils.CANCEL_COMMAND) {
			BBTracker.getInstance().showMainCanvas();
		}
	}

	private String validatePreferences() {
		// #ifndef AVOID_FILE_API
		final String trackDir = trackDirectoryField.getString();

		if (trackDir == null || trackDir.length() == 0) {
			return "No track directory has been selected!";
		}

		String dirResult = validateDirectory(trackDir);
		if (dirResult == null) {
			final String exportDir = exportDirectoryField.getString();
			if (exportDir != null && exportDir.length() != 0) {
				dirResult = validateDirectory(exportDir);
			}
		}
		// #endif

		String restartResult = null;
		final Preferences pref = Preferences.getInstance();
		if (locationProviderGroup.getSelectedIndex() != pref.getLocationProvider()) {
			restartResult = BBTracker.getName() +
					" needs to be restarted, for location provider changes to take effect!";
		}

		// #ifndef AVOID_FILE_API
		if (dirResult == null) {
			return restartResult;
		} else if (restartResult == null) {
			return dirResult;
		} else {
			return restartResult + "\n" + dirResult;
		}
		// #else
// @ return restartResult;
		// #endif
	}

	// #ifndef AVOID_FILE_API
	private String validateDirectory(final String dir) {
		javax.microedition.io.file.FileConnection connection = null;
		try {
			connection = (javax.microedition.io.file.FileConnection) Connector.open("file:///" + dir, Connector.READ);
			if (!connection.exists()) {
				return "The directory identified by <" + dir + "> does not exist.";
			} else if (!connection.isDirectory()) {
				return "The file identified by <" + dir + "> is not a directory.";
			} else if (!connection.canWrite()) {
				return "The directory identified by <" + dir + "> is not writeable.";
			}
		} catch (final IOException e) {
			return "Could not verify directory <" + dir + ">: " + e.getMessage();
		} catch (final IllegalArgumentException e) {
			return "Malformed directory <" + dir + ">!";
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (final IOException ignored) {
					// ignore
				}
			}
		}
		return null;
	}

	// #endif

	private void savePreferences() {
		final Preferences pref = Preferences.getInstance();
		try {
			try {
				final int sampleInterval = Integer.parseInt(sampleField.getString());
				pref.setSampleInterval(sampleInterval);
				trackManager.updateSampleInterval();
			} catch (final NumberFormatException e) {
				// should not happen
				Log.log(this, e, "parsing sampleInterval: " + sampleField.getString());
			}
			pref.setStartAction(startTypeGroup.getSelectedIndex());
			// #ifndef AVOID_FILE_API
			pref.setTrackDirectory(trackDirectoryField.getString());
			pref.setExportDirectory(exportDirectoryField.getString());
			Log.initLog();

			for (int i = 0; i < Preferences.EXPORT_FORMATS.length; i++) {
				pref.setExportFormat(i, exportFormatGroup.isSelected(i));
			}
			// #endif

			pref.setUnits(unitsGroup.getSelectedIndex());

			final int statusFontSize = getFontSize(statusFontSizeGroup.getSelectedIndex());
			pref.setStatusFontSize(statusFontSize);

			final int detailsFontSize = getFontSize(detailsFontSizeGroup.getSelectedIndex());
			pref.setDetailsFontSize(detailsFontSize);

			final int locationProvider = locationProviderGroup.getSelectedIndex();
			pref.setLocationProvider(locationProvider);

			pref.setBluetoothUrl(bluetoothUrl);

			final String bluetoothName = bluetoothNameField.getString();
			pref.setBluetoothName(bluetoothName);

			pref.store();
		} catch (final RecordStoreException e) {
			BBTracker.nonFatal(e, "storing preferences", null);
			return;
		}
	}

	public void commandAction(final Command command, final Item item) {
		if (command == selectBluetoothDeviceCommand) {
			if (locationProviderGroup.getSelectedIndex() != Preferences.LOCATION_BLUETOOTH) {
				final Alert alert = new Alert("Not needed!",
						"This is only needed, when Bluetooth is selected as the location provider.", null,
						AlertType.INFO);
				BBTracker.getDisplay().setCurrent(alert, this);
				return;
			}
			showBluetoothBrowser();
// #ifndef AVOID_FILE_API
		} else if (command == browseTrackCommand) {
			showDirectoryBrowser("Track Storage Directory", trackDirectoryField);
		} else if (command == browseExportCommand) {
			showDirectoryBrowser("Track Export Directory", exportDirectoryField);
// #endif
		}
	}

	// #ifndef AVOID_FILE_API
	private void showDirectoryBrowser(final String name, final TextField field) {
		final BrowseForm browser = new BrowseForm(name, field.getString());
		final Display display = BBTracker.getDisplay();
		browser.setCallback(new Runnable() {

			public void run() {
				final String selectedPath = browser.getPath();
				if (selectedPath != null) {
					field.setString(selectedPath);
				}
				display.setCurrent(OptionsForm.this);
			}

		});
		display.setCurrent(browser);
	}

	// #endif

	private void showBluetoothBrowser() {
		final CommandListener commandListener = new CommandListener() {

			public void commandAction(final Command command, final Displayable displayable) {
				final BLUElet bluelet = BLUElet.instance;
				if (command == BLUElet.SELECTED) {
					Log.log(this, "BLUElet 'selected' event");
					final RemoteDevice device = bluelet.getSelectedDevice();
					if (device != null) {
						Alert alert = new Alert("Device selected", "Looking for service", null, AlertType.INFO);
						alert.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
						alert.setTimeout(Alert.FOREVER);
						BBTracker.getDisplay().setCurrent(alert);
					}
				} else if (command == BLUElet.COMPLETED) {
					Log.log(this, "BLUElet 'completed' event");
					final RemoteDevice device = bluelet.getSelectedDevice();
					String deviceName;
					try {
						deviceName = device.getFriendlyName(false);
					} catch (IOException e) {
						deviceName = device.getBluetoothAddress();
					}
					final ServiceRecord serviceRecord = bluelet.getFirstDiscoveredService();
					Log.log(this, "Selected Bluetooth Device: " + deviceName);
					if (serviceRecord != null) {
						String url = serviceRecord.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
						Log.log(this, "ServiceRecord with URL " + url);
						bluetoothUrl = url;
						bluetoothNameField.setString(deviceName);
					} else {
						Alert alert = new Alert(
								"No matching service found",
								"No matching service found was found for this Bluetooth device. Please make sure that you selected a GPS device",
								null, AlertType.INFO);
						BBTracker.getDisplay().setCurrent(alert, OptionsForm.this);
						BLUElet.instance.destroyApp(false);
						return;
					}
					BBTracker.getDisplay().setCurrent(OptionsForm.this);
				} else if (command == BLUElet.BACK) {
					Log.log(this, "BLUElet 'back' event");
					BBTracker.getDisplay().setCurrent(OptionsForm.this);
					BLUElet.instance.destroyApp(false);
				}
			}

		};
		final BLUElet mBluelet;

		if (BLUElet.instance == null) {
			mBluelet = new BLUElet(BBTracker.getInstance(), commandListener);
			mBluelet.startApp();
		} else {
			mBluelet = BLUElet.instance;
			BLUElet.callback = commandListener;
		}
		mBluelet.startInquiry(DiscoveryAgent.GIAC, new UUID[] { new UUID(0x1101) });
		BBTracker.getDisplay().setCurrent(mBluelet.getUI());
	}
}
