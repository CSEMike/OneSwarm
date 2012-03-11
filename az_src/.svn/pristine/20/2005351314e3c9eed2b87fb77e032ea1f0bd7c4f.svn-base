/*
 * Created on 3 mai 2004
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
 * 8 Alle Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.mainwindow;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.gudy.azureus2.core3.internat.MessageText;


/**
 * @author Olivier Chalouhi
 *
 */
public class ClipboardCopy {
  
  public static void
  copyToClipBoard(
    String    data )
  {
	  new Clipboard(SWTThread.getInstance().getDisplay()).setContents(
			  new Object[] {data.replaceAll("\\x00", " " )  }, 
			  new Transfer[] {TextTransfer.getInstance()});
  }
  
  public static void
  addCopyToClipMenu(
	final Control				control,
	final copyToClipProvider	provider )
  {
	  control.addMouseListener(
		  new MouseAdapter()
		  {
			  public void 
			  mouseDown(
				 MouseEvent e ) 
			  {
				  if ( control.isDisposed()){
					  
					  return;
				  }
				  
				  final String	text = provider.getText();
				  
				  if ( control.getMenu() != null || text == null || text.length() == 0 ){

					  return;
				  }

				  if (!(e.button == 3 || (e.button == 1 && e.stateMask == SWT.CONTROL))){

					  return;
				  }

				  final Menu menu = new Menu(control.getShell(),SWT.POP_UP);

				  MenuItem   item = new MenuItem( menu,SWT.NONE );

				  item.setText( MessageText.getString( "ConfigView.copy.to.clipboard.tooltip"));

				  item.addSelectionListener(
						  new SelectionAdapter()
						  {
							  public void 
							  widgetSelected(
									  SelectionEvent arg0) 
							  {
								  new Clipboard(control.getDisplay()).setContents(new Object[] {text}, new Transfer[] {TextTransfer.getInstance()});
							  }
						  });

				  control.setMenu( menu );

				  menu.addMenuListener(
						  new MenuAdapter()
						  {
							  public void 
							  menuHidden(
									  MenuEvent arg0 )
							  {
								  if ( control.getMenu() == menu ){
								  
									  control.setMenu( null );
								  }
							  }
						  });

				  menu.setVisible( true );
			  }
		  });
  }
  
  public interface
  copyToClipProvider
  {
	  public String
	  getText();
  }
}
