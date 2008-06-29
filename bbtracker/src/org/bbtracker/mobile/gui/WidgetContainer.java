package org.bbtracker.mobile.gui;

import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.lcdui.Graphics;

public final class WidgetContainer implements Widget {
	/** true if in horizontal direction. */
	private boolean horizontal;

	/** widget list. */
	private Vector children;

	/** Alignement (Graphics.HCENTER, VCENTER, LEFT, RIGHT, TOP or DOWN). */
	private boolean alignement;

	/**
	 * 
	 * @param h
	 *            true if horizontal
	 */
	public WidgetContainer(final boolean h) {
		horizontal = h;
	}

	/**
	 * Add a child.
	 * 
	 * @param w
	 *            widget to add
	 */
	public void addElement(final Widget w) {
		children.addElement(w);
	}

	/**
	 * @return sum of children widget height
	 */
	public int getHeight() {
		int height = 0;
		for (final Enumeration e = children.elements(); e.hasMoreElements();) {
			final Widget widget = (Widget) e.nextElement();
			final int h = widget.getHeight();
			if (horizontal) {
				height = Math.max(height, h);
			} else {
				height += h;
			}
		}
		return height;
	}

	/**
	 * @return sum of children widget height
	 */
	public int getWidth() {
		int width = 0;
		for (final Enumeration e = children.elements(); e.hasMoreElements();) {
			final Widget widget = (Widget) e.nextElement();
			final int w = widget.getWidth();
			if (!horizontal) {
				width = Math.max(width, w);
			} else {
				width += w;
			}
		}
		return width;
	}

	public void paint(final Graphics g) {

	}

	public void setPosition(final int x, final int y) {
		// TODO Auto-generated method stub

	}

	public void setSize(final int w, final int h) {
		// TODO Auto-generated method stub

	}

	/**
	 * Compute and fillout position in children.
	 * 
	 * Start at starting position.
	 */
	public void layout(final int startingPosition) {

	}

	public void setHeight(final int h) {
		// TODO Auto-generated method stub

	}

	public void setWidth(final int w) {
		// TODO Auto-generated method stub

	}
}
