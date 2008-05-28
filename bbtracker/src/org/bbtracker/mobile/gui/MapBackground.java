/*
 * Copyright 2008 Sebastien Chauvin
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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import org.bbtracker.MathUtil;
import org.bbtracker.mobile.CompactStream;
import org.bbtracker.mobile.Log;
import org.bbtracker.mobile.config.ConfigFile;

/**
 * Draw a map background using regular png tiles. The configuration file used
 * specifies the following: baseFileName France8000001 base name for tile files
 * directory base directory (when not using tar file) tarFileName container file
 * (when using tar file) maxTileY 6400 total number of pixels available maxTileX
 * 5888 total number of pixels available latDiff 11.822276 latitude difference
 * between top and bottom longDiff 16.171856 longitude difference between top
 * and bottom mapOffsetLong -5.624991 longitude of the left of the map
 * mapOffsetLat 53.6615172 latitude of the top of the map
 */
public final class MapBackground implements Runnable {
	/** Number of degree in a circle. */
	private static final int CIRCLE_DEGREE = 360;

	/** If set, 1048577 is rounded to 1048576. (1% difference max) */
	private static final boolean FIND_NEAREST_GOOGLE_SCALE = true;

	/** Text position within progress bar. */
	private static final int PROGRESS_TEXT_POSITION_Y = 3;

	/** Text position within progress bar. */
	private static final int PROGRESS_TEXT_POSITION_X = 3;

	/** Color for progress bar text. */
	private static final int PROGRESS_TEXT_COLOR = 0xffffff;

	/** Color for progress bar active part. */
	private static final int PROGRESS_DONE_COLOR = 0xaaaaaa;

	/** Color for progress bar background. */
	private static final int PROGRESS_BACKGROUND_COLOR = 0xcccccc;

	/** Color for position rules. */
	private static final int POSITION_COLOR = 0x00ff00;

	/** Size of tiles. */
	private static final int TILE_DEFAULT_SIZE = 256;

	/** Progress bar dimension. */
	private static final int PROGRESS_BAR_HEIGHT = 30;

	/** Progress bar dimension. */
	private static final int PROGRESS_BAR_WIDTH = 200;

	/** Number of tiles cached. */
	private static final int MAX_TILES_CACHED = 10;

	/** Constant for loading progress state. */
	private static final int STATE_INDEX = 0;

	/** Constant for loading progress state. */
	private static final int STATE_OPENING = 1;

	/** Constant for loading progress state. */
	private static final int STATE_READING = 2;

	/** Constant for loading progress state. */
	private static final int STATE_DONE = 3;

	/** Constant for loading progress state. */
	private static final int STATE_ERROR = 4;

	/** Filename. */
	private static final String FILENAME_SUFFIX = ".png";

	/**
	 * If set, use input stream decorator to send end of stream when end of
	 * image stream is reached within the tar file.
	 */
	private static boolean s_useCompactStream = true;

	/** Tile width in pixels. */
	private int tileWidth;

	/** Tile height in pixels. */
	private int tileHeight;

	/** Hashtable of tileNumber = Tile. */
	private final Hashtable tileCache = new Hashtable();

	/** Mutex to notify missing images. */
	private final Object tileLoadMutex = new Object();

	/**
	 * Vector of Tile. Changes to this variable are only permitted when
	 * synchronized under tileLoadMutex.
	 */
	private volatile Vector tileLoadQueue = new Vector();

	/** basefilename for image files. */
	private String baseFileName;

	/** Scale to apply to the map. */
	private double scaleX;

	/** Scale to apply to the map. */
	private double scaleY;

	/** Offset of the map in longitude. */
	private double mapOffsetLong;

	/** Offset of the map in latitude. */
	private double mapOffsetLat;

	/** Number of pixels available in X. */
	private int maxTileX;

	/** Number of pixels available in Y. */
	private int maxTileY;

	/** Difference in longitude (used to compute scaleX). */
	private double longDiff;

	/** Difference in latitude (used to compute scaleY). */
	private double latDiff;

	/** Maincanvas (used to trigger repaints after map tile loading done. */
	private MainCanvas mainCanvas;

	/** Flag to stop load thread. */
	private volatile boolean stopLoadThread = false;

	/** Directory holding map tiles (when tar file is not used). */
	private String directory;

	/** Filename to the map description file. */
	private String mapDescriptionUrl;

