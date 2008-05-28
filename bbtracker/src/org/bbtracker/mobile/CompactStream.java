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
package org.bbtracker.mobile;

import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream decorator to notify end of "file" when reading a file from a meta file.
 *
 * @author Sebastien Chauvin
 */
public final class CompactStream extends InputStream {
    /** Stream. */
    private InputStream m_stream;
    /** bytes left. */
    private int         m_bytesRead;
    /** bytes to be read. */
    private int         m_length;
    
    /**
     * Construct.
     * 
     * @param stream parent stream
     * @param length number of bytes
     */
    public CompactStream(final InputStream stream, final int length) {
        m_stream = stream;
        m_bytesRead = 0;
        m_length = length;
    }
    
    /**
     * Change stream.
     * 
     * @param stream new stream
     */
    public void setStream(final InputStream stream) {
        m_stream = stream;
    }
    /**
     * Reset this object to read another file in the cfs.
     * 
     * @param length file length
     */
    public void reset(int length) {
        m_length = length;
        m_bytesRead = 0;
    }
    
    /** 
     * Read a byte.
     * 
     * @return byte
     * @throws IOException io error
     */
    public int read() throws IOException {
//        Log.log(this, "read() " + m_bytesRead + " / " + m_length);
        int rd = -1;
        if (m_bytesRead < m_length) {
            rd = m_stream.read();
        }
        if (rd != -1) {
            ++m_bytesRead;
        }
        return rd;
    }

    /** 
     * Read an array.
     * 
     * @param  b destination array
     * @return n bytes read
     * @throws IOException io error
     */
    public int read(byte[] b) throws IOException {
        int rd = -1;
        if (m_bytesRead < m_length) {
            if (m_length - m_bytesRead > b.length) {
                rd = m_stream.read(b);
            } else {
                rd = m_stream.read(b, 0, m_length - m_bytesRead);
//                Log.log(this, "Reading " + (m_length - m_bytesRead));
            }
        }
        if (rd != -1) {
            m_bytesRead += rd;
        }
        return rd;
    }
    
    /**
     * Return number of objects read.
     * 
     * @return n read
     */
    public int getNRead() {
        return m_bytesRead; 
    }
}
