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
 * file name  : BufferHelper.java
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

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata.AMFObject;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata.AMFTime;

/**
 * 
 * @author Jon Keys
 */
public class BufferHelper {

    private byte[] mbb;
    private int pos;
    private boolean debug;
    private String str;
    private byte[] buf;

    /** Creates a new instance of BufferHelper */
    public BufferHelper() {
        mbb = null;
        pos = 0;
        debug = false;
        str = null;
        buf = null;
    }

    public void reset() {
        mbb = null;
        pos = 0;
        str = null;
        buf = null;
    }

    public ByteBuffer byte2buffer(byte[] bytes) {

        ByteBuffer bbuf = ByteBuffer.allocate(bytes.length);
        bbuf.put(bytes);
        bbuf.rewind();

        return bbuf;

    }// byte2buffer()

    public int bit2uint(char[] bits) {

        int uint = 0;

        for (int i = 0; i < bits.length; i++) {
            if (bits[i] == '1') {
                uint += Math.pow(2, (bits.length - i - 1));
            }
        }

        return uint;

    }// bit2uint

    // read uint from existing byte[]
    public int readUint(byte[] mpb, int start, int len) {

        int uint = 0;

        for (int i = 0; i < len; i++) {
            uint += (mpb[i + start] & 0xFF) << ((len - i - 1) * 8);
        }

        return uint;

    }// readUint()

    // read int from existing byte[]
    public int readInt(byte[] mpb, int start, int len) {

        int uint = 0;

        for (int i = 0; i < len; i++) {
            uint += mpb[i + start];
        }

        return uint;

    }// readUint()

    // read binary string from existing byte[]
    public String readBinaryString(byte[] mpb, int start, int len) {

        buf = new byte[len];
        System.arraycopy(mpb, start, buf, 0, len);

        return new BigInteger(buf).toString(2);

    } // readBinaryString()

    // read String from existing byte[]
    public String readString(byte[] mpb, int start, int len) {

        buf = new byte[len];
        str = null;

        try {

            System.arraycopy(mpb, start, buf, 0, len);
            str = new String(buf);
            buf = null;

        } catch (Exception e) {

            System.out.println("Error - could not read string from given bytes");
            if (debug) {
                e.printStackTrace();
            }
            str = "";

        }

        return str;

    }// readString()

    // read uint from existing byte[]
    public double readDouble(byte[] mpb, int start, int len) {

        ByteBuffer bbuf = ByteBuffer.allocate(len);
        buf = new byte[len];
        System.arraycopy(mpb, start, buf, 0, len);
        bbuf.put(buf);
        bbuf.rewind();
        buf = null;

        return bbuf.getDouble();

    }// readDouble()

    public void reverseByteArray(byte[] b) {

        int left = 0;
        int right = b.length - 1;

        while (left < right) {

            byte temp = b[left];
            b[left] = b[right];
            b[right] = temp;

            left++;
            right--;

        }// while

    }// reverse

    public Object getAMFData() {

        int amfSwtch = readUint(mbb, pos, 1);
        pos += 1;

        return getAMFData(amfSwtch);

    }// getAMFData()

    public Object getAMFData(int amfSwtch) {

        Object amfData = null;

        switch (amfSwtch) {

        case 0:
            amfData = getAMFDouble();
            break;

        case 1:
            amfData = getAMFBoolean();
            break;

        case 2:
            amfData = getAMFString();
            break;

        case 3:
            amfData = getAMFObject();
            break;

        case 8:
            amfData = getAMFMixedArray();
            break;

        case 10:
            amfData = getAMFArray();
            break;

        case 11:
            amfData = getAMFTime();
            break;

        }

        return amfData;

    }// getAMFData()

    public Double getAMFDouble() {

        double dbl = readDouble(mbb, pos, 8);
        pos += 8;

        return new Double(dbl);

    }// getAMFDouble()

    public Boolean getAMFBoolean() {

        int val = readUint(mbb, pos, 1);
        pos += 1;

        return new Boolean((val == 1));

    }// getAMFBoolean()

    public String getAMFString() {

        int bytes2read = readUint(mbb, pos, 2);
        pos += 2;

        String str = readString(mbb, pos, bytes2read);
        pos += bytes2read;

        return str;

    }// getAMFString()

    public AMFObject getAMFObject() {

        AMFObject amfObj = new AMFObject();

        String key = "";
        int type = 0;

        do {

            if (pos >= mbb.length) {
                break;
            }

            key = getAMFString();
            type = readUint(mbb, pos, 1);
            pos += 1;

            amfObj.put(key, getAMFData(type));

        } while (!(key.length() < 1 && type == 9));

        return amfObj;

    }// getAMFObject()

    public HashMap<String, Object> getAMFMixedArray() {

        // just skip 4 bytes
        pos += 4;

        HashMap<String, Object> amfMap = new HashMap<String, Object>();

        String key = "";
        int type = 0;

        do {

            if (pos >= mbb.length) {
                break;
            }

            key = getAMFString();
            type = readUint(mbb, pos, 1);
            pos += 1;

            amfMap.put(key, getAMFData(type));

        } while (!(key.length() < 1 && type == 9));

        return amfMap;

    }// getAMFMixedArray()

    public ArrayList<Object> getAMFArray() {

        int size = readUint(mbb, pos, 4);
        pos += 4;

        ArrayList<Object> afmArray = new ArrayList<Object>();

        for (int i = 0; i < size; i++) {
            afmArray.add(getAMFData());
        }

        return afmArray;

    }// getAMFArray()

    public AMFTime getAMFTime() {

        // get time in milliseconds
        long time = (long) getAMFDouble().doubleValue();

        byte[] buf = new byte[2];
        System.arraycopy(mbb, pos, buf, 0, 2);
        pos += 2;

        reverseByteArray(buf);

        // get gmt offset in milliseconds
        int gmtOff = 0;
        for (int i = 0; i < 2; i++) {
            gmtOff += (buf[i] & 0xFF) << ((1 - i) * 8);
        }
        buf = null;
        int gmt = gmtOff * 60 * 1000;

        return new AMFTime(time, gmt);

    }// getAMFTime()

    public void clearData() {
        this.mbb = null;
    }

    public byte[] getBuffer() {
        return mbb;
    }

    public void setBuffer(byte[] mbb) {
        this.mbb = mbb;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

}// BufferHelper
