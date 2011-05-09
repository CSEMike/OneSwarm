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
 * file name  : FlvTag.java
 * authors    : Jon Keys
 * created    : June 28, 2007, 9:35 AM
 * copyright  : Sony Digital Authoring Services
 *
 * modifications:
 * Date:        Name:           Description:
 * ----------   --------------- ----------------------------------------------
 * June 28, 2007    Jon Keys         Creation
 */

package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.tags;

import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.BufferHelper;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.FileReader;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.IOHelper;

/**
 * 
 * @author Jon Keys
 */
public class FlvTag {

    public static final int AUDIO = 8;
    public static final int VIDEO = 9;
    public static final int META = 18;
    public static final int UNDEFINED = 0;

    private FileReader fh;
    private BufferHelper bh;

    private int dataSize;
    private long startingOffset;
    private long timestamp;
    private byte[] mbb;

    /** Creates a new instance of FlvTag */
    public FlvTag() {
        fh = null;
        bh = null;
        dataSize = 0;
        timestamp = 0;
        mbb = null;
    }

    public FlvTag(IOHelper ioh) {

        fh = ioh.getFileReader();
        fh.setDebug(ioh.isDebug());
        bh = ioh.getBufferHelper();

        mbb = fh.readByteArray(6);
        dataSize = bh.readUint(mbb, 0, 3);
        timestamp = bh.readUint(mbb, 3, 3);
        mbb = null;

        fh.skip(4);
        startingOffset = fh.getPos();
        mbb = fh.readByteArray(dataSize);

    }// FlvTag()

    public void clearData() {
        this.mbb = null;
    }

    public int getDataSize() {
        // 11 bytes for header plus 4 skipped bytes
        return dataSize + 15;
    }

    public void setDataSize(int dataSize) {
        this.dataSize = dataSize;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getData() {
        return mbb;
    }

    public void setData(byte[] mbb) {
        this.mbb = mbb;
    }

    public long getStartingOffset() {
        return startingOffset;
    }

    public void setStartingOffset(long startingOffset) {
        this.startingOffset = startingOffset;
    }

}// FlvTag
