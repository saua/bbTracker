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

import org.bbtracker.Track;
import org.bbtracker.mobile.BBTracker;
import org.bbtracker.mobile.Preferences;
import org.bbtracker.mobile.TrackManager;

public class TrackNameForm extends Form implements CommandListener {

	private final Command okCommand = new Command("Ok", Command.OK, 0);

	private final Command cancelCommand = new Command("Cancel", Command.CANCEL, 1);

	private TextField nameField;

	private final TrackManager trackManager;

	private final Track track;

	/**
	 * Creates a TrackNameForm used to create new tracks.
	 */
	public TrackNameForm(final TrackManager trackManager) {
		super("New Track");

		this.trackManager = trackManager;
		track = null;

		final String initialName = "Track " + Preferences.getInstance().getNextTrackNumber();
		initGui(initialName);
	}

	/**
	 * Creates a TrackNameForm used to rename existing tracks.
	 * 
	 * @param track
	 *            the track to rename
	 */
	public TrackNameForm(final Track track) {
		super("Rename Track");

		this.track = track;
		trackManager = null;

		initGui(track.getName());
	}

	private void initGui(final String trackName) {
		nameField = new TextField("Name: ", trackName, 32, TextField.ANY);

		append(nameField);

		addCommand(okCommand);
		addCommand(cancelCommand);

		setCommandListener(this);
	}

	public void commandAction(final Command command, final Displayable displayable) {
		if (command == okCommand) {
			if (trackManager != null) {
				// new track
				try {
					trackManager.newTrack(nameField.getString());
					BBTracker.getInstance().showMainCanvas();
				} catch (final LocationException e) {
					BBTracker.nonFatal(e, "Starting new track", null);
				}
			} else {
				track.setName(nameField.getString());
			}
		} else if (command == cancelCommand) {
			BBTracker.getInstance().showMainCanvas();
		}
	}
}
