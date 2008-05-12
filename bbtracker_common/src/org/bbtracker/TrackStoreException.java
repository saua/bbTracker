package org.bbtracker;

public class TrackStoreException extends Exception {
	public TrackStoreException(final Throwable t) {
		super(t.toString());
	}

	public TrackStoreException(final String message) {
		super(message);
	}
}
