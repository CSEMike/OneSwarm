/*
 * File    : TransferPanel.java
 * Created : 12 oct. 2003 19:41:14
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;

/**
 * @author Olivier
 * 
 */
public class TransferPanel extends AbstractWizardPanel {

  Label nbMaxActive;
  Label nbMaxDownloads;
  Label nbMaxUploadsPerTorrent;

  private static final int upRates[] =
    {
      0,
      5,
      6,
      7,
      8,
      9,
      10,
      11,
      12,
      13,
      14,
      15,
      20,
      25,
      30,
      35,
      40,
      45,
      50,
      55,
      60,
      70,
      80,
      85,
      90,
      100,
      110,
      150,
      200,
      250,
      300,
      350,
      400,
      450,
      500,
      600,
      700,
      800,
      900,
      1000 };

  public TransferPanel(ConfigureWizard wizard, IWizardPanel previous) {
    super(wizard, previous);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.wizard.IWizardPanel#show()
   */
  public void show() {
    wizard.setTitle(MessageText.getString("configureWizard.transfer.title"));
    wizard.setCurrentInfo(MessageText.getString("configureWizard.transfer.hint"));
    Composite rootPanel = wizard.getPanel();
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    rootPanel.setLayout(layout);

    Composite panel = new Composite(rootPanel, SWT.NULL);
    GridData gridData = new GridData(GridData.FILL_BOTH);
    panel.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    panel.setLayout(layout);

    Label label = new Label(panel, SWT.WRAP);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "configureWizard.transfer.message");

    label = new Label(panel, SWT.NULL);
    Messages.setLanguageText(label, "configureWizard.transfer.connection");

    final Combo connections = new Combo(panel, SWT.SINGLE | SWT.READ_ONLY);
    for (int i = 0; i < 8; i++) {
      connections.add(MessageText.getString("configureWizard.transfer.connection." + i));
    }

    label = new Label(panel, SWT.NULL);
    Messages.setLanguageText(label, "configureWizard.transfer.maxUpSpeed");

    final String upsLabels[] = new String[upRates.length];
    final int upsValues[] = new int[upRates.length];
    upsLabels[0] = MessageText.getString("ConfigView.unlimited"); //$NON-NLS-1$
    upsValues[0] = 0;
    for (int i = 1; i < upRates.length; i++) {
      upsLabels[i] = " " + upRates[i] + " KB/s"; //$NON-NLS-1$ //$NON-NLS-2$
      upsValues[i] = 1024 * upRates[i];
    }
    final Combo cMaxUpSpeed = new Combo(panel, SWT.SINGLE | SWT.READ_ONLY);
    for (int i = 0; i < upRates.length; i++) {
      cMaxUpSpeed.add(upsLabels[i]);
    }

    //final Text textMaxUpSpeed = new Text(panel, SWT.BORDER);
    gridData = new GridData();
    gridData.widthHint = 100;
    cMaxUpSpeed.setLayoutData(gridData);

    label = new Label(panel, SWT.NULL);
    Messages.setLanguageText(label, "configureWizard.transfer.maxActiveTorrents");
    nbMaxActive = new Label(panel, SWT.NULL);
    gridData = new GridData();
    gridData.widthHint = 100;
    nbMaxActive.setLayoutData(gridData);

    label = new Label(panel, SWT.NULL);
    Messages.setLanguageText(label, "configureWizard.transfer.maxDownloads");
    nbMaxDownloads = new Label(panel, SWT.NULL);
    gridData = new GridData();
    gridData.widthHint = 100;
    nbMaxDownloads.setLayoutData(gridData);

    label = new Label(panel, SWT.NULL);
    Messages.setLanguageText(label, "configureWizard.transfer.maxUploadsPerTorrent");
    nbMaxUploadsPerTorrent = new Label(panel, SWT.NULL);
    gridData = new GridData();
    gridData.widthHint = 100;
    nbMaxUploadsPerTorrent.setLayoutData(gridData);

    connections.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        int index = connections.getSelectionIndex();
        ((ConfigureWizard) wizard).upSpeed = index;
        if (index == 0) {
          cMaxUpSpeed.setEnabled(true);
        }
        else {
          cMaxUpSpeed.setEnabled(false);
          int upSpeeds[] = { 0, 5, 13, 25, 40, 55, 85, 110 };
          cMaxUpSpeed.select(findIndex(upSpeeds[index], upRates));
          int maxUp = upRates[cMaxUpSpeed.getSelectionIndex()];
          ((ConfigureWizard) wizard).maxUpSpeed = maxUp;
          computeAll(maxUp);
        }
      }
    });

    cMaxUpSpeed.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        int maxUp = upRates[cMaxUpSpeed.getSelectionIndex()];
        ((ConfigureWizard) wizard).maxUpSpeed = maxUp;
        computeAll(maxUp);
      }
    });

    connections.select(((ConfigureWizard) wizard).upSpeed);
    cMaxUpSpeed.select(findIndex(((ConfigureWizard) wizard).maxUpSpeed, upRates));
    cMaxUpSpeed.setEnabled(((ConfigureWizard) wizard).upSpeed == 0);
    computeAll(((ConfigureWizard) wizard).maxUpSpeed);

  }

  public void computeAll(int maxUploadSpeed) {
    if (maxUploadSpeed != 0) {

      int nbMaxActive = (int) (Math.pow(maxUploadSpeed,0.34) * 0.92);
      int nbMaxUploads = (int) (Math.pow(maxUploadSpeed,0.25) * 1.68);
      int nbMaxDownloads = (nbMaxActive * 4) / 5;
      
      if (nbMaxDownloads == 0)
      	nbMaxDownloads = 1;
      if (nbMaxUploads > 50)
        nbMaxUploads = 50;

      ((ConfigureWizard) wizard).maxActiveTorrents = nbMaxActive;
      ((ConfigureWizard) wizard).maxDownloads = nbMaxDownloads;
      ((ConfigureWizard) wizard).nbUploadsPerTorrent = nbMaxUploads;
    	
    }
    else {
      ((ConfigureWizard) wizard).maxActiveTorrents = 0;
      ((ConfigureWizard) wizard).maxDownloads = 0;
      ((ConfigureWizard) wizard).nbUploadsPerTorrent = 4;
    }
    refresh();
  }

  public void refresh() {
    nbMaxActive.setText("" + ((ConfigureWizard) wizard).maxActiveTorrents);
    nbMaxDownloads.setText("" + ((ConfigureWizard) wizard).maxDownloads);
    nbMaxUploadsPerTorrent.setText("" + ((ConfigureWizard) wizard).nbUploadsPerTorrent);
  }

  private int findIndex(int value, int values[]) {
    for (int i = 0; i < values.length; i++) {
      if (values[i] == value)
        return i;
    }
    return 0;
  }
  
  public boolean isNextEnabled() {
    return true;
  }
  
  public IWizardPanel getNextPanel() {
    return new NatPanel(((ConfigureWizard)wizard),this);
  }

}
