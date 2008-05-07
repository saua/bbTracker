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

import java.util.Calendar;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import org.bbtracker.Track;
import org.bbtracker.Utils;
import org.bbtracker.mobile.BBTracker;
import org.bbtracker.mobile.Preferences;
import org.bbtracker.mobile.TrackManager;
import org.bbtracker.mobile.gps.LocationException;

public class TrackNameForm extends Form implements CommandListener {

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

		// get the next track number, although we don't use it
		// maybe we can use it for some statistic sometime
		Preferences.getInstance().getNextTrackNumber();
		final String initialName = Utils.calendarToCompactString(Calendar.getInstance());
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

		addCommand(GuiUtils.OK_COMMAND);
		addCommand(GuiUtils.CANCEL_COMMAND);

		setCommandListener(this);
	}

	public void commandAction(final Command command, final Displayable displayable) {
		if (command == GuiUtils.OK_COMMAND) {
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
		} else if (command == GuiUtils.CANCEL_COMMAND) {
			BBTracker.getInstance().showMainCanvas();
		}
	}
}
