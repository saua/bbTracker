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

import javax.microedition.lcdui.Graphics;

import org.bbtracker.TrackPoint;
import org.bbtracker.mobile.TrackListener;

/**
 * A tile represents a Square area on the Screen that draws itself.
 * 
 * Each Tile will automagically receive all events received by a {@link TrackListener}, as long as it is visible. When
 * it is not shown (i.e. it has been hidden or the Canvas containing it was not shown, then some of those events may not
 * be received. In this case {@link #showNotify()} will be called before the next time {@link #doPaint(Graphics)} is
 * called.
 */
public abstract class Tile implements TrackListener {
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

	public void currentPointChanged(final TrackPoint newPoint, final int newIndex) {
		// nothing
	}

	public void newPoint(final TrackPoint newPoint, final boolean boundsChanged, final boolean newSegment) {
		// nothing
	}

	public void stateChanged(final int newState) {
		// nothing
	}

	public abstract void showNotify();
}
