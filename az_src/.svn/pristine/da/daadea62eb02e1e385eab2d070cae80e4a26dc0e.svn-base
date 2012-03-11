/*
 * Created on 19 Jul 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.core3.disk.impl.access.impl;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManagerRequest;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.SystemTime;

public abstract class 
DiskManagerRequestImpl 
	implements DiskManagerRequest
{
	private static final LogIDs LOGID = LogIDs.DISK;

	private static boolean	DEBUG;
	private static int		next_id;
	
	static{
		COConfigurationManager.addAndFireParameterListener(
			"diskmanager.request.debug.enable",
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String name )
				{
					DEBUG = COConfigurationManager.getBooleanParameter( name, false );
				}
			});
	}
	
	private long	start_time;
	private String	name;
	
	
	protected abstract String
	getName();
	
	public void
	requestStarts()
	{
		if ( DEBUG ){
				
			try{
				int	id;
					
				synchronized( DiskManagerRequestImpl.class ){
						
					id = next_id++;
				}
				
				name	= getName() + " [" + id + "]";
				
				start_time = SystemTime.getCurrentTime();
				
				Logger.log(new LogEvent( LOGID, "DMRequest start: " + name ));
				
			}catch( Throwable e ){
			}
		}
	}
	
	public void
	requestEnds(
		boolean	ok )
	{
		if ( DEBUG ){
			
			try{
				Logger.log(new LogEvent( LOGID, "DMRequest end: " + name + ",ok=" + ok + ", time=" + ( SystemTime.getCurrentTime() - start_time )));
				
			}catch( Throwable e ){
			}
		}	
	}
}
