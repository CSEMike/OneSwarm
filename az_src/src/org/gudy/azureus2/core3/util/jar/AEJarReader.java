/*
 * File    : WUJarReader.java
 * Created : 31-Mar-2004
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.util.jar;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;
import java.util.jar.*;

import org.gudy.azureus2.core3.util.Debug;

public class 
AEJarReader 
{
	protected Map		entries	= new HashMap();
	
	public
	AEJarReader(
		String		name )
	{
		InputStream 	is 	= null;
		JarInputStream 	jis = null;
		
		try{
			is = getClass().getClassLoader().getResourceAsStream(name);
			
			jis = new JarInputStream(is );
			
			while( true ){
				
				JarEntry ent = jis.getNextJarEntry();
				
				if ( ent == null ){
					
					break;
				}
				
				if ( ent.isDirectory()){
					
					continue;
				}
				
				ByteArrayOutputStream	baos = new ByteArrayOutputStream();
					
				byte[]	buffer = new byte[8192];
					
				while( true ){
						
					int	l = jis.read( buffer );
						
					if ( l <= 0 ){
							
						break;
					}
						
					baos.write( buffer, 0, l );
				}
					
				entries.put( ent.getName(), new ByteArrayInputStream( baos.toByteArray()));
			}
		
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
						
		}finally{
			
			try{
				if ( jis != null ){
					
					jis.close();
				}
				
				if (is != null){
					
					is.close();
				}
			}catch( Throwable e ){
				
			}
		}
	}
	
	public InputStream
	getResource(
		String	name )
	{
		return((InputStream)entries.get(name));
	}
}
