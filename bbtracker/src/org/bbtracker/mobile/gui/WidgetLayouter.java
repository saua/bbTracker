package org.bbtracker.mobile.gui;

/**
 * Layouter.
 */
public final class WidgetLayouter {
	/** Position. */
	private int px, py;
	/** Dimension. */
	private int width, height;
	/** if true Y increases. */
	private boolean topToBottom;
	/** if true centers widget in the middle. */
	private final boolean hcenter = false;

	/**
	 * @param topToBottom
	 *            if true Y increases
	 */
	public void setTopToBottom(final boolean topToBottom) {
		this.topToBottom = topToBottom;
	}

	/**
	 * @return the px
	 */
	public int getX() {
		return px;
	}

	/**
	 * @param x
	 *            the px to set
	 */
	public void setX(final int x) {
		px = x;
	}

	/**
	 * @return the py
	 */
	public int getY() {
		return py;
	}

	/**
	 * @param y
	 *            the py to set
	 */
	public void setY(final int y) {
		py = y;
	}

	/**
	 * @return the width
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @param w
	 *            the width to set
	 */
	public void setWidth(final int w) {
		width = w;
	}

	/**
	 * @return the height
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * @param h
	 *            the height to set
	 */
	public void setHeight(final int h) {
		height = h;
	}

	/**
	 * Add a line of widgets.
	 * 
	 * @param widgets
	 *            widget list
	 */
	public void addLine(final Widget[] widgets) {
		int lineHeight = 0;
		int lineWidth = 0;
		for (int i = 0; i < widgets.length; i++) {
			lineHeight = Math.max(lineHeight, widgets[i].getHeight());
			lineWidth += widgets[i].getWidth();
		}
		if (!topToBottom) {
			py -= lineHeight;
		}
		int offX = 0;
		if (hcenter) {
			offX = (width - lineWidth) >> 1;
		}
		for (int i = 0; i < widgets.length; i++) {
			final Widget widget = widgets[i];
			widget.setHeight(lineHeight);
			widget.setPosition(offX, py);
			offX += widget.getWidth();
		}
		if (topToBottom) {
			py += lineHeight;
		}
	}
}
