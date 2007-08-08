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
