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

package org.gudy.azureus2.ui.swt.plugins;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.eclipse.swt.graphics.Image;

/** An SWT image to be used in Azureus
 *
 * @see UISWTInstance#createGraphic
 */
public interface 
UISWTGraphic 
extends Graphic 
{
  /** Retrieve the Image object 
   *
   * @return image that is stored in this object
   */
	
	public Image getImage();

  /** Sets the image stored in this object to the supplied parameter.
   *
   * @param img new image to be stored in this object
   * @return true - Image Set<br>
   *         false - Image already set to supplied parameter
   */
  
	public boolean setImage(Image img);
}
