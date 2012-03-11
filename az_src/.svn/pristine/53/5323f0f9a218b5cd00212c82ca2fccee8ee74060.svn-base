package com.aelitis.azureus.core.speedmanager.impl.v2;

import org.gudy.azureus2.core3.util.SystemTime;
import com.aelitis.azureus.core.speedmanager.SpeedManagerLimitEstimate;
import com.aelitis.azureus.core.speedmanager.SpeedManagerPingMapper;
import com.aelitis.azureus.core.speedmanager.SpeedManager;
import com.aelitis.azureus.core.speedmanager.impl.SpeedManagerAlgorithmProviderAdapter;

import java.util.List;
import java.util.ArrayList;

/**
 * Created on Jul 16, 2007
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


public class PingSpaceMon
{

    private static final long INTERVAL = 1000 * 60 * 15L;


    long nextCheck = System.currentTimeMillis() + INTERVAL;

    TransferMode mode;


    List listeners = new ArrayList();//List<PSMonitorListener>


    public void addListener(PSMonitorListener listener){

        //don't register the same listener twice.
        for(int i=0; i<listeners.size(); i++){
            PSMonitorListener t = (PSMonitorListener) listeners.get(i);
            if( t==listener ){
                SpeedManagerLogger.trace("Not logging same listener twice. listener="+listener.toString());
                return;
            }
        }

        listeners.add( listener );
    }

    public boolean removeListener(PSMonitorListener listener){

        return listeners.remove(listener);

    }


    boolean checkForLowerLimits(){

        long curr = SystemTime.getCurrentTime();
        if( curr > nextCheck ){
            SpeedManagerLogger.trace("PingSpaceMon checking for lower limits.");

            for(int i=0; i<listeners.size(); i++ ){
                PSMonitorListener l =(PSMonitorListener) listeners.get(i);

                if(l!=null){
                    l.notifyUpload( getUploadEstCapacity() );
                }else{
                    SpeedManagerLogger.trace("listener index _"+i+"_ was null.");
                }
            }

            resetTimer();
            return true;
        }
        return false;
    }

    /**
     *
     * @param tMode -
     * @return - true if is has a new mode, and the clock starts over.
     */
    boolean updateStatus(TransferMode tMode){

        if(mode==null){
            mode=tMode;
            return true;
        }

        if( mode.getMode() != tMode.getMode() ){
            mode = tMode;
            resetTimer();
            return true;
        }
        return checkForLowerLimits();
    }//updateStatus

    void resetTimer(){
        long curr = SystemTime.getCurrentTime();
        nextCheck = curr + INTERVAL;
        SpeedManagerLogger.trace("Monitor resetting time. Next check in interval.");
    }

    /**
     * Get the current estimated upload limit from the ping mapper.
     * @param - true if the long-term persistent result should be used.
     * @return - SpeedManagerLimitEstimate.
     */
    public static SpeedManagerLimitEstimate getUploadLimit(boolean persistent){
        try{
            SMInstance pm = SMInstance.getInstance();
            SpeedManagerAlgorithmProviderAdapter adapter = pm.getAdapter();
            SpeedManagerPingMapper persistentMap = adapter.getPingMapper();
            SpeedManagerLimitEstimate upEst = persistentMap.getEstimatedUploadLimit(true);

            return upEst;
            
        }catch(Throwable t){
            //log this event and
            SpeedManagerLogger.log( t.toString() );
            t.printStackTrace();

            //something to return 1 and -1.0f results.
            return new DefaultLimitEstimate();
        }
    }//getUploadLimit

    public static SpeedManagerLimitEstimate getUploadEstCapacity()
    {
        try{
            SMInstance pm = SMInstance.getInstance();
            SpeedManagerAlgorithmProviderAdapter adapter = pm.getAdapter();
            SpeedManager sm = adapter.getSpeedManager();
            SpeedManagerLimitEstimate upEstCapacity = sm.getEstimatedUploadCapacityBytesPerSec();

            return upEstCapacity;

        }catch(Throwable t){
            //log this event and
            SpeedManagerLogger.log( t.toString() );
            t.printStackTrace();

            //something to return 1 and -1.0f results.
            return new DefaultLimitEstimate();
        }
    }

    /**
     * Get the current estimated download limit from the ping mapper.
     * @return - SpeedManagerLimitEstimate
     */
    public static SpeedManagerLimitEstimate getDownloadLimit(){
        try{
            SMInstance pm = SMInstance.getInstance();
            SpeedManagerAlgorithmProviderAdapter adapter = pm.getAdapter();
            SpeedManagerPingMapper persistentMap = adapter.getPingMapper();
            SpeedManagerLimitEstimate downEst = persistentMap.getEstimatedDownloadLimit(true);

            return downEst;

        }catch(Throwable t){
            //log this event and
            SpeedManagerLogger.log( t.toString() );
            t.printStackTrace();

            //something to return 0 and -1.0f results.
            return new DefaultLimitEstimate();
        }
    }//getDownloadLimit

    /**
     * Get the estimated download capacity from the SpeedManager.
     * @return - SpeedManagerLimitEstimate
     */
    public static SpeedManagerLimitEstimate getDownloadEstCapacity()
    {
        try{
            SMInstance pm = SMInstance.getInstance();
            SpeedManagerAlgorithmProviderAdapter adapter = pm.getAdapter();
            SpeedManager sm = adapter.getSpeedManager();
            SpeedManagerLimitEstimate downEstCapacity = sm.getEstimatedDownloadCapacityBytesPerSec();

            return downEstCapacity;

        }catch(Throwable t){
            //log this event and
            SpeedManagerLogger.log( t.toString() );
            t.printStackTrace();

            //something to return 0 and -1.0f results.
            return new DefaultLimitEstimate();
        }
    }

    static class DefaultLimitEstimate implements SpeedManagerLimitEstimate
    {

        public int getBytesPerSec() {
            return 1;
        }

        public float getEstimateType() {
        	return -1.0f;
        }
        public float getMetricRating() {
            return -1.0f;
        }

        public int[][] getSegments() {
            return new int[0][];
        }

        public long getWhen(){ return(0);}
        public String getString() {
            return "default";
        }
    }//class

}
