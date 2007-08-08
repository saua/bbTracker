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
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.rms.RecordStoreException;

import org.bbtracker.Track;
import org.bbtracker.mobile.BBTracker;
import org.bbtracker.mobile.IconManager;
import org.bbtracker.mobile.Preferences;
import org.bbtracker.mobile.TrackManager;
import org.bbtracker.mobile.TrackStore;
import org.bbtracker.mobile.exporter.GpxTrackExporter;
import org.bbtracker.mobile.exporter.KmlTrackExporter;
import org.bbtracker.mobile.exporter.TrackExporter;

public class TracksForm extends List implements CommandListener {
	private final TrackManager trackManager;

	private final Command selectCommand;

	private final Command exportCommand;

	private final Command deleteCommand;

	private final Command cancelCommand;

	public TracksForm(final TrackManager trackManager) throws RecordStoreException {
		super("Tracks", Choice.IMPLICIT);

		this.trackManager = trackManager;

		selectCommand = new Command("Display", "Display selected Track", Command.ITEM, 0);
		exportCommand = new Command("Export", "Export selected Track", Command.ITEM, 2);
		deleteCommand = new Command("Delete", "Delete selected Track", Command.ITEM, 3);
		cancelCommand = new Command("Cancel", "Return to previous screen", Command.CANCEL, 1);

		addCommand(selectCommand);
		addCommand(exportCommand);
		addCommand(deleteCommand);
		addCommand(cancelCommand);

		setSelectCommand(selectCommand);

		setCommandListener(this);

		trackManager.maybeSafeTrack();

		loadNames();
	}

	private void loadNames() throws RecordStoreException {
		deleteAll();
		final Image icon = IconManager.getInstance().getListImage("track");
		final TrackStore store = TrackStore.getInstance();
		final String[] trackNames = store.getTrackNames();
		for (int i = 0; i < trackNames.length; i++) {
			append(trackNames[i], icon);
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
		} else if (command == deleteCommand) {
			try {
				TrackStore.getInstance().deleteTrack(index);
				loadNames();
			} catch (final RecordStoreException e) {
				BBTracker.nonFatal(e, "deleting track", null);
				return;
			}
			return;
		} else {
			final Track track;
			try {
				track = TrackStore.getInstance().getTrack(index);
			} catch (final RecordStoreException e) {
				BBTracker.nonFatal(e, "loading track", this);
				return;
			}
			if (command == selectCommand) {
				trackManager.setTrack(track);
				BBTracker.getInstance().showMainCanvas();
			} else if (command == exportCommand) {
				final Preferences preferences = Preferences.getInstance();
				final String dir = preferences.getExportDirectory();
				if (dir == null) {
					final Alert alert = new Alert("No export directory defined!",
							"Please define an export directory in the options screen.", null, AlertType.WARNING);
					BBTracker.alert(alert, this);
					return;
				}
				int count;
				try {
					count = exportTrack(dir, track);
				} catch (final IOException e) {
					BBTracker.nonFatal(e, "exporting track", this);
					return;
				}

				final Alert alert = new Alert("Finished exporting", "The track " + track.getName() +
						" has been exported successfully to " + count + " formats!", null, AlertType.INFO);
				BBTracker.alert(alert, this);
			}
		}
	}

	private int exportTrack(final String dir, final Track track) throws IOException {
		final Preferences pref = Preferences.getInstance();
		int exportCount = 0;
		if (pref.getExportFormat(0)) {
			export(dir, track, new KmlTrackExporter());
			exportCount++;
		}
		if (pref.getExportFormat(1)) {
			export(dir, track, new GpxTrackExporter());
			exportCount++;
		}
		return exportCount;
	}

	private void export(final String dir, final Track track, final TrackExporter exporter) throws IOException {
		final String fileName = exporter.getFileName(track);
		final String fullName = dir.endsWith("/") ? dir + fileName : dir + "/" + fileName;
		FileConnection connection = null;
		OutputStream out = null;
		try {
			connection = (FileConnection) Connector.open("file:///" + fullName, Connector.READ_WRITE);
			connection.create();
			out = connection.openOutputStream();
			exporter.export(out, track);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (final IOException ignored) {
					// ignore
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (final IOException ignored) {
					// ignore
				}
			}
		}
	}
}
