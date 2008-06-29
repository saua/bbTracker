package org.bbtracker.mobile.gui;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import org.bbtracker.mobile.Log;

/**
 * Mini Widget can display: Image, Text.
 * 
 * @TODO direction, progress bar
 * 
 * @author Sebastien Chauvin
 * 
 */
public final class MiniWidget implements Widget {
	/** Widget type. */
	public static final int TEXT = 0;
	/** Widget type. */
	public static final int ORIENTATION = 1;
	/** Widget type. */
	public static final int IMAGE = 2;

	/** Size. */
	public static final int SMALL = 0;
	/** Size. */
	public static final int MEDIUM = 0x10000;
	/** Size. */
	public static final int LARGE = 0x20000;

	/** Extract type from {@link #mode}. */
	private static final int TYPE_MASK = 0xffff;

	/** Extract size from {@link #mode}. */
	private static final int SIZE_MASK = 0x10000;

	/** Extract size from {@link #mode}. */
	private static final int SIZE_SHIFT = 16;

	/** mode, type | size. */
	private int mode;

	/** SIZES[type][size]. */
	private static int[][] SIZES = new int[][] { { 10, 18, 30 }, };

	/** Flags to use when drawing images. */
	private static final int IMAGE_FLAGS = Graphics.HCENTER | Graphics.VCENTER;
	/** Position. */
	private int px, py;
	/** Dimension. */
	private int width, height;

	/** Text. */
	private String text;
	/** Image Reference. */
	private Image image;

