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
	private final TrackManager trackManager;

	private final Command okCommand;

	private final Command cancelCommand;

	private final Command browseCommand;

	private final TextField sampleField;

	private final ChoiceGroup startTypeGroup;

	private final TextField directoryField;

	private final ChoiceGroup exportFormatGroup;

	private final ChoiceGroup unitsGroup;

	private final ChoiceGroup statusFontSizeGroup;

	public OptionsForm(final TrackManager trackManager) {
		super("Options");

		this.trackManager = trackManager;

		final Preferences pref = Preferences.getInstance();

		sampleField = new TextField("Sample Interval in seconds: ", String.valueOf(pref.getSampleInterval()), 5,
				TextField.NUMERIC);

		unitsGroup = new ChoiceGroup("Units: ", Choice.POPUP, Preferences.UNITS, null);
		unitsGroup.setSelectedIndex(pref.getUnits(), true);

		statusFontSizeGroup = new ChoiceGroup("Status text size: ", Choice.POPUP, new String[] { "Small", "Medium",
				"Large" }, null);
		int selectedFontSizeItem;
		switch (pref.getStatusFontSize()) {
		case Font.SIZE_SMALL:
			selectedFontSizeItem = 0;
			break;
		case Font.SIZE_MEDIUM:
			selectedFontSizeItem = 1;
			break;
		case Font.SIZE_LARGE:
			selectedFontSizeItem = 2;
			break;
		default:
			selectedFontSizeItem = 1;
		}
		statusFontSizeGroup.setSelectedIndex(selectedFontSizeItem, true);

		startTypeGroup = new ChoiceGroup("Startup action: ", Choice.POPUP, Preferences.START_ACTIONS, null);
		int startAction = pref.getStartAction();
		if (startAction == Preferences.START_ACTION_SHOW_OPTIONS) {
			startAction = Preferences.DEFAULT_START_ACTION;
		}
		startTypeGroup.setSelectedIndex(startAction, true);

		directoryField = new TextField("Export directory: ", pref.getExportDirectory(), 100, TextField.URL);
		browseCommand = new Command("Browse", Command.ITEM, 1);
		directoryField.setDefaultCommand(browseCommand);
		directoryField.setItemCommandListener(this);

		exportFormatGroup = new ChoiceGroup("Export to: ", Choice.MULTIPLE, Preferences.EXPORT_FORMATS, null);
		for (int i = 0; i < Preferences.EXPORT_FORMATS.length; i++) {
			exportFormatGroup.setSelectedIndex(i, pref.getExportFormat(i));
		}

		append(sampleField);
		append(unitsGroup);
		append(statusFontSizeGroup);
		append(startTypeGroup);
		append(directoryField);
		append(exportFormatGroup);

		okCommand = new Command("OK", Command.OK, 0);
		cancelCommand = new Command("Cancel", Command.CANCEL, 1);

		addCommand(okCommand);
		addCommand(cancelCommand);
		setCommandListener(this);
	}

	public void commandAction(final Command command, final Displayable source) {
		if (command == okCommand) {
			final Preferences pref = Preferences.getInstance();
			try {
				final int sampleInterval = Integer.parseInt(sampleField.getString());
				pref.setSampleInterval(sampleInterval);
				trackManager.updateSampleInterval();
				pref.setStartAction(startTypeGroup.getSelectedIndex());
				pref.setExportDirectory(directoryField.getString());

				for (int i = 0; i < Preferences.EXPORT_FORMATS.length; i++) {
					pref.setExportFormat(i, exportFormatGroup.isSelected(i));
				}

				pref.setUnits(unitsGroup.getSelectedIndex());

				final int newFontSize;
				switch (statusFontSizeGroup.getSelectedIndex()) {
				case 0:
					newFontSize = Font.SIZE_SMALL;
					break;
				case 1:
					newFontSize = Font.SIZE_MEDIUM;
					break;
				case 2:
					newFontSize = Font.SIZE_LARGE;
					break;
				default:
					throw new IllegalStateException();
				}
				pref.setStatusFontSize(newFontSize);

				pref.store();
			} catch (final NumberFormatException e) {
				// should not happen
				BBTracker.log(e);
			} catch (final RecordStoreException e) {
				BBTracker.nonFatal(e, "storing preferences", null);
				return;
			}
		}
		BBTracker.getInstance().showMainCanvas();
	}

	public void commandAction(final Command command, final Item item) {
		if (command == browseCommand) {
			final BrowseForm browser = new BrowseForm("Save Directory", directoryField.getString());
			final Display display = BBTracker.getDisplay();
			browser.setCallback(new Runnable() {

				public void run() {
					final String selectedPath = browser.getPath();
					if (selectedPath != null) {
						directoryField.setString(selectedPath);
					}
					display.setCurrent(OptionsForm.this);
				}

			});
			display.setCurrent(browser);
		}
	}
}