	/** Index in tar file. */
	private int[] tarFileIndex;

	/** Index in tar file. */
	private int[] tarFileSize;

	/** tar file kept open. */
	private FileConnection tarFile;

	/** tar file kept open. */
	private InputStream tarFileInputStream;

	/** name of the tar file. */
	private String tarFileName;

	/** directory holding tar file. */
	private String tarFileDirectory;

	/** Loading progress state (see STATE_*). */
	private volatile int queueState;

	/** Value of progress state. */
	private volatile int queueProgress;

	/** Value of progress state. */
	private volatile int queueTotal;

	/** Tile meta information and image files. */
	final class Tile {
		/** File number. */
		private final int fileNumber;

		/** Image (or null if not yet loaded). */
		private Image image;

		/** lastUse time (for flushing). */
		private long lastUse;

		/**
		 * Construct.
		 * 
		 * @param index
		 *            tile index
		 */
		private Tile(final int index) {
			fileNumber = index;
		}

		/** @return Unique identifier */
		public int hashCode() {
			return fileNumber + 1;
		}

		/**
		 * Compare with a tile or an Integer holding a tile index.
		 * 
		 * @param object
		 *            Tile or Integer
		 * @return true if index is similar
		 */
		public boolean equals(final Object object) {
			if (object instanceof Tile) {
				final Tile tile = (Tile) object;
				return tile.fileNumber == fileNumber;
			} else if (object instanceof Integer) {
				final Integer index = (Integer) object;
				return index != null && index.intValue() == fileNumber;
			} else {
				return false;
			}
		}

		/**
		 * Compare LTU.
		 * 
		 * @param tile
		 *            tile
		 * @return true if this is older than tile param
		 */
		public boolean older(final Tile tile) {
			return lastUse < tile.lastUse;
		}

		/**
		 * @return Filename of the tile.
		 */
		public String getFileName() {
			final int nYTiles = maxTileY / tileHeight;
			final int y = (fileNumber % nYTiles) * tileHeight;
			final int x = (fileNumber / nYTiles) * tileWidth;

			final StringBuffer filenameBuffer = new StringBuffer();
			filenameBuffer.append(directory);
			filenameBuffer.append(baseFileName);
			filenameBuffer.append('_');
			filenameBuffer.append(x);
			filenameBuffer.append('_');
			filenameBuffer.append(y);
			filenameBuffer.append(FILENAME_SUFFIX);

			return filenameBuffer.toString();
		}
	}

	/** Must use create static method. */
	private MapBackground() {
	}

	/**
	 * Create a mapbackground object.
	 * 
	 * @param baseDir
	 *            directory to read from
	 * @param descriptor
	 *            map descriptor
	 * @return map background object
	 * @throws IOException
	 *             io error
	 */
	public static MapBackground create(final String baseDir, final String descriptor) throws IOException {
		final MapBackground self = new MapBackground();

		final String url = "file:///" + descriptor;
		self.mapDescriptionUrl = url;
		final ConfigFile config = ConfigFile.openConfig(url);

		self.directory = config.get("directory");
		self.baseFileName = config.get("baseFileName");
		self.tarFileDirectory = baseDir;
		self.tarFileName = config.get("tarFileName");
		self.longDiff = config.getDouble("longDiff", Double.NaN);
		self.latDiff = config.getDouble("latDiff", Double.NaN);
		self.maxTileX = config.getInteger("maxTileX", 0);
		self.maxTileY = config.getInteger("maxTileY", 0);
		self.tileWidth = config.getInteger("tileWidth", TILE_DEFAULT_SIZE);
		self.tileHeight = config.getInteger("tileHeight", TILE_DEFAULT_SIZE);
		self.scaleX = (self.maxTileX / self.longDiff) * CIRCLE_DEGREE;
		if (FIND_NEAREST_GOOGLE_SCALE) {
			final int maxZoomLevel = 20;
			final int percent = 100;
			for (int i = 0; i < maxZoomLevel; i++) {
				final double googleScale = (1 << i) * self.tileWidth;
				if (Math.abs(self.scaleX - googleScale) < googleScale / percent) {
					self.scaleX = googleScale;
					break;
				}
			}
		}
		self.scaleY = self.scaleX;
		self.mapOffsetLong = config.getDouble("mapOffsetLong", 0);
		self.mapOffsetLat = config.getDouble("mapOffsetLat", 0);

		return self;
	}

