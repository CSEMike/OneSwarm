/*
 * File    : NatPanel.java
 * Created : 12 oct. 2003 23:39:59
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
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.ipchecker.natchecker.NatChecker;
import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;

/**
 * @author Olivier
 * 
 */
public class NatPanel extends AbstractWizardPanel {

  StyledText textResults;
  Checker checker;

  Button bTest;
  Button bCancel;

 
  public class Checker extends AEThread {

    //private int lowPort;
    //private int highPort;
    private int TCPListenPort;

    private boolean bContinue;

    //public Checker(int lowPort, int highPort) {
    public Checker(int tcp_listen_port) {
      super("NAT Checker");
      //this.lowPort = lowPort;
      //this.highPort = highPort;
      this.TCPListenPort = tcp_listen_port;
      this.bContinue = true;
    }

    public void runSupport() {
      //if (lowPort <= highPort && (highPort-lowPort < 10)) {
        //for (int port = lowPort; port <= highPort && bContinue; port++) {
          printMessage(MessageText.getString("configureWizard.nat.testing") + " " + TCPListenPort + " ... ");
          NatChecker checker = new NatChecker(wizard.getAzureusCore(), NetworkAdmin.getSingleton().getMultiHomedOutgoingRoundRobinBindAddress(), TCPListenPort, false );
          switch (checker.getResult()) {
            case NatChecker.NAT_OK :
              String	additional_info = checker.getAdditionalInfo();
              printMessage(MessageText.getString("configureWizard.nat.ok") + "\n" + additional_info );
              break;
            case NatChecker.NAT_KO :
              printMessage( "\n" + MessageText.getString("configureWizard.nat.ko") + " - " + checker.getAdditionalInfo()+".\n");
              bContinue = false;
              break;
            default :
              printMessage( "\n" + MessageText.getString("configureWizard.nat.unable") + ". \n(" + checker.getAdditionalInfo()+").\n");
              break;
          }
        //}
      //}else {
      //  printMessage(MessageText.getString("configureWizard.nat.tooManyPorts") + "\n");
      //}
      enableNext();
    }

    public void stopIt() {
      bContinue = false;
    }
  }

  public NatPanel(ConfigureWizard wizard, IWizardPanel previous) {
    super(wizard, previous);
  }

  public void show() {
    wizard.setTitle(MessageText.getString("configureWizard.nat.title"));
    //wizard.setCurrentInfo(MessageText.getString("configureWizard.nat.hint"));
    Composite rootPanel = wizard.getPanel();
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    rootPanel.setLayout(layout);

    Composite panel = new Composite(rootPanel, SWT.NULL);
    GridData gridData = new GridData(GridData.FILL_BOTH);
    panel.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 4;
    panel.setLayout(layout);

    Label label = new Label(panel, SWT.WRAP);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 4;
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "configureWizard.nat.message");

    label = new Label(panel, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan = 4;
    label.setLayoutData(gridData);
    
    
    label = new Label(panel, SWT.NULL);
    gridData = new GridData();
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "configureWizard.nat.server.tcp_listen_port");

    final Text textServerTCPListen = new Text(panel, SWT.BORDER);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.widthHint = 80;
    textServerTCPListen.setLayoutData(gridData);
    textServerTCPListen.setText("" + ((ConfigureWizard) wizard).serverTCPListenPort);
    textServerTCPListen.addListener(SWT.Verify, new Listener() {
      public void handleEvent(Event e) {
        String text = e.text;
        char[] chars = new char[text.length()];
        text.getChars(0, chars.length, chars, 0);
        for (int i = 0; i < chars.length; i++) {
          if (!('0' <= chars[i] && chars[i] <= '9')) {
            e.doit = false;
            return;
          }
        }
      }
    });
    textServerTCPListen.addListener(SWT.Modify, new Listener() {
      public void handleEvent(Event e) {
      	try {
	        final int TCPListenPort = Integer.parseInt(textServerTCPListen.getText());
	        ((ConfigureWizard) wizard).serverTCPListenPort = TCPListenPort;
      	} catch (NumberFormatException ex) {
      		// ignore
      	}
      }
    });

