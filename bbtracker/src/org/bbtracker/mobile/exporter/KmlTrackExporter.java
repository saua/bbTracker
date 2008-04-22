/*
 * Copyright 2007 Joachim Sauer
 * Copyright 2007 SIB
 * 
 * This file is part of bbTracker.
 * 
 * bbTracker is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * bbTracker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.bbtracker.mobile.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;

import org.bbtracker.Track;
import org.bbtracker.TrackPoint;
import org.bbtracker.TrackSegment;
import org.bbtracker.UnitConverter;
import org.bbtracker.Utils;
import org.bbtracker.mobile.Preferences;

public class KmlTrackExporter implements TrackExporter {
	private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

	private static final String KML_HEADER = "<kml xmlns=\"http://earth.google.com/kml/2.1\">\n";

	private static final String STYLE_NAME = "sn_ylw-pushpin";

	private static final String STYLE_TAG = "\t<Style id=\"" + STYLE_NAME + "\">\n" + "<LineStyle>\n" +
			"<color>63eeee17</color>\n" + "<width>4</width>\n" + "</LineStyle>\n" + "</Style>\n";

	public String getExtension() {
		return ".kml";
	}

	public void export(final OutputStream out, final Track track) throws IOException {
		Writer w = null;
		try {
			final String xmlName = Utils.escapeXml(track.getName());
			// Added 2007 SIB
			final UnitConverter unit = Preferences.getInstance().getUnitsConverter();
			final String lengthString = unit.distanceToString(track.getLength());
			final String timeString;
			if (track.getPointCount() > 0) {
				final TrackPoint lastPoint = track.getPoint(track.getPointCount() - 1);
				final long duration = track.getPointOffset(lastPoint);
				timeString = Utils.durationToString(duration);
			} else {
				timeString = "-";
			}
			final String maxElevString = unit.elevationToString(track.getMaxElevation());
			final String minElevString = unit.elevationToString(track.getMinElevation());
			final String maxSpeedString = unit.speedToString(track.getMaxSpeed());

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

			w.write("<description><![CDATA[<table>\n");
			w.write("<tr><td><b>Total Distance: </b>");
			w.write(lengthString);
			w.write("</td></tr>\n");
			w.write("<tr><td><b>Total Time: </b>");
			w.write(timeString);
			w.write("</td></tr>\n");
			w.write("<tr><td><b>Max Speed: </b>");
			w.write(maxSpeedString);
			w.write("</td></tr>\n");
			w.write("<tr><td><b>Max Elevation: </b>");
			w.write(maxElevString);
			w.write("</td></tr>\n");
			w.write("<tr><td><b>Min Elevation: </b>");
			w.write(minElevString);
			w.write("</td></tr>\n");
			w.write(")</table>]]></description>)");

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
			w.write("\n\t\t\t</coordinates>\n\t\t</LineString>\n\t</Placemark>\n</Document>\n</kml>");
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
