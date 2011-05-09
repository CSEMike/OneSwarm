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
 * file name  : TagDescriptor.java
 * authors    : Jon Keys
 * created    : June 28, 2007, 3:59 PM
 * copyright  : Sony Digital Authoring Services
 *
 * modifications:
 * Date:        Name:           Description:
 * ----------   --------------- ----------------------------------------------
 * June 28, 2007    Jon Keys         Creation
 */

package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata;

/**
 * 
 * @author Jon Keys
 */
public class TagStor {

    private int type;
    private int dataSize;
    private long timestamp;
    private boolean isNewTag;
    private Object tag;

    /** Creates a new instance of TagDescriptor */
    public TagStor() {
        type = 0;
        dataSize = 0;
        timestamp = 0;
        tag = null;
        isNewTag = false;
    }

    public TagStor(int type, int dataSize, long timestamp, Object tag) {

        this.type = type;
        this.dataSize = dataSize;
        this.timestamp = timestamp;
        this.tag = tag;
        isNewTag = false;

    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getDataSize() {
        return dataSize;
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

    public Object getTag() {
        return tag;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public boolean isNew() {
        return isNewTag;
    }

    public void setIsNew(boolean isNewTag) {
        this.isNewTag = isNewTag;
    }

}// TagStor
