/*
 * Created on 2 feb. 2004
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;

public class TextViewerWindow {
  public TextViewerWindow(String sTitleID, String sMessageID, String sText) {
    final Display display = SWTThread.getInstance().getDisplay();
    final Shell shell = org.gudy.azureus2.ui.swt.components.shell.ShellFactory.createShell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);

    if (sTitleID != null) shell.setText(MessageText.keyExists(sTitleID)?MessageText.getString(sTitleID):sTitleID);
    
    Utils.setShellIcon(shell);
    
    GridLayout layout = new GridLayout();
    shell.setLayout(layout);

    Label label = new Label(shell, SWT.NONE);
    if (sMessageID != null) label.setText(MessageText.keyExists(sMessageID)?MessageText.getString(sMessageID):sMessageID);
    GridData gridData = new GridData();
    gridData.widthHint = 200;
    label.setLayoutData(gridData);

    final Text txtInfo = new Text(shell, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
    gridData = new GridData();
    gridData.widthHint = 300;
    txtInfo.setLayoutData(gridData);
    txtInfo.setText(sText);

    Button ok = new Button(shell, SWT.PUSH);
    ok.setText(MessageText.getString("Button.ok"));
    gridData = new GridData();
    gridData.widthHint = 70;
    ok.setLayoutData(gridData);
    shell.setDefaultButton(ok);
    ok.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        try {
        	shell.dispose();
        }
        catch (Exception e) {
        	Debug.printStackTrace( e );
        }
      }
    });

	shell.addListener(SWT.Traverse, new Listener() {	
		public void handleEvent(Event e) {
			if ( e.character == SWT.ESC){
				shell.dispose();
			}
		}
	});
	
    shell.pack();
	Utils.centreWindow( shell );
    shell.open();
    while (!shell.isDisposed())
      if (!display.readAndDispatch()) display.sleep();
  }
}
