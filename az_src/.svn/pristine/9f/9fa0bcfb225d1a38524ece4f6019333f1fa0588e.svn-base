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
package org.gudy.azureus2.ui.swt.pluginsinstaller;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.Wizard;

/**
 * @author Olivier Chalouhi
 *
 */
public class IPWInstallModePanel extends AbstractWizardPanel {
  
  private static final int MODE_USER   = 0;
  private static final int MODE_SHARED = 1;
  
  public 
  IPWInstallModePanel(
	Wizard 					wizard, 
	IWizardPanel 			previous ) 
  {
	super(wizard, previous);
  }


  public void 
  show() 
  {
	wizard.setTitle(MessageText.getString("installPluginsWizard.installMode.title"));
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

		// default is shared installation
	
	((InstallPluginWizard) wizard).shared = true;

	Button bSharedMode = new Button(panel,SWT.RADIO);
	Messages.setLanguageText(bSharedMode,"installPluginsWizard.installMode.shared");
	bSharedMode.setData("mode",new Integer(MODE_SHARED));
	bSharedMode.setSelection(true);
	GridData data = new GridData(GridData.FILL_VERTICAL);
	data.verticalAlignment = GridData.VERTICAL_ALIGN_END;
	bSharedMode.setLayoutData(data);
	
	
	Button bUserMode = new Button(panel,SWT.RADIO);
	Messages.setLanguageText(bUserMode,"installPluginsWizard.installMode.user");
	bUserMode.setData("mode",new Integer(MODE_USER));
	data = new GridData(GridData.FILL_VERTICAL);
	data.verticalAlignment = GridData.VERTICAL_ALIGN_BEGINNING;
	bUserMode.setLayoutData(data);
	
	
	Listener modeListener = new Listener() {
	  public void handleEvent(Event e) {
	    ((InstallPluginWizard) wizard).shared = ((Integer) e.widget.getData("mode")).intValue() == MODE_SHARED;
	  }
	};

	bUserMode.addListener(SWT.Selection,modeListener);
	bSharedMode.addListener(SWT.Selection,modeListener);
  }
	
	public boolean 
	isFinishEnabled() 
	{
	   return( true );
	}
	
	public IWizardPanel getFinishPanel() {
	    return new IPWFinishPanel(wizard,this);
	}
}
