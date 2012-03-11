/*
 * File    : MaxUploadsItem.java
 * Created : 01 febv. 2004
 * By      : TuxPaper
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

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.swt.views.table.impl.TableCellImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.menus.*;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.pluginsimpl.local.ui.menus.MenuManagerImpl;

import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableSelectedRowsListener;

public class MaxUploadsItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

  public static final String COLUMN_ID = "maxuploads";

	/** Default Constructor */
  public MaxUploadsItem(String sTableID) {
    super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_TRAIL, 30, sTableID);
    setRefreshInterval(INTERVAL_LIVE);
    setMinWidthAuto(true);
    
    TableContextMenuItem menuItem = addContextMenuItem("TableColumn.menu.maxuploads");
    menuItem.setStyle(TableContextMenuItem.STYLE_MENU);
    menuItem.addFillListener(new MenuItemFillListener() {
		
			public void menuWillBeShown(MenuItem menu, Object data) {
				menu.removeAllChildItems();
				
				PluginInterface pi = PluginInitializer.getDefaultInterface();
				UIManager uim = pi.getUIManager();
				MenuManager menuManager = uim.getMenuManager();

	      int iStart = COConfigurationManager.getIntParameter("Max Uploads") - 2;
	      if (iStart < 2) iStart = 2;
	      for (int i = iStart; i < iStart + 6; i++) {
					MenuItem item = menuManager.addMenuItem(menu, "MaxUploads." + i);
					item.setText(String.valueOf(i));
					item.setData(new Long(i));
					item.addMultiListener(new MenuItemListener() {
						public void selected(MenuItem item, Object target) {
							if (target instanceof Object[]) {
								Object[] targets = (Object[]) target;
								for (Object object : targets) {
									DownloadManager dm = (DownloadManager) object;
									int value = ((Long) item.getData()).intValue();
									dm.setMaxUploads(value);
								}
							} // run
						}
					}); // listener
				} // for
			}
		});
  }

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_SETTINGS
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_ADVANCED);
	}

  public void refresh(TableCell cell) {
    DownloadManager dm = (DownloadManager)cell.getDataSource();
    long value = (dm == null) ? 0 : dm.getEffectiveMaxUploads();

    if (!cell.setSortValue(value) && cell.isValid())
      return;
    cell.setText(String.valueOf(value));
  }
}
