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
 * file name  : ByteHelper.java
 * authors    : Jon Keys
 * created    : July 3, 2007, 5:34 PM
 * copyright  : Sony Digital Authoring Services
 *
 * modifications:
 * Date:        Name:           Description:
 * ----------   --------------- ----------------------------------------------
 * July 3, 2007    Jon Keys         Creation
 */

package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata.AMFObject;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata.AMFTime;

/**
 *
 * @author Jon Keys
 */
public class ByteHelper {

    private boolean debug;
    private int numshift;

    /** Creates a new instance of ByteHelper */
    public ByteHelper() {
        debug = false;
        numshift = 0;
    }

    public byte[] getUintBytes(int val, int len){

        byte[] numBytes = new byte[len];
        numshift = 0;

        for(int i=0;i<len;i++){

            numshift = ((len -1 -i) * 8);
            numBytes[i] = (byte)((val >> numshift) & 0xFF);

        }//for

        return numBytes;

    }//getUintBytes()

    //accomadate long vars
    public byte[] getUintBytes(long val, int len){

        byte[] numBytes = new byte[len];
        numshift = 0;

        for(int i=0;i<len;i++){

            numshift = ((len -1 -i) * 8);
            numBytes[i] = (byte)((val >> numshift) & 0xFF);

        }//for

        return numBytes;

    }//getUintBytes()

    public byte[] getAMFDataBytes(Object obj){

        byte[] amfBytes = null;
        String objClass = obj.getClass().getName();

        if(objClass.endsWith("String")){
            amfBytes = getAMFStringBytes((String)obj);
        }else if(objClass.endsWith("Double")){
            amfBytes = getAMFDoubleBytes(((Double)obj).doubleValue());
        }else if(objClass.endsWith("Boolean")){
            amfBytes = getAMFBooleanBytes(((Boolean)obj).booleanValue());
        }else if(objClass.endsWith("HashMap")){
            amfBytes = getAMFMixedArrayBytes((HashMap<String,Object>)obj);
        }else if(objClass.endsWith("ArrayList")){
            amfBytes = getAMFArrayBytes((ArrayList<Object>)obj);
        }else if(objClass.endsWith("AMFTime")){
            amfBytes = getAMFTimeBytes((AMFTime)obj);
        }else if(objClass.endsWith("AMFObject")){
            amfBytes = getAMFObjectBytes((AMFObject)obj);
        }

        //System.out.println("wrote data : " + amfBytes.length);

        return amfBytes;

    }//getAMFDataBytes()

    public byte[] getAMFDoubleBytes(Double dblObj){

        double dbl = dblObj.doubleValue();
        byte[] dblHead = getUintBytes(0, 1);
        byte[] dblBytes = null;

        try{

            ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
            DataOutputStream datastream = new DataOutputStream(bytestream);
            datastream.writeDouble(dbl);
            datastream.flush();
            dblBytes = bytestream.toByteArray();

        }catch(Exception e){

            System.out.println("Error - could not read double from given bytes");
            if(debug){e.printStackTrace();}
            dblBytes = new byte[0];

        }

        byte[] totBytes = new byte[dblHead.length + dblBytes.length];

        System.arraycopy(dblHead,0,totBytes,0,dblHead.length);
        System.arraycopy(dblBytes,0,totBytes,dblHead.length,dblBytes.length);
        dblHead = null;
        dblBytes = null;

        //System.out.println("wrote double : " + totBytes.length);

        return totBytes;

    }//getAMFDoubleBytes()

    public byte[] getAMFBooleanBytes(Boolean boolObj){

        boolean bool = boolObj.booleanValue();

        byte[] boolHead = getUintBytes(1, 1);
        byte[] boolBytes;

        if(bool){
           boolBytes = getUintBytes(1, 1);
        }else{
            boolBytes = getUintBytes(0, 1);
        }

        byte[] totBytes = new byte[boolHead.length + boolBytes.length];

        System.arraycopy(boolHead,0,totBytes,0,boolHead.length);
        System.arraycopy(boolBytes,0,totBytes,boolHead.length,boolBytes.length);

        boolHead = null;
        boolBytes = null;

        //System.out.println("wrote boolean : " + totBytes.length);

        return totBytes;

    }//getAMFBooleanBytes()

