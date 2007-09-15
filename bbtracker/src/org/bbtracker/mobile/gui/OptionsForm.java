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

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
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
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.TextField;
import javax.microedition.rms.RecordStoreException;

import org.bbtracker.mobile.BBTracker;
import org.bbtracker.mobile.Preferences;
import org.bbtracker.mobile.TrackManager;

public class OptionsForm extends Form implements CommandListener, ItemCommandListener {
	private static final String[] FONT_SIZE_NAMES = new String[] { "Small", "Medium", "Large" };

	private final TrackManager trackManager;

	private final Command okCommand;

	private final Command cancelCommand;

	private final Command browseTrackCommand;

	private final Command browseExportCommand;

	private final TextField sampleField;

	private final ChoiceGroup startTypeGroup;

	private final TextField trackDirectoryField;

	private final TextField exportDirectoryField;

	private final ChoiceGroup exportFormatGroup;

	private final ChoiceGroup unitsGroup;

	private final ChoiceGroup statusFontSizeGroup;

	private final ChoiceGroup detailsFontSizeGroup;

	public OptionsForm(final TrackManager trackManager) {
		super("Options");

		this.trackManager = trackManager;

		final Preferences pref = Preferences.getInstance();

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

		append(sampleField);
		append(unitsGroup);
		append(statusFontSizeGroup);
		append(detailsFontSizeGroup);
		append(startTypeGroup);
		append(trackDirectoryField);
		append(exportDirectoryField);
		append(exportFormatGroup);

		okCommand = new Command("OK", Command.OK, 0);
		cancelCommand = new Command("Cancel", Command.CANCEL, 1);

		addCommand(okCommand);
		addCommand(cancelCommand);
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
		if (command == okCommand) {
			final String message = validatePreferences();
			if (message == null) {
				savePreferences();
				BBTracker.getInstance().showMainCanvas();
			} else {
				final Alert alert = new Alert("Validate Preferences!", message, null, AlertType.CONFIRMATION);
				final Command continueCommand = new Command("Continue", "Continue and ignore warnings", Command.OK, 1);
				alert.addCommand(new Command("Cancel", "Return to Options Screen", Command.CANCEL, 0));
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
		} else if (command == cancelCommand) {
			BBTracker.getInstance().showMainCanvas();
		}
	}

	private String validatePreferences() {
		final String trackDir = trackDirectoryField.getString();

		if (trackDir == null || trackDir.length() == 0) {
			return "No track directory has been selected!";
		}

		String dirResult;

		dirResult = validateDirectory(trackDir);
		if (dirResult != null) {
			return dirResult;
		}

		final String exportDir = exportDirectoryField.getString();
		if (exportDir != null && exportDir.length() != 0) {
			dirResult = validateDirectory(exportDir);
			return dirResult;
		} else {
			return null;
		}
	}

	private String validateDirectory(final String dir) {
		FileConnection connection = null;
		try {
			connection = (FileConnection) Connector.open("file:///" + dir, Connector.READ);
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

	private void savePreferences() {
		final Preferences pref = Preferences.getInstance();
		try {
			try {
				final int sampleInterval = Integer.parseInt(sampleField.getString());
				pref.setSampleInterval(sampleInterval);
				trackManager.updateSampleInterval();
			} catch (final NumberFormatException e) {
				// should not happen
				BBTracker.log(this, e, "parsing sampleInterval: " + sampleField.getString());
			}
			pref.setStartAction(startTypeGroup.getSelectedIndex());
			pref.setTrackDirectory(trackDirectoryField.getString());
			pref.setExportDirectory(exportDirectoryField.getString());
			BBTracker.initLog();

			for (int i = 0; i < Preferences.EXPORT_FORMATS.length; i++) {
				pref.setExportFormat(i, exportFormatGroup.isSelected(i));
			}

			pref.setUnits(unitsGroup.getSelectedIndex());

			final int statusFontSize = getFontSize(statusFontSizeGroup.getSelectedIndex());
			pref.setStatusFontSize(statusFontSize);

			final int detailsFontSize = getFontSize(detailsFontSizeGroup.getSelectedIndex());
			pref.setDetailsFontSize(detailsFontSize);

			pref.store();
		} catch (final RecordStoreException e) {
			BBTracker.nonFatal(e, "storing preferences", null);
			return;
		}
	}

	public void commandAction(final Command command, final Item item) {
		if (command == browseTrackCommand) {
			showDirectoryBrowser("Track Storage Directory", trackDirectoryField);
		} else if (command == browseExportCommand) {
			showDirectoryBrowser("Track Export Directory", exportDirectoryField);
		}
	}

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
}
