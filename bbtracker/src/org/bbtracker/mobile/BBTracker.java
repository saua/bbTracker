package org.bbtracker.mobile;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.location.LocationException;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.rms.RecordStoreException;

import org.bbtracker.mobile.gui.MainCanvas;
import org.bbtracker.mobile.gui.NewTrackForm;

public class BBTracker extends MIDlet {
	private static final String NAME = "bbTracker";

	private static String version;

	private static String fullname;

	private static BBTracker instance;

	private final TrackManager trackManager;

	private final MainCanvas mainCanvas;

	private boolean firstStart = true;

	public BBTracker() {
		instance = this;

		version = getAppProperty("MIDlet-Version");
		fullname = NAME + " " + version;

		trackManager = new TrackManager();

		mainCanvas = new MainCanvas(trackManager);

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
			nonFatal(e, "Initializing Location Provider", mainCanvas);
		}
	}

	public void shutdown(final boolean destroy) {
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
	}

	protected void destroyApp(final boolean force) throws MIDletStateChangeException {
		shutdown(true);
	}

	protected void pauseApp() {
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

	public static void nonFatal(final Throwable t, final String action, final Displayable next) {
		log(t);
		final Alert alert = new Alert("Non-fatal Exception", "Non-fatal Exception while " + action + ": " +
				t.getMessage(), null, AlertType.WARNING);
		alert(alert, next);
	}

	public static void alert(final Alert alert, final Displayable next) {
		getDisplay().setCurrent(alert, next != null ? next : getInstance().mainCanvas);
	}

	public static void fatal(final Throwable t, final String action) {
		log(t);
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

	public void showMainCanvas() {
		getDisplay().setCurrent(mainCanvas);
	}

	public static void log(final Throwable e) {
		// used only for debugging
		e.printStackTrace();
	}

	public static void log(final String m) {
		// used only for debugging
		System.err.println(m);
	}
}