	/** @return map scale. */
	public double getScaleX() {
		return (1 / scaleX) * CIRCLE_DEGREE;
	}

	/**
	 * @param latitude
	 *            latitude at which the scale is
	 * @return map scale.
	 */
	public double getScaleY(final double latitude) {
		final double latN1 = toNormalisedPixelLat(latitude);
		final double latN2 = toNormalisedPixelLat(latitude + 1);
		return (1 / scaleY) / (latN1 - latN2);
	}

	/**
	 * @param g
	 *            graphic object to paint with
	 * @param longitude
	 *            top left coordinate
	 * @param latitude
	 *            top left coordinate
	 * @param centerOffsetX
	 *            offset of the center of the screen in pixels
	 * @param centerOffsetY
	 *            offset of the center of the screen in pixels
	 */
	public void paint(final Graphics g, final double longitude, final double latitude, final int centerOffsetX,
			final int centerOffsetY) {
		boolean missingImage;
		final int clipWidth = g.getClipWidth();
		final int clipHeight = g.getClipHeight();
		final int clipX = g.getClipX();
		final int clipY = g.getClipY();
		if (clipWidth != PROGRESS_BAR_WIDTH && clipHeight != PROGRESS_BAR_HEIGHT) {
			missingImage = paintMap(g, longitude, latitude, centerOffsetX, centerOffsetY);
		} else {
			missingImage = true;
		}
		if (missingImage) {
			paintLoadingProgress(g);
		}

		if (false) {
			paintMemoryStatus(g);
		}

		g.setColor(POSITION_COLOR);
		g.drawLine(clipX, centerOffsetY, clipWidth, centerOffsetY);
		g.drawLine(centerOffsetX, clipY, centerOffsetX, clipHeight);
	}

	/**
	 * Paint map tiles.
	 * 
	 * @param g
	 *            graphic object to paint with
	 * @param longitude
	 *            top left coordinate
	 * @param latitude
	 *            top left coordinate
	 * @param centerOffsetX
	 *            offset of the center of the screen in pixels
	 * @param centerOffsetY
	 *            offset of the center of the screen in pixels
	 * @return true if a tile is missing
	 */
	private boolean paintMap(final Graphics g, double longitude, double latitude, final int centerOffsetX,
			final int centerOffsetY) {
		final int width = g.getClipWidth();
		final int height = g.getClipHeight();
		int offX = (int) ((toNormalisedPixelLng(-longitude) + toNormalisedPixelLng(mapOffsetLong)) * scaleX)
				+ centerOffsetX;
		final double normalisedPixelLat = toNormalisedPixelLat(-latitude);
		final double normalisedPixelLatOffset = toNormalisedPixelLat(-mapOffsetLat);
		int offY = (int) ((normalisedPixelLat - normalisedPixelLatOffset) * scaleY) + centerOffsetY;

		final Vector missingImages = new Vector();

		final long time = System.currentTimeMillis();

		final int firstX = -offX / tileWidth;
		final int firstY = -offY / tileHeight;

		final int halfWidth = width >> 1;
		final int halfHeight = height >> 1;
		for (int x = firstX; offX + x * tileWidth < width; x++) {
			for (int y = firstY; offY + y * tileHeight < height; y++) {
				final int px = offX + x * tileWidth;
				final int py = offY + y * tileHeight;
				final boolean center = px < halfWidth && px + tileWidth > halfWidth && py < halfHeight
						&& py + tileHeight > halfHeight;
				paintMapTile(g, missingImages, time, x, px, y, py, center);
			}
		}
		synchronized (tileLoadMutex) {
			tileLoadQueue = missingImages;
			tileLoadMutex.notify();
		}
		return !missingImages.isEmpty();
	}

	private void paintMapTile(final Graphics g, final Vector missingImages, final long time, final int x,
			final int startX, final int y, final int startY, final boolean center) {
		if (x < maxTileX / tileWidth && y < maxTileY / tileHeight) {
			final int fileNumber = x * (maxTileY / tileHeight) + y;
			final Image img = getTileImage(fileNumber);
			if (img != null) {
				g.drawImage(img, startX, startY, Graphics.TOP | Graphics.LEFT);
			} else {
				final Tile newTile = new Tile(fileNumber);
				newTile.lastUse = time;
				if (center) {
					missingImages.insertElementAt(newTile, 0);
				} else {
					missingImages.addElement(newTile);
				}
			}
		}
	}

