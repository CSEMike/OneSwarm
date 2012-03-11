/*
 * File    : ASItem.java
 * Created : 24 dec 2008
 * By      : Parg
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

import java.net.InetAddress;

import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.networkmanager.admin.*;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;

/**
 *
 */
public class ASItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{  
  public static final String COLUMN_ID = "as";

  public ASItem(String table_id) {
    super(COLUMN_ID, ALIGN_LEAD, POSITION_INVISIBLE, 100, table_id);
    setRefreshInterval(INTERVAL_LIVE);
  }

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_PEER_IDENTIFICATION,
		});
	}

  public void refresh(TableCell cell) {
    final PEPeer peer = (PEPeer)cell.getDataSource();
   
    String	text = "";
    
    if ( peer != null ){
    	
    	text = (String)peer.getUserData( ASItem.class );
    	
    	if ( text == null ){
  
    		text = "";
    		
    		peer.setUserData( ASItem.class, text );
    	
    		try{
	    		NetworkAdmin.getSingleton().lookupASN( 
	    			InetAddress.getByName( peer.getIp()),
	    			new NetworkAdminASNListener()
	    			{
	    				public void
	    				success(
	    					NetworkAdminASN		asn )
	    				{
	    					peer.setUserData( ASItem.class, asn.getAS() + " - " + asn.getASName());
	    				}
	    				
	    				public void
	    				failed(
	    					NetworkAdminException	error )
	    				{
	    				}
	    			});
	    	
	    	}catch( Throwable e ){
	    	}
    	}
    }

    if (!cell.setSortValue(text) && cell.isValid()){
    	
      return;
    }

    cell.setText( text );
  }
}
