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

import java.util.TimerTask;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.rms.RecordStoreException;

import org.bbtracker.TrackPoint;
import org.bbtracker.mobile.BBTracker;
import org.bbtracker.mobile.TrackListener;
import org.bbtracker.mobile.TrackManager;

public class MainCanvas extends Canvas implements TrackListener, CommandListener {
	private static final int DEFAULT_STATUS_TIMEOUT = 5 * 1000;

	private static final int MAX_TILES = 2;

	private final Tile[] visibleTiles = new Tile[MAX_TILES];

	private final TrackManager manager;

	private final Tile trackTile;

	private final Tile elevationProfileTile;

	private final Tile speedProfileTile;

	private final StatusTile statusTile;

	private final Command newTrackCommand;

	private final Command tracksCommand;

	private final Command optionsCommand;

	private final Command switchViewCommand;

	private final Command aboutCommand;

	private final Command exitCommand;

	private String statusMessage = null;

	private long statusMessageEndTime = 0;

	private int tileConfiguration = 0;

	public MainCanvas(final TrackManager manager) {
		this.manager = manager;

		trackTile = new TrackTile(manager);
		elevationProfileTile = new ElevationPlotterTile(manager, DataProvider.TIME);
		speedProfileTile = new SpeedPlotterTile(manager, DataProvider.TIME);
		statusTile = new StatusTile(manager);

		switchViewCommand = new Command("Switch View", Command.SCREEN, 0);
		newTrackCommand = new Command("New Track", "Start a new Track", Command.SCREEN, 1);
		tracksCommand = new Command("Tracks", "Open Track Manager", Command.SCREEN, 2);
		optionsCommand = new Command("Options", Command.SCREEN, 3);
		aboutCommand = new Command("About", Command.SCREEN, 5);
		exitCommand = new Command("Exit", Command.EXIT, 6);

		addCommand(switchViewCommand);
		addCommand(newTrackCommand);
		addCommand(tracksCommand);
		addCommand(optionsCommand);
		addCommand(aboutCommand);
		addCommand(exitCommand);
		setCommandListener(this);

		setMainTile(trackTile, true);
	}

	protected void setMainTile(final Tile mainTile, final boolean withStatus) {
		visibleTiles[0] = mainTile;
		if (withStatus) {
			visibleTiles[1] = statusTile;
		} else {
			visibleTiles[1] = null;
		}
		updateTileSize();
	}

	protected void updateTileSize() {
		final int w = getWidth();
		final int h = getHeight();
		if (visibleTiles[1] == null) {
			visibleTiles[0].resize(0, 0, w, h);
		} else {
			final int statusTileHeight = statusTile.getPreferredHeight(w);
			visibleTiles[0].resize(0, 0, w, h - statusTileHeight);
			visibleTiles[1].resize(0, h - statusTileHeight, w, statusTileHeight);
		}
	}

	protected void setStatusMessage(final String statusMessage) {
		setStatusMessage(statusMessage, DEFAULT_STATUS_TIMEOUT);
	}

	protected void setStatusMessage(final String statusMessage, final int duration) {
		this.statusMessage = statusMessage;
		statusMessageEndTime = System.currentTimeMillis() + duration;
		BBTracker.getTimer().schedule(new RepaintTask(), duration + 10);
		repaint();
	}

	protected void sizeChanged(final int w, final int h) {
		super.sizeChanged(w, h);

		updateTileSize();
	}

	protected void paint(final Graphics g) {
		if (visibleTiles[0].width == 0) {
			// BlackBerry (at least 8800) seems not to call sizeChanged before initial paint()
			sizeChanged(getWidth(), getHeight());
		}

		for (int i = 0; i < visibleTiles.length && visibleTiles[i] != null; i++) {
			visibleTiles[i].paint(g);
		}

		if (statusMessageEndTime > System.currentTimeMillis()) {
			final Font font = Font.getDefaultFont();
			final int stringWidth = font.stringWidth(statusMessage);
			final int stringHeight = g.getFont().getHeight();

			g.setFont(font);
			g.setColor(0x00ffffff);
			g.fillRect(2, 2, stringWidth + 4, stringHeight + 4);
			g.setColor(0x00000000);
			g.drawRect(2, 2, stringWidth + 4, stringHeight + 4);
			g.drawString(statusMessage, 4, 4, Graphics.TOP | Graphics.LEFT);
		}
	}

