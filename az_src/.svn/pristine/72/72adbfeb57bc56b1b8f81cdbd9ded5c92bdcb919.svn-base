/*
 * Created on 12-Jun-2004
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

package org.gudy.azureus2.ui.swt.views.configsections;

import java.io.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.auth.CertificateCreatorWindow;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;

import com.aelitis.azureus.core.security.*;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;

/**
 * @author parg
 *
 */
public class 
ConfigSectionSecurity 
	implements UISWTConfigSection 
{
	public String 
	configSectionGetParentSection() 
	{
	    return ConfigSection.SECTION_ROOT;
	}

	public String 
	configSectionGetName() 
	{
		return( "security" );
	}

	public void 
	configSectionSave() 
	{
	}

	public void 
	configSectionDelete() 
	{
		ImageLoader imageLoader = ImageLoader.getInstance();
		imageLoader.releaseImage("openFolderButton");
	}
	
	public int maxUserMode() {
		return 2;
	}
	  
	public Composite 
	configSectionCreate(
		final Composite parent) 
	{
		int userMode = COConfigurationManager.getIntParameter("User Mode");

	    GridData gridData;

	    Composite gSecurity = new Composite(parent, SWT.NULL);
	    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
	    gSecurity.setLayoutData(gridData);
	    GridLayout layout = new GridLayout();
	    layout.numColumns = 3;
	    gSecurity.setLayout(layout);

	    // row
	    
	    Label cert_label = new Label(gSecurity, SWT.NULL );
	    Messages.setLanguageText(cert_label, "ConfigView.section.tracker.createcert");

	    Button cert_button = new Button(gSecurity, SWT.PUSH);

	    Messages.setLanguageText(cert_button, "ConfigView.section.tracker.createbutton");

	    cert_button.addListener(SWT.Selection, 
	    		new Listener() 
				{
			        public void 
					handleEvent(Event event) 
			        {
			        	new CertificateCreatorWindow();
			        }
			    });
	    
	    new Label(gSecurity, SWT.NULL );
	    
	    // row

	    Label	info_label = new Label( gSecurity, SWT.WRAP );
	    Messages.setLanguageText( info_label, "ConfigView.section.security.toolsinfo" );
	    info_label.setLayoutData(Utils.getWrappableLabelGridData(3, 0));
	
	    // row
	    
	    Label lStatsPath = new Label(gSecurity, SWT.NULL);
	    
	    Messages.setLanguageText(lStatsPath, "ConfigView.section.security.toolsdir"); //$NON-NLS-1$

			ImageLoader imageLoader = ImageLoader.getInstance();
			Image imgOpenFolder = imageLoader.getImage("openFolderButton");			
	    
	    gridData = new GridData();
	    
	    gridData.widthHint = 150;
	    
	    final StringParameter pathParameter = new StringParameter(gSecurity, "Security.JAR.tools.dir", ""); //$NON-NLS-1$ //$NON-NLS-2$
	    
	    pathParameter.setLayoutData(gridData);
	    
	    Button browse = new Button(gSecurity, SWT.PUSH);
	    
	    browse.setImage(imgOpenFolder);
	    
	    imgOpenFolder.setBackground(browse.getBackground());
	    
	    browse.setToolTipText(MessageText.getString("ConfigView.button.browse"));
	    
	    browse.addListener(SWT.Selection, new Listener() {
	      public void handleEvent(Event event) {
	        DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.APPLICATION_MODAL);
	
	        dialog.setFilterPath(pathParameter.getValue());
	      
	        dialog.setText(MessageText.getString("ConfigView.section.security.choosetoolssavedir")); //$NON-NLS-1$
	      
	        String path = dialog.open();
	      
	        if (path != null) {
	        	pathParameter.setValue(path);
	        }
	      }
	    });
	    
	   
	    	// row
	    
	    Label pw_label = new Label(gSecurity, SWT.NULL );
	    Messages.setLanguageText(pw_label, "ConfigView.section.security.clearpasswords");

	    Button pw_button = new Button(gSecurity, SWT.PUSH);

	    Messages.setLanguageText(pw_button, "ConfigView.section.security.clearpasswords.button");

	    pw_button.addListener(SWT.Selection, 
	    		new Listener() 
				{
			        public void 
					handleEvent(Event event) 
			        {
			        	SESecurityManager.clearPasswords();
			        	
			        	CryptoManagerFactory.getSingleton().clearPasswords();
			        }
			    });
	    
	    new Label(gSecurity, SWT.NULL );
	
	    if ( userMode >= 2 ){
	    	
	    	final CryptoManager crypt_man = CryptoManagerFactory.getSingleton();
	    	
	    	final Group crypto_group = new Group(gSecurity, SWT.NULL);
		    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.FILL_HORIZONTAL);
		    gridData.horizontalSpan = 3;
		    crypto_group.setLayoutData(gridData);
		    layout = new GridLayout();
		    layout.numColumns = 3;
		    crypto_group.setLayout(layout);
		    
			Messages.setLanguageText(crypto_group,"ConfigView.section.security.group.crypto");
			
				// wiki link
			
			final Label linkLabel = new Label(crypto_group, SWT.NULL);
			linkLabel.setText(MessageText.getString("ConfigView.label.please.visit.here"));
			linkLabel.setData("http://wiki.vuze.com/w/Public_Private_Keys");
			linkLabel.setCursor(linkLabel.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
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
			
			
				// publick key display

			byte[]	public_key = crypt_man.getECCHandler().peekPublicKey();
			
		    Label public_key_label = new Label(crypto_group, SWT.NULL );
		    Messages.setLanguageText(public_key_label, "ConfigView.section.security.publickey");

		    final Label public_key_value = new Label(crypto_group, SWT.NULL );
		    
			if ( public_key == null ){
				
			    Messages.setLanguageText(public_key_value, "ConfigView.section.security.publickey.undef");

			}else{
			    			    			    
			    public_key_value.setText( Base32.encode( public_key ));
			}
			
		    Messages.setLanguageText(public_key_value, "ConfigView.copy.to.clipboard.tooltip", true);

		    public_key_value.setCursor(public_key_value.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		    public_key_value.setForeground(Colors.blue);
		    public_key_value.addMouseListener(new MouseAdapter() {
		    	public void mouseDoubleClick(MouseEvent arg0) {
		    		copyToClipboard();
		    	}
		    	public void mouseDown(MouseEvent arg0) {
		    		copyToClipboard();
		    	}
		    	protected void
		    	copyToClipboard()
		    	{
	    			new Clipboard(parent.getDisplay()).setContents(new Object[] {public_key_value.getText()}, new Transfer[] {TextTransfer.getInstance()});
		    	}
		    });
			
			crypt_man.addKeyListener(
					new CryptoManagerKeyListener()
					{
						public void 
						keyChanged(
							final CryptoHandler handler ) 
						{
							final CryptoManagerKeyListener me = this;
							
							Utils.execSWTThread(
								new Runnable()
								{
									public void 
									run()
									{						
										if ( public_key_value.isDisposed()){
											
											crypt_man.removeKeyListener( me );
											
										}else{
											if ( handler.getType() == CryptoManager.HANDLER_ECC ){
												
												byte[]	public_key = handler.peekPublicKey();
			
												if ( public_key == null ){											
												
													Messages.setLanguageText(public_key_value, "ConfigView.section.security.publickey.undef");
													
												}else{
													
													public_key_value.setText( Base32.encode( public_key ));
												}
												
												crypto_group.layout();
											}
										}
									}
								});
						}
						
						public void
						keyLockStatusChanged(
							CryptoHandler		handler )
						{
						}
					});
			
		    new Label(crypto_group, SWT.NULL );
		    
		    	// manage keys
		    
		    /*
		    gridData = new GridData();
		    gridData.horizontalSpan = 3;

		    final BooleanParameter manage_keys = new BooleanParameter( 
		    		crypto_group, "crypto.keys.system.managed.temp",
		    		"ConfigView.section.security.system.managed");
		    
		    manage_keys.setLayoutData( gridData );
		    
		    final CryptoManager crypto_man 	= CryptoManagerFactory.getSingleton();
			final CryptoHandler ecc_handler = crypto_man.getECCHandler();

		    manage_keys.setSelected( 
		    		ecc_handler.getDefaultPasswordHandlerType() == CryptoManagerPasswordHandler.HANDLER_TYPE_SYSTEM ); 
		    

		    manage_keys.addChangeListener(
		    	new ParameterChangeAdapter ()
		    	{
		    		public void 
		    		parameterChanged(
		    			Parameter 	p,
		    			boolean 	caused_internally ) 
		    		{
	    				boolean existing_value = ecc_handler.getDefaultPasswordHandlerType() == CryptoManagerPasswordHandler.HANDLER_TYPE_SYSTEM;
	    				
	    				if ( existing_value == manage_keys.isSelected()){

	    					return;
	    				}
	    				
	    				String	error = null;
	    				
	    				int	new_type = manage_keys.isSelected()?CryptoManagerPasswordHandler.HANDLER_TYPE_SYSTEM:CryptoManagerPasswordHandler.HANDLER_TYPE_USER;
	    					    					
    					try{
    						ecc_handler.setDefaultPasswordHandlerType( new_type );
    						
    						error = null;
    						
    					}catch( CryptoManagerPasswordException e ){
    						
    						if ( e.wasIncorrect()){
    							
    							error = MessageText.getString( "ConfigView.section.security.unlockkey.error" );
    							
    						}else{
    							
    							if ( existing_value || !ecc_handler.isUnlocked()){
    								
    								error = MessageText.getString( "Torrent.create.progress.cancelled" );
    						    	
    							}else{
    								
    								error = MessageText.getString( "ConfigView.section.security.vuze.login" );
    							}
    						}
    					}catch( Throwable e ){
    						
    						error = Debug.getNestedExceptionMessage( e );
    					}
	    				
	    				if ( error != null ){
	    					
	    					MessageBoxShell mb = new MessageBoxShell(
	    							SWT.ICON_ERROR | SWT.OK,
	    							MessageText.getString("ConfigView.section.security.op.error.title"),
	    							MessageText.getString("ConfigView.section.security.op.error",
	    									new String[] { error }));
	      				mb.setParent(parent.getShell());
	    					mb.open(null);
	    				}
	    				
	    				boolean new_value = ecc_handler.getDefaultPasswordHandlerType() == CryptoManagerPasswordHandler.HANDLER_TYPE_SYSTEM;
	    				
	    				if ( new_value != manage_keys.isSelected()){
    					
	    					manage_keys.setSelected( new_value );
	    				}
		    		}
		    	});
		    */
		    
	    		// reset keys
		    
		    Label reset_key_label = new Label(crypto_group, SWT.NULL );
		    Messages.setLanguageText(reset_key_label, "ConfigView.section.security.resetkey");
	
		    Button reset_key_button = new Button(crypto_group, SWT.PUSH);
		    Messages.setLanguageText(reset_key_button, "ConfigView.section.security.clearpasswords.button");
	
		    reset_key_button.addListener(SWT.Selection, 
		    		new Listener() 
					{
				        public void 
						handleEvent(Event event) 
				        {
				        	MessageBoxShell mb = new MessageBoxShell(
				        			SWT.ICON_WARNING | SWT.OK | SWT.CANCEL,
				        			MessageText.getString("ConfigView.section.security.resetkey.warning.title"),
				        			MessageText.getString("ConfigView.section.security.resetkey.warning"));
				        	mb.setDefaultButtonUsingStyle(SWT.CANCEL);
				        	mb.setParent(parent.getShell());

				        	mb.open(new UserPrompterResultListener() {
										public void prompterClosed(int returnVal) {
											if (returnVal != SWT.OK) {
												return;
											}
											
											try{
												crypt_man.getECCHandler().resetKeys( "Manual key reset" );
												
											}catch( Throwable e ){
												
												MessageBoxShell mb = new MessageBoxShell( 
														SWT.ICON_ERROR | SWT.OK,
														MessageText.getString( "ConfigView.section.security.resetkey.error.title"),
														getError( e ));
							  				mb.setParent(parent.getShell());
							  				mb.open(null);
											}
										}
									});
				        }
				    });
		    
		    new Label(crypto_group, SWT.NULL );
		    	
		    	// unlock
		    
		    Label priv_key_label = new Label(crypto_group, SWT.NULL );
		    Messages.setLanguageText(priv_key_label, "ConfigView.section.security.unlockkey");
	
		    Button priv_key_button = new Button(crypto_group, SWT.PUSH);
		    Messages.setLanguageText(priv_key_button, "ConfigView.section.security.unlockkey.button");
	
		    priv_key_button.addListener(SWT.Selection, 
		    		new Listener() 
					{
				        public void 
						handleEvent(Event event) 
				        {
				        	try{
				        		crypt_man.getECCHandler().getEncryptedPrivateKey( "Manual unlock" );
				        		
				        	}catch( Throwable e ){
				        		
			 					MessageBoxShell mb = new MessageBoxShell( 
			 						SWT.ICON_ERROR | SWT.OK,
			 						MessageText.getString( "ConfigView.section.security.resetkey.error.title" ),
			 						getError( e ));
			 					mb.setParent(parent.getShell());
			 					mb.open(null);
				        	};

				        }
				    });
		    
		    new Label(crypto_group, SWT.NULL );
		    
		    	// backup
		    
		    Label backup_keys_label = new Label(crypto_group, SWT.NULL );
		    Messages.setLanguageText(backup_keys_label, "ConfigView.section.security.backupkeys");
	
		    final Button backup_keys_button = new Button(crypto_group, SWT.PUSH);
		    Messages.setLanguageText(backup_keys_button, "ConfigView.section.security.backupkeys.button");
	
		    backup_keys_button.addListener(SWT.Selection, 
		    		new Listener() 
					{
				        public void 
						handleEvent(Event event) 
				        {
				        	FileDialog dialog = new FileDialog( backup_keys_button.getShell(), SWT.APPLICATION_MODAL );
				        	
				        	String	target = dialog.open();
				        	
				        	if ( target != null ){
				        		
					        	try{
					        		String	keys = crypt_man.getECCHandler().exportKeys();
					        		
					        		PrintWriter pw = new PrintWriter(new FileWriter( target ));
					        		
					        		pw.println( keys );
					        		
					        		pw.close();
					        	
					        	}catch( Throwable e ){
					        	
					        		MessageBoxShell mb = new MessageBoxShell( 
					        				SWT.ICON_ERROR | SWT.OK,
					        				MessageText.getString( "ConfigView.section.security.op.error.title" ),
					        				MessageText.getString( "ConfigView.section.security.op.error", 
					        						new String[]{ getError(e) }));
					    				mb.setParent(parent.getShell());
					        		mb.open(null);
					        	}
				        	}
				        }
				    });
		    
		    new Label(crypto_group, SWT.NULL );
		    
		    	// restore
		    
		    Label restore_keys_label = new Label(crypto_group, SWT.NULL );
		    Messages.setLanguageText(restore_keys_label, "ConfigView.section.security.restorekeys");
	
		    final Button restore_keys_button = new Button(crypto_group, SWT.PUSH);
		    Messages.setLanguageText(restore_keys_button, "ConfigView.section.security.restorekeys.button");
	
		    restore_keys_button.addListener(SWT.Selection, 
		    		new Listener() 
					{
				        public void 
						handleEvent(Event event) 
				        {
				        	FileDialog dialog = new FileDialog( backup_keys_button.getShell(), SWT.APPLICATION_MODAL );
				        	
				        	String	target = dialog.open();
				        	
				        	if ( target != null ){
				        		
					        	try{
					        		LineNumberReader reader = new LineNumberReader(  new FileReader( target ));
					        		
					        		String	str = "";
					        		
					        		while( true ){
					        			
					        			String	line = reader.readLine();
					        			
					        			if ( line == null ){
					        				
					        				break;
					        			}
					        			
					        			str += line + "\r\n";
					        		}
					        		
					        		boolean restart = crypt_man.getECCHandler().importKeys(str);
					  
					        		if ( restart ){
					        			
					        			MessageBoxShell mb = new MessageBoxShell( 
						        				SWT.ICON_INFORMATION | SWT.OK,
						        				MessageText.getString( "ConfigView.section.security.restart.title" ),
						        				MessageText.getString( "ConfigView.section.security.restart.msg" ));
					      				mb.setParent(parent.getShell());
					        			mb.open(null); 
	
						        		
						        		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
						        		
						            	if ( uiFunctions != null ){
						            		
						            		uiFunctions.dispose(true, false);
						            	}
					        		}
					        	}catch( Throwable e ){
					        	
					        		MessageBoxShell mb = new MessageBoxShell(  
					        			SWT.ICON_ERROR | SWT.OK,
					        			MessageText.getString( "ConfigView.section.security.op.error.title" ),
					        			MessageText.getString( "ConfigView.section.security.op.error", 
					        					new String[]{ getError( e )}));
					    				mb.setParent(parent.getShell());
					        		mb.open(null);
					        	}
				        	}
				        }
				    });
		    
		    new Label(crypto_group, SWT.NULL );
	    }
	    
	    return gSecurity;
	  }
	
	protected String
	getError(
		Throwable e )
	{
		String	error;
		
		if ( e instanceof CryptoManagerPasswordException ){
			
			if (((CryptoManagerPasswordException)e).wasIncorrect()){
				
				error = MessageText.getString( "ConfigView.section.security.unlockkey.error");
				
			}else{
				
			    final CryptoManager crypto_man 	= CryptoManagerFactory.getSingleton();
				final CryptoHandler ecc_handler = crypto_man.getECCHandler();
				
				//if ( ecc_handler.getDefaultPasswordHandlerType() == CryptoManagerPasswordHandler.HANDLER_TYPE_SYSTEM ){
				//	
				//	error = MessageText.getString( "ConfigView.section.security.nopw_v" );
				//
				//}else{
					
					error = MessageText.getString( "ConfigView.section.security.nopw" );
				//}
			}
		}else{
			
			error = MessageText.getString( "ConfigView.section.security.resetkey.error" ) + ": " + Debug.getNestedExceptionMessage(e);
		}
		
		return( error );
	}
}