    /*
    label = new Label(panel, SWT.NULL);
    Messages.setLanguageText(label, "configureWizard.nat.serverhigh");

    final Text textServerHigh = new Text(panel, SWT.BORDER);
    gridData = new GridData();
    gridData.widthHint = 100;
    textServerHigh.setLayoutData(gridData);
    textServerHigh.setText("" + ((ConfigureWizard) wizard).serverMaxPort);
    textServerHigh.addListener(SWT.Verify, new Listener() {
      public void handleEvent(Event e) {
        String text = e.text;
        char[] chars = new char[text.length()];
        text.getChars(0, chars.length, chars, 0);
        for (int i = 0; i < chars.length; i++) {
          if (!('0' <= chars[i] && chars[i] <= '9')) {
            e.doit = false;
            return;
          }
        }
      }
    });
    textServerHigh.addListener(SWT.Modify, new Listener() {
      public void handleEvent(Event e) {
        int highPort = 0;
        try{
          highPort = Integer.parseInt(textServerHigh.getText());
        } catch(Exception ignore) { }
        ((ConfigureWizard) wizard).serverMaxPort = highPort;
      }
    });

    final Button sharePort = new Button(panel,SWT.CHECK);
    sharePort.setSelection(((ConfigureWizard)wizard).serverSharePort);
    sharePort.addListener(SWT.Selection,new Listener() {
    	public void handleEvent(Event arg0) {
    		((ConfigureWizard)wizard).serverSharePort = sharePort.getSelection();
    		textServerHigh.setEnabled(!((ConfigureWizard)wizard).serverSharePort);
    	}
    });
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
    sharePort.setLayoutData(gridData);
    
	label = new Label(panel,SWT.NULL);
	Messages.setLanguageText(label, "configureWizard.nat.sharePort");
	 
    textServerHigh.setEnabled(!((ConfigureWizard)wizard).serverSharePort);
    */

    bTest = new Button(panel, SWT.PUSH);
    Messages.setLanguageText(bTest, "configureWizard.nat.test");
    gridData = new GridData();
    gridData.widthHint = 70;
    bTest.setLayoutData(gridData);

    bCancel = new Button(panel, SWT.PUSH);
    Messages.setLanguageText(bCancel, "Button.cancel");
    gridData = new GridData();
    gridData.widthHint = 70;
    bCancel.setLayoutData(gridData);
    bCancel.setEnabled(false);

    textResults = new StyledText(panel, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP );
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.heightHint = 70;
    gridData.horizontalSpan = 4;
    textResults.setLayoutData(gridData);
    textResults.setBackground(panel.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    bTest.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        wizard.setNextEnabled(false);
        bTest.setEnabled(false);
        bCancel.setEnabled(true);
        textResults.setText("");
        ConfigureWizard cw = (ConfigureWizard) wizard;
        
        //int lowPort = cw.serverMinPort;
        //int highPort = cw.serverSharePort?cw.serverMinPort:cw.serverMaxPort;
        int TCPListenPort = cw.serverTCPListenPort;
        checker = new Checker(TCPListenPort);
        checker.start();
      }
    });

    bCancel.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        if (checker != null)
          checker.stopIt();
        bCancel.setEnabled(false);
      }
    });
  }

  public void printMessage(final String message) {
    Display display = wizard.getDisplay();
    if (display == null || display.isDisposed())
      return;
    display.asyncExec(new AERunnable() {
      public void runSupport() {
        if (textResults == null || textResults.isDisposed())
          return;
        textResults.append(message);
      }
    });
  }

  private void enableNext() {
    Display display = wizard.getDisplay();
    if (display == null || display.isDisposed())
      return;
    display.asyncExec(new AERunnable(){
      public void runSupport() {
      	if (bTest == null || bTest.isDisposed()) {
      		return;
      	}
      	
      	if (wizard.getCurrentPanel().equals(this)) {
      		return;
      	}

        wizard.setNextEnabled(true);
        bTest.setEnabled(true);
        bCancel.setEnabled(false);
      }
    });
  }
  
  public boolean isNextEnabled() {
    return true;
  }
  
  public IWizardPanel getNextPanel() {
    return new FilePanel(((ConfigureWizard)wizard),this);
  }

}
