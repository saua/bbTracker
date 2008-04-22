package org.bbtracker.mobile;

import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

public class FileUtil {
	private FileUtil() {
		// unneeded
	}

	public static FileConnection createFile(final String dir, final String name, final String extension)
			throws IOException {
		FileConnection connection = null;
		final String dirUrl = "file:///" + dir + (dir.endsWith("/") ? "" : "/");
		final String filename = makeFilesystemSafe(name);

		int i = 0;
		do {
			if (connection != null) {
				connection.close();
			}

			String fullName;
			if (i == 0) {
				fullName = dirUrl + filename + extension;
			} else {
				fullName = dirUrl + filename + "_" + i + extension;
			}

			try {
				connection = (FileConnection) Connector.open(fullName, Connector.READ_WRITE);
			} catch (final IllegalArgumentException e) {
				// some file system don't like file names that are longer than 8.3 (thanks CP/M and DOS!)
				if (i == 0) {
					if (filename.length() <= 8) {
						throw new IOException("Filesystem doesn't accept filename <" + fullName + ">: " +
								e.getMessage());
					}
					fullName = dirUrl + filename.substring(0, 8) + extension;
				} else {
					final String postfix = "_" + i;
					final int l = 8 - postfix.length();
					fullName = dirUrl + filename.substring(0, l) + postfix + extension;
				}
				try {
					connection = (FileConnection) Connector.open(fullName, Connector.READ_WRITE);
				} catch (final IllegalArgumentException e2) {
					throw new IOException("Filesystem doesn't accept filename <" + fullName + ">: " + e2.getMessage());
				}
			}
			i++;
		} while (connection.exists());
		connection.create();
		return connection;
	}

	public static String makeFilesystemSafe(final String name) {
		final char[] chars = name.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			final char c = chars[i];
			if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'))) {
				chars[i] = '_';
			}
		}
		return new String(chars);
	}
}
