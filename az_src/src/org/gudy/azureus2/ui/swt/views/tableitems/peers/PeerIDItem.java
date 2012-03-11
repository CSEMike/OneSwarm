/*
 * Created on 2 Mar 2007 Created by Allan Crooks Copyright (C) 2007 Aelitis, All
 * Rights Reserved. This program is free software; you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. This program is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; if not, write
 * to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
 * MA 02111-1307, USA. AELITIS, SAS au capital de 46,603.30 euros 8 Allee
 * Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.views.tableitems.peers;

import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;

import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/**
 * @author Allan Crooks
 */
public class PeerIDItem extends CoreTableColumn implements
		TableCellRefreshListener {
	
	/** Default Constructor */
	public PeerIDItem(String table_id) {
		// Uses same values for subclass constructor as ClientItem does.
		super("peer_id", POSITION_INVISIBLE, 100, table_id);
		setRefreshInterval(INTERVAL_LIVE);
	}

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_PEER_IDENTIFICATION,
		});
	}

	public void refresh(TableCell cell) {
		PEPeer peer = (PEPeer) cell.getDataSource();
		if (peer == null) {cell.setText(""); return;}
		
		byte[] peer_id = peer.getId();
		if (peer_id == null) {cell.setText(""); return;}
		try {
			String text = new String(peer_id, 0, peer_id.length, Constants.BYTE_ENCODING);
			text = text.replace((char)12, (char)32); // Replace newlines.
			text = text.replace((char)10, (char)32);
			cell.setText(text);
		}
		catch (java.io.UnsupportedEncodingException uee) {
			cell.setText("");
		}
	}
}