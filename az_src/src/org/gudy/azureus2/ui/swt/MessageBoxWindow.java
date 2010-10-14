/*
 * Created on 02-Oct-2005
 * Created by Paul Gardner
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.components.BufferedLabel;

import com.aelitis.azureus.ui.common.RememberedDecisionsManager;



public class 
MessageBoxWindow 
{
	public static final String ICON_ERROR 		= "error";
	public static final String ICON_WARNING 	= "warning";
	public static final String ICON_INFO	 	= "info";

	public static int 
	open(
		String	id,
		int		options,
		int		remember_map,
		boolean	default_is_yes,
		Display display,
		String	icon,
		String	title,
		String	message ) 
	{
		int remembered = RememberedDecisionsManager.getRememberedDecision(id,
				remember_map);
		
		if ( remembered > 0 ){
			
			return( remembered );
		}
		
		return( new MessageBoxWindow( id, options, remember_map != SWT.NULL, default_is_yes, display, icon, title, message ).getResult());
	}
  
	private Shell shell;
	
	private AESemaphore	result_sem = new AESemaphore( "MessageBoxWindow" );
	
	private volatile int			result;
	private volatile boolean		result_set;
	
	protected 
	MessageBoxWindow(
		final String	id,
		final int		options,
		final boolean	remember_decision,
		final boolean	default_is_yes,
		final Display 	display,
		final String	icon,
		final String	title,
		final String	message )
	{	
		shell = new Shell(display,SWT.APPLICATION_MODAL | SWT.TITLE | SWT.CLOSE );

		shell.setText( title );
		
		Utils.setShellIcon(shell);
		
	    GridLayout layout = new GridLayout();
	    layout.numColumns = 3;
	    shell.setLayout(layout);
	    
	    	// image and text
	    
	    Label label = new Label(shell,SWT.NONE);

	    Image image = ImageRepository.getImage(icon);

	    if ( image != null ){
	    	
	    	label.setImage( image );
	    }
	    
	    	// buffered label handles & in the text properly
	    
	    BufferedLabel	msg_label = new BufferedLabel(shell,SWT.WRAP);
	    msg_label.setText(message);
	    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
	    gridData.horizontalSpan = 2;
	    msg_label.setLayoutData(gridData);
	    
	    	// remember decision
	    
	    final Button checkBox;
	    
	    if ( remember_decision ){
	    	
		    checkBox = new Button(shell, SWT.CHECK);
		    checkBox.setSelection(false);
		    checkBox.setText( MessageText.getString( "MessageBoxWindow.rememberdecision" ));
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan = 3;
			checkBox.setLayoutData(gridData);
	    }else{
	    	checkBox = null;
	    	Label	pad = new Label( shell, SWT.NULL );
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan = 3;
			pad.setLayoutData(gridData);
	    }

	    
			// line
		
		Label labelSeparator = new Label(shell,SWT.SEPARATOR | SWT.HORIZONTAL);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		labelSeparator.setLayoutData(gridData);

			// buttons
			
		label = new Label(shell,SWT.NULL);

		final int yes_option = options & ( SWT.OK | SWT.YES );
		
		Button bYes = new Button(shell,SWT.PUSH);
	 	bYes.setText(MessageText.getString( yes_option==SWT.YES?"Button.yes":"Button.ok"));
	 	gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.HORIZONTAL_ALIGN_FILL);
	 	gridData.grabExcessHorizontalSpace = true;
	 	gridData.widthHint = 70;
	 	bYes.setLayoutData(gridData);
	 	bYes.addListener(SWT.Selection,new Listener() {
	  		public void handleEvent(Event e) {
	  			setResult( id, yes_option, checkBox==null?false:checkBox.getSelection());
	   		}
		 });
    
		final int no_option = options & ( SWT.CANCEL | SWT.NO );

	 	Button bNo = new Button(shell,SWT.PUSH);
	 	bNo.setText(MessageText.getString(no_option==SWT.NO?"Button.no":"Button.cancel"));
	 	gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
	 	gridData.grabExcessHorizontalSpace = false;
	 	gridData.widthHint = 70;
	 	bNo.setLayoutData(gridData);    
	 	bNo.addListener(SWT.Selection,new Listener() {
	 		public void handleEvent(Event e) {
	 			setResult( id, no_option, checkBox==null?false:checkBox.getSelection());
	   		}
	 	});
	 	
		shell.setDefaultButton( default_is_yes?bYes:bNo );
		
		shell.addListener(SWT.Traverse, new Listener() {	
			public void handleEvent(Event e) {
				if ( e.character == SWT.ESC){
					setResult( id, SWT.NULL, false );
				}
			}
		});
    
	    shell.addListener(
	    	SWT.Close,
	    	new Listener() 
	    	{
	    		public void 
	    		handleEvent(
	    			Event arg0) 
	    		{
	    			setResult( id, SWT.NULL, false );
	    		}
	    	});
    
		
	 	shell.pack ();
	 	
		Utils.centreWindow( shell );
        
	    shell.open();
	    
	    (default_is_yes?bYes:bNo).setFocus();
	    
	    while( !shell.isDisposed()) {
	       
	    	if (!display.readAndDispatch()){
	    		
	              display.sleep();
	        }
	    }
	}      

	protected void
	setResult(
		String		id,
		int			option,
		boolean		remember )
	{
		if ( !result_set ){
			
			result	= option;
			
			result_set	= true;
			
			if ( remember ){
				
				RememberedDecisionsManager.setRemembered(id, result);
			}
			
			result_sem.release();
			
			close();
		}
	}
	
	protected void
	close()
	{
		if ( !shell.isDisposed()){
			
			shell.dispose();
		}
	}
	
	protected int
	getResult()
	{
		result_sem.reserve();
		
		return( result );
	}
}