    public byte[] getAMFStringBytes(String str){

        byte[] strHead = getUintBytes(2, 1);
        byte[] strlen = getUintBytes(str.length(), 2);
        byte[] strBytes = str.getBytes();

        byte[] totBytes = new byte[strHead.length + strlen.length + strBytes.length];

        System.arraycopy(strHead,0,totBytes,0,strHead.length);
        System.arraycopy(strlen,0,totBytes,strHead.length,strlen.length);
        System.arraycopy(strBytes,0,totBytes,strlen.length + strHead.length,strBytes.length);

        strHead = null;
        strlen = null;
        strBytes = null;

        //System.out.println("wrote string '" + str + "': " + totBytes.length);

        return totBytes;

    }//getAMFStringBytes

    public byte[] getAMFObjectBytes(AMFObject amfObj){

        int totalObjectBytes = 0;
        ArrayList<byte[]> keyArrays = new ArrayList<byte[]>();
        ArrayList<byte[]> valArrays = new ArrayList<byte[]>();

        byte[] objHead = getUintBytes(3, 1);
        totalObjectBytes += objHead.length;

        byte[] keyBytes;
        byte[] keyLenBytes;

        int mapsize = amfObj.size();
        Iterator keyValuePairs = amfObj.entrySet().iterator();

	for(int i=0;i<mapsize;i++){

	    Map.Entry entry = (Map.Entry)keyValuePairs.next();
	    String key = (String)entry.getKey();

            keyLenBytes = getUintBytes(key.length(), 2);
            keyBytes = key.getBytes();

            byte[] keyTotBytes = new byte[keyLenBytes.length + keyBytes.length];
            //System.out.println("key '" + key + "' bytes : " + keyTotBytes.length);

            System.arraycopy(keyLenBytes,0,keyTotBytes,0,keyLenBytes.length);
            System.arraycopy(keyBytes,0,keyTotBytes,keyLenBytes.length,keyBytes.length);
            keyLenBytes = null;
            keyBytes = null;

            keyArrays.add(keyTotBytes);
            valArrays.add(getAMFDataBytes(entry.getValue()));

            totalObjectBytes += keyTotBytes.length;
            totalObjectBytes += valArrays.get(valArrays.size()-1).length;

	}//for

        byte[] objTail = getUintBytes(0, 2);
        totalObjectBytes += objTail.length;
        byte[] objClose = getUintBytes(9, 1);
        totalObjectBytes += objClose.length;

        //done reading in bytes -- now copy them all into one byte array

        int curPos = 0;

        byte[] objBytes = new byte[totalObjectBytes];
        System.arraycopy(objHead,0,objBytes,curPos,objHead.length);
        curPos += objHead.length;
        objHead = null;

        for(int q=0;q<keyArrays.size();q++){
            System.arraycopy(keyArrays.get(q),0,objBytes,curPos,keyArrays.get(q).length);
            curPos += keyArrays.get(q).length;
            System.arraycopy(valArrays.get(q),0,objBytes,curPos,valArrays.get(q).length);
            curPos += valArrays.get(q).length;
        }

        keyArrays = null;
        valArrays = null;

        System.arraycopy(objTail,0,objBytes,curPos,objTail.length);
        curPos += objTail.length;
        objTail = null;

        System.arraycopy(objClose,0,objBytes,curPos,objClose.length);
        curPos += objClose.length;
        objClose = null;

        //System.out.println("wrote object : " + objBytes.length);

        return objBytes;

    }//getAMFObjectBytes()