	public void newPoint(final TrackPoint newPoint, final boolean boundsChanged, final boolean newSegment) {
		for (int i = 0; i < visibleTiles.length && visibleTiles[i] != null; i++) {
			visibleTiles[i].newPoint(newPoint, boundsChanged, newSegment);
		}
	}

	public void currentPointChanged(final TrackPoint newPoint, final int newIndex) {
		for (int i = 0; i < visibleTiles.length && visibleTiles[i] != null; i++) {
			visibleTiles[i].currentPointChanged(newPoint, newIndex);
		}
		repaint();
	}

	public void stateChanged(final int newState) {
		updateStatusText(newState);
		for (int i = 0; i < visibleTiles.length && visibleTiles[i] != null; i++) {
			visibleTiles[i].stateChanged(newState);
		}
		repaint();
	}

	protected void updateStatusText(final int newState) {
		switch (newState) {
		case TrackManager.STATE_STATIC:
			setStatusMessage("Static Track");
			break;
		case TrackManager.STATE_TRACKING:
			setStatusMessage("Tracking");
			break;
		}
	}

	private void nextTileConfiguration() {
		tileConfiguration = (tileConfiguration + 1) % 3;
		switch (tileConfiguration) {
		case 0:
			setMainTile(trackTile, true);
			setStatusMessage("Track view");
			break;
		case 1:
			setMainTile(elevationProfileTile, true);
			setStatusMessage("Elevation over time");
			break;
		case 2:
			setMainTile(speedProfileTile, true);
			setStatusMessage("Speed over time");
			break;
		}
	}

	protected void hideNotify() {
		super.hideNotify();
		manager.removePointListener(this);
	}

	protected void showNotify() {
		super.showNotify();
		manager.addPointListener(this);
		updateTileSize();
		for (int i = 0; i < visibleTiles.length && visibleTiles[i] != null; i++) {
			visibleTiles[i].showNotify();
		}
		updateStatusText(manager.getState());
	}

	public void commandAction(final Command command, final Displayable displayable) {
		Displayable nextDisplayable = null;
		if (command == exitCommand) {
			exitAction();
		} else if (command == switchViewCommand) {
			nextTileConfiguration();
		} else {
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
			}
			BBTracker.getDisplay().setCurrent(nextDisplayable);
		}
	}

	private void exitAction() {
		final boolean isTracking = manager.getState() == TrackManager.STATE_TRACKING;
		String question = "Do you really want to quit?";
		if (isTracking) {
			question += "\nThe current Track will be finished and saved.";
		}
		final Alert alert = new Alert("Really Quit?", question, null, AlertType.CONFIRMATION);
		final Command quitCommand = new Command("Quit", Command.OK, 1);
		alert.addCommand(quitCommand);
		alert.addCommand(new Command("Cancel", Command.CANCEL, 0));
		alert.setCommandListener(new CommandListener() {

			public void commandAction(final Command cmd, final Displayable current) {
				if (cmd == quitCommand) {
					BBTracker.getInstance().shutdown(true);
				} else {
					BBTracker.getDisplay().setCurrent(MainCanvas.this);
				}
			}
		});
		BBTracker.alert(alert, this);
	}

	protected void keyReleased(final int keyCode) {
		final int gameAction = getGameAction(keyCode);
		switch (gameAction) {
		case LEFT:
			manager.changeCurrentPoint(-1);
			break;
		case RIGHT:
			manager.changeCurrentPoint(+1);
			break;
		case DOWN:
			manager.changeCurrentPoint(-10);
			break;
		case UP:
			manager.changeCurrentPoint(+10);
			break;
		}
	}

	private class RepaintTask extends TimerTask {
		public void run() {
			MainCanvas.this.repaint();
		}
	}
}