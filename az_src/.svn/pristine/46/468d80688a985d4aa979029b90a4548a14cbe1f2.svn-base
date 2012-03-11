/*
 * File    : RPTorrentManager.java
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

package org.gudy.azureus2.pluginsimpl.remote.torrent;

/**
 * @author parg
 *
 */

import java.net.URL;
import java.io.File;
import java.io.InputStream;

import org.gudy.azureus2.plugins.torrent.*;

import org.gudy.azureus2.pluginsimpl.remote.*;

public class
RPTorrentManager
	extends		RPObject
	implements 	TorrentManager
{
	protected transient TorrentManager		delegate;

	public static RPTorrentManager
	create(
		TorrentManager		_delegate )
	{
		RPTorrentManager	res =(RPTorrentManager)_lookupLocal( _delegate );

		if ( res == null ){

			res = new RPTorrentManager( _delegate );
		}

		return( res );
	}

	protected
	RPTorrentManager(
		TorrentManager		_delegate )
	{
		super( _delegate );
	}

	protected void
	_setDelegate(
		Object		_delegate )
	{
		delegate = (TorrentManager)_delegate;
	}

	public Object
	_setLocal()

		throws RPException
	{
		return( _fixupLocal());
	}


	public RPReply
	_process(
		RPRequest	request	)
	{
		String		method 	= request.getMethod();
		Object[]	params	= request.getParams();

		if ( method.equals( "getURLDownloader[URL]")){

			try{
				TorrentDownloader dl = delegate.getURLDownloader((URL)params[0]);

				RPTorrentDownloader res = RPTorrentDownloader.create( dl );

				return( new RPReply( res ));

			}catch( TorrentException e ){

				return( new RPReply( e ));
			}
		}else if ( method.equals( "getURLDownloader[URL,String,String]")){

			try{
				TorrentDownloader dl = delegate.getURLDownloader((URL)params[0],(String)params[1],(String)params[2]);

				RPTorrentDownloader res = RPTorrentDownloader.create( dl );

				return( new RPReply( res ));

			}catch( TorrentException e ){

				return( new RPReply( e ));
			}
		}else if ( method.equals( "createFromBEncodedData[byte[]]")){

			try{
				return( new RPReply( RPTorrent.create( delegate.createFromBEncodedData((byte[])params[0]))));

			}catch( TorrentException e ){

				return( new RPReply(e));
			}
		}

		throw( new RPException( "Unknown method: " + method ));
	}

	// ************************************************************************

	public TorrentDownloader
	getURLDownloader(
		URL		url )

		throws TorrentException
	{
		try{
			RPTorrentDownloader resp = (RPTorrentDownloader)_dispatcher.dispatch( new RPRequest( this, "getURLDownloader[URL]", new Object[]{url})).getResponse();

			resp._setRemote( _dispatcher );

			return( resp );

		}catch( RPException e ){

			if ( e.getCause() instanceof TorrentException ){

				throw((TorrentException)e.getCause());
			}

			throw( e );
		}
	}

	public TorrentDownloader
	getURLDownloader(
		URL		url,
		String	user_name,
		String	password )

		throws TorrentException
	{
		try{
			RPTorrentDownloader resp = (RPTorrentDownloader)_dispatcher.dispatch( new RPRequest( this, "getURLDownloader[URL,String,String]", new Object[]{url, user_name, password})).getResponse();

			resp._setRemote( _dispatcher );

			return( resp );

		}catch( RPException e ){

			if ( e.getCause() instanceof TorrentException ){

				throw((TorrentException)e.getCause());
			}

			throw( e );
		}
	}

	public Torrent
	createFromBEncodedFile(
		File		file )

		throws TorrentException
	{
		notSupported();

		return( null );
	}

	public Torrent
	createFromBEncodedFile(
		File		file,
		boolean		for_seeding )

		throws TorrentException
	{
		notSupported();

		return( null );
	}
	public Torrent
	createFromBEncodedInputStream(
		InputStream		data )

		throws TorrentException
	{
		notSupported();

		return( null );
	}

	public Torrent
	createFromBEncodedData(
		byte[]		data )

		throws TorrentException
	{
		try{
			RPTorrent	res = (RPTorrent)_dispatcher.dispatch( new RPRequest( this, "createFromBEncodedData[byte[]]", new Object[]{data})).getResponse();

			res._setRemote( _dispatcher );

			return( res );

		}catch( RPException e ){

			if ( e.getCause() instanceof TorrentException ){

				throw((TorrentException)e.getCause());
			}

			throw( e );
		}
	}


	public Torrent
	createFromBEncodedData(
			byte[] data,
			int preserve )

			throws TorrentException {

		notSupported();

		return( null );
	}


	public Torrent
	createFromBEncodedFile(
			File file,
			int preserve )

			throws TorrentException {

		notSupported();

		return( null );
	}

	public Torrent
	createFromBEncodedInputStream(
			InputStream data,
			int preserve )

			throws TorrentException {

		notSupported();

		return( null );
	}

	public Torrent
	createFromDataFile(
		File		data,
		URL			announce_url )

		throws TorrentException
	{
		notSupported();

		return( null );
	}

	public Torrent
	createFromDataFile(
		File		data,
		URL			announce_url,
		boolean		include_other_hashes )

		throws TorrentException
	{
		notSupported();

		return( null );
	}


	public TorrentCreator
	createFromDataFileEx(
		File					data,
		URL						announce_url,
		boolean					include_other_hashes )

		throws TorrentException
	{
		notSupported();

		return( null );
	}

	public TorrentAttribute[]
	getDefinedAttributes()
	{
		notSupported();

		return( null );
	}

	public TorrentAttribute
	getAttribute(
		String		name )
	{
		notSupported();

		return( null );
	}

	public TorrentAttribute
	getPluginAttribute(
		String		name )
	{
		notSupported();

		return( null );
	}

	public void
	addListener(
		TorrentManagerListener	l )
	{
		notSupported();
	}

	public void
	removeListener(
		TorrentManagerListener	l )
	{
		notSupported();
	}
}
