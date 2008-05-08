package org.bbtracker;

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