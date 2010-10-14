/*
 * File    : TableColumnManager.java
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
 
package org.gudy.azureus2.ui.swt.views.table.utils;

import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.OldMyTorrentsPluginItem;
import org.gudy.azureus2.ui.swt.views.tableitems.peers.OldPeerPluginItem;

import com.aelitis.azureus.ui.common.table.TableColumnCore;

import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.mytorrents.PluginMyTorrentsItemFactory;


/** Holds a list of column definitions (TableColumnCore) for 
 * all the tables in Azureus.
 *
 * Colum definitions are added via 
 * PluginInterface.addColumn(TableColumn)
 * See Use javadoc section for more uses.
 *
 * @author Oliver (Original Code)
 * @author TuxPaper (Modifications to make generic & comments)
 */
public class TableColumnManager {

  private static final String CONFIG_FILE = "tables.config";
	private static TableColumnManager instance;
  private static AEMonitor 			class_mon 	= new AEMonitor( "TableColumnManager" );

  /* Holds all the TableColumnCore objects.
   * key   = TABLE_* type (see TableColumnCore)
   * value = Map:
   *           key = column name
   *           value = TableColumnCore object
   */
  private Map 			items;
  private AEMonitor 	items_mon 	= new AEMonitor( "TableColumnManager:items" );
  
  /**
   * Holds the order in which the columns are auto-hidden
   * 
   * key   = TABLE_* type
   * value = List of TableColumn, indexed in the order they should be removed
   */ 
  private Map autoHideOrder = new HashMap();
	private Map mapTablesConfig; // key = table; value = map of columns
	private static Comparator orderComparator;
	
	static {
		orderComparator = new Comparator() {
			public int compare(Object arg0, Object arg1) {				
				if ((arg1 instanceof TableColumn) && (arg0 instanceof TableColumn)) {
					int iPositionA = ((TableColumn) arg0).getPosition();
					if (iPositionA < 0)
						iPositionA = 0xFFFF + iPositionA;
					int iPositionB = ((TableColumn) arg1).getPosition();
					if (iPositionB < 0)
						iPositionB = 0xFFFF + iPositionB;

					return iPositionA - iPositionB;
				}
				return 0;
			}
		};
	}

  
  private TableColumnManager() {
   items = new HashMap();

   mapTablesConfig = FileUtil.readResilientConfigFile(CONFIG_FILE);
  }
  
  /** Retrieve the static TableColumnManager instance
   * @return the static TableColumnManager instance
   */
  public static TableColumnManager getInstance() {
  	try{
  		class_mon.enter();
  	
  		if(instance == null)
  			instance = new TableColumnManager();
  		return instance;
  	}finally{
  		
  		class_mon.exit();
  	}
  }
  
  /** Adds a column definition to the list
   * @param item The column definition object
   */
  public void addColumn(TableColumnCore item) {
    try {
      String name = item.getName();
      String sTableID = item.getTableID();
     try{
     	items_mon.enter();
        Map mTypes = (Map)items.get(sTableID);
        if (mTypes == null) {
          // LinkedHashMap to preserve order
          mTypes = new LinkedHashMap();
          items.put(sTableID, mTypes);
        }
        if (!mTypes.containsKey(name)) {
          mTypes.put(name, item);
          Map mapColumnConfig = getTableConfigMap(sTableID);
          mapTablesConfig.put("Table." + sTableID, mapColumnConfig);
          ((TableColumnCore)item).loadSettings(mapColumnConfig);
        }
      }finally{
      	items_mon.exit();
      }
      if (!item.getColumnAdded()) {
        item.setColumnAdded(true);
      }
    } catch (Exception e) {
      System.out.println("Error while adding Table Column Extension");
      Debug.printStackTrace( e );
    }
  }

