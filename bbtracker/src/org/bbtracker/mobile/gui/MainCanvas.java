package org.bbtracker.mobile.gui;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.rms.RecordStoreException;

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;
import org.bbtracker.mobile.BBTracker;
import org.bbtracker.mobile.TrackListener;
import org.bbtracker.mobile.TrackManager;

public class MainCanvas extends Canvas implements TrackListener, CommandListener {
	private final TrackManager manager;

	private Track track;

	private TrackTile trackTile;

	private final StatusTile statusTile;

	private final Command waypointCommand = new Command("Add Waypoint", Command.ITEM, 0);

	private final Command newTrackCommand = new Command("New Track", Command.ITEM, 2);

	private final Command tracksCommand = new Command("Tracks", Command.ITEM, 2);

	private final Command optionsCommand = new Command("Options", Command.ITEM, 2);

	private final Command aboutCommand = new Command("About", Command.ITEM, 4);

	private final Command exitCommand = new Command("Exit", Command.CANCEL, 3);

	public MainCanvas(final TrackManager manager) {
		this.manager = manager;
		track = manager.getTrack();

		trackTile = new TrackTile(track);
		statusTile = new StatusTile(manager);

		addCommand(waypointCommand);
		addCommand(newTrackCommand);
		addCommand(tracksCommand);
		addCommand(optionsCommand);
		addCommand(aboutCommand);
		addCommand(exitCommand);
		setCommandListener(this);
	}

	protected void sizeChanged(final int w, final int h) {
		super.sizeChanged(w, h);

		final int statusHeight = statusTile.getPreferredHeight();
		trackTile.resize(0, 0, w, h - (statusHeight + 1));
		statusTile.resize(0, h - statusHeight, w, statusHeight);
	}

	protected void paint(final Graphics g) {
		if (trackTile.width == 0) {
			// BlackBerry (at least 8800) seems not to call sizeChanged before initial paint()
			sizeChanged(getWidth(), getHeight());
		}
		trackTile.paint(g);
		statusTile.paint(g);

		switch (manager.getState()) {
		case TrackManager.STATE_TRACKING:
			return;
		default:
			final String state = manager.getStateString();
			final Font font = Font.getDefaultFont();
			final int stringWidth = font.stringWidth(state);
			final int stringHeight = g.getFont().getHeight();

			g.setFont(font);
			g.setColor(0x00ffffff);
			g.fillRect(2, 2, stringWidth + 4, stringHeight + 4);
			g.setColor(0x00000000);
			g.drawRect(2, 2, stringWidth + 4, stringHeight + 4);
			g.drawString(state, 4, 4, Graphics.TOP | Graphics.LEFT);
		}
	}

	public void newPoint(final TrackPoint newPoint, final boolean boundsChanged, final boolean newSegment) {
		trackTile.setCurrentPoint(newPoint);
		if (boundsChanged) {
			trackTile.onResize(); // XXX Make that nicer
		}
		if (isShown()) {
			repaint();
		}
	}

	public void stateChanged(final int newState) {
		if (isShown()) {
			repaint();
		}
	}

	public void setTrack(final Track track) {
		this.track = track;
		trackTile = new TrackTile(track);
		repaint();
	}

	protected void hideNotify() {
		super.hideNotify();
		manager.removePointListener(this);
	}

	protected void showNotify() {
		super.showNotify();
		final Track newTrack = manager.getTrack();
		if (newTrack != track) {
			setTrack(newTrack);
		}
		manager.addPointListener(this);
	}

	public void commandAction(final Command command, final Displayable displayable) {
		Displayable nextDisplayable = null;
		if (command == aboutCommand) {
			nextDisplayable = new AboutForm();
		} else if (command == optionsCommand) {
			nextDisplayable = new OptionsForm(manager);
		} else if (command == newTrackCommand) {
			nextDisplayable = new NewTrackForm(manager);
		} else if (command == tracksCommand) {
			try {
				nextDisplayable = new TracksForm(manager);
			} catch (final RecordStoreException e) {
				BBTracker.nonFatal(e, "getting list of stored tracks", this);
			}
		} else if (command == exitCommand) {
			BBTracker.getInstance().shutdown(true);
			return;
		}
		BBTracker.getDisplay().setCurrent(nextDisplayable);
	}
}