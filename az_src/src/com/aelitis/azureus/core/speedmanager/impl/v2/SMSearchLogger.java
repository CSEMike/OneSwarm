package com.aelitis.azureus.core.speedmanager.impl.v2;

import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.AEDiagnosticsLogger;
import org.gudy.azureus2.core3.util.AEDiagnostics;
import com.aelitis.azureus.core.speedmanager.SpeedManagerLimitEstimate;
import com.aelitis.azureus.core.speedmanager.impl.SpeedManagerAlgorithmProviderAdapter;

/**
 * Created on Jul 30, 2007
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
 * Limit search diagnostics.
 */
public class SMSearchLogger
{

    private static final LogIDs ID = LogIDs.NWMAN;
    private static final AEDiagnosticsLogger dLog = AEDiagnostics.getLogger("AutoSpeedSearchHistory");

    private SMSearchLogger(){}

    public static void log(String str){

        //get the adapter values.
        SpeedManagerAlgorithmProviderAdapter adpter = SMInstance.getInstance().getAdapter();
        int adptCurrUpLimit = adpter.getCurrentUploadLimit();
        int adptCurrDownLimit = adpter.getCurrentDownloadLimit();

        //get the COConfigurationManager values.
        SMConfigurationAdapter conf = SMInstance.getInstance().getConfigManager();
        SpeedManagerLimitEstimate uploadSetting = conf.getUploadLimit();
        SpeedManagerLimitEstimate downloadSetting = conf.getDownloadLimit();


        StringBuffer sb = new StringBuffer(str);
        sb.append(", Download current =").append(adptCurrDownLimit);
        sb.append(", max limit =").append(downloadSetting.getString());

        sb.append(", Upload current = ").append(adptCurrUpLimit);
        sb.append(", max limit = ").append(uploadSetting.getString());

        String msg = sb.toString();

        LogEvent e = new LogEvent(ID,msg);
        Logger.log(e);

        if(dLog!=null){
            dLog.log(msg);
        }

    }//log

}
