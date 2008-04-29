package org.bbtracker.mobile.gui;

import javax.microedition.lcdui.Command;

public final class GuiUtils {
	private GuiUtils() {
		// don't instanciate
	}

	public static final Command OK_COMMAND = new Command("Ok", Command.OK, 0);

	public static final Command CANCEL_COMMAND = new Command("Cancel", Command.CANCEL, 1);
}
