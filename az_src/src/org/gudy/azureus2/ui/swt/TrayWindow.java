/*
 * Created on 8 juil. 2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.ui.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.mainwindow.MainMenu;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.mainwindow.MenuFactory;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
/**
 * Download Basket
 * 
 * @author Olivier
 * 
 */
public class TrayWindow implements GlobalManagerListener {

  GlobalManager globalManager;
  List managers;
  protected AEMonitor managers_mon 	= new AEMonitor( "TrayWindow:managers" );


  MainWindow main;
  Display display;
  Shell minimized;
  Label label;
  private Menu menu;

  private Rectangle screen;

  private int xPressed;
  private int yPressed;
  private boolean moving;

  public TrayWindow(MainWindow _main) {
    this.managers = new ArrayList();
    this.main = _main;
    UIFunctionsSWT uif = UIFunctionsManagerSWT.getUIFunctionsSWT();
    Shell mainShell = uif == null ? Utils.findAnyShell() : uif.getMainShell();
    this.display = mainShell.getDisplay();
    minimized = ShellFactory.createShell(mainShell, SWT.ON_TOP);
    minimized.setText("Azureus"); //$NON-NLS-1$
    label = new Label(minimized, SWT.NULL);
    Image img = ImageRepository.getImage("tray");
    label.setImage(img); //$NON-NLS-1$
    final Rectangle bounds = img.getBounds();
    label.setSize(bounds.width, bounds.height);
    minimized.setSize(bounds.width + 2, bounds.height + 2);
    screen = display.getClientArea();
 //NICO handle macosx and multiple monitors
    if (!Constants.isOSX) {
    	minimized.setLocation(screen.x + screen.width - bounds.width - 2,
					screen.y + screen.height - bounds.height - 2);
    } else {
    	minimized.setLocation(20, 20);
    }
    minimized.layout();
    minimized.setVisible(false);
    //minimized.open();    

    MouseListener mListener = new MouseAdapter() {
      public void mouseDown(MouseEvent e) {
        xPressed = e.x;
        yPressed = e.y;
        moving = true;
        //System.out.println("Position : " + xPressed + " , " + yPressed);          
      }

      public void mouseUp(MouseEvent e) {
        moving = false;
      }

      public void mouseDoubleClick(MouseEvent e) {
        restore();
      }

    };
    MouseMoveListener mMoveListener = new MouseMoveListener() {
      public void mouseMove(MouseEvent e) {
        if (moving) {
          int dX = xPressed - e.x;
          int dY = yPressed - e.y;
          Point currentLoc = minimized.getLocation();
          int x = currentLoc.x - dX;
          int y = currentLoc.y - dY;
          if (x < 10)
            x = 0;
          if (x > screen.width - (bounds.width + 12))
            x = screen.width - (bounds.width + 2);
          if (y < 10)
            y = 0;
          if (y > screen.height - (bounds.height + 12))
            y = screen.height - (bounds.height + 2);
          minimized.setLocation(x, y);
        }
      }
    };

    label.addMouseListener(mListener);
    label.addMouseMoveListener(mMoveListener);

    menu = new Menu(minimized, SWT.CASCADE);
    label.setMenu(menu);

    MenuItem file_show = new MenuItem(menu, SWT.NULL);
    Messages.setLanguageText(file_show, "TrayWindow.menu.show"); //$NON-NLS-1$
    menu.setDefaultItem(file_show);
    file_show.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        restore();
      }
    });

    new MenuItem(menu, SWT.SEPARATOR);
    
    MenuFactory.addCloseDownloadBarsToMenu(menu);
    
    new MenuItem(menu, SWT.SEPARATOR);

    MenuItem file_startalldownloads = new MenuItem(menu, SWT.NULL);
    Messages.setLanguageText(file_startalldownloads, "TrayWindow.menu.startalldownloads"); //$NON-NLS-1$
    file_startalldownloads.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
            globalManager.startAllDownloads();
        }
    });    
    
    MenuItem file_stopalldownloads = new MenuItem(menu, SWT.NULL);
    Messages.setLanguageText(file_stopalldownloads, "TrayWindow.menu.stopalldownloads"); //$NON-NLS-1$
    file_stopalldownloads.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
      	ManagerUtils.asyncStopAll();
      }
    });

    new MenuItem(menu, SWT.SEPARATOR);

    MenuItem file_close = new MenuItem(menu, SWT.NULL);
    Messages.setLanguageText(file_close, "TrayWindow.menu.close");
    file_close.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        COConfigurationManager.setParameter("Show Download Basket", false);
      }
    });

    MenuItem file_exit = new MenuItem(menu, SWT.NULL);
    Messages.setLanguageText(file_exit, "TrayWindow.menu.exit"); //$NON-NLS-1$
    file_exit.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        main.dispose(false,false);
      }
    });

    Utils.createTorrentDropTarget(minimized, false);
    try {
    	globalManager = AzureusCoreFactory.getSingleton().getGlobalManager();
    	globalManager.addListener(this);
    } catch (Exception e) {
    	Debug.out(e);
    }
  }

  public void setVisible(boolean visible) {
    if(visible || !COConfigurationManager.getBooleanParameter("Show Download Basket")) {
      minimized.setVisible(visible);
      if (!visible)
        moving = false;
    }
  }

  public void dispose() {
    minimized.dispose();
  }

  public void restore() {
    if(!COConfigurationManager.getBooleanParameter("Show Download Basket"))
      minimized.setVisible(false);
    UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
    if (functionsSWT != null) {
    	functionsSWT.bringToFront();
    }
    moving = false;
  }

  public void refresh() {
    if (minimized.isDisposed() || !minimized.isVisible())
      return;

    StringBuffer toolTip = new StringBuffer();
    String separator = ""; //$NON-NLS-1$
    try{
      managers_mon.enter();
      for (int i = 0; i < managers.size(); i++) {
        DownloadManager manager = (DownloadManager) managers.get(i);
		DownloadManagerStats	stats = manager.getStats();
		
        String name = manager.getDisplayName();
        String completed = DisplayFormatters.formatPercentFromThousands(stats.getCompleted());
        toolTip.append(separator);
        toolTip.append(name);
        toolTip.append(" -- C: ");
        toolTip.append(completed);
        toolTip.append(", D : ");
				toolTip.append(DisplayFormatters.formatDataProtByteCountToKiBEtcPerSec(
						stats.getDataReceiveRate(), stats.getProtocolReceiveRate()));
				toolTip.append(", U : ");
				toolTip.append(DisplayFormatters.formatDataProtByteCountToKiBEtcPerSec(
						stats.getDataSendRate(), stats.getProtocolSendRate()));
        separator = "\n"; //$NON-NLS-1$
      }
    }finally{
    	managers_mon.exit();
    }
    //label.setToolTipText(toolTip.toString());
    //minimized.moveAbove(null);
  }
 
   public void downloadManagerAdded(DownloadManager created) {
     try{
     	managers_mon.enter();
     
     	managers.add(created);
     }finally{
     	
     	managers_mon.exit();
    }
  }

   public void downloadManagerRemoved(DownloadManager removed) {
    try{
    	managers_mon.enter();
    	
    	managers.remove(removed);
    }finally{
    	managers_mon.exit();
    }
  }

  // globalmanagerlistener
	
	public void
	destroyed()
	{
	}
	
	public void
	destroyInitiated()
	{
	}

    public void seedingStatusChanged( boolean seeding_only_mode ){
    }
	
  public void updateLanguage() {
  	MenuFactory.updateMenuText(menu);
  }

  /**
   * @param moving
   */
  public void setMoving(boolean moving) {
    this.moving = moving;
  }

}
