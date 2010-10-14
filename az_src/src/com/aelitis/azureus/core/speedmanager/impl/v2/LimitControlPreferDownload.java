package com.aelitis.azureus.core.speedmanager.impl.v2;

/**
 * Created on Jul 12, 2007
 * Created by Alan Snyder
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 * <p/>
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
 * <p/>
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

/**
 * Prefer the download, but something like 80,20
 */
public class LimitControlPreferDownload implements LimitControl
{

    LimitControlSetting setting;

    float upMidPoint = 0.2f;
    float downMidPoint = 0.8f;

    int upMax;
    int upMin;
    int downMax;
    int downMin;

    TransferMode mode;
    float percentUpMaxUsed=0.6f;

    boolean isDownloadUnlimited=false;

    public SMUpdate adjust(float amount) {

        setting.adjust( amount );

        int upMaxUsed = upMax;
        if( mode.getMode() == TransferMode.State.DOWNLOADING ){

            upMaxUsed = Math.round( upMax*percentUpMaxUsed );
        }

        float normalizedUp;
        float normalizedDown;
        if( setting.getValue()>0.5f  ){
            //calculate rates above this limit.

        }else{
            //calculate rates below this limit.

        }

        return new SMUpdate(0,true,0,true);//Till implemented.

        //return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateLimits(int upMax, int upMin, int downMax, int downMin) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateSeedSettings(float downloadModeUsed) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateStatus(int currUpLimit, SaturatedMode uploadUsage, int currDownLimit, SaturatedMode downloadUsage, TransferMode transferMode) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setDownloadUnlimitedMode(boolean isUnlimited) {
        isDownloadUnlimited = isUnlimited;
    }

    public boolean isDownloadUnlimitedMode() {
        return isDownloadUnlimited;  
    }
}
