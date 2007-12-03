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

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

import org.bbtracker.Track;
import org.bbtracker.Utils;
import org.bbtracker.mobile.BBTracker;
import org.bbtracker.mobile.IconManager;
import org.bbtracker.mobile.Log;
import org.bbtracker.mobile.Preferences;
import org.bbtracker.mobile.TrackManager;
import org.bbtracker.mobile.TrackStore.TrackStoreEntry;
import org.bbtracker.mobile.TrackStore.TrackStoreException;

public class TracksForm extends List implements CommandListener {
	private final TrackManager trackManager;

	private final Command selectCommand;

	private final Command exportCommand;

	private final Command deleteCommand;

	private final Command cancelCommand;

	private TrackStoreEntry[] entries;

	public TracksForm(final TrackManager trackManager) throws TrackStoreException {
		super("Tracks", Choice.IMPLICIT);

		this.trackManager = trackManager;

		selectCommand = new Command("Display", Command.ITEM, 0);
		exportCommand = new Command("Export", Command.ITEM, 1);
		deleteCommand = new Command("Delete", Command.ITEM, 2);
		cancelCommand = new Command("Cancel", Command.CANCEL, 3);

		addCommand(selectCommand);
		addCommand(exportCommand);
		addCommand(deleteCommand);
		addCommand(cancelCommand);

		setSelectCommand(selectCommand);

		setCommandListener(this);

		loadEntries();
	}

	private void loadEntries() throws TrackStoreException {
		deleteAll();
		entries = trackManager.getEntries();
		final Image icon = IconManager.getInstance().getListImage("track");
		for (int i = 0; i < entries.length; i++) {
			final String description = entries[i].getName() + " (" + Utils.dateToString(entries[i].getDate()) + ")";
			append(description, icon);
		}
	}

	public void commandAction(final Command command, final Displayable displayable) {
		if (command == cancelCommand) {
			BBTracker.getInstance().showMainCanvas();
			return;
		}
		final int index = getSelectedIndex();
		if (index == -1) {
			BBTracker.alert(new Alert("No Track selected", "A track has to be selected for this action", null,
					AlertType.INFO), this);
			return;
		}

		final TrackStoreEntry tse = entries[index];
		if (command == deleteCommand) {
			try {
				tse.deleteTrack();
				loadEntries();
			} catch (final TrackStoreException e) {
				Log.log(this, e, "deleting track and listing tracks");
				final Alert alert = new Alert("Couldn't delete Track.", "The track " + tse.getName() +
						" couldn't be deleted: " + e.getMessage(), null, AlertType.INFO);
				BBTracker.alert(alert, this);
			}
		} else {
			if (command == selectCommand) {
				try {
					trackManager.maybeSaveTrack();
				} catch (final TrackStoreException e) {
					TrackManager.showSaveFailedAlert(e, this);
					return;
				}
			}
			final Track track;
			try {
				track = tse.loadTrack();
			} catch (final TrackStoreException e) {
				Log.log(this, e, "loading track");
				final Alert alert = new Alert("Couldn't load Track.", "The track " + tse.getName() +
						" couldn't be loaded: " + e.getMessage(), null, AlertType.INFO);
				BBTracker.alert(alert, this);
				return;
			}
			if (command == selectCommand) {
				trackManager.setTrack(track);
				BBTracker.getInstance().showMainCanvas();
				// #ifndef AVOID_FILE_API
			} else if (command == exportCommand) {
				exportTrack(track, this);
				// #endif
			}
		}
	}

	// #ifndef AVOID_FILE_API
	public static void exportTrack(final Track track, final Displayable next) {
		final Preferences preferences = Preferences.getInstance();
		final String dir = preferences.getEffectiveExportDirectory();
		if (dir == null) {
			final Alert alert = new Alert("No export directory defined!",
					"Please define either an export directory or a track directory in the options screen.", null,
					AlertType.WARNING);
			BBTracker.alert(alert, next);
			return;
		}
		int count;
		try {
			count = TrackManager.exportTrack(track);
		} catch (final IOException e) {
			BBTracker.nonFatal(e, "exporting track", next);
			return;
		}

		final Alert alert = new Alert("Finished exporting", "The track " + track.getName() +
				" has been exported successfully to " + count + " formats!", null, AlertType.INFO);
		BBTracker.alert(alert, next);
	}
	// #endif
}