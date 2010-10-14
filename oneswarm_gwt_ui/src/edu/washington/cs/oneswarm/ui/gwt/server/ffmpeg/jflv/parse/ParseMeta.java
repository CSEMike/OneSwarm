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
 * file name  : PrintMeta.java
 * authors    : Jon Keys
 * created    : July 9, 2007, 1:15 PM
 * copyright  : Sony Digital Authoring Services
 *
 * modifications:
 * Date:        Name:           Description:
 * ----------   --------------- ----------------------------------------------
 * July 9, 2007    Jon Keys         Creation
 */

package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.parse;

import java.util.HashMap;

import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.BufferHelper;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.FileReader;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.IOHelper;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata.EmbeddedData;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.tags.FlvTag;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.tags.MetaTag;

/**
 *
 * @author Jon Keys
 */
public class ParseMeta {

    private IOHelper ioh;
    private FileReader fh;
    private BufferHelper bh;
    private MetaTag mt;
    private FlvTag ft;
    private boolean didFind;
    private boolean debug;

    /**
     * Creates a new instance of ParseMeta
     */
    public ParseMeta(IOHelper ioh) {

        this.ioh = ioh;
        didFind = false;
        mt = null;
        fh = ioh.getFileReader();
        fh.setDebug(ioh.isDebug());
        bh = ioh.getBufferHelper();
        debug = ioh.isDebug();

    }

    // metadata getter
    public HashMap<String,Object> getMetaData(){
        return mt == null ? null : (HashMap<String,Object>)mt.getMetaData();
    }

    public void findMetaTag(){

        int tagType = 0;

        while(true){

            byte[] mbb = fh.readByteArray(5);

            if(mbb == null){
                //System.out.println("mbb null");
                break;
            }

            tagType = bh.readUint(mbb, 4, 1);

            if(tagType == FlvTag.META){

                mt = new MetaTag(ioh);

                if(mt.getEvent().equals("onMetaData")){
                    didFind = true;
                    break;
                }

            }else{

                //advance file position properly
                ft = new FlvTag(ioh);
                ft = null;

            }//else

        }//while

    }//findMetaTag()

    public void printMetaData(){

        if(!didFind){

            System.out.println("no metadata has been embedded");

        }else{

            EmbeddedData emb = new EmbeddedData();

            try{

                emb.setData((HashMap<String,Object>)mt.getMetaData());
                System.out.println(emb.printMetaData());

            }catch(Exception e){

                System.out.println("An error occurred while parsing metadata ... try re-embedding");
                if(debug){e.printStackTrace();}

            }

        }//else

    }//printMetaData()

}//ParseMeta
