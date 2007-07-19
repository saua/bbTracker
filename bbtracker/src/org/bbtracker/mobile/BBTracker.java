package org.bbtracker.mobile;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.location.LocationException;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import org.bbtracker.mobile.gui.MainCanvas;
import org.bbtracker.mobile.gui.NewTrackForm;

public class BBTracker extends MIDlet {
	private static final String NAME = "BBTracker";

	private static String version;

	private static String fullname;

	private static BBTracker instance;

	private final TrackManager trackManager;

	private MainCanvas mainCanvas;

	private boolean firstStart = true;

	public BBTracker() {
		instance = this;

		version = getAppProperty("MIDlet-Version");
		fullname = NAME + " " + version;

		trackManager = new TrackManager();

		try {
			switch (Preferences.getInstance().getStartAction()) {
			case Preferences.START_ACTION_INIT_GPS:
			case Preferences.START_ACTION_NEWTRACK:
				trackManager.initLocationProvider();
				break;
			case Preferences.START_ACTION_NOTHING:
				break;
			}
		} catch (final LocationException e) {
			nonFatal(e, "Initializing Location Provider");
		}
	}

	public void shutdown() {
		if (trackManager != null) {
			trackManager.shutdown();
		}
		Preferences.getInstance().store();
		notifyDestroyed();
	}

	protected void destroyApp(final boolean arg0) throws MIDletStateChangeException {
		shutdown();
	}

	protected void pauseApp() {
		mainCanvas = null;
	}

	protected void startApp() throws MIDletStateChangeException {
		if (firstStart) {
			if (Preferences.getInstance().getStartAction() == Preferences.START_ACTION_NEWTRACK) {
				Display.getDisplay(this).setCurrent(new NewTrackForm(trackManager));
			} else {
				showMainCanvas();
			}
			firstStart = false;
		} else {
			showMainCanvas();
		}

	}

	public static void alert(final Alert alert) {
		final Display d = getDisplay();
		Displayable next = d.getCurrent();
		if (next == null) {
			final BBTracker i = getInstance();
			if (i.mainCanvas == null) {
				i.mainCanvas = new MainCanvas(i.trackManager);
			}
			next = i.mainCanvas;
		}
		d.setCurrent(alert, next);
	}

	public static Display getDisplay() {
		return Display.getDisplay(instance);
	}

	public static String getFullName() {
		return fullname;
	}

	public static BBTracker getInstance() {
		return instance;
	}

	public static String getName() {
		return NAME;
	}

	public static String getVersion() {
		return version;
	}

	public static void nonFatal(final Throwable t, final String action) {
		t.printStackTrace();
		alert(new Alert("Non-fatal Exception", "Non-fatal Exception while " + action + ": " + t.getMessage(), null,
				AlertType.WARNING));
	}

	public static void fatal(final Throwable t, final String action) {
		// TODO
		getInstance().shutdown();
	}

	public void showMainCanvas() {
		if (mainCanvas == null) {
			mainCanvas = new MainCanvas(trackManager);
		}
		getDisplay().setCurrent(mainCanvas);
	}
}