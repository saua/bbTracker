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
import java.util.TimerTask;
import java.util.Vector;

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
import org.bbtracker.TrackStoreException;
import org.bbtracker.Utils;
import org.bbtracker.mobile.BBTracker;
import org.bbtracker.mobile.Log;
import org.bbtracker.mobile.Preferences;
import org.bbtracker.mobile.TrackListener;
import org.bbtracker.mobile.TrackManager;
import org.bbtracker.mobile.config.ConfigFile;
import org.bbtracker.mobile.heartRate.HeartRateProvider;

public class MainCanvas extends Canvas implements TrackListener, CommandListener {
	private static final int DEFAULT_STATUS_TIMEOUT = 5 * 1000;

	private static final int MAX_TILES = 2;

	private final Tile[] visibleTiles = new Tile[MAX_TILES];

	private final TrackManager manager;

	private final TrackTile trackTile;

	private final Tile elevationProfileTile;

	private final Tile speedProfileTile;

	private final Tile heartRateProfileTile;

	private final StatusTile statusTile;

	private final Tile detailsTile;

	private final Command newTrackCommand;

	private final Command stopTrackingCommand;

	private final Command pauseTrackingCommand;

	private final Command continueTrackingCommand;

	private final Command tracksCommand;

	private final Command optionsCommand;

	private final Command switchViewCommand;

	private final Command aboutCommand;

	private final Command exitCommand;

	private final Command markPointCommand;

	private final Command startHeartRate;

	private final Command stopHeartRate;

	// #ifndef AVOID_FILE_API
	private final Command exportCommand;

	// #endif

	private String statusMessage = null;

	private long statusMessageEndTime = 0;

	private int tileConfiguration = 0;

	private Vector mapBackgrounds = new Vector();

	private static final int KEYBOARD_DEFAULT = 0;

	private static final int KEYBOARD_PAN = 1;

	private static final int KEYBOARD_ADJUST_MAP = 2;

	/** Current input mode. */
	private int keyboardMode;

	public MainCanvas(final TrackManager manager) {
		final Preferences pref = Preferences.getInstance();
		this.manager = manager;

		trackTile = new TrackTile(manager);
		trackTile.setMainCanvas(this);
		elevationProfileTile = new ElevationPlotterTile(manager, DataProvider.TIME);
		speedProfileTile = new SpeedPlotterTile(manager, DataProvider.TIME);
		if (pref.isHeartRateEnabled()) {
			heartRateProfileTile = new HeartRatePlotterTile(manager, DataProvider.TIME);
		} else {
			heartRateProfileTile = null;
		}
		statusTile = new StatusTile(manager);
		detailsTile = new DetailsTile(manager);

		loadBackgrounds();

		switchViewCommand = new Command("Switch View", Command.SCREEN, 10);
		markPointCommand = new Command("Mark current Point", Command.ITEM, 0);
		newTrackCommand = new Command("Start Track", Command.SCREEN, 2);
		stopTrackingCommand = new Command("Stop Track", Command.STOP, 3);
		pauseTrackingCommand = new Command("Pause Track", Command.SCREEN, 4);
		continueTrackingCommand = new Command("Continue Track", Command.SCREEN, 4);
		tracksCommand = new Command("Tracks", Command.SCREEN, 5);
		optionsCommand = new Command("Options", Command.SCREEN, 6);
		aboutCommand = new Command("About", Command.SCREEN, 7);
		startHeartRate = new Command("Start HR", Command.SCREEN, 5);
		stopHeartRate = new Command("Stop HR", Command.SCREEN, 5);
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
		if (pref.isHeartRateEnabled()) {
			addCommand(startHeartRate);
		}

		setCommandListener(this);

		setMainTile(trackTile, true);
	}

	public void loadBackgrounds() {
		final String mapDirectory = Preferences.getInstance().getMapDirectory();

		mapBackgrounds = new Vector();
		int count = 0;
		if (mapDirectory != null) {
			try {
				final Vector list = ConfigFile.openList("file:///" + mapDirectory + "list.txt");

				for (int i = 0; i < list.size(); i++) {
					final String name = (String) list.elementAt(i);
					try {
						mapBackgrounds.addElement(MapBackground.create(mapDirectory, mapDirectory + name));
						++count;
					} catch (final IOException e) {
						Log.log(this, e, "loadBackground: " + name);
					}
				}
			} catch (final IOException e) {
				Log.log(this, e, "loadBackgrounds");
			}
		}
		Log.log(this, count + " backgrounds found in " + mapDirectory);
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
		visibleTiles[0].showNotify();
		if (visibleTiles[1] != null) {
			visibleTiles[1].showNotify();
		}
	}

