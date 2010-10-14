/*
 * Created on 19-Nov-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

/**
 * @author parg
 *
 */
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.debug.ObfusticateCellText;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;


public class TorrentPathItem
       extends CoreTableColumn 
       implements TableCellRefreshListener, ObfusticateCellText
{
  /** Default Constructor */
  public TorrentPathItem(String sTableID) {
    super("torrentpath", 150, sTableID);
    setObfustication(true);
    setRefreshInterval(INTERVAL_LIVE);
  }

  public void refresh(TableCell cell) {
    DownloadManager dm = (DownloadManager)cell.getDataSource();
    
    cell.setText((dm == null) ? "" : dm.getTorrentFileName());
  }

	public String getObfusticatedText(TableCell cell) {
		return Debug.secretFileName(cell.getText());
	}
}