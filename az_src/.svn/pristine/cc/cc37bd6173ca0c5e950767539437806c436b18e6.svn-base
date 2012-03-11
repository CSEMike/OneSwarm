/*
 * File    : AuthenticatorWindow.java
 * Created : 25-Nov-2003
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
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
 */

package org.gudy.azureus2.ui.swt.auth;

/**
 * @author parg
 *
 */

import java.util.Arrays;

import org.eclipse.swt.*;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.security.CryptoManagerFactory;
import com.aelitis.azureus.core.security.CryptoManagerPasswordHandler;

public class 
CryptoWindow 
	implements CryptoManagerPasswordHandler
{	
	private static final int DAY = 60*60*24;
	
	public
	CryptoWindow()
	{
		CryptoManagerFactory.getSingleton().addPasswordHandler( this );
	}
	
	public int
	getHandlerType()
	{
		return( HANDLER_TYPE_USER );
	}
	
	public passwordDetails 
	getPassword(
		final int 		handler_type, 
		final int 		action_type,
		final boolean	last_pw_incorrect,
		final String 	reason ) 
	{
		final Display	display = SWTThread.getInstance().getDisplay();
		
		if ( display.isDisposed()){
			
			return( null );
		}
		
		final cryptoDialog[]	dialog = new cryptoDialog[1];
		
		final AESemaphore	sem = new AESemaphore("CryptoWindowSem");

		try{
			if ( display.getThread() == Thread.currentThread()){
				
				dialog[0] = new cryptoDialog( sem, display, handler_type, action_type, last_pw_incorrect, reason );
				
				while ( !( display.isDisposed() || sem.isReleasedForever())){
					
					if ( !display.readAndDispatch()){
						
						display.sleep();
					}
				}
				
				if ( display.isDisposed()){
					
					return( null );
				}
			}else{
				display.asyncExec(
						new Runnable() 
						{
							public void
							run()
							{
								dialog[0] = new cryptoDialog( sem, display, handler_type, action_type, last_pw_incorrect, reason );
							}
						});
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			return( null );
		}
			
		sem.reserve();
			
		final char[]	pw			= dialog[0].getPassword();
		final int		persist_for	= dialog[0].getPersistForSeconds();
		
		if ( pw == null ){
			
			return( null );
		}
		
		return(
			new passwordDetails()
			{
				public char[]
	    		getPassword()
				{
					return( pw );
				}

	    		public int
	    		getPersistForSeconds()
	    		{
	    			return( persist_for );
	    		}
			});
	}
	
	public void 
	passwordOK(
		int 				handler_type, 
		passwordDetails 	details) 
	{		
	}
	
	protected class
	cryptoDialog
	{
		private AESemaphore	sem;
		private Shell		shell;
		
		private char[]		password;
		private char[]		password2;
		private	int			persist_for_secs;
		
		private boolean		verify_password;
		
		protected
		cryptoDialog(
			AESemaphore		_sem,
			Display			display,
			int				handler_type,
			int				action_type,
			boolean			last_pw_incorrect,
			String			reason )
		{
			sem	= _sem;
			
			if ( display.isDisposed()){
				
				sem.releaseForever();
				
				return;
			}
			
			shell = ShellFactory.createMainShell(SWT.DIALOG_TRIM
					| SWT.APPLICATION_MODAL);
	 	
	 		Utils.setShellIcon(shell);
	 		
		 	Messages.setLanguageText(shell, "security.crypto.title");
    		
		 	GridLayout layout = new GridLayout();
		 	layout.numColumns = 3;
		        
		 	shell.setLayout (layout);
	    
		 	GridData gridData;
	    
		 	if ( action_type == CryptoManagerPasswordHandler.ACTION_ENCRYPT ){
		 	
				Label reason_label = new Label(shell,SWT.WRAP);
				Messages.setLanguageText(reason_label, "security.crypto.encrypt");
				gridData = new GridData(GridData.FILL_BOTH);
				gridData.horizontalSpan = 3;
				gridData.widthHint = 300;
				reason_label.setLayoutData(gridData);
				
		 	}else{
		 	
				Label decrypt_label = new Label(shell,SWT.WRAP);
				Messages.setLanguageText(decrypt_label, "security.crypto.decrypt");
				gridData = new GridData(GridData.FILL_BOTH);
				gridData.horizontalSpan = 3;
				gridData.widthHint = 300;
				decrypt_label.setLayoutData(gridData);

		    		// reason
		    		
				Label reason_label = new Label(shell,SWT.NULL);
				Messages.setLanguageText(reason_label, "security.crypto.reason");
				gridData = new GridData(GridData.FILL_BOTH);
				gridData.horizontalSpan = 1;
				reason_label.setLayoutData(gridData);
				
				Label reason_value = new Label(shell,SWT.NULL);
				reason_value.setText(reason.replaceAll("&", "&&"));
				gridData = new GridData(GridData.FILL_BOTH);
				gridData.horizontalSpan = 2;
				reason_value.setLayoutData(gridData);
				
				// new Label(shell,SWT.NULL);
				
				if ( last_pw_incorrect ){
					
					Label pw_wrong_label = new Label(shell,SWT.WRAP);
					Messages.setLanguageText(pw_wrong_label, "security.crypto.badpw");
					gridData = new GridData(GridData.FILL_BOTH);
					gridData.horizontalSpan = 3;
					pw_wrong_label.setLayoutData(gridData);
					pw_wrong_label.setForeground( Colors.red );
				}
		 	}
		 	
				// password
	    		
			Label password_label = new Label(shell,SWT.NULL);
			Messages.setLanguageText(password_label, "security.crypto.password");
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 1;
			password_label.setLayoutData(gridData);
			
			final Text password_value = new Text(shell,SWT.BORDER);
			password_value.setEchoChar('*');
			password_value.setText("");
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 1;
			password_value.setLayoutData(gridData);

			password_value.addListener(SWT.Modify, new Listener() {
			   public void handleEvent(Event event) {
				 password = password_value.getText().toCharArray();
			   }});

			new Label(shell,SWT.NULL);
			
		 	if ( action_type == CryptoManagerPasswordHandler.ACTION_ENCRYPT ){

		 			// password

		 		verify_password = true;
		 		
				Label password2_label = new Label(shell,SWT.NULL);
				Messages.setLanguageText(password2_label, "security.crypto.password2");
				gridData = new GridData(GridData.FILL_BOTH);
				gridData.horizontalSpan = 1;
				password2_label.setLayoutData(gridData);
				
				final Text password2_value = new Text(shell,SWT.BORDER);
				password2_value.setEchoChar('*');
				password2_value.setText("");
				gridData = new GridData(GridData.FILL_BOTH);
				gridData.horizontalSpan = 1;
				password2_value.setLayoutData(gridData);

				password2_value.addListener(SWT.Modify, new Listener() {
				   public void handleEvent(Event event) {
					 password2 = password2_value.getText().toCharArray();
				   }});

				new Label(shell,SWT.NULL);	
		 	}
		 	
				// persist
			
			Label strength_label = new Label(shell,SWT.NULL);
			Messages.setLanguageText(strength_label, "security.crypto.persist_for");
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 1;
			strength_label.setLayoutData(gridData);
			
			String[] 		duration_keys = { "dont_save", "session", "day", "week", "30days", "forever" };
			final int[]		duration_secs = { 0,           -1,        DAY,   DAY*7,  DAY*30,   Integer.MAX_VALUE };
			
			final Combo durations_combo = new Combo(shell, SWT.SINGLE | SWT.READ_ONLY);
			   
			for (int i=0;i<duration_keys.length;i++){
				
				String text = MessageText.getString( "security.crypto.persist_for." + duration_keys[i] );
				
				durations_combo.add( text );
			}
			      
			durations_combo.select(4);
			
			persist_for_secs	= duration_secs[4];
			
			durations_combo.addListener(
				SWT.Selection,
				new Listener() 
				{
					public void 
					handleEvent(
						Event e ) 
					{
						persist_for_secs	= duration_secs[ durations_combo.getSelectionIndex()];
				   	}
				});
			
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 1;
			durations_combo.setLayoutData(gridData);
			
			new Label(shell,SWT.NULL);
			
				// wiki
						
			final Label linkLabel = new Label(shell, SWT.NULL);
			linkLabel.setText(MessageText.getString("ConfigView.label.please.visit.here"));
			linkLabel.setData("http://wiki.vuze.com/w/Public_Private_Keys");
			linkLabel.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
			linkLabel.setForeground(Colors.blue);
			gridData = new GridData();
			gridData.horizontalSpan = 3;
			linkLabel.setLayoutData(gridData);
			linkLabel.addMouseListener(new MouseAdapter() {
				public void mouseDoubleClick(MouseEvent arg0) {
					Utils.launch((String) ((Label) arg0.widget).getData());
				}

				public void mouseDown(MouseEvent arg0) {
					Utils.launch((String) ((Label) arg0.widget).getData());
				}
			});
				// line
			
			Label labelSeparator = new Label(shell,SWT.SEPARATOR | SWT.HORIZONTAL);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan = 3;
			labelSeparator.setLayoutData(gridData);
			
				// buttons
				
			new Label(shell,SWT.NULL);

			Button bOk = new Button(shell,SWT.PUSH);
		 	Messages.setLanguageText(bOk, "Button.ok");
		 	gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.HORIZONTAL_ALIGN_FILL);
		 	gridData.grabExcessHorizontalSpace = true;
		 	gridData.widthHint = 70;
		 	bOk.setLayoutData(gridData);
		 	bOk.addListener(SWT.Selection,new Listener() {
		  		public void handleEvent(Event e) {
			 		close(true);
		   		}
			 });
	    
		 	Button bCancel = new Button(shell,SWT.PUSH);
		 	Messages.setLanguageText(bCancel, "Button.cancel");
		 	gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		 	gridData.grabExcessHorizontalSpace = false;
		 	gridData.widthHint = 70;
		 	bCancel.setLayoutData(gridData);    
		 	bCancel.addListener(SWT.Selection,new Listener() {
		 		public void handleEvent(Event e) {
			 		close(false);
		   		}
		 	});
	    
			shell.setDefaultButton( bOk );
			
			shell.addListener(SWT.Traverse, new Listener() {	
				public void handleEvent(Event e) {
					if ( e.character == SWT.ESC){
						close( false );
					}
				}
			});
		
		 	shell.pack ();
		 			 	
			Utils.centreWindow( shell );
			
			shell.open();
		}
   
		protected void
		close(
			boolean		ok )
	 	{
			try{
		 		if ( ok ){
		 			
		 			if ( password == null ){
		 				
		 				password = new char[0];
		 			}
		 			
		 			if ( password2 == null ){
		 				
		 				password2 = new char[0];
		 			}

		 			if ( verify_password ){
		 				
		 				if ( !Arrays.equals( password, password2 )){
		 					
		 					MessageBox mb = new MessageBox( shell,SWT.ICON_ERROR | SWT.OK );
		 					
		 					mb.setText(MessageText.getString("security.crypto.password.mismatch.title"));
		 				
		 					mb.setMessage(	MessageText.getString("security.crypto.password.mismatch"));
		 					
		 					mb.open();
		 					
		 					return;
		 				}
		 			}
		 		}else{
		 			
		 			password	= null;
		 		}
		 		
		 		shell.dispose();
		 		
			}finally{
				
				sem.releaseForever();
			}
	 	}
	 	
	 	protected char[]
	 	getPassword()
	 	{
	 		return( password );
	 	}

	 	protected int
	 	getPersistForSeconds()
	 	{
	 		return( persist_for_secs );
	 	}
	}
}
