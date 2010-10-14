/*
 * Created on 2 juil. 2003
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
package org.gudy.azureus2.ui.swt.views;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.debug.ObfusticateTab;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

import org.gudy.azureus2.plugins.download.DownloadException;

/**
 * Torrent download view, consisting of several information tabs
 * 
 * @author Olivier
 * 
 */
public class ManagerView extends AbstractIView implements
		DownloadManagerListener, ObfusticateTab, ObfusticateImage {

  private AzureusCore		azureus_core;
  private DownloadManager 	manager;
  private TabFolder folder;
  private ArrayList tabViews = new ArrayList();
  
  public 
  ManagerView(
  	AzureusCore		_azureus_core,
	DownloadManager manager) 
  {
  	azureus_core	= _azureus_core;
  	dataSourceChanged(manager);
    
  }
  
  public void dataSourceChanged(Object newDataSource) {
  	super.dataSourceChanged(newDataSource);

    if (manager != null) {
    	manager.removeListener(this);
    }

  	DownloadImpl dataSourcePlugin = null;
  	if (newDataSource instanceof DownloadImpl) {
  		dataSourcePlugin = (DownloadImpl) newDataSource;
  		manager = dataSourcePlugin.getDownload();
  	} else if (newDataSource instanceof DownloadManager) {
    	manager = (DownloadManager) newDataSource;
      try {
      	dataSourcePlugin = DownloadManagerImpl.getDownloadStatic(manager);
      } catch (DownloadException e) { /* Ignore */ }
  	} else {
  		manager = null;
  	}
  	
    if (manager != null) {
    	manager.addListener(this);
    }

		for (int i = 0; i < tabViews.size(); i++) {
			IView view = (IView) tabViews.get(i);
			if (view != null) {
				if (view instanceof UISWTViewImpl) {
					((UISWTViewImpl) view).dataSourceChanged(dataSourcePlugin);
				} else {
					view.dataSourceChanged(newDataSource);
				}
			}
		}
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#delete()
   */
  public void delete() {
  	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
  	if (uiFunctions != null) {
  		uiFunctions.removeManagerView(manager);
  	}
  	if (manager != null) {
  		manager.removeListener(this);
  	}
    
    if ( !folder.isDisposed()){
    	
    	folder.setSelection(0);
    }
    
    //Don't ask me why, but without this an exception is thrown further
    // (in folder.dispose() )
    //TODO : Investigate to see if it's a platform (OSX-Carbon) BUG, and report to SWT team.
    if(Constants.isOSX) {
      if(folder != null && !folder.isDisposed()) {
        TabItem[] items = folder.getItems();
        for(int i=0 ; i < items.length ; i++) {
          if (!items[i].isDisposed())
            items[i].dispose();
        }
      }
    }

    for (int i = 0; i < tabViews.size(); i++) {
    	IView view = (IView) tabViews.get(i);
    	if (view != null)
    		view.delete();
    }
    tabViews.clear();

    if (folder != null && !folder.isDisposed()) {
      folder.dispose();
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getComposite()
   */
  public Composite getComposite() {
    return folder;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    int completed = manager.getStats().getCompleted();
    return DisplayFormatters.formatPercentFromThousands(completed) + " : " + manager.getDisplayName();
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite) {

  	if (folder == null) {
			folder = new TabFolder(composite, SWT.LEFT);
		} else {
			System.out.println("ManagerView::initialize : folder isn't null !!!");
		}
  	
	  IView views[] = { new GeneralView(), new PeersView(),
			new PeersGraphicView(), new PiecesView(), new FilesView(), new TorrentInfoView( manager ),
			new TorrentOptionsView( manager ), new LoggerView() };

		for (int i = 0; i < views.length; i++)
			addSection(views[i], manager);

    // Call plugin listeners
		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (uiFunctions != null) {
			UISWTInstanceImpl pluginUI = uiFunctions.getSWTPluginInstanceImpl();
			Map pluginViews = pluginUI == null ? null
					: pluginUI.getViewListeners(UISWTInstance.VIEW_MYTORRENTS);
			if (pluginViews != null) {
				String[] sNames = (String[]) pluginViews.keySet().toArray(new String[0]);
				for (int i = 0; i < sNames.length; i++) {
					UISWTViewEventListener l = (UISWTViewEventListener) pluginViews.get(sNames[i]);
					if (l != null) {
						try {
							UISWTViewImpl view = new UISWTViewImpl(
									UISWTInstance.VIEW_MYTORRENTS, sNames[i], l);
							addSection(view);
						} catch (Exception e) {
							// skip
						}
					}
				}
			}
		}
		
    
    // Initialize view when user selects it
    folder.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        TabItem item = (TabItem)e.item;
        if (item != null && item.getControl() == null) {
        	IView view = (IView)item.getData("IView");
        	
        	view.initialize(folder);
        	item.setControl(view.getComposite());
        }
        refresh();
      }
    });
    
    
    views[0].initialize(folder);
    folder.getItem(0).setControl(views[0].getComposite());
    views[0].refresh();
    views[0].getComposite().layout(true);
  }
  
  private IView getActiveView() {
		int index = folder.getSelectionIndex();
		if (index == -1) {
			return null;
		}

		TabItem ti = folder.getItem(index);
		if (ti.isDisposed()) {
			return null;
		}

		return (IView) ti.getData("IView");
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public void refresh() {
		if (getComposite() == null || getComposite().isDisposed())
			return;

		try {
			IView view = getActiveView();
			if (view != null)
				view.refresh();

			TabItem[] items = folder.getItems();
			
	    for (int i = 0; i < items.length; i++) {
	    	TabItem item = items[i];
	    	view = (IView) item.getData("IView");
        try {
          if (item.isDisposed())
            continue;
          String lastTitle = item.getText();
          String newTitle = view.getShortTitle();
          if (lastTitle == null || !lastTitle.equals(newTitle)) {
            item.setText(escapeAccelerators(newTitle));
          }
          String lastToolTip = item.getToolTipText();
          String newToolTip = view.getFullTitle() + " " +
					 MessageText.getString("Tab.closeHint");
          if (lastToolTip == null || !lastToolTip.equals(newToolTip)) {
            item.setToolTipText(newToolTip);
          }
        }
        catch (Exception e){
        	Debug.printStackTrace(e);
        }
      }
		} catch (Exception e) {
			Debug.printStackTrace(e);
		}
	}

  protected static String
  escapeAccelerators(
	 String	str )
  {
	  if ( str == null ){
		  
		  return( str );
	  }
	  
	  return( str.replaceAll( "&", "&&" ));
  }
  
  public boolean isEnabled(String itemKey) {
		if (itemKey.equals("run"))
			return true;

		if (itemKey.equals("start"))
			return ManagerUtils.isStartable(manager);

		if (itemKey.equals("stop"))
			return ManagerUtils.isStopable(manager);

		if (itemKey.equals("host"))
			return true;
		
		if (itemKey.equals("publish"))
			return true;

		if (itemKey.equals("remove"))
			return true;
		
		return false;
	}

	public void itemActivated(String itemKey) {
		if (itemKey.equals("run")) {
			ManagerUtils.run(manager);
			return;
		}
		
		if (itemKey.equals("start")) {
			ManagerUtils.queue(manager, folder.getShell());
			return;
		}
		
		if (itemKey.equals("stop")) {
			ManagerUtils.stop(manager, folder.getShell());
			return;
		}
		
		if (itemKey.equals("host")) {
			ManagerUtils.host(azureus_core, manager, folder);
			UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
			if (uiFunctions != null) {
				uiFunctions.showMyTracker();
			}
			return;
		}
		
		if (itemKey.equals("publish")) {
			ManagerUtils.publish(azureus_core, manager, folder);
			UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
			if (uiFunctions != null) {
				uiFunctions.showMyTracker();
			}
			return;
		}
		
		if (itemKey.equals("remove")) {
			ManagerUtils.remove(manager, null, false, false);
		}
	}
  
  
  public void downloadComplete(DownloadManager manager) {   
  }

  public void completionChanged(DownloadManager manager, boolean bCompleted) {
  }

  public void
  filePriorityChanged( DownloadManager download, org.gudy.azureus2.core3.disk.DiskManagerFileInfo file )
  {	  
  }
  
  public void stateChanged(DownloadManager manager, int state) {
    if(folder == null || folder.isDisposed())
      return;    
    Display display = folder.getDisplay();
    if(display == null || display.isDisposed())
      return;
    Utils.execSWTThread(new AERunnable() {
	    public void runSupport() {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
	    }
    });    
  }

  public void positionChanged(DownloadManager download, int oldPosition, int newPosition) {
  }

	public void addSection(UISWTViewImpl view) {
		Object pluginDataSource = null;
		try {
			pluginDataSource = DownloadManagerImpl.getDownloadStatic(manager);
		} catch (DownloadException e) { 
			/* Ignore */
		}
		addSection(view, pluginDataSource);
	}
	
	private void addSection(IView view, Object dataSource) {
		if (view == null)
			return;

		view.dataSourceChanged(dataSource);

		TabItem item = new TabItem(folder, SWT.NULL);
		Messages.setLanguageText(item, view.getData());
		item.setData("IView", view);
		tabViews.add(view);
	}

	public Image obfusticatedImage(Image image, Point shellOffset) {
		IView view = getActiveView();
		if (view instanceof ObfusticateImage) {
			try {
				((ObfusticateImage)view).obfusticatedImage(image, shellOffset);
			} catch (Exception e) {
				Debug.out("Obfusticating " + view, e);
			}
		}
		return image;
	}

	public String getObfusticatedHeader() {
    int completed = manager.getStats().getCompleted();
    return DisplayFormatters.formatPercentFromThousands(completed) + " : " + manager;
	}
	
	public DownloadManager getDownload() {return manager;}
	
}
