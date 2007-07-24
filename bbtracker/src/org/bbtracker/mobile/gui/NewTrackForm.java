package org.bbtracker.mobile.gui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import javax.microedition.location.LocationException;

import org.bbtracker.mobile.BBTracker;
import org.bbtracker.mobile.Preferences;
import org.bbtracker.mobile.TrackManager;

public class NewTrackForm extends Form implements CommandListener {
	private final TrackManager trackManager;

	private final Command okCommand = new Command("Ok", Command.OK, 0);

	private final Command cancelCommand = new Command("Cancel", Command.CANCEL, 1);

	private final TextField nameField;

	public NewTrackForm(final TrackManager trackManager) {
		super("New Track");

		this.trackManager = trackManager;

		final String initialName = "Track " + Preferences.getInstance().getNextTrackNumber();
		nameField = new TextField("Name: ", initialName, 32, TextField.ANY);

		append(nameField);

		addCommand(okCommand);
		addCommand(cancelCommand);

		setCommandListener(this);
	}

	public void commandAction(final Command command, final Displayable displayable) {
		if (command == okCommand) {
			try {
				trackManager.newTrack(nameField.getString());
				BBTracker.getInstance().showMainCanvas();
			} catch (final LocationException e) {
				BBTracker.nonFatal(e, "Starting new track", null);
			}
		} else if (command == cancelCommand) {
			BBTracker.getInstance().showMainCanvas();
		}
	}
}
