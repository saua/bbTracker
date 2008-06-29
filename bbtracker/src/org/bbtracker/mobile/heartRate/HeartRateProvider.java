package org.bbtracker.mobile.heartRate;

import javax.microedition.lcdui.Canvas;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.control.RecordControl;

/**
 * Record sound from microphone and decode using {@link DetectHeartBeat}.
 */
public final class HeartRateProvider {
	/** Excpected sampling rate. */
	private static final int DEFAULT_SAMPLING_RATE = 8000;

	/** Player. */
	Player player;

	/** Record control. */
	RecordControl rcontrol;

	/** Decoder. */
	DetectHeartBeat dhb = new DetectHeartBeat(DEFAULT_SAMPLING_RATE, true);

	/**
	 * Start recording.
	 * 
	 * @return true if start successfull
	 */
	public boolean start() {
		try {
			// Create a Player that captures live audio.
			player = Manager.createPlayer("capture://audio?encoding=pcm");
			player.realize();

			// Get the RecordControl, set the record stream,
			rcontrol = (RecordControl) player.getControl("RecordControl");
			// _rcontrol.setRecordStream(_output);

			rcontrol.setRecordStream(dhb);
			rcontrol.startRecord();
			player.start();
			return true;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/** Stop recording. */
	public void stop() {
		rcontrol.stopRecord();
		rcontrol = null;
		player.close();
		player = null;
	}

	/** @return current heart rate */
	public int getHeartRate() {
		return dhb.getHeartRate();
	}

	/**
	 * @param c
	 *            the canvas to set
	 */
	public void setCanvas(final Canvas c) {
		dhb.setCanvas(c);
	}
}