	/**
	 * Repaint progress status only.
	 */
	private void repaintQueueState() {
		repaint(0, 0, PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT);
	}

	/**
	 * Paint memory status.
	 * 
	 * @param g
	 *            graphic object
	 */
	private static void paintMemoryStatus(final Graphics g) {
		g.setColor(PROGRESS_BACKGROUND_COLOR);

		final long totalMemory = Runtime.getRuntime().totalMemory();
		final long freeMemory = Runtime.getRuntime().freeMemory();
		final int barX = (int) (PROGRESS_BAR_WIDTH * freeMemory / totalMemory);
		g.fillRect(0, PROGRESS_BAR_HEIGHT, barX, PROGRESS_BAR_HEIGHT);
		g.setColor(PROGRESS_DONE_COLOR);
		g.fillRect(barX, PROGRESS_BAR_HEIGHT, PROGRESS_BAR_WIDTH - barX, PROGRESS_BAR_HEIGHT);
		g.setColor(PROGRESS_TEXT_COLOR);
		final StringBuffer state = new StringBuffer();
		state.append(freeMemory);
		state.append('/');
		state.append(totalMemory);
		g.drawString(state.toString(), PROGRESS_TEXT_POSITION_X, PROGRESS_TEXT_POSITION_Y + PROGRESS_BAR_HEIGHT,
				Graphics.TOP | Graphics.LEFT);
	}

	/**
	 * Paint progress bar.
	 * 
	 * @param g
	 *            graphic object
	 */
	private void paintLoadingProgress(final Graphics g) {
		g.setColor(PROGRESS_BACKGROUND_COLOR);
		final int progressX;
		if (queueProgress >= 0 && queueTotal > 0) {
			progressX = PROGRESS_BAR_WIDTH * queueProgress / queueTotal;
		} else {
			progressX = PROGRESS_BAR_WIDTH;
		}
		g.fillRect(0, 0, progressX, PROGRESS_BAR_HEIGHT);
		g.setColor(PROGRESS_DONE_COLOR);
		g.fillRect(progressX, 0, PROGRESS_BAR_WIDTH - progressX, PROGRESS_BAR_HEIGHT);
		g.setColor(PROGRESS_TEXT_COLOR);
		final StringBuffer state = new StringBuffer();
		final String stateString;
		switch (queueState) {
		case STATE_INDEX:
			stateString = "Index";
			break;
		case STATE_OPENING:
			stateString = "Opening";
			break;
		case STATE_READING:
			stateString = "Reading";
			break;
		case STATE_DONE:
			stateString = "Done";
			break;
		case STATE_ERROR:
		default:
			stateString = "Error";
			break;
		}
		state.append(stateString);
		state.append(' ');
		if (queueProgress != -1) {
			state.append(queueProgress);
			state.append('/');
			state.append(queueTotal);
		}
		g
				.drawString(state.toString(), PROGRESS_TEXT_POSITION_X, PROGRESS_TEXT_POSITION_Y, Graphics.TOP
						| Graphics.LEFT);
	}

	/**
	 * Tile loading loop.
	 */
	public void run() {
		while (!stopLoadThread) {
			queueState = STATE_DONE;
			queueProgress = -1;
			final Tile tile = getNextTileInLoadQueue();
			if (tile != null && tile.image == null && !tileCache.containsKey(new Integer(tile.fileNumber))) {
				readTile(tile);
			}
		}
	}

	/** Show tile list in the load queue. */
	public void dumpLoadQueue() {
		final Enumeration e = tileLoadQueue.elements();
		final StringBuffer str = new StringBuffer();
		while (e.hasMoreElements()) {
			final int n = ((Tile) e.nextElement()).fileNumber;
			str.append(n);
			if (e.hasMoreElements()) {
				str.append(',');
			}
		}
		Log.log(this, "LoadQueue: " + str.toString());
	}

	/** Show tile list in the cache. */
	public void dumpCache() {
		final Enumeration e = tileCache.keys();
		final StringBuffer str = new StringBuffer();
		while (e.hasMoreElements()) {
			final int n = ((Integer) e.nextElement()).intValue();
			str.append(n);
			if (e.hasMoreElements()) {
				str.append(',');
			}
		}
		Log.log(this, "Cache   : " + str.toString());
	}

