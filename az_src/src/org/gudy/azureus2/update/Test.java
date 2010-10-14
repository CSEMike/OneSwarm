/*
 * Created on 25-May-2004
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

package org.gudy.azureus2.update;

/**
 * @author parg
 *
 */

import java.io.*;

import org.gudy.azureus2.core3.util.Debug;

public class 
Test 
{
	protected
	Test()
	{
		try{
			InputStream	is 	= new FileInputStream( "Azureus2.jar" );
			
			InputStream pis = new FileInputStream( "plugins" + File.separator + "azupdater" + File.separator + "Azureus2_2.0.8.5_P1.pat" );
			
			OutputStream os = new FileOutputStream( "test.jar" );
			
			new UpdateJarPatcher( is, pis, os, null );
			
			is.close();
			
			pis.close();
						
			os.close();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
		
	}
	
	public static void
	main(
		String[]	args )
	{
		new Test();
	}
}
