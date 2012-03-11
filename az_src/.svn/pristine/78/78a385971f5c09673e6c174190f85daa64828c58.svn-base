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
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.ipchecker.natchecker.NatChecker;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminProgressListener;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminProtocol;

/**
 * @author Olivier
 * 
 */
public class NatPanel extends AbstractWizardPanel {

  StyledText textResults;

  Button bTestTCP,bTestUDP;

 
  public class CheckerTCP extends AEThread {

	  	private AzureusCore	core;
	    private int TCPListenPort;
	    
	    public CheckerTCP(AzureusCore _core, int tcp_listen_port) {
	      super("NAT Checker TCP");
	      core		= _core;
	      this.TCPListenPort = tcp_listen_port;
	    }

	    public void 
	    runSupport() 
	    {
	    	try{
		          printMessage(MessageText.getString("configureWizard.nat.testing") + " TCP " + TCPListenPort + " ... ");
		          NatChecker checker = new NatChecker(core, NetworkAdmin.getSingleton().getMultiHomedServiceBindAddresses(true)[0], TCPListenPort, false);          
		          switch (checker.getResult()) {
		          case NatChecker.NAT_OK :
		            printMessage( "\n" + MessageText.getString("configureWizard.nat.ok") + "\n" + checker.getAdditionalInfo());
		            break;
		          case NatChecker.NAT_KO :
		            printMessage( "\n" + MessageText.getString("configureWizard.nat.ko") + " - " + checker.getAdditionalInfo()+".\n");
		            break;
		          default :
		            printMessage( "\n" + MessageText.getString("configureWizard.nat.unable") + ". \n(" + checker.getAdditionalInfo()+").\n");
		            break;
		          }
	    	}finally{
	          enableNext();
	    	}
	    }
	  }
	  
	  public class CheckerUDP extends AEThread {

		    private AzureusCore	core;
		    private int			udp_port;
		    
		    public CheckerUDP(AzureusCore _core, int _udp_port ){
		      super("NAT Checker UDP");
		      core 		= _core;
		      udp_port	= _udp_port;
		    }

		    public void 
		    runSupport() 
		    {
		    	try{
			    	final NetworkAdmin	admin = NetworkAdmin.getSingleton();
			    	
					NetworkAdminProtocol[] inbound_protocols = admin.getInboundProtocols(core);
					
					NetworkAdminProtocol selected = null;
					
					for ( NetworkAdminProtocol p: inbound_protocols ){
						
						if ( p.getType() == NetworkAdminProtocol.PT_UDP && p.getPort() == udp_port ){
							
							selected = p;
							
							break;
						}
					}
						
					if ( selected == null ){
						
						selected = admin.createInboundProtocol( core, NetworkAdminProtocol.PT_UDP, udp_port );
					}
					
			        if ( selected == null ){
			        	
			        	printMessage( "\n" + MessageText.getString("configureWizard.nat.ko") + ". \n( No UDP protocols enabled ).\n");
			        	
			        }else{
			        	
			        	printMessage(MessageText.getString("configureWizard.nat.testing") + " UDP " + udp_port + " ... ");
						
							try{
								selected.test( 
									null,
									true,
									new NetworkAdminProgressListener()
									{
										public void 
										reportProgress(
											String task )
										{
											printMessage( "\n    " + task );
										}
									});
								
					            printMessage( "\n" + MessageText.getString("configureWizard.nat.ok"));
									
							}catch( Throwable e ){
								
					            printMessage( "\n" + MessageText.getString("configureWizard.nat.ko") + ". " + Debug.getNestedExceptionMessage(e)+".\n");
							}
						}
			   
		    	}finally{
		    		enableNext();
		    	}
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
    
    	// TCP
    
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

    bTestTCP = new Button(panel, SWT.PUSH);
    Messages.setLanguageText(bTestTCP, "configureWizard.nat.test");
    gridData = new GridData();
    gridData.widthHint = 70;
    bTestTCP.setLayoutData(gridData);

    label = new Label(panel, SWT.NULL);
    
    	// UDP

    label = new Label(panel, SWT.NULL);
    gridData = new GridData();
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "configureWizard.nat.server.udp_listen_port");

    final Text textServerUDPListen = new Text(panel, SWT.BORDER);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.widthHint = 80;
    textServerUDPListen.setLayoutData(gridData);
    textServerUDPListen.setText("" + ((ConfigureWizard) wizard).serverUDPListenPort);
    textServerUDPListen.addListener(SWT.Verify, new Listener() {
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
    textServerUDPListen.addListener(SWT.Modify, new Listener() {
      public void handleEvent(Event e) {
      	try {
	        final int UDPListenPort = Integer.parseInt(textServerUDPListen.getText());
	        ((ConfigureWizard) wizard).serverUDPListenPort = UDPListenPort;
      	} catch (NumberFormatException ex) {
      		// ignore
      	}
      }
    });

    bTestUDP = new Button(panel, SWT.PUSH);
    Messages.setLanguageText(bTestUDP, "configureWizard.nat.test");
    gridData = new GridData();
    gridData.widthHint = 70;
    bTestUDP.setLayoutData(gridData);
    
    label = new Label(panel, SWT.NULL);
    
    	// blah
    
    textResults = new StyledText(panel, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP );
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.heightHint = 70;
    gridData.horizontalSpan = 4;
    textResults.setLayoutData(gridData);
    textResults.setBackground(panel.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    bTestTCP.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        wizard.setNextEnabled(false);
        bTestTCP.setEnabled(false);
        bTestUDP.setEnabled(false);
        textResults.setText("");
        CoreWaiterSWT.waitForCoreRunning(new AzureusCoreRunningListener() {
				
					public void azureusCoreRunning(AzureusCore core) {
						ConfigureWizard cw = (ConfigureWizard) wizard;
						
						int TCPListenPort = cw.serverTCPListenPort;
						CheckerTCP checker = new CheckerTCP(core, TCPListenPort);
						checker.start();
					}
				});
      }
    });

    bTestUDP.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event event) {
          wizard.setNextEnabled(false);
          bTestTCP.setEnabled(false);
          bTestUDP.setEnabled(false);
          textResults.setText("");
          CoreWaiterSWT.waitForCoreRunning(new AzureusCoreRunningListener() {
  				
  					public void azureusCoreRunning(AzureusCore core) {
  						ConfigureWizard cw = (ConfigureWizard) wizard;
  						
  						int UDPListenPort = cw.serverUDPListenPort;
  						CheckerUDP checker = new CheckerUDP(core, UDPListenPort);
  						checker.start();
  					}
  				});
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
       	if (bTestTCP == null || bTestTCP.isDisposed()) {
      		return;
      	}
     	if (bTestUDP == null || bTestUDP.isDisposed()) {
      		return;
      	}
      	if (wizard.getCurrentPanel().equals(this)) {
      		return;
      	}

        wizard.setNextEnabled(true);
        bTestTCP.setEnabled(true);
        bTestUDP.setEnabled(true);
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