	/**
	 * Read tile.
	 * 
	 * @param tile
	 *            to read
	 */
	private synchronized void readTile(final Tile tile) {
		FileConnection connection = null;
		InputStream in = null;
		try {
			queueState = STATE_OPENING;
			queueTotal = Math.min(tileCache.size() + tileLoadQueue.size(), MAX_TILES_CACHED);
			queueProgress = queueTotal - tileLoadQueue.size() - 1;
			if (tarFileName == null) {
				final String filename = tile.getFileName();
				connection = (FileConnection) Connector.open("file:///" + filename, Connector.READ);
				in = connection.openInputStream();
			} else {
				in = getTarInput(tile.fileNumber);
			}
			queueState = STATE_READING;
			repaintQueueState();

			if (in != null) {
				tile.image = Image.createImage(in);
				repaint();
			}
		} catch (final Throwable e) {
			queueState = STATE_ERROR;
			Log.log(this, e, "loading Map Tile " + tile.fileNumber);
		} finally {
			tileCache.put(new Integer(tile.fileNumber), tile);
			while (tileCache.size() > MAX_TILES_CACHED) {
				removeOldest();
			}
			if (tarFileName == null) {
				try {
					if (in != null) {
						in.close();
					}
				} catch (final IOException e) {
					Log.log(this, e, "closing Map Tile InStream " + tile.fileNumber);
				}
				try {
					if (connection != null) {
						connection.close();
					}
				} catch (final IOException e) {
					Log.log(this, e, "closing Map Tile " + tile.fileNumber);
				}
			} else {
				if (in != null && !in.markSupported()) {
					closeTarInputStream();
				}
			}
		}
	}

	/**
	 * Open tar file if necessary and skip to the index.
	 * 
	 * @param fileNumber
	 *            tile index
	 * @return input stream or null if error
	 */
	private synchronized InputStream getTarInput(final int fileNumber) {
		InputStream is;

		is = tarFileInputStream;
		if (is == null) {
			try {
				is = openTarFileAndReadIndex();
				tarFileInputStream = is;
			} catch (final Exception e) {
				Log.log(this, e, "opening Tar Tile " + tarFileName);
				if (is != null) {
					try {
						is.close();
					} catch (final IOException e1) {
						// ignored
						Log.log(this, e, "closing Tar Tile after error");
					}
				}
				is = null;
			}
		}

		if (is != null && tarFileIndex != null && tarFileSize[fileNumber] != 0) {
			try {
				if (is.markSupported()) {
					is.reset();
				}
				is.skip(tarFileIndex[fileNumber]);
			} catch (final IOException e) {
				Log.log(this, e, "skipping in Tar Tile " + tarFileName);
				closeTarInputStream();
				is = null;
			}
		} else {
			is = null;
		}

		if (is != null) {
			if (s_useCompactStream) {
				return new CompactStream(is, tarFileSize[fileNumber]);
			} else {
				return is;
			}
		} else {
			return null;
		}
	}

	/**
	 * Open tar file and read its index if not stored.
	 * 
	 * @return tar file input stream
	 * @throws IOException
	 *             io error
	 */
	private InputStream openTarFileAndReadIndex() throws IOException {
		InputStream is;
		is = openTarFileInputStream();
		final boolean markSupported = is.markSupported();
		if (markSupported) {
			is.mark(Integer.MAX_VALUE);
		}
		if (tarFileIndex == null) {
			readTarFileIndex();
			if (tarFileIndex == null) {
				readFileIndex(is);
				if (markSupported) {
					is.reset();
				} else {
					closeTarInputStream();
					is = openTarFileInputStream();
				}
				storeTarFileIndex();
			}
		}
		return is;
	}

	/**
	 * open tar file.
	 * 
	 * @return tar file input stream
	 * @throws IOException
	 *             io error
	 */
	private InputStream openTarFileInputStream() throws IOException {
		InputStream is;
		final FileConnection file = (FileConnection) Connector.open("file:///" + tarFileDirectory + tarFileName,
				Connector.READ);
		tarFile = file;
		is = file.openInputStream();
		return is;
	}

