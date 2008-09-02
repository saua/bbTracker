/*
 * Copyright 2008 Joachim Sauer
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
package org.bbtracker;

/**
 * The CsvWriter is used produce a CSV stream according to RFC 4180.
 */
public class CsvWriter {
	private final StringBuffer buffer = new StringBuffer();

	private boolean firstField = true;

	public CsvWriter append(final Object[] values) {
		for (int i = 0; i < values.length; i++) {
			append(String.valueOf(values[i]));
		}
		return this;
	}

	public CsvWriter append(final String s) {
		if (firstField) {
			firstField = false;
		} else {
			buffer.append(',');
		}
		appendQuoted(buffer, s);
		return this;
	}

	public static String quote(final String s) {
		final StringBuffer buf = appendQuoted(null, s);
		if (buf == null) {
			return s;
		} else {
			return buf.toString();
		}
	}

	private static StringBuffer appendQuoted(StringBuffer buf, final String s) {
		final boolean hasWhitespace = s.indexOf(' ') != -1 || s.indexOf('\r') != -1 || s.indexOf('\n') != -1;
		final boolean hasQuote = s.indexOf('"') != -1;
		if (hasWhitespace || hasQuote) {
			if (buf == null) {
				// quoted is at least 2 longer, let's give it some more space,
				// in
				// case it contains quotes
				buf = new StringBuffer(s.length() + 4);
			}
			buf.append('"');
			if (hasQuote) {
				int offset = buf.length();
				buf.append(s);
				int i = s.indexOf('"');
				while (i != -1) {
					buf.insert(offset + i, '"');
					offset++;
					i = s.indexOf('"', i + 1);
				}
			} else {
				buf.append(s);
			}
			buf.append('"');
		} else if (buf != null) {
			buf.append(s);
		}
		return buf;
	}

	public CsvWriter nl() {
		buffer.append("\r\n");
		firstField = true;
		return this;
	}

	public String toString() {
		return buffer.toString();
	}
}