    public byte[] getAMFMixedArrayBytes(HashMap<String,Object> mix){

        int totalArrayBytes = 0;
        ArrayList<byte[]> keyArrays = new ArrayList<byte[]>();
        ArrayList<byte[]> valArrays = new ArrayList<byte[]>();

        byte[] arrayHead = getUintBytes(8, 1);
        totalArrayBytes += arrayHead.length;

        byte[] arrayLen = getUintBytes(mix.size(), 4);
        totalArrayBytes += arrayLen.length;

        byte[] keyBytes;
        byte[] keyLenBytes;

        int mapsize = mix.size();
        Iterator keyValuePairs = mix.entrySet().iterator();

	for(int i=0;i<mapsize;i++){

	    Map.Entry entry = (Map.Entry)keyValuePairs.next();
	    String key = (String)entry.getKey();

            keyLenBytes = getUintBytes(key.length(), 2);
            keyBytes = key.getBytes();

            byte[] keyTotBytes = new byte[keyLenBytes.length + keyBytes.length];
            //System.err.println("key '" + key + "' bytes : " + keyTotBytes.length);

            System.arraycopy(keyLenBytes,0,keyTotBytes,0,keyLenBytes.length);
            System.arraycopy(keyBytes,0,keyTotBytes,keyLenBytes.length,keyBytes.length);
            keyLenBytes = null;
            keyBytes = null;

            keyArrays.add(keyTotBytes);
            valArrays.add(getAMFDataBytes(entry.getValue()));

            totalArrayBytes += keyTotBytes.length;
            totalArrayBytes += valArrays.get(valArrays.size()-1).length;

	}//for

        byte[] arrayTail = getUintBytes(0, 2);
        totalArrayBytes += arrayTail.length;
        byte[] arrayClose = getUintBytes(9, 1);
        totalArrayBytes += arrayClose.length;

        //done reading in bytes -- now copy them all into one byte array

        int curPos = 0;

        byte[] objBytes = new byte[totalArrayBytes];
        System.arraycopy(arrayHead,0,objBytes,curPos,arrayHead.length);
        curPos += arrayHead.length;
        arrayHead = null;

        System.arraycopy(arrayLen,0,objBytes,curPos,arrayLen.length);
        curPos += arrayLen.length;
        arrayLen = null;

        for(int q=0;q<keyArrays.size();q++){
            System.arraycopy(keyArrays.get(q),0,objBytes,curPos,keyArrays.get(q).length);
            curPos += keyArrays.get(q).length;
            System.arraycopy(valArrays.get(q),0,objBytes,curPos,valArrays.get(q).length);
            curPos += valArrays.get(q).length;
        }

        keyArrays = null;
        valArrays = null;

        System.arraycopy(arrayTail,0,objBytes,curPos,arrayTail.length);
        curPos += arrayTail.length;
        arrayTail = null;

        System.arraycopy(arrayClose,0,objBytes,curPos,arrayClose.length);
        curPos += arrayClose.length;
        arrayClose = null;

        //System.out.println("wrote hash : " + objBytes.length);

        return objBytes;

    }//getAMFMixedArrayBytes()

    public byte[] getAMFArrayBytes(ArrayList<Object> amfArray){

        int totalArrayBytes = 0;
        ArrayList<byte[]> valArrays = new ArrayList<byte[]>();

        byte[] arrayHead = getUintBytes(10, 1);
        totalArrayBytes += arrayHead.length;
        byte[] arrayLen = getUintBytes(amfArray.size(), 4);
        totalArrayBytes += arrayLen.length;

        for(Object obj : amfArray){
            valArrays.add(getAMFDataBytes(obj));
            totalArrayBytes += valArrays.get(valArrays.size()-1).length;
        }

        int curPos = 0;
        byte[] amfArrayBytes = new byte[totalArrayBytes];

        System.arraycopy(arrayHead,0,amfArrayBytes,curPos,arrayHead.length);
        curPos += arrayHead.length;
        arrayHead = null;

        System.arraycopy(arrayLen,0,amfArrayBytes,curPos,arrayLen.length);
        curPos += arrayLen.length;
        arrayLen = null;

        for(byte[] buf : valArrays){
            System.arraycopy(buf,0,amfArrayBytes,curPos,buf.length);
            curPos += buf.length;
        }

        valArrays = null;

        //System.out.println("wrote array : " + amfArrayBytes.length);

        return amfArrayBytes;

    }//getAMFArrayBytes()

    public byte[] getAMFTimeBytes(AMFTime dblTimeObj){

        double dblTime = dblTimeObj.getUTCTime();
        int gmt = dblTimeObj.getGMTOffset() / 60 / 1000;

        byte[] timeHead = getUintBytes(11, 1);
        byte[] gmtBytes = getUintBytes(gmt, 2);
        byte[] timeBytes = null;

        try{

            ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
            DataOutputStream datastream = new DataOutputStream(bytestream);
            datastream.writeDouble(dblTime);
            datastream.flush();
            timeBytes = bytestream.toByteArray();

        }catch(Exception e){

            System.out.println("Error - could not read time from given bytes");
            if(debug){e.printStackTrace();}
            timeBytes = new byte[0];

        }

        byte[] totBytes = new byte[timeHead.length + timeBytes.length + gmtBytes.length];

        System.arraycopy(timeHead,0,totBytes,0,timeHead.length);
        System.arraycopy(timeBytes,0,totBytes,timeHead.length,timeBytes.length);
        System.arraycopy(gmtBytes,0,totBytes,timeHead.length+timeBytes.length,gmtBytes.length);
        timeHead = null;
        timeBytes = null;
        gmtBytes = null;

        //System.out.println("wrote time : " + totBytes.length);

        return totBytes;

    }//getAMFTimeBytes()

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

}//ByteHelper
