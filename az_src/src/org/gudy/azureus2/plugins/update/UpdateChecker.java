/*
 * Created on 11-May-2004
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

package org.gudy.azureus2.plugins.update;

/**
 * @author parg
 *
 */


import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public interface 
UpdateChecker 
{
	public UpdatableComponent
	getComponent();
	
	public UpdateCheckInstance
	getCheckInstance();
	
		/**
		 * Add an update with a single downloader
		 * @param mandatory indicates that in a group of updates this one must succeed
		 */
	
	public Update
	addUpdate(
		String				name,
		String[]			description,
		String				new_version,
		ResourceDownloader	downloader,
		int					restart_required );
	
		/**
		 * Add an update with a number of downloaders
		 */
	
	public Update
	addUpdate(
		String					name,
		String[]				description,
		String					new_version,
		ResourceDownloader[]	downloaders,
		int						restart_required );
	
		/**
		 * For updates that require a stop, update-action and then start you create an installer.
		 * This allows the specification of actions such as the replacement of a DLL 
		 * @return
		 */
	
	public UpdateInstaller
	createInstaller()
		
		throws UpdateException;
	
		/**
		 * Indicate that update checking is complete and that any updates required have
		 * been added by the addUpdate methods
		 */
	
	public void
	completed();
	
		/**
		 * Indicates that the update check failed. Of particular importance for mandatory
		 * components (e.g. AZ core) as failure of a mandatory one causes all other
		 * updates to be aborted 
		 */
	
	public void
	failed();
	
		/**
		 * report a progress string to registered listeners
		 * @param str
		 */
	
	public void
	reportProgress(
		String	str );
	
	public void
	addListener(
		UpdateCheckerListener	l );
	
	public void
	removeListener(
		UpdateCheckerListener	l );
	
	public void
	addProgressListener(
		UpdateProgressListener	l );
	
	public void
	removeProgressListener(
		UpdateProgressListener	l );
}
