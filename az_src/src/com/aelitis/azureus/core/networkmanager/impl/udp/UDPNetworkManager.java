/*
 * Created on 22 Jun 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.networkmanager.impl.udp;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.RandomUtils;

import com.aelitis.azureus.core.networkmanager.impl.ProtocolDecoderPHE;
import com.aelitis.net.udp.uc.PRUDPPacket;

public class 
UDPNetworkManager 
{
	public static final boolean	MINIMISE_OVERHEADS	= true;
	
	public static final int MIN_INCOMING_INITIAL_PACKET_SIZE = ProtocolDecoderPHE.MIN_INCOMING_INITIAL_PACKET_SIZE;
	public static final int MAX_INCOMING_INITIAL_PACKET_SIZE = ProtocolDecoderPHE.getMaxIncomingInitialPacketSize(MINIMISE_OVERHEADS);
	
	private static final int MIN_MSS = 128;
	private static final int MAX_MSS = PRUDPPacket.MAX_PACKET_SIZE;
	
	private static int udp_mss_size;
	
	public static boolean UDP_INCOMING_ENABLED;
	public static boolean UDP_OUTGOING_ENABLED;
	
	static{
		COConfigurationManager.addAndFireParameterListener(
				"UDP.Listen.Port.Enable",
				new ParameterListener()
				{
					public void 
					parameterChanged(
						String name )
					{
						UDP_INCOMING_ENABLED = UDP_OUTGOING_ENABLED = COConfigurationManager.getBooleanParameter( name );
					}
				});
	}
	
	public static int getUdpMssSize() {  return udp_mss_size;  }

	public static void
	refreshRates(
		int		min_rate )
	{
		udp_mss_size = COConfigurationManager.getIntParameter( "network.udp.mtu.size" ) - 40; 	        

	    if( udp_mss_size > min_rate )  udp_mss_size = min_rate - 1;
	    
	    if( udp_mss_size < MIN_MSS )  udp_mss_size = MIN_MSS; 
	    
	    if ( udp_mss_size > MAX_MSS ) udp_mss_size = MAX_MSS;
	}
	
	private static UDPNetworkManager	singleton;
	

	public static UDPNetworkManager
	getSingleton()
	{
		synchronized( UDPNetworkManager.class ){
			
			if ( singleton == null ){
				
				singleton = new UDPNetworkManager();
			}
		}
		
		return( singleton );
	}
	
	private int udp_listen_port	= -1;
	private int udp_non_data_listen_port = -1;
	
	private UDPConnectionManager	_connection_manager;
	
	protected
	UDPNetworkManager()
	{
		COConfigurationManager.addAndFireParameterListener( 
			   "UDP.Listen.Port", 
			   new ParameterListener() 
			   {
				   public void 
				   parameterChanged(String name) 
				   {
					   int port = COConfigurationManager.getIntParameter( name );
					   
					   if ( port == udp_listen_port ){
						   
						   return;
					   }
					   
					   if ( port < 0 || port > 65535 || port == 6880 ) {
						   
					        String msg = "Invalid incoming UDP listen port configured, " +port+ ". The port has been reset. Please check your config!";
					        
					        Debug.out( msg );
					        
					        Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_ERROR, msg));
					        
					        udp_listen_port = RandomUtils.generateRandomNetworkListenPort();
					        
					        COConfigurationManager.setParameter( name, udp_listen_port );
					        
					    }else{
					
					    	udp_listen_port	= port;
					    }
				   }
			   });
	   
		COConfigurationManager.addAndFireParameterListener( 
				   "UDP.NonData.Listen.Port", 
				   new ParameterListener() 
				   {
					   public void 
					   parameterChanged(String name) 
					   {
						   int port = COConfigurationManager.getIntParameter( name );
						   
						   if ( port == udp_non_data_listen_port ){
							   
							   return;
						   }
						   
						   if ( port < 0 || port > 65535 || port == 6880 ) {
							   
						        String msg = "Invalid incoming UDP non-data listen port configured, " +port+ ". The port has been reset. Please check your config!";
						        
						        Debug.out( msg );
						        
						        Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_ERROR, msg));
						        
						        udp_non_data_listen_port = RandomUtils.generateRandomNetworkListenPort();
						        
						        COConfigurationManager.setParameter( name, udp_non_data_listen_port );
						        
						    }else{
						
						    	udp_non_data_listen_port	= port;
						    }
					   }
				   });
	}
	
	public boolean
	isUDPListenerEnabled()
	{
		return( UDP_INCOMING_ENABLED );
	}
  
	public int 
	getUDPListeningPortNumber()
	{
		return( udp_listen_port );
	}
	
	public boolean
	isUDPNonDataListenerEnabled()
	{
		return( UDP_INCOMING_ENABLED );
	}
	
	public int 
	getUDPNonDataListeningPortNumber()
	{
		return( udp_non_data_listen_port );
	}
	
	public UDPConnectionManager
	getConnectionManager()
	{
		synchronized( this ){
			
			if ( _connection_manager == null ){
				
				_connection_manager = new UDPConnectionManager();
			}
		}
		
		return( _connection_manager );
	}
}
