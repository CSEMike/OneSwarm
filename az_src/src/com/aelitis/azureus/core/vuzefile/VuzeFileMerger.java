/*
 * Created on Nov 12, 2008
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

import java.io.*;

public class 
VuzeFileMerger 
{
	protected
	VuzeFileMerger(
		String[]		args )
	{
		if ( args.length != 1 ){
			
			usage();
		}
		
		File	input_dir = new File( args[0] );
		
		if ( !input_dir.isDirectory()){
			
			usage();
		}
		
		try{
			File	output_file = new File( args[0] + ".vuze" );
			
			File[]	files = input_dir.listFiles();
			
			VuzeFileHandler vfh = VuzeFileHandler.getSingleton();
			
			VuzeFile target = vfh.create();
			
			for ( int i=0;i<files.length;i++){
				
				File f = files[i];
				
				if ( f.isDirectory()){
					
					continue;
				}
				
				if ( !f.getName().endsWith( ".vuze" )){
					
					continue;
				}
				
				VuzeFile vf = vfh.loadVuzeFile( f.getAbsolutePath());
				
				System.out.println( "Read " + f );
				
				VuzeFileComponent[] comps = vf.getComponents();
				
				for (int j=0;j<comps.length;j++){
					
					VuzeFileComponent comp = comps[j];
					
					target.addComponent( comp.getType(), comp.getContent());
					
					System.out.println( "    added component: " + comp.getType());
				}
			}
			
			target.write( output_file );
			
			System.out.println( "Wrote " + output_file );
			
		}catch( Throwable e ){
			
			System.err.print( "Failed to merge vuze files" );
			
			e.printStackTrace();
		}
	}
	
	protected void
	usage()
	{
		System.err.println( "Usage: <dir_of_vuze_files_to_merge>" );
		
		System.exit(1);
	}
	
	public static void
	main(
		String[]		args )
	{
		new VuzeFileMerger( args );

	}
}
