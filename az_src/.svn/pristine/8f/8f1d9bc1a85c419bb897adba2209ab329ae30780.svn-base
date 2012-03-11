/*
 * File    : TorrentManagerImpl.java
 * Created : 28-Feb-2004
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

package org.gudy.azureus2.pluginsimpl.local.torrent;

/**
 * @author parg
 *
 */

import java.util.*;
import java.net.URL;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.torrent.*;

import com.aelitis.azureus.core.util.CopyOnWriteList;

public class
TorrentManagerImpl
	implements TorrentManager, TOTorrentProgressListener
{
	private static TorrentManagerImpl	singleton;
	private static AEMonitor 			class_mon 	= new AEMonitor( "TorrentManager" );

	private static TorrentAttribute	category_attribute = new TorrentAttributeCategoryImpl();
	private static TorrentAttribute	share_properties_attribute = new TorrentAttributeSharePropertiesImpl();
	private static TorrentAttribute	networks_attribute = new TorrentAttributeNetworksImpl();
	private static TorrentAttribute	peer_sources_attribute = new TorrentAttributePeerSourcesImpl();
	private static TorrentAttribute	tr_ext_attribute = new TorrentAttributeTrackerClientExtImpl();
	private static TorrentAttribute disp_name_attribute = new TorrentAttributeDisplayNameImpl();
	private static TorrentAttribute comment_attribute = new TorrentAttributeUserCommentImpl();
	private static TorrentAttribute relative_save_path_attribute = new TorrentAttributeRelativeSavePathImpl();

	private static Map	attribute_map = new HashMap();

	static{
		attribute_map.put( TorrentAttribute.TA_CATEGORY, 				category_attribute );
		attribute_map.put( TorrentAttribute.TA_SHARE_PROPERTIES, 		share_properties_attribute );
		attribute_map.put( TorrentAttribute.TA_NETWORKS, 				networks_attribute );
		attribute_map.put( TorrentAttribute.TA_PEER_SOURCES, 			peer_sources_attribute );
		attribute_map.put( TorrentAttribute.TA_TRACKER_CLIENT_EXTENSIONS, tr_ext_attribute );
		attribute_map.put( TorrentAttribute.TA_DISPLAY_NAME,            disp_name_attribute );
		attribute_map.put( TorrentAttribute.TA_USER_COMMENT,            comment_attribute);
		attribute_map.put( TorrentAttribute.TA_RELATIVE_SAVE_PATH,      relative_save_path_attribute);
	}

	public static TorrentManagerImpl
	getSingleton()
	{
		try{
			class_mon.enter();

			if ( singleton == null ){

					// default singleton not attached to a plugin

				singleton = new TorrentManagerImpl(null);
			}

			return( singleton );

		}finally{

			class_mon.exit();
		}
	}

	protected static List		listeners = new ArrayList();

	protected PluginInterface	plugin_interface;

	protected
	TorrentManagerImpl(
		PluginInterface		_pi )
	{
		plugin_interface	= _pi;
	}

	public TorrentManager
	specialise(
		PluginInterface		_pi )
	{
			// specialised one attached to plugin

		return( new TorrentManagerImpl( _pi ));
	}

	public TorrentDownloader
	getURLDownloader(
		URL		url )

		throws TorrentException
	{
		return( new TorrentDownloaderImpl( this, url ));
	}

	public TorrentDownloader
	getURLDownloader(
		URL		url,
		String	user_name,
		String	password )

		throws TorrentException
	{
		return( new TorrentDownloaderImpl( this, url, user_name, password ));
	}

	public Torrent
	createFromBEncodedFile(
		File		file )

		throws TorrentException
	{
		return( createFromBEncodedFile( file, false ));
	}

	public Torrent
	createFromBEncodedFile(
		File		file,
		boolean		for_seeding )

		throws TorrentException
	{
		try{
			TOTorrent	torrent;

			if ( for_seeding ){

				torrent = TorrentUtils.readFromFile( file, true, true );

			}else{

				torrent = TorrentUtils.readFromFile( file, false );
			}

			return( new TorrentImpl(plugin_interface,torrent));

		}catch( TOTorrentException e ){

			throw( new TorrentException( "TorrentManager::createFromBEncodedFile Fails", e ));
		}
	}

	public Torrent
	createFromBEncodedInputStream(
		InputStream		data )

		throws TorrentException
	{
		try{
			return( new TorrentImpl(plugin_interface,TorrentUtils.readFromBEncodedInputStream( data )));

		}catch( TOTorrentException e ){

			throw( new TorrentException( "TorrentManager::createFromBEncodedFile Fails", e ));
		}
	}

	public Torrent
	createFromBEncodedData(
		byte[]		data )

		throws TorrentException
	{
		ByteArrayInputStream	is = null;

		try{
			is = new ByteArrayInputStream( data );

			return( new TorrentImpl(plugin_interface,TorrentUtils.readFromBEncodedInputStream(is)));

		}catch( TOTorrentException e ){

			throw( new TorrentException( "TorrentManager::createFromBEncodedData Fails", e ));

		}finally{

			try{
				is.close();

			}catch( Throwable e ){

				Debug.printStackTrace( e );
			}
		}
	}

	public Torrent
	createFromDataFile(
		File		data,
		URL			announce_url )

		throws TorrentException
	{
		return( createFromDataFile( data, announce_url, false ));
	}

	public Torrent
	createFromDataFile(
		File		data,
		URL			announce_url,
		boolean		include_other_hashes )

		throws TorrentException
	{
		try{
			TOTorrentCreator c = TOTorrentFactory.createFromFileOrDirWithComputedPieceLength( data, announce_url, include_other_hashes);

			c.addListener( this );

			return( new TorrentImpl(plugin_interface,c.create()));

		}catch( TOTorrentException e ){

			throw( new TorrentException( "TorrentManager::createFromDataFile Fails", e ));
		}
	}

	public TorrentCreator
	createFromDataFileEx(
		File					data,
		URL						announce_url,
		boolean					include_other_hashes )

		throws TorrentException
	{
		try{
			final TOTorrentCreator c = TOTorrentFactory.createFromFileOrDirWithComputedPieceLength( data, announce_url, include_other_hashes);

			return(
				new TorrentCreator()
				{
					private CopyOnWriteList	listeners = new CopyOnWriteList();

					public void
					start()
					{
						c.addListener(
							new TOTorrentProgressListener()
							{
								public void
								reportProgress(
									int		percent_complete )
								{
									for (Iterator it=listeners.iterator();it.hasNext();){

										((TorrentCreatorListener)it.next()).reportPercentageDone( percent_complete );
									}
								}

								public void
								reportCurrentTask(
									String	task_description )
								{
									for (Iterator it=listeners.iterator();it.hasNext();){

										((TorrentCreatorListener)it.next()).reportActivity( task_description );
									}
								}
							});

						new AEThread( "TorrentManager::create", true )
						{
							public void
							runSupport()
							{
								try{
									TOTorrent	t = c.create();

									Torrent	torrent = new TorrentImpl( plugin_interface, t );

									for (Iterator it=listeners.iterator();it.hasNext();){

										((TorrentCreatorListener)it.next()).complete( torrent );
									}

								}catch( TOTorrentException e ){

									for (Iterator it=listeners.iterator();it.hasNext();){

										((TorrentCreatorListener)it.next()).failed( new TorrentException( e ));
									}

								}
							}
						}.start();
					}

					public void
					cancel()
					{
						c.cancel();
					}

					public void
					addListener(
						TorrentCreatorListener listener )
					{
						listeners.add( listener );
					}

					public void
					removeListener(
						TorrentCreatorListener listener )
					{
						listeners.remove( listener );
					}
				});

		}catch( TOTorrentException e ){

			throw( new TorrentException( "TorrentManager::createFromDataFile Fails", e ));
		}
	}

	public TorrentAttribute[]
	getDefinedAttributes()
	{
		try{
			class_mon.enter();

			Collection	entries = attribute_map.values();

			TorrentAttribute[]	res = new TorrentAttribute[entries.size()];

			entries.toArray( res );

			return( res );

		}finally{

			class_mon.exit();
		}
	}

	public TorrentAttribute
	getAttribute(
		String		name )
	{
		try{
			class_mon.enter();

			TorrentAttribute	res = (TorrentAttribute)attribute_map.get(name);

			if ( res == null && name.startsWith( "Plugin." )){

				res = new TorrentAttributePluginImpl( name );

				attribute_map.put( name, res );
			}

			if (res == null) {throw new IllegalArgumentException("No such attribute: \"" + name + "\"");}
			return( res );

		}finally{

			class_mon.exit();
		}
	}

	public TorrentAttribute
	getPluginAttribute(
		String		name )
	{
			// this prefix is RELIED ON ELSEWHERE!!!!

		name	= "Plugin." + plugin_interface.getPluginID() + "." + name;

		try{
			class_mon.enter();

			TorrentAttribute	res = (TorrentAttribute)attribute_map.get(name);

			if ( res != null ){

				return( res );
			}

			res = new TorrentAttributePluginImpl( name );

			attribute_map.put( name, res );

			return( res );

		}finally{

			class_mon.exit();
		}
	}

	public Torrent
	createFromBEncodedData(
			byte[] data,
			int preserve)

			throws TorrentException
	{
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		try {
			TOTorrent torrent = TOTorrentFactory.deserialiseFromBEncodedInputStream( bais );
			return new TorrentImpl(plugin_interface, preserveFields(torrent,preserve));
		} catch (TOTorrentException e) {
			throw new TorrentException ("Failed to read TorrentData", e);
		} finally {
			try {
				bais.close();
			} catch (IOException e) {}
		}
	}


	public Torrent
	createFromBEncodedFile(
			File file,
			int preserve)

			throws TorrentException
	{
		FileInputStream fis = null;
		try {
			fis = new FileInputStream (file);
			TOTorrent torrent = TOTorrentFactory.deserialiseFromBEncodedInputStream( fis );
			return new TorrentImpl(plugin_interface, preserveFields(torrent,preserve));
		} catch (FileNotFoundException e) {
			throw new TorrentException ("Failed to read from TorrentFile", e);
		} catch (TOTorrentException e) {
			throw new TorrentException ("Failed to read TorrentData", e);
		} finally {
			if (fis != null)
				try {
					fis.close();
				} catch (IOException e) {}
		}
	}


	public Torrent createFromBEncodedInputStream(InputStream data, int preserve) throws TorrentException
	{
		try {
			TOTorrent torrent = TOTorrentFactory.deserialiseFromBEncodedInputStream( data );
			return new TorrentImpl(plugin_interface, preserveFields(torrent,preserve));
		} catch (TOTorrentException e) {
			throw new TorrentException ("Failed to read TorrentData", e);
		}

	}

	private TOTorrent
	preserveFields (
			TOTorrent torrent,
			int preserve)
	{
		if (preserve == TorrentManager.PRESERVE_ALL) {
			return torrent;
		} else if ((preserve & TorrentManager.PRESERVE_ENCODING) > 0) {
			String encoding = torrent.getAdditionalStringProperty("encoding");
			torrent.removeAdditionalProperties();
			if (encoding != null)
				torrent.setAdditionalStringProperty("encoding", encoding);
		} else if (preserve == TorrentManager.PRESERVE_NONE) {
			torrent.removeAdditionalProperties();
		}
		return torrent;
	}

	public void
	reportProgress(
		int		percent_complete )
	{
	}

	public void
	reportCurrentTask(
		final String	task_description )
	{
		for (Iterator it = listeners.iterator();it.hasNext();){

			((TorrentManagerListener)it.next()).event(
					new TorrentManagerEvent()
					{
						public Object
						getData()
						{
							return( task_description );
						}
					});
		}
	}

	protected void
	tryToSetTorrentEncoding(
		TOTorrent	torrent,
		String		encoding )

		throws TorrentEncodingException
	{
		try{
			LocaleTorrentUtil.setTorrentEncoding( torrent, encoding );

		}catch( LocaleUtilEncodingException e ){

			String[]	charsets = e.getValidCharsets();

			if ( charsets == null ){

				throw( new TorrentEncodingException("Failed to set requested encoding", e));

			}else{

				throw( new TorrentEncodingException(charsets,e.getValidTorrentNames()));
			}
		}
	}

	protected void
	tryToSetDefaultTorrentEncoding(
		TOTorrent		torrent )

		throws TorrentException
	{
		try{
			LocaleTorrentUtil.setDefaultTorrentEncoding( torrent );

		}catch( LocaleUtilEncodingException e ){

			String[]	charsets = e.getValidCharsets();

			if ( charsets == null ){

				throw( new TorrentEncodingException("Failed to set default encoding", e));

			}else{

				throw( new TorrentEncodingException(charsets,e.getValidTorrentNames()));
			}
		}
	}

	public void
	addListener(
		TorrentManagerListener	l )
	{
		try{
			class_mon.enter();

			ArrayList	new_listeners = new ArrayList( listeners );

			new_listeners.add( l );

			listeners	= new_listeners;
		}finally{

			class_mon.exit();
		}
	}

	public void
	removeListener(
		TorrentManagerListener	l )
	{
		try{
			class_mon.enter();

			ArrayList	new_listeners = new ArrayList( listeners );

			new_listeners.remove( l );

			listeners	= new_listeners;

		}finally{

			class_mon.exit();
		}
	}
}