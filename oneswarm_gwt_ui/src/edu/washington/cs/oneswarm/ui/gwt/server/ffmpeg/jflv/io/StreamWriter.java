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
 * file name  : StreamWriter.java
 * authors    : Jon Keys
 * created    : July 1, 2007, 1:14 PM
 * copyright  : Sony Digital Authoring Services
 *
 * modifications:
 * Date:        Name:           Description:
 * ----------   --------------- ----------------------------------------------
 * July 1, 2007    Jon Keys         Creation
 */

package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 
 * @author Jon Keys
 */
public class StreamWriter {

    private long pos;
    private FileChannel chan;
    private File srcFile;
    private RandomAccessFile raf;
    private boolean isOpened;
    protected StreamReader inStream;
    private boolean debug;

    /** Creates a new instance of StreamWriter */
    public StreamWriter() {

        pos = 0;
        chan = null;
        srcFile = null;
        raf = null;
        isOpened = false;
        debug = false;

    }

    public StreamWriter(File srcFile) {

        pos = 0;
        chan = null;
        this.srcFile = srcFile;
        raf = null;
        isOpened = false;
        debug = false;

    }

    public void open() throws FileNotFoundException {
        raf = new RandomAccessFile(srcFile, "rw");
        chan = raf.getChannel();
        pos = 0;
        isOpened = true;
    }

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

    public int write(ByteBuffer bbuf) {

        int lenWritten = 0;

        try {

            lenWritten = chan.write(bbuf);
            pos += lenWritten;

        } catch (IOException ex) {

            System.out.println("Error - unable to write specified bytes");
            if (debug) {
                ex.printStackTrace();
            }

        }// catch()

        return lenWritten;

    }// write()

    public long writeDirect(long startOff, long len) {

        long lenWritten = 0;

        try {

            lenWritten = inStream.getChan().transferTo(startOff, len, chan);
            pos += lenWritten;

        } catch (IOException ex) {

            System.out.println("Error - unable to transfer specified bytes from srcfile");
            if (debug) {
                ex.printStackTrace();
            }

        }

        return lenWritten;

    }

    public long getPos() {
        return pos;
    }

    public void setPos(long pos) {
        this.pos = pos;
    }

    public FileChannel getChan() {
        return chan;
    }

    public void setChan(FileChannel chan) {
        this.chan = chan;
    }

    public void setInputStream(StreamReader inStream) {
        this.inStream = inStream;
    }

    public StreamReader getInputStream() {
        return inStream;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

}// StreamWriter
