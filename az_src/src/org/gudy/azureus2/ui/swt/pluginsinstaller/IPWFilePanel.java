/*
 * Created on 30 nov. 2004
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
package org.gudy.azureus2.ui.swt.pluginsinstaller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.installer.InstallablePlugin;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.mainwindow.ListenerNeedingCoreRunning;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.Wizard;

import com.aelitis.azureus.core.AzureusCore;

/**
 * @author Olivier Chalouhi
 *
 */
public class IPWFilePanel extends AbstractWizardPanel {
  
  Text txtFile;
  boolean valid = false;
  
  public IPWFilePanel(
      Wizard wizard,
      IWizardPanel previous) {
    super(wizard,previous);
  }
  
  public void show() {
    wizard.setTitle(MessageText.getString("installPluginsWizard.file.title"));
	wizard.setErrorMessage("");
	
	Composite rootPanel = wizard.getPanel();
	GridLayout layout = new GridLayout();
	layout.numColumns = 1;
	rootPanel.setLayout(layout);

	Composite panel = new Composite(rootPanel, SWT.NULL);
	GridData gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.FILL_HORIZONTAL);
	panel.setLayoutData(gridData);
	layout = new GridLayout();
	layout.numColumns = 3;
	panel.setLayout(layout);
	
	Label label = new Label(panel,SWT.NULL);
	Messages.setLanguageText(label,"installPluginsWizard.file.file");
	
	txtFile = new Text(panel,SWT.BORDER);
	GridData data = new GridData(GridData.FILL_HORIZONTAL);
	txtFile.setLayoutData(data);
	txtFile.addListener(SWT.Modify,new ListenerNeedingCoreRunning() {
	  public void handleEvent(AzureusCore core, Event event) {
	    checkValidFile(core);
	  }
	}
	);
	
	
	Button btnBrowse = new Button(panel,SWT.PUSH);
	Messages.setLanguageText(btnBrowse,"installPluginsWizard.file.browse");
	btnBrowse.addListener(SWT.Selection,new Listener() {
	  public void handleEvent(Event event) {
	    FileDialog fd = new FileDialog(wizard.getWizardWindow());
	    fd.setFilterExtensions(new String[] {"*.zip;*.jar"});
	    fd.setFilterNames(new String[] {"Azureus Plugins"});
	    String fileName = fd.open();
	    if(fileName != null) txtFile.setText(fileName);	    
	  }
	});	
	
  }
  
  private void checkValidFile(AzureusCore core) {
		String fileName = txtFile.getText();
		String error_message = null;
		try {
			File f = new File(fileName);
			if (f.isFile()
					&& (f.getName().endsWith(".jar") || f.getName().endsWith(".zip"))) {
				wizard.setErrorMessage("");
				wizard.setNextEnabled(true);
				List list = new ArrayList();
				InstallablePlugin plugin = core.getPluginManager().getPluginInstaller().installFromFile(
						f);
				list.add(plugin);
				((InstallPluginWizard) wizard).plugins = list;
				valid = true;
				return;
			}
		} catch (org.gudy.azureus2.plugins.PluginException e) {
			error_message = e.getMessage();
			Debug.printStackTrace(e);
		} catch (Exception e) {
			error_message = null;
			Debug.printStackTrace(e);
		}
		valid = false;
		if (!fileName.equals("")) {
			String error_message_full;
			if (new File(fileName).isFile()) {
				error_message_full = MessageText.getString("installPluginsWizard.file.invalidfile");
			} else {
				error_message_full = MessageText.getString("installPluginsWizard.file.no_such_file");
			}
			if (error_message != null) {
				error_message_full += " (" + error_message + ")";
			}
			wizard.setErrorMessage(error_message_full);
			wizard.setNextEnabled(false);
		}
  }
  
	public boolean 
	isNextEnabled() 
	{
	   return valid;
	}
	
	public IWizardPanel getNextPanel() {
	   return new IPWInstallModePanel(wizard,this);
	}
}
