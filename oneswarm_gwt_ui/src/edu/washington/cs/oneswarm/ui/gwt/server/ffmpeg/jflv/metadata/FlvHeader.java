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
 * file name  : FlvHeader.java
 * authors    : Jon Keys
 * created    : June 28, 2007, 2:14 PM
 * copyright  : Sony Digital Authoring Services
 *
 * modifications:
 * Date:        Name:           Description:
 * ----------   --------------- ----------------------------------------------
 * June 28, 2007    Jon Keys         Creation
 */

package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata;

import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.ByteHelper;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.FileReader;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.IOHelper;

/**
 *
 * @author Jon Keys
 */
public class FlvHeader {

    private String signature;
    private int version;
    private boolean containsAudio;
    private boolean containsVideo;
    private int dataOffset;
    private String extraData;
    private IOHelper ioh;

    /** Creates a new instance of FlvHeader */
    public FlvHeader(){

        signature = null;
        version = 0;
        containsAudio = false;
        containsVideo = false;
        dataOffset = 0;
        extraData = null;

    }

    public FlvHeader(IOHelper ioh) {

        this.ioh = ioh;

        FileReader fh = ioh.getFileReader();
        fh.setDebug(ioh.isDebug());

        signature = fh.readString(3);
        version = fh.readUint(1);

        int typeFlags = fh.readUint(1);
        containsAudio = ((typeFlags & 4) == 1);
        containsVideo = ((typeFlags & 1) == 1);

        dataOffset = fh.readUint(4);

        if((dataOffset - 9) > 0){
            extraData = fh.readString((dataOffset - 9));
        }else{
            extraData = "";
        }

    }//FlvHeader()

    public byte[] getFlvHeaderBytes(){

        int fpos = 0;

        int typeFlags = 0;
        if(containsAudio){typeFlags += 4;}
        if(containsVideo){typeFlags += 1;}

        ByteHelper bh = ioh.getByteHelper();

        byte[] flv = new String("FLV").getBytes();
        byte[] typ = bh.getUintBytes(1, 1);
        byte[] typFlag = bh.getUintBytes(typeFlags, 1);
        byte[] extDataLen = bh.getUintBytes((9+extraData.length()),4);
        byte[] extData = extraData.getBytes();

        byte[] flvhBytes = new byte[flv.length + typ.length + typFlag.length + extDataLen.length + extData.length];

        System.arraycopy(flv,0,flvhBytes,0,flv.length);
        fpos += flv.length;
        System.arraycopy(typ,0,flvhBytes,fpos,typ.length);
        fpos += typ.length;
        System.arraycopy(typFlag,0,flvhBytes,fpos,typFlag.length);
        fpos += typFlag.length;
        System.arraycopy(extDataLen,0,flvhBytes,fpos,extDataLen.length);
        fpos += extDataLen.length;
        System.arraycopy(extData,0,flvhBytes,fpos,extData.length);

        flv = null;
        typ = null;
        typFlag = null;
        extDataLen = null;
        extData = null;

        return flvhBytes;

    }//writeFlvHeader()

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean hasAudio() {
        return containsAudio;
    }

    public void setHasAudio(boolean containsAudio) {
        this.containsAudio = containsAudio;
    }

    public boolean hasVideo() {
        return containsVideo;
    }

    public void setHasVideo(boolean containsVideo) {
        this.containsVideo = containsVideo;
    }

    public int getDataOffset() {
        return dataOffset;
    }

    public void setDataOffset(int dataOffset) {
        this.dataOffset = dataOffset;
    }

    public String getExtraData() {
        return extraData;
    }

    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

}//FlvHeader
