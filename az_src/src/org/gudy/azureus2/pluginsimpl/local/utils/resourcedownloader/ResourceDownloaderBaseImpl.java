/*
 * File    : TorrentDownloader2Impl.java
 * Created : 27-Feb-2004
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

package org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public abstract class 
ResourceDownloaderBaseImpl
	implements ResourceDownloader
{
	private List			listeners		= new ArrayList();
	
	private boolean		result_informed;
	private Object		result_informed_data;
	
	private ResourceDownloaderBaseImpl		parent;
	private List							children = new ArrayList();
	
	private boolean		download_cancelled;
	
	private Map			properties	= new HashMap();
	
	protected AEMonitor		this_mon	= new AEMonitor( "ResourceDownloader" );

	protected
	ResourceDownloaderBaseImpl(
		ResourceDownloaderBaseImpl	_parent )
	{
		parent	= _parent;
		
		if ( parent != null ){
			
			parent.addChild(this);
		}
	}
	
	public abstract ResourceDownloaderBaseImpl
	getClone(
		ResourceDownloaderBaseImpl	_parent );

	protected abstract void
	setSize(
		long	size );
	
	public Object
	getProperty(
		String		name )
	
		throws ResourceDownloaderException
	{
			// hack this, properties are read during size acquisition - should treat size as a property
			// too....
		
		getSize();
		
		return( properties.get( name ));
	}
	
	protected void
	setPropertySupport(
		String	name,
		Object	value )
	{
		properties.put( name, value );
	}

	protected void
	setProperties(
		ResourceDownloaderBaseImpl	other )
	{
		Map p = other.properties;
		
		Iterator it = p.keySet().iterator();
		
		while( it.hasNext()){
			
			String	key = (String)it.next();
			
			setProperty( key, p.get(key));
		}
	}
	
	protected abstract void
	setProperty(
		String	name,
		Object	value );

	protected void
	setParent(
		ResourceDownloader		_parent )
	{
		ResourceDownloaderBaseImpl	old_parent	= parent;
		
		parent	= (ResourceDownloaderBaseImpl)_parent;
		
		if( old_parent != null ){
			
			old_parent.removeChild( this );
		}
		
		if ( parent != null ){
			
			parent.addChild( this );
		}
	}
	
	protected ResourceDownloaderBaseImpl
	getParent()
	{
		return( parent );
	}
	
	protected void
	addChild(
		ResourceDownloaderBaseImpl	kid )
	{
		children.add( kid );
	}
	
	protected void
	removeChild(
		ResourceDownloaderBaseImpl	kid )
	{
		children.remove( kid );
	}
	
	protected List
	getChildren()
	{
		return( children );
	}
	
	protected String
	getLogIndent()
	{
		String	indent = "";
		
		ResourceDownloaderBaseImpl	pos = parent;
		
		while( pos != null ){
			
			indent += "  ";
			
			pos = pos.getParent();
		}
		
		return( indent );
	}
	
		// adds a listener that simply logs messages. used during size getting
	
	protected void
	addReportListener(
		ResourceDownloader	rd )
	{
		rd.addListener(
				new ResourceDownloaderAdapter()
				{
					public void
					reportActivity(
						ResourceDownloader	downloader,
						String				activity )
					{
						informActivity( activity );
					}
					
					public void
					failed(
						ResourceDownloader			downloader,
						ResourceDownloaderException e )
					{
						informActivity( downloader.getName() + ":" + e.getMessage());
					}
				});
	}
	
	protected void
	informPercentDone(
		int	percentage )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((ResourceDownloaderListener)listeners.get(i)).reportPercentComplete(this,percentage);
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	protected void
	informAmountComplete(
		long	amount )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((ResourceDownloaderListener)listeners.get(i)).reportAmountComplete(this,amount);
				
			}catch( NoSuchMethodError e ){
				
				// handle addition of this new method with old impls
			}catch( AbstractMethodError e ){
				
				// handle addition of this new method with old impls
			
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	public void
	reportActivity(
		String	str )
	{
		informActivity( str );
	}
	
	protected void
	informActivity(
		String	activity )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((ResourceDownloaderListener)listeners.get(i)).reportActivity(this,activity);
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	protected boolean
	informComplete(
		InputStream	is )
	{
		if ( !result_informed ){
			
			for (int i=0;i<listeners.size();i++){
				
				try{
					if ( !((ResourceDownloaderListener)listeners.get(i)).completed(this,is)){
						
						return( false );
					}
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
					
					return( false );
				}
			}
			
			result_informed	= true;
			
			result_informed_data	= is;
		}
		
		return( true );
	}
	
	protected void
	informFailed(
		ResourceDownloaderException	e )
	{
		if ( !result_informed ){
			
			result_informed	= true;
		
			result_informed_data = e;
			
			for (int i=0;i<listeners.size();i++){
				
				try{
					((ResourceDownloaderListener)listeners.get(i)).failed(this,e);
					
				}catch( Throwable f ){
					
					Debug.printStackTrace(f);
				}
			}
		}
	}
	
	public void
	reportActivity(
		ResourceDownloader	downloader,
		String				activity )
	{
		informActivity( activity );
	}
	
	public void
	reportPercentComplete(
		ResourceDownloader	downloader,
		int					percentage )
	{
		informPercentDone( percentage );
	}
	
	public void
	reportAmountComplete(
		ResourceDownloader	downloader,
		long				amount )
	{
		informAmountComplete( amount );
	}
	
	protected void
	setCancelled()
	{
		download_cancelled	= true;
	}
	
	public boolean
	isCancelled()
	{
		return( download_cancelled );
	}
	
	public void
	addListener(
		ResourceDownloaderListener		l )
	{
		listeners.add( l );
		
		if ( result_informed ){
			
			if (result_informed_data instanceof InputStream ){
				
				l.completed( this, (InputStream)result_informed_data);
			}else{
				
				l.failed( this, (ResourceDownloaderException)result_informed_data);
			}
		}
	}
	
	public void
	removeListener(
		ResourceDownloaderListener		l )
	{
		listeners.remove(l);
	}
}
