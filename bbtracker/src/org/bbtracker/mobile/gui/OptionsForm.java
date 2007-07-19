package org.bbtracker.mobile.gui;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import org.bbtracker.mobile.BBTracker;
import org.bbtracker.mobile.Preferences;
import org.bbtracker.mobile.TrackManager;

public class OptionsForm extends Form implements CommandListener {
	private final TrackManager trackManager;

	private final Command okCommand = new Command("OK", Command.OK, 0);

	private final Command cancelCommand = new Command("Cancel", Command.CANCEL, 1);

	private final TextField sampleField;

	private final ChoiceGroup startTypeGroup;

	public OptionsForm(final TrackManager trackManager) {
		super("Options");

		this.trackManager = trackManager;

		final Preferences pref = Preferences.getInstance();

		sampleField = new TextField("Sample Interval in seconds: ", String.valueOf(pref.getSampleInterval()), 5,
				TextField.NUMERIC);
		startTypeGroup = new ChoiceGroup("Startup action: ", Choice.EXCLUSIVE, Preferences.START_ACTIONS, null);
		startTypeGroup.setSelectedIndex(pref.getStartAction(), true);

		append(sampleField);
		append(startTypeGroup);

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
				pref.setStartAction(startTypeGroup.getSelectedIndex());

				pref.store();
			} catch (final NumberFormatException e) {
				// should not happen
			}

			trackManager.updateSampleInterval();
		}
		BBTracker.getInstance().showMainCanvas();
	}
}