	/**
	 * Store tar file index.
	 */
	private void storeTarFileIndex() {
		final String url = getTarFileIndexUrl();
		try {
			final FileConnection file = (FileConnection) Connector.open(url, Connector.READ_WRITE);
			if (!file.exists()) {
				file.create();
			}
			try {
				final DataOutputStream out = file.openDataOutputStream();

				try {
					out.writeInt(tarFileIndex.length);
					for (int i = 0; i < tarFileIndex.length; i++) {
						out.writeInt(tarFileIndex[i]);
						out.writeInt(tarFileSize[i]);
					}
				} finally {
					out.close();
				}
			} finally {
				file.close();
			}
		} catch (final IOException e) {
			Log.log(this, e, "Storing tar index: " + url);
		}
	}

	private String getTarFileIndexUrl() {
		return "file:///" + tarFileDirectory + getTarIndexFileName();
	}

	/**
	 * @return tar file index (replace .tar by .idx)
	 */
	private String getTarIndexFileName() {
		final int pos = tarFileName.lastIndexOf('.');
		if (pos == -1) {
			throw new RuntimeException("Invalid tar filename");
		}
		return tarFileName.substring(0, pos + 1) + "idx";
	}

	/**
	 * open and read tar file index and file sizes.
	 */
	private void readTarFileIndex() {
		int[] tarIndex = null;
		int[] tarSize = null;
		final String url = getTarFileIndexUrl();
		try {
			final FileConnection file = (FileConnection) Connector.open(url, Connector.READ);
			final DataInputStream in = file.openDataInputStream();
			try {
				final int length = in.readInt();
				tarIndex = new int[length];
				tarSize = new int[length];
				for (int i = 0; i < length; i++) {
					tarIndex[i] = in.readInt();
					tarSize[i] = in.readInt();
				}
			} finally {
				in.close();
			}
		} catch (final IOException e) {
			Log.log(this, e, "reading tar file index " + url);
			tarIndex = null;
		}
		tarFileIndex = tarIndex;
		tarFileSize = tarSize;
	}

	/**
	 * Read Tar file index. See tar file format at:
	 * http://en.wikipedia.org/wiki/Tar_(file_format)
	 * 
	 * @param tarStream
	 *            tar stream
	 */
	private void readFileIndex(final InputStream tarStream) {
		queueState = STATE_INDEX;
		final int nElements = (maxTileX / tileWidth) * (maxTileY / tileHeight);
		queueTotal = nElements;
		final int[] fileIndex = new int[nElements];
		final int[] tarSize = new int[nElements];
		long fileOffset = 0;
		final int blockSize = 512;
		final int headerSize = blockSize;
		final long startTime = System.currentTimeMillis();
		long lastRepaint = 0;
		try {
			boolean interrupted = false;
			int imageFound = 0;
			final int headerFileNameSize = 100;
			final int headerFileSizeSize = 12;
			final int headerSkip = 24;
			final int headerBufferSize = headerFileNameSize + headerSkip + headerFileSizeSize;
			final byte[] header = new byte[headerBufferSize];
			for (;;) {
				fileOffset += headerSize;
				tarStream.read(header);
				final ByteArrayInputStream headerInputStream = new ByteArrayInputStream(header);
				final String name = readTarString(headerInputStream, headerFileNameSize);
				if (name == null || name.length() == 0) {
					break;
				}
				headerInputStream.skip(headerSkip);
				final int fileSize = readTarOctal(headerInputStream, headerFileSizeSize);
				final int padding;
				if (fileSize % blockSize > 0) {
					padding = (blockSize - (fileSize % blockSize));
				} else {
					padding = 0;
				}
				if (name.startsWith(baseFileName)) {
					final int fileNumber = parseFileNumber(name);
					fileIndex[fileNumber] = (int) fileOffset;
					tarSize[fileNumber] = fileSize;
					++imageFound;
					queueProgress = imageFound;
					final int repaintPeriod = 500;
					final long currentTimeMillis = System.currentTimeMillis();
					if (currentTimeMillis - lastRepaint > repaintPeriod) {
						lastRepaint = currentTimeMillis;
						repaintQueueState();
					}
				} else {
					Log.log(this, "skipping file in tar " + name);
				}
				// System.out.println("Read: " + name
				// + " [" + fileSize + "] @ " + fileOffset);
				fileOffset += fileSize + padding;
				final int skip = fileSize + padding + headerSize - headerBufferSize;
				tarStream.skip(skip);
				if (stopLoadThread) {
					interrupted = true;
					break;
				}
			}
			if (!interrupted) {
				tarFileIndex = fileIndex;
				tarFileSize = tarSize;
			}
		} catch (final IOException e) {
			Log.log(this, e, "reading tar index " + tarFileName);
		} catch (final NumberFormatException e) {
			Log.log(this, e, "Invalid index @" + fileOffset);
		}
		final int timeTaken = (int) (System.currentTimeMillis() - startTime);
		Log.log(this, "Read index file for " + tarFileName + "(markSupported: " + tarStream.markSupported() + ") in "
				+ timeTaken + " msec tarFileIndex: "
				+ (tarFileIndex == null ? "null" : Integer.toString(tarFileIndex.length)) + " stopped: "
				+ stopLoadThread);
	}

