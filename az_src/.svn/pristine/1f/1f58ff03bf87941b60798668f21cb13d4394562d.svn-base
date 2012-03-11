/*
 * File    : ClientItem.java
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.peers;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.plugins.ui.tables.*;

import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;


public class StateItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  /** Default Constructor */
  public StateItem(String table_id) {
    super("state", POSITION_LAST, 65, table_id);
    setRefreshInterval(INTERVAL_LIVE);
  }

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_PROTOCOL,
			CAT_CONNECTION,
		});
	}

  public void refresh(TableCell cell) {
    PEPeerTransport peer = (PEPeerTransport)cell.getDataSource();  //TODO fix this "naughty" cast
    String state_text = "";
    if( peer != null ) {
      int state = peer.getConnectionState();
      
      if( !cell.setSortValue( state ) && cell.isValid() ) {
        return;
      }
      
      switch( state ) {
        case PEPeerTransport.CONNECTION_PENDING :
          state_text = MessageText.getString( "PeersView.state.pending" );
          break;
        case PEPeerTransport.CONNECTION_CONNECTING :
          state_text = MessageText.getString( "PeersView.state.connecting" );
          break;
        case PEPeerTransport.CONNECTION_WAITING_FOR_HANDSHAKE :
          state_text = MessageText.getString( "PeersView.state.handshake" );
          break;
        case PEPeerTransport.CONNECTION_FULLY_ESTABLISHED :
          state_text = MessageText.getString( "PeersView.state.established" );
          break;
      }
    }
    
    cell.setText( state_text );
  }
}
