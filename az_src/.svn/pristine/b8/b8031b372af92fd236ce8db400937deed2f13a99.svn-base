/*
 * Created on 7 sept. 2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

/**
 * @author Olivier
 * 
 */
public class PasswordWindow {

  private Shell shell;
  
  //protected static AEMonitor	class_mon	= new AEMonitor( "PasswordWindow:class" );
  
  private static boolean bOk;
  
  private static long lastSuccess = 0;

  final private static long REMEMBER_SUCCESS_MS = 3000;
  
  protected static AESemaphore class_sem = new AESemaphore("PasswordWindow");

	private static PasswordWindow window = null;

  public static boolean showPasswordWindow(final Display display) {
  	if (lastSuccess + REMEMBER_SUCCESS_MS >= SystemTime.getCurrentTime()) {
  		return true;
  	}
  	
		final boolean bSWTThread = display.getThread() == Thread.currentThread ();
		display.syncExec(new AERunnable() {
			public void runSupport() {
				if (window == null) {
					window = new PasswordWindow(display);
					window.open();
				} else {
					window.shell.setVisible(true);
					window.shell.forceActive();
				}

				if (bSWTThread) {
					window.run();
				}
			}
		});

		if (!bSWTThread) {
			class_sem.reserve();
		}
		
		lastSuccess = bOk ? SystemTime.getCurrentTime() : 0;
		return bOk;
	}
  protected PasswordWindow(Display display) {
  }
  
  private void open() {
  	bOk = false;

    shell = ShellFactory.createMainShell(SWT.APPLICATION_MODAL | SWT.TITLE | SWT.CLOSE);
    shell.setText(MessageText.getString("PasswordWindow.title"));
    Utils.setShellIcon(shell);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.makeColumnsEqualWidth = true;
    shell.setLayout(layout);
    
    Label label = new Label(shell,SWT.NONE);
    label.setText(MessageText.getString("PasswordWindow.passwordprotected"));
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);
    
    final Text password = new Text(shell,SWT.BORDER);
    password.setEchoChar('*');
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    password.setLayoutData(gridData);
    
    Button ok = new Button(shell,SWT.PUSH);
    ok.setText(MessageText.getString("Button.ok"));
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
    gridData.widthHint = 70;
    ok.setLayoutData(gridData);
    shell.setDefaultButton(ok);
    ok.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				try {
					SHA1Hasher hasher = new SHA1Hasher();
					byte[] passwordText = password.getText().getBytes();
					byte[] encoded = hasher.calculateHash(passwordText);
					byte[] correct = COConfigurationManager.getByteParameter("Password",
							"".getBytes());
					boolean same = true;
					for (int i = 0; i < correct.length; i++) {
						if (correct[i] != encoded[i])
							same = false;
					}
					if (same) {
						bOk = same;
						shell.dispose();
					} else {
						close();
					}
				} catch (Exception e) {
					Debug.printStackTrace(e);
				}
			}
		});    
    
    Button cancel = new Button(shell,SWT.PUSH);
    cancel.setText(MessageText.getString("Button.cancel"));
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
    gridData.widthHint = 70;
    cancel.setLayoutData(gridData);
    cancel.addListener(SWT.Selection,new Listener() {
          /* (non-Javadoc)
           * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
           */
          public void handleEvent(Event event) {
             
             close();
          }
        });    
    
    
    shell.addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent arg0) {
      	window = null;
      	class_sem.releaseAllWaiters();
      }
    });
    
    shell.addTraverseListener(new TraverseListener() {
    	public void keyTraversed(TraverseEvent e) {
    		if (e.detail == SWT.TRAVERSE_ESCAPE) {
    			close();
    			e.doit = false;
    		}
    	}
    });
    
    shell.addListener(SWT.Close,new Listener() {
      public void handleEvent(Event arg0) {
        close();
      }
    });

    shell.pack();
        
    Utils.centreWindow(shell);

    shell.open();
  }
  
  protected void run() {
    while (!shell.isDisposed()) {
    	Display d = shell.getDisplay();
    	if (!d.readAndDispatch() && !shell.isDisposed()) {
    		d.sleep();
    	}
    }
  }
  
  private void close() {
    shell.dispose();
    if(Utils.isCarbon) {
    	UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
    	if (uiFunctions != null) {
				Shell mainShell = uiFunctions.getMainShell();
				if (mainShell != null) {
					mainShell.setMinimized(true);
					mainShell.setVisible(true);
				}
			}
    } 
  }

  public static void main(String[] args) {
  	final Display display = new Display();
		new Thread(new Runnable() {
			
			public void run() {
				System.out.println("2: " + showPasswordWindow(display));
			}
		
		}).start();
		new Thread(new Runnable() {

			public void run() {
				display.syncExec(new Runnable() {
					public void run() {
						System.out.println("3: " + showPasswordWindow(display));
					}
				});
			}

		}).start();
		display.asyncExec(new Runnable() {
			public void run() {
				System.out.println("4: " + showPasswordWindow(display));
			}
		});
		System.out.println("1: " + showPasswordWindow(display));
	}
}
