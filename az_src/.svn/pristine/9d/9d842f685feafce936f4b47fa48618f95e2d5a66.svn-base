/*
 * Created on 06-Dec-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.proxy.impl;

import java.util.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;


import com.aelitis.azureus.core.networkmanager.VirtualChannelSelector;
import com.aelitis.azureus.core.proxy.*;

/**
 * @author parg
 *
 */

public class 
AEProxyImpl 
	implements AEProxy, VirtualChannelSelector.VirtualSelectorListener
{
	private static final LogIDs LOGID = LogIDs.NET;
	private static final int	DEBUG_PERIOD	= 60000;
	private long				last_debug;
	
	private int				port;
	private long				connect_timeout;
	private long				read_timeout;
	private AEProxyHandler	proxy_handler;
	
	private VirtualChannelSelector	read_selector;
	private VirtualChannelSelector	connect_selector;
	private VirtualChannelSelector	write_selector;
	
	private List				processors = new ArrayList();
	private final HashMap write_select_regs = new HashMap();
	
	private boolean				allow_external_access;
	
	private AEMonitor			this_mon	= new AEMonitor( "AEProxyImpl" );
	
	public 
	AEProxyImpl(
		int				_port,
		long			_connect_timeout,
		long			_read_timeout,
		AEProxyHandler	_proxy_handler )	
		throws AEProxyException
	{
		port					= _port;
		connect_timeout			= _connect_timeout;
		read_timeout			= _read_timeout;
		proxy_handler			= _proxy_handler;
		
		String	name = "Proxy:" + port;
		
		read_selector	 = new VirtualChannelSelector( name, VirtualChannelSelector.OP_READ, false );
		connect_selector = new VirtualChannelSelector( name, VirtualChannelSelector.OP_CONNECT, true );
		write_selector	 = new VirtualChannelSelector( name, VirtualChannelSelector.OP_WRITE, true );
		
		try{
			
			final ServerSocketChannel	ssc = ServerSocketChannel.open();
			
			ServerSocket ss	= ssc.socket();
			
			ss.setReuseAddress(true);

			ss.bind(  new InetSocketAddress( InetAddress.getByName("127.0.0.1"), port), 128 );
			
			if ( port == 0 ){
				
				port	= ss.getLocalPort();
			}
				
			Thread connect_thread = 
				new AEThread("AEProxy:connect.loop")
				{
					public void
					runSupport()
					{
						selectLoop( connect_selector );
					}
				};
	
			connect_thread.setDaemon( true );
	
			connect_thread.start();
	
			Thread read_thread = 
				new AEThread("AEProxy:read.loop")
				{
					public void
					runSupport()
					{
						selectLoop( read_selector );
					}
				};
	
			read_thread.setDaemon( true );
	
			read_thread.start();
			
			Thread write_thread = 
				new AEThread("AEProxy:write.loop")
				{
					public void
					runSupport()
					{
						selectLoop( write_selector );
					}
				};
	
			write_thread.setDaemon( true );
	
			write_thread.start();
			
			Thread accept_thread = 
					new AEThread("AEProxy:accept.loop")
					{
						public void
						runSupport()
						{
							acceptLoop( ssc );
						}
					};
		
			accept_thread.setDaemon( true );
		
			accept_thread.start();									
		
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "AEProxy: listener established on port "
						+ port)); 
			
		}catch( Throwable e){

			Logger.logTextResource(new LogAlert(LogAlert.UNREPEATABLE,
					LogAlert.AT_ERROR, "Tracker.alert.listenfail"), new String[] { ""
					+ port });
	
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "AEProxy: listener failed on port "
						+ port, e)); 
						
			throw( new AEProxyException( "AEProxy: accept fails: " + e.toString()));
		}			
	}	
	
	public void
	setAllowExternalConnections(
		boolean	permit )
	{
		allow_external_access = permit;
	}
	
	protected void
	acceptLoop(
		ServerSocketChannel	ssc )
	{		
		long	successfull_accepts = 0;
		long	failed_accepts		= 0;

		while(true){
			
			try{				
				SocketChannel socket_channel = ssc.accept();
						
				successfull_accepts++;
				
				if ( !( allow_external_access || socket_channel.socket().getInetAddress().isLoopbackAddress())){
					
					if (Logger.isEnabled())
						Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
								"AEProxy: incoming connection from '"
										+ socket_channel.socket().getInetAddress()
										+ "' - closed as not local"));
				
					socket_channel.close();
					
				}else{
						
					socket_channel.configureBlocking(false);
	
					AEProxyConnectionImpl processor = new AEProxyConnectionImpl(this, socket_channel, proxy_handler);
					
					if ( !processor.isClosed()){
						
						try{
							this_mon.enter();
						
							processors.add( processor );
			
							if (Logger.isEnabled())
								Logger.log(new LogEvent(LOGID, "AEProxy: active processors = "
										+ processors.size()));
							
						}finally{
							
							this_mon.exit();
						}
						
						read_selector.register( socket_channel, this, processor );
					}
				}
			}catch( Throwable e ){
				
				failed_accepts++;

				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID, "AEProxy: listener failed on port "
							+ port, e)); 
			
				if ( failed_accepts > 100 && successfull_accepts == 0 ){

						// looks like its not going to work...
						// some kind of socket problem
					Logger.logTextResource(new LogAlert(LogAlert.UNREPEATABLE,
							LogAlert.AT_ERROR, "Network.alert.acceptfail"), new String[] {
							"" + port, "TCP" });
			
					break;
				}			
			}
		}
	}
	
	protected void
	close(
		AEProxyConnectionImpl	processor )
	{
		try{
			this_mon.enter();
			
			processors.remove( processor );
			
		}finally{
		
			this_mon.exit();
		}
	}
	
	protected void
	selectLoop(
      VirtualChannelSelector	selector )
	{
		long	last_time	= 0;
		
		while( true ){
			
			try{
				selector.select( 100 );
				
					// only use one selector to trigger the timeouts!
				
				if ( selector == read_selector ){
					
					long	now = SystemTime.getCurrentTime();
					
					if ( now < last_time ){
						
						last_time	= now;
						
					}else if ( now - last_time >= 5000 ){
						
						last_time	= now;
						
						checkTimeouts();
					}
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}

	protected void
	checkTimeouts()
	{
		long	now = SystemTime.getCurrentTime();
		
		if ( now - last_debug > DEBUG_PERIOD ){
			
			last_debug	= now;
			
			try{
				this_mon.enter();
				
				Iterator	it = processors.iterator();
				
				while( it.hasNext()){
					
					AEProxyConnectionImpl	processor = (AEProxyConnectionImpl)it.next();
					
					if (Logger.isEnabled())
						Logger.log(new LogEvent(LOGID, "AEProxy: active processor: "
								+ processor.getStateString()));
				}
			}finally{
				
				this_mon.exit();
			}
		}
		
		if ( connect_timeout <= 0 && read_timeout <= 0 ){
			
			return;
		}
		
		List	closes = new ArrayList();
		
		try{
			this_mon.enter();
			
			Iterator	it = processors.iterator();
			
			while( it.hasNext()){
				
				AEProxyConnectionImpl	processor = (AEProxyConnectionImpl)it.next();
				
				long diff = now - processor.getTimeStamp();
				
				if ( 	connect_timeout > 0 &&
						diff >= connect_timeout && 
						!processor.isConnected()){
					
					closes.add( processor );
				
				}else if (	read_timeout > 0 &&
							diff >= read_timeout &&
							processor.isConnected()){
					
					closes.add( processor );
				}
			}
		}finally{
			
			this_mon.exit();
		}
		
		for (int i=0;i<closes.size();i++){
			
			((AEProxyConnectionImpl)closes.get(i)).failed( new Throwable( "timeout" ));
		}
	}
	
	protected void
	requestWriteSelect(
		AEProxyConnectionImpl	processor,
		SocketChannel 			sc )
	{
    
    if( write_select_regs.containsKey( sc ) ) {  //already been registered, just resume
      write_selector.resumeSelects( sc );
    }
    else {  //not yet registered
      write_select_regs.put( sc, null );
      write_selector.register( sc, this, processor );
    }
	}
	
	protected void
	cancelWriteSelect(
		SocketChannel 			sc )
	{
    write_select_regs.remove( sc );
		write_selector.cancel( sc );
	}
	
	protected void
	requestReadSelect(
		AEProxyConnectionImpl	processor,
		SocketChannel 		sc )
	{
		read_selector.register( sc, this, processor );
	}
	
	protected void
	cancelReadSelect(
		SocketChannel 		sc )
	{
		read_selector.cancel( sc );
	}
	
	protected void
	requestConnectSelect(
		AEProxyConnectionImpl	processor,
		SocketChannel 			sc )
	{
		connect_selector.register( sc, this, processor );
	}
	
	protected void
	cancelConnectSelect(
		SocketChannel 		sc )
	{
		connect_selector.cancel( sc );
	}
	
    public boolean 
	selectSuccess( 
      VirtualChannelSelector	selector, 
		SocketChannel 			sc,
		Object 					attachment )
    {
    	AEProxyConnectionImpl	processor = (AEProxyConnectionImpl)attachment;
    	   	
    	if ( selector == read_selector ){
    		
    		return( processor.read(sc));
    		
    	}else if ( selector == write_selector ){
    		
    		return( processor.write(sc));
    		
    	}else{
    		
    		return( processor.connect(sc));
    	}
    }
    
    public void 
	selectFailure( 
      VirtualChannelSelector	selector, 
		SocketChannel 			sc,
		Object 					attachment,
		Throwable 				msg )
    {
    	AEProxyConnectionImpl	processor = (AEProxyConnectionImpl)attachment;
    	
    	processor.failed( msg );
    }
    
	public int
	getPort()
	{
		return( port );
	}
}
