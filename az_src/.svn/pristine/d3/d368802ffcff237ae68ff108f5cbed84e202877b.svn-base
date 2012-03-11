/*
 * Created on 2004/May/23
 * Created by TuxPaper
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 * 
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.ui.swt.pluginsimpl;


import org.eclipse.swt.graphics.Image;

import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;

/** An SWT image to be used in Azureus
 *
 * @see SWTManager.createGraphic
 */
public class 
UISWTGraphicImpl 
	implements UISWTGraphic
{
	Image img;
  
  public UISWTGraphicImpl(Image newImage) {
    img = newImage;
  }

  public Image getImage() {
  	if (img == null || img.isDisposed()) {
  		return null;
  	}
    return img;
  }

  public boolean setImage(Image newImage) {
    if (img == newImage)
      return false;
    img = newImage;
    return true;
  }
}
