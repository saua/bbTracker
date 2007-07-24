package org.bbtracker.mobile.gui;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
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

	public OptionsForm(final TrackManager trackManager) {
		super("Options");

		this.trackManager = trackManager;

		final Preferences pref = Preferences.getInstance();

		sampleField = new TextField("Sample Interval in seconds: ", String.valueOf(pref.getSampleInterval()), 5,
				TextField.NUMERIC);
		startTypeGroup = new ChoiceGroup("Startup action: ", Choice.EXCLUSIVE, Preferences.START_ACTIONS, null);
		startTypeGroup.setSelectedIndex(pref.getStartAction(), true);
		directoryField = new TextField("Export directory: ", pref.getExportDirectory(), 100, TextField.URL);
		browseCommand = new Command("Browse", Command.ITEM, 1);
		directoryField.addCommand(browseCommand);
		directoryField.setItemCommandListener(this);

		append(sampleField);
		append(startTypeGroup);
		append(directoryField);

		okCommand = new Command("OK", Command.OK, 0);
		addCommand(okCommand);
		cancelCommand = new Command("Cancel", Command.CANCEL, 1);
		addCommand(cancelCommand);
		setCommandListener(this);
	}

	public void commandAction(final Command command, final Displayable source) {
		if (command == okCommand) {
			final Preferences pref = Preferences.getInstance();
			try {
				final int sampleInterval = Integer.parseInt(sampleField.getString());
				pref.setSampleInterval(sampleInterval);
				pref.setStartAction(startTypeGroup.getSelectedIndex());
				pref.setExportDirectory(directoryField.getString());

				pref.store();
			} catch (final NumberFormatException e) {
				// should not happen
			} catch (final RecordStoreException e) {
				BBTracker.nonFatal(e, "storing preferences", null);
			}

			trackManager.updateSampleInterval();
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
			System.out.println(browser.getPath());
		}
	}
}
