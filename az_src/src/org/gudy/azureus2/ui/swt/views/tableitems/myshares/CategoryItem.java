/*
 * Created on 10-Dec-2004
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

package org.gudy.azureus2.ui.swt.views.tableitems.myshares;

import org.gudy.azureus2.plugins.sharing.ShareResource;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentManagerImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/**
 * @author parg
 *
 */

public class 
CategoryItem 
	extends CoreTableColumn 
	implements TableCellRefreshListener
{
		/** Default Constructor */
	
	protected static final TorrentAttribute	category_attribute = 
		TorrentManagerImpl.getSingleton().getAttribute( TorrentAttribute.TA_CATEGORY );
	
	public 
	CategoryItem() 
	{
		super("category", POSITION_LAST, 400, TableManager.TABLE_MYSHARES);
		
		setRefreshInterval(INTERVAL_LIVE);
	}
	
	public void 
	refresh(TableCell cell)
	{
		ShareResource item = (ShareResource)cell.getDataSource();
		
		if (item == null){
			
			cell.setText("");
			
		}else{
			
			String	value = item.getAttribute(category_attribute);
			
			cell.setText( value==null?"":value);
		}
	}
}
