/*
 * Copyright 2007 Joachim Sauer
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
package org.bbtracker.mobile;

import java.util.Date;

import org.bbtracker.Comparator;
import org.bbtracker.Track;
import org.bbtracker.TrackStoreException;

public interface TrackStore {
	public static final Comparator TSE_COMPARATOR = new Comparator() {
		public int compare(Object a, Object b) {
			return ((TrackStoreEntry) a).compareTo((TrackStoreEntry) b);
		}
	};

	public TrackStoreEntry[] getEntries() throws TrackStoreException;

	public void saveTrack(final Track track) throws TrackStoreException;

	public static abstract class TrackStoreEntry {
		private final Date date;

		private final String name;

		public TrackStoreEntry(final String name, final Date date) {
			this.name = name;
			this.date = date;
		}

		public Date getDate() {
			return date;
		}

		public String getName() {
			return name;
		}

		public abstract Track loadTrack() throws TrackStoreException;

		public abstract void deleteTrack() throws TrackStoreException;

		public boolean equals(final Object obj) {
			if (obj == this) {
				return true;
			} else if (obj == null || obj.getClass() != getClass()) {
				return false;
			}
			final TrackStoreEntry other = (TrackStoreEntry) obj;
			return other.getDate().equals(getDate()) && other.getName().equals(getName());
		}

		public int hashCode() {
			return getClass().hashCode() ^ getDate().hashCode() ^ getName().hashCode();
		}

		public int compareTo(final TrackStoreEntry other) {
			final long thisTime = getDate().getTime();
			final long otherTime = other.getDate().getTime();
			if (thisTime > otherTime) {
				return -1;
			} else if (thisTime < otherTime) {
				return 1;
			} else {
				return getName().compareTo(other.getName());
			}
		}
	}
}