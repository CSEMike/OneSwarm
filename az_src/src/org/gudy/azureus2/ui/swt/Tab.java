/*
 * Created on 29 juin 2003
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.plugins.PluginView;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.plugins.UISWTPluginView;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.views.*;

import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

/**
 * @author Olivier
 * @author James Yeh Added Add/Remove event listeners
 */
public class Tab {

  private static HashMap 	tabs;
  private static AEMonitor  class_mon 	= new AEMonitor( "Tab:class" );

  private static boolean useCustomTab;

  private static Composite _folder;


  private Composite folder;


  private Item tabItem;
  private static boolean eventCloseAllowed = true;
  private static Item selectedItem = null;


  private IView view;

   // events
   private static List tabAddListeners;
   private static List tabRemoveListeners;
	private static MainWindow mainwindow;

  static {
    tabs = new HashMap();
    tabAddListeners = new LinkedList();
    tabRemoveListeners = new LinkedList();
  }


  public Item getTabItem() {
    return tabItem;
  }

  public Tab(IView _view) {
  	this(_view, true);
  }

  public Tab(IView _view, boolean bFocus) {
    this.view = _view;
    this.folder = _folder;

    if (folder.isDisposed()) {
    	return;
    }
    
    final MouseAdapter mouseListener;
    
    if (useCustomTab) {
    	CTabFolder tabFolder = (CTabFolder) folder;
      tabItem = new CTabItem(tabFolder, SWT.NULL,
					(_view instanceof MyTorrentsSuperView) ? 0 : tabFolder.getItemCount());

	    folder.addMouseListener(mouseListener = new MouseAdapter() {
	      public void mouseDown(MouseEvent arg0) {
	        if(arg0.button == 2) {
	          if(eventCloseAllowed) { 
	            Rectangle rectangle =((CTabItem)tabItem).getBounds(); 
	            if(rectangle.contains(arg0.x, arg0.y)) {
	              eventCloseAllowed = false;
	              selectedItem = null;
	              //folder.removeMouseListener(this);
	              closed(tabItem);
	            }
	          }
	        } else {          
	          selectedItem = ((CTabFolder) folder).getSelection();	         
	        }
	      }
	      
	      public void mouseUp(MouseEvent arg0) {
	        eventCloseAllowed = true;
	        if(selectedItem != null) {
	          if(_folder instanceof CTabFolder)
	            ((CTabFolder) _folder).setSelection((CTabItem)selectedItem);	          
	        }
	      }
	    });
    }
    else {
    	mouseListener = null;
    	TabFolder tabFolder = (TabFolder) folder;
      tabItem = new TabItem(tabFolder, SWT.NULL,
					(_view instanceof MyTorrentsSuperView) ? 0 : tabFolder.getItemCount());
    }
    
    Listener activateListener = new Listener() {
			public void handleEvent(Event event) {
				IView view = null;
				Composite parent = (Composite)event.widget;
				IView oldView = getView(selectedItem);
				if (oldView instanceof IViewExtension) {
					((IViewExtension)oldView).viewDeactivated();
				}
				
				while (parent != null && !parent.isDisposed() && view == null) {
					if (parent instanceof CTabFolder) {
						CTabFolder folder = (CTabFolder)parent;
						selectedItem = folder.getSelection();
						view = getView(selectedItem);
					} else if (parent instanceof TabFolder) {
						TabFolder folder = (TabFolder)parent;
						TabItem[] selection = folder.getSelection();
						if (selection.length > 0) {
							selectedItem = selection[0];
							view = getView(selectedItem);
						}
					}
					
					if (view == null)
						parent = parent.getParent();
				}
				
				if (view != null) {
					if (view instanceof IViewExtension) {
						((IViewExtension)view).viewActivated();
					}
					view.refresh();
				}
			}
		};

    tabs.put(tabItem, view);

		try {
			// Always create a composite around the IView, because a lot of them
			// assume that their parent is of GridLayout layout.
			final Composite tabArea = new Composite(folder, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			tabArea.setLayout(layout);

			_view.initialize(tabArea);
			tabItem.setText(escapeAccelerators(view.getShortTitle()));
			
			Composite viewComposite = _view.getComposite();
			if (viewComposite != null && !viewComposite.isDisposed()) {
				viewComposite.addListener(SWT.Activate, activateListener);

				// make sure the view's layout data is of GridLayoutData
				if ((tabArea.getLayout() instanceof GridLayout)
						&& !(viewComposite.getLayoutData() instanceof GridData)) {
					viewComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
				}

				if (viewComposite != tabArea) {
  				viewComposite.addDisposeListener(new DisposeListener() {
  					boolean alreadyHere = false;
  					public void widgetDisposed(DisposeEvent e) {
  						if (alreadyHere) {
  							return;
  						}
  						alreadyHere = true;
  						Utils.disposeComposite(tabArea);
  					}
  				});
				}
			}

			if (useCustomTab) {
				((CTabItem) tabItem).setControl(tabArea);
// Disabled for SWT 3.2RC5.. CTabItem tooltip doesn't always disappear
//				((CTabItem) tabItem).setToolTipText(view.getFullTitle());
				if (bFocus)
					((CTabFolder) folder).setSelection((CTabItem) tabItem);
			} else {
				((TabItem) tabItem).setControl(tabArea);
				((TabItem) tabItem).setToolTipText(view.getFullTitle());
				TabItem items[] = { (TabItem) tabItem };
				if (bFocus)
					((TabFolder) folder).setSelection(items);
			}
		} catch (Exception e) {
			tabs.remove(tabItem);
			Debug.printStackTrace(e);
		}
    
    if (bFocus) {
    	UIFunctionsSWT uif = UIFunctionsManagerSWT.getUIFunctionsSWT();
    	if (uif != null) {
    		uif.refreshIconBar();
    		uif.refreshTorrentMenu();
    	}
    	selectedItem = tabItem;
    }

    // events
    notifyListeners(tabAddListeners, tabItem);
    tabItem.addDisposeListener(new DisposeListener() {
        public void widgetDisposed(DisposeEvent event) {
        	folder.removeMouseListener(mouseListener);
            notifyListeners(tabRemoveListeners, tabItem);
        }
    });
//    System.out.println("selected: "+selectedItem.getText());
  }

  //public static IView getView(TabItem item) {
  //public static IView getView(CTabItem item) {
  public static IView getView(Item item) {
    return (IView) tabs.get(item);
  }

  public IView
  getView()
  {
	  return( view );
  }
  
  public static Item
  getTab(
		IView	view )
  {
	   try{
		   class_mon.enter();
	    	
		   Iterator iter = tabs.keySet().iterator();
		   
		   while( iter.hasNext()){
			 
			   Item item = (Item) iter.next();
			   
			   IView this_view = (IView) tabs.get(item); 
			   
			   if ( this_view == view ){
				   
				   return( item );
			   }
		   }
		   
		   return( null );
		   
	   }finally{
		   
		   class_mon.exit();
	   }
  }
  
  public static Item[] getAllTabs() {
		try {
			class_mon.enter();

			Item[] tabItems = new Item[tabs.size()];
			if (tabItems.length > 0) {
				tabItems = (Item[]) tabs.keySet().toArray(tabItems);
			}

			return tabItems;
		} finally {

			class_mon.exit();
		}
	}
  
  public static IView[] getAllViews() {
		try {
			class_mon.enter();

			IView[] views = new IView[tabs.size()];
			if (views.length > 0) {
				views = (IView[])tabs.values().toArray(views);
			}
			
			return views;
		} finally {

			class_mon.exit();
		}
	}

  
  public static void refresh() {
    try{
    	class_mon.enter();
    	
      Iterator iter = tabs.keySet().iterator();
      while (iter.hasNext()) {
        //TabItem item = (TabItem) iter.next();
        //CTabItem item = (CTabItem) iter.next();
        Item item = (Item) iter.next();
        IView view = (IView) tabs.get(item);
        try {
          if (item.isDisposed())
            continue;
          String lastTitle = item.getText();
          String newTitle = view.getShortTitle();
          if (lastTitle == null || !lastTitle.equals(newTitle)) {
            item.setText(escapeAccelerators(newTitle));
          }
          if (item instanceof CTabItem) {
// Disabled for SWT 3.2RC5.. CTabItem tooltip doesn't always disappear
//            String lastToolTip = ((CTabItem) item).getToolTipText();
//            String newToolTip = view.getFullTitle();
//            if (lastToolTip == null || !lastToolTip.equals(newToolTip)) {
//              ((CTabItem) item).setToolTipText(newToolTip);
//            }
          }
          else if (item instanceof TabItem) {
            String lastToolTip = ((TabItem) item).getToolTipText();
            String newToolTip = view.getFullTitle() + " " +
						 MessageText.getString("Tab.closeHint");
            if (lastToolTip == null || !lastToolTip.equals(newToolTip)) {
              ((TabItem) item).setToolTipText(newToolTip);
            }
          }
        }
        catch (Exception e){
        	
        	Debug.printStackTrace(e);
        }
      }
    }finally{
    	
    	class_mon.exit();
    }
  }

  public static void 
  updateLanguage() 
  {
  	IView[] views;
  	
    try{
    	class_mon.enter();
      
    	views = (IView[]) tabs.values().toArray(new IView[tabs.size()]);
    	   	
    }finally{
    	
    	class_mon.exit();
    }   
    
    for (int i = 0; i < views.length; i++) {

    	IView view = views[i];
    	
        try {
          view.updateLanguage();
          view.refresh();
        }
        catch (Exception e) {
        	Debug.printStackTrace(e);
        }
    }
  }


  public static void 
  closeAllTabs() 
  {
  	Item[] tab_items;
  	
    try{
    	class_mon.enter();
      
    	tab_items = (Item[]) tabs.keySet().toArray(new Item[tabs.size()]);
    	
    }finally{
    	
    	class_mon.exit();
    }
    
    for (int i = 0; i < tab_items.length; i++) {
    	
        closed(tab_items[i], true);
      }
  }

  public static boolean hasDetails()
  {
      boolean hasDetails = false;
      try
      {
          class_mon.enter();

          Iterator iter = tabs.values().iterator();
          while (iter.hasNext())
          {
              IView view = (IView) iter.next();
              if(view instanceof ManagerView)
              {
                  hasDetails = true;
                  break;
              }
          }
      }
      finally
      {
          class_mon.exit();
      }

      return hasDetails;
  }

  public static void
  closeAllDetails() 
  {
  	Item[] tab_items;
  	
    try{
    	class_mon.enter();
    	
    	tab_items = (Item[]) tabs.keySet().toArray(new Item[tabs.size()]);

    }finally{
    	
    	class_mon.exit();
    }
    
    for (int i = 0; i < tab_items.length; i++) {
        IView view = (IView) tabs.get(tab_items[i]);
        if (view instanceof ManagerView) {
          closed(tab_items[i]);
        }
      }
  }

  public static void closeCurrent() {
    if (_folder == null || _folder.isDisposed())
      return;
    if(_folder instanceof TabFolder) {    
      TabItem[] items =  ((TabFolder)_folder).getSelection();
      if(items.length == 1) {
        closed(items[0]);		
      }
     } else {
       closed(((CTabFolder)_folder).getSelection());
     }
  }

  /**
   * @param selectNext if true, the next tab is selected, else the previous
   *
   * @author Rene Leonhardt
   */
  public static void selectNextTab(boolean selectNext) {
    if (_folder == null || _folder.isDisposed())
      return;
    final int nextOrPrevious = selectNext ? 1 : -1;
    if(_folder instanceof TabFolder) {
      TabFolder tabFolder = (TabFolder)_folder;
      int index = tabFolder.getSelectionIndex() + nextOrPrevious;
      if(index == 0 && selectNext || index == -2 || tabFolder.getItemCount() < 2)
        return;
      if(index == tabFolder.getItemCount())
        index = 0;
      else if(index < 0)
        index = tabFolder.getItemCount() - 1;
      tabFolder.setSelection(index);
    } else {
      CTabFolder tabFolder = (CTabFolder)_folder;
      int index = tabFolder.getSelectionIndex() + nextOrPrevious;
      if(index == 0 && selectNext || index == -2 || tabFolder.getItemCount() < 2)
        return;
      if(index == tabFolder.getItemCount())
        index = 0;
      else if(index < 0)
        index = tabFolder.getItemCount() - 1;
      tabFolder.setSelection(index);
    }
  }

  //public static void setFolder(TabFolder folder) {
  //public static void setFolder(CTabFolder folder) {
  public static void initialize(MainWindow mainwindow, Composite folder) {
    Tab.mainwindow = mainwindow;
		_folder = folder;
  }

  public static boolean 
  closed(Item item) 
  {
  	return closed(item, false);
  }
  
  public static boolean 
  closed(Item item, boolean bForceClose) 
  {
  	if (item == null) {
  		return true;
  	}

    IView view = (IView) tabs.get(item);
    if (!bForceClose && view instanceof UISWTViewImpl) {
    	if (!((UISWTViewImpl)view).requestClose()) {
    		return false;
    	}
    }

    try{
    	class_mon.enter();
    	
    	view = (IView) tabs.remove(item);
    }finally{
    	
    	class_mon.exit();
    }
    
    if (view != null) {
        try {
          if(view instanceof PluginView) {
          	mainwindow.removeActivePluginView(((PluginView)view).getPluginViewName());
          }
          if(view instanceof UISWTPluginView) {
          	mainwindow.removeActivePluginView(((UISWTPluginView)view).getPluginViewName());
          }
          if(view instanceof UISWTView)
          	mainwindow.removeActivePluginView(((UISWTView)view).getViewID());
   
          view.delete();
        } catch (Exception e) {
        	Debug.printStackTrace( e );
        }

        if (view instanceof MyTorrentsSuperView) {
          //TODO : There is a problem here on OSX when using Normal TABS
          /*  org.eclipse.swt.SWTException: Widget is disposed
                at org.eclipse.swt.SWT.error(SWT.java:2691)
                at org.eclipse.swt.SWT.error(SWT.java:2616)
                at org.eclipse.swt.SWT.error(SWT.java:2587)
                at org.eclipse.swt.widgets.Widget.error(Widget.java:546)
                at org.eclipse.swt.widgets.Widget.checkWidget(Widget.java:296)
                at org.eclipse.swt.widgets.Control.setVisible(Control.java:2573)
                at org.eclipse.swt.widgets.TabItem.releaseChild(TabItem.java:180)
                at org.eclipse.swt.widgets.Widget.dispose(Widget.java:480)
                at org.gudy.azureus2.ui.swt.Tab.closed(Tab.java:322)
           */
          //Tried to add a if(! item.isDisposed()) but it's not fixing it
          //Need to investigate...
          item.dispose();
          return true;
        }
        if (view instanceof MyTrackerView) {
          item.dispose();
          return true;
        }
        if (view instanceof MySharesView) {
        	item.dispose();
          return true;
        }
      }
      try {
        /*Control control;
        if(item instanceof CTabItem) {
          control = ((CTabItem)item).getControl();
        } else {
          control = ((TabItem)item).getControl();
        }
        if (control != null && !control.isDisposed())
          control.dispose();
        */
        item.dispose();
      }
      catch (Exception e) {
      	Debug.printStackTrace( e );
      }
      return true;
  }

  public void setFocus() {
    if (folder != null && !folder.isDisposed()) {
      if(useCustomTab) {
        ((CTabFolder)folder).setSelection((CTabItem)tabItem);
      } else {
        TabItem items[] = {(TabItem)tabItem};
        ((TabFolder)folder).setSelection(items);    
      }      
    }
  }

  public void dispose() {
    IView localView = null;
    try{
    	class_mon.enter();
      
      localView = (IView) tabs.get(tabItem);

      if (localView instanceof UISWTViewImpl) {
				if (!((UISWTViewImpl) localView).requestClose())
					return;
			}

      tabs.remove(tabItem);
    }finally{
    
    	class_mon.exit();
    }
    try {
      if (localView != null) {
        if(localView instanceof PluginView) {
        	mainwindow.removeActivePluginView(((PluginView)localView).getPluginViewName());
        }
        if(localView instanceof UISWTPluginView) {
        	mainwindow.removeActivePluginView(((UISWTPluginView)localView).getPluginViewName());
        }

        localView.delete();
      }
      tabItem.dispose();
    }
    catch (Exception e) {}
  }

  public static void addTabAddedListener(Listener listener)
  {
      addListener(tabAddListeners, listener);
  }

  public static void removeTabAddedListener(Listener listener)
  {
      removeListener(tabAddListeners, listener);
  }

  public static void addTabRemovedListener(Listener listener)
  {
      addListener(tabRemoveListeners, listener);
  }

  public static void removeTabRemovedListener(Listener listener)
  {
      removeListener(tabRemoveListeners, listener);
  }

  private static void addListener(List listenerList, Listener listener)
  {
      try
      {
          class_mon.enter();
          listenerList.add(listener);
      }
      finally
      {
          class_mon.exit();
      }
  }

  private static void removeListener(List listenerList, Listener listener)
  {
      try
      {
          class_mon.enter();
          listenerList.remove(listener);
      }
      finally
      {
          class_mon.exit();
      }
  }

  private static void notifyListeners(List listenerList, Item sender)
  {
      try
      {
          class_mon.enter();

          Iterator iter = listenerList.iterator();
          for (int i = 0; i < listenerList.size(); i++)
          {
                ((Listener)iter.next()).handleEvent(getEvent(sender));
          }
      }
      finally
      {
          class_mon.exit();
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
  
  private static Event getEvent(Item sender)
  {
      Event e = new Event();
      e.widget = sender;
      return e;
  }
  
  public void
  generateDiagnostics(
	IndentWriter	writer )
  {
	  view.generateDiagnostics( writer );
  }
  
  public static void setUseCustomTab(boolean newUseCustomTab) {
  	useCustomTab = newUseCustomTab;
  }
}
