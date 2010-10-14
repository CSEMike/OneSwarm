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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreOperation;
import com.aelitis.azureus.core.AzureusCoreOperationListener;

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
					if ( 	operation.getOperationType() == AzureusCoreOperation.OP_FILE_MOVE &&
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
	
	private volatile Shell 		shell;
	private volatile boolean 	task_complete;
	
	public 
	ProgressWindow(
		final AzureusCoreOperation	operation )
	{
		final RuntimeException[] error = {null};
		
		new DelayedEvent( 
				"ProgWin",
				1000,
				new AERunnable()
				{
					public void
					runSupport()
					{
						synchronized( this ){
							
							if ( !task_complete ){
								
								Utils.execSWTThread(
									new Runnable()
									{
										public void
										run()
										{
											synchronized( this ){
												
												if ( !task_complete ){
											
													showDialog();
												}
											}
										}
									},
									false );
							}
						}
					}
				});
		
		new AEThread( "ProgressWindow", true )
		{
			public void 
			runSupport()
			{
				try{	
					// Thread.sleep(10000);
					
					operation.getTask().run( operation );
					
				}catch( RuntimeException e ){
					
					error[0] = e;
					
				}catch( Throwable e ){
		
					error[0] = new RuntimeException( e );
					
				}finally{
					
					synchronized( this ){
						
						task_complete = true;
						
						Utils.execSWTThread( new Runnable(){public void run(){}}, true );
					}			
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
			
			synchronized( this ){
				
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
	
	protected void
	showDialog()
	{	
		shell = org.gudy.azureus2.ui.swt.components.shell.ShellFactory.createMainShell(
				( SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL ));

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

		InputStream	is = ImageRepository.getImageAsStream( "working" );
		
		if ( is == null ){
			
			new Label( shell, SWT.NULL );
			
		}else{
			
			final ImageLoader loader = new ImageLoader();
			
			final Color	background = shell.getBackground();

		    loader.load( is );
		    		    
		    final Canvas	canvas =
		    	new Canvas( shell, SWT.NULL )
		    	{
		    		public Point computeSize(int wHint, int hHint,boolean changed )
		    		{
		    			return( new Point(loader.logicalScreenWidth,loader.logicalScreenWidth));
		    		}
		    	};
		    			    		    
		    final GC canvas_gc = new GC( canvas );

	        new AEThread("GifAnim", true )
	        {
	        	private Image	image;
	        	private boolean useGIFBackground;
	        	
	        	public void 
	        	runSupport()
	        	{
	        		Display display = shell.getDisplay();  
	        	
	        		ImageData[]	image_data = loader.data;
	        		
	        			/* Create an off-screen image to draw on, and fill it with the shell background. */
	        		
	        		Image offScreenImage = new Image(display, loader.logicalScreenWidth, loader.logicalScreenHeight);
	        		
	        		GC offScreenImageGC = new GC(offScreenImage);
	        		
	        		offScreenImageGC.setBackground(background);
	        		
	        		offScreenImageGC.fillRectangle(0, 0, loader.logicalScreenWidth, loader.logicalScreenHeight);

	        		try{
	        				/* Create the first image and draw it on the off-screen image. */
	        			
	        			int imageDataIndex = 0;
	        			
	        			ImageData imageData = image_data[imageDataIndex];
	        			
	        			if (image != null && !image.isDisposed()) image.dispose();
	        			
	        			image = new Image(display, imageData);
	        			
	        			offScreenImageGC.drawImage(
	        					image,
	        					0,
	        					0,
	        					imageData.width,
	        					imageData.height,
	        					imageData.x,
	        					imageData.y,
	        					imageData.width,
	        					imageData.height);

		        			/* Now loop through the images, creating and drawing each one
		        			 * on the off-screen image before drawing it on the shell. */
		        			
	        			int repeatCount = loader.repeatCount;
	        			
	        			while ( !task_complete && loader.repeatCount == 0 || repeatCount > 0) {
	        				
	        				switch (imageData.disposalMethod){
	        				
		        				case SWT.DM_FILL_BACKGROUND:
		        					
		        						/* Fill with the background color before drawing. */
		        					
		        					Color bgColor = null;
		        					
		        					if (useGIFBackground && loader.backgroundPixel != -1) {
		        						
		        						bgColor = new Color(display, imageData.palette.getRGB(loader.backgroundPixel));
		        					}
		        					
		        					offScreenImageGC.setBackground(bgColor != null ? bgColor : background);
		        					
		        					offScreenImageGC.fillRectangle(imageData.x, imageData.y, imageData.width, imageData.height);
		        					
		        					if (bgColor != null) bgColor.dispose();
		        					
		        					break;
		        					
		        				case SWT.DM_FILL_PREVIOUS:
		        					
		        						/* Restore the previous image before drawing. */
		        					
		        					offScreenImageGC.drawImage(
		        							image,
		        							0,
		        							0,
		        							imageData.width,
		        							imageData.height,
		        							imageData.x,
		        							imageData.y,
		        							imageData.width,
		        							imageData.height);
		        					break;
	        				}

	        				imageDataIndex = (imageDataIndex + 1) % image_data.length;
	        				
	        				imageData = image_data[imageDataIndex];
	        				
	        				image.dispose();
	        				
	        				image = new Image(display, imageData);
	        				
	        				offScreenImageGC.drawImage(
	        						image,
	        						0,
	        						0,
	        						imageData.width,
	        						imageData.height,
	        						imageData.x,
	        						imageData.y,
	        						imageData.width,
	        						imageData.height);

	        					/* Draw the off-screen image to the shell. */
	        				
	        				canvas_gc.drawImage(offScreenImage, 0, 0);

	        					/* Sleep for the specified delay time (adding commonly-used slow-down fudge factors). */
	        				
	        				try {
	        					int ms = imageData.delayTime * 10;
	        					if (ms < 20) ms += 30;
	        					if (ms < 30) ms += 10;
	        					
	        					Thread.sleep(ms);
	        				} catch (InterruptedException e) {
	        				}

	        					/* If we have just drawn the last image, decrement the repeat count and start again. */
	        				
	        				if (imageDataIndex == image_data.length - 1) repeatCount--;
	        			}
	        		} catch (SWTException ex) {
	        			ex.printStackTrace();
	        		} finally {
	        			if (offScreenImage != null && !offScreenImage.isDisposed()) offScreenImage.dispose();
	        			if (offScreenImageGC != null && !offScreenImageGC.isDisposed()) offScreenImageGC.dispose();
	        			if (image != null && !image.isDisposed()) image.dispose();
	        		}
	        	}
	        }.start();
		}
		
		
		Label label = new Label(shell, SWT.NONE);
		label.setText(MessageText.getString( "progress.window.msg.filemove" ));
		GridData gridData = new GridData();
		label.setLayoutData(gridData);

		shell.pack();
		
		Utils.centreWindow( shell );

		shell.open();
	}
}
