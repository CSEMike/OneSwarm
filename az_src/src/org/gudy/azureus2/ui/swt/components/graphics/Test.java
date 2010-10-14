/*
 * Created on 11 oct. 2004
 * Created by Olivier Chalouhi
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
package org.gudy.azureus2.ui.swt.components.graphics;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;



/**
 * @author Olivier Chalouhi
 *
 */
public class Test {

  final static int IMAGES_TO_CREATE = 100;
  
  public static void main (String [] args) {
  	Display display = new Display ();
  	Image[] images = new Image[IMAGES_TO_CREATE];
  	for(int i = 0 ; i < IMAGES_TO_CREATE ; i++) {
  	  images[i] = new Image(display,800,600);
  	}
  	for(int i = 0 ; i < IMAGES_TO_CREATE ; i++) {
  	  images[i].dispose();
  	}
  	display.dispose ();
  }
  
}
