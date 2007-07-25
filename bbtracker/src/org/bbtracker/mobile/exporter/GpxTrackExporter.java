package org.bbtracker.mobile.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.Enumeration;

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;
import org.bbtracker.TrackSegment;
import org.bbtracker.Utils;
import org.bbtracker.mobile.BBTracker;

public class GpxTrackExporter implements TrackExporter {
	private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

	public String getFileName(final Track track) {
		return track.getName().replace('/', '_') + ".gpx";
	}

	public void export(final OutputStream out, final Track track) throws IOException {
		Writer w = null;
		try {
			final String xmlName = Utils.escapeXml(track.getName());
			w = new OutputStreamWriter(out, "UTF-8");
			w.write(XML_HEADER);
			w.write("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"");
			w.write(BBTracker.getFullName());
			w.write(" http://www.bbtracker.org/\">\n");
			w.write("\t<metadata>\n");

			w.write("\t\t<name>");
			w.write(xmlName);
			w.write("</name>\n");

			w.write("\t\t<time>");
			w.write(Utils.dateToXmlDateTime(track.getCreationDate()));
			w.write("</time>\n");

			w.write("\t\t<bounds minlat=\"");
			w.write(String.valueOf(track.getMinLatitude()));
			w.write("\" minlon=\"");
			w.write(String.valueOf(track.getMinLongitude()));
			w.write("\"\n\t\t\tmaxlat=\"");
			w.write(String.valueOf(track.getMaxLatitude()));
			w.write("\" maxlon=\"");
			w.write(String.valueOf(track.getMaxLongitude()));
			w.write("\"/>\n");

			w.write("\t</metadata>\n");

			w.write("\t<trk>\n\t\t<name>");
			w.write(xmlName);
			w.write("</name>\n");

			final Enumeration segments = track.getSegments();
			while (segments.hasMoreElements()) {
				w.write("\t\t<trkseg>\n");
				final TrackSegment segment = (TrackSegment) segments.nextElement();
				final Enumeration points = segment.getPoints();
				while (points.hasMoreElements()) {
					final TrackPoint point = (TrackPoint) points.nextElement();
					w.write("\t\t\t<trkpt lat=\"");
					w.write(String.valueOf(point.getLatitude()));
					w.write("\" lon=\"");
					w.write(String.valueOf(point.getLongitude()));
					w.write("\"><ele>");
					w.write(String.valueOf(point.getElevation()));
					w.write("</ele><time>");
					w.write(Utils.dateToXmlDateTime(new Date(point.getTimestamp())));
					w.write("</time>");
					if (point.getName() != null) {
						w.write("<name>");
						w.write(Utils.escapeXml(point.getName()));
						w.write("</name>");
					}
					w.write("</trkpt>\n");
				}
				w.write("\t\t</trkseg>\n");
			}
			w.write("\t</trk>\n</gpx>");
		} finally {
			if (w != null) {
				try {
					w.close();
				} catch (final IOException ignored) {
					// ignore
				}
			}
		}
	}
}