	/** Text font. */
	private Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);

	/**
	 * direction in degree when in direction mode.
	 */
	private int orientation;

	/**
	 * Temporary color definition.
	 */
	private static final int BACKGROUND_COLOR = 0xffffff;
	/** Temporary color definition. */
	private static final int TEXT_FRONT_COLOR = 0;

	/** Padding for text content. */
	private static final int TEXT_PADDING_W = 3;
	/** Padding for text content. */
	private static final int TEXT_PADDING_H = 2;

	/**
	 * MiniWidget construction.
	 * 
	 */
	public MiniWidget() {
		px = Integer.MIN_VALUE;
		py = Integer.MIN_VALUE;
		width = Integer.MIN_VALUE;
		height = Integer.MIN_VALUE;
	}

	/**
	 * MiniWidget construction.
	 * 
	 * @param t
	 *            text used as a template to get dimension
	 */
	public MiniWidget(final String t) {
		px = Integer.MIN_VALUE;
		py = Integer.MIN_VALUE;
		setDimensionsForString(t);
	}

	/**
	 * MiniWidget construction.
	 * 
	 * @param x
	 *            x
	 * @param y
	 *            y
	 * @param w
	 *            width
	 * @param h
	 *            height
	 */
	public MiniWidget(final int x, final int y, final int w, final int h) {
		px = x;
		py = y;
		width = w;
		height = h;
		Log.log(this, "MiniWidget: " + dbgCoordinates());
	}

	/**
	 * Clear all content bearing members.
	 */
	private void reset() {
		text = null;
		image = null;
		orientation = -1;
	}

	/**
	 * Set Text.
	 * 
	 * @param text
	 *            text
	 */
	public void setText(final String text) {
		reset();
		this.text = text;
	}

	/**
	 * Set Image to be downloaded.
	 * 
	 * @param reference
	 *            image
	 */
	public void setImageReference(final String reference) {
		reset();

		// TODO
	}

	/**
	 * Print widget coordinates in a string.
	 * 
	 * @return string
	 */
	private String dbgCoordinates() {
		return "[" + width + "x" + height + "+" + px + "," + py + "]";
	}

	/** Get size from mode. */
	private int getSize() {
		return mode >> SIZE_SHIFT;
	}

	/**
	 * Paint widget.
	 * 
	 * @param g
	 *            target graphic object
	 */
	public void paint(final Graphics g) {
		g.setClip(px, py, width, height);

		paintBackground(g);

		if (text != null) {
			g.setColor(TEXT_FRONT_COLOR);
			g.drawString(text, px + TEXT_PADDING_W, py + TEXT_PADDING_H, Graphics.TOP | Graphics.LEFT);
			// TODO: Implement scrolling !
		} else if (image != null) {
			g.drawImage(image, px + width / 2, py + height / 2, IMAGE_FLAGS);
		} else if (orientation != 1) {
			paintOrientation(g);
		}
	}

	/** Paint orientation. */
	private void paintOrientation(final Graphics g) {
		final double rad1 = -(orientation * 2 * Math.PI) / 360.0 + Math.PI;
		g.setColor(0x669999);
		final int cx = px + width / 2;
		final int cy = py + height / 2;
		final double rad2 = rad1 + 2 * Math.PI / 3;
		final double rad3 = rad1 - 2 * Math.PI / 3;
		final int r1 = 9;
		final int r2 = 5;
		final int p1x = (int) (cx + Math.sin(rad1) * r1);
		final int p1y = (int) (cy + Math.cos(rad1) * r1);
		final int p2x = (int) (cx + Math.sin(rad2) * r2);
		final int p2y = (int) (cy + Math.cos(rad2) * r2);
		final int p3x = (int) (cx + Math.sin(rad3) * r2);
		final int p3y = (int) (cy + Math.cos(rad3) * r2);
		g.fillTriangle(p1x, p1y, p2x, p2y, p3x, p3y);
		g.setColor(0x992222);
		g.drawLine(cx, cy, p1x, p1y);
	}

	public void setOrientationWidth() {
		setWidth(12 + 2 * TEXT_PADDING_W);
	}

	private int getGrey(final int i) {
		final int g = 0xff - (i << 3);
		return (g << 16) | (g << 8) | g;
	}

	/**
	 * Draw background.
	 * 
	 * @param g
	 *            graphics object
	 */
	private void paintBackground(final Graphics g) {
		final int sideMargins = 3;
		g.setColor(getGrey(2));
		g.fillRect(px, py, sideMargins, height);
		g.fillRect(px + width - sideMargins, py, sideMargins, height);
		final int nLevels = 3;
		int ly = py;
		final int heightBand = height / nLevels;
		for (int i = 3; i < nLevels + 3; i++) {
			g.setColor(getGrey(i));
			g.fillRect(px + sideMargins, ly, width - (sideMargins * 2), heightBand);
			ly += heightBand;
		}
	}

	/**
	 * Change position.
	 * 
	 * @param x
	 *            x
	 * @param y
	 *            y
	 */
	public void setPosition(final int x, final int y) {
		px = x;
		py = y;
	}

	/**
	 * @param w
	 *            w
	 * @param h
	 *            h
	 */
	public void setSize(final int w, final int h) {
		width = w;
		height = h;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bbtracker.mobile.gui.Widget#getX()
	 */
	public int getX() {
		return px;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bbtracker.mobile.gui.Widget#getY()
	 */
	public int getY() {
		return py;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bbtracker.mobile.gui.Widget#getHeight()
	 */
	public int getHeight() {
		return height;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bbtracker.mobile.gui.Widget#getWidth()
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Compute minimum width for a String.
	 * 
	 * @param t
	 *            string
	 * 
	 * @return width
	 */
	public int getTextMinimumWidth(final String t) {
		return font.stringWidth(t);
	}

	/**
	 * Set width to fit given text.
	 * 
	 * @param t
	 *            text to fit in item
	 */
	public void setDimensionsForString(final String t) {
		setWidth(getTextMinimumWidth(t) + TEXT_PADDING_W * 2);
		setHeight(font.getHeight() + TEXT_PADDING_H * 2);
	}

	/**
	 * @param x
	 *            position
	 */
	public void setX(final int x) {
		px = x;
	}

	/**
	 * @param y
	 *            position
	 */
	public void setY(final int y) {
		py = y;
	}

	/**
	 * @return the font
	 */
	public Font getFont() {
		return font;
	}

	/**
	 * @param f
	 *            the font to set
	 */
	public void setFont(final Font f) {
		font = f;
	}

	/**
	 * @param w
	 *            width
	 */
	public void setWidth(final int w) {
		width = w;
	}

	/**
	 * @param h
	 *            height
	 */
	public void setHeight(final int h) {
		height = h;
	}

	/**
	 * @param orientation
	 *            the orientation to set
	 */
	public void setOrientation(final int orientation) {
		this.orientation = orientation;
	}
}
