package org.bbtracker.mobile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;

import org.bbtracker.Track;

public class TrackStore {
	private static final String RECORD_STORE_NAME = "Tracks";

	private static TrackStore instance;

	private Track lastStoredTrack;

	private int lastStoredTrackIndex;

	private int[] indices;

	private String[] names;

	public static synchronized TrackStore getInstance() {
		if (instance == null) {
			instance = new TrackStore();
		}
		return instance;
	}

	private TrackStore() {
	}

	public String[] getTrackNames() throws RecordStoreException {
		if (names == null) {
			RecordStore rs = null;
			try {
				rs = RecordStore.openRecordStore(RECORD_STORE_NAME, true);
				final RecordEnumeration enumeration = rs.enumerateRecords(null, null, false);
				final int s = enumeration.numRecords();
				indices = new int[s];
				names = new String[s];
				int i = 0;
				while (enumeration.hasNextElement()) {
					final byte[] data = enumeration.nextRecord();
					try {
						final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
						names[i] = Track.readDescriptionFromStream(dis);
						dis.close();
					} catch (final IOException e) {
						names[i] = "Unreadable track record";
					}
					i++;
				}
				enumeration.reset();
				i = 0;
				while (enumeration.hasNextElement()) {
					indices[i++] = enumeration.nextRecordId();
				}
				enumeration.destroy();
			} catch (final RecordStoreNotFoundException e) {
				names = new String[0];
				indices = new int[0];
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
		return names;
	}

	public Track getTrack(final int index) throws RecordStoreException {
		if (indices == null) {
			throw new IllegalStateException("Must not call getTrack() without calling getTrackNames() before!");
		}
		RecordStore rs = null;
		DataInputStream dis = null;
		try {
			rs = RecordStore.openRecordStore(RECORD_STORE_NAME, false);
			final byte[] data = rs.getRecord(indices[index]);
			final Track track;
			dis = new DataInputStream(new ByteArrayInputStream(data));
			track = Track.readFromStream(dis);
			dis.close();
			return track;
		} catch (final IOException e) {
			throw new RecordStoreException(e.getMessage());
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

	public void store(final Track track) throws RecordStoreException {
		RecordStore rs = null;
		try {
			rs = RecordStore.openRecordStore(RECORD_STORE_NAME, true);

			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final DataOutputStream out = new DataOutputStream(baos);

			track.writeToStream(out);

			out.close();
			final byte[] data = baos.toByteArray();

			if (track == lastStoredTrack) {
				rs.setRecord(lastStoredTrackIndex, data, 0, data.length);
			} else {
				lastStoredTrack = track;
				lastStoredTrackIndex = rs.addRecord(data, 0, data.length);
			}

			indices = null;
			names = null;
		} catch (final IOException e) {
			throw new RecordStoreException(e.getMessage());
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

	public void deleteTrack(final int selectedIndex) throws RecordStoreException {
		RecordStore rs = null;
		try {
			rs = RecordStore.openRecordStore(RECORD_STORE_NAME, true);

			rs.deleteRecord(indices[selectedIndex]);

			indices = null;
			names = null;
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
}
