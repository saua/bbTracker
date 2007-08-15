package org.bbtracker.mobile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;

import org.bbtracker.Track;

public class RMSTrackStore implements TrackStore {
	private static final String RECORD_STORE_NAME = "Tracks";

	public TrackStoreEntry[] getEntries() throws TrackStoreException {
		RecordStore rs = null;
		try {
			final TrackStoreEntry[] entries;
			rs = RecordStore.openRecordStore(RECORD_STORE_NAME, false);
			final RecordEnumeration enumeration = rs.enumerateRecords(null, null, false);
			final int s = enumeration.numRecords();

			entries = new TrackStoreEntry[s];
			int i = 0;
			while (enumeration.hasNextElement()) {
				final byte[] data = enumeration.nextRecord();
				try {
					final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
					final String name = Track.readNameFromStream(dis);
					final Date date = Track.readDateFromStream(dis);
					entries[i] = new RMSTrackStoreEntry(name, date);
					dis.close();
				} catch (final IOException e) {
					BBTracker.log(e);
				}
				i++;
			}
			i = 0;
			enumeration.reset();
			while (enumeration.hasNextElement()) {
				final int index = enumeration.nextRecordId();
				final RMSTrackStoreEntry e = (RMSTrackStoreEntry) entries[i++];
				if (e != null) {
					e.index = index;
				}
			}
			enumeration.destroy();
			return entries;
		} catch (final RecordStoreNotFoundException e) {
			return new TrackStoreEntry[0];
		} catch (final RecordStoreException e) {
			BBTracker.log(e);
			throw new TrackStoreException(e);
		} finally {
			if (rs != null) {
				try {
					rs.closeRecordStore();
				} catch (final RecordStoreException e) {
					// ignore
				}
			}
		}
	}

	public void saveTrack(final Track track) throws TrackStoreException {
		RecordStore rs = null;
		try {
			rs = RecordStore.openRecordStore(RECORD_STORE_NAME, true);

			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final DataOutputStream out = new DataOutputStream(baos);

			track.writeToStream(out);

			out.close();

			final byte[] data = baos.toByteArray();

			rs.addRecord(data, 0, data.length);
		} catch (final IOException e) {
			throw new TrackStoreException(e);
		} catch (final RecordStoreException e) {
			throw new TrackStoreException(e);
		} finally {
			if (rs != null) {
				try {
					rs.closeRecordStore();
				} catch (final RecordStoreException e) {
					// ignore
				}
			}
		}
	}

	private class RMSTrackStoreEntry extends TrackStoreEntry {
		int index;

		public RMSTrackStoreEntry(final String name, final Date date) {
			super(name, date);
		}

		public void deleteTrack() throws TrackStoreException {
			RecordStore rs = null;
			try {
				rs = RecordStore.openRecordStore(RECORD_STORE_NAME, false);
				rs.deleteRecord(index);
			} catch (final RecordStoreException e) {
				throw new TrackStoreException(e);
			} finally {
				if (rs != null) {
					try {
						rs.closeRecordStore();
					} catch (final RecordStoreException e) {
						// ignore
					}
				}
			}
		}

		public Track loadTrack() throws TrackStoreException {
			RecordStore rs = null;
			DataInputStream dis = null;
			try {
				rs = RecordStore.openRecordStore(RECORD_STORE_NAME, false);
				final byte[] data = rs.getRecord(index);
				final Track track;
				dis = new DataInputStream(new ByteArrayInputStream(data));
				track = Track.readFromStream(dis);
				dis.close();
				return track;
			} catch (final IOException e) {
				throw new TrackStoreException(e);
			} catch (final RecordStoreException e) {
				throw new TrackStoreException(e);
			} finally {
				if (rs != null) {
					try {
						rs.closeRecordStore();
					} catch (final RecordStoreException e) {
						// ignore
					}
				}
				if (dis != null) {
					try {
						dis.close();
					} catch (final IOException e) {
						// ignore
					}
				}
			}
		}
	}
}