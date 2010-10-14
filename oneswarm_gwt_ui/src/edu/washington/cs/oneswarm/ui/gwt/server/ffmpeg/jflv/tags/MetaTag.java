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
 * file name  : MetaTag.java
 * authors    : Jon Keys
 * created    : June 28, 2007, 12:16 PM
 * copyright  : Sony Digital Authoring Services
 *
 * modifications:
 * Date:        Name:           Description:
 * ----------   --------------- ----------------------------------------------
 * June 28, 2007    Jon Keys         Creation
 */

package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.tags;

import java.nio.ByteBuffer;
import java.util.HashMap;

import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.BufferHelper;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.ByteHelper;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.IOHelper;

/**
 *
 * @author Jon Keys
 */
public class MetaTag extends FlvTag {

    private byte[] mbb;
    private ByteHelper bh;
    private BufferHelper bufh;

    private Object event;
    private Object metaData;

    /** Creates a new instance of MetaTag */
    public MetaTag(){
        mbb = null;
        bh = new ByteHelper();
        bufh = new BufferHelper();
        event = null;
        metaData = null;
    }

    public MetaTag(IOHelper ioh) {

        super(ioh);

        bh = ioh.getByteHelper();
        bufh = ioh.getBufferHelper();
        bufh.setBuffer(super.getData());
        super.clearData();

        event = bufh.getAMFData();
        metaData = bufh.getAMFData();
        bufh.clearData();

        mbb = null;

    }//MetaTag()

    private byte[] getEventBytes(){

        String eventStr = (String)event;

        return bh.getAMFStringBytes(eventStr);

    }//getEventBytes()

    private byte[] getMetaDataBytes(){

        HashMap<String,Object> mixArray = (HashMap<String,Object>)metaData;

        return bh.getAMFDataBytes(mixArray);

    }//getMetaDataBytes()


    public int getDataSizeFromBuffer() {
        return (getDataAsBuffer().capacity() + 15);
    }

    public ByteBuffer getDataAsBuffer(){

        byte[] eventBytes = getEventBytes();
        byte[] metaBytes = getMetaDataBytes();

        byte[] totBytes = new byte[eventBytes.length + metaBytes.length];

        System.arraycopy(eventBytes,0,totBytes,0,eventBytes.length);
        System.arraycopy(metaBytes,0,totBytes,eventBytes.length,metaBytes.length);

        eventBytes = null;
        metaBytes = null;

        return bufh.byte2buffer(totBytes);

    }//getData()

    public Object getEvent() {
        return event;
    }

    public Object getMetaData() {
        return metaData;
    }

    public void setEvent(Object event) {
        this.event = event;
    }

    public void setMetaData(Object metaData) {
        this.metaData = metaData;
    }

}//MetaTag
