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
 * file name  : AMFTime.java
 * authors    : Jon Keys
 * created    : July 2, 2007, 2:34 PM
 * copyright  : Sony Digital Authoring Services
 *
 * modifications:
 * Date:        Name:           Description:
 * ----------   --------------- ----------------------------------------------
 * July 2, 2007    Jon Keys         Creation
 */

package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata;

import java.util.Calendar;

/**
 *
 * @author Jon Keys
 */
public class AMFTime {

    private long localTime;
    private long utcTime;
    private int gmtOffset;

    private Calendar cal;

    /** Creates a new instance of AMFTime */
    public AMFTime() {
        localTime = System.currentTimeMillis();
        cal = Calendar.getInstance();
        cal.setTimeInMillis(localTime);
        utcTime = calcUTCTime(cal);
    }

    private long calcUTCTime(Calendar cal){
        gmtOffset = cal.get(Calendar.DST_OFFSET) - cal.get(Calendar.ZONE_OFFSET);
        cal.add(Calendar.MILLISECOND, -gmtOffset);
        return cal.getTime().getTime();
    }

    //set last time edited in milliseconds
    public AMFTime(long utcTime, int gmtOffset) {

        this.utcTime = utcTime;
        this.gmtOffset = gmtOffset;

        cal = Calendar.getInstance();
        cal.setTimeInMillis(utcTime);
        cal.add(Calendar.MILLISECOND, -gmtOffset);

        this.localTime = cal.getTime().getTime();

    }//AMFTime()

    public long getTime() {
        return localTime;
    }

    public String getTimeString(){

        StringBuffer calStr = new StringBuffer();

        cal = Calendar.getInstance();
        cal.setTimeInMillis(localTime);

        calStr.append((cal.get(Calendar.MONTH) +1) + "/");
        calStr.append(cal.get(Calendar.DAY_OF_MONTH) + "/");
        calStr.append(cal.get(Calendar.YEAR) + " ");
        calStr.append(cal.get(Calendar.HOUR) + ":");
        calStr.append(cal.get(Calendar.MINUTE) + ":");
        calStr.append(cal.get(Calendar.SECOND) + " ");
        if(cal.get(Calendar.AM_PM) == 0){
            calStr.append("AM  ");
        }else{
            calStr.append("PM  ");
        }


        return calStr.toString();

    }//getTimeString()

    //amf time is written as a double
    public double getUTCTime(){
        return (double)utcTime;
    }

    //gmt offset is written as signed int
    public int getGMTOffset(){
        return gmtOffset;
    }


}//AMFTime
