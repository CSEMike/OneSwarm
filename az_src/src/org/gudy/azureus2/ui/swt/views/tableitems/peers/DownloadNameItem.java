/*
 * Created on 14 Sep 2007
 * Created by Allan Crooks
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.ui.swt.views.tableitems.peers;

import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author Allan Crooks
 *
 */
public class DownloadNameItem extends CoreTableColumn implements TableCellRefreshListener /*, ObfusticateCellText */ {
	public static final String COLUMN_ID = "name";

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_CONTENT,
		});
	}
	
	/** Default Constructor */
	public DownloadNameItem(String table_id) {
		super(COLUMN_ID, 250, table_id);
		this.setPosition(0);
		//setObfustication(true);
		setRefreshInterval(INTERVAL_LIVE);
		setType(TableColumn.TYPE_TEXT);
		setMinWidth(100);
	}


	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	 */
	public void refresh(TableCell cell) {
		PEPeer peer = (PEPeer)cell.getDataSource();
		if (peer == null) {cell.setText(""); return;}
		PEPeerManager manager = peer.getManager();
		if (manager == null) {cell.setText(""); return;}
		cell.setText(manager.getDisplayName());
	}

}
