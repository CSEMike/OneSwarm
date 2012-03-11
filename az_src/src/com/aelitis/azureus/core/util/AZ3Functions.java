/*
 * Created on Jan 7, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.util;

import java.net.URL;

import org.gudy.azureus2.core3.download.DownloadManager;

public class 
AZ3Functions 
{
	private static volatile provider	provider;
	
	public static void
	setProvider(
		provider		_p )
	{
		provider	= _p;
	}
	
	public static provider
	getProvider()
	{
		return( provider );
	}
	
	public interface
	provider
	{
		public static int SERVICE_SITE_RELATIVE = 27;

		public void
		subscribeToRSS(
			String		name,
			URL 		url,
			int			interval,
			boolean		is_public,
			String		creator_ref )
		
			throws Exception;
		
		public boolean
		canPlay(
			DownloadManager		dm,
			int					file_index );
		
		public void
		play(
			DownloadManager		dm,
			int					file_index );

		public TranscodeTarget[]
		getTranscodeTargets();
		
		public interface
		TranscodeTarget
		{
			public String
			getName();
			
			public TranscodeProfile[]
			getProfiles();
		}
		
		public interface
		TranscodeProfile
		{
			public String
			getUID();
			
			public String
			getName();
		}

		public String
		getDefaultContentNetworkURL(
				int type, 
				Object[] params);
	}
}
