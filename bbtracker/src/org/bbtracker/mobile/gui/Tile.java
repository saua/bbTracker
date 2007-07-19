package org.bbtracker.mobile.gui;

import javax.microedition.lcdui.Graphics;

public abstract class Tile {
	protected int xOffset;

	protected int yOffset;

	protected int width;

	protected int height;

	public final void resize(final int xOffset, final int yOffset, final int width, final int height) {
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		this.width = width;
		this.height = height;
		onResize();
	}

	protected void onResize() {
		// NOOP
	}

	public final void paint(final Graphics g) {
		g.translate(xOffset, yOffset);
		try {
			doPaint(g);
		} finally {
			g.translate(-xOffset, -yOffset);
		}
	}

	protected abstract void doPaint(final Graphics g);
}
