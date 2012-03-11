/*
 * Created on 27 Jul 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.ui.swt.progress;

import java.io.InputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreOperation;
import com.aelitis.azureus.core.AzureusCoreOperationListener;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

public class 
ProgressWindow 
{
	public static void
	register(
		AzureusCore		core )
	{
		core.addOperationListener(
			new AzureusCoreOperationListener()
			{
				public boolean
				operationCreated(
					AzureusCoreOperation	operation )
				{
					if ( 	( 	operation.getOperationType() == AzureusCoreOperation.OP_FILE_MOVE ||
								operation.getOperationType() == AzureusCoreOperation.OP_PROGRESS )&&
							Utils.isThisThreadSWT()){
												
						if ( operation.getTask() != null ){
							
							new ProgressWindow( operation );
														
							return( true );
						}
					}
					
					return( false );
				}
			});
	}
	
	private volatile Shell 			shell;
	private volatile boolean 		task_complete;
	
	private final 	String	 resource;
	private Image[] spinImages;
	protected int curSpinIndex = 0;
	
	protected 
	ProgressWindow(
		final AzureusCoreOperation	operation )
	{
		final RuntimeException[] error = {null};
		
		resource = operation.getOperationType()==AzureusCoreOperation.OP_FILE_MOVE?"progress.window.msg.filemove":"progress.window.msg.progress";

		new DelayedEvent( 
				"ProgWin",
				operation.getOperationType()==AzureusCoreOperation.OP_FILE_MOVE?1000:10,
				new AERunnable()
				{
					public void
					runSupport()
					{							
						if ( !task_complete ){
								
							Utils.execSWTThread(
								new Runnable()
								{
									public void
									run()
									{
										synchronized( ProgressWindow.this ){
											
											if ( !task_complete ){
										
												Shell shell = org.gudy.azureus2.ui.swt.components.shell.ShellFactory.createMainShell(
														( SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL ));


												showDialog( shell );
											}
										}
									}
								},
								false );
						}
					}
				});
		
		new AEThread2( "ProgressWindow", true )
		{
			public void 
			run()
			{
				try{	
					// Thread.sleep(10000);
					
					operation.getTask().run( operation );
					
				}catch( RuntimeException e ){
					
					error[0] = e;
					
				}catch( Throwable e ){
		
					error[0] = new RuntimeException( e );
					
				}finally{
					
					Utils.execSWTThread(
							new Runnable()
							{
								public void
								run()
								{
									destroy();
								}
							});
				}
			}
		}.start();
			
		try{
			final Display display = SWTThread.getInstance().getDisplay();
	
			while( !( task_complete || display.isDisposed())){
				
				if (!display.readAndDispatch()) display.sleep();
			}
		}finally{
			
				// bit of boiler plate in case something fails in the dispatch loop
			
			synchronized( ProgressWindow.this ){
				
				task_complete = true;
			}
			
			try{
				if ( shell != null && !shell.isDisposed()){
				
					shell.dispose();
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		if ( error[0] != null ){
			
			throw( error[0] );
		}
	}
	
	public
	ProgressWindow(
		Shell		_parent,
		String		_resource,
		int			_style,
		int			_delay_millis )
	{
		resource = _resource;
			
		final Shell shell = new Shell( _parent, _style );

		if ( _delay_millis <= 0 ){
		
			showDialog( shell );
			
		}else{
			
			new DelayedEvent( 
					"ProgWin",
					_delay_millis,
					new AERunnable()
					{
						public void
						runSupport()
						{								
							if ( !task_complete ){
									
								Utils.execSWTThread(
									new Runnable()
									{
										public void
										run()
										{
											synchronized( ProgressWindow.this ){
												
												if ( !task_complete ){
											
													showDialog( shell );
												}
											}
										}
									},
									false );
							}
						}
					});
		}
	}
	
	protected void
	showDialog(
		Shell		_shell )
	{	
		shell	= _shell;
		
		shell.setText( MessageText.getString( "progress.window.title" ));

		Utils.setShellIcon(shell);

		shell.addListener( 
				SWT.Close, 
				new Listener()
				{
					public void 
					handleEvent(
						org.eclipse.swt.widgets.Event event)
					{
						event.doit = false;
					}
				});
		
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		shell.setLayout(layout);

		spinImages = ImageLoader.getInstance().getImages("working");
		
		if ( spinImages.length == 0 || spinImages == null ){
			
			new Label( shell, SWT.NULL );
			
		}else{

			final Rectangle spinBounds = spinImages[0].getBounds();
		    final Canvas	canvas =
		    	new Canvas( shell, SWT.NULL )
		    	{
		    		public Point computeSize(int wHint, int hHint,boolean changed )
		    		{
		    			return( new Point(spinBounds.width, spinBounds.height));
		    		}
		    	};
		    	
		    	canvas.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent e) {
							e.gc.drawImage(spinImages[curSpinIndex ], 0, 0);
						}
					});
		    	
		    	Utils.execSWTThreadLater(100, new AERunnable() {
						public void runSupport() {
							if (canvas == null || canvas.isDisposed()) {
								return;
							}

							canvas.redraw();
							canvas.update();
							if (curSpinIndex == spinImages.length - 1) {
								curSpinIndex = 0;
							} else {
								curSpinIndex++;
							}
							Utils.execSWTThreadLater(100, this);
						}
					});
		    			    		    
		}
		
		
		Label label = new Label(shell, SWT.NONE);
				
		label.setText(MessageText.getString( resource ));
		GridData gridData = new GridData();
		label.setLayoutData(gridData);

		shell.pack();
		
		Composite parent = shell.getParent();
		
		if ( parent != null ){
			
			Utils.centerWindowRelativeTo( shell, parent );
			
		}else{
			
			Utils.centreWindow( shell );
		}
		
		shell.open();
	}
	
	public void
	destroy()
	{
		synchronized( ProgressWindow.this ){
			
			task_complete = true;
		}
		
		try{
			if ( shell != null && !shell.isDisposed()){
			
				shell.dispose();
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}

		if (spinImages != null) {
			ImageLoader.getInstance().releaseImage("working");
			spinImages =  null;
		}
	}
}
