package org.bbtracker.mobile.exporter;

import java.io.IOException;
import java.io.OutputStream;

import org.bbtracker.Track;

public interface TrackExporter {
	public String getFileName(final Track track);

	public void export(final OutputStream out, final Track track) throws IOException;
}
