/*
 * IUserInterface.java
 *
 * Created on 9. Oktober 2003, 00:07
 *
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
public interface IUserInterface {
  
  /** Initializes the UI. 
   * The UI should not be started at this stage.
   *
   * @param first This UI Instance is the first on the command line and should take control of singular stuff (LocaleUtil and torrents added via Command Line).
   * @param others Indicates wether other UIs run along.
   */
  public void init(boolean first, boolean others);
  /** Process UI specific command line arguments.
   * @return Unprocessed Args
   */
  public String[] processArgs(String[] args);
  /** Start the UI.
   * Now the GlobalManager is initialized.
   */
  public void startUI();
  /** Determine if the UI is already started
   * You usually don't need to override this from UITemplate
   */
  public boolean isStarted();
  /** Open a torrent file.
   * This is for torrents passed in the command line. Only called for the first UI.
   */
  public void openTorrent(final String fileName);
  
}
