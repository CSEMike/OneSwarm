/*
 * File    : FinishPanel.java
 * Created : 13 oct. 2003 02:37:31
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

package org.gudy.azureus2.ui.swt.config.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;

/**
 * @author Olivier
 * 
 */
public class FinishPanel extends AbstractWizardPanel {

  public FinishPanel(ConfigureWizard wizard, IWizardPanel previous) {
    super(wizard, previous);
  }

  public void show() {
    wizard.setTitle(MessageText.getString("configureWizard.finish.title"));
    //wizard.setCurrentInfo(MessageText.getString("configureWizard.nat.hint"));
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

    Label label = new Label(panel, SWT.WRAP);
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    gridData.widthHint = 380;
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "configureWizard.finish.message");
  }

  public void finish() {
    ConfigureWizard cfWizard = ((ConfigureWizard)wizard);
    COConfigurationManager.setParameter("Max Upload Speed KBs",cfWizard.maxUpSpeed);
    COConfigurationManager.setParameter("Max Uploads",cfWizard.nbUploadsPerTorrent);
    COConfigurationManager.setParameter("max active torrents",cfWizard.maxActiveTorrents);
    COConfigurationManager.setParameter("max downloads",cfWizard.maxDownloads);
    COConfigurationManager.setParameter("TCP.Listen.Port",cfWizard.serverTCPListenPort);
    COConfigurationManager.setParameter("UDP.Listen.Port",cfWizard.serverTCPListenPort);
    COConfigurationManager.setParameter("UDP.NonData.Listen.Port",cfWizard.serverTCPListenPort);
    //COConfigurationManager.setParameter("Low Port",cfWizard.serverMinPort);
	//COConfigurationManager.setParameter("High Port",cfWizard.serverMaxPort);
	//COConfigurationManager.setParameter("Server.shared.port",cfWizard.serverSharePort);
    COConfigurationManager.setParameter("General_sDefaultTorrent_Directory",cfWizard.torrentPath);
    //COConfigurationManager.setParameter("Use Resume",cfWizard.fastResume);
    COConfigurationManager.setParameter("Wizard Completed",true);
    COConfigurationManager.save();
    wizard.switchToClose();
    cfWizard.completed = true;
  }
  
  public boolean isPreviousEnabled() {
    return false;
  }
}
