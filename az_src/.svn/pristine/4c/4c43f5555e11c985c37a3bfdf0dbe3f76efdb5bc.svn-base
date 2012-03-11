/**
 * Created on 19-Apr-2004
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

package org.gudy.azureus2.pluginsimpl.local.ui.tables;


import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.impl.TableColumnImpl;

import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.tables.*;

import org.gudy.azureus2.pluginsimpl.local.ui.UIManagerEventAdapter;
import org.gudy.azureus2.pluginsimpl.local.ui.UIManagerImpl;

/** Manage Tables
 *
 * There's a TableManager per plugin interface
 *
 * @author TuxPaper
 * @since 2.0.8.5
 */
public class TableManagerImpl implements TableManager
{
	private UIManagerImpl ui_manager;
	
	public TableManagerImpl(UIManagerImpl _ui_manager) {
		ui_manager = _ui_manager;
	}

	public TableColumn createColumn(final String tableID, final String cellID) {
		final TableColumnImpl column = new TableColumnImpl(tableID, cellID);

		ui_manager.addUIListener(new UIManagerListener() {
			public void UIDetached(UIInstance instance) {
			}

			public void UIAttached(UIInstance instance) {
				UIManagerEventAdapter event = new UIManagerEventAdapter(
						ui_manager.getPluginInterface(), UIManagerEvent.ET_CREATE_TABLE_COLUMN, column);

				UIManagerImpl.fireEvent(event);
				// event.result used to have the TableColumn which we would populate
				// with info.
			}
		});

		return column;
	}
	
	public void registerColumn(final Class forDataSourceType, final String cellID, final TableColumnCreationListener listener) {
		ui_manager.addUIListener(new UIManagerListener() {
			public void UIDetached(UIInstance instance) {
			}

			public void UIAttached(UIInstance instance) {
				UIManagerEventAdapter event = new UIManagerEventAdapter(
						ui_manager.getPluginInterface(), UIManagerEvent.ET_REGISTER_COLUMN, new Object[]{ forDataSourceType, cellID, listener });
				UIManagerImpl.fireEvent(event);
			}
		});
	}

	public void unregisterColumn(final Class forDataSourceType, final String cellID, final TableColumnCreationListener listener) {
		ui_manager.addUIListener(new UIManagerListener() {
			public void UIDetached(UIInstance instance) {
			}

			public void UIAttached(UIInstance instance) {
				UIManagerEventAdapter event = new UIManagerEventAdapter(
						ui_manager.getPluginInterface(), UIManagerEvent.ET_UNREGISTER_COLUMN, new Object[]{ forDataSourceType, cellID, listener });
				UIManagerImpl.fireEvent(event);
			}
		});
	}
  
	public void addColumn(final TableColumn tableColumn) {
		if (!(tableColumn instanceof TableColumnCore))
			throw (new UIRuntimeException(
					"TableManager.addColumn(..) can only add columns created by createColumn(..)"));

		ui_manager.addUIListener(new UIManagerListener() {
			public void UIDetached(UIInstance instance) {
			}

			public void UIAttached(UIInstance instance) {
				UIManagerEventAdapter event = new UIManagerEventAdapter(
						ui_manager.getPluginInterface(), UIManagerEvent.ET_ADD_TABLE_COLUMN, tableColumn);
				UIManagerImpl.fireEvent(event);
			}
		});
	}

	public TableContextMenuItem addContextMenuItem(TableContextMenuItem parent,
			String resourceKey) {
		if (!(parent instanceof TableContextMenuItemImpl)) {
			throw new UIRuntimeException(
					"parent must have been created by addContextMenuItem");
		}
		if (parent.getStyle() != TableContextMenuItemImpl.STYLE_MENU) {
			throw new UIRuntimeException(
					"parent menu item must have the menu style associated");
		}
		TableContextMenuItemImpl item = new TableContextMenuItemImpl(
				(TableContextMenuItemImpl) parent, resourceKey);
		UIManagerImpl.fireEvent(ui_manager.getPluginInterface(), UIManagerEvent.ET_ADD_TABLE_CONTEXT_SUBMENU_ITEM,
				new Object[] {item, parent});
		return item;
	}

	public TableContextMenuItem addContextMenuItem(String tableID,
			String resourceKey) {
		TableContextMenuItemImpl item = new TableContextMenuItemImpl(ui_manager.getPluginInterface(), tableID, resourceKey);

		// this event is replayed for us on UI attaches so no extra work

		UIManagerImpl.fireEvent(ui_manager.getPluginInterface(), UIManagerEvent.ET_ADD_TABLE_CONTEXT_MENU_ITEM, item);
		
		return item;
	}
}
