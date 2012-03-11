/*
 * Created on 09-Sep-2004
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

package org.gudy.azureus2.core3.util;

import java.nio.ByteBuffer;

/**
 * @author parg
 *
 */

public class 
ConcurrentHasherRequest 
{	
	private static AEMonitor		class_mon = new AEMonitor( "ConcHashRequest:class" );
	
	private ConcurrentHasher					concurrent_hasher;
	private ByteBuffer							buffer;
	private ConcurrentHasherRequestListener		listener;

	private int									size;
	private byte[]								result;
	private boolean								cancelled;
	private boolean								low_priority;
	
	private AESemaphore	sem = new AESemaphore("ConcHashRequest");
	
	protected
	ConcurrentHasherRequest(
		ConcurrentHasher					_concurrent_hasher,
		ByteBuffer							_buffer,
		ConcurrentHasherRequestListener		_listener,
		boolean								_low_priorty )
	{
		concurrent_hasher	= _concurrent_hasher;
		buffer				= _buffer;
		listener			= _listener;
		low_priority		= _low_priorty;
		
		size				= buffer.limit() - buffer.position();
	}
	
		/**
		 * synchronously get the result of the hash - null returned if it is cancelled
		 * @return
		 */
	
	public byte[]
	getResult()
	{
		sem.reserve();
		
		return( result );
	}
	
		/**
		 * cancel the hash request. If it is cancelled before it is completed then
		 * a subsequent call to getResult will return null
		 */
	
	public void
	cancel()
	{	
		if ( !cancelled ){
			
			cancelled	= true;
			
			sem.releaseForever();
			
			ConcurrentHasherRequestListener	listener_copy;
			
			try{
				class_mon.enter();
			
				listener_copy	= listener;
				
				listener	= null;
				
			}finally{
				
				class_mon.exit();
			}
			
			if ( listener_copy != null ){
				
				listener_copy.complete( this );
			}
		}
	}
	
	public boolean
	getCancelled()
	{
		return( cancelled );
	}
	
	public int
	getSize()
	{
		return( size );
	}
	
	public boolean
	isLowPriority()
	{
		return( low_priority );
	}
	
	protected void
	run(
		SHA1Hasher	hasher )
	{
		if ( !cancelled ){
			
			if ( AEDiagnostics.ALWAYS_PASS_HASH_CHECKS ){
			
				result = new byte[0];
				
			}else{
				
				result = hasher.calculateHash( buffer );
			}
			
			sem.releaseForever();

			if ( !cancelled ){
				
				ConcurrentHasherRequestListener	listener_copy;
				
				try{
					class_mon.enter();
				
					listener_copy	= listener;
					
					listener	= null;
					
				}finally{
					
					class_mon.exit();
				}
				
				if ( listener_copy != null ){
					
					listener_copy.complete( this );
				}	
			}
		}
	}
}
