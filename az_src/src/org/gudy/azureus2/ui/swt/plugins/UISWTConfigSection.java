/*
 * Created on 11-Sep-2005
 * Created by Paul Gardner
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.ui.swt.plugins;

import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;

/**
 * 
 * @since 2.3.0.5
 */
public interface 
UISWTConfigSection 
	extends ConfigSection 
{
	  /**
	   * Create your own configuration panel here.  It can be anything that inherits
	   * from SWT's Composite class.
	   * Please be mindful of small screen resolutions
	   *
	   * @param parent The parent of your configuration panel
	   * @return your configuration panel
	   */
	
	  public Composite 
	  configSectionCreate(
			 Composite parent );
	  
	  /**
	   * Indicate if additional options are available to display a hint to the users
	   * 
	   * @return the highest user mode that reveals additional options (0 = Beginner, 1 = Intermediate, 2 = Advanced)
	   */
	  public int maxUserMode();
}