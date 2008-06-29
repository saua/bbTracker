package org.bbtracker.mobile.heartRate;

import java.io.OutputStream;

import javax.microedition.lcdui.Canvas;

/**
 * Detect heart beat from a signed 16bit pcm stream.
 * 
 * @author Sebastien
 */
public final class DetectHeartBeat extends OutputStream {
	/** Number of clicks to average before giving a heart rate. */
	private static final int PERIOD_AVERAGE = 6;

	/** Period in ms under which we ignore clicks. */
	private static final int IGNORE_PERIOD_MS = 250;

	/**
	 * If average is below this level, something is wrong. This also means that
	 * the decoder won't work for heart beat < 40.
	 */
	private static final int MINIMUM_AVERAGE_HR = 80;

	/** PCM sampling rate. */
	private final int inputRate;

	/** If true, use 16bits. */
	private final boolean word;

	/** Position in stream. */
	private long position;

	/** Pcm level below which we consider silence. */
	private final int silenceLevel = 15;

	/** Number of samples of silence before and after a click. */
	private final int silenceMargin = 10;

	/** PCM level for a click. */
	private final int clickLevel = 40;

	/** Number of samples for a click. */
	private final int clickDuration = 20;

	/** Rolling buffer for click positions. */
	private final long[] clickPositions = new long[100];

	/** Number of clicks in the clickPositions buffer. */
	private int nClicks = 0;

	/** Current average heart rate. */
	private int heartRate;

	/** Number of clicks samples detected. */
	private int detected = 0;

	/** Number samples of silence in the stream. */
	private int silence = 0;

	/** Number samples of non silence in the stream (before a click). */
	private int nosilence = 0;

	/** position of the click. */
	private int clickPos = -1;

	/** Canvas to trigger repaints. */
	private Canvas canvas;

	/**
	 * Construct.
	 * 
	 * @param samplingRate
	 *            number of samples per second
	 * @param wordInput
	 *            if true, use 16bits
	 */
	public DetectHeartBeat(final int samplingRate, final boolean wordInput) {
		inputRate = samplingRate;
		word = wordInput;
		position = 0;
	}

	/**
	 * @param c
	 *            the canvas to set
	 */
	public void setCanvas(final Canvas c) {
		canvas = c;
	}

	/**
	 * @return array with click locations in samples
	 */
	public long[] getClickPositions() {
		return clickPositions;
	}

	/**
	 * Outputstream implementation.
	 * 
	 * @param buffer
	 *            input buffer
	 * @param off
	 *            offset
	 * @param length
	 *            number of samples
	 */
	public void write(final byte[] buffer, final int off, final int length) {
		for (int i = off; i < off + length - 3; i += 4) {
			final int w = Math.abs(buffer[i + 1]) + Math.abs(buffer[i + 3]);
			if (w < silenceLevel) {
				++silence;
				if (clickPos != -1 && silence > silenceMargin) {
					addClick(clickPos + position);
					clickPos = -1;
				}
				nosilence = 0;
			} else {
				final boolean validPrephase = silence > silenceMargin || nosilence > 0;
				if (w > clickLevel) {
					++detected;
					if (validPrephase && clickPos == -1) {
						clickPos = i - off;
						i += (clickDuration - nosilence) * 2;
					} else {
						clickPos = -1;
					}
				} else {
					if (validPrephase) {
						++nosilence;
						silence = 0;
						if (nosilence > clickDuration) {
							clickPos = -1;
							nosilence = 0;
						}
					} else {
						nosilence = 0;
					}
				}
				silence = 0;
			}
		}

		position += length;
	}

	/**
	 * Add a potential click to the click array.
	 * 
	 * @param position
	 *            position of the click
	 */
	private void addClick(final long position) {
		if (nClicks <= PERIOD_AVERAGE) {
			clickPositions[nClicks++] = position;
		} else {
			final long lastPos = clickPositions[nClicks - 1];
			final long lastPeriod = position - lastPos;
			final int ignorePeriodSamples = IGNORE_PERIOD_MS * inputRate * 2 / 1000;
			if (lastPeriod < ignorePeriodSamples) {
				return;
			}
			final long periodSum = lastPos - clickPositions[nClicks - 1 - PERIOD_AVERAGE];
			heartRate = (int) (60 * PERIOD_AVERAGE * inputRate * 2 / periodSum);
			final int maximumAveragePeriod = inputRate * 2 * 60 / MINIMUM_AVERAGE_HR;
			final long avg = Math.min(maximumAveragePeriod, periodSum / PERIOD_AVERAGE);
			System.out.println("HR: " + heartRate + " avg: " + avg + " " + maximumAveragePeriod);
			final long minPeriod = avg / 2;
			final long maxPeriod = avg * 2 - minPeriod; // * 1.5
			if (nClicks >= clickPositions.length - PERIOD_AVERAGE - 1) {
				System.arraycopy(clickPositions, nClicks - PERIOD_AVERAGE - 1, clickPositions, 0, PERIOD_AVERAGE + 1);
				nClicks = PERIOD_AVERAGE + 1;
			}
			if (lastPeriod < minPeriod) {
				if (Math.abs(position - clickPositions[nClicks - 2] - avg) < Math.abs(lastPeriod - avg)) {
					// simply replace last click by the new one
					clickPositions[nClicks - 1] = position;
				} // else ignore this one
			} else if (lastPeriod > maxPeriod) {
				// add fake position here
				clickPositions[nClicks++] = lastPos + lastPeriod / 2;
				clickPositions[nClicks++] = position;
			} else {
				clickPositions[nClicks++] = position;
			}
			if (canvas != null) {
				canvas.repaint();
			}
		}
	}

	/**
	 * @return heart rate
	 */
	public int getHeartRate() {
		return heartRate;
	}

	/**
	 * Outputstream implementation.
	 * 
	 * @param b
	 *            byte
	 */
	public void write(final int b) {
		// System.out.println("Write byte: " + b);
	}

	/**
	 * Outputstream implementation.
	 * 
	 * @param buffer
	 *            buffer
	 */
	public void write(final byte[] buffer) {
		write(buffer, 0, buffer.length);
	}
}
