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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.impl.TransportHelper;

public class 
UDPTransportHelper 
	implements TransportHelper
{
	public static final int READ_TIMEOUT		= 30*1000;
	public static final int CONNECT_TIMEOUT		= 60*1000;
	
	private UDPConnectionManager	manager;
	private UDPSelector				selector;
	private InetSocketAddress		address;
	private UDPTransport			transport;
	
	private boolean					incoming;
	
	private UDPConnection			connection;
	
	private selectListener		read_listener;
	private Object				read_attachment;
	private boolean 			read_selects_paused;
	
	private selectListener		write_listener;
	private Object				write_attachment;
	private boolean 			write_selects_paused	= true;	// default is paused

	private boolean				closed;
	private IOException			failed;
	
	private ByteBuffer			pending_partial_write;
		
	private Map	user_data;
	
	protected
	UDPTransportHelper(
		UDPConnectionManager	_manager,
		InetSocketAddress		_address,
		UDPTransport			_transport )
	
		throws IOException
	{
			// outgoing
	
		manager		= _manager;
		address 	= _address;
		transport	= _transport;
		
		incoming	= false;
		
		connection 	= manager.registerOutgoing( this );
		
		selector	= connection.getSelector();

	}
	
	protected
	UDPTransportHelper(
		UDPConnectionManager	_manager,
		InetSocketAddress		_address, 
		UDPConnection			_connection )
	{
			// incoming
			
		manager		= _manager;
		address 	= _address;
		connection = _connection;
	
		incoming	= true;
		
		selector	= connection.getSelector();
	}
	
	protected void
	setTransport(
		UDPTransport	_transport )
	{
		transport	= _transport;
	}

	protected UDPTransport
	getTransport()
	{
		return( transport );
	}
	
	protected int
	getMss()
	{
		if ( transport == null ){
			
			return( UDPNetworkManager.getUdpMssSize());
		}
		
		return( transport.getMssSize());
	}
	
	public boolean
	minimiseOverheads()
	{
		return( UDPNetworkManager.MINIMISE_OVERHEADS );
	}
	
	public int
	getConnectTimeout()
	{
		return( CONNECT_TIMEOUT );
	}
	
	public int
	getReadTimeout()
	{
		return( READ_TIMEOUT );
	}
	
	public InetSocketAddress
	getAddress()
	{
		return( address );
	}
	
	public String
	getName()
	{
		return( " (UDP)" );
	}
	
	public boolean
	isIncoming()
	{
		return( incoming );
	}
	
	protected UDPConnection
	getConnection()
	{
		return( connection );
	}
	
	public boolean 
	delayWrite(
		ByteBuffer buffer) 
	{
			// TODO: support this one day?
		
		return false;
	}
	
	public boolean
	hasDelayedWrite()
	{
		return( false );
	}
	
	public int 
	write( 
		ByteBuffer 	buffer, 
		boolean		partial_write )
	
		throws IOException
	{
		synchronized( this ){
			
			if ( failed != null ){
				
				throw( failed );
			}
			
			if ( closed ){
				
				throw( new IOException( "Transport closed" ));
			}
		}
		
		if ( partial_write ){
			
			if ( pending_partial_write == null ){
				
				if ( buffer.remaining() < UDPConnectionSet.MIN_WRITE_PAYLOAD ){
				
					ByteBuffer	copy = ByteBuffer.allocate( buffer.remaining());
					
					copy.put( buffer );
					
					copy.position( 0 );
					
					pending_partial_write = copy;
					
					return( copy.remaining());
				}
			}
		}
		
		if ( pending_partial_write != null ){
			
			try{
				int	pw_len = pending_partial_write.remaining();
				
				int	written = connection.write( new ByteBuffer[]{ pending_partial_write, buffer }, 0, 2 );
				
				if ( written >= pw_len ){
					
					return( written - pw_len );
					
				}else{
					
					return( 0 );
				}
				
			}finally{
				
				if ( pending_partial_write.remaining() == 0 ){
					
					pending_partial_write = null;
				}
			}
			
		}else{
			
			return( connection.write( new ByteBuffer[]{ buffer }, 0, 1 ));
		}
	}

    public long 
    write( 
    	ByteBuffer[] 	buffers, 
    	int 			array_offset, 
    	int 			length ) 
    
    	throws IOException
    {
		synchronized( this ){
			
			if ( failed != null ){
				
				throw( failed );
			}
			
			if ( closed ){
				
				throw( new IOException( "Transport closed" ));
			}
		}
		
		if ( pending_partial_write != null ){
			
			ByteBuffer[]	buffers2 = new ByteBuffer[length+1];
			
			buffers2[0] = pending_partial_write;
			
			int	pos = 1;

			for (int i=array_offset;i<array_offset+length;i++){
				
				buffers2[pos++] = buffers[i];
			}
			
			try{
				int	pw_len = pending_partial_write.remaining();
				
				int written = connection.write( buffers2, 0, buffers2.length );
				
				if ( written >= pw_len ){
					
					return( written - pw_len );
					
				}else{
					
					return( 0 );
				}
				
			}finally{
				
				if ( pending_partial_write.remaining() == 0 ){
					
					pending_partial_write = null;
				}
			}
		}else{
		
			return( connection.write( buffers, array_offset, length ));
		}
    }

    public int 
    read( 
    	ByteBuffer buffer ) 
    
    	throws IOException
    {
		synchronized( this ){
			
			if ( failed != null ){
				
				throw( failed );
			}
			
			if ( closed ){
				
				throw( new IOException( "Transport closed" ));
			}
		}
		
    	return( connection.read( buffer ));
    }

    public long 
    read( 
    	ByteBuffer[] 	buffers, 
    	int 			array_offset, 
    	int 			length ) 
    
    	throws IOException
    {
		synchronized( this ){
			
			if ( failed != null ){
				
				throw( failed );
			}
			
			if ( closed ){
				
				throw( new IOException( "Transport closed" ));
			}
		}
		
    	long	total = 0;
    	
    	for (int i=array_offset;i<array_offset+length;i++){
    		
    		ByteBuffer	buffer = buffers[i];
    		
    		int	max = buffer.remaining();
    		
    		int	read = connection.read( buffer );
    		
    		total += read;
    		
    		if ( read < max ){
    		
    			break;
    		}
    	}
    	//System.out.println( "total = " + total );
    	return( total );
    }

    protected void
    canRead()
    {
    	fireReadSelect();
    }
    
    protected void
    canWrite()
    {
    	fireWriteSelect();
    }
    
    public synchronized void
    pauseReadSelects()
    {
    	if ( read_listener != null ){
    		
    		selector.cancel( this, read_listener );
    	}
    	
    	read_selects_paused	= true;
    }
    
    public synchronized void
    pauseWriteSelects()
    {
    	if ( write_listener != null ){
    		
    		selector.cancel( this, write_listener );
    	}
    	
    	write_selects_paused = true;
    }
 
    public synchronized void
    resumeReadSelects()
    {
    	read_selects_paused = false;
    	
    	fireReadSelect();
    }
    
    public synchronized void
    resumeWriteSelects()
    {
    	write_selects_paused = false;
    	    	
    	fireWriteSelect();
    }
    
    public void
    registerForReadSelects(
    	selectListener	listener,
    	Object			attachment )
    {
    	synchronized( this ){
    		
	    	read_listener		= listener;
	    	read_attachment		= attachment;
    	}
    	
    	resumeReadSelects();
    }
    
    public void
    registerForWriteSelects(
    	selectListener	listener,
    	Object			attachment )
    {
    	synchronized( this ){
    		
	      	write_listener		= listener;
	    	write_attachment	= attachment;  
    	} 
    	
    	resumeWriteSelects();
    }
    
    public synchronized void
    cancelReadSelects()
    {
    	selector.cancel( this, read_listener );
    	
    	read_selects_paused	= true;
      	read_listener		= null;
    	read_attachment		= null;
    }
    
    public synchronized void
    cancelWriteSelects()
    {
       	selector.cancel( this, write_listener );
        
    	write_selects_paused	= true;
     	write_listener			= null;
    	write_attachment		= null;
    }
    
    protected void
    fireReadSelect()
    {
     	synchronized( this ){
     		 
	   		if ( read_listener != null && !read_selects_paused ){
	   			
	   			if ( failed != null  ){
	   				
	   	 			selector.ready( this, read_listener, read_attachment, failed );
	   	 		  
	   			}else if ( closed ){
	   				
	   	   			selector.ready( this, read_listener, read_attachment, new Throwable( "Transport closed" ));
	   	   		 
	   			}else if ( connection.canRead()){
	   				
	   	 			selector.ready( this, read_listener, read_attachment );
	   			}
	   		}
     	}
    }
    protected void
    fireWriteSelect()
    {
      	synchronized( this ){
       	    
	   		if ( write_listener != null && !write_selects_paused ){
	   			
	   			if ( failed != null  ){
	   				
	   				write_selects_paused	= true;
	   				
	   	 			selector.ready( this, write_listener, write_attachment, failed );
	   	 		  
	   			}else if ( closed ){
	   				
	   				write_selects_paused	= true;

	   	   			selector.ready( this, write_listener, write_attachment, new Throwable( "Transport closed" ));

	   			}else if ( connection.canWrite()){
	   				
	   				write_selects_paused	= true;
	   					   				
	   	 			selector.ready( this, write_listener, write_attachment );
	   			}
	   		}
    	}
    }
    
    public void
    failed(
    	Throwable	reason )
    {
    	synchronized( this ){
        		
    		if ( reason instanceof IOException ){
    			
    			failed = (IOException)reason;
    			
    		}else{
    			
    			failed	= new IOException( Debug.getNestedExceptionMessageAndStack(reason));
    		}
    	
    		fireReadSelect();
    		fireWriteSelect();
    	}
    	
    	connection.failedSupport( reason );
    }
    
    public void
    close(
    	String	reason )
    {
    	synchronized( this ){
    		
       		closed	= true;
       		
    		fireReadSelect();
      		fireWriteSelect();
      	}
    	
    	connection.closeSupport( reason );
    }
    
	protected void
	poll()
	{
	   	synchronized( this ){
	   		
	   		fireReadSelect();
	   		
	   		fireWriteSelect();
	   	}
	}
	
	public synchronized void
	setUserData(
			Object	key,
			Object	data )
	{
		if ( user_data == null ){

			user_data = new HashMap();
		}

		user_data.put( key, data );
	}

	public synchronized Object
	getUserData(
			Object	key )
	{
		if ( user_data == null ){

			return(null);

		}

		return( user_data.get( key ));
	}
	
	public void
	setTrace(
		boolean	on )
	{
	}
	
	public void setScatteringMode(long forBytes) {
		// currently not implemented for UDP		
	}
}
