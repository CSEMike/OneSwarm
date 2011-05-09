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
 * file name  : FileWriter.java
 * authors    : Jon Keys
 * created    : July 5, 2007, 9:39 AM
 * copyright  : Sony Digital Authoring Services
 *
 * modifications:
 * Date:        Name:           Description:
 * ----------   --------------- ----------------------------------------------
 * July 5, 2007    Jon Keys         Creation
 */

package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata.FlvHeader;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata.TagStor;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.tags.FlvTag;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.tags.MetaTag;

/**
 * 
 * @author Jon Keys
 */
public class FileWriter {

    private StreamWriter stream;
    private StreamReader inStream;
    private ByteBuffer buf;
    private ArrayList<TagStor> tags;
    private FlvHeader flvh;
    private boolean debug;

    /** Creates a new instance of FileWriter */
    public FileWriter(FlvHeader flvh) {
        stream = null;
        buf = null;
        tags = null;
        this.flvh = flvh;
        debug = false;
    }

    public void writeTags() {

        BufferHelper bufh = new BufferHelper();
        ByteHelper bh = new ByteHelper();
        FlvTag tmpFlvt = null;

        int fpos = 0;
        byte[] buf = null;
        byte[] type = null;
        byte[] dsize = null;
        byte[] tstamp = null;
        byte[] prevTagSize = null;
        byte[] ender = bh.getUintBytes(0, 4);

        try {

            stream.open();

        } catch (Exception ex) {

            System.out.println("Error - unable to open specified output file");
            if (debug) {
                ex.printStackTrace();
            }
            return;

        }// catch()

        // write FLV header
        stream.write(bufh.byte2buffer(flvh.getFlvHeaderBytes()));
        stream.write(bufh.byte2buffer(ender));
        stream.setInputStream(inStream);

        int cntr = 1;

        for (TagStor ts : tags) {

            type = bh.getUintBytes(ts.getType(), 1);
            dsize = bh.getUintBytes(ts.getDataSize(), 3);
            tstamp = bh.getUintBytes(ts.getTimestamp(), 3);

            if (prevTagSize != null) {
                buf = new byte[type.length + dsize.length + tstamp.length + ender.length
                        + prevTagSize.length];
                System.arraycopy(prevTagSize, 0, buf, 0, prevTagSize.length);
                fpos += prevTagSize.length;
            } else {
                buf = new byte[type.length + dsize.length + tstamp.length + ender.length];
            }

            System.arraycopy(type, 0, buf, fpos, type.length);
            fpos += type.length;
            System.arraycopy(dsize, 0, buf, fpos, dsize.length);
            fpos += dsize.length;
            System.arraycopy(tstamp, 0, buf, fpos, tstamp.length);
            fpos += tstamp.length;
            System.arraycopy(ender, 0, buf, fpos, ender.length);

            prevTagSize = null;
            type = null;
            dsize = null;
            tstamp = null;

            stream.write(bufh.byte2buffer(buf));
            if (ts.getType() == FlvTag.META && ts.isNew()) {
                stream.write(((MetaTag) ts.getTag()).getDataAsBuffer());
                prevTagSize = bh.getUintBytes(
                        (((MetaTag) ts.getTag()).getDataSizeFromBuffer() - 4), 4);
                // System.out.println("writing meta tag");
            } else {
                tmpFlvt = (FlvTag) ts.getTag();
                stream.writeDirect(tmpFlvt.getStartingOffset(), (tmpFlvt.getDataSize() - 15));
                prevTagSize = bh.getUintBytes((tmpFlvt.getDataSize() - 4), 4);
                // System.out.println("writing other tag");
            }

            buf = null;
            fpos = 0;

        }// for

        if (prevTagSize != null) {
            stream.write(bufh.byte2buffer(prevTagSize));
        }

    }// writeTags()

    public void setStream(StreamWriter stream) {
        this.stream = stream;
    }

    public void setTags(ArrayList<TagStor> tags) {
        this.tags = tags;
    }

    public void setInStream(StreamReader inStream) {
        this.inStream = inStream;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

}// FileWriter()
