/*
 * Created on Apr 9, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004, 2005, 2006 Alon Rohter, All Rights Reserved.
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
package org.gudy.azureus2.ui.swt.snippets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread;

/**
 * 
 */
public class OnTopProblem {
  private static int[] sizes = {100,120,140};
  
  Display display;
  Shell mainShell;
  Shell onTopShell;
  
  Label labelIter;
  
  int iter;
  
  
  public OnTopProblem() {
    display = new Display();
    mainShell = new Shell(display,SWT.SHELL_TRIM);        
    mainShell.setText("OnTopProblem");
    
    mainShell.setLayout(new FillLayout());
    
    Button btnClose = new Button(mainShell,SWT.PUSH);
    btnClose.setText("Close");
    btnClose.addListener(SWT.Selection,new Listener() {
    	public void handleEvent(Event arg0) {
    		mainShell.dispose();
    	} 
    });    
    
    mainShell.setSize(300,200);
    mainShell.open();
    
    onTopShell = new Shell(mainShell,SWT.ON_TOP);
    onTopShell.setSize(200,30);
    onTopShell.open();
    
    onTopShell.setLayout(new FillLayout());
    
    labelIter = new Label(onTopShell,SWT.NULL);
    
    Tray tray = display.getSystemTray();
    TrayItem trayItem = new TrayItem(tray,SWT.NULL);
    trayItem.addListener(SWT.DefaultSelection, new Listener() {
      public void handleEvent(Event e) {
       mainShell.setVisible(true); 
      }
    });
    
    mainShell.addListener(SWT.Close, new Listener(){
        public void handleEvent(Event e) {
        	e.doit = false;
          mainShell.setVisible(false);
          onTopShell.setVisible(true);
        }
    });
    
    Thread t = new AEThread("OnTopProblem") {
      public void runSupport() {
       while(updateDisplay()) {
        try { Thread.sleep(100); } catch(Exception ignore) {}   
       }
      }
     };
     
     t.start();
    
    waitForDispose();
    display.dispose();
  }
  

  
  public void waitForDispose() {
    while(!mainShell.isDisposed()) {
      if(!display.readAndDispatch())
        display.sleep();
    }
  }
  
  public boolean updateDisplay() {
    if(display != null && ! display.isDisposed() ) {
      display.asyncExec(new AERunnable() {
        public void runSupport() {
          iter++;
          labelIter.setText("" + iter);
          onTopShell.setSize(sizes[iter % sizes.length],20);
        }
      });
      return true;
    }
    return false;
  }
  
  public static void main(String args[]) {
    new OnTopProblem();
  }
}
