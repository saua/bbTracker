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
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;
import org.bbtracker.Utils;
import org.bbtracker.mobile.BBTracker;
import org.bbtracker.mobile.Log;
import org.bbtracker.mobile.Preferences;
import org.bbtracker.mobile.TrackListener;
import org.bbtracker.mobile.TrackManager;
import org.bbtracker.mobile.TrackStore.TrackStoreException;

public class MainCanvas extends Canvas implements TrackListener, CommandListener {
	private static final int DEFAULT_STATUS_TIMEOUT = 5 * 1000;

	private static final int MAX_TILES = 2;

	private final Tile[] visibleTiles = new Tile[MAX_TILES];

	private final TrackManager manager;

	private final Tile trackTile;

	private final Tile elevationProfileTile;

	private final Tile speedProfileTile;

	private final StatusTile statusTile;

	private final Tile detailsTile;

	private final Command newTrackCommand;

	private final Command stopTrackingCommand;

	private final Command tracksCommand;

	private final Command optionsCommand;

	private final Command switchViewCommand;

	private final Command aboutCommand;

	private final Command exitCommand;

	private final Command markPointCommand;

	// #ifndef AVOID_FILE_API
	private final Command exportCommand;

	// #endif

	private String statusMessage = null;

	private long statusMessageEndTime = 0;

	private int tileConfiguration = 0;

