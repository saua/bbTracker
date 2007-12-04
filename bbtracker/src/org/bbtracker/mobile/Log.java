package org.bbtracker.mobile;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;

import javax.microedition.io.Connector;

public class Log {
	public static final int MAX_LOG = 25;

	public static final String[] log = new String[MAX_LOG];

	public static int head;

	public static int tail;

	private static PrintStream logStream;

	public static void initLog() {
		// #ifndef AVOID_FILE_API
		if (logStream != null || System.getProperty("microedition.io.file.FileConnection.version") == null) {
			return;
		}
		final String dirName = Preferences.getInstance().getTrackDirectory();
		if (dirName == null) {
			return;
		}
		final String logUrl = "file:///" + dirName + "debug.txt";
		try {
			final javax.microedition.io.file.FileConnection fileConnection = (javax.microedition.io.file.FileConnection) Connector
					.open(logUrl);
			if (!(fileConnection.exists() && fileConnection.canWrite())) {
				fileConnection.close();
				return;
			}
			final OutputStream out = fileConnection.openOutputStream();
			logStream = new PrintStream(out);
		} catch (final Throwable e) {
			log(BBTracker.class, e, "opening " + logUrl);
		}
		// #endif
	}

	public static void setFileActive(final boolean logActive) {
		if (!logActive && logStream != null) {
			logStream.close();
			logStream = null;
		}
		// #ifndef AVOID_FILE_API

		final String dirName = Preferences.getInstance().getTrackDirectory();
		final String logUrl = "file:///" + dirName + "debug.txt";
		try {
			final javax.microedition.io.file.FileConnection fileConnection = (javax.microedition.io.file.FileConnection) Connector
					.open(logUrl);
			if (logActive) {
				if (!fileConnection.exists()) {
					fileConnection.create();
				}
				final OutputStream out = fileConnection.openOutputStream();
				logStream = new PrintStream(out);
			} else {
				if (fileConnection.exists()) {
					fileConnection.delete();
					fileConnection.close();
				}
			}
		} catch (final Throwable e) {
			log(BBTracker.class, e, "opening " + logUrl);
		}
		// #endif
	}

	public static boolean isFileActive() {
		return logStream != null;
	}

	public static void log(final Object source, final Throwable e) {
		log(source, "Exception: " + e.toString());
		// this is only useful for debugging in the emulator
		e.printStackTrace();
	}

	public static void log(final Object source, final Throwable e, final String message) {
		log(source, "Exception <" + message + ">: " + e.toString());
		// this is only useful for debugging in the emulator
		e.printStackTrace();
	}

	public static void log(final Object source, final String m) {
		final String d = new Date().toString();
		final String line = d + ": [" + source + "] " + m;
		System.err.println(line);
		if (logStream != null) {
			logStream.println(line);
		}

		synchronized (log) {
			final Class c = (source instanceof Class) ? (Class) source : source.getClass();
			final String cn = c.getName();
			final String l = d.substring(11, 20) + cn.substring(cn.lastIndexOf('.') + 1) + " " + m + "\n";
			log[tail] = l;
			tail = (tail + 1) % MAX_LOG;
			if (tail == head) {
				head = (head + 1) % MAX_LOG;
			}
		}
	}

	public static String[] getLog() {
		synchronized (log) {
			final int length = (tail + MAX_LOG - head) % MAX_LOG;
			final String[] result = new String[length];
			for (int i = head, j = 0; i != tail; i = (i + 1) % MAX_LOG, j++) {
				result[j] = log[i];
			}
			return result;
		}
	}

	public static void shutdown() {
		if (logStream != null) {
			logStream.close();
			logStream = null;
		}
	}
}