  /**
   *  Add an extension from the deprecated PluginMyTorrentsItemFactory
   *  @deprecated
   */
  public void addExtension(String name, org.gudy.azureus2.plugins.ui.tables.mytorrents.PluginMyTorrentsItemFactory item) {
    String sAlign = item.getOrientation();
    int iAlign;
    if (sAlign.equals(PluginMyTorrentsItemFactory.ORIENT_RIGHT))
      iAlign = TableColumnCore.ALIGN_TRAIL;
    else
      iAlign = TableColumnCore.ALIGN_LEAD;

    int iVisibleIn = item.getTablesVisibleIn();
    if ((iVisibleIn & PluginMyTorrentsItemFactory.TABLE_COMPLETE) != 0) {
      TableColumnCore tci = 
        new OldMyTorrentsPluginItem(TableManager.TABLE_MYTORRENTS_COMPLETE, 
                                    name, item);
      tci.initialize(iAlign, item.getDefaultPosition(), item.getDefaultSize());
      addColumn(tci);
    }
    if ((iVisibleIn & PluginMyTorrentsItemFactory.TABLE_INCOMPLETE) != 0) {
      TableColumnCore tci = 
        new OldMyTorrentsPluginItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE, 
                                    name, item);
      tci.initialize(iAlign, item.getDefaultPosition(), item.getDefaultSize());
      addColumn(tci);
    }
  }

  /** 
   * Add an extension from the deprecated PluginPeerItemFactory
   * @deprecated
   */
  public void addExtension(String name, org.gudy.azureus2.plugins.ui.tables.peers.PluginPeerItemFactory item) {
    TableColumnCore tci = new OldPeerPluginItem(TableManager.TABLE_TORRENT_PEERS,
                                            name, item);
    tci.initialize(TableColumnCore.ALIGN_LEAD, 
                   TableColumnCore.POSITION_INVISIBLE, item.getDefaultSize());
    addColumn(tci);
  }

  /** Retrieves TableColumnCore objects of a particular type.
   * @param sTableID TABLE_* constant.  See {@link TableColumn} for list 
   * of constants
   *
   * @return Map of column definition objects matching the supplied criteria.
   *         key = name
   *         value = TableColumnCore object
   */
  public Map getTableColumnsAsMap(String sTableID) {
    //System.out.println("getTableColumnsAsMap(" + sTableID + ")");
    try{
    	items_mon.enter();
      Map mReturn = new LinkedHashMap();
      Map mTypes = (Map)items.get(sTableID);
      if (mTypes != null) {
        mReturn.putAll(mTypes);
      }
      //System.out.println("getTableColumnsAsMap(" + sTableID + ") returnsize: " + mReturn.size());
      return mReturn;
    }finally{
    	
    	items_mon.exit();
    }
  }
  
  public int getTableColumnCount(String sTableID) {
    Map mTypes = (Map)items.get(sTableID);
    if (mTypes == null) {
    	return 0;
    }
    return mTypes.size();
  }

  
  public TableColumnCore[] getAllTableColumnCoreAsArray(String sTableID) {
    Map mTypes = (Map)items.get(sTableID);
    if (mTypes != null) {
      return (TableColumnCore[])mTypes.values().toArray(new TableColumnCore[mTypes.values().size()]);
    }
    return new TableColumnCore[0];
  }
  
  public TableColumnCore getTableColumnCore(String sTableID,
                                            String sColumnName) {
    Map mTypes = (Map)items.get(sTableID);
    if (mTypes == null)
      return null;
    return (TableColumnCore)mTypes.get(sColumnName);
  }
  
  public void ensureIntegrety(String sTableID) {
    Map mTypes = (Map)items.get(sTableID);
    if (mTypes == null)
      return;

    TableColumnCore[] tableColumns = 
      (TableColumnCore[])mTypes.values().toArray(new TableColumnCore[mTypes.values().size()]);

    Arrays.sort(tableColumns, getTableColumnOrderComparator());

    int iPos = 0;
    for (int i = 0; i < tableColumns.length; i++) {
      int iCurPos = tableColumns[i].getPosition();
      if (iCurPos == TableColumnCore.POSITION_INVISIBLE) {
      	tableColumns[i].setVisible(false);
      } else {
        tableColumns[i].setPositionNoShift(iPos++);
      }
    }
  }
  
  public String getDefaultSortColumnName(String tableID) {
  	Map mapTableConfig = getTableConfigMap(tableID);
  	Object object = mapTableConfig.get("SortColumn");
  	if( object instanceof byte[])
  		object =  new String((byte[])object);
  	
  	if (object instanceof String) {
			return (String) object;
		}

		String s = COConfigurationManager.getStringParameter(tableID + ".sortColumn");
		if (s != null) {
			COConfigurationManager.removeParameter(tableID + ".sortColumn");
			COConfigurationManager.removeParameter(tableID + ".sortAsc");
		}
		return s;
  }
  
  public void setDefaultSortColumnName(String tableID, String columnName) {
  	Map mapTableConfig = getTableConfigMap(tableID);
  	mapTableConfig.put("SortColumn", columnName);
    FileUtil.writeResilientConfigFile(CONFIG_FILE, mapTablesConfig);
  }

  /** Saves all the user configurable Table Column settings at once, complete
   * with a COConfigurationManager.save().
   *
   * @param sTableID Table to save settings for
   */
  public void saveTableColumns(String sTableID) {
  	try {
  		Map mapTableConfig = getTableConfigMap(sTableID);
      TableColumnCore[] tcs = getAllTableColumnCoreAsArray(sTableID);
      for (int i = 0; i < tcs.length; i++) {
        if (tcs[i] != null)
          tcs[i].saveSettings(mapTableConfig);
      }
      FileUtil.writeResilientConfigFile(CONFIG_FILE, mapTablesConfig);
  	} catch (Exception e) {
  		Debug.out(e);
  	}
  }
  
  public Map getTableConfigMap(String sTableID) {
  	Map mapTableConfig = (Map) mapTablesConfig.get("Table." + sTableID);
  	if (mapTableConfig == null) {
  		mapTableConfig = new HashMap();
  		mapTablesConfig.put("Table." + sTableID, mapTableConfig);
  	}
  	return mapTableConfig;
  }
  
  public void setAutoHideOrder(String sTableID, String[] autoHideOrderColumnIDs) {
  	ArrayList autoHideOrderList = new ArrayList(autoHideOrderColumnIDs.length);
  	for (int i = 0; i < autoHideOrderColumnIDs.length; i++) {
			String sColumnID = autoHideOrderColumnIDs[i];
			TableColumnCore column = getTableColumnCore(sTableID, sColumnID);
			if (column != null) {
				autoHideOrderList.add(column);
			}
		}
  	
  	autoHideOrder.put(sTableID, autoHideOrderList);
  }
  
  public List getAutoHideOrder(String sTableID) {
		List list = (List) autoHideOrder.get(sTableID);
		if (list == null) {
			return Collections.EMPTY_LIST;
		}
		return list;
	}

	/**
	 * @param writer
	 */
	public void generateDiagnostics(IndentWriter writer) {
    try{
     	items_mon.enter();
     	
     	writer.println("TableColumns");

     	for (Iterator iter = items.keySet().iterator(); iter.hasNext();) {
     		String sTableID = (String)iter.next();
        Map mTypes = (Map)items.get(sTableID);

        writer.indent();
     		writer.println(sTableID + ": " + mTypes.size() + " columns:");
     		
     		writer.indent();
       	for (Iterator iter2 = mTypes.values().iterator(); iter2.hasNext();) {
       		TableColumnCore tc = (TableColumnCore)iter2.next();
       		tc.generateDiagnostics(writer);
       	}
        writer.exdent();

        writer.exdent();
			}
    } catch (Exception e) {
    	e.printStackTrace();
    } finally {
    	items_mon.exit();
    }
	}
	
	public static Comparator getTableColumnOrderComparator()
	{
		return orderComparator;
	}
}
