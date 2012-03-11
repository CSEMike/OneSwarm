/*
 * Created on Apr 2, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.plugins.net.buddy.swt;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.ui.UIInputReceiver;
import org.gudy.azureus2.plugins.ui.UIInputReceiverListener;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;

import com.aelitis.azureus.core.security.*;
import com.aelitis.azureus.core.util.AZ3Functions;
import com.aelitis.azureus.plugins.net.buddy.*;

public class 
BuddyPluginViewInstance 
	implements BuddyPluginListener, BuddyPluginBuddyRequestListener
{
	private static final int LOG_NORMAL 	= 1;
	private static final int LOG_SUCCESS 	= 2;
	private static final int LOG_ERROR 		= 3;


	private BuddyPlugin	plugin;
	private UIInstance	ui_instance;
	private Composite	composite;
	private Table 		buddy_table;
	private StyledText 	log;

	private List	buddies = new ArrayList();

	private boolean	init_complete;
		
	protected
	BuddyPluginViewInstance(
		BuddyPlugin		_plugin,
		UIInstance		_ui_instance,
		Composite		_composite )
	{
		plugin		= _plugin;
		ui_instance	= _ui_instance;
		composite	= _composite;

		final LocaleUtilities lu = plugin.getPluginInterface().getUtilities().getLocaleUtilities();
			
		Composite main = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		main.setLayout(layout);
		GridData grid_data = new GridData(GridData.FILL_BOTH );
		main.setLayoutData(grid_data);

		if ( !plugin.isEnabled()){
			
			Label control_label = new Label( main, SWT.NULL );
			control_label.setText( lu.getLocalisedMessageText( "azbuddy.disabled" ));

			return;
		}
			// control area
		
		final Composite controls = new Composite(main, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 6;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		controls.setLayout(layout);
		grid_data = new GridData(GridData.FILL_HORIZONTAL );
		controls.setLayoutData(grid_data);
		
		Label control_label = new Label( controls, SWT.NULL );
		control_label.setText( lu.getLocalisedMessageText( "azbuddy.ui.new_buddy" ) + " " );
		
		final Text control_text = new Text( controls, SWT.BORDER );
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		control_text.setLayoutData(gridData);
	
		final Button control_button = new Button( controls, SWT.NULL );
		control_button.setText( lu.getLocalisedMessageText( "azbuddy.ui.add" ));
		
		control_button.setEnabled( false );
		
		control_text.addModifyListener(
			new ModifyListener() {
	        	public void 
	        	modifyText(
	        		ModifyEvent e ) 
	        	{					
					control_button.setEnabled( plugin.verifyPublicKey( control_text.getText().trim())); 
	        	}
	        });
		
		control_button.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent e )
				{
					plugin.addBuddy( control_text.getText().trim(), BuddyPlugin.SUBSYSTEM_AZ2 );
					
					control_text.setText( "" );
				}
			});
		
		final Label control_lab_pk = new Label( controls, SWT.NULL );
		control_lab_pk.setText( lu.getLocalisedMessageText( "azbuddy.ui.mykey" ) + " ");

		final Text control_val_pk = new Text( controls, SWT.NULL );
		gridData = new GridData();
		gridData.widthHint = 400;
		control_val_pk.setLayoutData(gridData);

		control_val_pk.setEditable( false );
		control_val_pk.setBackground( control_lab_pk.getBackground());
		
		control_val_pk.addKeyListener(
			new KeyListener()
			{
				public void 
				keyPressed(
					KeyEvent event) 
				{
					int	key = event.character;
					
					if (key <= 26 && key > 0){
						
						key += 'a' - 1;
					}
				
					if ( event.stateMask == SWT.MOD1 && key == 'a' ){
	
						control_val_pk.setSelection( 0, control_val_pk.getText().length());	
					}
				}
				
				public void 
				keyReleased(
					KeyEvent event) 
				{	
				}
			});
		

	

    	final CryptoManager crypt_man = CryptoManagerFactory.getSingleton();

		byte[]	public_key = crypt_man.getECCHandler().peekPublicKey();
		  
		if ( public_key == null ){
			
		    Messages.setLanguageText(control_val_pk, "ConfigView.section.security.publickey.undef");

		}else{
		    			    			    
			control_val_pk.setText( Base32.encode( public_key ));
		}
		
	    Messages.setLanguageText(control_val_pk, "ConfigView.copy.to.clipboard.tooltip", true);

	    control_val_pk.setCursor(main.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
	    control_val_pk.setForeground(Colors.blue);
	    control_val_pk.addMouseListener(new MouseAdapter() {
	    	public void mouseDoubleClick(MouseEvent arg0) {
	    		copyToClipboard();
	    	}
	    	public void mouseDown(MouseEvent arg0) {
	    		copyToClipboard();
	    	}
	    	protected void
	    	copyToClipboard()
	    	{
    			new Clipboard(control_val_pk.getDisplay()).setContents(new Object[] {control_val_pk.getText()}, new Transfer[] {TextTransfer.getInstance()});
	    	}
	    });
		
		crypt_man.addKeyListener(
				new CryptoManagerKeyListener()
				{
					public void 
					keyChanged(
						final CryptoHandler handler ) 
					{
						if ( control_val_pk.isDisposed()){
							
							crypt_man.removeKeyListener( this );
							
						}else if ( handler.getType() == CryptoManager.HANDLER_ECC ){
							
							control_val_pk.getDisplay().asyncExec(
								new Runnable()
								{
									public void 
									run()
									{
										byte[]	public_key = handler.peekPublicKey();
		
										if ( public_key == null ){
																				
											 Messages.setLanguageText(control_val_pk, "ConfigView.section.security.publickey.undef");
											
										}else{
											
											control_val_pk.setText( Base32.encode( public_key ));
										}
										 
										controls.layout();
									}
								});
						}
					}
					
					public void
					keyLockStatusChanged(
						CryptoHandler		handler )
					{
					}
				});
		
		final Button config_button = new Button( controls, SWT.NULL );
		config_button.setText( lu.getLocalisedMessageText( "plugins.basicview.config" ));

		config_button.addSelectionListener(
				new SelectionAdapter() 
				{
					public void 
					widgetSelected(
						SelectionEvent e )
					{
						plugin.showConfig();
					}
				});
		
			// table and log
		
		final Composite form = new Composite(main, SWT.NONE);
		FormLayout flayout = new FormLayout();
		flayout.marginHeight = 0;
		flayout.marginWidth = 0;
		form.setLayout(flayout);
		gridData = new GridData(GridData.FILL_BOTH);
		form.setLayoutData(gridData);


		final Composite child1 = new Composite(form,SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		child1.setLayout(layout);

		final Sash sash = new Sash(form, SWT.HORIZONTAL);
	
		final Composite child2 = new Composite(form,SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		child2.setLayout(layout);

		FormData formData;

			// child1
		
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(0, 0);
		child1.setLayoutData(formData);

		final FormData child1Data = formData;
		
		final int SASH_WIDTH = 4;
		
			// sash
		
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(child1);
		formData.height = SASH_WIDTH;
		sash.setLayoutData(formData);

			// child2
		
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.bottom = new FormAttachment(100, 0);
		formData.top = new FormAttachment(sash);
		child2.setLayoutData(formData);

		final PluginConfig pc = plugin.getPluginInterface().getPluginconfig();
		
		sash.setData( "PCT", new Float( pc.getPluginFloatParameter( "swt.sash.position", 0.7f )));
		
		sash.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent e ) 
				{
					if (e.detail == SWT.DRAG){
						return;
					}
					
					child1Data.height = e.y + e.height - SASH_WIDTH;
					
					form.layout();
	
					Float l = new Float((double)child1.getBounds().height / form.getBounds().height);
					
					sash.setData( "PCT", l );
					
					pc.setPluginParameter( "swt.sash.position", l.floatValue());
				}
			});

		form.addListener(
			SWT.Resize, 
			new Listener() 
			{
				public void 
				handleEvent(Event e) 
				{
					Float l = (Float) sash.getData( "PCT" );
					
					if ( l != null ){
						
						child1Data.height = (int) (form.getBounds().height * l.doubleValue());
					
						form.layout();
					}
				}
			});
			
			// table
		
		buddy_table = new Table(child1, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);

		final String[] headers = { 
				"azbuddy.ui.table.name", 
				"azbuddy.ui.table.online",  
				"azbuddy.ui.table.lastseen", 
				"azbuddy.ui.table.last_ygm", 
				"azbuddy.ui.table.last_msg",
				"azbuddy.ui.table.loc_cat",
				"azbuddy.ui.table.rem_cat",
				"azbuddy.ui.table.read_cat",
				"azbuddy.ui.table.con",
				"azbuddy.ui.table.msg_in",
				"azbuddy.ui.table.msg_out",
				"azbuddy.ui.table.msg_queued",
				"MyTrackerView.bytesin",
				"MyTrackerView.bytesout",
				"azbuddy.ui.table.ss" };

		int[] sizes = { 250, 100, 100, 100, 200, 100, 100, 100, 75, 75, 75, 75, 75, 75, 40 };

		int[] aligns = { SWT.LEFT, SWT.CENTER, SWT.CENTER, SWT.CENTER, SWT.CENTER, SWT.LEFT, SWT.LEFT, SWT.LEFT, SWT.LEFT, SWT.CENTER, SWT.CENTER, SWT.CENTER, SWT.CENTER, SWT.CENTER, SWT.CENTER };

		for (int i = 0; i < headers.length; i++){

			TableColumn tc = new TableColumn(buddy_table, aligns[i]);
				
			tc.setWidth(sizes[i]);

			Messages.setLanguageText(tc, headers[i]);
		}	

	    buddy_table.setHeaderVisible(true);

	    TableColumn[] columns = buddy_table.getColumns();
	    columns[0].setData(new Integer(FilterComparator.FIELD_NAME));
	    columns[1].setData(new Integer(FilterComparator.FIELD_ONLINE));
	    columns[2].setData(new Integer(FilterComparator.FIELD_LAST_SEEN));
	    columns[3].setData(new Integer(FilterComparator.FIELD_YGM));
	    columns[4].setData(new Integer(FilterComparator.FIELD_LAST_MSG));
	    columns[5].setData(new Integer(FilterComparator.FIELD_LOC_CAT));
	    columns[6].setData(new Integer(FilterComparator.FIELD_REM_CAT));
	    columns[7].setData(new Integer(FilterComparator.FIELD_READ_CAT));
	    columns[8].setData(new Integer(FilterComparator.FIELD_CON));
	    columns[9].setData(new Integer(FilterComparator.FIELD_MSG_IN));
	    columns[10].setData(new Integer(FilterComparator.FIELD_MSG_OUT));
	    columns[11].setData(new Integer(FilterComparator.FIELD_QUEUED));
	    columns[12].setData(new Integer(FilterComparator.FIELD_BYTES_IN));
	    columns[13].setData(new Integer(FilterComparator.FIELD_BYTES_OUT));
	    columns[14].setData(new Integer(FilterComparator.FIELD_SS));
	    
	    
	    final FilterComparator comparator = new FilterComparator();
	    
	    Listener sort_listener = 
	    	new Listener() 
	    	{
		    	public void 
		    	handleEvent(
		    		Event e ) 
		    	{
		    		TableColumn tc = (TableColumn) e.widget;
	
		    		int field = ((Integer) tc.getData()).intValue();
	
		    		comparator.setField( field );
	
		    		Collections.sort( buddies,comparator);
	
		    		updateTable();
		    	}
	    	};
	    
	    for (int i=0;i<columns.length;i++){
	    	
	    	columns[i].addListener(SWT.Selection,sort_listener);
	    }	    
	    
	    gridData = new GridData(GridData.FILL_BOTH);
	    gridData.heightHint = buddy_table.getHeaderHeight() * 3;
		buddy_table.setLayoutData(gridData);
		
		
		buddy_table.addListener(
			SWT.SetData,
			new Listener()
			{
				public void 
				handleEvent(
					Event event) 
				{
					TableItem item = (TableItem)event.item;
					
					int index = buddy_table.indexOf(item);
	
					if ( index < 0 || index >= buddies.size()){
						
						return;
					}
					
					BuddyPluginBuddy	buddy = (BuddyPluginBuddy)buddies.get(index);
					
					item.setText(0, buddy.getName());
					
					int	os;
					
					if ( buddy.isOnline( false )){
						
						os = buddy.getOnlineStatus();
						
					}else{
						
						os = BuddyPlugin.STATUS_APPEAR_OFFLINE;
					}
					
					if ( os == BuddyPlugin.STATUS_APPEAR_OFFLINE ){
						
						item.setText( 1, "" );
						
					}else{
					
						item.setText(1, plugin.getOnlineStatus( os ));
					}
					
					long lo = buddy.getLastTimeOnline();
					
					item.setText(2, lo==0?"":new SimpleDateFormat().format(new Date( lo )));

					long	last_ygm = buddy.getLastMessagePending();;
					
					item.setText(3, last_ygm==0?"":new SimpleDateFormat().format(new Date( last_ygm )));
					
					String	lm = buddy.getLastMessageReceived();
					
					item.setText(4, lm==null?"":lm);
					
					String loc_cat = buddy.getLocalAuthorisedRSSCategoriesAsString();
					if ( loc_cat == null ){
						loc_cat = "";
					}
					item.setText(5, "" + loc_cat);

					String rem_cat = buddy.getRemoteAuthorisedRSSCategoriesAsString();
					if ( rem_cat == null ){
						rem_cat = "";
					}
					item.setText(6, "" + rem_cat);

					String read_cat = buddy.getLocalReadCategoriesAsString();
					if ( read_cat == null ){
						read_cat = "";
					}
					item.setText(7, "" + read_cat);

					item.setText(8, "" + buddy.getConnectionsString());
					
					String in_frag = buddy.getMessageInFragmentDetails();
					
					item.setText(9, "" + buddy.getMessageInCount() + (in_frag.length()==0?"":("+" + in_frag )));
					item.setText(10, "" + buddy.getMessageOutCount());
					item.setText(11, "" + buddy.getMessageHandler().getMessageCount());
					item.setText(12, "" + DisplayFormatters.formatByteCountToKiBEtc(buddy.getBytesInCount()));
					item.setText(13, "" + DisplayFormatters.formatByteCountToKiBEtc(buddy.getBytesOutCount()));

					item.setText(14, "" + buddy.getSubsystem() + " v" + buddy.getVersion());
					
					item.setData( buddy );
				}
			});
		
		final Listener tt_label_listener = 
			new Listener() 
			{
				public void handleEvent(Event event) {
					Label label = (Label) event.widget;
					Shell shell = label.getShell();
					switch (event.type) {
					case SWT.MouseDown:
						Event e = new Event();
						e.item = (TableItem) label.getData("_TABLEITEM");
						buddy_table.setSelection(new TableItem[] { (TableItem) e.item });
						buddy_table.notifyListeners(SWT.Selection, e);
						// fall through
					case SWT.MouseExit:
						shell.dispose();
						break;
					}
				}
			};


		Listener	tt_table_listener = 
			new Listener()
			{
				private Shell tip = null;

				private Label label = null;

				public void 
				handleEvent(
					Event event ) 
				{
					switch (event.type){
						case SWT.Dispose:
						case SWT.KeyDown:
						case SWT.MouseMove: {
							if (tip == null)
								break;
							tip.dispose();
							tip = null;
							label = null;
							break;
						}
						case SWT.MouseHover: 
						{
							Point mouse_position = new Point(event.x, event.y);
							
							TableItem item = buddy_table.getItem( mouse_position );
														
							if (item != null) {
								
								if (tip != null && !tip.isDisposed()){
									
									tip.dispose();
									
									tip = null;
								}
								
								int index = buddy_table.indexOf(item);
								
								if ( index < 0 || index >= buddies.size()){
									
									return;
								}

								BuddyPluginBuddy	buddy = (BuddyPluginBuddy)buddies.get(index);

								int	item_index = 0;
								
								for (int i=0;i<headers.length;i++){
									
									Rectangle bounds = item.getBounds(i);
									
									if ( bounds.contains( mouse_position )){
										
										item_index = i;
										
										break;
									}
								}
								
								if( item_index != 0 ){
									
									return;
								}
								
								tip = new Shell(buddy_table.getShell(), SWT.ON_TOP | SWT.TOOL);
								tip.setLayout(new FillLayout());
								label = new Label(tip, SWT.NONE);
								label.setForeground(buddy_table.getDisplay()
										.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
								label.setBackground(buddy_table.getDisplay()
										.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
								label.setData("_TABLEITEM", item);
								
								label.setText( getToolTip( buddy ));
								
								label.addListener(SWT.MouseExit, tt_label_listener);
								label.addListener(SWT.MouseDown, tt_label_listener);
								Point size = tip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
								Rectangle rect = item.getBounds(item_index);
								Point pt = buddy_table.toDisplay(rect.x, rect.y);
								tip.setBounds(pt.x, pt.y, size.x, size.y);
								tip.setVisible(true);
							}
						}
					}
				}
				
				protected String
				getToolTip(
					BuddyPluginBuddy	buddy )
				{
					List addresses = buddy.getAdjustedIPs();
					
					InetAddress	ip	= buddy.getIP();
					
					InetAddress adj = buddy.getAdjustedIP();
					
					String	str = "";
					
					if ( ip == null ){
						
						str = "<none>";
						
					}else if ( ip == adj ){
						
						str = ip.getHostAddress();
						
					}else{
						
						str = ip.getHostAddress() + "{";
					
						for (int i=0;i<addresses.size();i++){
							
							str += (i==0?"":"/") + ((InetAddress)addresses.get(i)).getHostAddress();
						}
						
						str += "}";
					}
					
					return(  "ip=" + str + ",tcp=" + buddy.getTCPPort() + ",udp=" + buddy.getUDPPort());
				}
			};
			
		buddy_table.addListener(SWT.Dispose, tt_table_listener);
		buddy_table.addListener(SWT.KeyDown, tt_table_listener);
		buddy_table.addListener(SWT.MouseMove, tt_table_listener);
		buddy_table.addListener(SWT.MouseHover, tt_table_listener);
		    			


		final Menu menu = new Menu(buddy_table);
		
		final MenuItem remove_item = new MenuItem(menu, SWT.PUSH);
		
		remove_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.remove" ));

		remove_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent e) 
				{
					TableItem[] selection = buddy_table.getSelection();
					
					for (int i=0;i<selection.length;i++){
						
						BuddyPluginBuddy buddy = (BuddyPluginBuddy)selection[i].getData();
						
						buddy.remove();
					}
				};
			});
		
			// get public key
				
		final MenuItem get_pk_item = new MenuItem(menu, SWT.PUSH);

		get_pk_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.copypk" ) );

		get_pk_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					TableItem[] selection = buddy_table.getSelection();
					
					StringBuffer sb = new StringBuffer();
					
					for (int i=0;i<selection.length;i++){
						
						BuddyPluginBuddy buddy = (BuddyPluginBuddy)selection[i].getData();
						
						sb.append( buddy.getPublicKey() + "\r\n" );
					}
					
					if ( sb.length() > 0 ){
					
						writeToClipboard( sb.toString());
					}
				};
			});
		
			// disconnect message
		
		if ( Constants.isCVSVersion()){
			final  MenuItem send_msg_item = new MenuItem(menu, SWT.PUSH);
	
			send_msg_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.disconnect" ) );
	
			send_msg_item.addSelectionListener(
				new SelectionAdapter() 
				{
					public void 
					widgetSelected(
						SelectionEvent event ) 
					{
						TableItem[] selection = buddy_table.getSelection();
				
						for (int i=0;i<selection.length;i++){
							
							BuddyPluginBuddy buddy = (BuddyPluginBuddy)selection[i].getData();
							
							buddy.disconnect();
						}
					}
				});
		}
		
			// send message
		
		final  MenuItem send_msg_item = new MenuItem(menu, SWT.PUSH);

		send_msg_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.send" ) );

		send_msg_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					final TableItem[] selection = buddy_table.getSelection();
					
					UIInputReceiver prompter = ui_instance.getInputReceiver();
					
					prompter.setLocalisedTitle( lu.getLocalisedMessageText( "azbuddy.ui.menu.send" ));
					prompter.setLocalisedMessage( lu.getLocalisedMessageText( "azbuddy.ui.menu.send_msg" ) );
					
					try{
						prompter.prompt(new UIInputReceiverListener() {
							public void UIInputReceiverClosed(UIInputReceiver prompter) {
								String text = prompter.getSubmittedInput();
								
								if ( text != null ){
									
									for (int i=0;i<selection.length;i++){
										
										BuddyPluginBuddy buddy = (BuddyPluginBuddy)selection[i].getData();
										
										plugin.getAZ2Handler().sendAZ2Message( buddy, text );
									}
								}
							}
						});
						
					}catch( Throwable e ){
						
					}
				};
			});
		
			// chat
		
		final  MenuItem chat_item = new MenuItem(menu, SWT.PUSH);

		chat_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.chat" ) );

		chat_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					TableItem[] selection = buddy_table.getSelection();
					
					BuddyPluginBuddy[] buddies = new BuddyPluginBuddy[selection.length];
					
					for (int i=0;i<selection.length;i++){
						
						BuddyPluginBuddy buddy = (BuddyPluginBuddy)selection[i].getData();

						buddies[i] = buddy;
					}
								
					plugin.getAZ2Handler().createChat( buddies );
				};
			});
		
			// ping
		
		final MenuItem ping_item = new MenuItem(menu, SWT.PUSH);

		ping_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.ping" ) );

		ping_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					TableItem[] selection = buddy_table.getSelection();
					
					for (int i=0;i<selection.length;i++){
						
						BuddyPluginBuddy buddy = (BuddyPluginBuddy)selection[i].getData();
						
						try{					
							buddy.ping();
							
						}catch( Throwable e ){
							
							print( "Ping failed", e );
						}
					}
				};
			});
		
			// ygm
		
		final MenuItem ygm_item = new MenuItem(menu, SWT.PUSH);

		ygm_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.ygm" ) );

		ygm_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					TableItem[] selection = buddy_table.getSelection();
					
					for (int i=0;i<selection.length;i++){
						
						BuddyPluginBuddy buddy = (BuddyPluginBuddy)selection[i].getData();
						
						try{
							buddy.setMessagePending();
							
						}catch( Throwable e ){
							
							print( "YGM failed", e );
						}
					}
				};
			});
		
		
			// encrypt
		
		final MenuItem encrypt_item = new MenuItem(menu, SWT.PUSH);

		encrypt_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.enc" ) );

		encrypt_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					TableItem[] selection = buddy_table.getSelection();
					
					String	str = readFromClipboard();
					
					if( str != null ){
						
						StringBuffer sb = new StringBuffer();
						
						for (int i=0;i<selection.length;i++){
							
							BuddyPluginBuddy buddy = (BuddyPluginBuddy)selection[i].getData();
							
							try{
								byte[]	contents = str.getBytes( "UTF-8" );
								
								BuddyPlugin.cryptoResult result = buddy.encrypt( contents );
								
								sb.append( "key: " );
								sb.append( plugin.getPublicKey());
								sb.append( "\r\n" );
								
								sb.append( "hash: " );
								sb.append( Base32.encode( result.getChallenge()));
								sb.append( "\r\n" );

								sb.append( "payload: " );
								sb.append( Base32.encode( result.getPayload()));
								sb.append( "\r\n\r\n" );
								
							}catch( Throwable e ){
								
								print( "YGM failed", e );
							}
						}
						
						writeToClipboard( sb.toString());
					}
				};
			});
		
			// decrypt
		
		final MenuItem decrypt_item = new MenuItem(menu, SWT.PUSH);

		decrypt_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.dec" ) );

		decrypt_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					String	str = readFromClipboard();
					
					if ( str != null ){
						
						String[] 	bits = str.split( "\n" );
						
						StringBuffer sb = new StringBuffer();
	
						BuddyPluginBuddy	buddy 	= null;
						byte[]				hash	= null;
						
						for (int i=0;i<bits.length;i++){
							
							String	bit = bits[i].trim();
							
							if ( bit.length() > 0 ){
							
								int	pos = bit.indexOf( ':' );
								
								if ( pos == -1 ){
									
									continue;
								}
								
								String	lhs = bit.substring( 0, pos ).trim();
								String	rhs	= bit.substring( pos+1 ).trim();
								
								if ( lhs.equals( "key" )){
									
									buddy = plugin.getBuddyFromPublicKey( rhs );
									
								}else if ( lhs.equals( "hash" )){
									
									hash	= Base32.decode( rhs );
									
								}else if ( lhs.equals( "payload" )){
								
									byte[]	payload = Base32.decode( rhs );
									
									if ( buddy != null ){
										
										try{
											BuddyPlugin.cryptoResult result = buddy.decrypt( payload );
											
											byte[] sha1 = new SHA1Simple().calculateHash( result.getChallenge());
											
											sb.append( "key: " );
											sb.append( buddy.getPublicKey());
											sb.append( "\r\n" );

											sb.append( "hash_ok: " + Arrays.equals( hash, sha1 ));
											sb.append( "\r\n" );
											
											sb.append( "payload: " );
											sb.append( new String( result.getPayload(), "UTF-8" ));
											sb.append( "\r\n\r\n" );
											
										}catch( Throwable e ){
											
											print( "decrypt failed", e );
										}
									}
								}
							}
						}
						
						if ( sb.length() > 0 ){
						
							writeToClipboard( sb.toString());
						}
					}
				};
			});
		
			// sign
		
		final MenuItem sign_item = new MenuItem(menu, SWT.PUSH);

		sign_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.sign" ) );

		sign_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					String	str = readFromClipboard();
					
					if ( str != null ){
						
						StringBuffer sb = new StringBuffer();
						
						try{
							sb.append( "key: " );
							sb.append( plugin.getPublicKey());
							sb.append( "\r\n" );

							byte[] payload = str.getBytes( "UTF-8" );
							
							sb.append( "data: " );
							sb.append( Base32.encode( payload ));
							sb.append( "\r\n" );

							byte[]	sig = plugin.sign( payload );

							sb.append( "sig: " );
							sb.append( Base32.encode( sig ));
							sb.append( "\r\n" );

						}catch( Throwable e ){
							
							print( "sign failed", e );
						}
						
						if ( sb.length() > 0 ){
						
							writeToClipboard( sb.toString());
						}
					}
				};
			});
		
			// verify
		
		final MenuItem verify_item = new MenuItem(menu, SWT.PUSH);

		verify_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.verify" ) );

		verify_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					String	str = readFromClipboard();
					
					if ( str != null ){
						
						String[] 	bits = str.split( "\n" );
						
						StringBuffer sb = new StringBuffer();
	
						String				pk 		= null;
						byte[]				data	= null;
						
						for (int i=0;i<bits.length;i++){
							
							String	bit = bits[i].trim();
							
							if ( bit.length() > 0 ){
							
								int	pos = bit.indexOf( ':' );
								
								if ( pos == -1 ){
									
									continue;
								}
								
								String	lhs = bit.substring( 0, pos ).trim();
								String	rhs	= bit.substring( pos+1 ).trim();
								
								if ( lhs.equals( "key" )){
									
									pk = rhs;
									
								}else if ( lhs.equals( "data" )){
									
									data	= Base32.decode( rhs );
									
								}else if ( lhs.equals( "sig" )){
								
									byte[]	sig = Base32.decode( rhs );
									
									if ( pk != null && data != null ){
										
										try{
											
											sb.append( "key: " );
											sb.append( pk );
											sb.append( "\r\n" );

											boolean ok = plugin.verify( pk, data, sig );
											
											sb.append( "sig_ok: " + ok  );
											sb.append( "\r\n" );
											
											sb.append( "data: " );
											sb.append( new String( data, "UTF-8" ));
											sb.append( "\r\n\r\n" );
											
										}catch( Throwable e ){
											
											print( "decrypt failed", e );
										}
									}
								}
							}
						}
						
						if ( sb.length() > 0 ){
						
							writeToClipboard( sb.toString());
						}
					}
				};
			});
				
		
			// cats
		
		Menu cat_menu = new Menu(menu.getShell(), SWT.DROP_DOWN);
		MenuItem cat_item = new MenuItem(menu, SWT.CASCADE);
		Messages.setLanguageText(cat_item, "azbuddy.ui.menu.cat" );
		cat_item.setMenu(cat_menu);

			// cats - share
		
		final MenuItem cat_share_item = new MenuItem(cat_menu, SWT.PUSH);

		cat_share_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.cat.share" ) );

		cat_share_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					UIInputReceiver prompter = ui_instance.getInputReceiver();
					
					prompter.setLocalisedTitle( lu.getLocalisedMessageText( "azbuddy.ui.menu.cat.set" ));
					prompter.setLocalisedMessage( lu.getLocalisedMessageText( "azbuddy.ui.menu.cat.set_msg" ));
					
					prompter.prompt(new UIInputReceiverListener() {
						public void UIInputReceiverClosed(UIInputReceiver prompter) {
							String cats = prompter.getSubmittedInput();
							
							if ( cats != null ){
								
								cats = cats.trim();
								
								if ( cats.equalsIgnoreCase( "None" )){
									
									cats = "";
								}
								
								TableItem[] selection = buddy_table.getSelection();
								
								for (int i=0;i<selection.length;i++){
									
									BuddyPluginBuddy buddy = (BuddyPluginBuddy)selection[i].getData();
									
									buddy.setLocalAuthorisedRSSCategories( cats );
								}
							}
						}
					});
					
				};
			});
		
			// cats - subscribe
		
		final Menu cat_subs_menu = new Menu(cat_menu.getShell(), SWT.DROP_DOWN);
		final MenuItem cat_subs_item = new MenuItem(cat_menu, SWT.CASCADE);
		Messages.setLanguageText(cat_subs_item, "azbuddy.ui.menu.cat_subs" );
		cat_subs_item.setMenu(cat_subs_menu);

		cat_subs_menu.addMenuListener(
			new MenuListener()
			{
				public void 
				menuShown(
					MenuEvent arg0 ) 
				{
					MenuItem[] items = cat_subs_menu.getItems();
					
					for (int i = 0; i < items.length; i++){
						
						items[i].dispose();
					}
					
					final AZ3Functions.provider az3 = AZ3Functions.getProvider();
					
					if ( az3 != null ){
						
						final TableItem[] selection = buddy_table.getSelection();
						
						Set<String> avail_cats = new TreeSet<String>();
						
						for (int i=0;i<selection.length;i++){
							
							BuddyPluginBuddy buddy = (BuddyPluginBuddy)selection[i].getData();
							
							Set<String> cats = buddy.getRemoteAuthorisedRSSCategories();
							
							if ( cats != null ){
								
								avail_cats.addAll( cats );
							}
						}
						
						for ( final String cat: avail_cats ){
							
							final MenuItem subs_item = new MenuItem( cat_subs_menu, SWT.PUSH );
	
							subs_item.setText( cat );
	
							subs_item.addSelectionListener(
								new SelectionAdapter() 
								{
									public void 
									widgetSelected(
										SelectionEvent event ) 
									{
										for (int i=0;i<selection.length;i++){
											
											BuddyPluginBuddy buddy = (BuddyPluginBuddy)selection[i].getData();

											if ( buddy.isRemoteRSSCategoryAuthorised( cat )){
												
												try{
													buddy.subscribeToCategory( cat );
													
												}catch( Throwable e ){
													
													print( "Failed", e );
												}
											}
										}
									}
								});
						}
					}
				}
				
				public void 
				menuHidden(
					MenuEvent arg0 ) 
				{
				}
			});
		
		
			// done with menus
		
		buddy_table.setMenu( menu );
			
		menu.addMenuListener(
			new MenuListener()
			{
				public void 
				menuShown(
					MenuEvent arg0 ) 
				{
					boolean	available = plugin.isAvailable();
					
					TableItem[] selection = buddy_table.getSelection();
						
					remove_item.setEnabled( selection.length > 0 );
					get_pk_item.setEnabled( available && selection.length > 0 );
					send_msg_item.setEnabled(available && selection.length > 0);
					chat_item.setEnabled(available && selection.length > 0);
					ping_item.setEnabled(available && selection.length > 0);
					ygm_item.setEnabled(available && selection.length > 0);
					encrypt_item.setEnabled(selection.length > 0);
					decrypt_item.setEnabled(true);
					sign_item.setEnabled(true);
					verify_item.setEnabled(true);
				}
				
				public void 
				menuHidden(
					MenuEvent arg0 ) 
				{
				}
			});
		
			// log area

		log = new StyledText(child2,SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		grid_data = new GridData(GridData.FILL_BOTH);
		grid_data.horizontalSpan = 1;
		grid_data.horizontalIndent = 4;
		log.setLayoutData(grid_data);
		log.setIndent( 4 );

		buddies = plugin.getBuddies();
		
		for (int i=0;i<buddies.size();i++){
			
			buddyAdded((BuddyPluginBuddy)buddies.get(i));
		}
		
		Collections.sort( buddies, comparator );

		plugin.addListener( this );
		
		plugin.addRequestListener( this );
		
		init_complete	= true;
	    
		updateTable();
	}

	protected String
	readFromClipboard()
	{
		 Object o = 
			 new Clipboard(SWTThread.getInstance().getDisplay()).getContents(
			      TextTransfer.getInstance());
		 
		 if ( o instanceof String ){
			 
			 return((String)o);
		 }
		 
		 return( null );
	}

	protected void
	writeToClipboard(
		String	str )
	{
		 new Clipboard(SWTThread.getInstance().getDisplay()).setContents(
			      new Object[] {str }, 
			      new Transfer[] {TextTransfer.getInstance()});
	}

	protected void
	updateTable()
	{
		if ( init_complete ){
			
			buddy_table.setItemCount( buddies.size());
			buddy_table.clearAll();
			buddy_table.redraw();
		}
	}
	
	public void 
	initialised(
		boolean available ) 
	{
		print( "Initialisation complete: available=" + available );
	}
	
	public void
	buddyAdded(
		final BuddyPluginBuddy	buddy )
	{
		if ( buddy_table.isDisposed()){
			
			return;
		}
		
		buddy.getMessageHandler().addListener(
			new BuddyPluginBuddyMessageListener()
			{
				public void
				messageQueued(
					BuddyPluginBuddyMessage		message )
				{
					print( message.getBuddy().getName() + ": message queued, id=" + message.getID());
					
					update();
				}
				
				public void
				messageDeleted(
					BuddyPluginBuddyMessage		message )
				{
					print( message.getBuddy().getName() + ": message deleted, id=" + message.getID());
					
					update();
				}
				
				public boolean
				deliverySucceeded(
					BuddyPluginBuddyMessage		message,
					Map							reply )
				{
					print( message.getBuddy().getName() + ": message delivered, id=" + message.getID() + ", reply=" + reply );
					
					update();
					
					return( true );
				}
				
				public void
				deliveryFailed(
					BuddyPluginBuddyMessage		message,
					BuddyPluginException		cause )
				{
					print( message.getBuddy().getName() + ": message failed, id=" + message.getID(), cause );
					
					update();
				}
				
				protected void
				update()
				{
					if ( !buddy_table.isDisposed()){
						
						buddy_table.getDisplay().asyncExec(
							new Runnable()
							{
								public void
								run()
								{
									if ( !buddy_table.isDisposed()){
										
										updateTable();
									}
								}
							});
					}
				}
			});
		
		if ( !buddies.contains( buddy )){
			
			buddy_table.getDisplay().asyncExec(
					new Runnable()
					{
						public void
						run()
						{
							if ( !buddy_table.isDisposed()){
								
								if ( !buddies.contains( buddy )){
									
									buddies.add( buddy );
									
									updateTable();
								}
							}
						}
					});
		}
	}

	public void
	buddyRemoved(
		final BuddyPluginBuddy	buddy )
	{
		if ( !buddy_table.isDisposed()){
			
			buddy_table.getDisplay().asyncExec(
					new Runnable()
					{
						public void
						run()
						{
							if ( !buddy_table.isDisposed()){
								
								if ( buddies.remove( buddy )){
																	
									updateTable();
								}
							}
						}
					});	
		}
	}

	public void
	buddyChanged(
		final BuddyPluginBuddy	buddy )
	{
		if ( !buddy_table.isDisposed()){
			
			buddy_table.getDisplay().asyncExec(
					new Runnable()
					{
						public void
						run()
						{
							if ( !buddy_table.isDisposed()){
																								
								updateTable();
							}
						}
					});	
		}
	}
	
	public void
	messageLogged(
		String		str,
		boolean		error )
	{
		print( str, error?LOG_ERROR:LOG_NORMAL, false, false );
	}
	
	// @see com.aelitis.azureus.plugins.net.buddy.BuddyPluginListener#enabledStateChanged(boolean)
	public void 
	enabledStateChanged(
			boolean enabled) {
	}

	public Map
	requestReceived(
		BuddyPluginBuddy	from_buddy,
		int					subsystem,
		Map					request )
	
		throws BuddyPluginException
	{
		return( null );
	}
	
	public void
	pendingMessages(
		BuddyPluginBuddy[]	from_buddies )
	{
		String	str = "";
		
		for (int i=0;i<from_buddies.length;i++){
			
			str += (str.length()==0?"":",") + from_buddies[i].getName();
		}
		
		print( "YGM received: " + str );
	}
	
	protected void
	print(
		String		str,
		Throwable	e )
	{
		print( str + ": " + Debug.getNestedExceptionMessage( e ));
	}
	
	protected void
	print(
		String		str )
	{
		print( str, LOG_NORMAL, false, true );
	}

	protected void
	print(
		final String		str,
		final int			log_type,
		final boolean		clear_first,
		boolean				log_to_plugin )
	{
		if ( log_to_plugin ){

			plugin.log( str );
		}

		if ( !log.isDisposed()){

			final int f_log_type = log_type;

			log.getDisplay().asyncExec(
					new Runnable()
					{
						public void
						run()
						{
							if ( log.isDisposed()){

								return;
							}

							int	start;

							if ( clear_first ){

								start	= 0;

								log.setText( str + "\n" );

							}else{

								String	text = log.getText();
								
								start = text.length();

								if ( start > 32000 ){
									
									log.replaceTextRange( 0, 1024, "" );
									
									start = log.getText().length();
								}
								
								log.append( str + "\n" );
							}

							Color 	color;

							if ( f_log_type == LOG_NORMAL ){

								color = Colors.black;

							}else if ( f_log_type == LOG_SUCCESS ){

								color = Colors.green;

							}else{

								color = Colors.red;
							}

							if ( color != Colors.black ){
								
								StyleRange styleRange = new StyleRange();
								styleRange.start = start;
								styleRange.length = str.length();
								styleRange.foreground = color;
								log.setStyleRange(styleRange);
							}
							
							log.setSelection( log.getText().length());
						}
					});
		}
	}

	protected void
	destroy()
	{
		composite = null;
		
		plugin.removeListener( this );
		
		plugin.removeRequestListener( this );

	}
	
	protected class 
	FilterComparator 
		implements Comparator 
	{
		boolean ascending = false;

		static final int FIELD_NAME			= 0;
		static final int FIELD_ONLINE 		= 1;
		static final int FIELD_LAST_SEEN 	= 2;
		static final int FIELD_YGM		 	= 3;
		static final int FIELD_LAST_MSG 	= 4;
		static final int FIELD_LOC_CAT	 	= 5;
		static final int FIELD_REM_CAT 		= 6;
		static final int FIELD_READ_CAT 	= 7;
		static final int FIELD_CON		 	= 8;
		static final int FIELD_MSG_IN	 	= 9;
		static final int FIELD_MSG_OUT	 	= 10;
		static final int FIELD_QUEUED	 	= 11;
		static final int FIELD_BYTES_IN 	= 12;
		static final int FIELD_BYTES_OUT 	= 13;
		static final int FIELD_SS		 	= 14;

		int field = FIELD_NAME;

		public int 
		compare(
			Object arg0,
			Object arg1) 
		{
			BuddyPluginBuddy b1 = (BuddyPluginBuddy) arg0;
			BuddyPluginBuddy b2 = (BuddyPluginBuddy) arg1;
			
			int	res = 0;
			
			if(field == FIELD_NAME){				
				 res = b1.getName().compareTo( b2.getName());
			}else if(field == FIELD_ONLINE){
				res = ( b1.isOnline( false )?1:0 ) - ( b2.isOnline( false )?1:0 );
			}else if(field == FIELD_LAST_SEEN){
				res = sortInt( b1.getLastTimeOnline() - b2.getLastTimeOnline());
			}else if(field == FIELD_YGM){
				res = sortInt( b1.getLastMessagePending() - b2.getLastMessagePending());
			}else if(field == FIELD_LAST_MSG){
				res = b1.getLastMessageReceived().compareTo( b2.getLastMessageReceived());
			}else if(field == FIELD_LOC_CAT){
				res = compareStrings( b1.getLocalAuthorisedRSSCategoriesAsString(), b2.getLocalAuthorisedRSSCategoriesAsString());
			}else if(field == FIELD_REM_CAT){
				res = compareStrings( b1.getRemoteAuthorisedRSSCategoriesAsString(), b2.getRemoteAuthorisedRSSCategoriesAsString());
			}else if(field == FIELD_READ_CAT){
				res = compareStrings( b1.getLocalReadCategoriesAsString(), b2.getLocalReadCategoriesAsString());
			}else if(field == FIELD_CON){
				res = b1.getConnectionsString().compareTo( b2.getConnectionsString());
			}else if(field == FIELD_MSG_IN){
				res = b1.getMessageInCount() - b2.getMessageInCount();
			}else if(field == FIELD_MSG_OUT){
				res = b1.getMessageOutCount() - b2.getMessageOutCount();
			}else if(field == FIELD_QUEUED){
				res = b1.getMessageHandler().getMessageCount() - b2.getMessageHandler().getMessageCount();
			}else if(field == FIELD_BYTES_IN){
				res = b1.getBytesInCount() - b2.getBytesInCount();
			}else if(field == FIELD_BYTES_OUT){
				res = b1.getBytesOutCount() - b2.getBytesOutCount();
			}else if(field == FIELD_SS){
				res =  b1.getSubsystem() - b2.getSubsystem();
			}
			
			return(( ascending ? 1 : -1) * res );
		}

		protected int
		compareStrings(
			String	s1,
			String	s2 )
		{
			if ( s1 == null && s2 == null ){
				return(0);
			}else if ( s1 == null ){
				return(-1);
			}else if ( s2 == null ){
				return( 1 );
			}else{
				return( s1.compareTo(s2));
			}
		}
		
		protected int
		sortInt(
			long	l )
		{
			if ( l < 0 ){
				return( -1 );
			}else if ( l > 0 ){
				return( 1 );
			}else{
				return( 0 );
			}
		}
		public void 
		setField(
			int newField ) 
		{      
			if(field == newField) ascending = ! ascending;
			
			field = newField;
		}
	}
}
