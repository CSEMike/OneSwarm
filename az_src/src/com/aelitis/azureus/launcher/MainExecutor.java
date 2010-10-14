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
package com.aelitis.azureus.launcher;

import java.lang.reflect.Method;

/**
 * @author Aaron Grunthal
 * @create 28.12.2007
 */
public class MainExecutor {

	static void load(final ClassLoader loader,final String mainClass, final String[] args)
	{
		Thread t = new Thread(new Runnable() {
			public void run() {
				try
				{
					Method main = loader.loadClass(mainClass).getMethod("main", new Class[] {String[].class});
					main.invoke(null, new Object[] {args});
				} catch (Exception e)
				{
					System.err.println("Invoking main failed");
					e.printStackTrace();
					System.exit(1);
				}
			}
		},"MainRunner");
		t.setContextClassLoader(loader);
		t.start();
	}

}
