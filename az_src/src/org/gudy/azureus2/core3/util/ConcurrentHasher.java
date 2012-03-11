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

/**
 * @author parg
 *
 */

import java.util.*;
import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;

public class 
ConcurrentHasher 
{
	
	protected static ConcurrentHasher		singleton	= new ConcurrentHasher();
	
	protected int			processor_num;
	
	protected List<ConcurrentHasherRequest>				requests		= new LinkedList<ConcurrentHasherRequest>();
	
	protected List<SHA1Hasher>	hashers			= new ArrayList<SHA1Hasher>();
	
	protected AESemaphore		request_sem		= new AESemaphore("ConcHashReqQ");
	protected AESemaphore		scheduler_sem	= new AESemaphore("ConcHashSched");
		
	protected AEMonitor			requests_mon	= new AEMonitor( "ConcurrentHasher:R" );

	private static boolean friendly_hashing;

	static{
		friendly_hashing = COConfigurationManager.getBooleanParameter( "diskmanager.friendly.hashchecking" );

		COConfigurationManager.addParameterListener( "diskmanager.friendly.hashchecking", new ParameterListener() {
			public void parameterChanged( String  str ) {
				friendly_hashing = COConfigurationManager.getBooleanParameter( "diskmanager.friendly.hashchecking" );        
			}
		});
	}


	
	public static ConcurrentHasher
	getSingleton()
	{
		return( singleton );
	}
	
	public static boolean
	concurrentHashingAvailable()
	{
		return( getSingleton().processor_num > 1 );
	}
	
	protected
	ConcurrentHasher()
	{
			// TODO: number of processors can apparently change....
			// so periodically grab num + reserve/release as necessary
		
		processor_num = Runtime.getRuntime().availableProcessors();
		
			// just in case :P
		
		if ( processor_num <= 0 ){
			
			processor_num	= 1;
		}
			
			// one more that proc num seems to improve performance ;)
		
		for (int i=0;i<processor_num + 1;i++){
			
			scheduler_sem.release();
		}
	
		final ThreadPool pool	= new ThreadPool( "ConcurrentHasher", 64 );
		
		new AEThread2("ConcurrentHasher:scheduler", true )
		{
			public void
			run()
			{
				while(true){
					
						// get a request to run
					
					request_sem.reserve();
					
						// now extract the request
					
					final ConcurrentHasherRequest	req;
					final SHA1Hasher				hasher;
					
					try{
						requests_mon.enter();
						
						req	= requests.remove(0);
						
						if ( hashers.size() == 0 ){
							
							hasher = new SHA1Hasher();
							
						}else{
							
							hasher	= hashers.remove( hashers.size()-1 );
						}
					}finally{
						
						requests_mon.exit();
					}
					
					pool.run( 
							new AERunnable()
							{
								public void
								runSupport()
								{
									try{											
										req.run( hasher );
										
									}finally{
										try{
											requests_mon.enter();

											hashers.add( hasher );
										
										}finally{
											
											requests_mon.exit();
										}

										if ( friendly_hashing && req.isLowPriority()){
				
											try{  
												int	size = req.getSize();
												
													// pieces can be several MB so delay based on size
												
												final int max = 250;
												final int min = 50;
												
												size = size/1024;	// in K
												
												size = size/8;
												
													// 4MB -> 500
													// 1MB -> 125
												
												size = Math.min( size, max );
												size = Math.max( size, min );
												
												Thread.sleep( size );
					
											}catch( Throwable e ){ 
					
												Debug.printStackTrace( e ); 
											}
										}
									       		
										scheduler_sem.release();
									}
								}
							});
												
				}
			}
		}.start();
	}
	
		/**
		 * add a synchronous request - on return it will have run (or been cancelled)
	     */
	
	public ConcurrentHasherRequest
	addRequest(
		ByteBuffer		buffer )
	{
		return( addRequest( buffer, null, false ));
	}
	
		/**
		 * Add an asynchronous request if listener supplied, sync otherwise 
		 * @param buffer
		 * @param priority
		 * @param listener
		 * @param low_priorty low priority checks will cause the "friendly hashing" setting to be
		 * taken into account
		 * @return
		 */
	
	public ConcurrentHasherRequest
	addRequest(
		ByteBuffer							buffer,
		ConcurrentHasherRequestListener		listener,
		boolean								low_priorty )
	{
		final ConcurrentHasherRequest	req = new ConcurrentHasherRequest( this, buffer, listener, low_priorty );
			
			// get permission to run a request
		

		// test code to force synchronous checking
		//SHA1Hasher	hasher = new SHA1Hasher();
		//req.run( hasher );
		
		scheduler_sem.reserve();
		
		try{
			requests_mon.enter();
			
			requests.add( req );
		
		}finally{
			
			requests_mon.exit();
		}
		
		request_sem.release();
		
		return( req );
	}
	
	public static void
	main(
		String[]	args )
	{
		/*
		final ConcurrentHasher	hasher = ConcurrentHasher.getSingleton();
		
		int		threads			= 1;
		
		final long	buffer_size		= 128*1024;
		final long	loop			= 1024;
		
		for (int i=0;i<threads;i++){
						
			new Thread()
			{
				public void
				run()
				{
					// SHA1Hasher sha1_hasher = new SHA1Hasher();
					
					long	start = System.currentTimeMillis();
					//ByteBuffer	buffer = ByteBuffer.allocate((int)buffer_size);
					
					for (int j=0;j<loop;j++){
						
												
						//sha1_hasher.calculateHash( buffer );
						//ConcurrentHasherRequest req = hasher.addRequest( buffer );
					}
					
					long	elapsed = System.currentTimeMillis() - start;
					
					System.out.println( 
							"elapsed = " + elapsed + ", " + 
							((loop*buffer_size*1000)/elapsed) + " B/sec" );
				}
			}.start();
		}
		*/
	}
}
