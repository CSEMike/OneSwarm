/*
 * Copyright (c) 2000, 2003 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
 
/*
 * Table example snippet: place arbitrary controls in a table
 *
 * For a list of all SWT example snippets see
 * http://dev.eclipse.org/viewcvs/index.cgi/%7Echeckout%7E/platform-swt-home/dev.html#snippets
 */

package org.gudy.azureus2.ui.swt.test;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

public class Main {
  
  private Color blue;
  private Table table;
  
  
  private boolean mousePressed;
  private TableItem selectedItem;
  Rectangle oldBounds;
  Image oldImage;
  
  public Main() {
    final Display display = new Display ();
    blue = new Color(display,0,0,128);
    final Shell shell = new Shell (display);
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    GridData gridData;
    shell.setLayout (layout); 
    table = new Table (shell, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 3;
    table.setLayoutData(gridData);
    table.setLinesVisible (true);    
    Font f = table.getFont();
    FontData fd = f.getFontData()[0];
    fd.setHeight(9);
    Font font = new Font(display, fd);
    table.setFont(font);
    
    Button bOk = new Button(shell,SWT.PUSH);
    bOk.setText("Ok");
    gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.HORIZONTAL_ALIGN_FILL);
    gridData.grabExcessHorizontalSpace = true;
    gridData.widthHint = 70;
    bOk.setLayoutData(gridData);
      
    Button bCancel = new Button(shell,SWT.PUSH);
    bCancel.setText("Cancel");
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
    gridData.grabExcessHorizontalSpace = false;
    gridData.widthHint = 70;
    bCancel.setLayoutData(gridData);
      
    Button bApply = new Button(shell,SWT.PUSH);
    bApply.setText("Apply");
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
    gridData.grabExcessHorizontalSpace = false;
    gridData.widthHint = 70;
    bApply.setLayoutData(gridData);
    
    for (int i=0; i<2; i++) {
      new TableColumn(table, SWT.NONE);    
    }
    for (int i=0; i<12; i++) {
      createTableRow(-1,"Toto" + i , false);
    }
    TableItem item  = new TableItem(table,SWT.NULL);
    item.setText(1,"---");
    //Hack to get a correct width
    table.getColumn(0).setWidth(20);
    table.getColumn(1).setWidth(200);
    
    
    table.addMouseListener(new MouseAdapter() {
      
      public void mouseDown(MouseEvent arg0) {
        mousePressed = true;
        selectedItem = table.getItem(new Point(arg0.x,arg0.y));
        if(selectedItem.getText(1).equals("---")) {
          selectedItem = null;
        }
      }
      
      public void mouseUp(MouseEvent e) {
        mousePressed = false;
        //1. Restore old image
        if(oldBounds != null && oldImage != null) {
          GC gc = new GC(table);
          gc.drawImage(oldImage,oldBounds.x,oldBounds.y);
          oldImage.dispose();
          oldImage = null;
          oldBounds = null;
        }
        Point p = new Point(e.x,e.y);
        TableItem item = table.getItem(p);
        if(item != null && selectedItem != null) {
          int index = table.indexOf(item);
          int oldIndex = table.indexOf(selectedItem);
          if(index == oldIndex)
            return;
          String name = (String) selectedItem.getData("name");
          Button oldBtn = (Button)selectedItem.getData("button");
          boolean selected = oldBtn.getSelection();
          oldBtn.dispose();
          createTableRow(index,name,selected);
          selectedItem.dispose();        
          Point size = shell.getSize();
          shell.setSize(size.x+1,size.y+1);
          shell.setSize(size);
        }
      }
    });
    
    table.addMouseMoveListener(new MouseMoveListener(){
      public void mouseMove(MouseEvent e) {
        if(mousePressed && selectedItem != null) {
          Point p = new Point(e.x,e.y);
          TableItem item = table.getItem(p);
          if(item != null) {
            GC gc = new GC(table);
            Rectangle bounds = item.getBounds(1);
            //1. Restore old image
            if(oldBounds != null && oldImage != null) {
              gc.drawImage(oldImage,oldBounds.x,oldBounds.y);
              oldImage.dispose();
              oldImage = null;
              oldBounds = null;
            }
            //2. Store the image
            oldImage = new Image(display,bounds.width,2);
            gc.copyArea(oldImage,bounds.x,bounds.y);
            oldBounds = bounds;
            
            //3. Draw a thick line
            gc.setBackground(blue);
            gc.fillRectangle(bounds.x,bounds.y,bounds.width,2);
          }
        }
      }
    });
    shell.pack ();
    shell.open ();
    while (!shell.isDisposed ()) {
      if (!display.readAndDispatch ()) display.sleep ();
    }
    display.dispose ();
    if (font != null && !font.isDisposed()) {
      font.dispose();
    }
}
  
  private void createTableRow(int index,String name,boolean selected) {
    TableItem item;
    
    if(index == -1)
      item = new TableItem (table, SWT.NONE);
    else
      item = new TableItem (table, SWT.NONE,index);
    
    item.setText(1,name);
    item.setData("name",name);
    TableEditor editor = new TableEditor (table);
    Button button = new Button (table, SWT.CHECK);
    button.setSelection(selected);
    button.pack ();
    editor.minimumWidth = button.getSize ().x;    
    editor.horizontalAlignment = SWT.CENTER;
    editor.setEditor (button, item, 0);
    item.setData("button",button);      
  }
  
public static void main(String[] args) {
  new Main();
}
}

