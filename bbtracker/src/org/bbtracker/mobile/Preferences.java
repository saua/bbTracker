package org.bbtracker.mobile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;

public class Preferences {
	private static final String RECORD_STORE_NAME = "Preferences";

	public static final int START_ACTION_NOTHING = 0;

	public static final int START_ACTION_INIT_GPS = 1;

	public static final int START_ACTION_NEWTRACK = 2;

	public static String[] START_ACTIONS = new String[] { "Do nothing", "Initialize GPS", "Start new track" };

	private static Preferences instance;

	public static synchronized Preferences getInstance() {
		if (instance == null) {
			instance = new Preferences();
			try {
				instance.load();
			} catch (final RecordStoreException ignored) {
				// ignore
			}
		}
		return instance;
	}

	private int recordIndex = -1;

	private int sampleInterval = 5;

	private int startAction = START_ACTION_INIT_GPS;

	private int trackNumber = 1;

	private String exportDirectory;

	private Preferences() {
	}

	public int getSampleInterval() {
		return sampleInterval;
	}

	public void setSampleInterval(final int sampleInterval) {
		this.sampleInterval = sampleInterval;
	}

	public int getStartAction() {
		return startAction;
	}

	public void setStartAction(final int startAction) {
		this.startAction = startAction;
	}

	public int getNextTrackNumber() {
		return trackNumber++;
	}

	private void load() throws RecordStoreException {
		RecordStore rs = null;
		try {
			rs = RecordStore.openRecordStore(RECORD_STORE_NAME, false);

			final byte[] data;
			if (recordIndex != -1) {
				data = rs.getRecord(recordIndex);
			} else {
				final RecordEnumeration enumerateRecords = rs.enumerateRecords(null, null, false);
				if (enumerateRecords.hasNextElement()) {
					data = enumerateRecords.nextRecord();
					enumerateRecords.destroy();
				} else {
					enumerateRecords.destroy();
					return;
				}
			}

			final DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));

			startAction = in.readShort();
			sampleInterval = in.readInt();
			trackNumber = in.readInt();
			if (in.readByte() != 0) {
				exportDirectory = in.readUTF();
			}

			in.close();
		} catch (final RecordStoreNotFoundException e) {
			// ignore, don't load anything
		} catch (final InvalidRecordIDException e) {
			// ignore, don't load anything
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

	public void store() throws RecordStoreException {
		RecordStore rs = null;
		try {
			rs = RecordStore.openRecordStore(RECORD_STORE_NAME, true);

			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final DataOutputStream out = new DataOutputStream(baos);

			out.writeShort(startAction);
			out.writeInt(sampleInterval);
			out.writeInt(trackNumber);
			if (exportDirectory == null) {
				out.writeByte(0);
			} else {
				out.writeByte(1);
				out.writeUTF(exportDirectory);
			}

			out.close();
			final byte[] data = baos.toByteArray();

			if (recordIndex != -1) {
				rs.setRecord(recordIndex, data, 0, data.length);
			} else {
				final RecordEnumeration enumerateRecords = rs.enumerateRecords(null, null, false);
				if (enumerateRecords.hasNextElement()) {
					recordIndex = enumerateRecords.nextRecordId();
					rs.setRecord(recordIndex, data, 0, data.length);
				} else {
					recordIndex = rs.addRecord(data, 0, data.length);
				}
				enumerateRecords.destroy();
			}
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

	public String getExportDirectory() {
		return exportDirectory;
	}

	public void setExportDirectory(final String exportDirectory) {
		this.exportDirectory = exportDirectory;
	}
}
