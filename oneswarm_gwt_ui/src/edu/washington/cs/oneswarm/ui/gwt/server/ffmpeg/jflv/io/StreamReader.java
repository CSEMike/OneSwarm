/**
 * This file is part of jFlvTool.
 *
 * jFlvTool is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * jFlvTool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * file name  : StreamReader.java
 * authors    : Jon Keys
 * created    : June 28, 2007, 9:55 AM
 * copyright  : Sony Digital Authoring Services
 *
 * modifications:
 * Date:        Name:           Description:
 * ----------   --------------- ----------------------------------------------
 * June 28, 2007    Jon Keys         Creation
 */

package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 
 * @author Jon Keys
 */
public class StreamReader {

    private static final int CHUNK_SIZE = 1048576;

    private long pos;
    private long fpos;
    private long fsize;
    private long byt2write;
    private int written;
    private boolean debug;

    private MappedByteBuffer data;
    private FileChannel chan;
    private File srcFile;
    private RandomAccessFile raf;
    private boolean isOpened;

    /** Creates a new instance of StreamReader */
    public StreamReader() {

        fsize = 0;
        byt2write = 0;
        written = 0;
        pos = 0;
        fpos = 0;
        data = null;
        chan = null;
        srcFile = null;
        raf = null;
        isOpened = false;
        debug = false;

    }

    public StreamReader(File srcFile) {

        fsize = srcFile.length();
        byt2write = 0;
        written = 0;
        pos = 0;
        fpos = 0;
        data = null;
        chan = null;
        this.srcFile = srcFile;
        raf = null;
        isOpened = false;

    }

    public void open() throws FileNotFoundException {

        raf = new RandomAccessFile(srcFile, "r");
        chan = raf.getChannel();
        fsize = srcFile.length();
        byt2write = 0;
        written = 0;
        pos = 0;
        fpos = 0;
        isOpened = true;

        try {

            fillBuffer();

        } catch (Exception e) {

            isOpened = false;
            System.out.println("Error - unable to initialize buffer");
            if (debug) {
                e.printStackTrace();
            }

        }// catch()

    }// open()

    public boolean isOpen() {
        return this.isOpened;
    }

    public void close() {

        try {

            chan.close();
            raf.close();

        } catch (Exception e) {

            if (debug) {
                System.out.println("Error - unable to close stream");
                e.printStackTrace();
            }

        } finally {

            chan = null;
            raf = null;
            isOpened = false;

        }// finally()

    }// close()

    private void fillBuffer() throws EoflvException {

        if (fpos >= fsize) {

            // done reading
            throw new EoflvException();

        } else if ((fpos + CHUNK_SIZE) > fsize) {

            byt2write = fsize - fpos;

        } else {

            byt2write = CHUNK_SIZE;

        }// else

        try {

            data = chan.map(FileChannel.MapMode.READ_ONLY, fpos, byt2write);
            // System.out.println("cap: " + data.capacity() + " remaining: " +
            // data.remaining());
            fpos += byt2write;

        } catch (IOException ex) {

            System.out.println("Error - unable to refill buffer");
            if (debug) {
                ex.printStackTrace();
            }

        }// catch()

    }// fillBuffer()

    public byte[] read(int len) throws EoflvException {
        // System.err.println("reading position: " + pos + " len=" + len);
        byte[] buf = new byte[len];

        if (len > data.remaining()) {
            written = data.remaining();
            data.get(buf, 0, written);
            fillBuffer();
            data.get(buf, written, (len - written));
            // data.position((len-written));
        } else {
            data.get(buf);
        }

        pos += len;

        return buf;

    }// read()

    public void skip(int len) throws EoflvException {

        if (len > data.remaining()) {
            written = data.remaining();
            fillBuffer();
            data.position((len - written));
        } else {
            data.position(data.position() + len);
        }

        pos += len;

    }// skip()

    public long getPos() {
        return pos;
    }

    public MappedByteBuffer getData() {
        return data;
    }

    public FileChannel getChan() {
        return chan;
    }

    public void setChan(FileChannel chan) {
        this.chan = chan;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

}// StreamReader
