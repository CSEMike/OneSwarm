package org.gudy.azureus2.plugins.ui.tables;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.common.table.impl.TableColumnImpl;


/**
 * 
 * Provides a simple way to get a TableCell refreshed more often than the normal GUI refresh cycle
 * It always clocks at 100ms
 * as well as time synchronization methods for cells showing animated icons
 * @author olivier
 *
 */
public class TableCellRefresher {
	
	private static TableCellRefresher instance = null;
	
	private  AEThread2 refresher;
	
	private Map<TableCell, TableColumn> mapCellsToColumn = new HashMap<TableCell, TableColumn>();
	
	private  long iterationNumber;
	
	private boolean inProgress = false;

	private AERunnable runnable;
	
	private TableCellRefresher() {
		runnable = new AERunnable() {
			public void runSupport() {
				try {
					Map<TableCell, TableColumn> cellsCopy;
  				synchronized (mapCellsToColumn) {
  					cellsCopy = new HashMap<TableCell, TableColumn>(mapCellsToColumn);
  					mapCellsToColumn.clear();
  				}

  				for (TableCell cell : cellsCopy.keySet()) {
  					TableColumn column = (TableColumn) cellsCopy.get(cell);

  					try {
  						//cc.cell.invalidate();
  						if (column instanceof TableCellRefreshListener) {
  							((TableCellRefreshListener) column).refresh(cell);
  						}else if ( column instanceof TableColumnImpl ){
  							List<TableCellRefreshListener> listeners =((TableColumnImpl)column).getCellRefreshListeners();
  							for ( TableCellRefreshListener listener: listeners ){
  								listener.refresh(cell);
  							}
  						}
  
  					} catch (Throwable t) {
  						t.printStackTrace();
  					}
  				}
				} finally {
					inProgress = false;
				}
			}
		};
		
		refresher = new AEThread2("Cell Refresher",true) {
			public void run() {
				try {
					
					iterationNumber = 0;
					
					while (true) {

						if (mapCellsToColumn.size() > 0 && !inProgress) {
							inProgress = true;
  						Utils.execSWTThread(runnable);
						}

						Thread.sleep(200);

						iterationNumber++;
					}
				}catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		
		refresher.start();
	}
	
	
	private void _addColumnCell(TableColumn column,TableCell cell) {
		synchronized (mapCellsToColumn) {
			if (mapCellsToColumn.containsKey(cell)) {
				return;
			}
			mapCellsToColumn.put(cell, column);
		}
	}
	
	private int _getRefreshIndex(int refreshEvery100ms, int nbIndices) {
		if(refreshEvery100ms <= 0) return 1;
		if(nbIndices <= 0) return 1;
		
		return (int) ( (iterationNumber / refreshEvery100ms) % nbIndices);
	}
	
	private static synchronized TableCellRefresher getInstance() {
		if(instance == null) {
			instance = new TableCellRefresher();
		}
		return instance;
	}
	
	//Add a cell to be refreshed within the next iteration
	//The cell will only get refreshed once
	public static void addCell(TableColumn column,TableCell cell) {
		
		getInstance()._addColumnCell(column,cell);
	}
	
	
	
	public static int getRefreshIndex(int refreshEvery100ms, int nbIndices) {
		return getInstance()._getRefreshIndex(refreshEvery100ms, nbIndices);
	}

}
