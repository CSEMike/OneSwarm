/**
 * Copyright (C) 2004-2007 Aelitis SAS, All rights Reserved
 * 
 * Date: July 14, 2004
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

package com.aelitis.azureus.ui.common.table.impl;

import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;

import com.aelitis.azureus.ui.common.table.*;

import org.gudy.azureus2.plugins.ui.UIRuntimeException;
import org.gudy.azureus2.plugins.ui.tables.*;

import org.gudy.azureus2.pluginsimpl.local.ui.tables.TableContextMenuItemImpl;

/** 
 * Table Column definition and modification routines.
 * Implements both the plugin API and the core API.
 * <P>
 * A column is defined in relation to a table.  When one column is in  
 * multiple tables of different table ids, each table has it's own column
 * instance
 * 
 * @author TuxPaper
 * 
 * @see org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager
 */
public class TableColumnImpl
	implements TableColumnCore
{
	private static final String CFG_SORTDIRECTION = "config.style.table.defaultSortOrder";

	/** Internal Name/ID of the column **/
	private String sName;

	/** key of the displayed title for the column.  If null, uses default calc */
	private String sTitleLanguageKey = null;

	private int iAlignment;

	private int iType;

	private int iPosition;

	private int iWidth;

	private int iInterval;

	private long lLastSortValueChange;

	/** Table the column belongs to */
	private String sTableID;

	private boolean bColumnAdded;

	private boolean bCoreDataSource;

	private ArrayList cellRefreshListeners;

	private ArrayList cellAddedListeners;

	private ArrayList cellDisposeListeners;

	private ArrayList cellToolTipListeners;

	private ArrayList cellMouseListeners;

	private ArrayList cellMouseMoveListeners;

	private ArrayList cellVisibilityListeners;

	private int iConsecutiveErrCount;

	private ArrayList menuItems;

	private boolean bObfusticateData;

	protected AEMonitor this_mon = new AEMonitor("TableColumn");

	private boolean bSortValueLive;

	private long lStatsRefreshTotalTime;

	private long lStatsRefreshCount = 0;

	private long lStatsRefreshZeroCount = 0;

	private boolean bSortAscending;
	
	private boolean editable;

	private int iMinWidth = -1;

	private int iMaxWidth = -1;

	private boolean bVisible;
	
	private boolean bMaxWidthAuto = false;

	private boolean bWidthAuto;

	private int iPreferredWidth;

	private boolean bPreferredWidthAuto = true;

	private int iPreferredWidthMax = -1;
	
	private boolean auto_tooltip = false;

	/** Create a column object for the specified table.
	 *
	 * @param tableID table in which the column belongs to
	 * @param columnID name/id of the column
	 */
	public TableColumnImpl(String tableID, String columnID) {
		sTableID = tableID;
		sName = columnID;
		iType = TYPE_TEXT_ONLY;
		iWidth = 50;
		iAlignment = ALIGN_LEAD;
		bColumnAdded = false;
		bCoreDataSource = false;
		iInterval = INTERVAL_INVALID_ONLY;
		iConsecutiveErrCount = 0;
		lLastSortValueChange = 0;
		bVisible = false;
		iMinWidth = 16;
		iPosition = POSITION_INVISIBLE;
		int iSortDirection = COConfigurationManager.getIntParameter(CFG_SORTDIRECTION);
		bSortAscending = iSortDirection == 1 ? false : true;
	}

	public void initialize(int iAlignment, int iPosition, int iWidth,
			int iInterval) {
		if (bColumnAdded) {
			throw (new UIRuntimeException("Can't set properties. Column '" + sName
					+ " already added"));
		}

		this.iAlignment = iAlignment;
		setPosition(iPosition);
		this.iWidth = iWidth;
		this.iMinWidth = 16;
		this.iInterval = iInterval;
	}

	public void initialize(int iAlignment, int iPosition, int iWidth) {
		if (bColumnAdded) {
			throw (new UIRuntimeException("Can't set properties. Column '" + sName
					+ " already added"));
		}

		this.iAlignment = iAlignment;
		setPosition(iPosition);
		this.iWidth = iWidth;
		this.iMinWidth = 16;
	}

	public String getName() {
		return sName;
	}

	public String getTableID() {
		return sTableID;
	}

	public void setType(int type) {
		if (bColumnAdded) {
			throw (new UIRuntimeException("Can't set properties. Column '" + sName
					+ " already added"));
		}

		iType = type;
	}

	public int getType() {
		return iType;
	}

	public void setWidth(int width) {
		if (width == iWidth || width < 0) {
			return;
		}

		if (iMinWidth > 0 && width < iMinWidth) {
			return;
		}

		if (iMaxWidth > 0 && width > iMaxWidth) {
			if (width == iMaxWidth) {
				return;
			}
			width = iMaxWidth;
		}

		if (iMinWidth < 0) {
			iMinWidth = width;
		}

		//		if (iPreferredWidth <= 0) {
		//			iPreferredWidth = iWidth;
		//		}

		iWidth = width;

		if (bColumnAdded && bVisible) {
			triggerColumnSizeChange();
		}
	}

	public void triggerColumnSizeChange() {
		TableStructureEventDispatcher tsed = TableStructureEventDispatcher.getInstance(sTableID);
		tsed.columnSizeChanged(this);
		if (iType == TYPE_GRAPHIC) {
			invalidateCells();
		}
	}

	public int getWidth() {
		return iWidth;
	}

	public void setPosition(int position) {
		if (bColumnAdded) {
			throw (new UIRuntimeException("Can't set properties. Column '" + sName
					+ " already added"));
		}

		if (iPosition == POSITION_INVISIBLE && position != POSITION_INVISIBLE) {
			setVisible(true);
		}
		iPosition = position;
		if (position == POSITION_INVISIBLE) {
			setVisible(false);
		}
	}

	public int getPosition() {
		return iPosition;
	}

	public void setAlignment(int alignment) {
		if (bColumnAdded) {
			throw (new UIRuntimeException("Can't set properties. Column '" + sName
					+ " already added"));
		}

		iAlignment = alignment;
	}

	public int getAlignment() {
		return iAlignment;
	}

	public void addCellRefreshListener(TableCellRefreshListener listener) {
		try {
			this_mon.enter();

			if (cellRefreshListeners == null) {
				cellRefreshListeners = new ArrayList(1);
			}

			cellRefreshListeners.add(listener);
			//System.out.println(this + " :: addCellRefreshListener " + listener + ". " + cellRefreshListeners.size());

		} finally {

			this_mon.exit();
		}
	}

	public List getCellRefreshListeners() {
		try {
			this_mon.enter();

			if (cellRefreshListeners == null) {
				return (new ArrayList(0));
			}

			return (new ArrayList(cellRefreshListeners));

		} finally {

			this_mon.exit();
		}
	}

	public void removeCellRefreshListener(TableCellRefreshListener listener) {
		try {
			this_mon.enter();

			if (cellRefreshListeners == null) {
				return;
			}

			cellRefreshListeners.remove(listener);
		} finally {
			this_mon.exit();
		}
	}

	public boolean hasCellRefreshListener() {
		return cellRefreshListeners != null && cellRefreshListeners.size() > 0;
	}

	public void setRefreshInterval(int interval) {
		iInterval = interval;
	}

	public int getRefreshInterval() {
		return iInterval;
	}

	public void addCellAddedListener(TableCellAddedListener listener) {
		try {
			this_mon.enter();

			if (cellAddedListeners == null) {
				cellAddedListeners = new ArrayList(1);
			}

			cellAddedListeners.add(listener);

		} finally {

			this_mon.exit();
		}
	}

	public List getCellAddedListeners() {
		try {
			this_mon.enter();

			if (cellAddedListeners == null) {
				return (new ArrayList(0));
			}

			return (new ArrayList(cellAddedListeners));

		} finally {

			this_mon.exit();
		}
	}

	public void removeCellAddedListener(TableCellAddedListener listener) {
		try {
			this_mon.enter();

			if (cellAddedListeners == null) {
				return;
			}

			cellAddedListeners.remove(listener);

		} finally {

			this_mon.exit();
		}
	}

	public void addCellDisposeListener(TableCellDisposeListener listener) {
		try {
			this_mon.enter();

			if (cellDisposeListeners == null) {
				cellDisposeListeners = new ArrayList(1);
			}

			cellDisposeListeners.add(listener);
		} finally {

			this_mon.exit();
		}
	}

	public void removeCellDisposeListener(TableCellDisposeListener listener) {
		try {
			this_mon.enter();

			if (cellDisposeListeners == null) {
				return;
			}

			cellDisposeListeners.remove(listener);
		} finally {

			this_mon.exit();
		}
	}

	public void addCellToolTipListener(TableCellToolTipListener listener) {
		try {
			this_mon.enter();

			if (cellToolTipListeners == null) {
				cellToolTipListeners = new ArrayList(1);
			}

			cellToolTipListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeCellToolTipListener(TableCellToolTipListener listener) {
		try {
			this_mon.enter();

			if (cellToolTipListeners == null) {
				return;
			}

			cellToolTipListeners.remove(listener);
		} finally {
			this_mon.exit();
		}
	}

	public void addCellMouseListener(TableCellMouseListener listener) {
		try {
			this_mon.enter();

			if (cellMouseListeners == null) {
				cellMouseListeners = new ArrayList(1);
			}

			cellMouseListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeCellMouseListener(TableCellMouseListener listener) {
		try {
			this_mon.enter();

			if (cellMouseListeners == null) {
				return;
			}

			cellMouseListeners.remove(listener);

		} finally {
			this_mon.exit();
		}
	}
	
	public boolean hasCellMouseMoveListener() {
		return cellMouseMoveListeners != null && cellMouseMoveListeners.size() > 0;
	}

	public void addCellMouseMoveListener(TableCellMouseMoveListener listener) {
		try {
			this_mon.enter();

			if (cellMouseMoveListeners == null) {
				cellMouseMoveListeners = new ArrayList(1);
			}

			cellMouseMoveListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeCellMouseMoveListener(TableCellMouseMoveListener listener) {
		try {
			this_mon.enter();

			if (cellMouseMoveListeners == null) {
				return;
			}

			cellMouseMoveListeners.remove(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void addCellVisibilityListener(TableCellVisibilityListener listener) {
		try {
			this_mon.enter();

			if (cellVisibilityListeners == null) {
				cellVisibilityListeners = new ArrayList(1);
			}

			cellVisibilityListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeCellVisibilityListener(TableCellVisibilityListener listener) {
		try {
			this_mon.enter();

			if (cellVisibilityListeners == null) {
				return;
			}

			cellVisibilityListeners.remove(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void invalidateCells() {
		TableStructureEventDispatcher tsed = TableStructureEventDispatcher.getInstance(sTableID);
		tsed.columnInvalidate(this);
	}
	
	public void invalidateCell(Object data_source) {
		TableStructureEventDispatcher tsed = TableStructureEventDispatcher.getInstance(sTableID);
		tsed.cellInvalidate(this, data_source);
	}

	public void addListeners(Object listenerObject) {
		if (listenerObject instanceof TableCellDisposeListener) {
			addCellDisposeListener((TableCellDisposeListener) listenerObject);
		}

		if (listenerObject instanceof TableCellRefreshListener) {
			addCellRefreshListener((TableCellRefreshListener) listenerObject);
		}

		if (listenerObject instanceof TableCellToolTipListener) {
			addCellToolTipListener((TableCellToolTipListener) listenerObject);
		}

		if (listenerObject instanceof TableCellAddedListener) {
			addCellAddedListener((TableCellAddedListener) listenerObject);
		}

		if (listenerObject instanceof TableCellMouseMoveListener) {
			addCellMouseMoveListener((TableCellMouseMoveListener) listenerObject);
		} 

		if (listenerObject instanceof TableCellMouseListener) {
			addCellMouseListener((TableCellMouseListener) listenerObject);
		}

		if (listenerObject instanceof TableCellVisibilityListener) {
			addCellVisibilityListener((TableCellVisibilityListener) listenerObject);
		}
	}

	/* Start of not plugin public API functions */
	//////////////////////////////////////////////
	public void setColumnAdded(boolean bAdded) {
		bColumnAdded = bAdded;
	}

	public boolean getColumnAdded() {
		return bColumnAdded;
	}

	public void setUseCoreDataSource(boolean bCoreDataSource) {
		this.bCoreDataSource = bCoreDataSource;
	}

	public boolean getUseCoreDataSource() {
		return bCoreDataSource;
	}

	public void invokeCellRefreshListeners(TableCell cell, boolean fastRefresh) throws Throwable {
		//System.out.println(this + " :: invokeCellRefreshListeners" + cellRefreshListeners);
		if (cellRefreshListeners == null) {
			return;
		}
		
		Throwable firstError = null;

		//System.out.println(this + " :: invokeCellRefreshListeners" + cellRefreshListeners.size());
		for (int i = 0; i < cellRefreshListeners.size(); i++) {
			
			TableCellRefreshListener l = (TableCellRefreshListener)cellRefreshListeners.get(i); 

			try {
				if(l instanceof TableCellLightRefreshListener)
					((TableCellLightRefreshListener)l).refresh(cell, fastRefresh);
				else
					l.refresh(cell);
			} catch (Throwable e) {
				
				if (firstError == null) {
					firstError = e;
				}
				Debug.printStackTrace(e);
			}
		}
		
		if (firstError != null) {
			throw firstError;
		}
	}

	public void invokeCellAddedListeners(TableCell cell) {
		if (cellAddedListeners == null) {
			return;
		}
		for (int i = 0; i < cellAddedListeners.size(); i++) {

			try {
				((TableCellAddedListener) (cellAddedListeners.get(i))).cellAdded(cell);

			} catch (Throwable e) {

				Debug.printStackTrace(e);
			}
		}
	}

	public void invokeCellDisposeListeners(TableCell cell) {
		if (cellDisposeListeners == null) {
			return;
		}
		for (int i = 0; i < cellDisposeListeners.size(); i++) {
			try {
				((TableCellDisposeListener) (cellDisposeListeners.get(i))).dispose(cell);

			} catch (Throwable e) {

				Debug.printStackTrace(e);
			}
		}
	}

	public void invokeCellToolTipListeners(TableCellCore cell, int type) {
		if (cellToolTipListeners == null) {
			return;
		}
		if (type == TableCellCore.TOOLTIPLISTENER_HOVER) {
			for (int i = 0; i < cellToolTipListeners.size(); i++) {
				try {
					((TableCellToolTipListener) (cellToolTipListeners.get(i))).cellHover(cell);
				} catch (Throwable e) {

					Debug.printStackTrace(e);
				}
			}
		} else {
			for (int i = 0; i < cellToolTipListeners.size(); i++) {

				try {
					((TableCellToolTipListener) (cellToolTipListeners.get(i))).cellHoverComplete(cell);
				} catch (Throwable e) {

					Debug.printStackTrace(e);
				}
			}
		}
	}

	public void invokeCellMouseListeners(TableCellMouseEvent event) {
		ArrayList listeners = event.eventType == TableCellMouseEvent.EVENT_MOUSEMOVE
				? cellMouseMoveListeners : this.cellMouseListeners;
		if (listeners == null) {
			return;
		}

		for (int i = 0; i < listeners.size(); i++) {
			try {
				TableCellMouseListener l = (TableCellMouseListener) (listeners.get(i));

				l.cellMouseTrigger(event);

			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}

	public void invokeCellVisibilityListeners(TableCellCore cell, int visibility) {
		if (cellVisibilityListeners == null) {
			return;
		}

		for (int i = 0; i < cellVisibilityListeners.size(); i++) {
			try {
				TableCellVisibilityListener l = (TableCellVisibilityListener) (cellVisibilityListeners.get(i));

				l.cellVisibilityChanged(cell, visibility);

			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}

	public void setPositionNoShift(int position) {
		//if (iPosition < 0 && position >= 0) {
		//	setVisible(true);
		//}
		iPosition = position;
		//if (position < 0) {
		//	setVisible(false);
		//}
	}

	public void loadSettings(Map mapSettings) {
		// Format: Key = [TableID].column.[columnname]
		// Value[] = { visible, width, position, autotooltip, sortorder }
		String itemPrefix = "Column." + sName;
		String oldItemPrefix = "Table." + sTableID + "." + sName;
		Object object = mapSettings.get(itemPrefix);
		Object[] list;
		if (object instanceof List) {
			list = ((List) object).toArray();
		} else {
			list = new String[0];
		}

		int pos = 0;
		if (list.length >= (pos + 1) && (list[pos] instanceof Number)) {
			boolean vis = ((Number) list[pos]).intValue() == 1;
			setVisible(vis);
		}

		pos++;
		if (list.length >= (pos + 1) && (list[pos] instanceof Number)) {
			int position = ((Number) list[pos]).intValue();
			setPositionNoShift(position);
		} else {
			int position = COConfigurationManager.getIntParameter(oldItemPrefix + ".position",
					iPosition);
			if (iPosition == POSITION_INVISIBLE && position != POSITION_INVISIBLE) {
				setVisible(true);
			}
			setPositionNoShift(position);
			if (position == POSITION_INVISIBLE) {
				setVisible(false);
			}
		}

		pos++;
		if (list.length >= (pos + 1) && (list[pos] instanceof Number)) {
			int width = ((Number) list[pos]).intValue();
			setWidth(width);
		} else {
			setWidth(COConfigurationManager.getIntParameter(oldItemPrefix + ".width",
					iWidth));
		}
		
		pos++;
		if (list.length >= (pos + 1) && (list[pos] instanceof Number)) {
			boolean autoTooltip = ((Number) list[pos]).intValue() == 1;
			setAutoTooltip(autoTooltip);
		} else {
			setAutoTooltip(COConfigurationManager.getBooleanParameter(oldItemPrefix
					+ ".auto_tooltip", auto_tooltip));
		}

		pos++;
		if (list.length >= (pos + 1) && (list[pos] instanceof Number)) {
			int sortOrder = ((Number) list[pos]).intValue();
			if (sortOrder >= 0) {
				// dont call setSordOrder, since it will change lLastSortValueChange
				// which we shouldn't do if we aren't the sorting column
				bSortAscending = sortOrder == 1;
			}
		}
	}

	public void saveSettings(Map mapSettings) {
		if (mapSettings == null) {
			mapSettings = TableColumnManager.getInstance().getTableConfigMap(sTableID);
			if (mapSettings == null) {
				return;
			}
		}
		String sItemPrefix = "Column." + sName;
		mapSettings.put(sItemPrefix, Arrays.asList(new Object[] {
			new Integer(bVisible ? 1 : 0),
			new Integer(iPosition),
			new Integer(iWidth),
			new Integer(auto_tooltip ? 1 : 0),
			new Integer(lLastSortValueChange == 0 ? -1 : (bSortAscending ? 1 : 0)),
		}));
		// cleanup old config
		sItemPrefix = "Table." + sTableID + "." + sName; 
		if (COConfigurationManager.hasParameter(sItemPrefix + ".width", true)) {
			COConfigurationManager.removeParameter(sItemPrefix + ".position");
			COConfigurationManager.removeParameter(sItemPrefix + ".width");
			COConfigurationManager.removeParameter(sItemPrefix + ".auto_tooltip");
		}
	}

	public String getTitleLanguageKey() {
		try {
			this_mon.enter();

			if (sTitleLanguageKey == null) {
				sTitleLanguageKey = sTableID + ".column." + sName;
				if (MessageText.keyExists(sTitleLanguageKey)) {
					return sTitleLanguageKey;
				}

				String sKeyPrefix;
				// Try a generic one of "TableColumn." + columnid
				sKeyPrefix = "TableColumn.header.";
				if (MessageText.keyExists(sKeyPrefix + sName)) {
					sTitleLanguageKey = sKeyPrefix + sName;
					return sTitleLanguageKey;
				}

				// Support "Old Style" language keys, which have a prefix of TableID + "View."
				// Also, "MySeeders" is actually stored in "MyTorrents"..
				sKeyPrefix = (sTableID.equals(TableManager.TABLE_MYTORRENTS_COMPLETE)
						? TableManager.TABLE_MYTORRENTS_INCOMPLETE : sTableID)
						+ "View.";
				if (MessageText.keyExists(sKeyPrefix + sName)) {
					sTitleLanguageKey = sKeyPrefix + sName;
					return sTitleLanguageKey;
				}
				
				// The "all peers" view should just share the same peer columns, so reuse them.
				if (sTableID.equals(TableManager.TABLE_ALL_PEERS)) {
					sKeyPrefix = TableManager.TABLE_TORRENT_PEERS + ".column.";
					if (MessageText.keyExists(sKeyPrefix + sName)) {
						sTitleLanguageKey = sKeyPrefix + sName;
						return sTitleLanguageKey;
					}
					
					// Or try "PeersView".
					sKeyPrefix = "PeersView.";
					if (MessageText.keyExists(sKeyPrefix + sName)) {
						sTitleLanguageKey = sKeyPrefix + sName;
						return sTitleLanguageKey;
					}
				}
				
				// another "Old Style"
				sKeyPrefix = "MyTorrentsView." + sName;
				//System.out.println(sKeyPrefix + ";" + MessageText.getString(sKeyPrefix));
				if (MessageText.keyExists(sKeyPrefix)) {
					sTitleLanguageKey = sKeyPrefix;
					return sTitleLanguageKey;
				}
			}
			return sTitleLanguageKey;
		} finally {

			this_mon.exit();
		}
	}

	public int getConsecutiveErrCount() {
		return iConsecutiveErrCount;
	}

	public void setConsecutiveErrCount(int iCount) {
		iConsecutiveErrCount = iCount;
	}

	public void removeContextMenuItem(TableContextMenuItem menuItem) {
		if (menuItems == null) {
			return;
		}

		menuItems.remove(menuItem);
	}

	public TableContextMenuItem addContextMenuItem(String key) {
		if (menuItems == null) {
			menuItems = new ArrayList();
		}

		// Hack.. should be using our own implementation..
		TableContextMenuItemImpl item = new TableContextMenuItemImpl("", key);
		menuItems.add(item);
		return item;
	}

	public TableContextMenuItem[] getContextMenuItems() {
		if (menuItems == null) {
			return new TableContextMenuItem[0];
		}

		return (TableContextMenuItem[]) menuItems.toArray(new TableContextMenuItem[0]);
	}

	public boolean isObfusticated() {
		return bObfusticateData;
	}

	public void setObfustication(boolean hideData) {
		bObfusticateData = hideData;
	}

	public long getLastSortValueChange() {
		if (bSortValueLive) {
			return SystemTime.getCurrentTime();
		}
		return lLastSortValueChange;
	}

	public void setLastSortValueChange(long lastSortValueChange) {
		lLastSortValueChange = lastSortValueChange;
	}

	public boolean isSortValueLive() {
		return bSortValueLive;
	}

	public void setSortValueLive(boolean live) {
		//		if (live && !bSortValueLive) {
		//			System.out.println("Setting " + sTableID + ": " + sName + " to live sort value");
		//		}
		bSortValueLive = live;
	}

	public void addRefreshTime(long ms) {
		if (ms == 0) {
			lStatsRefreshZeroCount++;
		} else {
			lStatsRefreshTotalTime += ms;
			lStatsRefreshCount++;
		}
	}

	public void generateDiagnostics(IndentWriter writer) {
		writer.println("Column " + sTableID + ":" + sName
				+ (bSortValueLive ? " (Live Sort)" : ""));
		try {
			writer.indent();

			if (lStatsRefreshCount > 0) {
				writer.println("Avg refresh time (" + lStatsRefreshCount
						+ " samples): " + (lStatsRefreshTotalTime / lStatsRefreshCount)
						+ " (" + lStatsRefreshZeroCount
						+ " zero ms refreshes not included)");
			}
			writer.println("Listeners: refresh="
					+ getListCountString(cellRefreshListeners) + "; dispose="
					+ getListCountString(cellDisposeListeners) + "; mouse="
					+ getListCountString(cellMouseListeners) + "; mm="
					+ getListCountString(cellMouseMoveListeners) + "; vis="
					+ getListCountString(cellVisibilityListeners) + "; added="
					+ getListCountString(cellAddedListeners) + "; tooltip="
					+ getListCountString(cellToolTipListeners));

			writer.println("lLastSortValueChange=" + lLastSortValueChange);
		} catch (Exception e) {
		} finally {
			writer.exdent();
		}
	}

	private String getListCountString(List l) {
		if (l == null) {
			return "-0";
		}
		return "" + l.size();
	}

	public void setTableID(String tableID) {
		sTableID = tableID;
	}

	// @see java.util.Comparator#compare(T, T)
	public int compare(Object arg0, Object arg1) {
		TableCellCore cell0 = ((TableRowCore) arg0).getTableCellCore(sName);
		TableCellCore cell1 = ((TableRowCore) arg1).getTableCellCore(sName);

		Comparable c0 = (cell0 == null) ? "" : cell0.getSortValue();
		Comparable c1 = (cell1 == null) ? "" : cell1.getSortValue();

		try {
			boolean c0isString = c0 instanceof String;
			boolean c1isString = c1 instanceof String;
			if (c0isString && c1isString) {
				if (bSortAscending) {
					return ((String) c0).compareToIgnoreCase((String) c1);
				}

				return ((String) c1).compareToIgnoreCase((String) c0);
			}

			int val;
			if (c1 == null) {
				if (c0 == null) {
					return 0;
				}
				// always place nulls at bottom
				return -1;
			} else if (c0 == null) {
				// always place nulls at bottom
				return 1;
			} else if (c0isString && !c1isString) {
				val = -1;
			} else if (c1isString && !c0isString) {
				val = 1;
			} else {
				val = c1.compareTo(c0);
			}
			return bSortAscending ? -val : val;
		} catch (ClassCastException e) {
			System.err.println("Can't compare " + c0.getClass().getName() + "("
					+ c0.toString() + ") from row #" + cell0.getTableRowCore().getIndex()
					+ " to " + c1.getClass().getName() + "(" + c1.toString()
					+ ") from row #" + cell1.getTableRowCore().getIndex()
					+ " while sorting column " + sName);
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * @param bAscending The bAscending to set.
	 */
	public void setSortAscending(boolean bAscending) {
		if (this.bSortAscending == bAscending) {
			return;
		}
		setLastSortValueChange(SystemTime.getCurrentTime());

		this.bSortAscending = bAscending;
	}

	/**
	 * @return Returns the bAscending.
	 */
	public boolean isSortAscending() {
		return bSortAscending;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableColumn#getMinWidth()
	public int getMinWidth() {
		if (iMinWidth < 0) {
			return iWidth;
		}

		return iMinWidth;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableColumn#setMinWidth(int)
	public void setMinWidth(int minwidth) {
		if (minwidth > iMaxWidth && iMaxWidth >= 0) {
			iMaxWidth = minwidth;
		}
		if (iPreferredWidth > 0 && iPreferredWidth < minwidth) {
			iPreferredWidth = minwidth;
		}
		iMinWidth = minwidth;
		if (iWidth < minwidth) {
			setWidth(minwidth);
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableColumn#getMaxWidth()
	public int getMaxWidth() {
		return iMaxWidth;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableColumn#setMaxWidth(int)
	public void setMaxWidth(int maxwidth) {
		if (maxwidth >= 0 && maxwidth < iMinWidth) {
			iMinWidth = maxwidth;
		}
		if (iPreferredWidth > maxwidth) {
			iPreferredWidth = maxwidth;
		}
		iMaxWidth = maxwidth;
		if (maxwidth >= 0 && iWidth > iMaxWidth) {
			setWidth(maxwidth);
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableColumn#setWidthLimits(int, int)
	public void setWidthLimits(int min, int max) {
		setMinWidth(min);
		setMaxWidth(max);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableColumn#isVisible()
	public boolean isVisible() {
		return bVisible;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableColumn#setVisible(boolean)
	public void setVisible(boolean visible) {
		if (bVisible == visible) {
			return;
		}
		
		//System.out.println("set " + sTableID + "/" + sName + " to " + visible
		//		+ " via " + Debug.getCompressedStackTrace());

		bVisible = visible;
		invalidateCells();
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableColumn#isMaxWidthAuto()
	public boolean isMaxWidthAuto() {
		return bMaxWidthAuto;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableColumn#setMaxWidthAuto(boolean)
	public void setMaxWidthAuto(boolean automaxwidth) {
		bMaxWidthAuto = automaxwidth;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableColumn#isMinWidthAuto()
	public boolean isMinWidthAuto() {
		return bWidthAuto;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableColumn#setMinWidthAuto(boolean)
	public void setMinWidthAuto(boolean autominwidth) {
		bWidthAuto = autominwidth;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableColumn#getPreferredWidth()
	public int getPreferredWidth() {
		return iPreferredWidth;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableColumn#setPreferredWidthAuto(boolean)
	public void setPreferredWidthAuto(boolean auto) {
		bPreferredWidthAuto = auto;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableColumn#isPreferredWidthAuto()
	public boolean isPreferredWidthAuto() {
		return bPreferredWidthAuto;
	}

	public void setPreferredWidthMax(int maxprefwidth) {
		iPreferredWidthMax = maxprefwidth;
		if (iPreferredWidth > iPreferredWidthMax) {
			setPreferredWidth(maxprefwidth);
		}
	}

	public int getPreferredWidthMax() {
		return iPreferredWidthMax;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableColumn#setPreferredWidth(int)
	public void setPreferredWidth(int width) {
		if (iPreferredWidthMax > 0 && width > iPreferredWidthMax) {
			width = iPreferredWidthMax;
		}

		if (width < iMinWidth) {
			iPreferredWidth = iMinWidth;
		} else if (iMaxWidth > 0 && width > iMaxWidth) {
			iPreferredWidth = iMaxWidth;
		} else {
			iPreferredWidth = width;
		}

		if (bColumnAdded && bVisible) {
			triggerColumnSizeChange();
		}
	}
	
	public void setAutoTooltip(boolean auto_tooltip) {
		this.auto_tooltip = auto_tooltip;
	}
	
	public boolean doesAutoTooltip() {
		return this.auto_tooltip;
	}
	
	public boolean isInplaceEdit() {
		return editable;
	}
	
	public void setInplaceEdit(boolean editable) {
		this.editable = editable;		
	}
	
	public boolean inplaceValueSet(TableCell cell, String value, boolean finalEdit) {
		return false;
	}

}
