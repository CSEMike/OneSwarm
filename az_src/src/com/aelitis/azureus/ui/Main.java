/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui;

import java.lang.reflect.Constructor;

/**
 * This is the main of all mains! 
 * 
 * @author TuxPaper
 * @created May 17, 2007
 *
 */
public class Main
{
	public static void main(String[] args) {
		
		// For now, fire off the old org.gudy.azureus2.ui.swt.Main class
		//
		// In the future, this class may do some logic to find out available
		// startup classes and pick one (like the uis one does)
		try {
			final Class startupClass = Class.forName("org.gudy.azureus2.ui.swt.Main");

			final Constructor constructor = startupClass.getConstructor(new Class[] {
				String[].class
			});
			
			constructor.newInstance(new Object[] {
				args
			});

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
