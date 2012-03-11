/*
 * Created on 27-Apr-2004
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

package org.gudy.azureus2.plugins.ui.model;

import org.gudy.azureus2.plugins.logging.*;

/**
 * @author parg
 *
 * <p><b>Note:</b> Only for implementation by Azureus, not plugins.</p>
 */

import org.gudy.azureus2.plugins.ui.components.*;

public interface
BasicPluginViewModel
	extends PluginViewModel
{
		/**
		 * All UI Components are initially enabled - disable if not required
		 * @return
		 */
	
	public UITextField
	getStatus();
	
	public UITextField
	getActivity();
	
	public UITextArea
	getLogArea();
	
	public UIProgressBar
	getProgress();

	/**
	 * 
	 * @param id
	 * 
	 * @since 2.3.0.7
	 */
	public void
	setConfigSectionID(String id);

	/**
	 * 
	 * @return
	 * 
	 * @since 2.3.0.7
	 */
	public String
	getConfigSectionID();
	
	/**
	 * Convenience method to configure this model to receive any logged
	 * messages on the associated channel and display it in the main
	 * window area.
	 * 
	 * <p>
	 * 
	 * You can handle this manually if you want to, by creating your own
	 * {@link LoggerChannelListener} instance and making it append to the
	 * log area on this object.
	 *
	 * @since 3.0.5.3
	 * @param channel The log channel to listen to.
	 */
	public void attachLoggerChannel(LoggerChannel channel);
}