	/**
	 * Extract tile index from file number.
	 * 
	 * @param name
	 *            file name
	 * @return tile index
	 */
	private int parseFileNumber(final String name) {
		final int xOff = baseFileName.length() + 1;
		int xEnd = -1;
		for (int p = xOff; p < name.length(); p++) {
			if (!Character.isDigit(name.charAt(p))) {
				xEnd = p;
				break;
			}
		}
		final int yOff = xEnd + 1;
		int yEnd = -1;
		for (int p = yOff; p < name.length(); p++) {
			if (!Character.isDigit(name.charAt(p))) {
				yEnd = p;
				break;
			}
		}
		final int x = Integer.parseInt(name.substring(xOff, xEnd));
		final int y = Integer.parseInt(name.substring(yOff, yEnd));
		final int fileNumber = getFileNumber(x, y);
		return fileNumber;
	}

	/**
	 * Read a string from a tar file Stop at the first null character.
	 * 
	 * @param is
	 *            tar file input stream
	 * @param size
	 *            total size of the string.
	 * @return string
	 * @throws IOException
	 *             io error
	 */
	private String readTarString(final InputStream is, final int size) throws IOException {
		final byte[] string = new byte[size];
		int len = size;
		if (is.read(string) == -1) {
			return null;
		}
		for (int i = 0; i < size; i++) {
			if (string[i] == 0) {
				len = i;
				break;
			}
		}
		return new String(string, 0, len);
	}

	/**
	 * Read an octal number from a tar file. Convert null characters before
	 * calling parseInt
	 * 
	 * @param is
	 *            tar file input stream
	 * @param size
	 *            total size of the string.
	 * @return number
	 * @throws IOException
	 *             io error throws NumberFormatException number parsing error
	 */
	private int readTarOctal(final InputStream is, final int size) throws IOException {
		final byte[] string = new byte[size];
		is.read(string);
		for (int i = 0; i < size; i++) {
			if (string[i] == 0) {
				string[i] = ' ';
			}
		}
		final int octalBasis = 8;
		return Integer.parseInt(new String(string).trim(), octalBasis);
	}

	/**
	 * Flush oldest tile from cache.
	 */
	private void removeOldest() {
		final Enumeration elements = tileCache.elements();
		Tile oldest = (Tile) elements.nextElement();
		while (elements.hasMoreElements()) {
			final Tile tile = (Tile) elements.nextElement();
			if (tile.older(oldest)) {
				oldest = tile;
			}
		}
		tileCache.remove(new Integer(oldest.fileNumber));
	}

	/**
	 * Force repaint.
	 */
	private void repaint() {
		if (mainCanvas != null) {
			mainCanvas.repaint();
		}
	}

	/**
	 * Force partial repaint.
	 * 
	 * @param x
	 *            screen coordinate
	 * @param y
	 *            screen coordinate
	 * @param w
	 *            screen coordinate
	 * @param h
	 *            screen coordinate
	 */
	private void repaint(final int x, final int y, final int w, final int h) {
		if (mainCanvas != null) {
			mainCanvas.repaint(x, y, w, h);
		}
	}

	/**
	 * Convert coordinate into tile index.
	 * 
	 * @param x
	 *            x position
	 * @param y
	 *            y position
	 * @return tile index
	 */
	private int getFileNumber(final int x, final int y) {
		return x / tileWidth * (maxTileY / tileHeight) + y / tileHeight;
	}

	/**
	 * Get Tile image from cache. If not in cache, return null and schedule tile
	 * load.
	 * 
	 * @return tile image (or null if not in cache)
	 */
	private Image getTileImage(final int fileNumber) {
		final Tile tile = (Tile) tileCache.get(new Integer(fileNumber));
		final long time = System.currentTimeMillis();
		if (tile != null) {
			tile.lastUse = time;
			return tile.image;
		} else {
			return null;
		}
	}

