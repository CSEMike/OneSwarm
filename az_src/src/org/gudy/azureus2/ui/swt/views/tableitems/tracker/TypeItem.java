/*
 * File    : TypeItem.java
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.tracker;

import java.util.Locale;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.internat.MessageText.MessageTextListener;
import org.gudy.azureus2.plugins.ui.tables.*;

import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.tracker.TrackerPeerSource;


public class 
TypeItem
	extends CoreTableColumn 
    implements TableCellRefreshListener
{
	private static final String[] js_resource_keys = {
		"SpeedView.stats.unknown",
		"MyTrackerView.tracker",
		"wizard.webseed.title",
		"tps.type.dht",
		"ConfigView.section.transfer.lan",
		"tps.type.pex",
		"tps.type.incoming",
		"tps.type.plugin",
	};

	private static String[] js_resources = new String[js_resource_keys.length];

	
	public 
	TypeItem()
	{
		super( "type", ALIGN_LEAD, POSITION_LAST, 75, TableManager.TABLE_TORRENT_TRACKERS );
    
		setRefreshInterval(INTERVAL_INVALID_ONLY);
		
		MessageText.addAndFireListener(new MessageTextListener() {
			public void localeChanged(Locale old_locale, Locale new_locale) {
				for (int i = 0; i < js_resources.length; i++) {
					js_resources[i] = MessageText.getString(js_resource_keys[i]);
				}
			}
		});
	}

	public void 
	fillTableColumnInfo(
		TableColumnInfo info ) 
	{
		info.addCategories( new String[]{
			CAT_ESSENTIAL,
		});
	}

	public void 
	refresh(
		TableCell cell ) 
	{
		TrackerPeerSource ps = (TrackerPeerSource)cell.getDataSource();
    
		int value = (ps==null)?TrackerPeerSource.TP_UNKNOWN:ps.getType();

		if (!cell.setSortValue(value) && cell.isValid()){
		
			return;
		}

		cell.setText( js_resources[value]);
	}
}
