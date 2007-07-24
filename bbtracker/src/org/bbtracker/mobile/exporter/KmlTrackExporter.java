package org.bbtracker.mobile.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;
import org.bbtracker.TrackSegment;
import org.bbtracker.Utils;

public class KmlTrackExporter implements TrackExporter {
	private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

	private static final String KML_HEADER = "<kml xmlns=\"http://earth.google.com/kml/2.1\">\n";

	private static final String STYLE_NAME = "sn_style";

	private static final String STYLE_TAG = "\t<Style id=\"" + STYLE_NAME + "\"></Style>\n";

	public String getFileName(final Track track) {
		return track.getName().replace('/', '_') + ".kml";
	}

	public void export(final OutputStream out, final Track track) throws IOException {
		Writer w = null;
		try {
			final String xmlName = Utils.escapeXml(track.getName());
			w = new OutputStreamWriter(out, "UTF-8");
			w.write(XML_HEADER);
			w.write(KML_HEADER);

			w.write("<Document>\n");
			w.write("\t<name>");
			w.write(xmlName);
			w.write(".kml</name>\n");

			w.write(STYLE_TAG);

			w.write("\t<Placemark>\n\t\t<name>");
			w.write(xmlName);
			w.write("</name>\n");
			w.write("\t\t<visibility>0</visibility>\n");
			w.write("\t\t<styleUrl>#");
			w.write(STYLE_NAME);
			w.write("</styleUrl>\n");
			w.write("\t\t<LineString>\n\t\t\t<coordinates>\n");
			final Enumeration segments = track.getSegments();
			while (segments.hasMoreElements()) {
				final TrackSegment segment = (TrackSegment) segments.nextElement();
				final Enumeration points = segment.getPoints();
				while (points.hasMoreElements()) {
					final TrackPoint point = (TrackPoint) points.nextElement();
					w.write(String.valueOf(point.getLongitude()));
					w.write(',');
					w.write(String.valueOf(point.getLatitude()));
					w.write(',');
					w.write(String.valueOf(point.getElevation()));
					w.write(' ');
				}
			}
			w.write("\t\t\t</coordinates>\n\t\t</LineString>\n\t</Placemark>\n</Document>\n</kml>");
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
