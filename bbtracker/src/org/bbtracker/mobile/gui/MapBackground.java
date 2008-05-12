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
 * Draw a map background using regular png tiles.
 * The configuration file used specifies the following:
 * baseFileName France8000001	base name for tile files
 * directory                    base directory (when not using tar file)
 * tarFileName                  container file (when using tar file)
 * maxTileY 6400 				total number of pixels available
 * maxTileX 5888 				total number of pixels available
 * latDiff 11.822276			latitude difference between top and bottom 
 * longDiff 16.171856			longitude difference between top and bottom
 * mapOffsetLong -5.624991		longitude of the left of the map
 * mapOffsetLat 53.6615172		latitude of the top of the map 
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
	private static final int STATE_READING = 1;
	
	/** Constant for loading progress state. */
	private static final int STATE_DONE = 2;
	
	/** Filename. */
	private static final String FILENAME_SUFFIX = ".png";
	
	/** Tile width in pixels. */
	private int tileWidth;

	/** Tile height in pixels. */
	private int tileHeight;
	
	/** Hashtable of tileNumber = Tile. */ 
	private Hashtable tileCache = new Hashtable();
	
	/** Vector of Tile. */
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
	private long[] tarFileIndex;

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
		private int fileNumber;
		
		/** Image (or null if not yet loaded). */
		private Image image;

		/** lastUse time (for flushing). */
		private long lastUse;
		
		/** Construct. 
		 * @param index tile index
		 * */
		private Tile(int index) {
			this.fileNumber = index;
		}

		/** @return Unique identifier */
		public int hashCode() {
			return fileNumber + 1;
		}
		
		/** 
		 * Compare with a tile or an Integer holding a tile index.
		 * @param object Tile or Integer 
		 * @return true if index is similar
		 */
		public boolean equals(Object object) {
			if (object instanceof Tile) {
				Tile tile = (Tile) object;
				return tile.fileNumber == this.fileNumber;
			} else if (object instanceof Integer) {
				Integer index = (Integer) object;
				return index != null 
					&& index.intValue() == this.fileNumber;
			} else {
				return false;
			}
		}
		
		/** 
		 * Compare LTU.
		 * @param tile tile
		 * @return true if this is older than tile param
		 */
		public boolean older(final Tile tile) {
			return lastUse < tile.lastUse;
		}
		/**
		 * @return Filename of the tile. 
		 */
		public String getFileName() {
			int nYTiles = maxTileY / tileHeight;
			int y = (fileNumber % nYTiles) * tileHeight;
			int x = (fileNumber / nYTiles) * tileWidth;
			
			StringBuffer filenameBuffer = new StringBuffer();
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
	 * @param baseDir    directory to read from
	 * @param descriptor map descriptor
	 * @return map background object
	 * @throws IOException io error
	 */
	public static MapBackground create(String baseDir, String descriptor) 
		throws IOException {
		MapBackground self = new MapBackground();
		
		String url = "file:///" + descriptor;
		self.mapDescriptionUrl = url;
		ConfigFile config = ConfigFile.openConfig(url);
		
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
				double googleScale = (1 << i) * self.tileWidth;
				if (Math.abs(self.scaleX - googleScale) 
						< googleScale / percent) {
					System.out.println("Using rounded google scale.");
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

	/** @param latitude latitude at which the scale is
	 * @return map scale. */
	public double getScaleY(double latitude) {
		double latN1 = toNormalisedPixelLat(latitude);
		double latN2 = toNormalisedPixelLat(latitude + 1);
		return (1 / scaleY) / (latN1 - latN2);
	}
	
	/**
	 * @param g graphic object to paint with
	 * @param longitude top left coordinate
	 * @param latitude  top left coordinate
	 * @param centerOffsetX offset of the center of the screen in pixels
	 * @param centerOffsetY offset of the center of the screen in pixels
	 */
	public void paint(Graphics g, double longitude, double latitude, 
			int centerOffsetX, int centerOffsetY) {
		boolean missingImage;
		int clipWidth = g.getClipWidth();
		int clipHeight = g.getClipHeight();
		int clipX = g.getClipX();
		int clipY = g.getClipY();
		if (clipWidth != PROGRESS_BAR_WIDTH 
				&& clipHeight != PROGRESS_BAR_HEIGHT) {
			missingImage = paintMap(g, longitude, latitude, centerOffsetX,
					centerOffsetY);
		} else {
			missingImage = true;
		}
		if (missingImage) {
			paintLoadingProgress(g);
		}
		
		g.setColor(POSITION_COLOR);
		g.drawLine(clipX, centerOffsetY, clipWidth, centerOffsetY);
		g.drawLine(centerOffsetX, clipY, centerOffsetX, clipHeight);
	}

	/**
	 * Paint map tiles.
	 * 
	 * @param g graphic object to paint with
	 * @param longitude top left coordinate
	 * @param latitude  top left coordinate
	 * @param centerOffsetX offset of the center of the screen in pixels
	 * @param centerOffsetY offset of the center of the screen in pixels
	 * @return true if a tile is missing
	 */
	private boolean paintMap(Graphics g, double longitude,
			double latitude, int centerOffsetX,
			int centerOffsetY) {
		final int width = g.getClipWidth();
		final int height = g.getClipHeight();
		int offX = 
			(int) ((toNormalisedPixelLng(-longitude) 
					+ toNormalisedPixelLng(mapOffsetLong)) * scaleX) 
			+ centerOffsetX;
		double normalisedPixelLat = toNormalisedPixelLat(-latitude);
		double normalisedPixelLatOffset = toNormalisedPixelLat(-mapOffsetLat);
		int offY = 
			(int) ((normalisedPixelLat - normalisedPixelLatOffset) * scaleY) 
			+ centerOffsetY;
		System.out.println("Offset: " + offX + "," + offY 
				+ "max: " + maxTileX + ", " + maxTileY);
		boolean missingImage = false;

		for (int x = 0; x < maxTileX; x += tileWidth) {
			int startX = offX + x;
			if (startX < width && startX + tileWidth > 0) { 
				for (int y = 0; y < maxTileY; y += tileHeight) {
					int startY = offY + y;
					if (startY < height && startY + tileHeight > 0) {
						System.out.println("Found tile.");
						Image img = getTileImage(x, y);
						if (img != null) {
							g.drawImage(img, startX, startY, 
									Graphics.TOP | Graphics.LEFT);
						} else {
							missingImage = true;
						}
					}
				}
			}
		}
		return missingImage;
	}

	/**
	 * Repaint progress status only.
	 */
	private void repaintQueueState() {
		repaint(0, 0, PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT);
	}
	
	/**
	 * Paint progress bar.
	 * 
	 * @param g graphic object
	 */
	private void paintLoadingProgress(Graphics g) {
		g.setColor(PROGRESS_BACKGROUND_COLOR);
		final int progressX; 
		if (queueProgress >= 0 && queueTotal > 0) {
			progressX = PROGRESS_BAR_WIDTH * queueProgress / queueTotal;
		} else {
			progressX = PROGRESS_BAR_WIDTH;
		}
		g.fillRect(0, 0, progressX, PROGRESS_BAR_HEIGHT);
		g.setColor(PROGRESS_DONE_COLOR);
		g.fillRect(progressX, 0, 
				PROGRESS_BAR_WIDTH - progressX, PROGRESS_BAR_HEIGHT);
		g.setColor(PROGRESS_TEXT_COLOR);
		StringBuffer state = new StringBuffer();
		switch (queueState) {
		case STATE_INDEX:
			state.append("Index");
			break;
		case STATE_READING:
			state.append("Reading");
			break;
		default:
			state.append("Error");
			break;
		}
		state.append(' ');
		if (queueProgress != -1) {
			state.append(queueProgress);
			state.append('/');
			state.append(queueTotal);
		}
		g.drawString(state.toString(), 
				PROGRESS_TEXT_POSITION_X, PROGRESS_TEXT_POSITION_Y, 
				Graphics.TOP | Graphics.LEFT);
	}

	/**
	 * Tile loading loop.
	 */
	public void run() {
		while (!stopLoadThread) {
			queueState = STATE_DONE;
			queueProgress = -1;
			Tile tile = getNextTileInLoadQueue();
			if (tile != null && tile.image == null 
					&& !tileCache.containsKey(new Integer(tile.fileNumber))) {
				readTile(tile);
			}
		}
	}

	/**
	 * Read tile.
	 * 
	 * @param tile to read
	 */
	private synchronized void readTile(final Tile tile) {
		FileConnection connection = null;
		InputStream in = null;
		try {
			if (tarFileName == null) {
				final String filename = tile.getFileName();
				connection = 
					(FileConnection) Connector.open("file:///" + filename, 
							Connector.READ);
				in = connection.openInputStream();
			} else {
				in = getTarInput(tile.fileNumber);
			}
			queueState = STATE_READING;
			queueTotal = tileCache.size();
			queueProgress = queueTotal - tileLoadQueue.size();
			
			tile.image = Image.createImage(in);
			repaint();
		} catch (Throwable e) {
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
				} catch (IOException e) {
					Log.log(this, e, 
							"closing Map Tile InStream " + tile.fileNumber);
				}
				try {
					if (connection != null) {
						connection.close();
					}
				} catch (IOException e) {
					Log.log(this, e, "closing Map Tile " + tile.fileNumber);
				}
			} else {
				if (!in.markSupported()) {
					closeTarInputStream();
				}
			}
		}
	}

	/**
	 * open tar file if necessary and skip to the index.
	 * 
	 * @param fileNumber tile index
	 * @return input stream
	 */
	private synchronized InputStream getTarInput(final int fileNumber) {
		InputStream is = this.tarFileInputStream;
		boolean markSupported = false;
		if (is == null) {
			try {
				is = openTarFileInputStream();
				markSupported = is.markSupported();
				if (markSupported) {
					is.mark(Integer.MAX_VALUE);
				}
				if (tarFileIndex == null) {
					readFileIndex(is);
					if (markSupported) {
						is.reset();
					} else {
						closeTarInputStream();
						is = openTarFileInputStream();
					}
				}
				this.tarFileInputStream = is;
			} catch (Exception e) {
				Log.log(this, e, "opening Tar Tile " + tarFileName);
				if (is != null) {
					try {
						is.close();
					} catch (IOException e1) {
						// ignored
						Log.log(this, e, "closing Tar Tile after error");
					}
				}
				is = null;
			}
		}

		if (is != null) {
			try {
				if (markSupported) {
					is.reset();
				}
				is.skip(tarFileIndex[fileNumber]);
			} catch (IOException e) {
				Log.log(this, e, "skipping in Tar Tile " + tarFileName);
				closeTarInputStream();
				is = null;
			}
		}

		if (is != null) {
			return new CompactStream(is, tarFileSize[fileNumber]);
		} else {
			return null;
		}
	}

	/**
	 * open tar file.
	 * 
	 * @return tar file input stream
	 * @throws IOException io error
	 */
	private InputStream openTarFileInputStream() throws IOException {
		InputStream is;
		FileConnection file = 
			(FileConnection) Connector.open(
					"file:///" + tarFileDirectory + tarFileName, 
					Connector.READ);
		this.tarFile = file;
		is = file.openInputStream();
		return is;
	}
	
	/**
	 * Read Tar file index.
	 * See tar file format at:  http://en.wikipedia.org/wiki/Tar_(file_format)
	 * 
	 * @param tarStream tar stream
	 */
	private void readFileIndex(InputStream tarStream) {
		queueState = STATE_INDEX;
		final int nElements = (maxTileX / tileWidth) * (maxTileY / tileHeight);
		queueTotal = nElements;
		final long[] fileIndex = new long[nElements];
		final int[] tarSize    = new int[nElements];
		long fileOffset = 0;
		final int blockSize = 512;
		final int headerSize = blockSize;
		final long startTime = System.currentTimeMillis();
		long lastRepaint = 0;
		try {
			boolean interrupted = false;
			int imageFound = 0;
			for (;;) {
				fileOffset += headerSize;
				final int headerFileNameSize = 100;
				final int headerFileSizeSize = 12;
				final int headerSkip = 24;
				final int headerBufferSize = headerFileNameSize 
					+ headerSkip + headerFileSizeSize;
				byte[] header = new byte[headerBufferSize];
				tarStream.read(header);
				ByteArrayInputStream headerInputStream = 
					new ByteArrayInputStream(header);
				String name = readTarString(headerInputStream, 
						headerFileNameSize);
				if (name == null || name.length() == 0) {
					break;
				}
				headerInputStream.skip(headerSkip);
				int fileSize = readTarOctal(headerInputStream, 
						headerFileSizeSize);
				final int padding;
				if (fileSize % blockSize > 0) {
					padding =  (blockSize - (fileSize % blockSize));
				} else {
					padding = 0;
				}
				if (name.startsWith(baseFileName)) {
					final int fileNumber = parseFileNumber(name);
					fileIndex[fileNumber] = fileOffset;
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
//				System.out.println("Read: " + name 
//				+ " [" + fileSize + "]  @ " + fileOffset);
				fileOffset += fileSize + padding;
				int skip = fileSize + padding 
							+ headerSize - headerBufferSize;
				tarStream.skip(skip);
				if (stopLoadThread) {
					interrupted = true;
					break;
				}
			}
			if (!interrupted) {
				this.tarFileIndex = fileIndex;
				this.tarFileSize = tarSize;
			}
		} catch (IOException e) {
			Log.log(this, e, "reading tar index " + tarFileName);
		} catch (NumberFormatException e) {
			Log.log(this, e, "Invalid index @" + fileOffset);
		}
		int timeTaken = (int) (System.currentTimeMillis() - startTime);
		Log.log(this, "Read index file for " + tarFileName 
				+ "(markSupported: " + tarStream.markSupported() 
				+ ") in " + timeTaken 
				+ " stopped: " + stopLoadThread);
	}

	/**
	 * Extract tile index from file number.
	 * 
	 * @param name file name
	 * @return tile index
	 */
	private int parseFileNumber(String name) {
		final int xOff = baseFileName.length() + 1;
		int xEnd = -1;
		for (int p  = xOff; p < name.length(); p++) {
			if (!Character.isDigit(name.charAt(p))) {
				xEnd = p;
				break;
			}
		}
		final int yOff = xEnd + 1;
		int yEnd = -1;
		for (int p  = yOff; p < name.length(); p++) {
			if (!Character.isDigit(name.charAt(p))) {
				yEnd = p;
				break;
			}
		}
		int x = Integer.parseInt(name.substring(xOff, xEnd));
		int y = Integer.parseInt(name.substring(yOff, yEnd));
		int fileNumber = getFileNumber(x, y);
		return fileNumber;
	}
	
	/**
	 * Read a string from a tar file
	 * Stop at the first null character.
	 * 
	 * @param is   tar file input stream
	 * @param size total size of the string.
	 * @return string
	 * @throws IOException io error
	 */
	private String readTarString(InputStream is, int size) throws IOException {
		byte[] string = new byte[size];
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
	 * Read an octal number from a tar file.
	 * Convert null characters before calling parseInt
	 * 
	 * @param is   tar file input stream
	 * @param size total size of the string.
	 * @return number
	 * @throws IOException io error
	 * throws NumberFormatException number parsing error
	 */
	private int readTarOctal(final InputStream is, final int size) 
		throws IOException {
		byte[] string = new byte[size];
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
		Enumeration elements = tileCache.elements();
		Tile oldest = (Tile) elements.nextElement();
		while (elements.hasMoreElements()) {
			Tile tile = (Tile) elements.nextElement();
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
	 * @param x screen coordinate
	 * @param y screen coordinate
	 * @param w screen coordinate
	 * @param h screen coordinate
	 */
	private void repaint(int x, int y, int w, int h) {
		if (mainCanvas != null) {
			mainCanvas.repaint(x, y, w, h);
		}
	}
	
	/**
	 * Convert coordinate into tile index.
	 * 
	 * @param x x position
	 * @param y y position
	 * @return tile index
	 */
	private int getFileNumber(int x, int y) {
		return x / tileWidth * (maxTileY / tileHeight) + y / tileHeight;
	}
	
	/**
	 * Get Tile image from cache. If not in cache, return null and schedule
	 * tile load.
	 * 
	 * @param x x
	 * @param y y
	 * @return tile image (or null if not in cache)
	 */
	private Image getTileImage(int x, int y) {
		final int fileNumber = getFileNumber(x, y);
		Tile tile = (Tile) tileCache.get(new Integer(fileNumber));
		long time = System.currentTimeMillis();
		if (tile != null) {
			tile.lastUse = time;
			return tile.image;
		} else {
			synchronized (tileLoadQueue) {
				Tile newTile = new Tile(fileNumber);
				if (!tileLoadQueue.contains(newTile)) {
					newTile.lastUse = time;
					tileLoadQueue.addElement(newTile);
					tileLoadQueue.notify();
				}
			}
			return null;
		}
	}
	
	/**
	 * Blocking call.
	 * 
	 * @return next tile to load
	 */
	private Tile getNextTileInLoadQueue() {
		synchronized (tileLoadQueue) {
			if (tileLoadQueue.size() == 0) {
				try {
					tileLoadQueue.wait();
				} catch (InterruptedException e) {
					ignoreException();
				}
			}
			if (tileLoadQueue.size() > 0) {
				Tile tile = (Tile) tileLoadQueue.firstElement();
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
		Log.log(this, baseFileName + " scale: " + scaleX + "x" + scaleY
				+ " offset:" + mapOffsetLong + " : " + mapOffsetLat 
				+ " tiles: " + maxTileX + " : " + maxTileY);
	}


	/** Start loading thread. */
	public void start() {
		stopLoadThread = false;
		new Thread(this).start();
	}
	
	/** Stop loading thread and flush cache. */
	public void stop() {
		stopLoadThread = true;
		synchronized (tileLoadQueue) {
			tileLoadQueue.removeAllElements();
			tileLoadQueue.notifyAll();
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
		} catch (IOException e) {
			Log.log(this, e, "closing Tar InStream " + tarFileName);
		}
		tarFileInputStream = null;
		try {
			if (tarFile != null) {
				tarFile.close();
			}
		} catch (IOException e) {
			Log.log(this, e, "closing Tar Tile " + tarFileName);
		}
		tarFile = null;
	}
	
	/**
	 * Inject main canvas (used for repaint).
	 * 
	 * @param canvas canvas
	 */
	public void setMainCanvas(final MainCanvas canvas) {
		this.mainCanvas = canvas;
	}
	
	/**
	 * Change map offset graphically.
	 * 
	 * @param dx map delta in pixels
	 * @param dy map delta in pixels
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
		ConfigFile config = ConfigFile.createEmtpyConfig();

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
		} catch (IOException e) {
			Log.log(this, "saving back config file" + e);
		}
	}

	/**
	 * @param lng longitude in degree
	 * @return normalized longitude
	 */
    private static double toNormalisedPixelLng(double lng) {
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
     * @param lat latitude in degree
     * @return normalized latitude
     */
    private static double toNormalisedPixelLat(double lat) {
        final double half = 0.5;
		final int four = 4;
		final double normalizedLat = -((MathUtil.log(Math.tan((Math.PI / four) 
        		+ ((half * Math.PI * lat) / (CIRCLE_DEGREE / 2)))) 
        			/ Math.PI) / 2.0);
        
        return normalizedLat;
    }
}
