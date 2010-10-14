/*
 * Created on 16 July 2006
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
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.UIInputValidator;
import org.gudy.azureus2.ui.swt.components.ControlUtils;
import org.gudy.azureus2.ui.swt.pluginsimpl.AbstractUISWTInputReceiver;
import org.eclipse.swt.widgets.MessageBox;

/**
 * @author amc1
 * Based on CategoryAdderWindow.
 */
public class SimpleTextEntryWindow extends AbstractUISWTInputReceiver {
	
	/**
	 * This is here just to make it more straight-forward for code that wants
	 * to add a validator, without having to import a plugin API interface.
	 */
	public abstract static class Validator implements UIInputValidator {}
	
	private Display display;
	
	public SimpleTextEntryWindow(final Display display) {
		this.display = display;
	}
	
	protected void promptForInput() {
		final Shell shell = org.gudy.azureus2.ui.swt.components.shell.ShellFactory.createShell(Utils.findAnyShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);

		if (this.title != null) {
			shell.setText(this.title);
		}
	    
		Utils.setShellIcon(shell);
		
	    GridLayout layout = new GridLayout();
	    shell.setLayout(layout);
	    
	    // Default width hint is 330.
	    int width_hint = (this.width_hint == -1) ? 330 : this.width_hint;
	    
	    // Process any messages.
	    Label label = null;
	    GridData gridData = null;
	    for (int i=0; i<this.messages.length; i++) {
	    	label = new Label(shell, SWT.NONE);
	    	label.setText(this.messages[i]);
	    	
	    	// 330 is the current default width.
	    	gridData = new GridData();
	    	gridData.widthHint = width_hint;
		    label.setLayoutData(gridData);
	    }

	    // We may, at a later date, allow more customisable behaviour w.r.t. to this.
	    // (e.g. "Should we wrap this, should we provide H_SCROLL capabilities" etc.)
	    int text_entry_flags = SWT.BORDER;
	    if (this.multiline_mode) {
	    	text_entry_flags |= SWT.MULTI | SWT.V_SCROLL | SWT.WRAP; 
	    }
	    else {
	    	text_entry_flags |= SWT.SINGLE;
	    }
	    
	    // Create Text object with pre-entered text.
	    final Text text_entry = new Text(shell, text_entry_flags);
	    if (this.preentered_text != null) {
	    	text_entry.setText(this.preentered_text);
	    	if (this.select_preentered_text) {
	    		text_entry.selectAll();
	    	}
	    }
	    
	    // TAB will take them out of the text entry box.
	    text_entry.addTraverseListener(new TraverseListener() {
	    	public void keyTraversed(TraverseEvent e) {
	    		if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
	    			e.doit = true;
	    		}
	    	}
	    });
	    
	    // Default behaviour - single mode results in default height of 1 line,
	    // multiple lines has default height of 3.
	    int line_height = this.line_height;
	    if (line_height == -1) {
	    	line_height = (this.multiline_mode) ? 3 : 1;
	    }
	    
	    gridData = new GridData();
	    gridData.widthHint = width_hint;
	    gridData.minimumHeight = text_entry.getLineHeight() * line_height;
	    gridData.heightHint = gridData.minimumHeight;
	    text_entry.setLayoutData(gridData);

	    Composite panel = new Composite(shell, SWT.NULL);
	    final RowLayout rLayout = new RowLayout();
	    rLayout.marginTop = 0;
	    rLayout.marginLeft = 0;
	    rLayout.marginBottom = 0;
	    rLayout.marginRight = 0;
	    try {
	    	rLayout.fill = true;
	    } catch (NoSuchFieldError e) {
	    	// SWT 2.x
	    }
	    rLayout.spacing = ControlUtils.getButtonMargin();
	    panel.setLayout(rLayout);
	    gridData = new GridData();
	    gridData.horizontalAlignment = (Constants.isOSX) ? SWT.END : SWT.CENTER;
	    panel.setLayoutData(gridData);

	    Button ok = createAlertButton(panel, "Button.ok");
	    Button cancel = createAlertButton(panel, "Button.cancel");

	    ok.addListener(SWT.Selection, new Listener() {
	    	
	    	private void showError(String text) {
	    		  String error_title = SimpleTextEntryWindow.this.title;
	    		  if (error_title == null) {error_title = "";}

	    		  MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
	    		  mb.setText(error_title);
	    		  mb.setMessage(text);
	    		  mb.open();
	    	}
	    	
	      /* (non-Javadoc)
	       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
	       */
	      public void handleEvent(Event event) {
	    	  try {
	    		  String entered_data = text_entry.getText();
	    		  if (!SimpleTextEntryWindow.this.maintain_whitespace) {
	    			  entered_data = entered_data.trim();
	    		  }
	    		  
    		  
	    		  
	    		  if (!SimpleTextEntryWindow.this.allow_empty_input && entered_data.length() == 0) {
	    			  showError(MessageText.getString("UI.cannot_submit_blank_text"));
	    			  return;
	    		  }
	    		  
	    		  UIInputValidator validator = SimpleTextEntryWindow.this.validator;
	    		  if (validator != null) {
	    			  String validate_result = validator.validate(entered_data);
	    			  if (validate_result != null) {
		    			  showError(MessageText.getString(validate_result));
		    			  return;	    				  
	    			  }
	    		  }
	    		  SimpleTextEntryWindow.this.recordUserInput(entered_data);
	    	  }
	    	  catch (Exception e) {
	    		  Debug.printStackTrace(e);
	    		  SimpleTextEntryWindow.this.recordUserAbort();
	    	  }
	    	  shell.dispose();
	      }
	    });
	
	    cancel.addListener(SWT.Selection, new Listener() {
	        /* (non-Javadoc)
	         * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
	         */
	        public void handleEvent(Event event) {
	        	SimpleTextEntryWindow.this.recordUserAbort();
	            shell.dispose();
	        }
	      });

	    shell.setDefaultButton(ok);
	    
		shell.addListener(SWT.Traverse, new Listener() {	
			public void handleEvent(Event e) {
				if ( e.character == SWT.ESC){
					SimpleTextEntryWindow.this.recordUserAbort();
					shell.dispose();
				}
			}
		});
		
	    shell.pack();
	    Utils.createURLDropTarget(shell, text_entry);
	    Utils.centreWindow(shell);
	    shell.open();
	    while (!shell.isDisposed())
	      if (!display.readAndDispatch()) display.sleep();
	  }

  private static Button createAlertButton(final Composite panel, String localizationKey)
  {
      final Button button = new Button(panel, SWT.PUSH);
      button.setText(MessageText.getString(localizationKey));
      final RowData rData = new RowData();
      rData.width = Math.max(
              ControlUtils.getDialogButtonMinWidth(),
              button.computeSize(SWT.DEFAULT,  SWT.DEFAULT).x
        );
      button.setLayoutData(rData);
      return button;
  }

}
