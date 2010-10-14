/*
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.views.table.impl.FakeTableCell;
import org.gudy.azureus2.ui.swt.views.utils.VerticalAligner;

import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableStructureModificationListener;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

/**
 * Choose columns to display, and in what order
 */
public class TableColumnEditorWindow {
  
  private Display display;
  private Shell shell;
  private Table table;
  
  //private TableColumnCore[] tableColumns;
  private ArrayList tableColumns;
  private Map newEnabledState;
  private TableStructureModificationListener listener;
  
  private boolean mousePressed;
  private TableItem selectedItem;
  private Point oldPoint;
	private Composite cSample;
  private FakeTableCell fakeTableCell;
	private final String tableID;


  /**
   * Default Constructor
   * 
   * @param parent Parent Shell
   * @param _tableColumns List of columns available
   * @param _listener Callback listener to trigger when columns changed
   */
  public TableColumnEditorWindow(Shell parent, String sTableID,
			TableColumnCore[] _tableColumns, final Object sampleDS,
			TableStructureModificationListener _listener) {    
    tableID = sTableID;
		RowData rd;
    display = parent.getDisplay();
    listener = _listener;
    
    tableColumns = new ArrayList(Arrays.asList(_tableColumns));
    Collections.sort(tableColumns, new Comparator () {
      public final int compare (Object a, Object b) {
        int iPositionA = ((TableColumnCore)a).getPosition();
        if (iPositionA == -1)
          iPositionA = 0xFFFF;
        int iPositionB = ((TableColumnCore)b).getPosition();
        if (iPositionB == -1)
          iPositionB = 0xFFFF;

        return iPositionA - iPositionB;
      }
    });
    
    newEnabledState = new HashMap();
    for (Iterator iter = tableColumns.iterator(); iter.hasNext();) {
			TableColumnCore item = (TableColumnCore) iter.next();
			Boolean value = new Boolean(
					item.getPosition() != org.gudy.azureus2.plugins.ui.tables.TableColumn.POSITION_INVISIBLE
							&& item.isVisible()); 
			newEnabledState.put(item, value);
		}
    
    final Color blue = ColorCache.getColor(display,0,0,128);
    
    shell = ShellFactory.createShell(parent, SWT.DIALOG_TRIM | SWT.RESIZE);
    Utils.setShellIcon(shell);
    shell.setText(MessageText.getString("columnChooser.title"));
    
    GridLayout layout = new GridLayout();
    shell.setLayout (layout);
    
    GridData gridData;
    
    Label label = new Label(shell,SWT.NULL);
    label.setText(MessageText.getString("columnChooser.move"));
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    label.setLayoutData(gridData);
    
    table = new Table (shell, SWT.VIRTUAL | SWT.CHECK | SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
    gridData = new GridData(GridData.FILL_BOTH);
    table.setLayoutData(gridData);
    table.setHeaderVisible(true);
    
    cSample = new Composite(shell, SWT.BORDER); 
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    cSample.setLayoutData(gridData);
    
    Composite cButtonArea = new Composite(shell, SWT.NULL);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
    cButtonArea.setLayoutData(gridData);
    RowLayout rLayout = new RowLayout(SWT.HORIZONTAL);
    rLayout.marginLeft = 0;
 		rLayout.marginTop = 0;
 		rLayout.marginRight = 0;
 		rLayout.marginBottom = 0;
 		rLayout.spacing = 5;
 		cButtonArea.setLayout (rLayout);
    
    Button bOk = new Button(cButtonArea,SWT.PUSH);
    bOk.setText(MessageText.getString("Button.ok"));
    rd = new RowData();
    rd.width = 70;
    bOk.setLayoutData(rd);
    bOk.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        saveAndApply();
        close();
      }
    });
    
    Button bCancel = new Button(cButtonArea,SWT.PUSH);
    bCancel.setText(MessageText.getString("Button.cancel"));
    rd = new RowData();
    rd.width = 70;
    bCancel.setLayoutData(rd);
    bCancel.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        close();
      }
    });
    
    Button bApply = new Button(cButtonArea,SWT.PUSH);
    bApply.setText(MessageText.getString("columnChooser.apply"));
    rd = new RowData();
    rd.width = 70;
    bApply.setLayoutData(rd);
    bApply.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        saveAndApply();
      }
    });
    
    
    final String[] columnsHeader = { "columnname", "columndescription" };
    for (int i=0; i< columnsHeader.length; i++) {
      TableColumn column = new TableColumn(table, SWT.NONE);    
      if (columnsHeader[i] != "")
        column.setText(MessageText.getString("columnChooser." + columnsHeader[i]));
    }
    table.getColumn(0).setWidth(160);
    table.getColumn(1).setWidth(1000);

    table.addListener(SWT.Selection,new Listener() {
			public void handleEvent(Event e) {
				TableItem item = (TableItem) e.item;
				int index = item.getParent().indexOf(item);
				TableColumnCore tableColumn = (TableColumnCore)tableColumns.get(index);

				if (sampleDS != null) {
  				if (fakeTableCell != null && !fakeTableCell.isDisposed()) {
  					fakeTableCell.setControl(null);
  				}
  		    fakeTableCell = new FakeTableCell(tableColumn);
  		    fakeTableCell.setControl(cSample);
  		    if (sampleDS instanceof TableRowCore) {
  		    	fakeTableCell.setDataSource(((TableRowCore)sampleDS).getDataSource(tableColumn.getUseCoreDataSource()));
  		    } else {
  		    	fakeTableCell.setDataSource(sampleDS);
  		    }
  		    if (tableColumn instanceof TableColumnCore) {
  		    	((TableColumnCore)tableColumn).invokeCellAddedListeners(fakeTableCell);
  		    	try {
  						((TableColumnCore)tableColumn).invokeCellRefreshListeners(fakeTableCell, false);
  					} catch (Throwable e1) {
  						// TODO Auto-generated catch block
  						e1.printStackTrace();
  					}
  		    }
  		    fakeTableCell.refresh();
  		    cSample.redraw();
  				cSample.update(); // force initial update
				}
  
				if (e.detail != SWT.CHECK)
      		return;
        mousePressed = false;

				newEnabledState.put(tableColumn, new Boolean(item.getChecked()));
      }
    });
    
    table.addListener(SWT.SetData, new Listener() {
			public void handleEvent(Event event) {
				final TableItem item = (TableItem) event.item;
				if (item == null)
					return;
				Table table = item.getParent();
				int index = table.indexOf(item);
				if (index < 0) {
					// Trigger a Table._getItem, which assigns the item to the array
					// in Table, so indexOf(..) can find it.  This is a workaround for
					// a WinXP bug.
					Rectangle r = item.getBounds(0);
					table.getItem(new Point(r.x, r.y));
					index = table.indexOf(item);
					if (index < 0)
						return;
				}
				
				TableColumnCore tableColumn = (TableColumnCore)tableColumns.get(index);
		    String sTitleLanguageKey = tableColumn.getTitleLanguageKey();
		    item.setText(0, MessageText.getString(sTitleLanguageKey));
		    item.setText(1, MessageText.getString(sTitleLanguageKey + ".info", ""));
		    
		    //Causes SetData listener to be triggered again, which messes up SWT 
	      //table.getColumn(1).pack();

		    final boolean bChecked = ((Boolean) newEnabledState.get(tableColumn))
						.booleanValue();
		    Utils.setCheckedInSetData(item, bChecked);
		    Utils.alternateRowBackground(item);
			}
    });
    table.setItemCount(tableColumns.size());
    
    table.addMouseListener(new MouseAdapter() {
      
      public void mouseDown(MouseEvent arg0) {
        mousePressed = true;
        selectedItem = table.getItem(new Point(arg0.x,arg0.y));
      }
      
      public void mouseUp(MouseEvent e) {
        mousePressed = false;
        //1. Restore old image
        if(oldPoint != null) {
        	table.redraw(oldPoint.x, oldPoint.y, shell.getSize().x,
							oldPoint.y + 2, false);
          oldPoint = null;
        }
        Point p = new Point(e.x,e.y);
        TableItem item = table.getItem(p);
        if(item != null && selectedItem != null) {
          int index = table.indexOf(item);
          int oldIndex = table.indexOf(selectedItem);
          if(index == oldIndex)
            return;

          TableColumnCore tableColumn = 
                           (TableColumnCore)tableColumns.get(oldIndex);
          
          tableColumns.remove(tableColumn);
          tableColumns.add(index, tableColumn);
          table.clearAll();
        }
      }
    });
    
    table.addMouseMoveListener(new MouseMoveListener(){
      public void mouseMove(MouseEvent e) {
        if (!mousePressed || selectedItem == null)
          return;

        Point p = new Point(e.x,e.y);
        TableItem item = table.getItem(p);
        if (item == null)
          return;

        Rectangle bounds = item.getBounds(0);
        int selectedPosition = table.indexOf(selectedItem);
        int newPosition = table.indexOf(item);

        //1. Restore old area
        if(oldPoint != null) {
        	table.redraw(oldPoint.x, oldPoint.y, bounds.width, oldPoint.y + 2, false);
          oldPoint = null;
        }            
        bounds.y += VerticalAligner.getTableAdjustVerticalBy(table);
        if(newPosition <= selectedPosition)
          oldPoint = new Point(bounds.x,bounds.y);
        else
          oldPoint = new Point(bounds.x,bounds.y+bounds.height);

        //3. Draw a thick line
      	table.redraw(oldPoint.x, oldPoint.y, bounds.width, oldPoint.y + 2, false);
      }
    });
    
    table.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				if (!mousePressed || selectedItem == null || oldPoint == null) {
					return;
				}
				
        Point p = new Point(e.x,e.y);
        TableItem item = table.getItem(p);
        if (item == null)
          return;
		
        Rectangle bounds = item.getBounds(0);
        GC gc = new GC(table);
        gc.setBackground(blue);
        gc.fillRectangle(oldPoint.x,oldPoint.y,bounds.width,2);
        gc.dispose();
			}
		});

    shell.pack();
    Point p = shell.getSize();
    p.x = 550;
    // For Windows, to get rid of the scrollbar
    p.y += 2;
    
    if (p.y + 64 > display.getClientArea().height)
    	p.y = display.getBounds().height - 64;
    
    shell.setSize(p);
    
    Utils.centreWindow(shell);
    shell.open (); 
  }
  
	private void close() {
    if (!shell.isDisposed())
    	shell.dispose();
  }
  
  private void saveAndApply() {
		for (int i = 0; i < tableColumns.size(); i++) {
			TableColumnCore tableColumn = (TableColumnCore) tableColumns.get(i);
			boolean bChecked = ((Boolean) newEnabledState.get(tableColumn)).booleanValue();
			tableColumn.setPositionNoShift(i);
			tableColumn.setVisible(bChecked);
		}
		TableColumnManager.getInstance().saveTableColumns(tableID);
		listener.tableStructureChanged();
	}
}

