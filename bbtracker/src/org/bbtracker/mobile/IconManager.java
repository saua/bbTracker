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

import java.io.IOException;
import java.util.Hashtable;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Image;

public class IconManager {
	private static final int[] SIZES = new int[] { 12, 16, 22, 32 };

	private final int listSizeIndex;

	private final int choiceGroupSizeIndex;

	private final int alertSizeIndex;

	private static final Object NO_ICON = new Object();

	private static IconManager instance;

	private IconManager() {
		final Display display = BBTracker.getDisplay();
		listSizeIndex = getBestMatchingSizeIndex(display.getBestImageWidth(Display.LIST_ELEMENT), display
				.getBestImageHeight(Display.LIST_ELEMENT));
		choiceGroupSizeIndex = getBestMatchingSizeIndex(display.getBestImageWidth(Display.CHOICE_GROUP_ELEMENT),
				display.getBestImageHeight(Display.CHOICE_GROUP_ELEMENT));
		alertSizeIndex = getBestMatchingSizeIndex(display.getBestImageWidth(Display.ALERT), display
				.getBestImageHeight(Display.ALERT));
	}

	private static int getBestMatchingSizeIndex(final int width, final int height) {
		// at the moment we ignore the width and only look at the height
		for (int i = 0; i < SIZES.length; i++) {
			if (height <= SIZES[i]) {
				return i;
			}
		}
		return SIZES.length - 1;
	}

	public static IconManager getInstance() {
		if (instance == null) {
			instance = new IconManager();
		}
		return instance;
	}

	private final Hashtable iconCache = new Hashtable();

	public Image getListImage(final String name) {
		return getImage(name, listSizeIndex);
	}

	public Image getChoiceGroupImage(final String name) {
		return getImage(name, choiceGroupSizeIndex);
	}

	public Image getAlertImage(final String name) {
		return getImage(name, alertSizeIndex);
	}

	private synchronized Image getImage(final String name, final int sizeIndex) {
		Object[] icons = (Object[]) iconCache.get(name);
		if (icons == null) {
			icons = new Object[SIZES.length];
			iconCache.put(name, icons);
		}

		Object icon = icons[sizeIndex];
		if (icon == null) {
			final String resourceName = "/icons/" + SIZES[sizeIndex] + "/" + name + ".png";
			try {
				icon = Image.createImage(resourceName);
			} catch (final IOException e) {
				BBTracker.log(this, "Couldn't read \"" + resourceName + "\": " + e.getMessage());
				icon = NO_ICON;
			}
			icons[sizeIndex] = icon;
		}
		if (icon == NO_ICON) {
			if (sizeIndex > 0) {
				icon = getImage(name, sizeIndex - 1);
			} else {
				icon = null;
			}
		}
		return (Image) icon;
	}
}
