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
 * file name  : VideoTag.java
 * authors    : Jon Keys
 * created    : June 28, 2007, 1:40 PM
 * copyright  : Sony Digital Authoring Services
 *
 * modifications:
 * Date:        Name:           Description:
 * ----------   --------------- ----------------------------------------------
 * June 28, 2007    Jon Keys         Creation
 */

package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.tags;

import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.BufferHelper;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.IOHelper;

/**
 * 
 * @author Jon Keys
 */
public class VideoTag extends FlvTag {

    public static final int H263VIDEOPACKET = 2;
    public static final int SCREENVIDEOPACKET = 3;
    public static final int ON2VP6 = 4;
    public static final int KEYFRAME = 1;
    public static final int INTERFRAME = 2;
    public static final int DISPOSABLEINTERFRAME = 3;

    private BufferHelper bh;
    private int pos;
    private String bits;

    private int width;
    private int height;
    private long byteOffset;
    private int codecIdFrameType;
    private int frameType;
    private int codecId;

    /** Creates a new instance of VideoTag */
    public VideoTag() {

        bh = new BufferHelper();
        pos = 0;
        bits = null;
        width = 0;
        height = 0;
        byteOffset = 0;
        codecIdFrameType = 0;
        frameType = 0;
        codecId = 0;

    }

    public VideoTag(IOHelper ioh) {

        super(ioh);

        bh = ioh.getBufferHelper();
        pos = 0;

        codecIdFrameType = bh.readInt(super.getData(), pos, 1);
        pos += 1;

        frameType = codecIdFrameType >> 4;
        codecId = codecIdFrameType & 0xf;

        bits = padBitSequence(bh.readBinaryString(super.getData(), pos, 9));
        super.clearData();
        pos += 9;

        if (codecId == H263VIDEOPACKET) {

            int hwCheck = bh.bit2uint(bits.substring(30, 33).toCharArray());
            width = findWidth(hwCheck);
            height = findHeight(hwCheck);

        } else if (codecId == SCREENVIDEOPACKET) {

            width = bh.bit2uint(bits.substring(4, 16).toCharArray());
            height = bh.bit2uint(bits.substring(16, 28).toCharArray());

        }

    }// VideoTag()

    private String padBitSequence(String bitSrc) {

        String bitSeq = bitSrc;
        int pad = 72 - bitSeq.length();

        if (pad > 0) {
            for (int i = 0; i < pad; i++) {
                bitSeq = "0" + bitSeq;
            }
        }

        return bitSeq;

    }// padBitSequence()

    private int findWidth(int hwCheck) {

        int width = 0;

        switch (hwCheck) {

        case 0:
            width = bh.bit2uint(bits.substring(33, 41).toCharArray());
            break;

        case 1:
            width = bh.bit2uint(bits.substring(33, 49).toCharArray());
            break;

        case 2:
            width = 352;
            break;

        case 3:
            width = 176;
            break;

        case 4:
            width = 128;
            break;

        case 5:
            width = 320;
            break;

        case 6:
            width = 160;
            break;

        }

        return width;

    }// getWidth()

    private int findHeight(int hwCheck) {

        int height = 0;

        switch (hwCheck) {

        case 0:
            height = bh.bit2uint(bits.substring(41, 49).toCharArray());
            break;

        case 1:
            height = bh.bit2uint(bits.substring(49, 65).toCharArray());
            break;

        case 2:
            height = 288;
            break;

        case 3:
            height = 144;
            break;

        case 4:
            height = 96;
            break;

        case 5:
            height = 240;
            break;

        case 6:
            height = 120;
            break;

        }

        return height;

    }// findHeight()

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getCodecIdFrameType() {
        return codecIdFrameType;
    }

    public int getFrameType() {
        return frameType;
    }

    public int getCodecId() {
        return codecId;
    }

    public long getByteOffset() {
        return byteOffset;
    }

    public void setByteOffset(long byteOffset) {
        this.byteOffset = byteOffset;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setCodecIdFrameType(int codecIdFrameType) {
        this.codecIdFrameType = codecIdFrameType;
    }

    public void setFrameType(int frameType) {
        this.frameType = frameType;
    }

    public void setCodecId(int codecId) {
        this.codecId = codecId;
    }

}// VideoTag
