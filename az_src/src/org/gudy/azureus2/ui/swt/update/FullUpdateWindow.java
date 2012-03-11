/**
 * Created on June 29th, 2009
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package org.gudy.azureus2.ui.swt.update;


import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

import com.aelitis.azureus.ui.UIFunctions;


public class FullUpdateWindow
{
	private static Shell current_shell = null;

	private static Browser browser;

	private static BrowserFunction browserFunction;

	public static void 
	handleUpdate(
		final String						url,
		final UIFunctions.actionListener	listener )
	{
		try{
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					open( url, listener );
				}
			});
			
		}catch( Throwable e ){
			
			Debug.out( e );
			
			listener.actionComplete( false );
		}
	}

	public static void 
	open(
		final String 						url,
		final UIFunctions.actionListener	listener ) 
	{
		boolean	ok = false;
		
		final boolean[] listener_informed = { false };
		
		try{
			if ( current_shell != null && !current_shell.isDisposed()){
				
				return;
			}
			
			final Shell parentShell = Utils.findAnyShell();
			
			final Shell shell = current_shell = 
				ShellFactory.createShell(parentShell, SWT.BORDER | SWT.APPLICATION_MODAL | SWT.TITLE | SWT.DIALOG_TRIM );
			
			shell.setLayout(new FillLayout());
			
			if (parentShell != null) {
				parentShell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
			}
			
			shell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					try{
						if (parentShell != null) {
							parentShell.setCursor(e.display.getSystemCursor(SWT.CURSOR_ARROW));
						}
						if (browserFunction != null && !browserFunction.isDisposed()) {
							browserFunction.dispose();
						}
						current_shell = null;
						
					}finally{
						
						if ( !listener_informed[0] ){
						
							try{
								listener.actionComplete( false );
								
							}catch( Throwable f ){
								
								Debug.out( f );
							}
						}
					}
				}
			});
	
			browser = Utils.createSafeBrowser(shell, SWT.NONE);
			if (browser == null) {
				shell.dispose();
				return;
			}
	
			browser.addTitleListener(new TitleListener() {
				public void changed(TitleEvent event) {
					if (shell == null || shell.isDisposed()) {
						return;
					}
					shell.setText(event.title);
				}
			});
	
			browserFunction = new BrowserFunction(browser, "sendVuzeUpdateEvent") {
				private String last = null;

				public Object function(Object[] arguments) {

					if (shell == null || shell.isDisposed()) {
						return null;
					}
					
					if (arguments == null) {
						Debug.out("Invalid sendVuzeUpdateEvent null ");
						return null;
					}
					if (arguments.length < 1) {
						Debug.out("Invalid sendVuzeUpdateEvent length " + arguments.length + " not 1");
						return null;
					}
					if (!(arguments[0] instanceof String)) {
						Debug.out("Invalid sendVuzeUpdateEvent "
								+ (arguments[0] == null ? "NULL"
										: arguments.getClass().getSimpleName()) + " not String");
						return null;
					}

					String text = ((String) arguments[0]).toLowerCase();
					if (last  != null && last.equals(text)) {
						return null;
					}
					last = text;
					if ( text.contains("page-loaded")) {
						
						Utils.centreWindow(shell);
						if (parentShell != null) {
							parentShell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
						}
						shell.open();
						
					} else if (text.startsWith("set-size")){
						
						String[] strings = text.split(" ");
						
						if (strings.length > 2){
							try {
								
								int w = Integer.parseInt(strings[1]);
								int h = Integer.parseInt(strings[2]);

								Rectangle computeTrim = shell.computeTrim(0, 0, w, h);
								shell.setSize(computeTrim.width, computeTrim.height);
								
							} catch (Exception e) {
							}
						}
					}else if ( text.contains( "decline" ) || text.contains( "close" )){
						
						Utils.execSWTThreadLater(0, new AERunnable() {	
							public void runSupport() {
								shell.dispose();
							}
						});
						
					}else if ( text.contains("accept")){
						
						Utils.execSWTThreadLater(0, new AERunnable() {	
							public void runSupport(){
								
								listener_informed[0] = true;
								
								try{
									listener.actionComplete( true );
									
								}catch( Throwable e ){
									
									Debug.out( e );
								}
								
								shell.dispose();
							}
						});
					}
					return null;
				}
			};

			browser.addStatusTextListener(new StatusTextListener() {
				public void changed(StatusTextEvent event) {
					browserFunction.function(new Object[] {
						event.text
					});
				}
			});

			browser.addLocationListener(new LocationListener() {
				public void changing(LocationEvent event) {
				}
	
				public void changed(LocationEvent event) {
				}
			});
	
			String final_url = url + ( url.indexOf('?')==-1?"?":"&") + 
						"locale=" + MessageText.getCurrentLocale().toString() + 
						"&azv=" + Constants.AZUREUS_VERSION; 
				
			SimpleTimer.addEvent(
				"fullupdate.pageload", 
				SystemTime.getOffsetTime(5000),
				new TimerEventPerformer() {
					public void perform(TimerEvent event) {
						Utils.execSWTThread(new AERunnable() {
							public void runSupport() {
								if ( !shell.isDisposed()){
								
									shell.open();
								}
							}
						});
					}
				});
			
			browser.setUrl(final_url);
			
			ok = true;
			
		}finally{
			
			if ( !ok ){
				
				try{
					listener.actionComplete( false );
					
				}catch( Throwable f ){
					
					Debug.out( f );
				}
			}
		}
	}

	public static void 
	main(String[] args) 
	{
		try {
			open( 
				"http://192.168.0.88:8080/client/Update.html", 
				new UIFunctions.actionListener()
				{
					public void actionComplete(Object result) {
						System.out.println( "result=" + result );
						
						//System.exit(1);
					}
				});
		} catch (Exception e) {
			e.printStackTrace();
		}
		Display d = Display.getDefault();
		while (true) {
			if (!d.readAndDispatch()) {
				d.sleep();
			}
		}
	}

}