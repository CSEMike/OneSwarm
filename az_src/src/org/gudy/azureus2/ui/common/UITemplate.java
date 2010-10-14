/*
 * UITemplate.java
 *
 * Created on 26. Oktober 2003, 22:40
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
 * @author  tobi
 */
public abstract class 
UITemplate 
	implements IUserInterface 
{
  
  private boolean started = false;
  /** Creates a new instance of UITemplate */
  public UITemplate() {
  }
  
  public void init(boolean first, boolean others) {
  }
  
  abstract public void openTorrent(String fileName);
  
  abstract public String[] processArgs(String[] args);
  
  public void startUI() {
    started = true;
  }
   
  public boolean isStarted() {
    return started;
  }
  
}
