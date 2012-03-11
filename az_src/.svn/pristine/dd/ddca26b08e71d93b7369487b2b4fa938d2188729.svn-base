/**
 * Created on May 10, 2007
 * Created by Alan Snyder
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.speedtest;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTesterResult;

/**
 * Use this class to store data that should persist across panels.
 */
public class SpeedTestData {
    private static SpeedTestData ourInstance = new SpeedTestData();

    private String lastTestData;

    private NetworkAdminSpeedTesterResult lastResult;


    private int highestDownloadOnlyResult;
    private int lastUploadOnlyResult;

    public static SpeedTestData getInstance() {
        return ourInstance;
    }

    private SpeedTestData() {
    }

    public void setLastTestData(String text){
        lastTestData = text;
    }

    public String getLastTestData(){
        return lastTestData;
    }

    public void setResult( NetworkAdminSpeedTesterResult result){
        lastResult = result;
    }

    public NetworkAdminSpeedTesterResult getLastResult(){
        return lastResult;
    }


    //Results needed for AutoSpeedV2.

    /**
     * We are keeping the highest download result, since we want results biased toward
     * fast downloads.
     * @param currDownRateInKBytePerSec - result of a "download only" test.
     */
    public void setHighestDownloadResult(int currDownRateInKBytePerSec){
        if( highestDownloadOnlyResult<currDownRateInKBytePerSec ){
            highestDownloadOnlyResult=currDownRateInKBytePerSec;
        }
    }

    /**
     *
     * @return - int
     */
    public int getHightestDownloadResult(){
        return highestDownloadOnlyResult;
    }

    /**
     * Record the last upload only result, but the minimum allowed result is 20 kbytes/sec.
     * @param currUpRateInKBytesPerSec - 
     */
    public void setLastUploadOnlyResult(int currUpRateInKBytesPerSec){

        //The lowest upload rate allowed in 20 kB/s.
        if(currUpRateInKBytesPerSec<20){
            currUpRateInKBytesPerSec=20;
        }

        lastUploadOnlyResult = currUpRateInKBytesPerSec;
    }

    
    public int getLastUploadOnlyResult(){
        return lastUploadOnlyResult;
    }

}
