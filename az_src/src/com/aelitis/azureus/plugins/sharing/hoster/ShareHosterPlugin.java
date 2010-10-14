/*
 * File    : ShareHosterPlugin.java
 * Created : 05-Jan-2004
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

package com.aelitis.azureus.plugins.sharing.hoster;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.plugins.download.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;

public class 
ShareHosterPlugin
	implements Plugin, PluginListener, ShareManagerListener
{
	protected PluginInterface	plugin_interface;
	protected LoggerChannel		log;
	protected Tracker			tracker;
	protected ShareManager		share_manager;
	protected DownloadManager	download_manager;

	protected Map				resource_dl_map = new HashMap();
	protected Map				resource_tt_map = new HashMap();
	
	protected Download			download_being_removed;
	protected TrackerTorrent	torrent_being_removed;
		
	protected boolean			initialised	= false;
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	{
		plugin_interface = _plugin_interface;
		
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		"Share Hoster" );

		log	= plugin_interface.getLogger().getChannel("ShareHosterPlugin");
		
		log.log( LoggerChannel.LT_INFORMATION, "ShareHosterPlugin: initialisation starts");
		
		plugin_interface.addListener( this );
	}
	
	public void
	initializationComplete()
	{
			// take this process off the main thread as we don't want share-recreation to hang
			// initialisation
		
		plugin_interface.getUtilities().createThread(
			"ShareHosterImplugin::init",
			new Runnable()
			{
				public void
				run()
				{
					initialise();
				}
			});
	}
	
	protected void
	initialise()
	{
		log.log( LoggerChannel.LT_INFORMATION, "ShareHosterPlugin: initialisation complete");
		
		Thread.currentThread().setPriority( Thread.MIN_PRIORITY );
		
		try{
			tracker	=  plugin_interface.getTracker();
	
			download_manager = plugin_interface.getDownloadManager();
			
			share_manager = plugin_interface.getShareManager();
						
			share_manager.addListener( this );
			
			share_manager.initialise();
			
			initialised	= true;
			
			ShareResource[]	shares = share_manager.getShares();
			
			for ( int i=0;i<shares.length;i++){
				
				resourceAdded( shares[i] );
			}
			
		}catch( ShareException e ){
			
			Debug.printStackTrace( e );
			
			log.log( e );
			
		}finally{
			
			plugin_interface.getPluginManager().firePluginEvent( PluginEvent.PEV_INITIAL_SHARING_COMPLETE );
		}
	}
	
	public void
	closedownInitiated()
	{
	}
	
	public void
	closedownComplete()
	{
	}
	
	public void
	resourceAdded(
		final ShareResource		resource )
	{
		if ( initialised ){
			
			log.log( LoggerChannel.LT_INFORMATION, "Resource added:".concat(resource.getName()));
			
			try{
				
				resource.addDeletionListener(
					new ShareResourceWillBeDeletedListener()
					{
						public void
						resourceWillBeDeleted(
							ShareResource	resource )
						
							throws ShareResourceDeletionVetoException
						{
							canResourceBeDeleted( resource );
						}
					});
				
				Download	new_download = null;
				
				int	type = resource.getType();
				
				if ( type == ShareResource.ST_FILE ){
					
					ShareResourceFile	file_resource = (ShareResourceFile)resource;
					
					ShareItem	item = file_resource.getItem();
			
					Torrent torrent = item.getTorrent();
					
					Download	download = download_manager.getDownload( torrent );
					
					if ( download == null ){
						
						new_download = download_manager.addNonPersistentDownload( torrent, item.getTorrentFile(), file_resource.getFile());
					}
				}else if ( type == ShareResource.ST_DIR ){
				
					ShareResourceDir	dir_resource = (ShareResourceDir)resource;
					
					ShareItem	item = dir_resource.getItem();
					
					Torrent torrent = item.getTorrent();
					
					Download	download = download_manager.getDownload( torrent );
					
					if ( download == null ){
						
						new_download = download_manager.addNonPersistentDownload( torrent, item.getTorrentFile(), dir_resource.getDir());
					}
				}
				
				if ( new_download != null ){

					final Download	f_new_download = new_download;
					
					resource_dl_map.put( resource, new_download );
					
					resource.addChangeListener(
						new ShareResourceListener()
						{
							public void
							shareResourceChanged(
								ShareResource			resource,
								ShareResourceEvent		event )
							{
								if ( event.getType() == ShareResourceEvent.ET_ATTRIBUTE_CHANGED ){
							
									TorrentAttribute	attribute = (TorrentAttribute)event.getData();
									
									// System.out.println( "sh: res -> ds: " + attribute.getName() + "/" + resource.getAttribute( attribute ));
									
									f_new_download.setAttribute(
											attribute,
											resource.getAttribute( attribute ));
								}
							}
						});
						
					TorrentAttribute[]	attributes = resource.getAttributes();
					
					for (int i=0;i<attributes.length;i++){
						
						TorrentAttribute	ta = attributes[i];
									
						new_download.setAttribute( ta,	resource.getAttribute( ta ));
					}
					
					new_download.addPropertyListener(
						new DownloadPropertyListener()
						{
							public void
							propertyChanged(
								Download				download,
								DownloadPropertyEvent	event )
							{
								if ( event.getType() == DownloadPropertyEvent.PT_TORRENT_ATTRIBUTE_WRITTEN ){
									
									TorrentAttribute	at = (TorrentAttribute)event.getData();
									
									// System.out.println( "sh: rs -> res " + at.getName() + "/" + download.getAttribute( at ));
									
									resource.setAttribute( at, download.getAttribute( at ));
						
								}
							}
						});
					
					Torrent	dl_torrent = new_download.getTorrent();
					
					if ( dl_torrent != null ){
						
						TrackerTorrent	tt = tracker.host(dl_torrent, false );
						
						tt.addRemovalListener(
								new TrackerTorrentWillBeRemovedListener()
								{
									public void
									torrentWillBeRemoved(
										TrackerTorrent	tt )
									
										throws TrackerTorrentRemovalVetoException
									{
										if ( tt != torrent_being_removed ){
											
											throw( new TrackerTorrentRemovalVetoException(
													MessageText.getString("plugin.sharing.torrent.remove.veto")));
										}
									}								
								});
						
						resource_tt_map.put( resource, tt );
					}
					
					new_download.addDownloadWillBeRemovedListener(
							new DownloadWillBeRemovedListener()
							{
								public void
								downloadWillBeRemoved(
									Download	dl )
								
									throws DownloadRemovalVetoException
								{
									if ( dl != download_being_removed ){
										
										throw( new DownloadRemovalVetoException(
													MessageText.getString("plugin.sharing.download.remove.veto")));
									}
								}
							});
				}
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	protected void
	canResourceBeDeleted(
		ShareResource	resource )
	
		throws ShareResourceDeletionVetoException
	{
		Download	dl = (Download)resource_dl_map.get(resource);
		
		if ( dl != null ){
			
			try{
				download_being_removed	= dl;
				
				dl.canBeRemoved();
				
			}catch( DownloadRemovalVetoException e ){
				
				throw( new ShareResourceDeletionVetoException( e.getMessage()));
				
			}finally{
				
				download_being_removed	= null;
			}
		}
		
		TrackerTorrent	tt = (TrackerTorrent)resource_tt_map.get(resource);
		
		if ( tt != null ){
		
			try{
				torrent_being_removed	= tt;
				
				tt.canBeRemoved();
				
			}catch( TrackerTorrentRemovalVetoException e ){
				
				throw( new ShareResourceDeletionVetoException( e.getMessage()));
				
			}finally{
				
				torrent_being_removed	= null;
			}
		}	
	}
	
	public void
	resourceModified(
		ShareResource		resource )
	{
		log.log( LoggerChannel.LT_INFORMATION, "Resource modified:".concat(resource.getName()));
		
		if ( initialised ){
			
			resourceDeleted( resource );
			
			resourceAdded( resource );
		}
	}
	
	public void
	resourceDeleted(
		ShareResource		resource )
	{
		log.log( LoggerChannel.LT_INFORMATION, "Resource deleted:".concat(resource.getName()));
		
		if ( initialised ){
		
			Download	dl = (Download)resource_dl_map.get(resource);
			
			if ( dl != null ){
				
				try{
					download_being_removed	= dl;
					
						// the resource has gone! stop torrent if running to permit 
						// deletion
					
					try{
						dl.stop();
						
					}catch( Throwable e ){
						
						// ignore this as it might already be stopped
					}
					
					dl.remove();
					
				}catch( Throwable e ){
					
					Debug.printStackTrace( e );
					
				}finally{
					
					download_being_removed	= null;
				}
				
				resource_dl_map.remove( resource );
			}	
			
			TrackerTorrent	tt = (TrackerTorrent)resource_tt_map.get(resource);
			
			if ( tt != null ){
				
				try{
					torrent_being_removed	= tt;
					
					tt.remove();
					
				}catch( Throwable e ){
					
					Debug.printStackTrace( e );
					
				}finally{
					
					torrent_being_removed	= null;
				}
				
				resource_tt_map.remove( resource );
			}	
		}
	}

	public void
	reportProgress(
		int		percent_complete )
	{
	}
	
	public void
	reportCurrentTask(
		String	task_description )
	{
		log.log( LoggerChannel.LT_INFORMATION, "Current Task:".concat(task_description) );
	}
}