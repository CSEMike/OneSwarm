/*
 * Created on 14 avr. 2005
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
package org.gudy.azureus2.ui.swt.nat;

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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.ipchecker.natchecker.NatChecker;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;

public class NatTestWindow {
  
  Display display;
  
  Button bTest,bApply,bCancel;
  StyledText textResults;
  
  Checker checker;
  int serverTCPListenPort;
  public class Checker extends AEThread {

    //private int lowPort;
    //private int highPort;
    private int TCPListenPort;
    
    public Checker(int tcp_listen_port) {
      super("NAT Checker");
      this.TCPListenPort = tcp_listen_port;
    }

    public void runSupport() {
          printMessage(MessageText.getString("configureWizard.nat.testing") + " " + TCPListenPort + " ... ");
          NatChecker checker = new NatChecker(AzureusCoreFactory.getSingleton(), NetworkAdmin.getSingleton().getMultiHomedOutgoingRoundRobinBindAddress(), TCPListenPort, false);          
          switch (checker.getResult()) {
          case NatChecker.NAT_OK :
            printMessage(MessageText.getString("configureWizard.nat.ok") + "\n" + checker.getAdditionalInfo());
            break;
          case NatChecker.NAT_KO :
            printMessage( "\n" + MessageText.getString("configureWizard.nat.ko") + " - " + checker.getAdditionalInfo()+".\n");
            break;
          default :
            printMessage( "\n" + MessageText.getString("configureWizard.nat.unable") + ". \n(" + checker.getAdditionalInfo()+").\n");
            break;
          }
          if (display.isDisposed()) {return;}
          display.asyncExec(new AERunnable()  {
            public void runSupport() {
              if(bTest != null && ! bTest.isDisposed())
               bTest.setEnabled(true);
            }
          });
    }
  }
  
  public NatTestWindow() {
    serverTCPListenPort = COConfigurationManager.getIntParameter( "TCP.Listen.Port" );
    
    final Shell shell = ShellFactory.createShell(SWT.BORDER | SWT.TITLE | SWT.CLOSE);        
    shell.setText(MessageText.getString("configureWizard.nat.title"));
    Utils.setShellIcon(shell);

    display = shell.getDisplay();
    
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    shell.setLayout(layout);

    Composite panel = new Composite(shell, SWT.NULL);
    GridData gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.FILL_HORIZONTAL);
    panel.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 3;
    panel.setLayout(layout);

    Label label = new Label(panel, SWT.WRAP);
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    gridData.widthHint = 400;
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "configureWizard.nat.message");

    label = new Label(panel, SWT.NULL);
    label = new Label(panel, SWT.NULL);
    label = new Label(panel, SWT.NULL);
    label = new Label(panel, SWT.NULL);
    Messages.setLanguageText(label, "configureWizard.nat.server.tcp_listen_port");

    final Text textServerTCPListen = new Text(panel, SWT.BORDER);
    gridData = new GridData();    
    gridData.grabExcessHorizontalSpace = true;
    gridData.horizontalAlignment = SWT.FILL;
    textServerTCPListen.setLayoutData(gridData);
    textServerTCPListen.setText("" + serverTCPListenPort);
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
        final int TCPListenPort = Integer.parseInt(textServerTCPListen.getText());
        serverTCPListenPort = TCPListenPort;
      }
    });

   

    bTest = new Button(panel, SWT.PUSH);
    Messages.setLanguageText(bTest, "configureWizard.nat.test");
    gridData = new GridData();
    gridData.widthHint = 70;
    bTest.setLayoutData(gridData);

    textResults = new StyledText(panel, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP );
    gridData = new GridData();
    gridData.widthHint = 400;
    gridData.heightHint = 100;
    gridData.grabExcessVerticalSpace = true;
    gridData.verticalAlignment = SWT.FILL;
    gridData.horizontalSpan = 3;
    textResults.setLayoutData(gridData);
    textResults.setBackground(panel.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    bTest.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        bTest.setEnabled(false);
        textResults.setText("");
        
        checker = new Checker(serverTCPListenPort);
        checker.start();
      }
    });
        
        
    bApply = new Button(panel,SWT.PUSH);
    bApply.setText(MessageText.getString("Button.apply"));
    gridData = new GridData();
    gridData.widthHint = 70;
    gridData.grabExcessHorizontalSpace = true;
    gridData.horizontalAlignment = SWT.RIGHT;
    gridData.horizontalSpan = 2;
    bApply.setLayoutData(gridData);
    
    
    bApply.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
	   	int	old_tcp 	= COConfigurationManager.getIntParameter( "TCP.Listen.Port" );
	   	int	old_udp 	= COConfigurationManager.getIntParameter( "UDP.Listen.Port" );
	   	int	old_udp2 	= COConfigurationManager.getIntParameter( "UDP.NonData.Listen.Port" );
    	
        COConfigurationManager.setParameter("TCP.Listen.Port",serverTCPListenPort);
        
        if ( old_tcp == old_udp ){
        	COConfigurationManager.setParameter("UDP.Listen.Port",serverTCPListenPort);
        }
        if ( old_tcp == old_udp2 ){
        	COConfigurationManager.setParameter("UDP.NonData.Listen.Port",serverTCPListenPort);
        }

        COConfigurationManager.save();
        
        shell.close();
      }
    });
    
    bCancel = new Button(panel,SWT.PUSH);
    bCancel.setText(MessageText.getString("Button.cancel"));
    gridData = new GridData();
    gridData.widthHint = 70;
    bCancel.setLayoutData(gridData);
    bCancel.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        shell.close();
      }      
    });
    
	shell.setDefaultButton( bApply );
	
	shell.addListener(SWT.Traverse, new Listener() {	
		public void handleEvent(Event e) {
			if ( e.character == SWT.ESC){
				shell.close();
			}
		}
	});
	
    shell.pack();
    Utils.centreWindow(shell);
    shell.open();
  }

  public void printMessage(final String message) {
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
}
