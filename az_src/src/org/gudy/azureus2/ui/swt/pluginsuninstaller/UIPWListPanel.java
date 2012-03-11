/*
 * Created on 29 nov. 2004
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
package org.gudy.azureus2.ui.swt.pluginsuninstaller;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.Wizard;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreRunningListener;


/**
 * @author Olivier Chalouhi
 *
 */
public class UIPWListPanel extends AbstractWizardPanel {

  Table pluginList;
  
  public 
  UIPWListPanel(
	Wizard 					wizard, 
	IWizardPanel 			previous ) 
  {
	super(wizard, previous);
  }


  public void 
  show() 
  {
  	CoreWaiterSWT.waitForCoreRunning(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				_show(core);
			}
		});
  }
  
  private void _show(AzureusCore core) {
    wizard.setTitle(MessageText.getString("uninstallPluginsWizard.list.title"));
    wizard.setErrorMessage("");
    
	Composite rootPanel = wizard.getPanel();
	GridLayout layout = new GridLayout();
	layout.numColumns = 1;
	rootPanel.setLayout(layout);

	Composite panel = new Composite(rootPanel, SWT.NULL);
	GridData gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.FILL_HORIZONTAL);
	panel.setLayoutData(gridData);
	layout = new GridLayout();
	layout.numColumns = 1;
	panel.setLayout(layout);
	
	final Label lblStatus = new Label(panel,SWT.NULL);
	Messages.setLanguageText(lblStatus,"uninstallPluginsWizard.list.loaded");
	
	pluginList = new Table(panel,SWT.BORDER | SWT.V_SCROLL | SWT.CHECK | SWT.FULL_SELECTION | SWT.SINGLE); 
	pluginList.setHeaderVisible(true);
	GridData data = new GridData(GridData.FILL_HORIZONTAL);
	data.heightHint = 200;
	pluginList.setLayoutData(data);
	
	
	TableColumn tcName = new TableColumn(pluginList,SWT.LEFT);
	Messages.setLanguageText(tcName,"installPluginsWizard.list.name");
	tcName.setWidth(200);
	
	TableColumn tcVersion = new TableColumn(pluginList,SWT.LEFT);
	Messages.setLanguageText(tcVersion,"installPluginsWizard.list.version");
	tcVersion.setWidth(150);

    PluginInterface plugins[] = new PluginInterface[0];
    try {
      plugins = core.getPluginManager().getPluginInterfaces();
      
      Arrays.sort( 
	      	plugins,
		  	new Comparator()
			{
	      		public int 
				compare(
					Object o1, 
					Object o2)
	      		{
	      			return(((PluginInterface)o1).getPluginName().compareTo(((PluginInterface)o2).getPluginName()));
	      		}
			});
    } catch(final Exception e) {
    	
    	Debug.printStackTrace(e);
    }
     
    	// one "plugin" can have multiple interfaces. We need to group by their id
    
    Map	pid_map = new HashMap();
    
    for(int i = 0 ; i < plugins.length ; i++){
    	
        PluginInterface plugin = plugins[i];
                
        String	pid   = plugin.getPluginID();
        
        ArrayList	pis = (ArrayList)pid_map.get( pid );
        
        if ( pis == null ){
        	
        	pis = new ArrayList();
        	
        	pid_map.put( pid, pis );
        }
        
        pis.add( plugin );
    }
    
    ArrayList[]	pid_list = new ArrayList[pid_map.size()];
    
    pid_map.values().toArray( pid_list );
	
    Arrays.sort( 
    		pid_list,
		  	new Comparator()
			{
	      		public int 
				compare(
					Object o1, 
					Object o2)
	      		{
	      			ArrayList	l1 = (ArrayList)o1;
	      			ArrayList	l2 = (ArrayList)o2;
	      			return(((PluginInterface)l1.get(0)).getPluginName().compareToIgnoreCase(((PluginInterface)l2.get(0)).getPluginName()));
	      		}
			});
    
    for(int i = 0 ; i < pid_list.length ; i++){
    	
      ArrayList	pis = pid_list[i];
      
      boolean	skip = false;
      
      String	display_name = "";
      	
      for (int j=0;j<pis.size();j++){
      	
      	PluginInterface	pi = (PluginInterface)pis.get(j);
      	
      	if ( pi.getPluginState().isMandatory() || pi.getPluginState().isBuiltIn()){
      		
      		skip = true;
      		
      		break;
      	}
      	
      	display_name += (j==0?"":",") + pi.getPluginName();
      }
      
      if ( skip ){
      	
      	continue;
      }
      
      PluginInterface plugin = (PluginInterface)pis.get(0);
      
      List	selected_plugins = ((UnInstallPluginWizard)wizard).getPluginList();
      
      TableItem item = new TableItem(pluginList,SWT.NULL);
      item.setData(plugin);
      item.setText(0, display_name);
      item.setChecked( selected_plugins.contains( plugin ));
      String version = plugin.getPluginVersion();
      if(version == null) version = MessageText.getString("installPluginsWizard.list.nullversion");
      item.setText(1,version);
    }
	
	pluginList.addListener(SWT.Selection,new Listener() {
	  public void handleEvent(Event e) {
	    updateList();	  
	  }
	});
  }
  
	public boolean 
	isFinishEnabled() 
	{
		return(((UnInstallPluginWizard)wizard).getPluginList().size() > 0 );
	}
	
	public IWizardPanel getFinishPanel() {
	    return new UIPWFinishPanel(wizard,this);
	}
	
  public void updateList() {
    ArrayList list = new ArrayList();
    TableItem[] items = pluginList.getItems();
    for(int i = 0 ; i < items.length ; i++) {
      if(items[i].getChecked())
        list.add(items[i].getData());          
    }
    ((UnInstallPluginWizard)wizard).setPluginList(list);
    ((UnInstallPluginWizard)wizard).setFinishEnabled( isFinishEnabled() );
    
  }
}
