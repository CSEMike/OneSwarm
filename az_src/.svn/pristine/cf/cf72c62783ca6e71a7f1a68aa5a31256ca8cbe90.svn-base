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
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.download.Download;
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
	public static final Class DATASOURCE_TYPE = Download.class;

  private final static String CONFIG_ID = "StartStopManager_iFirstPriority_ShareRatio";
	public static final String COLUMN_ID = "shareRatio";
  private int iMinShareRatio;
  private boolean changeFG = true;

  public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] { CAT_SHARING, CAT_SWARM });
	}

	/** Default Constructor */
  public ShareRatioItem(String sTableID) {
    super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_TRAIL, 73, sTableID);
		setType(TableColumn.TYPE_TEXT);
    setRefreshInterval(INTERVAL_LIVE);
    setMinWidthAuto(true);

    setPosition(POSITION_LAST);

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
    
    if ( sr == Integer.MAX_VALUE ){
    	sr = Integer.MAX_VALUE-1;
    }
    if ( sr == -1 ){
      sr = Integer.MAX_VALUE;
    }
    
    if (!cell.setSortValue(sr) && cell.isValid())
      return;
    
    String shareRatio = "";
    
    if (sr == Integer.MAX_VALUE ) {
      shareRatio = Constants.INFINITY_STRING;
    } else {
      shareRatio = DisplayFormatters.formatDecimal((double) sr / 1000, 3);
    }
    
    if( cell.setText(shareRatio) && changeFG ) {
    	Color color = sr < iMinShareRatio ? Colors.colorWarning : null;
    	cell.setForeground(Utils.colorToIntArray(color));
    }
  }

  public void parameterChanged(String parameterName) {
    iMinShareRatio = COConfigurationManager.getIntParameter(CONFIG_ID);
    invalidateCells();
  }

  public boolean isChangeFG() {
		return changeFG;
	}

	public void setChangeFG(boolean changeFG) {
		this.changeFG = changeFG;
	}
}
