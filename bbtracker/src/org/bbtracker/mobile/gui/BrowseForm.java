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
import java.util.Enumeration;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import org.bbtracker.mobile.BBTracker;
import org.bbtracker.mobile.IconManager;

public class BrowseForm extends List implements CommandListener {
	private final String title;

	private String path;

	private Runnable callback;

	private final Command selectCommand;

	private final Command cancelCommand;

	public BrowseForm(final String title, final String path) {
		this(title, path, null);
	}

	public BrowseForm(final String title, final String path, final Runnable callback) {
		super(title, List.IMPLICIT);
		this.title = title;
		this.path = path.length() == 0 ? null : path;
		this.callback = callback;

		selectCommand = new Command("Select", Command.ITEM, 1);
		cancelCommand = new Command("Cancel", Command.CANCEL, 2);
		setSelectCommand(selectCommand);
		addCommand(cancelCommand);
		setCommandListener(this);

		updateContent();
	}

	public void setCallback(final Runnable callback) {
		this.callback = callback;
	}

	private void updateContent() {
		deleteAll();
		final IconManager im = IconManager.getInstance();
		setTitle(path == null ? title : path + " - " + title);
		if (path == null) {
			final Enumeration roots = FileSystemRegistry.listRoots();
			while (roots.hasMoreElements()) {
				final String root = (String) roots.nextElement();
				append(root, im.getListImage(getRootIconName(root)));
			}
		} else {
			try {
				final FileConnection connection = (FileConnection) Connector.open("file:///" + path);
				if (!connection.isDirectory()) {
					path = null;
					updateContent();
				}
				append("<select this directory>", null);
				append("..", im.getListImage("go-up"));
				final Enumeration list = connection.list();
				while (list.hasMoreElements()) {
					final String element = (String) list.nextElement();
					if (element.endsWith("/")) {
						append(element, im.getListImage("folder"));
					}
				}
			} catch (final IOException e) {
				BBTracker.nonFatal(e, "getting file roots", null);
			}
		}
	}

	private String getRootIconName(final String string) {
		if (string.startsWith("CF")) {
			return "cf";
		} else if (string.startsWith("SD")) {
			return "sd";
		}
		// generic default
		return "disk";
	}

	public void setPath(final String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public void commandAction(final Command cmd, final Displayable current) {
		if (cmd == selectCommand) {
			if (path != null && getSelectedIndex() == 0) {
				// <SELECT> selected
				callback.run();
			} else {
				final String selected = getString(getSelectedIndex());
				if (selected.equals("..")) {
					final int slashIndex = path.lastIndexOf('/', path.length() - 2);
					if (slashIndex == -1) {
						path = null;
					} else {
						path = path.substring(0, slashIndex);
					}
				} else {
					path = path == null ? selected : path + selected;
				}
				updateContent();
			}
		} else if (cmd == cancelCommand) {
			path = null;
			callback.run();
		}
	}
}
