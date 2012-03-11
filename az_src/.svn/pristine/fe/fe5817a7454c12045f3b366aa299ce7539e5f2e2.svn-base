/*
 * File    : ShareResourceDirImpl.java
 * Created : 02-Jan-2004
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

package org.gudy.azureus2.pluginsimpl.local.sharing;

/**
 * @author parg
 *
 */

import java.io.File;
import java.util.Map;

import org.gudy.azureus2.plugins.sharing.*;

public class 
ShareResourceDirImpl
	extends 	ShareResourceFileOrDirImpl
	implements 	ShareResourceDir
{
	protected static ShareResourceDirImpl
	getResource(
		ShareManagerImpl	_manager,
		File				_file )
	
		throws ShareException
	{
		ShareResourceImpl	res = ShareResourceFileOrDirImpl.getResourceSupport( _manager, _file );

		if ( res instanceof ShareResourceDirImpl ){
			
			return((ShareResourceDirImpl)res);
		}
		
		return( null );
	}
	
	protected
	ShareResourceDirImpl(
		ShareManagerImpl				_manager,
		ShareResourceDirContentsImpl	_parent,
		File							_file,
		boolean							_personal )
	
		throws ShareException
	{
		super( _manager, _parent, ST_DIR, _file, _personal );
	}
	
	protected
	ShareResourceDirImpl(
		ShareManagerImpl	_manager,
		File				_file,
		Map					_map )
	
		throws ShareException
	{
		super( _manager, ST_DIR, _file, _map );
	}
	
	protected byte[]
	getFingerPrint()
		throws ShareException
	{
		return( getFingerPrint( getFile()));
	}
	
	public File
	getDir()
	{
		return( getFile());
	}
}