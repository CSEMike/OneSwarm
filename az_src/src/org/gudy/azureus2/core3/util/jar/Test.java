/*
 * Created on 07-Jun-2004
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

package org.gudy.azureus2.core3.util.jar;

/**
 * @author parg
 *
 */

import java.io.*;

import org.gudy.azureus2.core3.security.*;
import org.gudy.azureus2.core3.util.Debug;

public class 
Test 
{
	public static void
	main(
		String[]		args )
	{
		try{
			SESecurityManager.initialise();
			
			System.out.println( System.getProperty( "java.home" ));
			
			String	alias = "Azureus"; // SESecurityManager.DEFAULT_ALIAS;
			
			// SEKeyDetails	kd = SESecurityManager.getKeyDetails( alias );
			// WUJarSigner signer = new WUJarSigner(alias, (PrivateKey)kd.getKey(), kd.getCertificateChain());
			
			AEJarSigner2 signer = 
				new AEJarSigner2(
						alias,
						SESecurityManager.getKeystoreName(),
						SESecurityManager.getKeystorePassword());
			
			FileOutputStream	fos = new FileOutputStream( "c:\\temp\\sj.jar");
			
			signer.signJarFile( new File( "c:\\temp\\si.jar"), fos );
			
			fos.close();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
		
		System.out.println("normal exit");
	}
}
