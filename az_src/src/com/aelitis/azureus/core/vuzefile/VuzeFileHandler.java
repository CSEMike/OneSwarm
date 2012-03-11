/*
 * Created on May 16, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.vuzefile;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.utils.StaticUtilities;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;

import com.aelitis.azureus.core.util.CopyOnWriteList;


public class 
VuzeFileHandler 
{
	private static VuzeFileHandler singleton = new VuzeFileHandler();
	
	public static VuzeFileHandler
	getSingleton()
	{
		return( singleton );
	}
	
	private CopyOnWriteList	processors = new CopyOnWriteList();
	
	
	protected
	VuzeFileHandler()
	{
	}
	
	public VuzeFile
	loadVuzeFile(
		String	target  )
	{
		try{
			File test_file = new File( target );
	
			if ( test_file.isFile()){
					
				return( getVuzeFile( new FileInputStream( test_file )));
				
			}else{
				
				URL	url = new URI( target ).toURL();
				
				String	protocol = url.getProtocol().toLowerCase();
				
				if ( protocol.equals( "http" ) || protocol.equals( "https" )){
					
					ResourceDownloader rd = StaticUtilities.getResourceDownloaderFactory().create( url );
				
					return( getVuzeFile(rd.download()));
				}
			}
		}catch( Throwable e ){
		}
		
		return( null );
	}
	
	public VuzeFile
	loadVuzeFile(
		byte[]		bytes )
	{
		return( loadVuzeFile( new ByteArrayInputStream( bytes )));
	}
	
	public VuzeFile
	loadVuzeFile(
		InputStream 	is )
	{
		return( getVuzeFile( is ));
	}
	
	public VuzeFile
	loadVuzeFile(
		File 	file )
	{
		InputStream is = null;
		
		try{
			is = new FileInputStream( file );
			
			return( getVuzeFile( is ));
			
		}catch( Throwable e ){
			
			return( null );
			
		}finally{
	
			if ( is != null ){
				
				try{
					is.close();
					
				}catch( Throwable e ){	
				}
			}
		}
	}
	
	protected VuzeFile
	getVuzeFile(
		InputStream		is )
	{
		try{
			BufferedInputStream bis = new BufferedInputStream( is );
			
			try{
				Map	map = BDecoder.decode(bis);
				
				return( loadVuzeFile( map ));
				
			}finally{
				
				is.close();
			}
		}catch( Throwable e ){
		}
		
		return( null );
	}
	
	public VuzeFile
	loadVuzeFile(
		Map	map )
	{
		if ( map.containsKey( "vuze" ) && !map.containsKey( "info" )){
					
			return( new VuzeFileImpl( this, (Map)map.get( "vuze" )));
		}
		
		return( null );
	}
	
	public VuzeFile
	loadAndHandleVuzeFile(
		String		target,
		int			expected_types )
	{
		VuzeFile vf = loadVuzeFile( target );
		
		if ( vf == null ){
			
			return( null );
		}
		
		handleFiles( new VuzeFile[]{ vf }, expected_types );
		
		return( vf );
	}
	
	public void
	handleFiles(
		VuzeFile[]		files,
		int				expected_types )
	{
		Iterator it = processors.iterator();
		
		while( it.hasNext()){
			
			VuzeFileProcessor	proc = (VuzeFileProcessor)it.next();
			
			try{
				proc.process( files, expected_types );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		for (int i=0;i<files.length;i++){
			
			VuzeFile vf = files[i];
			
			VuzeFileComponent[] comps = vf.getComponents();
			
			for (int j=0;j<comps.length;j++){
				
				VuzeFileComponent comp = comps[j];
				
				if ( !comp.isProcessed()){
				
					Debug.out( "Failed to handle Vuze file component " + comp.getContent());
				}
			}
		}
	}
	
	public VuzeFile
	create()
	{
		return( new VuzeFileImpl( this ));
	}
			
	public void
	addProcessor(
		VuzeFileProcessor		proc )
	{
		processors.add( proc );
	}
}
