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
class CsvWriter {
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
		final boolean hasWhitespace = s.indexOf(' ') != -1 || s.indexOf('\r') != -1 || s.indexOf('\n') != -1;
		final boolean hasQuote = s.indexOf('"') != -1;
		if (hasWhitespace || hasQuote) {
			buffer.append('"');
			if (hasQuote) {
				int offset = buffer.length();
				buffer.append(s);
				int i = s.indexOf('"');
				while (i != -1) {
					buffer.insert(offset + i, '"');
					offset++;
					i = s.indexOf('"', i + 1);
				}
			} else {
				buffer.append(s);
			}
			buffer.append('"');
		} else {
			buffer.append(s);
		}
		return this;
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