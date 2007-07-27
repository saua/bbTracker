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
		append("Icons have been taken (and sometimes modified) from the Tango Project (http://tango-project.org/) and the Human Icon Theme (Copyright 2004-2006 Canonical Ltd.). Both projects release their icons under the Creative Commons Attribution-ShareAlike 2.5 license. Any modifications I did on those icons are released under the same license.");
		addCommand(new Command("Back", Command.BACK, 0));
		setCommandListener(this);
	}

	public void commandAction(final Command command, final Displayable source) {
		BBTracker.getInstance().showMainCanvas();
	}

}