	/**
	 * Blocking call.
	 * 
	 * @return next tile to load
	 */
	private Tile getNextTileInLoadQueue() {
		synchronized (tileLoadMutex) {
			if (tileLoadQueue.size() == 0) {
				try {
					tileLoadMutex.wait();
				} catch (final InterruptedException e) {
					ignoreException();
				}
			}
			if (tileLoadQueue.size() > 0) {
				final Tile tile = (Tile) tileLoadQueue.firstElement();
				tileLoadQueue.removeElementAt(0);
				return tile;
			} else {
				return null;
			}
		}
	}

	/** */
	private void ignoreException() {
	}

	/** Dump map background configuration. */
	public void dump() {
		Log.log(this, baseFileName + " scale: " + scaleX + "x" + scaleY + " offset:" + mapOffsetLong + " : "
				+ mapOffsetLat + " tiles: " + maxTileX + " : " + maxTileY);
	}

	/** Start loading thread. */
	public void start() {
		stopLoadThread = false;
		new Thread(this).start();
	}

	/** Stop loading thread and flush cache. */
	public void stop() {
		stopLoadThread = true;
		synchronized (tileLoadMutex) {
			tileLoadQueue = null;
			tileLoadMutex.notify();
		}
		tileCache.clear();
		synchronized (this) {
			closeTarInputStream();
		}
	}

	/**
	 * Close tar file input stream.
	 */
	private void closeTarInputStream() {
		try {
			if (tarFileInputStream != null) {
				tarFileInputStream.close();
			}
		} catch (final IOException e) {
			Log.log(this, e, "closing Tar InStream " + tarFileName);
		}
		tarFileInputStream = null;
		try {
			if (tarFile != null) {
				tarFile.close();
			}
		} catch (final IOException e) {
			Log.log(this, e, "closing Tar Tile " + tarFileName);
		}
		tarFile = null;
	}

	/**
	 * Inject main canvas (used for repaint).
	 * 
	 * @param canvas
	 *            canvas
	 */
	public void setMainCanvas(final MainCanvas canvas) {
		mainCanvas = canvas;
	}

	/**
	 * Change map offset graphically.
	 * 
	 * @param dx
	 *            map delta in pixels
	 * @param dy
	 *            map delta in pixels
	 */
	public void adjustMapPosition(final int dx, final int dy) {
		mapOffsetLong -= dx * (CIRCLE_DEGREE / scaleX);
		mapOffsetLat += dy * (CIRCLE_DEGREE / scaleY);
		repaint();
	}

	/**
	 * Save current configuration back to config file.
	 */
	public void saveConfiguration() {
		final ConfigFile config = ConfigFile.createEmtpyConfig();

		if (directory != null) {
			config.put("directory", directory);
		}
		if (tarFileName != null) {
			config.put("tarFileName", tarFileName);
		}
		config.put("baseFileName", baseFileName);
		config.put("longDiff", longDiff);
		config.put("latDiff", latDiff);
		config.put("maxTileX", maxTileX);
		config.put("maxTileY", maxTileY);
		config.put("tileWidth", tileWidth);
		config.put("tileHeight", tileHeight);
		config.put("mapOffsetLong", mapOffsetLong);
		config.put("mapOffsetLat", mapOffsetLat);

		try {
			config.saveConfig(mapDescriptionUrl);
		} catch (final IOException e) {
			Log.log(this, "saving back config file" + e);
		}
	}

	/**
	 * @param lng
	 *            longitude in degree
	 * @return normalized longitude
	 */
	private static double toNormalisedPixelLng(final double lng) {
		double normLng = lng;
		if (lng > CIRCLE_DEGREE / 2) {
			normLng -= CIRCLE_DEGREE;
		} else {
			normLng = lng;
		}

		normLng /= CIRCLE_DEGREE;

		return normLng;
	}

	/**
	 * @param lat
	 *            latitude in degree
	 * @return normalized latitude
	 */
	private static double toNormalisedPixelLat(final double lat) {
		final double half = 0.5;
		final int four = 4;
		final double normalizedLat = -((MathUtil.log(Math.tan((Math.PI / four)
				+ ((half * Math.PI * lat) / (CIRCLE_DEGREE / 2)))) / Math.PI) / 2.0);

		return normalizedLat;
	}
}
