package org.bbtracker.mobile.gui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;

import org.bbtracker.mobile.BBTracker;

public class AboutForm extends Form implements CommandListener {
	public AboutForm() {
		super("About " + BBTracker.getName());
		append(new StringItem("Version: ", BBTracker.getVersion()));
		addCommand(new Command("Back", Command.BACK, 0));
		setCommandListener(this);
	}

	public void commandAction(final Command command, final Displayable source) {
		BBTracker.getInstance().showMainCanvas();
	}

}
