package org.bbtracker;

public class TrackStoreException extends Exception {
	private static final long serialVersionUID = 1L;

	public TrackStoreException(final Throwable t) {
		super(t.toString());
	}

	public TrackStoreException(final String message) {
		super(message);
	}
}
