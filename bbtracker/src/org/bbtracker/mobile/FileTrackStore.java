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

public class FileTrackStore implements TrackStore {

	private static final String EXTENSION = ".bbt";

	public TrackStoreEntry[] getEntries() throws TrackStoreException {
		final String dir = getTrackDirectory();
		FileConnection connection = null;
		try {
			final Vector result = new Vector();
			connection = (FileConnection) Connector.open("file:///" + dir, Connector.READ);
			final Enumeration list = connection.list("*" + EXTENSION + "*", true);
			while (list.hasMoreElements()) {
				final String file = (String) list.nextElement();
				DataInputStream din = null;
				try {
					final String fileUrl = connection.getURL() + file;
					din = Connector.openDataInputStream(fileUrl);
					final String name = Track.readNameFromStream(din);
					final Date date = Track.readDateFromStream(din);
					result.addElement(new FileTrackStoreEntry(name, date, fileUrl));
				} catch (final IOException e) {
					Log.log(this, e, "loading info from " + file);
				} finally {
					if (din != null) {
						try {
							din.close();
						} catch (final IOException e) {
							Log.log(this, e);
						}
					}
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

	public void saveTrack(final Track track) throws TrackStoreException {
		final String dir = getTrackDirectory();
		FileConnection connection = null;
		DataOutputStream dout = null;
		try {
			connection = createTrackFile(dir, track.getName());
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

	private FileConnection createTrackFile(final String dir, final String trackName) throws IOException,
			TrackStoreException {
		final String filename = getFileName(trackName);
		FileConnection connection = null;

		int i = 0;
		do {
			if (connection != null) {
				connection.close();
			}

			String fullName;
			if (i == 0) {
				fullName = "file:///" + dir + filename + EXTENSION;
			} else {
				fullName = "file:///" + dir + filename + "_" + i + EXTENSION;
			}

			try {
				connection = (FileConnection) Connector.open(fullName, Connector.READ_WRITE);
			} catch (final IllegalArgumentException e) {
				// some file system don't like file names that are longer than 8.3 (thanks CP/M and DOS!)
				if (i == 0) {
					if (filename.length() <= 8) {
						throw new TrackStoreException("Filesystem doesn't accept filename <" + fullName + ">: " +
								e.getMessage());
					}
					fullName = "file:///" + dir + filename.substring(0, 8) + EXTENSION;
				} else {
					final String postfix = "_" + i;
					final int l = 8 - postfix.length();
					fullName = "file:///" + dir + filename.substring(0, l) + postfix + EXTENSION;
				}
				try {
					connection = (FileConnection) Connector.open(fullName, Connector.READ_WRITE);
				} catch (final IllegalArgumentException e2) {
					throw new TrackStoreException("Filesystem doesn't accept filename <" + fullName + ">: " +
							e2.getMessage());
				}
			}
			i++;
		} while (connection.exists());
		connection.create();
		return connection;
	}

	private String getTrackDirectory() throws TrackStoreException {
		final String dir = Preferences.getInstance().getTrackDirectory();
		if (dir == null) {
			throw new TrackStoreException("No track directory set, please configure it on the options screen!");
		}
		return dir;
	}

	private String getFileName(final String trackName) {
		final char[] chars = trackName.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			final char c = chars[i];
			if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'))) {
				chars[i] = '_';
			}
		}
		return new String(chars);
	}

	private class FileTrackStoreEntry extends TrackStoreEntry {
		final String url;

		public FileTrackStoreEntry(final String name, final Date date, final String url) {
			super(name, date);
			this.url = url;
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
