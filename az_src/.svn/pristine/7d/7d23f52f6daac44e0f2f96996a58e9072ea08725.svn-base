/*
 * Created on 25 May 2008
 * Created by Allan Crooks
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 */
package org.gudy.azureus2.ui.swt.plugins;

import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.config.UIParameterContext;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * This is to be used in conjunction with the
 * {@link BasicPluginConfigModel#addUIParameter2(UIParameterContext, String) addUIParameter2}
 * method - any plugin that wants to add a SWT object directly to a configuration
 * section should create a parameter with an object that implements this interface.
 * 
 * @since 3.0.5.3
 */
public interface UISWTParameterContext extends UIParameterContext {
	
	/**
	 * This method is invoked when the config section is going to be displayed
	 * in a SWT user interface.
	 * 
	 * @param c The parent composite object which will contain the SWT object.
	 */
	public void create(Composite c);
}
