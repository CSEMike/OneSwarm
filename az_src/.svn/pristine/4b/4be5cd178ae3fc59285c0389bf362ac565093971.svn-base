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

package org.gudy.azureus2.ui.swt.views.tableitems.mytracker;

import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.host.TRHostTorrent;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.AzureusCoreFactory;

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
	
	protected static GlobalManager gm;
	
	public 
	CategoryItem() 
	{
		super("category", CoreTableColumn.POSITION_INVISIBLE, 400, TableManager.TABLE_MYTRACKER);
		
		setRefreshInterval(INTERVAL_LIVE);
	}
	
	public void 
	refresh(TableCell cell)
	{
	    TRHostTorrent tr_torrent = (TRHostTorrent)cell.getDataSource();
		
		if ( tr_torrent == null ){
			
			cell.setText("");
			
		}else{
			
			TOTorrent	torrent = tr_torrent.getTorrent();
			
			if (gm == null) {
				if (AzureusCoreFactory.isCoreRunning()) {
					return;
				}
				gm = AzureusCoreFactory.getSingleton().getGlobalManager();
			}

			DownloadManager dm = gm.getDownloadManager( torrent );

			String	cat_str = null;

			if ( dm != null ){
			
			    Category cat = dm.getDownloadState().getCategory();   
				
				if (cat != null){
					
					cat_str = cat.getName();
				}
			}else{
				
					// pick up specific torrent category, bit 'o' a hack tis
			
				cat_str = TorrentUtils.getPluginStringProperty( torrent, "azcoreplugins.category" );
			}
			
			cell.setText( cat_str==null?"":cat_str);
		}
	}
}
