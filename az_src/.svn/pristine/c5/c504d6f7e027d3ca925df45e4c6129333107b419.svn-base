/*
 * Created on 8 oct. 2004
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
package org.gudy.azureus2.ui.swt.views.tableitems.peers;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;

/**
 * @author Olivier Chalouhi
 *
 */
public class UpSpeedLimitItem 
  extends CoreTableColumn 
  implements TableCellRefreshListener
{
	public static final String COLUMN_ID = "maxupspeed";

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_SETTINGS,
		});
	}
  
	/** Default Constructor */
	public UpSpeedLimitItem(String table_id) {
	  super(COLUMN_ID, ALIGN_TRAIL, POSITION_INVISIBLE, 35, table_id);
	  setRefreshInterval(INTERVAL_LIVE);
	}

	public void refresh(TableCell cell) {
		PEPeer peer = (PEPeer)cell.getDataSource();
	  long value = (peer == null) ? 0 : peer.getUploadRateLimitBytesPerSecond();
	  if (!cell.setSortValue(value) && cell.isValid())
	    return;
	  
	  if(value == -1) {
	    cell.setText(MessageText.getString("MyTorrents.items.UpSpeedLimit.disabled"));
	  } else if(value  == 0) {
	    cell.setText(Constants.INFINITY_STRING);
	  } else {
	    cell.setText(DisplayFormatters.formatByteCountToKiBEtcPerSec(value));
	  }
	}
}
