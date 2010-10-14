/*
 * File    : MySharesView.java
 * Created : 18-Jan-2004
 * By      : parg
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


package org.gudy.azureus2.ui.swt.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.category.CategoryManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentManagerImpl;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.tableitems.myshares.CategoryItem;
import org.gudy.azureus2.ui.swt.views.tableitems.myshares.NameItem;
import org.gudy.azureus2.ui.swt.views.tableitems.myshares.TypeItem;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.*;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.tracker.TrackerTorrent;
import org.gudy.azureus2.plugins.ui.tables.TableManager;

/**
 * @author parg
 * @author TuxPaper
 *         2004/Apr/20: Remove need for tableItemToObject
 *         2004/Apr/21: extends TableView instead of IAbstractView
 */
public class MySharesView 
extends TableViewTab
implements ShareManagerListener,
		TableLifeCycleListener, TableViewSWTMenuFillListener,
		TableRefreshListener
{
  private static final TableColumnCore[] basicItems = {
    new NameItem(),
    new TypeItem(),
	new CategoryItem(),
  };
  
	protected static final TorrentAttribute	category_attribute = 
		TorrentManagerImpl.getSingleton().getAttribute( TorrentAttribute.TA_CATEGORY );

  	private AzureusCore		azureus_core;
  	
	private GlobalManager	global_manager;
	
	private Menu			menuCategory;

	private TableViewSWTImpl tv;
	
	public 
	MySharesView(
		AzureusCore	_azureus_core )
	{	
		tv = new TableViewSWTImpl(TableManager.TABLE_MYSHARES, "MySharesView",
				basicItems, "name", SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER
						| SWT.VIRTUAL);
		setTableView(tv);
		azureus_core	= _azureus_core;
		global_manager = azureus_core.getGlobalManager();

		tv.addSelectionListener(new TableSelectionAdapter() {
			public void defaultSelected(TableRowCore[] rows) {
				MySharesView.this.defaultSelected(rows);
			}
		
		}, false);

		tv.addLifeCycleListener(this);
		tv.addMenuFillListener(this);
		tv.addRefreshListener(this, false);
	}
	
	private void defaultSelected(TableRowCore[] rows) {
		ShareResource share = (ShareResource) tv.getFirstSelectedDataSource();
		if (share == null) {
			return;
		}

		List dms = global_manager.getDownloadManagers();

		for (int i = 0; i < dms.size(); i++) {
			DownloadManager dm = (DownloadManager) dms.get(i);

			try {
				byte[] share_hash = null;

				if (share.getType() == ShareResource.ST_DIR) {

					share_hash = ((ShareResourceDir) share).getItem().getTorrent().getHash();

				} else if (share.getType() == ShareResource.ST_FILE) {

					share_hash = ((ShareResourceFile) share).getItem().getTorrent().getHash();
				}

				if (Arrays.equals(share_hash, dm.getTorrent().getHash())) {

					UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
					if (uiFunctions != null) {
						uiFunctions.openManagerView(dm);
					}

					break;
				}
			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}
	
	public void tableViewInitialized() {
		createRows();
	}
	
	public void tableViewDestroyed() {
		try {
			azureus_core.getPluginManager().getDefaultPluginInterface().getShareManager().removeListener(
					this);
		} catch (ShareException e) {
			Debug.printStackTrace(e);
		}
	}

  private void createRows() {
		try{

			ShareManager	sm = azureus_core.getPluginManager().getDefaultPluginInterface().getShareManager();
			
			ShareResource[]	shares = sm.getShares();
			
			for (int i=0;i<shares.length;i++){
				
				resourceAdded(shares[i]);
			}
			
			sm.addListener(this);
			
		}catch( ShareException e ){
			
			Debug.printStackTrace( e );
		}
	}

  public void 
  fillMenu(
  	final Menu menu) 
  {
  	Shell shell = menu.getShell();
		/*
	   final MenuItem itemStart = new MenuItem(menu, SWT.PUSH);
	   Messages.setLanguageText(itemStart, "MySharesView.menu.start"); //$NON-NLS-1$
	   itemStart.setImage(ImageRepository.getImage("start"));

	   final MenuItem itemStop = new MenuItem(menu, SWT.PUSH);
	   Messages.setLanguageText(itemStop, "MySharesView.menu.stop"); //$NON-NLS-1$
	   itemStop.setImage(ImageRepository.getImage("stop"));
	   */
		
	    menuCategory = new Menu(shell, SWT.DROP_DOWN);
	    final MenuItem itemCategory = new MenuItem(menu, SWT.CASCADE);
	    Messages.setLanguageText(itemCategory, "MyTorrentsView.menu.setCategory"); //$NON-NLS-1$
	    //itemCategory.setImage(ImageRepository.getImage("speed"));
	    itemCategory.setMenu(menuCategory);

	    addCategorySubMenu();
	    
	    new MenuItem(menu, SWT.SEPARATOR);

	   final MenuItem itemRemove = new MenuItem(menu, SWT.PUSH);
	   Messages.setLanguageText(itemRemove, "MySharesView.menu.remove"); //$NON-NLS-1$
	   Utils.setMenuItemImage(itemRemove, "delete");


	   Object[] shares = tv.getSelectedDataSources();

	   itemRemove.setEnabled(shares.length > 0);

	   itemRemove.addListener(SWT.Selection, new Listener() {
		 public void handleEvent(Event e) {
		   removeSelectedShares();
		 }   
	   });

    new MenuItem(menu, SWT.SEPARATOR);
	}
	
	public void resourceAdded(ShareResource resource) {		
	  tv.addDataSource(resource);
	}
	
	public void resourceModified(ShareResource resource) { }
	
	public void resourceDeleted(ShareResource resource) {
	  tv.removeDataSource(resource);
	}
	
	public void reportProgress(final int percent_complete) {	}
	
	public void	reportCurrentTask(final String task_description) { }
 
	public void tableRefresh() {
		computePossibleActions();
	  	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
 	  	if (uiFunctions != null) {
 	  		uiFunctions.refreshIconBar();
 	  	}
	}	 

	 private void addCategorySubMenu() {
	    MenuItem[] items = menuCategory.getItems();
	    int i;
	    for (i = 0; i < items.length; i++) {
	      items[i].dispose();
	    }

	    Category[] categories = CategoryManager.getCategories();
	    Arrays.sort(categories);

	    if (categories.length > 0) {
	      Category catUncat = CategoryManager.getCategory(Category.TYPE_UNCATEGORIZED);
	      if (catUncat != null) {
	        final MenuItem itemCategory = new MenuItem(menuCategory, SWT.PUSH);
	        Messages.setLanguageText(itemCategory, catUncat.getName());
	        itemCategory.setData("Category", catUncat);
	        itemCategory.addListener(SWT.Selection, new Listener() {
	          public void handleEvent(Event event) {
	            MenuItem item = (MenuItem)event.widget;
	            assignSelectedToCategory((Category)item.getData("Category"));
	          }
	        });

	        new MenuItem(menuCategory, SWT.SEPARATOR);
	      }

	      for (i = 0; i < categories.length; i++) {
	        if (categories[i].getType() == Category.TYPE_USER) {
	          final MenuItem itemCategory = new MenuItem(menuCategory, SWT.PUSH);
	          itemCategory.setText(categories[i].getName());
	          itemCategory.setData("Category", categories[i]);

	          itemCategory.addListener(SWT.Selection, new Listener() {
	            public void handleEvent(Event event) {
	              MenuItem item = (MenuItem)event.widget;
	              assignSelectedToCategory((Category)item.getData("Category"));
	            }
	          });
	        }
	      }

	      new MenuItem(menuCategory, SWT.SEPARATOR);
	    }

	    final MenuItem itemAddCategory = new MenuItem(menuCategory, SWT.PUSH);
	    Messages.setLanguageText(itemAddCategory,
	                             "MyTorrentsView.menu.setCategory.add");

	    itemAddCategory.addListener(SWT.Selection, new Listener() {
	      public void handleEvent(Event event) {
	        addCategory();
	      }
	    });

	  }
	
	  private void addCategory() {
	    CategoryAdderWindow adderWindow = new CategoryAdderWindow(Display.getDefault());
	    Category newCategory = adderWindow.getNewCategory();
	    if (newCategory != null)
	      assignSelectedToCategory(newCategory);
	  }
	  
	  private void assignSelectedToCategory(final Category category) {
	    tv.runForSelectedRows(new TableGroupRowRunner() {
	      public void run(TableRowCore row) {
	      	String value;
	      	
	      	if ( category == null ){
	      		
	      		value = null;
	      		
	      	}else if ( category == CategoryManager.getCategory(Category.TYPE_UNCATEGORIZED)){
	      		
	      		value = null;
	      		
	      	}else{
	      		
	      		value = category.getName();
	      	}
	      	
	        ((ShareResource)row.getDataSource(true)).setAttribute( category_attribute, value );
	      }
	    });
	  }
	  
  private boolean start,stop,remove;
  
  private void 
  computePossibleActions() 
  {
    start = stop = remove = false;
    
	List	items = getSelectedItems();
	
    if (items.size() > 0) {
    
  	  PluginInterface pi = azureus_core.getPluginManager().getDefaultPluginInterface();

  	  org.gudy.azureus2.plugins.download.DownloadManager	dm 		= pi.getDownloadManager();

      remove = true;
      
      for (int i=0; i < items.size(); i++){        
       
    	ShareItem	item = (ShareItem)items.get(i);
        
        try{
    		Torrent	t = item.getTorrent();
    		       		
    		Download	download = dm.getDownload( t );
    		
    		if ( download == null ){
    			    			
    			continue;
    		}
    		
    		int	dl_state = download.getState();
    		
    		if ( 	dl_state == Download.ST_ERROR ){
    			
    		}else if ( dl_state != Download.ST_STOPPED ){
    			
    			stop = true;
    			
    		}else{
    			
    			start = true;
    		}
    	}catch( Throwable e ){
    		
    		Debug.printStackTrace(e);
    	}
      }
    }
  }
  
  public boolean isEnabled(String itemKey) {
    if(itemKey.equals("start"))
      return start;
    if(itemKey.equals("stop"))
      return stop;
    if(itemKey.equals("remove"))
      return remove;
    return false;
  }
  

  public void itemActivated(String itemKey) {
    if(itemKey.equals("remove")){
      removeSelectedShares();
      return;
    }else if ( itemKey.equals( "stop" )){
    	stopSelectedShares();
    }else if ( itemKey.equals( "start" )){
    	startSelectedShares();
    }
  }
  
  private List
  getSelectedItems()
  {
	  Object[] shares = tv.getSelectedDataSources();
	    
	  List	items = new ArrayList();
	  
	  if ( shares.length > 0 ){
	      		    
	      for (int i=0; i < shares.length; i++){        
	       
	        ShareResource	share = (ShareResource)shares[i];
	       
	        int	type = share.getType();
	        	        
	        if ( type == ShareResource.ST_DIR ){
	        	
	        	ShareResourceDir	sr = (ShareResourceDir)share;
	        	
	        	items.add( sr.getItem());
	        	
	        }else if ( type == ShareResource.ST_FILE ){
	        	
	        	ShareResourceFile	sr = (ShareResourceFile)share;
	        	
	        	items.add( sr.getItem());
	        	
	        }else{
	        	
	        	ShareResourceDirContents	cont = (ShareResourceDirContents)share;
	        	
	        	List	entries = new ArrayList();
	        	
	        	getEntries( entries, cont );
	        	
	        	for (int j=0;j<entries.size();j++){
	        		
	    	        share = (ShareResource)entries.get(j);
	    		       
	    	        type = share.getType();
	    	            	        
	    	        if ( type == ShareResource.ST_DIR ){
	    	        	
	    	        	ShareResourceDir	sr = (ShareResourceDir)share;
	    	        	
	    	        	items.add( sr.getItem());
	    	        	
	    	        }else if ( type == ShareResource.ST_FILE ){
	    	        	
	    	        	ShareResourceFile	sr = (ShareResourceFile)share;
	    	        	
	    	        	items.add( sr.getItem());
	    	        }
	        	}
	        }
	      }
	  }
	  
	  return( items );
  }
  
  private void
  getEntries(
	List						entries,
	ShareResourceDirContents	cont )
  {
	  ShareResource[]	kids = cont.getChildren();
	  
	  for ( int i=0;i<kids.length;i++){
		  
		  ShareResource	share = kids[i];
		  
		  int	type  = share.getType();
		  
		  if ( type == ShareResource.ST_DIR_CONTENTS ){
			  
			  getEntries( entries, (ShareResourceDirContents)share );
			  
		  }else{
	
			  entries.add( share );
		  }
	  }
  }
  
  private void 
  startStopSelectedShares(
	boolean	do_stop )
  {
	  List	items = getSelectedItems();
	
	  PluginInterface pi = azureus_core.getPluginManager().getDefaultPluginInterface();
	    
	  org.gudy.azureus2.plugins.download.DownloadManager	dm 		= pi.getDownloadManager();
	    
	  Tracker			tracker = pi.getTracker();
	    

      for (int i=0;i<items.size();i++){
    	  
    	  ShareItem	item = (ShareItem)items.get(i);
        
        	try{
        		Torrent	t = item.getTorrent();
        		
        		TrackerTorrent	tracker_torrent = tracker.getTorrent( t );
        		
        		Download	download = dm.getDownload( t );
        		
        		if ( tracker_torrent == null || download == null ){
        				        			
        			continue;
        		}
        		
        		int	dl_state = download.getState();
        		
        		if ( 	dl_state == Download.ST_ERROR ){
        			
        		}else if ( dl_state != Download.ST_STOPPED ){
        			
        			if ( do_stop ){
        				
        				try{
        					download.stop();
        				}catch( Throwable e ){
        				}
        				
        				try{
        					tracker_torrent.stop();
        				}catch( Throwable e ){
        				}
        			}
        			
        		}else{
        			
        			if ( !do_stop ){
        				
        				try{
        					download.restart();
        				}catch( Throwable e ){
        				}
        				
        				try{
        					tracker_torrent.start();
        				}catch( Throwable e ){
        				}
        			}
        		}
        	}catch( Throwable e ){
        		
        		Debug.printStackTrace(e);
        	}
        }
  }
  
  private void 
  startSelectedShares()
  {  
	  startStopSelectedShares( false );
  }
  
  private void 
  stopSelectedShares()
  {
	  startStopSelectedShares( true );
  }
  
  private void 
  removeSelectedShares()
  {
	stopSelectedShares();
    Object[] shares = tv.getSelectedDataSources();
    for (int i = 0; i < shares.length; i++) {
    	try{
    		((ShareResource)shares[i]).delete();
    		
    	}catch( Throwable e ){
    		
    	  Alerts.showErrorMessageBoxUsingResourceString(
						new Object[] { shares[i] },
    	  		"globalmanager.download.remove.veto", e );
    	}
    }
  }

	public void addThisColumnSubMenu(String columnName, Menu menuThisColumn) {
		// TODO Auto-generated method stub
		
	}
}
