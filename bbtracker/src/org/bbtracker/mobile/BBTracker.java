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
package org.bbtracker.mobile;

import java.util.Timer;
import java.util.Vector;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.rms.RecordStoreException;

import org.bbtracker.mobile.TrackStore.TrackStoreException;
import org.bbtracker.mobile.gps.DummyLocationProvider;
import org.bbtracker.mobile.gps.Jsr179LocationProvider;
import org.bbtracker.mobile.gps.LocationException;
import org.bbtracker.mobile.gps.LocationProvider;
import org.bbtracker.mobile.gps.SerialLocationProvider;
import org.bbtracker.mobile.gui.MainCanvas;
import org.bbtracker.mobile.gui.OptionsForm;
import org.bbtracker.mobile.gui.TrackNameForm;
import org.bbtracker.mobile.gui.TracksForm;

public class BBTracker extends MIDlet {
	private static final String NAME = "bbTracker";

	private static String version;

	private static String fullname;

	private static BBTracker instance;

	private static boolean firstStart = true;

	private static boolean jsr179Available = false;

	private static boolean bluetoothAvailable = false;

	// #ifndef AVOID_FILE_API
	private static boolean fileUrlAvailable = false;

	// #endif

	private final TrackManager trackManager;

	private final MainCanvas mainCanvas;

	private final Timer timer;

	public BBTracker() {
		instance = this;

		version = getAppProperty("MIDlet-Version");
		fullname = NAME + " " + version;

		timer = new Timer();

		trackManager = new TrackManager();

		mainCanvas = new MainCanvas(trackManager);

		Log.initLog();
	}

	public void shutdown(final boolean destroy) {
		Log.log(this, "shutdown " + destroy);
		if (trackManager != null) {
			trackManager.shutdown();
		}
		try {
			Preferences.getInstance().store();
		} catch (final RecordStoreException ignored) {
			// ignore
		}
		if (destroy) {
			notifyDestroyed();
		}
		Log.shutdown();
	}

	public static String getFullName() {
		return fullname;
	}

	public static String getName() {
		return NAME;
	}

	public static String getVersion() {
		return version;
	}

	public static BBTracker getInstance() {
		return instance;
	}

	public static Display getDisplay() {
		return Display.getDisplay(instance);
	}

	public static Timer getTimer() {
		return instance.timer;
	}

	public static void nonFatal(final Throwable t, final String action, final Displayable next) {
		Log.log(BBTracker.class, t, "non-fatal " + action);
		final Alert alert = new Alert("Non-fatal Exception", "Non-fatal Exception while " + action + ": " +
				t.getMessage(), null, AlertType.WARNING);
		alert(alert, next);
	}

	public static void fatal(final Throwable t, final String action) {
		Log.log(BBTracker.class, t, "fatal " + action);
		final BBTracker i = getInstance();
		i.shutdown(false);
		final Form errorForm = new Form("Fatal Exception!");
		errorForm.append("Fatal Exception while " + action + ":");
		errorForm.append(t.toString());
		errorForm.addCommand(new Command("Exit", Command.EXIT, 0));
		errorForm.setCommandListener(new CommandListener() {

			public void commandAction(final Command cmd, final Displayable displayable) {
				i.notifyDestroyed();
			}
		});
		getDisplay().setCurrent(errorForm);
	}

	public static void alert(final Alert alert, final Displayable next) {
		getDisplay().setCurrent(alert, next != null ? next : getInstance().mainCanvas);
	}

	public void showMainCanvas() {
		getDisplay().setCurrent(mainCanvas);
	}

	public static boolean isJsr179Available() {
		return jsr179Available;
	}

	public static boolean isBluetoothAvailable() {
		return bluetoothAvailable;
	}

	// #ifndef AVOID_FILE_API
	public static boolean isFileUrlAvailable() {
		return fileUrlAvailable;
	}

	// #endif

	protected void destroyApp(final boolean force) throws MIDletStateChangeException {
		shutdown(true);
	}

	protected void pauseApp() {
		Log.log(this, "pauseApp");
	}

	protected void startApp() throws MIDletStateChangeException {
		Log.log(this, firstStart ? "first startApp" : "startApp");
		if (firstStart) {
			firstStart = false;
			doFirstStart();
		} else {
			showMainCanvas();
		}
	}

	private void doFirstStart() {
		final Form initForm = new Form("Initializing...");
		initForm.append("Initializing " + getFullName() + "...\n");
		getDisplay().setCurrent(initForm);
		final Runnable run = new Initializer(initForm);
		new Thread(run).start();
	}

	private final class Initializer implements Runnable {
		private final Form initForm;

