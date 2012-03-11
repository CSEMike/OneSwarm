/*
 * Created on 01-Dec-2004
 * Created by Paul Gardner
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

package org.gudy.azureus2.ui.swt.update;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;

import java.util.ArrayList;

/**
 * @author parg
 * @deprecated This class is no longer maintained and may be removed sometime in the future; its
 * functionality has been replaced by the new implementation of ProgressReportingWindow and ProgressReporter usage. KN
 */

public class 
UpdateProgressWindow 
	implements UpdateManagerListener
{  
	public static void 
	show(
		UpdateCheckInstance[]		instances,
		Shell						shell )
	{
		if ( instances.length == 0){
			
			return;
		}
	
		new UpdateProgressWindow().showSupport(instances,shell);
	}
  
	protected Display		display;
	protected Shell			window;
	protected StyledText 	text_area;
	
	protected UpdateManager	manager;
	
	protected ArrayList		current_instances	= new ArrayList();
	
	protected void 
	showSupport(
		UpdateCheckInstance[]		instances,
		Shell						shell )
	{
	  	manager	= instances[0].getManager();
	  	
	  	display = shell.getDisplay();
	  	
	    window = org.gudy.azureus2.ui.swt.components.shell.ShellFactory.createShell(display,SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
	    Messages.setLanguageText(window,"updater.progress.window.title");
	    Utils.setShellIcon(shell);
	    FormLayout layout = new FormLayout();
	    try {
	      layout.spacing = 5;
	    } catch (NoSuchFieldError e) {
	      /* Ignore for Pre 3.0 SWT.. */
	    }
	    layout.marginHeight = 5;
	    layout.marginWidth = 5;
	    window.setLayout(layout);
	    FormData formData;
	    
	    	// text area
	    
	    text_area = new StyledText(window,SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
	       
	    text_area.setEditable(false);
	    
	    Button btnOk = new Button(window,SWT.PUSH);
	    Button btnAbort = new Button(window,SWT.PUSH);
	    
	            
	    formData = new FormData();
	    formData.left = new FormAttachment(0,0);
	    formData.right = new FormAttachment(100,0);
	    formData.top = new FormAttachment(0,0);   
	    formData.bottom = new FormAttachment(90,0);   
	    text_area.setLayoutData(formData);
	    
	    
	    	// label
	    
	    Label	info_label = new Label(window, SWT.NULL);
	    Messages.setLanguageText(info_label,"updater.progress.window.info");
	    formData = new FormData();
	    formData.top = new FormAttachment(text_area);    
	    formData.right = new FormAttachment(btnAbort);    
	    formData.left = new FormAttachment(0,0);    
	    info_label.setLayoutData( formData );    
	   
	    
	    	// abort button
	    
	    Messages.setLanguageText(btnAbort,"Button.abort");
	    formData = new FormData();
	    formData.right = new FormAttachment(btnOk);    
	    formData.bottom = new FormAttachment(100,0);    
	    formData.width = 70;
	    btnAbort.setLayoutData(formData);
	    btnAbort.addListener(
	    		SWT.Selection,
				new Listener() 
				{
				    public void 
					handleEvent(
						Event e) 
				    {
				    	manager.removeListener( UpdateProgressWindow.this  );
				    	
				    	for (int i=0;i<current_instances.size();i++){
				    		
				    		((UpdateCheckInstance)current_instances.get(i)).cancel();
				    	}
				    	
				    	window.dispose();
				   	}
				});
	    
	    	// ok button
	    
	    Messages.setLanguageText(btnOk,"Button.ok");
	    formData = new FormData();
	    formData.right = new FormAttachment(95,0);    
	    formData.bottom = new FormAttachment(100,0);    
	    formData.width = 70;
	    btnOk.setLayoutData(formData);
	    btnOk.addListener(
	    		SWT.Selection,
				new Listener() 
				{
	    			public void 
					handleEvent(
						Event e) 
	    			{
	    				manager.removeListener( UpdateProgressWindow.this  );
	    				
	    				window.dispose();
	    			}
			    });
	        
	    window.setDefaultButton( btnOk );
	    
	    window.addListener(SWT.Traverse, new Listener() {	
			public void handleEvent(Event e) {
				if ( e.character == SWT.ESC){
					
					manager.removeListener( UpdateProgressWindow.this  );
					
				    window.dispose();
				 }
			}
	    });
	    
	    manager.addListener( this );
	    
	    window.setSize(620,450);
	    window.layout();
	    
	    Utils.centreWindow( window );
	    
	    window.open();
	    
	    for (int i=0;i<instances.length;i++){
	    	
	    	addInstance( instances[i] );
	    }
	} 
	
	protected void
	log(
		UpdateCheckInstance		instance,
		String					str )
	{
		String	name = instance.getName();
		
      	if ( MessageText.keyExists(name)){
           	
           	name = MessageText.getString( name );
        }
      	
		log( name + " - " + str );
	}
	
	protected void
	log(
		UpdateChecker			checker,
		String					str )
	{
		log( "    " + checker.getComponent().getName() + " - " + str );		
	}
	
	protected void
	log(
		final String	str )
	{
		try{
			if ( !display.isDisposed()){
				
				display.asyncExec(
						new AERunnable()
						{
							public void 
							runSupport() 
							{
								if ( !text_area.isDisposed()){
									
									text_area.append( str + "\n" );
								}
							}
						});
			}
		}catch( Throwable e ){
			
		}
	}
	public void
	checkInstanceCreated(
		UpdateCheckInstance	instance )
	{
		addInstance( instance );
	}
	
	protected void
	addInstance(
		final UpdateCheckInstance	instance )
	{
		if ( !display.isDisposed()){
	
			display.asyncExec(
				new AERunnable()
				{
					public void 
					runSupport() 
					{
						if ( display.isDisposed() || window.isDisposed()){
							
							return;
						}
						
						if ( !current_instances.contains( instance )){
							
							current_instances.add( instance );
							
							log( instance, "added" );
							
							instance.addListener(
								new UpdateCheckInstanceListener()
								{
									public void
									cancelled(
										UpdateCheckInstance		instance )
									{
										log( instance, "cancelled" );
									}
									
									public void
									complete(
										UpdateCheckInstance		instance )
									{
										log( instance, "complete" );
									}
								});
							
							UpdateChecker[]	checkers = instance.getCheckers();
							
							for (int i=0;i<checkers.length;i++){
								
								final UpdateChecker	checker = checkers[i];
								
								log( checker, "added" );
								
								checker.addListener(
									new UpdateCheckerListener()
									{
										public void
										completed(
											UpdateChecker	checker )
										{
											log( checker, "completed" );
										}
											
										public void
										failed(
											UpdateChecker	checker )
										{
											log( checker, "failed" );
										}
									
										
										public void
										cancelled(
											UpdateChecker	checker )
										{
											log( checker, "cancelled" );
										}
									});
								
								checker.addProgressListener(
									new UpdateProgressListener()
									{
										public void
										reportProgress(
											String	str )
										{
											log( checker, "    " + str );
										}
									});
							}
						}
					}
				});
		}
	}
}
