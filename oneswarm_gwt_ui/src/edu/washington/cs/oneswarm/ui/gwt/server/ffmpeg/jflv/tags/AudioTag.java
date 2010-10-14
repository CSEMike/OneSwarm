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
 * file name  : AudioTag.java
 * authors    : Jon Keys
 * created    : June 28, 2007, 11:50 AM
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
public class AudioTag extends FlvTag{

    public static final int UNCOMPRESSED = 0;
    public static final int ADPCM = 1;
    public static final int MP3 = 2;
    public static final int NELLYMOSER8KHZMONO = 5;
    public static final int NELLYMOSER = 6;
    public static final int MONO = 0;
    public static final int STEREO = 1;

    private BufferHelper bh;

    private int soundFormat;
    private int soundRate;
    private int soundSampleSize;
    private int soundType;

    /** Creates a new instance of AudioTag */
    public AudioTag(){
        bh = new BufferHelper();
        soundFormat = 0;
        soundRate = 0;
        soundSampleSize = 0;
        soundType = 0;
    }

    public AudioTag(IOHelper ioh) {

        super(ioh);
        bh = ioh.getBufferHelper();

        String bits = padBitSequence(bh.readUint(super.getData(), 0, 1));
        super.clearData();

        soundFormat = bh.bit2uint(bits.substring(0,4).toCharArray());
        soundSampleSize = ((bh.bit2uint(bits.substring(6,7).toCharArray())) * 8) + 8;

        if(soundFormat == NELLYMOSER8KHZMONO){
            soundRate = 8000;
            soundType = MONO;
        }else{
            soundRate = findSoundRate(bh.bit2uint(bits.substring(4,6).toCharArray()));
            soundType = bh.bit2uint(bits.substring(7,8).toCharArray());
            //System.out.println("sound type : " + soundType);
        }

    }//AudioTag()

    private String padBitSequence(int bitSrc){

        String bitSeq = Integer.toBinaryString(bitSrc);
        int pad = 8 - bitSeq.length();

        if(pad > 0){
            for(int i=0;i<pad;i++){
                bitSeq = "0" + bitSeq;
            }
        }

        return bitSeq;

    }//padBitSequence()

    private int findSoundRate(int rateSwtch){

        int realRate = 0;

        switch(rateSwtch){

            case 0:
                realRate = 5500;
                break;

            case 1:
                realRate = 11000;
                break;

            case 2:
                realRate = 22000;
                break;

            case 3:
                realRate = 44000;
                break;
        }

        return realRate;

    }//findSoundRate()

    public int getSoundFormat() {
        return soundFormat;
    }

    public int getSoundRate() {
        return soundRate;
    }

    public int getSoundSampleSize() {
        return soundSampleSize;
    }

    public int getSoundType() {
        return soundType;
    }

    public void setSoundFormat(int soundFormat) {
        this.soundFormat = soundFormat;
    }

    public void setSoundRate(int soundRate) {
        this.soundRate = soundRate;
    }

    public void setSoundSampleSize(int soundSampleSize) {
        this.soundSampleSize = soundSampleSize;
    }

    public void setSoundType(int soundType) {
        this.soundType = soundType;
    }

}//AudioTag
