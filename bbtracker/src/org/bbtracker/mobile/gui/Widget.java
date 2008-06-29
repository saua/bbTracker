package org.bbtracker.mobile.gui;

import javax.microedition.lcdui.Graphics;

public interface Widget {

	/**
	 * Draw.
	 * 
	 * @param g
	 *            graphics object
	 */
	public abstract void paint(final Graphics g);

	/**
	 * Position setter.
	 * 
	 * @param x
	 *            x
	 * @param y
	 *            y
	 */
	public abstract void setPosition(final int x, final int y);

	/**
	 * size setter.
	 * 
	 * @param w
	 *            x
	 * @param h
	 *            y
	 */
	public abstract void setSize(final int w, final int h);

	/**
	 * Dimension accessor.
	 * 
	 * @return sy
	 */
	public abstract int getHeight();

	/**
	 * Dimension accessor.
	 * 
	 * @return sx
	 */
	public abstract int getWidth();

	/**
	 * Dimension accessor.
	 * 
	 * @param h
	 */
	public abstract void setHeight(final int h);

	/**
	 * Dimension accessor.
	 * 
	 * @param w
	 */
	public abstract void setWidth(final int w);
}