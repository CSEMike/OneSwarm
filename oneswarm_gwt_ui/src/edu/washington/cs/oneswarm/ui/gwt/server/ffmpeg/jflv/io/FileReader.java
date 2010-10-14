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
 * file name  : FileHelper.java
 * authors    : Jon Keys
 * created    : July 3, 2007, 5:51 PM
 * copyright  : Sony Digital Authoring Services
 *
 * modifications:
 * Date:        Name:           Description:
 * ----------   --------------- ----------------------------------------------
 * July 3, 2007    Jon Keys         Creation
 */

package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.MappedByteBuffer;

/**
 *
 * @author Jon Keys
 */
public class FileReader {

    private StreamReader stream;
    private byte[] mbb;
    private boolean debug;

    /** Creates a new instance of BufferHelper */
    public FileReader() {
        stream = null;
        mbb = null;
        debug = false;
    }

    public void skip(int len){

        try{

            if(!stream.isOpen()){
                stream.open();
            }

            stream.skip(len);

        }catch(Exception e){

            System.out.println("Error - unable to skip specified bytes");
            if(debug){e.printStackTrace();}

        }//catch()

    }//skip()

    public byte[] readByteArray(int len){

        mbb = null;

        try{

            if(!stream.isOpen()){
                stream.open();
            }

            mbb = stream.read(len);

        }catch(EoflvException eolex){

            //do nothing -- this is an expected exception when eof is reached

        }catch(Exception e){

            System.out.println("Error - unable to read byte array at specified position");
            if(debug){e.printStackTrace();}
            mbb = null;

        }//catch()

        return mbb;

    }//readByteBuffer()

    public String readString(int len){

        String str = null;

        try{

            if(!stream.isOpen()){
                stream.open();
            }

            mbb = stream.read(len);
            str = new String(mbb);

        }catch(Exception e){

            System.out.println("Error - unable to read string at specified position");
            if(debug){e.printStackTrace();}
            str = "";

        }//catch()

        return str;

    }//readString()

    public int readUint(int len){

        int uint = 0;

        try{

            if(!stream.isOpen()){
                stream.open();
            }

            mbb = stream.read(len);
            for(int i=0;i<len;i++){
                uint += (mbb[i] & 0xFF) << ((len -i -1) * 8);
            }

        }catch(Exception e){

            System.out.println("Error - unable to read unsigned integer at specified position");
            if(debug){e.printStackTrace();}
            uint = 0;

        }//catch()

        return uint;

    }//readUint()

    public int readInt(int len){

        int uint = 0;

        try{

            if(!stream.isOpen()){
                stream.open();
            }

            mbb = stream.read(len);
            for(int i=0;i<len;i++){
                uint += mbb[i];
            }

        }catch(Exception e){

            System.out.println("Error - unable to read signed integer at specified position");
            if(debug){e.printStackTrace();}
            uint = 0;

        }//catch()

        return uint;

    }//readUint()

    public double readDouble(int len){

        ByteBuffer bbuf = ByteBuffer.allocate(len);
        double db = 0;

        try{

            if(!stream.isOpen()){
                stream.open();
            }

            mbb = stream.read(len);
            bbuf.put(mbb);
            db = bbuf.getDouble();
            bbuf = null;

        }catch(Exception e){

            System.out.println("Error - unable to read doubler at specified position");
            if(debug){e.printStackTrace();}
            db = 0;

        }//catch()

        return db;

    }//readDouble()



    public StreamReader getStream() {
        return stream;
    }

    public void setStream(StreamReader stream) {
        this.stream = stream;
    }

    public long getPos(){
        return stream.getPos();
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

}//FileReader
