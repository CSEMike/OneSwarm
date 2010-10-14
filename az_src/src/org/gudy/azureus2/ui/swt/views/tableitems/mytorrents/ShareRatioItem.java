/*
 * File    : ShareRatioItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.eclipse.swt.graphics.Color;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.ui.tables.*;


/**
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class ShareRatioItem
       extends CoreTableColumn 
       implements TableCellRefreshListener, ParameterListener
{
  private final static String CONFIG_ID = "StartStopManager_iFirstPriority_ShareRatio";
  private int iMinShareRatio;

  /** Default Constructor */
  public ShareRatioItem(String sTableID, boolean visible) {
    super("shareRatio", ALIGN_TRAIL, POSITION_LAST, 70, sTableID);
		setType(TableColumn.TYPE_TEXT);
    setRefreshInterval(INTERVAL_LIVE);
    setMinWidthAuto(true);

    if (visible)
      setPosition(POSITION_LAST);
    else
      setPosition(POSITION_INVISIBLE);

    iMinShareRatio = COConfigurationManager.getIntParameter(CONFIG_ID);
    COConfigurationManager.addParameterListener(CONFIG_ID, this);
  }

  protected void finalize() throws Throwable {
    super.finalize();
    COConfigurationManager.removeParameterListener(CONFIG_ID, this);
  }

  public void refresh(TableCell cell) {
    DownloadManager dm = (DownloadManager)cell.getDataSource();
                       
    int sr = (dm == null) ? 0 : dm.getStats().getShareRatio();
    if (sr == -1)
      sr = Constants.INFINITY_AS_INT;
      
    if (!cell.setSortValue(sr) && cell.isValid())
      return;
    
    String shareRatio = "";
    
    if (sr == Constants.INFINITY_AS_INT) {
      shareRatio = Constants.INFINITY_STRING;
    } else {
      String partial = String.valueOf(sr % 1000);
      while (partial.length() < 3) {
        partial = "0" + partial;
      }
      shareRatio = (sr / 1000) + "." + partial;
    }
    
    if( cell.setText(shareRatio) ) {
    	Color color = sr < iMinShareRatio ? Colors.colorWarning : null;
    	cell.setForeground(Utils.colorToIntArray(color));
    }
  }

  public void parameterChanged(String parameterName) {
    iMinShareRatio = COConfigurationManager.getIntParameter(CONFIG_ID);
    invalidateCells();
  }
}
