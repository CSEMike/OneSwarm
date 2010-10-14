/*
 * Created on 10-May-2004
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

package org.gudy.azureus2.pluginsimpl.local.utils;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.download.*;

import org.gudy.azureus2.pluginsimpl.local.download.*;

import org.gudy.azureus2.plugins.utils.*;

public class 
ShortCutsImpl
	implements ShortCuts
{
	protected PluginInterface			pi;
	
	public
	ShortCutsImpl(
		PluginInterface		_pi )
	{
		pi		= _pi;
	}
	
	public DownloadStats
	getDownloadStats(
		byte[]		hash )
	
		throws DownloadException
	{
		return( getDownload(hash).getStats());
	}
	
	public void
	restartDownload(
		byte[]		hash )
	
		throws DownloadException
	{
		getDownload(hash).restart();
	}
	
	public void
	stopDownload(
		byte[]		hash )
	
		throws DownloadException
	{
		getDownload(hash).stop();
	}
	
	public void
	removeDownload(
		byte[]		hash )
	
		throws DownloadException, DownloadRemovalVetoException
	{
		getDownload(hash).remove();
	}

	public Download
	getDownload(
		byte[]		hash )
	
		throws DownloadException
	{
		Download	dl = ((DownloadManagerImpl)pi.getDownloadManager()).getDownload( hash );
		
		if ( dl == null ){
			
			throw( new DownloadException("Torrent not found" ));
		}
		
		return( dl );
	}
}