		private final Font font;

		private Initializer(final Form initForm) {
			this.initForm = initForm;
			font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
		}

		public void run() {
			// GPS
			addString("Checking GPS abilities ...\n");
			final String jsr179Version = System.getProperty("microedition.location.version");
			jsr179Available = (jsr179Version != null);
			addAPI("JSR-179", jsr179Available);

			try {
				Class.forName("javax.bluetooth.LocalDevice");
				bluetoothAvailable = true;
			} catch (final ClassNotFoundException e1) {
				bluetoothAvailable = false;
			}
			addAPI("Bluetooth", bluetoothAvailable);

			// storage
			addString("Checking Storage abilities ...\n");

			// everyone has RMS
			addAPI("RMS", true);

			// #ifndef AVOID_FILE_API
			final String fileConnectionVersion = System.getProperty("microedition.io.file.FileConnection.version");
			fileUrlAvailable = (fileConnectionVersion != null);
			addAPI("File", fileUrlAvailable);
			// #endif

			final Vector stores = new Vector();
			// #ifndef AVOID_FILE_API
			if (fileUrlAvailable) {
				stores.addElement(new FileTrackStore());
			}
			// #endif
			stores.addElement(new RMSTrackStore());
			final TrackStore[] trackStores = new TrackStore[stores.size()];
			stores.copyInto(trackStores);
			final LocationProvider locationProvider;

			final Preferences preferences = Preferences.getInstance();
			String forceOptionsMessage = null;
			final int selectedLocationProvider = preferences.getLocationProvider();
			switch (selectedLocationProvider) {
			case Preferences.LOCATION_JSR179:
				if (jsr179Available) {
					Log.log(this, "Using JSR 179 Location Provider");
					locationProvider = new Jsr179LocationProvider();
				} else {
					Log.log(this, "NOT using JSR 179 Location Provider, because JSR 179 is not available.");
					locationProvider = new DummyLocationProvider();
					forceOptionsMessage = "Invalid location provider selected, please check options.";
				}
				break;
			case Preferences.LOCATION_BLUETOOTH:
				if (!bluetoothAvailable) {
					Log.log(this,
							"NOT using Serial/Bluetooth Location Provider, because Bluetooth API is not available.");
					locationProvider = new DummyLocationProvider();
					forceOptionsMessage = "Invalid location provider selected, please check options.";
				} else if (preferences.getBluetoothUrl() == null || preferences.getBluetoothUrl().length() == 0) {
					Log.log(this,
							"NOT using Serial/Bluetooth Location Provider, because no Bluetooth device is selectede.");
					locationProvider = new DummyLocationProvider();
					forceOptionsMessage = "No Bluetooth device selected, please check options.";
				} else {
					Log.log(this, "Using Serial/Bluetooth Location Provider");
					locationProvider = new SerialLocationProvider();
				}

				break;
			case Preferences.LOCATION_NONE:
			default:
				Log.log(this, "Using Dummy Location Provider, as configured.");
				locationProvider = new DummyLocationProvider();
				break;
			}
			trackManager.initialize(locationProvider, trackStores);

			final Display display = getDisplay();
			if (forceOptionsMessage != null) {
				final Alert alert = new Alert("Warning!", forceOptionsMessage, null, AlertType.WARNING);
				display.setCurrent(alert, new OptionsForm(trackManager));
			} else {
				final int startAction = preferences.getStartAction();
				switch (startAction) {
				case Preferences.START_ACTION_SHOW_OPTIONS:
					display.setCurrent(new OptionsForm(trackManager));
					break;
				case Preferences.START_ACTION_NEWTRACK:
					display.setCurrent(new TrackNameForm(trackManager));
					break;
				case Preferences.START_ACTION_TRACKS_SCREEN:
					try {
						display.setCurrent(new TracksForm(trackManager));
					} catch (final TrackStoreException e) {
						nonFatal(e, "Opening Track Screen", mainCanvas);
					}
					break;
				case Preferences.START_ACTION_INIT_GPS:
					try {
						locationProvider.init();
						showMainCanvas();
					} catch (final LocationException e) {
						nonFatal(e, "initializing GPS", mainCanvas);
					}
					break;
				default:
					showMainCanvas();
				}
			}
		}

		private void addAPI(final String apiName, final boolean available) {
			final String msg = apiName + (available ? "" : " NOT") + " available";
			addString(msg + "\n");
			Log.log(this, "[API] " + msg);
		}

		private void addString(final String str) {
			final StringItem si = new StringItem(null, str);
			si.setFont(font);
			initForm.append(si);
		}
	}
}
