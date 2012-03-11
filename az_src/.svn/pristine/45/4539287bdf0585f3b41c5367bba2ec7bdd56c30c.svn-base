/*
 * File    : ShareResourceDirContentsImpl.java
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

import java.io.*;
import java.util.*;

import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.internat.*;

public class 
ShareResourceDirContentsImpl
	extends		ShareResourceImpl
	implements 	ShareResourceDirContents
{
	private final File			root;
	private final boolean		recursive;
	private final byte[]		personal_key;
	
	protected ShareResource[]		children	= new ShareResource[0];
	
	protected
	ShareResourceDirContentsImpl(
		ShareManagerImpl	_manager,
		File				_dir,
		boolean				_recursive,
		boolean				_personal,
		boolean				_async_check )

		throws ShareException
	{
		super( _manager, ST_DIR_CONTENTS );
		
		root 		= _dir;
		recursive	= _recursive;
		
		if ( !root.exists()){
			
			throw( new ShareException( "Dir '" + root.getName() + "' not found"));
		}
		
		if ( root.isFile()){
			
			throw( new ShareException( "Not a directory"));
		}
		
		personal_key = _personal?RandomUtils.nextSecureHash():null;
		
			// new resource, trigger processing
		
		if ( _async_check ){
			
			new AEThread2( "SM:asyncCheck", true )
			{
				public void
				run()
				{
					try{
						checkConsistency();
						
					}catch( Throwable e ){
						
						Debug.out( "Failed to update consistency", e );
					}
				}
			}.start();
			
		}else{
			
		      checkConsistency();
		}
	}
	
	protected
	ShareResourceDirContentsImpl(
		ShareManagerImpl	_manager,
		File				_dir,
		boolean				_recursive,
		Map					_map )

		throws ShareException
	{
		super( _manager, ST_DIR_CONTENTS, _map );
		
		root 		= _dir;
		recursive	= _recursive;
		
			// recovery - see comment below about not failing if dir doesn't exist...
		
		if ( !root.exists()){
			
			Debug.out( "Dir '" + root.getName() + "' not found");
			
			// throw( new ShareException( "Dir '".concat(root.getName()).concat("' not found")));
			
		}else{
		
			if ( root.isFile()){
			
				throw( new ShareException( "Not a directory"));
			}
		}
		
		personal_key = (byte[])_map.get( "per_key" );
		
			// deserialised resource, checkConsistency will be called later to trigger sub-share adding
	}
	
	public boolean
	canBeDeleted()
	
		throws ShareResourceDeletionVetoException
	{
		for (int i=0;i<children.length;i++){
			
			if ( !children[i].canBeDeleted()){
				
				return( false );
			}
		}
		
		return( true );
	}
	
	protected void
	checkConsistency()

		throws ShareException
	{
		// ensure all shares are defined as per dir contents and recursion flag
		
		List	kids = checkConsistency(root);
		
		if ( kids != null ){
			
			children = new ShareResource[kids.size()];
		
			kids.toArray( children );
			
		}else{
			
			children = new ShareResource[0];
		}
	}
	
	protected List
	checkConsistency(
		File		dir )

		throws ShareException
	{
		List	kids = new ArrayList();
		
		File[]	files = dir.listFiles();
		
		if ( files == null || !dir.exists() ){
			
				// dir has been deleted
			
				// actually, this can be bad as some os errors (e.g. "too many open files") can cause the dir
				// to appear to have been deleted. However, we don't want to delete the share. So let's just
				// leave it around, manual delete required if deletion required.
			
			if ( dir == root ){
				
				return( null );
				
			}else{
			
				manager.delete( this, true );
			}
		}else{
					
			for (int i=0;i<files.length;i++){
				
				File	file = files[i];
			
				String	file_name = file.getName();
				
				if (!(file_name.equals(".") || file_name.equals(".." ))){
					
					if ( file.isDirectory()){
						
						if ( recursive ){
							
							List	child = checkConsistency( file );
							
							kids.add( new shareNode( this, file, child ));
							
						}else{
							
							try{
								ShareResource res = manager.getDir( file );
								
								if ( res == null ){
								
									res = manager.addDir( this, file, personal_key != null );
								}
								
								kids.add( res );
								
							}catch( Throwable e ){
								
								Debug.printStackTrace( e );
							}
						}
					}else{
		
						try{
							ShareResource res = manager.getFile( file );
							
							if ( res == null ){
								
								res = manager.addFile( this, file, personal_key != null );
							}
							
							kids.add( res );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace( e );
						}
					}
				}
			}
			
			for (int i=0;i<kids.size();i++){
				
				Object	o = kids.get(i);
				
				if ( o instanceof ShareResourceImpl ){
			
					((ShareResourceImpl)o).setParent(this);
				}else{
					
					((shareNode)o).setParent(this);
				}
			}
		}
		
		return( kids );
	}
	
	protected void
	deleteInternal()
	{
		for (int i=0;i<children.length;i++){
			
			try{
				if ( children[i] instanceof ShareResourceImpl ){
				
					((ShareResourceImpl)children[i]).delete(true);
				}else{
					
					((shareNode)children[i]).delete(true);
					
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	protected void
	serialiseResource(
		Map		map )
	{
		super.serialiseResource( map );
		
		map.put( "type", new Long(getType()));
		
		map.put( "recursive", new Long(recursive?1:0));
		
		try{
			map.put( "file", root.toString().getBytes( Constants.DEFAULT_ENCODING));
			
		}catch( UnsupportedEncodingException e ){
			
			Debug.printStackTrace( e );
		}
		
		if ( personal_key != null ){
			
			map.put( "per_key", personal_key );
		}
	}
	
	protected static ShareResourceImpl
	deserialiseResource(
		ShareManagerImpl	manager,
		Map					map )
	
		throws ShareException
	{
		try{
			File root = new File(new String((byte[])map.get("file"), Constants.DEFAULT_ENCODING));
		
			boolean	recursive = ((Long)map.get("recursive")).longValue() == 1;
		
			ShareResourceImpl res  = new ShareResourceDirContentsImpl( manager, root, recursive, map );
						
			return( res );
			
		}catch( UnsupportedEncodingException e ){
			
			throw( new ShareException( "internal error", e ));
		}
	}
	
	public String
	getName()
	{
		return( root.toString());
	}
	
	public File
	getRoot()
	{
		return( root );
	}
	
	public boolean
	isRecursive()
	{
		return( recursive );
	}
		
	public ShareResource[]
	getChildren()
	{
		return( children );
	}
	
	protected class
	shareNode
		implements ShareResourceDirContents
	{
		protected ShareResourceDirContents	node_parent;
		protected File						node;
		protected ShareResource[]			node_children;
		
		protected
		shareNode(
			ShareResourceDirContents	_parent,
			File						_node,
			List						kids )
		{
			node_parent	= _parent;
			node		=_node;
			
			node_children = new ShareResource[kids.size()];
			
			kids.toArray( node_children );
			
			for (int i=0;i<node_children.length;i++){
				
				Object	o = node_children[i];
				
				if ( o instanceof ShareResourceImpl ){
					
					((ShareResourceImpl)o).setParent( this );
				}else{
					
					((shareNode)o).setParent( this );

				}
			}
		}
		
		public ShareResourceDirContents
		getParent()
		{
			return( node_parent );
		}
		
		protected void
		setParent(
			ShareResourceDirContents	_parent )
		{
			node_parent	= _parent;
		}
		
		public int
		getType()
		{
			return( ShareResource.ST_DIR_CONTENTS );
		}
		
		public String
		getName()
		{
			return( node.toString());
		}
		
		public void
		setAttribute(
			TorrentAttribute		attribute,
			String					value )
		{
			for (int i=0;i<node_children.length;i++){
				
				node_children[i].setAttribute( attribute, value );
			}
		}
		
		public String
		getAttribute(
			TorrentAttribute		attribute )
		{
			return( null );
		}
		
		public TorrentAttribute[]
		getAttributes()
		{
			return( new TorrentAttribute[0]);
		}
		
		public void
		delete()
		
			throws ShareResourceDeletionVetoException
		{
			throw( new ShareResourceDeletionVetoException( MessageText.getString("plugin.sharing.remove.veto")));
		}
		
		public void
		delete(
			boolean	force )
		
			throws ShareException, ShareResourceDeletionVetoException
		{
			for (int i=0;i<node_children.length;i++){
				
				Object	o = node_children[i];
				
				if ( o instanceof ShareResourceImpl ){
					
					((ShareResourceImpl)o).delete(force);
				}else{
					
					((shareNode)o).delete(force);
				}
			}
		}
		
		
		public boolean
		canBeDeleted()
		
			throws ShareResourceDeletionVetoException
		{
			for (int i=0;i<node_children.length;i++){
				
				node_children[i].canBeDeleted();
			}
			
			return( true );
		}
		
		public File
		getRoot()
		{
			return( node );
		}
			
		public boolean
		isRecursive()
		{
			return( recursive );
		}
			
		public ShareResource[]
		getChildren()
		{
			return( node_children );
		}
		
		public void
		addChangeListener(
			ShareResourceListener	l )
		{
		}
		
		public void
		removeChangeListener(
			ShareResourceListener	l )
		{
		}
		
		public void
		addDeletionListener(
			ShareResourceWillBeDeletedListener	l )
		{
		}
		
		public void
		removeDeletionListener(
			ShareResourceWillBeDeletedListener	l )
		{
		}
	}
}
