/*
 * Created on 27-May-2004
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

package org.gudy.azureus2.ui.swt.views.tableitems.peers;

import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;


/**
 * @author parg
 *
 */

public class 
HostNameItem 
	extends CoreTableColumn 
	implements TableCellRefreshListener
{
	 /** Default Constructor */
	  public HostNameItem(String table_id) {
	    super("host", POSITION_INVISIBLE, 100, table_id);
	    setRefreshInterval(INTERVAL_LIVE);
	    setObfustication(true);
	  }

	  public void refresh(TableCell cell) {
	    PEPeer peer = (PEPeer)cell.getDataSource();

	    cell.setText( peer == null ? "" : peer.getIPHostName() );
	  }
}
