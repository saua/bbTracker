// #ifndef AVOID_FILE_API
package org.bbtracker.mobile;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import org.bbtracker.Track;
import org.bbtracker.TrackStoreException;
import org.bbtracker.mobile.config.ConfigFile;

public class FileTrackStore implements TrackStore {

	private static final String BBT_EXTENSION = ".bbt";
	private static final String TXT_EXTENSION = ".txt";

	public TrackStoreEntry[] getEntries() throws TrackStoreException {
		final String dir = getTrackDirectory();
		FileConnection connection = null;
		try {
			final Vector result = new Vector();
			connection = (FileConnection) Connector.open("file:///" + dir, Connector.READ);
			final Enumeration binList = connection.list("*" + BBT_EXTENSION + "*", true);
			while (binList.hasMoreElements()) {
				final String file = (String) binList.nextElement();
				final FileTrackStoreEntry entry = readBinaryTrack(connection, file);
				if (entry != null) {
					result.addElement(entry);
				}
			}
			final Enumeration txtList = connection.list("*" + TXT_EXTENSION + "*", true);
			while (txtList.hasMoreElements()) {
				final String file = (String) txtList.nextElement();
				final FileTrackStoreEntry entry = readTextTrack(connection, file);
				;
				if (entry != null) {
					result.addElement(entry);
				}
			}
			final TrackStoreEntry[] entries = new TrackStoreEntry[result.size()];
			result.copyInto(entries);
			return entries;
		} catch (final IOException e) {
			Log.log(this, e, "loading track list");
			throw new TrackStoreException(e);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (final IOException e) {
					// can't do anything about it
					Log.log(this, e);
				}
			}
		}
	}

	private FileTrackStoreEntry readTextTrack(final FileConnection connection, final String file) {
		FileTrackStoreEntry entry = null;
		try {
			// TODO: Parse only first line
			final String filename = connection.getURL() + file;
			final Vector list = ConfigFile.openList(filename);
			final String name = (String) list.elementAt(0);
			if (!isValidName(name)) {
				return null;
			}
			final Date date = new Date(0);
			entry = new FileTrackStoreEntry(name, date, filename, false);
		} catch (final Exception e) {
			Log.log(this, e, "loading text info from " + file);
		}
		return entry;
	}

	private boolean isValidName(final String name) {
		if (name == null || name.length() <= 0) {
			return false;
		}
		for (int i = 0; i < name.length(); i++) {
			if (!isValidChar(name.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private boolean isValidChar(final char ch) {
		final String valid = ".-+=_.,<>:;";

		return Character.isDigit(ch) || Character.isLowerCase(ch) || Character.isUpperCase(ch)
				|| valid.indexOf(ch) != -1;
	}

	private FileTrackStoreEntry readBinaryTrack(final FileConnection connection, final String file) {
		DataInputStream din = null;
		FileTrackStoreEntry entry = null;
		try {
			final String fileUrl = connection.getURL() + file;
			din = Connector.openDataInputStream(fileUrl);
			final String name = Track.readNameFromStream(din);
			final Date date = Track.readDateFromStream(din);
			entry = new FileTrackStoreEntry(name, date, fileUrl, true);
		} catch (final IOException e) {
			Log.log(this, e, "loading binary info from " + file);
		} catch (final TrackStoreException e) {
			Log.log(this, e, "loading binary info from " + file);
		} finally {
			if (din != null) {
				try {
					din.close();
				} catch (final IOException e) {
					Log.log(this, e);
				}
			}
		}
		return entry;
	}

	public void saveTrack(final Track track) throws TrackStoreException {
		final String dir = getTrackDirectory();
		FileConnection connection = null;
		DataOutputStream dout = null;
		try {
			connection = FileUtil.createFile(dir, track.getName(), BBT_EXTENSION);
			dout = connection.openDataOutputStream();
			track.writeToStream(dout);
		} catch (final IOException e) {
			Log.log(this, e, "saving track");
			throw new TrackStoreException(e);
		} finally {
			if (dout != null) {
				try {
					dout.close();
				} catch (final IOException e) {
					// can't do anything about it
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (final IOException e) {
					// can't do anything about it
					Log.log(this, e);
				}
			}
		}
	}

	private String getTrackDirectory() throws TrackStoreException {
		final String dir = Preferences.getInstance().getTrackDirectory();
		if (dir == null) {
			throw new TrackStoreException("No track directory set, please configure it on the options screen!");
		}
		return dir;
	}

	private class FileTrackStoreEntry extends TrackStoreEntry {
		final String url;

		final boolean binary;

		public FileTrackStoreEntry(final String name, final Date date, final String url, final boolean binary) {
			super(name, date);
			this.url = url;
			this.binary = binary;
		}

		public void deleteTrack() throws TrackStoreException {
			try {
				final FileConnection connection = (FileConnection) Connector.open(url);
				connection.delete();
			} catch (final IOException e) {
				Log.log(this, e, "deleting track");
				throw new TrackStoreException("Failed to delete track: " + e.getMessage());
			}
		}

		public Track loadTrack() throws TrackStoreException {
			if (binary) {
				return loadBinaryTrack();
			} else {
				return loadTextTrack();
			}
		}

		private Track loadTextTrack() throws TrackStoreException {
			try {
				final Vector list = ConfigFile.openList(url);
				final Track track = Track.readFromStringList(list);
				return track;
			} catch (final Exception e) {
				Log.log(this, e, "loading track");
				throw new TrackStoreException("Failed to load track: " + e.getMessage());
			}
		}

		private Track loadBinaryTrack() throws TrackStoreException {
			DataInputStream din = null;
			try {
				din = Connector.openDataInputStream(url);
				final Track track = Track.readFromStream(din);
				return track;
			} catch (final IOException e) {
				Log.log(this, e, "loading track");
				throw new TrackStoreException("Failed to load track: " + e.getMessage());
			} finally {
				if (din != null) {
					try {
						din.close();
					} catch (final IOException e) {
						// can't do anything about it
						Log.log(this, e);
					}
				}
			}
		}
	}
}
// #endif
