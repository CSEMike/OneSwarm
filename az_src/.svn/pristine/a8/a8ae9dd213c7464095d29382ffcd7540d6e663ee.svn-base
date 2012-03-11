/*
 * File    : ChooseServicePanel.java
 * Created : 10 nov. 2003
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
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
 */
 
package org.gudy.azureus2.ui.swt.ipchecker;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.ipchecker.extipchecker.ExternalIPCheckerFactory;
import org.gudy.azureus2.core3.ipchecker.extipchecker.ExternalIPCheckerService;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;

/**
 * @author Olivier
 *
 */
public class ChooseServicePanel extends AbstractWizardPanel {

  private Combo servicesList;
  private ExternalIPCheckerService[] services;
  Label serviceDescription;
  Label serviceUrl;
  
  public ChooseServicePanel(IpCheckerWizard wizard,IWizardPanel previousPanel) {
    super(wizard,previousPanel);
  }
  
  public void show() {
    wizard.setTitle(MessageText.getString("ipCheckerWizard.service"));
    wizard.setCurrentInfo(MessageText.getString("ipCheckerWizard.chooseService"));
    Composite rootPanel = wizard.getPanel();
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    rootPanel.setLayout(layout);
    
    Label label = new Label(rootPanel,SWT.WRAP);
    GridData gridData = new GridData();
    gridData.widthHint = 380;    
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);
    label.setText(MessageText.getString("ipCheckerWizard.explanations"));
    
    this.servicesList = new Combo(rootPanel,SWT.READ_ONLY);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    servicesList.setLayoutData(gridData);
    
    this.services = ExternalIPCheckerFactory.create().getServices();
    
    for(int i = 0 ; i < services.length ; i++) {
      servicesList.add(services[i].getName());
    }        
        
    label = new Label(rootPanel,SWT.NULL);
    label.setText(MessageText.getString("ipCheckerWizard.service.url"));

    Cursor handCursor = new Cursor(rootPanel.getDisplay(), SWT.CURSOR_HAND);
    
    this.serviceUrl = new Label(rootPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    serviceUrl.setLayoutData(gridData);
    serviceUrl.setForeground(Colors.blue);
    serviceUrl.setCursor(handCursor);
    serviceUrl.addMouseListener(new MouseAdapter() {
      public void mouseDoubleClick(MouseEvent arg0) {
      	Utils.launch((String) ((Label) arg0.widget).getText());
      }
      public void mouseDown(MouseEvent arg0) {
      	Utils.launch((String) ((Label) arg0.widget).getText());
      }
    });
    
    servicesList.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        updateInfos();        
      }
    });
    servicesList.select(0);
    
	label = new Label(rootPanel,SWT.NULL);
	gridData = new GridData();
	gridData.heightHint = 50;
	gridData.verticalAlignment = GridData.VERTICAL_ALIGN_BEGINNING;
	label.setLayoutData(gridData);
	label.setText(MessageText.getString("ipCheckerWizard.service.description"));
    
	this.serviceDescription = new Label(rootPanel,SWT.WRAP);
	gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.heightHint = 50;
    gridData.verticalAlignment = GridData.VERTICAL_ALIGN_BEGINNING;
	serviceDescription.setLayoutData(gridData);

    updateInfos();
    
  }
  
  private void updateInfos() {
    int selection = servicesList.getSelectionIndex();
    serviceDescription.setText(services[selection].getDescription());
    serviceUrl.setText(services[selection].getURL());
    ((IpCheckerWizard)wizard).selectedService = services[selection];
	((IpCheckerWizard)wizard).setFinishEnabled( services[selection].supportsCheck());
  }
    
  public boolean isFinishEnabled() {
    return true;
  }
  
  public IWizardPanel getFinishPanel() {
    return new ProgressPanel((IpCheckerWizard)wizard,this);
  }

}
