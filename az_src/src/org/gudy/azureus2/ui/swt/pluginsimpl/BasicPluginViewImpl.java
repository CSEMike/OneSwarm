/*
 * Created on 27-Apr-2004
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


package org.gudy.azureus2.ui.swt.pluginsimpl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.plugins.ui.components.UIPropertyChangeEvent;
import org.gudy.azureus2.plugins.ui.components.UIPropertyChangeListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.components.BufferedLabel;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;


/**
 * 
 */
public class 
BasicPluginViewImpl 
	implements UISWTViewEventListener, UIPropertyChangeListener 
{
  
  BasicPluginViewModel model;
  
  //GUI elements
  Display display;
  Composite panel;
  ProgressBar progress;
  BufferedLabel status;
  BufferedLabel task;
  StyledText log;
  
  boolean isCreated;
  
  public 
  BasicPluginViewImpl(
	BasicPluginViewModel 	model) 
  {
    this.model = model;
    isCreated = false;
  }
  
	public boolean eventOccurred(UISWTViewEvent event) {
		switch (event.getType()) {
			case UISWTViewEvent.TYPE_CREATE:
				if (isCreated)
					return false;
				isCreated = true;
				break;
				
			case UISWTViewEvent.TYPE_INITIALIZE:
				initialize((Composite)event.getData());
				break;
			
			case UISWTViewEvent.TYPE_REFRESH:
				refresh();
				break;
			
			case UISWTViewEvent.TYPE_DESTROY:
				delete();
				isCreated = false;
				break;
		}
		return true;
	}

  private void initialize(Composite composite) {
    GridData gridData;
    GridLayout gridLayout;
    String sConfigSectionID = model.getConfigSectionID();

    this.display = composite.getDisplay();
    panel = new Composite(composite,SWT.NULL);
    gridLayout = new GridLayout();
    gridLayout.numColumns = 2;
    panel.setLayout(gridLayout);
		gridData = new GridData(GridData.FILL_BOTH);
		panel.setLayoutData(gridData);
    
    /*
     * Status       : [Status Text]
     * Current Task : [Task Text]
     * Progress     : [||||||||||----------]
     * Log :
     * [
     * 
     * 
     * ]
     */
		
	Composite topSection = new Composite(panel, SWT.NONE);
    gridLayout = new GridLayout();
    gridLayout.numColumns = 2;
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    topSection.setLayout(gridLayout);
	gridData = new GridData(GridData.FILL_HORIZONTAL);
	if (sConfigSectionID == null){
		gridData.horizontalSpan = 2;
	}
	topSection.setLayoutData(gridData);
    
    if(model.getStatus().getVisible()) {
      Label statusTitle = new Label(topSection,SWT.NULL);
      Messages.setLanguageText(statusTitle,"plugins.basicview.status");
    
      status = new BufferedLabel(topSection,SWT.NULL);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      status.setLayoutData(gridData);
    }
    
    if(model.getActivity().getVisible()) {
      Label activityTitle = new Label(topSection,SWT.NULL);
      Messages.setLanguageText(activityTitle,"plugins.basicview.activity");
    
      task = new BufferedLabel(topSection,SWT.NULL);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      task.setLayoutData(gridData);
    }
    
    if(model.getProgress().getVisible()) {
      Label progressTitle = new Label(topSection,SWT.NULL);
      Messages.setLanguageText(progressTitle,"plugins.basicview.progress");
    
      progress = new ProgressBar(topSection,SWT.NULL);
      progress.setMaximum(100);
      progress.setMinimum(0);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      progress.setLayoutData(gridData);
    }
    
    if (sConfigSectionID != null) {
    	Composite configSection = new Composite(panel, SWT.NONE);
        gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 2;
        configSection.setLayout(gridLayout);
        gridData = new GridData(GridData.END | GridData.VERTICAL_ALIGN_END );
        configSection.setLayoutData(gridData);
        //Label padding = new Label(configSection,SWT.NULL);
        //gridData = new GridData(GridData.FILL_HORIZONTAL);
        //padding.setLayoutData(gridData);
    	Button btnConfig = new Button(configSection, SWT.PUSH);
    	Messages.setLanguageText(btnConfig, "plugins.basicview.config");
    	btnConfig.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
       	 UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
      	 if (uiFunctions != null) {
      		 uiFunctions.showConfig(model.getConfigSectionID());
      	 }
    		}
    	});
    	btnConfig.setLayoutData(new GridData());
    }
    
    if(model.getLogArea().getVisible()) {
      Label logTitle = new Label(topSection,SWT.NULL);
      Messages.setLanguageText(logTitle,"plugins.basicview.log");
    //  gridData = new GridData(GridData.FILL_HORIZONTAL);
    //  gridData.horizontalSpan = 1;
    //  logTitle.setLayoutData(gridData);
      
      Button button = new Button( topSection, SWT.PUSH );
      Messages.setLanguageText(button,"plugins.basicview.clear");
      
      button.addListener(SWT.Selection, new Listener() {
  	      public void handleEvent(Event event) 
  	      {
  	      	model.getLogArea().setText("");
  	      }});

      log = new StyledText(panel,SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 2;
      log.setLayoutData(gridData);
      log.setText( model.getLogArea().getText());
      model.getLogArea().addPropertyChangeListener(this);
    }
  }
  
  private void refresh() {
    if(status != null) {
      status.setText(model.getStatus().getText());
    }
    if(task != null) {
      task.setText(model.getActivity().getText());
    }
    if(progress != null) {
      progress.setSelection(model.getProgress().getPercentageComplete());
    }
  }
  
  public void propertyChanged(final UIPropertyChangeEvent ev) {
    if(ev.getSource() != model.getLogArea())
      return;
    if(display == null || display.isDisposed())
      return;
    if(log == null)
      return;
    display.asyncExec(new AERunnable(){
      public void runSupport() {
        if(log.isDisposed())
          return;
        String old_value = (String)ev.getOldPropertyValue();
        String new_value = (String) ev.getNewPropertyValue();
     
        if ( new_value.startsWith( old_value )){
               		
        	log.append( new_value.substring(old_value.length()));
       
        }else{
        	log.setText(new_value);
        }
      }
    });
  }
  
  private void
  delete()
  {
    model.getLogArea().removePropertyChangeListener( this );
  }
}
