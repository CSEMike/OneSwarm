/*
 * UserInterfaceFactory.java
 *
 * Created on 9. Oktober 2003, 00:33
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.ui.common;

/**
 *
 * @author  Tobias Minich
 */
public class UserInterfaceFactory {
  
  /** Creates a new instance of UserInterfaceFactory */
  public static IUserInterface getUI(String ui) {
    IUserInterface cui = null;
    String uiclass = "org.gudy.azureus2.ui."+ui+".UI";
    try {
      cui = (IUserInterface) Class.forName(uiclass).newInstance();
    } catch (ClassNotFoundException e) {
      throw new Error("Could not find class: "+uiclass);
    } catch (InstantiationException e) {
      throw new Error("Could not instantiate User Interface: "+ uiclass);
    } catch (IllegalAccessException e) {
      throw new Error("Could not access User Interface: "+ uiclass);
    }
    return cui;
  }
      
  
}