	public MainCanvas(final TrackManager manager) {
		this.manager = manager;

		trackTile = new TrackTile(manager);
		elevationProfileTile = new ElevationPlotterTile(manager, DataProvider.TIME);
		speedProfileTile = new SpeedPlotterTile(manager, DataProvider.TIME);
		statusTile = new StatusTile(manager);
		detailsTile = new DetailsTile(manager);

		switchViewCommand = new Command("Switch View", Command.SCREEN, 10);
		markPointCommand = new Command("Mark current Point", Command.ITEM, 0);
		newTrackCommand = new Command("Start Track", Command.SCREEN, 2);
		stopTrackingCommand = new Command("Stop Track", Command.STOP, 3);
		tracksCommand = new Command("Tracks", Command.SCREEN, 4);
		optionsCommand = new Command("Options", Command.SCREEN, 5);
		aboutCommand = new Command("About", Command.SCREEN, 6);
		// #ifndef AVOID_FILE_API
		exportCommand = new Command("Export Track", Command.SCREEN, 1);
		// #endif
		exitCommand = new Command("Exit", Command.EXIT, 11);

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
		if (withStatus == true) {
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
			final Font font = Preferences.getInstance().getStatusFont();
			final int stringWidth = font.stringWidth(statusMessage);
			final int stringHeight = font.getHeight();
			final int width = getWidth();

			g.setFont(font);
			g.setColor(0x00ffffff);
			g.fillRect((width - 4) - stringWidth, 2, stringWidth + 2, stringHeight + 4);
			g.setColor(0x00000000);
			g.drawRect((width - 4) - stringWidth, 2, stringWidth + 2, stringHeight + 4);
			g.drawString(statusMessage, width - 2, 4, Graphics.TOP | Graphics.RIGHT);
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
		updateState(newState);
		for (int i = 0; i < visibleTiles.length && visibleTiles[i] != null; i++) {
			visibleTiles[i].stateChanged(newState);
		}
		repaint();
	}

	protected void updateState(final int newState) {
		switch (newState) {
		case TrackManager.STATE_STATIC:
			setStatusMessage("Static Track");
			removeCommand(stopTrackingCommand);
			removeCommand(markPointCommand);
			// #ifndef AVOID_FILE_API
			addCommand(exportCommand);
			// #endif
			break;
		case TrackManager.STATE_TRACKING:
			setStatusMessage("Tracking");
			addCommand(stopTrackingCommand);
			addCommand(markPointCommand);
			// #ifndef AVOID_FILE_API
			removeCommand(exportCommand);
			// #endif
			break;
		default:
			removeCommand(stopTrackingCommand);
			// #ifndef AVOID_FILE_API
			removeCommand(exportCommand);
			// #endif
			break;
		}
	}

	private void nextTileConfiguration() {
		tileConfiguration = (tileConfiguration + 1) % 4;
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
		case 3:
			setMainTile(detailsTile, false);
			setStatusMessage("Details");
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
		final int state = manager.getState();
		updateState(state);
	}

	public void commandAction(final Command command, final Displayable displayable) {
		if (command == exitCommand) {
			exitAction();
		} else if (command == markPointCommand) {
			markPointAction();
		} else if (command == switchViewCommand) {
			nextTileConfiguration();
			// #ifndef AVOID_FILE_API
		} else if (command == exportCommand) {
			final Track track = manager.getTrack();
			TracksForm.exportTrack(track, this);
			// #endif
		} else {
			final Displayable nextDisplayable;
			if (command == aboutCommand) {
				nextDisplayable = new AboutForm();
			} else if (command == optionsCommand) {
				nextDisplayable = new OptionsForm(manager);
			} else if (command == newTrackCommand) {
				nextDisplayable = new TrackNameForm(manager);
				if (manager.getState() == TrackManager.STATE_TRACKING) {
					final Alert alert = new Alert("Stop tracking?", "The track <" + manager.getTrack().getName() +
							"> is currently beeing recorded. Save that track and start a new one?", null,
							AlertType.WARNING);
					final Command startNewTrack = new Command("Start new Track", Command.OK, 1);
					alert.addCommand(startNewTrack);
					alert.addCommand(new Command("Continue tracking", Command.CANCEL, 0));
					alert.setCommandListener(new CommandListener() {

						public void commandAction(final Command cmd, final Displayable current) {
							if (cmd == startNewTrack) {
								try {
									manager.saveTrack();
									BBTracker.getDisplay().setCurrent(nextDisplayable);
								} catch (final TrackStoreException e) {
									TrackManager.showSaveFailedAlert(e, MainCanvas.this);
								}
							} else {
								BBTracker.getDisplay().setCurrent(MainCanvas.this);
							}
						}
					});
					BBTracker.alert(alert, null);
					return;
				}
			} else if (command == tracksCommand) {
				try {
					nextDisplayable = new TracksForm(manager);
				} catch (final TrackStoreException e) {
					BBTracker.nonFatal(e, "getting list of stored tracks", this);
					return;
				}
			} else if (command == stopTrackingCommand) {
				try {
					manager.saveTrack();
				} catch (final TrackStoreException e) {
					TrackManager.showSaveFailedAlert(e, MainCanvas.this);
					return;
				}
				nextDisplayable = this;
			} else {
				Log.log(this, "Unknown command: " + command + " <" + command.getLabel() + "/" + command.getLongLabel() +
						">");
				nextDisplayable = this;
			}
			BBTracker.getDisplay().setCurrent(nextDisplayable);
		}
	}

	private void exitAction() {
		final boolean isTracking = manager.getState() == TrackManager.STATE_TRACKING;
		String question = "Do you really want to quit?";
		if (isTracking) {
			question += "\nRecording the current Track <" + manager.getTrack().getName() +
					"> will stop and it will be saved.";
		}
		final Alert alert = new Alert("Really Quit?", question, null, AlertType.WARNING);
		final Command quitCommand = new Command("Quit", Command.OK, 2);
		alert.addCommand(quitCommand);
		alert.addCommand(GuiUtils.CANCEL_COMMAND);
		alert.setCommandListener(new CommandListener() {

			public void commandAction(final Command cmd, final Displayable current) {
				if (cmd == quitCommand) {
					if (isTracking) {
						try {
							manager.saveTrack();
						} catch (final TrackStoreException e) {
							final Alert saveFailedAlert = new Alert("Failed to save track!", "Failed to safe track:\n" +
									e.getMessage(), null, AlertType.ERROR);
							final Command quitAnywayCommand = new Command("Quit", "Quit anyway", Command.OK, 1);
							saveFailedAlert
									.addCommand(new Command("Cancel", "Return to Main Screen", Command.CANCEL, 0));
							saveFailedAlert.addCommand(quitAnywayCommand);
							saveFailedAlert.setCommandListener(new CommandListener() {
								public void commandAction(final Command cmd, final Displayable displayable) {
									if (cmd == quitAnywayCommand) {
										BBTracker.getInstance().shutdown(true);
									} else {
										BBTracker.getInstance().showMainCanvas();
									}
								}
							});
							BBTracker.alert(saveFailedAlert, null);
							return;
						}
					}
					BBTracker.getInstance().shutdown(true);
				} else {
					BBTracker.getDisplay().setCurrent(MainCanvas.this);
				}
			}
		});
		BBTracker.alert(alert, this);
	}

	private void markPointAction() {
		String errorMessage = null;
		final TrackPoint p = manager.getCurrentPoint();
		final int pi = manager.getCurrentPointIndex();
		if (manager.getState() != TrackManager.STATE_TRACKING) {
			errorMessage = "Not currently tracking!";
		} else if (p == null) {
			errorMessage = "No point to mark, yet!";
		}
		if (errorMessage != null) {
			BBTracker.alert(new Alert("Can not mark point", errorMessage, null, AlertType.INFO), this);
		} else {
			final Form f = new Form("Mark Point");
			final TextField textField = new TextField("Note: ", "", 30, TextField.ANY);
			f.append(textField);
			f.append(new StringItem("Point: ", pi + "/" + manager.getTrack().getPointCount()));
			f.append(new StringItem("Longitude: ", Utils.longitudeToString(p.getLongitude())));
			f.append(new StringItem("Latitude: ", Utils.latitudeToString(p.getLatitude())));
			f.addCommand(GuiUtils.OK_COMMAND);
			f.addCommand(GuiUtils.CANCEL_COMMAND);
			f.setCommandListener(new CommandListener() {

				public void commandAction(final Command cmd, final Displayable displayable) {
					if (cmd == GuiUtils.OK_COMMAND) {
						final String s = textField.getString();
						p.setName(s.length() == 0 ? "X" : s);
					}
					BBTracker.getDisplay().setCurrent(MainCanvas.this);
				}
			});
			BBTracker.getDisplay().setCurrent(f);
		}
	}

	protected void keyReleased(final int keyCode) {
		final int gameAction = getGameAction(keyCode);
		switch (keyCode) {
		case ' ':
		case '0':
			nextTileConfiguration();
			break;
		case 's':
		case '7':
			// start
			manager.changeToFirstPoint();
			break;
		case 'e':
		case '9':
			// last
			manager.changeToLastPoint();
			break;
		case 'x':
		case '8':
			markPointAction();
			break;
		default:
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
	}

	private class RepaintTask extends TimerTask {
		public void run() {
			MainCanvas.this.repaint();
		}
	}
}