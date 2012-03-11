/*
 * Created on Mar 6, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package org.gudy.azureus2.ui.swt.update;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.update.Update;
import org.gudy.azureus2.plugins.update.UpdateCheckInstance;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderAdapter;
import org.gudy.azureus2.ui.swt.Utils;


public class 
SimpleInstallUI 
{
	private UpdateMonitor			monitor;
	private	UpdateCheckInstance		instance;
	
	private  boolean				cancelled;
	private ResourceDownloader		current_downloader;
	
	protected
	SimpleInstallUI(
		UpdateMonitor			_monitor,
		UpdateCheckInstance		_instance )
	{
		monitor		= _monitor;
		instance	= _instance;
		
		try{
			monitor.addDecisionHandler(_instance );
					
			Utils.execSWTThread(
				new Runnable()
				{
					public void
					run()
					{
						try{
							build();
							
						}catch( Throwable e ){
							
							Debug.out( e );
							
							instance.cancel();
						}
					}
				});
		}catch( Throwable e ){
			
			Debug.out( e );
			
			instance.cancel();
		}
	}
	
	protected void
	build()
	{
		Composite parent = (Composite)instance.getProperty( UpdateCheckInstance.PT_UI_PARENT_SWT_COMPOSITE );
		
		if ( parent != null ){
			
			if (parent.isDisposed()) {
				throw( new RuntimeException( "cancelled" ));
			}
			
			build( parent );
			
		}else{
			
			throw( new RuntimeException( "borkeroo" ));
		}
	}
	
	protected void
	build(
		Composite		parent )
	{
		parent.setLayout(new FormLayout());
				
		Button cancel_button = new Button( parent, SWT.NULL );

		cancel_button.setText( "Cancel" );
		
		cancel_button.addListener(
				SWT.Selection, 
				new Listener() 
				{
					public void 
					handleEvent(
						Event arg0 ) 
					{
						synchronized( SimpleInstallUI.this ){
						
							cancelled = true;

							if ( current_downloader != null ){
								
								current_downloader.cancel();
							}
						}
						
						instance.cancel();
					}
				});
			
		FormData	data = new FormData();
		data.right 	= new FormAttachment(100,0);
		data.top	= new FormAttachment(0,0);
		data.bottom	= new FormAttachment(100,0);

		cancel_button.setLayoutData( data );
			
		final Label label = new Label(parent, SWT.NULL );
		
		label.setText( "blah blah " );
		
		data = new FormData();
		data.left 	= new FormAttachment(0,0);
		data.top	= new FormAttachment(cancel_button,0, SWT.CENTER);

		label.setLayoutData( data );

		final ProgressBar progress = new ProgressBar(parent, SWT.NULL );
		
		progress.setMinimum( 0 );
		progress.setMaximum( 100 );
		progress.setSelection( 0 );
		
		
		data = new FormData();
		data.left 	= new FormAttachment(label,4);
		data.top	= new FormAttachment(cancel_button, 0, SWT.CENTER);
		data.right	= new FormAttachment(cancel_button,-4);

		progress.setLayoutData( data );
		
		parent.layout( true, true );
		
		new AEThread2( "SimpleInstallerUI", true )
		{
			public void
			run()
			{
				try{
					Update[] updates = instance.getUpdates();

					for ( Update update: updates ){
						
						String	name = update.getName();
						
						int	pos = name.indexOf('/');
						
						if ( pos >= 0 ){
							
							name = name.substring( pos+1 );
						}
						
						setLabel( name );

						ResourceDownloader[] downloaders = update.getDownloaders();
						
						for ( ResourceDownloader downloader: downloaders ){
							
							synchronized( SimpleInstallUI.this ){
								
								if ( cancelled ){
									
									return;
								}
								
								current_downloader = downloader;
							}
							
							setProgress( 0 );
							
							downloader.addListener(
								new ResourceDownloaderAdapter()
								{
									public void
									reportPercentComplete(
										ResourceDownloader	downloader,
										int					percentage )
									{
										setProgress( percentage );
									}
									
									public void
									reportAmountComplete(
										ResourceDownloader	downloader,
										long				amount )
									{
										
									}
								});
							
							downloader.download();
						}
					}
					
					boolean	restart_required = false;
					
					for (int i=0;i<updates.length;i++){
		
						if ( updates[i].getRestartRequired() == Update.RESTART_REQUIRED_YES ){
							
							restart_required = true;
						}
					}
					
					if ( restart_required ){
						
						monitor.handleRestart();
					}
				}catch( Throwable e ){
					
					Debug.out( "Install failed", e );
					
					instance.cancel();
				}
			}
			
			protected void
			setLabel(
				final String		str )
			{
				Utils.execSWTThread(
					new Runnable()
					{
						public void
						run()
						{
							if (label != null && !label.isDisposed()) {
								label.setText( str );
								label.getParent().layout();
							}
						}
					});
			}
			
			protected void
			setProgress(
				final int		percent )
			{
				Utils.execSWTThread(
					new Runnable()
					{
						public void
						run()
						{
							if (progress != null && !progress.isDisposed()) {
								progress.setSelection( percent );
							}
						}
					});
			}
		}.start();
	}
}