	protected void setStatusMessage(final String statusMessage) {
		setStatusMessage(statusMessage, DEFAULT_STATUS_TIMEOUT);
	}

	protected void setStatusMessage(final String statusMessage, final int duration) {
		this.statusMessage = statusMessage;
		if (duration != -1) {
			statusMessageEndTime = System.currentTimeMillis() + duration;
			BBTracker.getTimer().schedule(new RepaintTask(), duration + 10);
		} else {
			statusMessageEndTime = Long.MAX_VALUE;
		}
		repaint();
	}

	protected void sizeChanged(final int w, final int h) {
		super.sizeChanged(w, h);

		updateTileSize();
	}

	protected void paint(final Graphics g) {
		if (visibleTiles[0].width == 0) {
			// BlackBerry (at least 8800) seems not to call sizeChanged before
			// initial paint()
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
			removeCommand(pauseTrackingCommand);
			removeCommand(continueTrackingCommand);
			removeCommand(markPointCommand);
			// #ifndef AVOID_FILE_API
			addCommand(exportCommand);
			// #endif
			break;
		case TrackManager.STATE_TRACKING:
			setStatusMessage("Tracking");
			addCommand(stopTrackingCommand);
			addCommand(pauseTrackingCommand);
			addCommand(markPointCommand);
			// #ifndef AVOID_FILE_API
			removeCommand(exportCommand);
			// #endif
			break;
		default:
			removeCommand(stopTrackingCommand);
			removeCommand(pauseTrackingCommand);
			removeCommand(continueTrackingCommand);
			removeCommand(markPointCommand);
			// #ifndef AVOID_FILE_API
			removeCommand(exportCommand);
			// #endif
			break;
		}
	}

	private void nextTileConfiguration() {
		tileConfiguration = (tileConfiguration + 1) % 5;
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
			if (Preferences.getInstance().isHeartRateEnabled()) {
				setMainTile(heartRateProfileTile, true);
				setStatusMessage("HR over time");
				break;
			} else {
				tileConfiguration++;
				// deliberate fall-through
			}
		case 4:
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
		final HeartRateProvider heartRateProvider = manager.getHeartRateProvider();
		if (command == exitCommand) {
			exitAction();
		} else if (command == markPointCommand) {
			markPointAction();
		} else if (command == startHeartRate) {
			if (heartRateProvider.start()) {
				removeCommand(startHeartRate);
				addCommand(stopHeartRate);
				heartRateProvider.setCanvas(this);
			}
		} else if (command == stopHeartRate) {
			heartRateProvider.stop();
			removeCommand(stopHeartRate);
			addCommand(startHeartRate);
		} else if (command == pauseTrackingCommand) {
			pauseTrackingAction();
		} else if (command == continueTrackingCommand) {
			continueTrackingAction();
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
					final Alert alert = new Alert("Stop tracking?", "The track <" + manager.getTrack().getName()
							+ "> is currently beeing recorded. Save that track and start a new one?", null,
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
				Log.log(this, "Unknown command: " + command + " <" + command.getLabel() + "/" + command.getLongLabel()
						+ ">");
				nextDisplayable = this;
			}
			BBTracker.getDisplay().setCurrent(nextDisplayable);
		}
	}

	private void exitAction() {
		final boolean isTracking = manager.getState() == TrackManager.STATE_TRACKING;
		String question = "Do you really want to quit?";
		if (isTracking) {
			question += "\nRecording the current Track <" + manager.getTrack().getName()
					+ "> will stop and it will be saved.";
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
							final Alert saveFailedAlert = new Alert("Failed to save track!", "Failed to safe track:\n"
									+ e.getMessage(), null, AlertType.ERROR);
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
			final String oldName = p.getName();
			final Form f = new Form("Mark Point");
			final TextField textField = new TextField("Note: ", oldName != null ? oldName : "", 30, TextField.ANY);
			f.append(textField);
			f.append(new StringItem("Point: ", (pi + 1) + "/" + manager.getTrack().getPointCount()));
			f.append(new StringItem("Longitude: ", Utils.longitudeToString(p.getLongitude())));
			f.append(new StringItem("Latitude: ", Utils.latitudeToString(p.getLatitude())));
			f.addCommand(GuiUtils.OK_COMMAND);
			f.addCommand(GuiUtils.CANCEL_COMMAND);
			f.setCommandListener(new CommandListener() {

				public void commandAction(final Command cmd, final Displayable displayable) {
					if (cmd == GuiUtils.OK_COMMAND) {
						final String s = textField.getString();
						final String newName;
						if ("".equals(s)) {
							if (oldName == null) {
								newName = "X";
							} else {
								newName = null;
							}
						} else {
							newName = s;
						}
						p.setName(newName);
					}
					BBTracker.getDisplay().setCurrent(MainCanvas.this);
				}
			});
			BBTracker.getDisplay().setCurrent(f);
		}
	}

