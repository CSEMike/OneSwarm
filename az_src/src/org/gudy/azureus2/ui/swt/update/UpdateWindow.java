/*
 * Created on 7 mai 2004
 * Created by Olivier Chalouhi
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
package org.gudy.azureus2.ui.swt.update;

import java.io.InputStream;
import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.LinkArea;
import org.gudy.azureus2.ui.swt.components.StringListChooser;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.mainwindow.MainStatusBar;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

import org.gudy.azureus2.plugins.update.Update;
import org.gudy.azureus2.plugins.update.UpdateCheckInstance;
import org.gudy.azureus2.plugins.update.UpdateManagerDecisionListener;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener;

/**
 * @author Olivier Chalouhi
 *
 */
public class 
UpdateWindow
	implements 	ResourceDownloaderListener{
  
  private UpdateMonitor			update_monitor;
  private UpdateCheckInstance	check_instance;
  private int					check_type;
  
  Display display;  
  
  Shell updateWindow;
  Table table;
  LinkArea link_area;
  ProgressBar progress;
  Label status;
  
  Button btnOk;
  Listener lOk;
  
  Button btnCancel;
  Listener lCancel;
  
  
  
  boolean	hasMandatoryUpdates;
  boolean 	restartRequired;
  
  private long totalDownloadSize;
  private List downloaders;
  private Iterator iterDownloaders;
	private Browser browser;
  
  private static final int COL_NAME = 0;
  private static final int COL_VERSION = 1;
  private static final int COL_SIZE = 2;
  
  
  public 
  UpdateWindow(
	UpdateMonitor		_update_monitor,
  	AzureusCore			_azureus_core,
  	UpdateCheckInstance	_check_instance )
  {
	update_monitor	= _update_monitor;
  	check_instance 	= _check_instance;
  	
  	check_type = check_instance.getType();
  	
  	check_instance.addDecisionListener(
	  		new UpdateManagerDecisionListener()
	  		{
	  			public Object
	  			decide(
	  				Update		update,
	  				int			decision_type,
	  				String		decision_name,
	  				String		decision_description,
	  				Object		decision_data )
	  			{
	  				if ( decision_type == UpdateManagerDecisionListener.DT_STRING_ARRAY_TO_STRING ){
	  					
	  					String[]	options = (String[])decision_data;
  					
	  					Shell	shell = updateWindow;
	  					
	  					if ( shell == null ){
	  						
	  						Debug.out( "Shell doesn't exist" );
	  						
	  						return( null );
	  					}
	  					
	  					StringListChooser chooser = new StringListChooser( shell );
	  					
	  					chooser.setTitle( decision_name );
	  					chooser.setText( decision_description );
	  					
	  					for (int i=0;i<options.length;i++){
	  						
	  						chooser.addOption( options[i] );
	  					}
	  					
	  					String	result = chooser.open();
	  					
	  					return( result );
	  				}
	  				
	  				return( null );
	  			}
	  		});
  	
    this.updateWindow = null;
    this.display = SWTThread.getInstance().getDisplay();
    
    Utils.execSWTThreadWithBool("UpdateWindow", new AERunnableBoolean() {
			public boolean runSupport() {
				buildWindow();
				return true;
			}
		});
  }
  
  //The Shell creation process
  public void buildWindow() {
    if(display == null || display.isDisposed())
      return;

    Utils.waitForModals();
    
    //Do not use ~SWT.CLOSE cause on some linux/GTK platform it
    //forces the window to be only 200x200
    //catch close event instead, and never do it
		updateWindow = ShellFactory.createMainShell(SWT.DIALOG_TRIM | SWT.RESIZE);
    
    updateWindow.addListener(SWT.Close,new Listener() {
      public void handleEvent(Event e) {
        dispose();
      }
    });
    
    Utils.setShellIcon(updateWindow);
    
    String	res_prefix = "swt.";
    
    if ( check_type == UpdateCheckInstance.UCI_INSTALL ){
    	
    	res_prefix += "install.window";
    	
    }else if (check_type == UpdateCheckInstance.UCI_UNINSTALL ){
    	
    	res_prefix += "uninstall.window";
    	
    }else{
    	
    	res_prefix = "UpdateWindow";
    }
    
    Messages.setLanguageText(updateWindow, res_prefix + ".title");
    
    FormLayout layout = new FormLayout();
    try {
      layout.spacing = 5;
    } catch (NoSuchFieldError e) { /* Pre SWT 3.0 */ }
    layout.marginHeight = 10;
    layout.marginWidth = 10;
    FormData formData;
    updateWindow.setLayout(layout);
    
    Label lHeaderText = new Label(updateWindow,SWT.WRAP);
    Messages.setLanguageText(lHeaderText,res_prefix + ".header");
    formData = new FormData();
    formData.left = new FormAttachment(0,0);
    formData.right = new FormAttachment(100,0);
    formData.top = new FormAttachment(0,0);
    lHeaderText.setLayoutData(formData);
    
    SashForm sash = new SashForm(updateWindow,SWT.VERTICAL);
       
    table = new Table(sash,SWT.CHECK | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
    String[] names = {"name" , "version" , "size"};
    int[] sizes = {350,100,100};
    for(int i = 0 ; i < names.length ; i++) {
      TableColumn column = new TableColumn(table, i == 0 ? SWT.LEFT : SWT.RIGHT);
      Messages.setLanguageText(column,"UpdateWindow.columns." + names[i]);
      column.setWidth(sizes[i]);
    }
    table.setHeaderVisible(true);    

    table.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
      	rowSelected();
      }
    });
    
    Composite cInfoArea = new Composite(sash, SWT.NONE);
    cInfoArea.setLayout(new FormLayout());
    
    link_area = new LinkArea(cInfoArea);
    FormData fd = new FormData();
    fd.top = new FormAttachment(0, 0);
    fd.bottom = new FormAttachment(100, 0);
    fd.right = new FormAttachment(100, 0);
    fd.left = new FormAttachment(0, 0);
    link_area.getComponent().setLayoutData(fd);
    
    try {
    	browser = Utils.createSafeBrowser(cInfoArea, SWT.BORDER);
    	if (browser != null) {
        fd = new FormData();
        fd.top = new FormAttachment(0, 0);
        fd.bottom = new FormAttachment(100, 0);
        fd.right = new FormAttachment(100, 0);
        fd.left = new FormAttachment(0, 0);
        browser.setLayoutData(fd);
    	}
    } catch (Throwable t) {
    }

    progress = new ProgressBar(updateWindow,SWT.NULL);
    progress.setMinimum(0);
    progress.setMaximum(100);
    progress.setSelection(0);
    
    status = new Label(updateWindow,SWT.NULL);
    
 
    Composite cButtons = new Composite(updateWindow, SWT.NONE);
		FillLayout fl = new FillLayout(SWT.HORIZONTAL);
		fl.spacing = 3;
		cButtons.setLayout(fl);
    
    btnOk = new Button(cButtons,SWT.PUSH);
    Messages.setLanguageText(btnOk,res_prefix + ".ok" );
    
    updateWindow.setDefaultButton( btnOk );
    lOk = new Listener() {
      public void handleEvent(Event e) {
        update();
      }
    };
    
    btnOk.addListener(SWT.Selection, lOk);
    btnOk.setEnabled( false );
    
    btnCancel = new Button(cButtons,SWT.PUSH);
    
    Messages.setLanguageText(btnCancel,"UpdateWindow.cancel");
    
    lCancel = new Listener() {
	      public void handleEvent(Event e) {
	        dispose();
	       	check_instance.cancel();
	      }
	   };
    btnCancel.addListener(SWT.Selection,lCancel);
    
    updateWindow.addListener(SWT.Traverse, new Listener() {	
		public void handleEvent(Event e) {
			if ( e.character == SWT.ESC){
			      dispose();
			      check_instance.cancel();			
			 }
		}
    });
    
    formData = new FormData();
    formData.left = new FormAttachment(0,0);
    formData.right = new FormAttachment(100,0);
    formData.top = new FormAttachment(lHeaderText);
    formData.bottom = new FormAttachment(progress);
    sash.setLayoutData(formData);
    
    formData = new FormData();
    formData.left = new FormAttachment(0,0);
    formData.right = new FormAttachment(100,0);
    formData.bottom = new FormAttachment(status);
    progress.setLayoutData(formData);
    
    formData = new FormData();
    formData.left = new FormAttachment(0,0);
    formData.right = new FormAttachment(100,0);
    formData.bottom = new FormAttachment(cButtons);
    status.setLayoutData(formData);
    
    formData = new FormData();
    formData.right = new FormAttachment(100,0);
    formData.bottom = new FormAttachment(100,0);
    cButtons.setLayoutData(formData);
    
    sash.setWeights(new int[] { 25, 75 });
    
    updateWindow.setSize(600,450);
    //updateWindow.open();
  }
  
  protected void
  rowSelected()
  {
    checkMandatory();
    checkRestartNeeded();
    TableItem[] items = table.getSelection();
    if(items.length == 0) return;
    Update update = (Update) items[0].getData();
    
    String desciptionURL = update.getDesciptionURL();
    if (desciptionURL != null && browser != null) {
    	browser.setUrl(desciptionURL);
    	browser.setVisible(true);
    	link_area.getComponent().setVisible(false);
    } else {
    	if (browser != null) {
    		browser.setVisible(false);
    	}
    	link_area.getComponent().setVisible(true);
    
      String[] descriptions = update.getDescription();
      
      link_area.reset();
      
      link_area.setRelativeURLBase( update.getRelativeURLBase());
      
      for(int i = 0 ; i < descriptions.length ; i++) {
  
      	link_area.addLine( descriptions[i] );
      }
    }
  }
  
  public Shell
  getShell()
  {
	  return( updateWindow );
  }
  
  public void dispose() {
  	Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
		    updateWindow.dispose();
      	UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
      	if (functionsSWT != null) {
      		MainStatusBar mainStatusBar = functionsSWT.getMainStatusBar();
      		if (mainStatusBar != null) {
      			mainStatusBar.setUpdateNeeded(null);
      		}
      	}
			}
		});
  }
  
  public void addUpdate(final Update update) {
    if(display == null || display.isDisposed())
      return;
  
    if ( update.isMandatory()){
    	
    	hasMandatoryUpdates = true;
    }
    
    display.asyncExec(new AERunnable() {
      public void runSupport() {
        if(table == null || table.isDisposed())
          return;
        
        final TableItem item = new TableItem(table,SWT.NULL);
        item.setData(update);
        item.setText(COL_NAME,update.getName()==null?"Unknown":update.getName());  
        item.setText(COL_VERSION,update.getNewVersion()==null?"Unknown":update.getNewVersion());
        ResourceDownloader[] rds = update.getDownloaders();
        long totalLength = 0;
        for(int i = 0 ; i < rds.length ; i++) {
          try {
            totalLength += rds[i].getSize();
          } catch(Exception e) {
          }
        }                
        
        item.setText(COL_SIZE,DisplayFormatters.formatByteCountToBase10KBEtc(totalLength));                
        
        item.setChecked(true);
        
        	// select first entry
        
        if ( table.getItemCount() == 1 ){
        	
        	table.select(0);
        	
        	rowSelected();	// don't seem to be getting the selection event, do it explicitly
        }
        
        checkRestartNeeded();
        
        if( 	COConfigurationManager.getBooleanParameter("update.opendialog")|| 
        		check_instance.getType() != UpdateCheckInstance.UCI_UPDATE ) {
        	
        	show();
        	
        }else{
        	
        	UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
        	if (functionsSWT != null) {
        		MainStatusBar mainStatusBar = functionsSWT.getMainStatusBar();
        		if (mainStatusBar != null) {
        			mainStatusBar.setUpdateNeeded(UpdateWindow.this);
        		}
        	}
        }
      }
    });
  }
  
  protected void
  updateAdditionComplete()
  {
    if(display == null || display.isDisposed())
        return;
    
      display.asyncExec(new AERunnable() {
        public void runSupport() {
          if(btnOk == null || btnOk.isDisposed())
            return;
          
          btnOk.setEnabled(true);
        }
      });
  }
  
  public void show() {
    if(updateWindow == null || updateWindow.isDisposed()) {
      return;
    }
    
    Utils.centreWindow( updateWindow );
    updateWindow.open();
    updateWindow.forceActive();       
  }
  
  
  private void checkMandatory() {
    TableItem[] items = table.getItems();
    for(int i = 0 ; i < items.length ; i++) {
      Update update = (Update) items[i].getData();
      if(update.isMandatory()) items[i].setChecked(true);
    }
  }
  
  private void checkRestartNeeded() {  
    restartRequired = false;
    boolean	restartMaybeRequired = false;
    TableItem[] items = table.getItems();
    for(int i = 0 ; i < items.length ; i++) {
      if(! items[i].getChecked()) continue;
      Update update = (Update) items[i].getData();
      int required = update.getRestartRequired();
      if((required == Update.RESTART_REQUIRED_MAYBE)){
      	restartMaybeRequired = true;
      }else if ( required == Update.RESTART_REQUIRED_YES ){
      	restartRequired = true;
      }
    }
    if(restartRequired) {
        status.setText(MessageText.getString("UpdateWindow.status.restartNeeded"));
    }else if(restartMaybeRequired) {
        status.setText(MessageText.getString("UpdateWindow.status.restartMaybeNeeded"));
    }else{
      status.setText("");
    }
  }
  
  private void update() {
    btnOk.setEnabled(false);    
    Messages.setLanguageText(btnCancel,"UpdateWindow.cancel");
    table.setEnabled(false);
    
    link_area.reset();
    if (browser != null) {
    	browser.setVisible(false);
    }
  	link_area.getComponent().setVisible(true);
    
    TableItem[] items = table.getItems();
    
    totalDownloadSize = 0;   
    downloaders = new ArrayList();
    
    for(int i = 0 ; i < items.length ; i++) {
      if(! items[i].getChecked()) continue;
      
      Update update = (Update) items[i].getData();
      ResourceDownloader[] rds = update.getDownloaders();
      for(int j = 0 ; j < rds.length ; j++) {
        downloaders.add(rds[j]);        
        try {
          totalDownloadSize += rds[j].getSize();
        } catch (Exception e) {
          link_area.addLine(MessageText.getString("UpdateWindow.no_size") + rds[j].getName());
        }        
      }
    }
    downloadersToData = new HashMap();
    iterDownloaders = downloaders.iterator();
    nextUpdate();
  }
  
  private void nextUpdate() {
    if(iterDownloaders.hasNext()) {
      ResourceDownloader downloader = (ResourceDownloader) iterDownloaders.next();
      downloader.addListener(this);
      downloader.asyncDownload();
    } else {
      switchToRestart();      
    }
  }
  
  private void switchToRestart() {
    if(display == null || display.isDisposed())
      return;
    
    display.asyncExec(new AERunnable(){
      public void runSupport() {
    	  
		Boolean b = (Boolean)check_instance.getProperty( UpdateCheckInstance.PT_CLOSE_OR_RESTART_ALREADY_IN_PROGRESS );
			
		if ( b != null && b ){
			
			finishUpdate(false, true);
			
			return;
		}
		
      	checkRestartNeeded();	// gotta recheck coz a maybe might have got to yes
        progress.setSelection(100);
        status.setText(MessageText.getString("UpdateWindow.status.done"));
        btnOk.removeListener(SWT.Selection,lOk);
        btnOk.setEnabled(true);
        btnOk.addListener(SWT.Selection,new Listener() {
          public void handleEvent(Event e) {
            finishUpdate(true, false);
          }
        });
        if(restartRequired) {
          Messages.setLanguageText(btnOk,"UpdateWindow.restart");
          btnCancel.removeListener(SWT.Selection,lCancel);
          Messages.setLanguageText(btnCancel,"UpdateWindow.restartLater");
          btnCancel.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
              finishUpdate(false,false);
            }
          });
          updateWindow.layout();
        } else {
          Messages.setLanguageText(btnOk,"UpdateWindow.close");
          btnCancel.setEnabled(false);
          updateWindow.layout();
        }
      }
    });
  }
  
  public void reportPercentComplete(ResourceDownloader downloader,
      int percentage) {
    setProgressSelection(percentage);   
  }
  
  public void
  reportAmountComplete(
	 ResourceDownloader	downloader,
	 long				amount )
  {  
  }
  
  private void setProgressSelection(final int percent) {
    if(display == null || display.isDisposed())
      return;
    
    display.asyncExec(new AERunnable() {
      public void runSupport() {
        if(progress != null && !progress.isDisposed())
        progress.setSelection(percent);
      }
    });
  }
  
  private Map downloadersToData;
  
  public boolean completed(ResourceDownloader downloader, InputStream data) {
    downloadersToData.put(downloader,data);
    downloader.removeListener(this);
    setProgressSelection(0);
    nextUpdate();
    return true;
  }
  
  public void failed(ResourceDownloader downloader,
      ResourceDownloaderException e) {
    downloader.removeListener(this);
    setStatusText(MessageText.getString("UpdateWindow.status.failed"));
    
    String	msg = downloader.getName() + " : " + e;
    
    if ( e.getCause() != null ){
    	
    	msg += " [" + e.getCause() + "]";
    }
    
    appendDetails(msg);
  }
  
  public void reportActivity(ResourceDownloader downloader, final String activity) {
    setStatusText(activity.trim());
    appendDetails(activity);
  }
  
  private void setStatusText(final String text) {
    if(display == null || display.isDisposed())
      return;
    
    display.asyncExec(new AERunnable(){
      public void runSupport() {
        if(status != null && !status.isDisposed())
          status.setText(text);
      }
    });  
  }
  
  private void appendDetails(final String text) {
    if(display == null || display.isDisposed())
      return;
    
    display.asyncExec(new AERunnable(){
      public void runSupport() {
    	  link_area.addLine( text );
      }
    });  
  }
  
  
  private void finishUpdate(boolean restartNow, boolean just_close ) {
    //When completing, remove the link in mainWindow :
  	UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
  	if (functionsSWT != null) {
  		MainStatusBar mainStatusBar = functionsSWT.getMainStatusBar();
  		if (mainStatusBar != null) {
  			mainStatusBar.setUpdateNeeded(null);
  		}
  	}
    
  	boolean bDisposeUpdateWindow = true;

  	if ( !just_close ){
	    //If restart is required, then restart
	    if( restartRequired && restartNow) {
	    	// this HAS to be done this way around else the restart inherits
	    	// the 6880 port listen. However, this is a general problem....
	    	UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
	    	if (uiFunctions != null && uiFunctions.dispose(true, false)) {
	   			bDisposeUpdateWindow = false;
				}
	    }else if ( hasMandatoryUpdates && !restartRequired ){
	    	
	    		// run a further update check as we can immediately install non-mandatory updates now
	    	
	    	update_monitor.requestRecheck();
	    }
  	}

    if (bDisposeUpdateWindow) {
      updateWindow.dispose();
    }
  }
  
  protected boolean
  isDisposed()
  {
  	return( display == null || display.isDisposed() || updateWindow == null || updateWindow.isDisposed());
  }
}
