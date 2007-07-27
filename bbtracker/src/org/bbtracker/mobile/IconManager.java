package org.bbtracker.mobile;

import java.io.IOException;
import java.util.Hashtable;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Image;

public class IconManager {
	private static final int[] SIZES = new int[] { 16, 22, 32 };

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
				BBTracker.log("Couldn't read \"" + resourceName + "\": " + e.getMessage());
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
