/*
 * Copyright 2008 Sebastien Chauvin
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
package org.bbtracker.mobile.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import org.bbtracker.CsvWriter;

/**
 * Read and write configuration files. (name / value pairs)
 */
public final class ConfigFile {
	/** Hold all parameters. */
	private final Hashtable params = new Hashtable();

	/** Construct. */
	private ConfigFile() {
	}

	/**
	 * Create a configuration object and read data from a file.
	 * 
	 * @param filename
	 *            filename
	 * @return config object
	 * @throws IOException
	 *             io error
	 */
	public static ConfigFile openConfig(final String filename) throws IOException {
		return (ConfigFile) openFile(filename, false);
	}

	/**
	 * Create a configuration object and read data from an InputStream.
	 * 
	 * @param in
	 *            InputStream
	 * @return config object
	 * @throws IOException
	 *             io error
	 */
	public static ConfigFile openConfig(final InputStream in) throws IOException {
		return (ConfigFile) openStream(in, false);
	}

	/**
	 * Open a list from a file.
	 * 
	 * @param filename
	 *            filename
	 * @return list
	 * @throws IOException
	 *             io error
	 */
	public static Vector openList(final String filename) throws IOException {
		return (Vector) openFile(filename, true);
	}

	/**
	 * Create an empty configuration.
	 * 
	 * @return config object
	 */
	public static ConfigFile createEmtpyConfig() {
		return new ConfigFile();
	}

	/**
	 * Multi purpose file reading. (name/value or list) Any space is a
	 * separator.
	 * 
	 * @param filename
	 *            file to read
	 * @param list
	 *            if true read as a list
	 * @return vector or configfile
	 * @throws IOException
	 *             io error
	 */
	private static Object openFile(final String filename, final boolean list) throws IOException {
		final FileConnection connection = (FileConnection) Connector.open(filename, Connector.READ);
		final InputStream in = connection.openInputStream();
		try {
			final Object result = openStream(in, list);
			return result;
		} finally {
			try {
				connection.close();
			} catch (final IOException ignored) {
				// ignore exception
			}
		}
	}

	private static Object openStream(final InputStream in, final boolean list) throws IOException {
		ConfigFile self = null;
		Vector listVector = null;
		if (list) {
			listVector = new Vector();
		} else {
			self = new ConfigFile();
		}

		final InputStreamReader reader = new InputStreamReader(in);
		for (;;) {
			final String key;
			if (list) {
				key = null;
			} else {
				key = getWord(reader);
				if (key.length() == 0) {
					break;
				}
			}
			final String value = getWord(reader);
			if (value.length() == 0) {
				break;
			}
			if (list) {
				listVector.addElement(value);
			} else {
				self.params.put(key, value);
			}
		}

		in.close();

		if (list) {
			return listVector;
		} else {
			return self;
		}
	}

	/**
	 * Save the content of this config file back to a file.
	 * 
	 * @param url
	 *            url
	 * @throws IOException
	 *             io error
	 */
	public void saveConfig(final String url) throws IOException {
		final FileConnection connection = (FileConnection) Connector.open(url, Connector.WRITE);
		try {
			final OutputStream out = connection.openOutputStream();
			saveConfig(out);
		} finally {
			connection.close();
		}
	}

	/**
	 * Save the content of this config file back to a OutputStream.
	 * 
	 * @param out
	 *            OutputStream
	 * @throws IOException
	 *             io error
	 */
	public void saveConfig(final OutputStream out) throws IOException {
		final OutputStreamWriter writer = new OutputStreamWriter(out);
		try {
			final Enumeration keys = params.keys();
			final Enumeration values = params.elements();
			while (keys.hasMoreElements()) {
				final String key = (String) keys.nextElement();
				final String value = (String) values.nextElement();

				writer.write(key);
				writer.write(' ');
				writer.write(quoteValue(value));
				writer.write('\n');
			}
		} finally {
			writer.close();
			out.close();
		}
	}

	private String quoteValue(final String value) {
		if (value.length() == 0) {
			return "\"\"";
		} else {
			return CsvWriter.quote(value);
		}
	}

	/**
	 * Parse a word from the input stream.
	 * 
	 * @param reader
	 *            reader
	 * @return word read from stream
	 * @throws IOException
	 *             io error
	 */
	private static String getWord(final InputStreamReader reader) throws IOException {
		final StringBuffer word = new StringBuffer();
		int ch;
		boolean start = true;
		boolean quote = false;
		for (;;) {
			ch = reader.read();
			if (start) {
				if (isSpace((char) ch)) {
					continue;
				} else if (ch == '"') {
					quote = true;
					start = false;
					continue;
				}
			}
			if (ch == -1) {
				break;
			}

			start = false;
			if (quote) {
				if (((char) ch) == '"') {
					ch = reader.read();
					if (((char) ch) == '"') {
						word.append('"');
					} else {
						break;
					}
				} else {
					word.append((char) ch);
				}
			} else if (!isSpace((char) ch)) {
				word.append((char) ch);
			} else {
				break;
			}
		}
		return word.toString();
	}

	/**
	 * Check if character is a space.
	 * 
	 * @param ch
	 *            character
	 * @return true when space
	 */
	private static boolean isSpace(final char ch) {
		return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r';
	}

	/**
	 * Get matching value (or null).
	 * 
	 * @param key
	 *            key
	 * @return value
	 */
	public String get(final String key) {
		return (String) params.get(key);
	}

	/**
	 * Get matching value (or default value).
	 * 
	 * @param key
	 *            key
	 * @param defaultValue
	 *            String
	 * @return value
	 */
	public String get(final String key, final String defaultValue) {
		String result = (String) params.get(key);
		if (result == null) {
			result = defaultValue;
		}
		return result;
	}

	/**
	 * Get matching integer value (or null). throws NumberFormatException on
	 * invalid value.
	 * 
	 * @param key
	 *            key
	 * @param defaultValue
	 *            value
	 * @return integer value or default value if key not found
	 */
	public int getInteger(final String key, final int defaultValue) {
		final String value = get(key);
		if (value != null) {
			return Integer.parseInt(value);
		} else {
			return defaultValue;
		}
	}

	/**
	 * Get matching double value (or null). throws NumberFormatException on
	 * invalid value.
	 * 
	 * @param key
	 *            key
	 * @param defaultValue
	 *            value
	 * @return integer value or default value if key not found
	 */
	public double getDouble(final String key, final double defaultValue) {
		final String value = get(key);
		if (value != null) {
			return Double.parseDouble(value);
		} else {
			return defaultValue;
		}
	}

	/**
	 * Add name / value pair.
	 * 
	 * @param key
	 *            key
	 * @param value
	 *            value
	 */
	public void put(final String key, final double value) {
		put(key, Double.toString(value));
	}

	/**
	 * Add name / value pair.
	 * 
	 * @param key
	 *            key
	 * @param value
	 *            value
	 */
	public void put(final String key, final int value) {
		put(key, Integer.toString(value));
	}

	/**
	 * Add name / value pair.
	 * 
	 * @param key
	 *            key
	 * @param value
	 *            value
	 */
	public void put(final String key, final String value) {
		if (value == null) {
			params.remove(key);
		} else {
			params.put(key, value);
		}
	}
}
