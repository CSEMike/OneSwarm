/*
 * Created on 05-Sep-2005
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

package org.gudy.azureus2.ui.swt.pluginsimpl;

import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.common.util.MenuItemManager;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.mainwindow.*;
import org.gudy.azureus2.ui.swt.minibar.AllTransfersBar;
import org.gudy.azureus2.ui.swt.minibar.DownloadBar;
import org.gudy.azureus2.ui.swt.plugins.*;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;
import org.gudy.azureus2.ui.swt.views.table.utils.TableContextMenuManager;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.impl.TableColumnImpl;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;

import org.gudy.azureus2.pluginsimpl.local.download.DownloadImpl;

public class 
UISWTInstanceImpl
	implements UIInstanceFactory, UISWTInstance, UIManagerEventListener
{
	private AzureusCore		core;
	
	private Map awt_view_map 	= new WeakHashMap();
	private Map config_view_map = new WeakHashMap();
	
	private Map views = new HashMap();
	
	private Map	plugin_map	= new WeakHashMap();
	
	private boolean bUIAttaching;

	private final UIFunctionsSWT uiFunctions;
	
	
	public UISWTInstanceImpl(AzureusCore _core) {
		core		= _core;

		// Since this is a UI **SWT** Instance Implementor, it's assumed
		// that the UI Functions are of UIFunctionsSWT 
		uiFunctions = (UIFunctionsSWT) UIFunctionsManager.getUIFunctions();
	}
	
	public void init() {
		try{
			UIManager	ui_manager = core.getPluginManager().getDefaultPluginInterface().getUIManager();
			
			ui_manager.addUIEventListener( this );
			
			bUIAttaching = true;
			
			ui_manager.attachUI( this );
			
			bUIAttaching = false;
			
		}catch( UIException e ){
			
			Debug.printStackTrace(e);
		}
	}
  
	public UIInstance
	getInstance(
		PluginInterface		plugin_interface )
	{
		UIInstance	instance = (UIInstance)plugin_map.get( plugin_interface );
		
		if ( instance == null ){
			
			instance = new instanceWrapper( plugin_interface, this );
			
			plugin_map.put( plugin_interface, instance );
		}
		
		return( instance );
	}
	
	public boolean
	eventOccurred(
		final UIManagerEvent	event )
	{
		boolean	done = true;
		
		final Object	data = event.getData();
		
		switch( event.getType()){
		
			case UIManagerEvent.ET_SHOW_TEXT_MESSAGE:
			{
				Utils.execSWTThread(
					new Runnable()
					{
						public void 
						run()
						{
							String[]	params = (String[])data;
							
							new TextViewerWindow( params[0], params[1], params[2] );
						}
					});
				
				break;
			}
			
			case UIManagerEvent.ET_OPEN_TORRENT_VIA_FILE:
			{	
				TorrentOpener.openTorrent(((File)data).toString());

				break;
			}
			case UIManagerEvent.ET_OPEN_TORRENT_VIA_URL:
			{
				Display display = SWTThread.getInstance().getDisplay();

				display.syncExec(new AERunnable() {
					public void runSupport() {
						Object[] params = (Object[]) data;

						URL target = (URL) params[0];
						URL referrer = (URL) params[1];
						boolean auto_download = ((Boolean) params[2]).booleanValue();

						// programmatic request to add a torrent, make sure az is visible

						if (!COConfigurationManager.getBooleanParameter("add_torrents_silently")) {
							uiFunctions.bringToFront();
						}

						if (auto_download) {
							Shell shell = uiFunctions.getMainShell();
							if (shell != null) {
								new FileDownloadWindow(core, shell, target.toString(),
										referrer == null ? null : referrer.toString());
							}
						} else {

							// TODO: handle referrer?

							TorrentOpener.openTorrent(target.toString());
						}
					}
				});

				break;
			}
			case UIManagerEvent.ET_PLUGIN_VIEW_MODEL_CREATED:
			{
				if ( data instanceof BasicPluginViewModel ){
					BasicPluginViewModel model = (BasicPluginViewModel)data;
					
					// property bundles can't handle spaces in keys
					String sViewID = model.getName().replaceAll(" ", ".");
					BasicPluginViewImpl view = new BasicPluginViewImpl(model);
					addView(UISWTInstance.VIEW_MAIN, sViewID, view);
				}
				
				break;
			}
			case UIManagerEvent.ET_PLUGIN_VIEW_MODEL_DESTROYED:
			{
				if ( data instanceof BasicPluginViewModel ){
					BasicPluginViewModel model = (BasicPluginViewModel)data;
					// property bundles can't handle spaces in keys
					String sViewID = model.getName().replaceAll(" ", ".");
					removeViews(UISWTInstance.VIEW_MAIN, sViewID);
				}
				
				break;
			}
			case UIManagerEvent.ET_PLUGIN_CONFIG_MODEL_CREATED:
			{
				if ( data instanceof BasicPluginConfigModel ){
					
					BasicPluginConfigModel	model = (BasicPluginConfigModel)data;
					
					BasicPluginConfigImpl view = new BasicPluginConfigImpl(model);
					   
					config_view_map.put( model, view );
					
					model.getPluginInterface().addConfigSection( view );
				}
				
				break;
			}
			case UIManagerEvent.ET_PLUGIN_CONFIG_MODEL_DESTROYED:
			{
				if ( data instanceof BasicPluginConfigModel ){
					
					BasicPluginConfigModel	model = (BasicPluginConfigModel)data;
					
					BasicPluginConfigImpl view = (BasicPluginConfigImpl)config_view_map.get( model );
					   
					if ( view != null ){
						
						model.getPluginInterface().removeConfigSection( view );
					}
				}
				
				break;
			}
			case UIManagerEvent.ET_COPY_TO_CLIPBOARD:
			{
				ClipboardCopy.copyToClipBoard((String)data);
				
				break;
			}
			case UIManagerEvent.ET_OPEN_URL:
			{
				Utils.launch(((URL)data).toExternalForm());
				
				break;
			}
			case UIManagerEvent.ET_CREATE_TABLE_COLUMN:{
				
	 			String[]	args = (String[])data;
	 			
	 			event.setResult( new TableColumnImpl(args[0], args[1]));
	 			
	 			break;
			} 
			case UIManagerEvent.ET_ADD_TABLE_COLUMN:{
				
				TableColumn	_col = (TableColumn)data;
				
				if ( _col instanceof TableColumnImpl ){
					
					TableColumnManager.getInstance().addColumn((TableColumnImpl)_col);
					
				}else{
					
					throw(new UIRuntimeException("TableManager.addColumn(..) can only add columns created by createColumn(..)"));
				}
				
				break;
			} 
			case UIManagerEvent.ET_ADD_TABLE_CONTEXT_MENU_ITEM:{
				TableContextMenuItem	item = (TableContextMenuItem)data;
				TableContextMenuManager.getInstance().addContextMenuItem(item);
				break;
			}
			case UIManagerEvent.ET_ADD_MENU_ITEM: {
				MenuItem item = (MenuItem)data;
				MenuItemManager.getInstance().addMenuItem(item);
				break;
			}
			case UIManagerEvent.ET_REMOVE_TABLE_CONTEXT_MENU_ITEM:{
				TableContextMenuItem item = (TableContextMenuItem)data;
				TableContextMenuManager.getInstance().removeContextMenuItem(item);
				break;
			}
			case UIManagerEvent.ET_REMOVE_MENU_ITEM: {
				MenuItem item = (MenuItem)data;
				MenuItemManager.getInstance().removeMenuItem(item);
				break;
			}			
			case UIManagerEvent.ET_SHOW_CONFIG_SECTION: {
				event.setResult(new Boolean(false));

				if (!(data instanceof String))
					break;

	    	event.setResult(new Boolean(uiFunctions.showConfig((String)data)));

				break;
			}
			default:
			{
				done	= false;
				
				break;
			}
		}
		
		return( done );
	}
	
	public Display 
	getDisplay() 
	{
		return SWTThread.getInstance().getDisplay();
	}
  
	public Image
	loadImage(
		String	resource )
	{
		throw( new RuntimeException( "plugin specific instance required" ));
	}
	
	protected Image  
	loadImage(
		PluginInterface	pi,
		String 			res ) 
	{
		InputStream is = pi.getPluginClassLoader().getResourceAsStream( res);
		
		if ( is != null ){
		        
			ImageData imageData = new ImageData(is);
		    
			return new Image(getDisplay(), imageData);
		}
		
		return null;
	}
	
	public UISWTGraphic 
	createGraphic(
		Image img) 
	{
		return new UISWTGraphicImpl(img);
	}
  

	/** @deprecated */
	public void addView(final UISWTPluginView view, boolean bAutoOpen) {
		try {
			uiFunctions.addPluginView(view);
			if (bAutoOpen) {
				uiFunctions.openPluginView(view);
			}
		} catch (Throwable e) {
			// SWT not available prolly
		}
	} 
  

	public void removeView(UISWTPluginView view) {
		try {
			uiFunctions.removePluginView(view);
		} catch (Throwable e) {
			// SWT not available prolly
		}
	}

	/** @deprecated */
	public void
	addView(
		final UISWTAWTPluginView	view,
		boolean						auto_open )
	{
		UISWTPluginView	v = 
			new UISWTPluginView()
			{
				Composite		composite;
				Component		component;
				
				boolean	first_paint = true;
				
				public String
				getPluginViewName()
				{
					return( view.getPluginViewName());
				}
				
				public String 
				getFullTitle() 
				{
					return( view.getPluginViewName());
				}
				 
				public void 
				initialize(
					Composite _composite )
				{
					first_paint	= true;
					
					composite	= _composite;
					
					Composite frame_composite = new Composite(composite, SWT.EMBEDDED);
	
					GridData data = new GridData(GridData.FILL_BOTH);
					
					frame_composite.setLayoutData(data);
	
					Frame	f = SWT_AWT.new_Frame(frame_composite);
	
					BorderLayout	layout = 
						new BorderLayout()
						{
							public void 
							layoutContainer(Container parent)
							{
								try{
									super.layoutContainer( parent );
								
								}finally{
									if ( first_paint ){
										
										first_paint	= false;
											
										view.open( component );
									}
								}
							}
						};
					
					Panel	pan = new Panel( layout );
	
					f.add( pan );
							
					component	= view.create();
					
					pan.add( component, BorderLayout.CENTER );
				}
				
				public Composite 
				getComposite()
				{
					return( composite );
				}
				
				public void 
				delete() 
				{
					super.delete();
					
					view.delete( component );
				}
			};
			
		awt_view_map.put( view, v );
		
		addView( v, auto_open );
	}
	
	public void
	removeView(
		UISWTAWTPluginView		view )
	{
		UISWTPluginView	v = (UISWTPluginView)awt_view_map.remove(view );
		
		if ( v != null ){
			
			removeView( v );
		}
	}
	
	public void
	detach()
	
		throws UIException
	{
		throw( new UIException( "not supported" ));
	}


	public void addView(String sParentID, final String sViewID,
			final UISWTViewEventListener l) {
		Map subViews = (Map) views.get(sParentID);
		if (subViews == null) {
			subViews = new HashMap();
			views.put(sParentID, subViews);
		}

		subViews.put(sViewID, l);

		if (sParentID.equals(UISWTInstance.VIEW_MAIN)) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					try {
						uiFunctions.addPluginView(sViewID, l);
					} catch (Throwable e) {
						// SWT not available prolly
					}
				}
			});
		}
	}
	
	// TODO: Remove views from PeersView, etc
	public void removeViews(String sParentID, final String sViewID) {
		Map subViews = (Map) views.get(sParentID);
		if (subViews == null)
			return;

		if (sParentID.equals(UISWTInstance.VIEW_MAIN)) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					try {
						if (uiFunctions != null) {
							uiFunctions.removePluginView(sViewID);
						}
					} catch (Throwable e) {
						// SWT not available prolly
					}
				}
			});
		}
		subViews.remove(sViewID);
	}
	
	public boolean openView(final String sParentID, final String sViewID,
			final Object dataSource) {
		Map subViews = (Map) views.get(sParentID);
		if (subViews == null) {
			return false;
		}

		final UISWTViewEventListener l = (UISWTViewEventListener) subViews.get(sViewID);
		if (l == null) {
			return false;
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (uiFunctions != null) {
					uiFunctions.openPluginView(sParentID, sViewID, l, dataSource,
							!bUIAttaching);
				}
			}
		});

		return true;
	}

	public void openMainView(final String sViewID,
			final UISWTViewEventListener l, final Object dataSource) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (uiFunctions != null) {
					uiFunctions.openPluginView(UISWTInstance.VIEW_MAIN, sViewID, l, dataSource, !bUIAttaching);
				}
			}
		});
	}

	public UISWTView[] getOpenViews(String sParentID) {
		if (sParentID.equals(UISWTInstance.VIEW_MAIN)) {
			try {
				if (uiFunctions != null) {
					return uiFunctions.getPluginViews();
				}
			} catch (Throwable e) {
				// SWT not available prolly
			}
		}
		return new UISWTView[0];
	}
	
	// @see org.gudy.azureus2.plugins.ui.UIInstance#promptUser(java.lang.String, java.lang.String, java.lang.String[], int)
	public int promptUser(String title, String text, String[] options,
			int defaultOption) {
		return MessageBoxShell.open(uiFunctions.getMainShell(), title, text,
				options, defaultOption);
	}
	
	public void showDownloadBar(Download download, final boolean display) {
		if (!(download instanceof DownloadImpl)) {return;}
		final DownloadManager dm = ((DownloadImpl)download).getDownload();
		if (dm == null) {return;} // Not expecting this, but just in case...
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (display) {
					DownloadBar.open(dm, getDisplay().getActiveShell());
				}
				else {
					DownloadBar.close(dm);
				}
			}
		}, false);
	}
	
	public void showTransfersBar(final boolean display) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (display) {
					AllTransfersBar.open(core.getGlobalManager(), getDisplay().getActiveShell());
				}
				else {
					AllTransfersBar.close(core.getGlobalManager());
				}
			}
		}, false);
	}

	// Core Functions
	// ==============
	
	public Map getViewListeners(String sParentID) {
		return (Map)views.get(sParentID);
	}
	
	public UIInputReceiver getInputReceiver() {
		return new SimpleTextEntryWindow(getDisplay());
	}
	
	public UISWTStatusEntry createStatusEntry() {
		final UISWTStatusEntryImpl entry = new UISWTStatusEntryImpl();
		UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (functionsSWT == null) {
			return null;
		}
		
		MainStatusBar mainStatusBar = functionsSWT.getMainStatusBar();
		if (mainStatusBar == null) {
			return null;
		}
		final CLabel label = mainStatusBar.createStatusEntry(entry);
		final Listener click_listener = new Listener() {
			public void handleEvent(Event e) {
				entry.onClick();
			}
		};

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				label.addListener(SWT.MouseDoubleClick, click_listener);
			}
		}, true);
		
		return entry;
	}
	
	
	protected static class
	instanceWrapper
		implements UISWTInstance
	{
		private PluginInterface			pi;
		private UISWTInstanceImpl		delegate;
		
		protected
		instanceWrapper(
			PluginInterface		_pi,
			UISWTInstanceImpl	_delegate )
		{
			pi			= _pi;
			delegate	= _delegate;
		}
		
		public void
		detach()
		
			throws UIException
		{
			delegate.detach();
		}
	
		public Display
		getDisplay()
		{
			return( delegate.getDisplay());
		}
		
		public Image
		loadImage(
			String	resource )
		{
			return( delegate.loadImage( pi, resource ));
		}
		
		public UISWTGraphic 
		createGraphic(
			Image img )
		{
			return( delegate.createGraphic( img ));
		}
		
		public void 
		addView(String sParentID, String sViewID, UISWTViewEventListener l)
		{
			delegate.addView( sParentID, sViewID, l );
		}

		public void 
		openMainView(String sViewID, UISWTViewEventListener l,Object dataSource)
		{
			delegate.openMainView( sViewID, l, dataSource );
		}

		public void 
		removeViews(String sParentID, String sViewID)
		{
			delegate.removeViews(sParentID, sViewID );
		}


		public UISWTView[] 
		getOpenViews(String sParentID)
		{
			return( delegate.getOpenViews(sParentID));
		}


		public void 
		addView(UISWTPluginView view, boolean autoOpen)
		{
			delegate.addView( view, autoOpen );
		}


		public void 
		removeView(UISWTPluginView view)
		{
			delegate.removeView( view );
		}

		public void 
		addView(UISWTAWTPluginView view, boolean auto_open)
		{
			delegate.addView( view, auto_open );
		}


		public void 
		removeView(UISWTAWTPluginView view)
		{
			delegate.removeView( view );
		}
		
		public int promptUser(String title, String text, String[] options,
				int defaultOption) {
			return delegate.promptUser(title, text, options, defaultOption);
		}

		public boolean openView(String sParentID, String sViewID, Object dataSource) {
			return delegate.openView(sParentID, sViewID, dataSource);
		}
		
		public UIInputReceiver getInputReceiver() {
			return delegate.getInputReceiver();
		}
		
		public void showDownloadBar(Download download, boolean display) {
			delegate.showDownloadBar(download, display);
		}
		
		public void showTransfersBar(boolean display) {
			delegate.showTransfersBar(display);
		}
		
		public UISWTStatusEntry createStatusEntry() {
			return delegate.createStatusEntry();
		}
		
		
	}
}
