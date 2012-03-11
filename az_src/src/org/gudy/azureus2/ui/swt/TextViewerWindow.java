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

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;

public class TextViewerWindow {
  private Shell shell;
  private Text txtInfo;
  
  private List<TextViewerWindowListener> listeners = new ArrayList<TextViewerWindowListener>();
  
  public 
  TextViewerWindow(
		 String sTitleID, String sMessageID, String sText )
  {
	  this( sTitleID, sMessageID, sText, true );
  }
  
  public 
  TextViewerWindow(
		 String sTitleID, String sMessageID, String sText, boolean modal ) 
  {
    final Display display = SWTThread.getInstance().getDisplay();
    
    if ( modal ){
    
    	shell = org.gudy.azureus2.ui.swt.components.shell.ShellFactory.createShell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE );
    	
    }else{
    	
    	shell = ShellFactory.createMainShell(SWT.DIALOG_TRIM | SWT.RESIZE );
    }
    
    if (sTitleID != null) shell.setText(MessageText.keyExists(sTitleID)?MessageText.getString(sTitleID):sTitleID);
    
    Utils.setShellIcon(shell);
    
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    shell.setLayout(layout);

    Label label = new Label(shell, SWT.NONE);
    if (sMessageID != null) label.setText(MessageText.keyExists(sMessageID)?MessageText.getString(sMessageID):sMessageID);
    GridData gridData = new GridData(  GridData.FILL_HORIZONTAL );
    gridData.widthHint = 200;
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);

    txtInfo = new Text(shell, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
    gridData = new GridData(  GridData.FILL_BOTH );
    gridData.widthHint = 500;
    gridData.heightHint = 400;
    gridData.horizontalSpan = 2;
    txtInfo.setLayoutData(gridData);
    txtInfo.setText(sText);

    label = new Label(shell, SWT.NONE);
    gridData = new GridData( GridData.FILL_HORIZONTAL );
    label.setLayoutData(gridData);
    
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
	
	shell.addDisposeListener(
		new DisposeListener()
		{
			public void 
			widgetDisposed(
				DisposeEvent arg0) 
			{
				for ( TextViewerWindowListener l: listeners ){
					
					l.closed();
				}
			}
		});
	
    shell.pack();
	Utils.centreWindow( shell );
    shell.open();
    
    if ( modal ){
    	while (!shell.isDisposed())
    		if (!display.readAndDispatch()) display.sleep();
    }
  }
  
  public void
  append(
	String	str )
  {
	  txtInfo.setText( txtInfo.getText() + str );
	  
	  txtInfo.setSelection( txtInfo.getTextLimit());
  }
  
  public String
  getText()
  {
	  return( txtInfo.getText());
  }
  
  public void
  setEditable(
	boolean	editable )
  {
	  txtInfo.setEditable( editable );
  }
  public void
  addListener(
	 TextViewerWindowListener		l )
  {
	  listeners.add( l );
  }
  
  public boolean
  isDisposed()
  {
	  return( shell.isDisposed());
  }
  
  public void
  close()
  {
	  if ( !shell.isDisposed()){
		  
		  shell.dispose();
	  }
  }
  public interface
  TextViewerWindowListener
  {
	  public void
	  closed();
  }
}
