package org.bbtracker.mobile.gui;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import org.bbtracker.Track;
import org.bbtracker.mobile.BBTracker;
import org.bbtracker.mobile.TrackManager;
import org.bbtracker.mobile.TrackStore;

public class TracksForm extends List implements CommandListener {
	private final TrackManager trackManager;

	private final Command selectCommand;

	private final Command deleteCommand;

	private final Command cancelCommand;

	public TracksForm(final TrackManager trackManager) {
		super("Tracks", Choice.IMPLICIT);

		this.trackManager = trackManager;

		selectCommand = new Command("Select", Command.OK, 0);
		addCommand(selectCommand);

		deleteCommand = new Command("Delete", Command.ITEM, 3);
		addCommand(deleteCommand);

		cancelCommand = new Command("Cancel", Command.CANCEL, 1);
		addCommand(cancelCommand);

		setSelectCommand(selectCommand);

		setCommandListener(this);

		trackManager.maybeSafeTrack();

		loadNames();
	}

	private void loadNames() {
		deleteAll();
		final TrackStore store = TrackStore.getInstance();
		final String[] trackNames = store.getTrackNames();
		for (int i = 0; i < trackNames.length; i++) {
			append(trackNames[i], null);
		}
	}

	public void commandAction(final Command command, final Displayable displayable) {
		if (command == selectCommand) {
			final Track track = TrackStore.getInstance().getTrack(getSelectedIndex());
			trackManager.setTrack(track);
			BBTracker.getInstance().showMainCanvas();
		} else if (command == deleteCommand) {
			TrackStore.getInstance().deleteTrack(getSelectedIndex());
			loadNames();
		} else if (command == cancelCommand) {
			BBTracker.getInstance().showMainCanvas();
		}
	}
}