	private void pauseTrackingAction() {
		removeCommand(pauseTrackingCommand);
		addCommand(continueTrackingCommand);
		manager.pauseTracking();
		setStatusMessage("Paused!", -1);
	}

	private void continueTrackingAction() {
		removeCommand(continueTrackingCommand);
		addCommand(pauseTrackingCommand);
		manager.continueTracking();
		setStatusMessage("Continuing...");
	}

	protected void keyRepeated(final int keyCode) {
		keyReleased(keyCode);
	}

	protected void keyReleased(final int keyCode) {
		final int gameAction = getGameAction(keyCode);
		switch (keyboardMode) {
		case KEYBOARD_DEFAULT:
			handleDefaultKey(keyCode, gameAction);
			break;
		case KEYBOARD_PAN:
			handlePanMap(keyCode, gameAction);
			break;
		case KEYBOARD_ADJUST_MAP:
			handleAdjustMap(keyCode, gameAction);
			break;
		default:
			break;
		}
	}

	private void handleAdjustMap(final int keyCode, final int gameAction) {
		final MapBackground mapBackground = trackTile.getMapBackground();
		if (mapBackground == null) {
			keyboardMode = KEYBOARD_DEFAULT;
			return;
		}
		switch (gameAction) {
		case LEFT:
			mapBackground.adjustMapPosition(-1, 0);
			break;
		case RIGHT:
			mapBackground.adjustMapPosition(1, 0);
			break;
		case DOWN:
			mapBackground.adjustMapPosition(0, 1);
			break;
		case UP:
			mapBackground.adjustMapPosition(0, -1);
			break;
		default:
			mapBackground.saveConfiguration();
			keyboardMode = KEYBOARD_DEFAULT;
		}
	}

	/**
	 * Reset map to center (if a map background is currently displayed).
	 */
	private void resetPanMap() {
		trackTile.resetMapPan();
	}

	/**
	 * Pan the map.
	 * 
	 * @param keyCode
	 *            keycode
	 * @param gameAction
	 *            key action
	 */
	private void handlePanMap(final int keyCode, final int gameAction) {
		switch (gameAction) {
		case LEFT:
			trackTile.panPosition(-getWidth() >> 1, 0);
			break;
		case RIGHT:
			trackTile.panPosition(getWidth() >> 1, 0);
			break;
		case DOWN:
			trackTile.panPosition(0, getHeight() >> 1);
			break;
		case UP:
			trackTile.panPosition(0, -getHeight() >> 1);
			break;
		default:
			keyboardMode = KEYBOARD_DEFAULT;
			resetPanMap();
			break;
		}
		trackTile.onScaleChanged();
		repaint();
	}

	private void handleDefaultKey(final int keyCode, final int gameAction) {
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
		case 'z':
		case '3':
			changeZoom(1);
			resetPanMap();
			break;
		case 'h':
		case '6':
			changeZoom(-1);
			resetPanMap();
			break;
		case 'a':
		case '2':
			keyboardMode = KEYBOARD_ADJUST_MAP;
			break;
		case 'p':
		case '4':
			keyboardMode = KEYBOARD_PAN;
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
			resetPanMap();
		}
	}

	private void changeZoom(final int direction) {
		final MapBackground current = trackTile.getBackground();
		int idx;
		if (current == null) {
			if (direction > 0) {
				idx = -1;
			} else {
				idx = mapBackgrounds.size();
			}
		} else {
			idx = mapBackgrounds.indexOf(current);
		}
		idx += direction;
		MapBackground newBackground;
		if (idx >= mapBackgrounds.size() || idx < 0) {
			newBackground = null;
		} else {
			newBackground = (MapBackground) mapBackgrounds.elementAt(idx);
		}
		trackTile.setMapBackground(newBackground);
		repaint();
	}

	private class RepaintTask extends TimerTask {
		public void run() {
			MainCanvas.this.repaint();
		}
	}
}