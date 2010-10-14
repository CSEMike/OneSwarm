/*
 * Created on Sep 13, 2004
 * Created by Olivier Chalouhi
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
package org.gudy.azureus2.ui.swt.views.stats;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.AbstractIView;
import org.gudy.azureus2.ui.swt.views.IView;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;

/**
 * 
 */
public class StatsView extends AbstractIView {
  
  GlobalManager manager;
  AzureusCore core;
  
  TabFolder folder;
  
  TabItem itemActivity;
  TabItem itemStats;
  TabItem itemCache;
  TabItem[] itemDHTs;
  TabItem itemVivaldi;
  
  ActivityView viewActivity;
  TransferStatsView viewStats;
  CacheView viewCache;
  DHTView[] viewDHTs;
  IView viewVivaldi;
  UpdateThread updateThread;
  
  public StatsView(GlobalManager manager,AzureusCore core) {
   this.manager = manager;
   this.core = core;
  }
  
  private class UpdateThread extends Thread {
    boolean bContinue;
    
		public UpdateThread() {
			super("StatsView Update Thread");
		}
    
		public void run() {
			bContinue = true;
			while(bContinue) {  
				try {

					viewActivity.periodicUpdate();
					viewCache.periodicUpdate(); 
					viewStats.periodicUpdate();

					for (int i=0;i<viewDHTs.length;i++){
						viewDHTs[i].periodicUpdate();
					}
				} catch(Exception e) {
					Debug.printStackTrace( e );  
				}
				try{
					Thread.sleep(1000);
				}catch( Throwable e ){

					Debug.out( e );
					break;
				}
			}
		}
    
    public void stopIt() {
      bContinue = false;
    }
  }
  
  public void initialize(Composite composite) {
    folder = new TabFolder(composite, SWT.LEFT);
    folder.setBackground(Colors.background);
    
    List	dhts = new ArrayList();

    dhts.add( new DHTView( DHTView.DHT_TYPE_MAIN ));  

    if ( NetworkAdmin.getSingleton().hasIPV6Potential()){
  
    	dhts.add(  new DHTView( DHTView.DHT_TYPE_MAIN_V6 ));
    }
    
    if( Constants.isCVSVersion()){
    	
       	dhts.add(  new DHTView( DHTView.DHT_TYPE_CVS ));
    }
    
    viewDHTs = new DHTView[dhts.size()];
    
    dhts.toArray( viewDHTs );
    
    itemActivity = new TabItem(folder, SWT.NULL);
    itemStats = new TabItem(folder, SWT.NULL);
    itemCache  = new TabItem(folder, SWT.NULL);
    
    itemDHTs = new TabItem[viewDHTs.length];
    
    for (int i=0;i<itemDHTs.length;i++){
    	itemDHTs[i] = new TabItem(folder, SWT.NULL);
    }
  
    itemVivaldi = new TabItem(folder,SWT.NULL);

    viewActivity = new ActivityView(manager);
    viewStats = new TransferStatsView(manager,core);
    viewCache = new CacheView();
    
 
    
    
    viewVivaldi = new VivaldiView();
    
    Messages.setLanguageText(itemActivity, viewActivity.getData());
    Messages.setLanguageText(itemStats, viewStats.getData());
    Messages.setLanguageText(itemCache, viewCache.getData());
    
    for (int i=0;i<viewDHTs.length;i++){
    	Messages.setLanguageText(itemDHTs[i], viewDHTs[i].getData());
    }
       
    Messages.setLanguageText(itemVivaldi, viewVivaldi.getData());
    
    TabItem items[] = {itemActivity};
    folder.setSelection(items);
    
    viewActivity.initialize(folder);
    itemActivity.setControl(viewActivity.getComposite());
    
    viewStats.initialize(folder);
    itemStats.setControl(viewStats.getComposite());
    
    viewCache.initialize(folder);
    itemCache.setControl(viewCache.getComposite());
    
    for (int i=0;i<viewDHTs.length;i++){
    	viewDHTs[i].initialize(folder);
    	itemDHTs[i].setControl(viewDHTs[i].getComposite());
    }
    
    viewVivaldi.initialize(folder);
    itemVivaldi.setControl(viewVivaldi.getComposite());
    
    folder.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) {
        refresh();
      }
      public void widgetDefaultSelected(SelectionEvent e) {
      }
    });
    
    
    refresh();
    viewActivity.getComposite().layout(true);
    viewVivaldi.getComposite().layout(true);
    
    updateThread = new UpdateThread(); 
    updateThread.setDaemon(true);
    updateThread.start();
  }
  
  public void refresh() {
    if(getComposite() == null || getComposite().isDisposed())
      return;

    try {
      int index = folder.getSelectionIndex();
      
      if( index == 0 ) {
        if (viewActivity != null && !itemActivity.isDisposed())   viewActivity.refresh();
        return;
      }
      
      if( index == 1 ) {
        if (viewStats != null && !itemStats.isDisposed())  viewStats.refresh();
        return;
      }
      
      if( index == 2 ) {
        if (viewCache != null && !itemCache.isDisposed())  viewCache.refresh();
        return;
      }
      
      if ( index-3 < viewDHTs.length ){
    	  if ( !itemDHTs[index-3].isDisposed()){
    		  viewDHTs[index-3].refresh();
    	  }
    	  return;
      }
      
      if (viewVivaldi != null && !itemVivaldi.isDisposed())  viewVivaldi.refresh();
  
    } catch (Exception e) {
    	Debug.printStackTrace( e );
    }
  }
  
  public void
  showTransfers()
  {
	  folder.setSelection( new TabItem[]{ itemStats });
  }
  
  public void
  showDHT()
  {
	  folder.setSelection( new TabItem[]{ itemDHTs[0] });
  }
  
  public Composite getComposite() {
    return folder;
  }

  public String getFullTitle() {
    return MessageText.getString("Stats.title.full"); //$NON-NLS-1$
  }
  
  public void delete() {
    updateThread.stopIt();

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
    
    viewActivity.delete();
    viewStats.delete();
    viewCache.delete();
    
    for (int i=0;i<viewDHTs.length;i++){
    	viewDHTs[i].delete();
    }

    if(! folder.isDisposed()) {
      Utils.disposeComposite(folder);
    }
  }
}
