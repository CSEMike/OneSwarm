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
 * file name  : FrameSequence.java
 * authors    : Jon Keys
 * created    : June 29, 2007, 10:19 AM
 * copyright  : Sony Digital Authoring Services
 *
 * modifications:
 * Date:        Name:           Description:
 * ----------   --------------- ----------------------------------------------
 * June 29, 2007    Jon Keys         Creation
 */

package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author Jon Keys
 */
public class FrameSequence {

    private HashMap<Integer,Integer> sequenceCount;
    private int tmp;

    /** Creates a new instance of FrameSequence */
    public FrameSequence() {
        sequenceCount = new HashMap<Integer,Integer>();
        tmp = 0;
    }

    public double getFrameRate(){

        tmp = 0;
        int lastCount = 0;
        int framerate = 0;
        Iterator keyValuePairs = sequenceCount.entrySet().iterator();

	for(int i=0;i<sequenceCount.size();i++){

	    Map.Entry entry = (Map.Entry)keyValuePairs.next();
	    tmp = ((Integer)entry.getValue()).intValue();

            if(tmp > lastCount){
                framerate = ((Integer)entry.getKey()).intValue();
                lastCount = tmp;
            }

        }//for

        keyValuePairs = null;

        return (1000d / framerate);

    }//getFrameRate()

    public void addSequence(int seq){

        if(seq > 0){

            Integer sequenceVal = new Integer(seq);

            if(sequenceCount.containsKey(sequenceVal)){
                tmp = sequenceCount.get(sequenceVal).intValue() + 1;
                sequenceCount.remove(sequenceVal);
                sequenceCount.put(sequenceVal, new Integer(tmp));
            }else{
                sequenceCount.put(sequenceVal, new Integer(1));
            }

        }//if

    }//addSequence()

}//FrameSequence
