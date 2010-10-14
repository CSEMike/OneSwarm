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
 * file name  : IOHelper.java
 * authors    : Jon Keys
 * created    : June 28, 2007, 10:16 AM
 * copyright  : Sony Digital Authoring Services
 *
 * modifications:
 * Date:        Name:           Description:
 * ----------   --------------- ----------------------------------------------
 * June 28, 2007    Jon Keys         Creation
 */

package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io;

import java.io.File;

import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata.FlvHeader;

/**
 *
 * @author Jon Keys
 */
public class IOHelper {

    private StreamReader inStream;
    private StreamWriter outStream;
    private FileReader fh;
    private FileWriter fw;

    private boolean debug;
    private BufferHelper bufh;
    private ByteHelper bh;

    /**
     * Creates a new instance of IOHelper
     */
    public IOHelper(File srcFile) {

        debug = false;

        inStream = new StreamReader(srcFile);
        outStream = new StreamWriter(new File(srcFile.getParentFile(), "out_" + srcFile.getName()));

        fh = new FileReader();
        fh.setStream(inStream);
        bh = new ByteHelper();
        bufh = new BufferHelper();

    }
    
    public IOHelper(byte[] array, byte[] newArray){
    	   debug = false;
          
           inStream = new ByteStreamReader(array);
           outStream = new ByteStreamWriter(newArray,array);

           fh = new FileReader();
           fh.setStream(inStream);
           bh = new ByteHelper();
           bufh = new BufferHelper();
    }

    public void closeAll(){
        inStream.close();
        outStream.close();
    }

    public FileReader getFileReader(){
        return fh;
    }

    public FileWriter getFileWriter(FlvHeader flvh){
        fw = new FileWriter(flvh);
        fw.setStream(outStream);
        return fw;
    }

    public StreamWriter getOutStream() {
		return outStream;
	}

	public ByteHelper getByteHelper(){
        return this.bh;
    }

    public BufferHelper getBufferHelper(){
        this.bufh.reset();
        return this.bufh;
    }

    public void setOutFile(String outfile){
        outStream = null;
        outStream = new StreamWriter(new File(outfile));
    }

    public boolean isDebug(){
        return this.debug;
    }

    public void setDebug(boolean debug){
        this.debug = debug;

        try{
            outStream.setDebug(debug);
            inStream.setDebug(debug);
            fw.setDebug(debug);
            fh.setDebug(debug);
            bufh.setDebug(debug);
            bh.setDebug(debug);
        }catch(Exception e){
            //do nothing -- vars may not be initialized yet
        }

    }//setDebug()

}//IOHelper
