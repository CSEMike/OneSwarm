/*
 * Created on 16-Jan-2006
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

package org.gudy.azureus2.ui.swt.views;

import java.util.*;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.download.DownloadManagerStateAttributeListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.ChangeSelectionActionPerformer;
import org.gudy.azureus2.ui.swt.config.generic.GenericBooleanParameter;
import org.gudy.azureus2.ui.swt.config.generic.GenericIntParameter;
import org.gudy.azureus2.ui.swt.config.generic.GenericParameterAdapter;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;

import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

public class 
TorrentOptionsView
	implements DownloadManagerStateAttributeListener, UISWTViewCoreEventListener
{
	private static final String	TEXT_PREFIX	= "TorrentOptionsView.param.";
	
		// adhoc parameters need explicit code to reset default values below
	
	private static final String	MAX_UPLOAD		= "max.upload";
	private static final String	MAX_DOWNLOAD	= "max.download";

	public static final String MSGID_PREFIX = "TorrentOptionsView";
	
	private boolean						multi_view;
	private DownloadManager[]			managers;
	
	private GenericParameterAdapter	ds_param_adapter	= new downloadStateParameterAdapter();
	private GenericParameterAdapter	adhoc_param_adapter	= new adhocParameterAdapter();
	
	private Map<String, Object> adhoc_parameters	= new HashMap<String, Object>();
	private Map<String, Object>	ds_parameters 		= new HashMap<String, Object>();
	
	private Composite 			panel;
	private Font 				headerFont;

	private Composite parent;

	private UISWTView swtView;
	
	public
	TorrentOptionsView()
	{
	}
	
	/**
	 * @param managers2
	 */
	public TorrentOptionsView(DownloadManager[] managers2) {
		dataSourceChanged(managers2);
	}

	private void 
	initialize(
		Composite composite) 
	{
		this.parent = composite;
		
		GridLayout layout;
		
		// cheap trick to allow datasource changes.  Normally we'd just
		// refill the components with new info, but I didn't write this and
		// I don't want to waste my time :) [tux]
		if (panel != null && !panel.isDisposed()) {
			Utils.disposeComposite(panel, false);
		} else {
			panel = new Composite(composite, SWT.NULL);

			layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			layout.numColumns = 1;
			panel.setLayout(layout);
			
			Layout parentLayout = parent.getLayout();
			if (parentLayout instanceof FormLayout) {
				panel.setLayoutData(Utils.getFilledFormData());
			} else {
				panel.setLayoutData(new GridData(GridData.FILL_BOTH));
			}
		}

		
		if (managers == null) {
			return;
		}

		int userMode = COConfigurationManager.getIntParameter("User Mode");

			// header 
		
		Composite cHeader = new Composite(panel, SWT.BORDER);
		GridLayout configLayout = new GridLayout();
		configLayout.marginHeight = 3;
		configLayout.marginWidth = 0;
		cHeader.setLayout(configLayout);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		cHeader.setLayoutData(gridData);
		
		Display d = panel.getDisplay();
		cHeader.setBackground(d.getSystemColor(SWT.COLOR_LIST_SELECTION));
		cHeader.setForeground(d.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
		
		Label lHeader = new Label(cHeader, SWT.NULL);
		lHeader.setBackground(d.getSystemColor(SWT.COLOR_LIST_SELECTION));
		lHeader.setForeground(d.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
		FontData[] fontData = lHeader.getFont().getFontData();
		fontData[0].setStyle(SWT.BOLD);
		int fontHeight = (int)(fontData[0].getHeight() * 1.2);
		fontData[0].setHeight(fontHeight);
		headerFont = new Font(d, fontData);
		lHeader.setFont(headerFont);
		
		if ( managers.length == 1 ){
			lHeader.setText( " " + MessageText.getString( "authenticator.torrent" ) + " : " + managers[0].getDisplayName().replaceAll("&", "&&"));
		}else{
			String	str = "";
			
			for (int i=0;i<Math.min( 3, managers.length ); i ++ ){
				
				str += (i==0?"":", ") + managers[i].getDisplayName().replaceAll("&", "&&");
			}
			
			if ( managers.length > 3 ){
				
				str += "...";
			}
			
			lHeader.setText( " " + managers.length + " " + MessageText.getString( "ConfigView.section.torrents" ) + " : " + str );
		}
		
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		lHeader.setLayoutData(gridData);
		
		Group gTorrentOptions = new Group(panel, SWT.NULL);
		Messages.setLanguageText(gTorrentOptions, "ConfigView.section.transfer");
		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
		gTorrentOptions.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 2;
		gTorrentOptions.setLayout(layout);

		//Disabled for release. Need to convert from user-specified units to
	    //KB/s before restoring the following line
	    //String k_unit = DisplayFormatters.getRateUnit(DisplayFormatters.UNIT_KB).trim()
	    String k_unit = DisplayFormatters.getRateUnitBase10(DisplayFormatters.UNIT_KB).trim();

			// max upload speed
		
		Label label = new Label(gTorrentOptions, SWT.NULL);
		gridData = new GridData();
		label.setLayoutData( gridData );
		label.setText(k_unit + " " + MessageText.getString( "GeneralView.label.maxuploadspeed.tooltip" ));

		GenericIntParameter max_upload = new GenericIntParameter(
				adhoc_param_adapter, gTorrentOptions, MAX_UPLOAD);
		adhoc_parameters.put( MAX_UPLOAD, max_upload );
		gridData = new GridData();
		max_upload.setLayoutData(gridData);
		
		if ( userMode > 0) {

				// max upload when busy
			
			label = new Label(gTorrentOptions, SWT.NULL);
			gridData = new GridData();
			label.setLayoutData( gridData );
			Messages.setLanguageText(label, TEXT_PREFIX + "max.uploads.when.busy");
			
			GenericIntParameter max_upload_when_busy = new GenericIntParameter(
					ds_param_adapter, gTorrentOptions,
					DownloadManagerState.PARAM_MAX_UPLOAD_WHEN_BUSY);
			ds_parameters.put( DownloadManagerState.PARAM_MAX_UPLOAD_WHEN_BUSY, max_upload_when_busy );
			gridData = new GridData();
			max_upload_when_busy.setLayoutData(gridData);
		}
		
			// max download speed
		
		label = new Label(gTorrentOptions, SWT.NULL);
		gridData = new GridData();
		label.setLayoutData( gridData );
		label.setText(k_unit + " " + MessageText.getString( "GeneralView.label.maxdownloadspeed.tooltip" ));
	     
		GenericIntParameter max_download = new GenericIntParameter(
				adhoc_param_adapter, gTorrentOptions, MAX_DOWNLOAD);
		adhoc_parameters.put( MAX_DOWNLOAD, max_download );
		gridData = new GridData();
		max_download.setLayoutData(gridData);
		
			// max uploads
		
		if (userMode > 0) {
			label = new Label(gTorrentOptions, SWT.NULL);
			gridData = new GridData();
			label.setLayoutData( gridData );
			Messages.setLanguageText(label, TEXT_PREFIX + "max.uploads" );
			
			GenericIntParameter max_uploads = new GenericIntParameter(
					ds_param_adapter, gTorrentOptions,
					DownloadManagerState.PARAM_MAX_UPLOADS);
			ds_parameters.put( DownloadManagerState.PARAM_MAX_UPLOADS, max_uploads );
			max_uploads.setMinimumValue(2);
			gridData = new GridData();
			max_uploads.setLayoutData(gridData);
			
				//	max uploads when seeding enabled
			
			final Composite cMaxUploadsOptionsArea = new Composite(gTorrentOptions, SWT.NULL);
			layout = new GridLayout();
			layout.numColumns = 3;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			cMaxUploadsOptionsArea.setLayout(layout);
			gridData = new GridData();
			gridData.horizontalIndent = 15;
			gridData.horizontalSpan = 2;
			cMaxUploadsOptionsArea.setLayoutData(gridData);
			
			label = new Label(cMaxUploadsOptionsArea, SWT.NULL);
			ImageLoader.getInstance().setLabelImage(label, "subitem");
			gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			label.setLayoutData(gridData);
	
			gridData = new GridData();
			GenericBooleanParameter	max_uploads_when_seeding_enabled = 
				new GenericBooleanParameter( 
						ds_param_adapter, 
						cMaxUploadsOptionsArea, 
						DownloadManagerState.PARAM_MAX_UPLOADS_WHEN_SEEDING_ENABLED,
						false,
						TEXT_PREFIX + "alternative.value.enable");
			ds_parameters.put( DownloadManagerState.PARAM_MAX_UPLOADS_WHEN_SEEDING_ENABLED, max_uploads_when_seeding_enabled );
			max_uploads_when_seeding_enabled.setLayoutData( gridData );
			
	
			GenericIntParameter max_uploads_when_seeding = new GenericIntParameter(
					ds_param_adapter, cMaxUploadsOptionsArea,
					DownloadManagerState.PARAM_MAX_UPLOADS_WHEN_SEEDING);
			ds_parameters.put( DownloadManagerState.PARAM_MAX_UPLOADS_WHEN_SEEDING, max_uploads_when_seeding );
			gridData = new GridData();
			max_uploads_when_seeding.setMinimumValue(2);
			max_uploads_when_seeding.setLayoutData(gridData);
			
			max_uploads_when_seeding_enabled.setAdditionalActionPerformer(
					new ChangeSelectionActionPerformer( max_uploads_when_seeding.getControl()));
					
				// max peers
			
			label = new Label(gTorrentOptions, SWT.NULL);
			gridData = new GridData();
			label.setLayoutData( gridData );
			Messages.setLanguageText(label, TEXT_PREFIX + "max.peers");
			
			GenericIntParameter max_peers = new GenericIntParameter(ds_param_adapter,
					gTorrentOptions, DownloadManagerState.PARAM_MAX_PEERS);
			ds_parameters.put( DownloadManagerState.PARAM_MAX_PEERS, max_peers );
			gridData = new GridData();
			max_peers.setLayoutData(gridData);
	
				// max peers when seeding
			
			final Composite cMaxPeersOptionsArea = new Composite(gTorrentOptions, SWT.NULL);
			layout = new GridLayout();
			layout.numColumns = 3;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			cMaxPeersOptionsArea.setLayout(layout);
			gridData = new GridData();
			gridData.horizontalIndent = 15;
			gridData.horizontalSpan = 2;
			cMaxPeersOptionsArea.setLayoutData(gridData);
			
			label = new Label(cMaxPeersOptionsArea, SWT.NULL);
			ImageLoader.getInstance().setLabelImage(label, "subitem");
			gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			label.setLayoutData(gridData);
	
			gridData = new GridData();
			GenericBooleanParameter	max_peers_when_seeding_enabled = 
				new GenericBooleanParameter( 
						ds_param_adapter, 
						cMaxPeersOptionsArea, 
						DownloadManagerState.PARAM_MAX_PEERS_WHEN_SEEDING_ENABLED,
						false,
						TEXT_PREFIX + "alternative.value.enable");
			ds_parameters.put( DownloadManagerState.PARAM_MAX_PEERS_WHEN_SEEDING_ENABLED, max_peers_when_seeding_enabled );
			max_peers_when_seeding_enabled.setLayoutData( gridData );
			
	
			GenericIntParameter max_peers_when_seeding = new GenericIntParameter(
					ds_param_adapter, cMaxPeersOptionsArea,
					DownloadManagerState.PARAM_MAX_PEERS_WHEN_SEEDING);
			ds_parameters.put( DownloadManagerState.PARAM_MAX_PEERS_WHEN_SEEDING, max_peers_when_seeding );
			gridData = new GridData();
			max_peers_when_seeding.setLayoutData(gridData);
			
			max_peers_when_seeding_enabled.setAdditionalActionPerformer(
					new ChangeSelectionActionPerformer( max_peers_when_seeding.getControl()));

			
				// max seeds
			
			label = new Label(gTorrentOptions, SWT.NULL);
			gridData = new GridData();
			label.setLayoutData( gridData );
			Messages.setLanguageText(label, TEXT_PREFIX + "max.seeds" );
			
			GenericIntParameter max_seeds = new GenericIntParameter(
					ds_param_adapter, gTorrentOptions,
					DownloadManagerState.PARAM_MAX_SEEDS);
			ds_parameters.put( DownloadManagerState.PARAM_MAX_SEEDS, max_seeds );
			gridData = new GridData();
			max_seeds.setLayoutData(gridData);
		}
		
	    Label reset_label = new Label(gTorrentOptions, SWT.NULL );
	    Messages.setLanguageText(reset_label, TEXT_PREFIX + "reset.to.default");

	    Button reset_button = new Button(gTorrentOptions, SWT.PUSH);

	    Messages.setLanguageText(reset_button, TEXT_PREFIX + "reset.button" );

	    reset_button.addListener(SWT.Selection, 
	    		new Listener() 
				{
			        public void 
					handleEvent(Event event) 
			        {
			        	setDefaults();
			        }
			    });
		
	    for (int i=0;i<managers.length;i++){		
	    	managers[i].getDownloadState().addListener(this, DownloadManagerState.AT_PARAMETERS, DownloadManagerStateAttributeListener.WRITTEN);
	    }
	    
	    panel.layout(true, true);
	}
	
	protected void
	setDefaults()
	{
		Iterator<?>	it = ds_parameters.keySet().iterator();
		
		while( it.hasNext()){
						
			String	key 	= (String)it.next();

		    for (int i=0;i<managers.length;i++){

		    	managers[i].getDownloadState().setParameterDefault( key );
		    }
		}
		
		it = adhoc_parameters.values().iterator();
		
		while ( it.hasNext()){
			
			Object	param 	= it.next();
			
			if ( param instanceof GenericIntParameter ){
				
				GenericIntParameter	int_param = (GenericIntParameter)param;
				
				int_param.setValue( 0, true );
				
			}else{
				Debug.out( "Unknown parameter type: " + param.getClass());
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.download.DownloadManagerStateAttributeListener#attributeEventOccurred(org.gudy.azureus2.core3.download.DownloadManager, java.lang.String, int)
	 */
	public void attributeEventOccurred(DownloadManager dm, String attribute_name, int event_type) {
		final DownloadManagerState state = dm.getDownloadState();
		Utils.execSWTThread(new Runnable() {
			public void	run() {
				Iterator<Entry<String, Object>> it = ds_parameters.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<String, Object>	entry = it.next();
					String	key 	= entry.getKey();
					Object	param 	= entry.getValue();
								
					if (param instanceof GenericIntParameter) {
						GenericIntParameter	int_param = (GenericIntParameter)param;
						int	value = state.getIntParameter( key );
						int_param.setValue( value );
					} else if (param instanceof GenericBooleanParameter) {
						GenericBooleanParameter	bool_param = (GenericBooleanParameter)param;
						boolean	value = state.getBooleanParameter( key );
						bool_param.setSelected( value );
					} else {		
						Debug.out( "Unknown parameter type: " + param.getClass());
					}
				} 
			}
		}, true);
	}
	
	private Composite 
	getComposite() 
	{
		return panel;
	}
	
	private String 
	getFullTitle() 
	{
		return MessageText.getString( multi_view?"TorrentOptionsView.multi.title.full":"TorrentOptionsView.title.full");
	}

	private void 
	delete()
	{
		if ( headerFont != null ){
			
			headerFont.dispose();
		}
		
		if (managers != null) {
			for (int i = 0; i < managers.length; i++) {
				managers[i].getDownloadState().removeListener(this,
						DownloadManagerState.AT_PARAMETERS,
						DownloadManagerStateAttributeListener.WRITTEN);
			}
		}
	}
	
	
	
	protected class
	adhocParameterAdapter
		extends GenericParameterAdapter
	{
		public int
		getIntValue(
			String	key )
		{
			return( getIntValue( key, 0 ));
		}
		
		public int
		getIntValue(
			String	key,
			int		def )
		{
			if ( key == MAX_UPLOAD ){
				int	result = def;
				
				for (int i=0;i<managers.length;i++){
					int	val = managers[i].getStats().getUploadRateLimitBytesPerSecond()/1024;
					
					if ( i==0 ){
						result = val;
					}else if ( result != val ){
						return( def );
					}
				}
				
				return( result );
				
			}else if ( key == MAX_DOWNLOAD ){
				int	result = def;
				
				for (int i=0;i<managers.length;i++){
					int	val = managers[i].getStats().getDownloadRateLimitBytesPerSecond()/1024;
					
					if ( i==0 ){
						result = val;
					}else if ( result != val ){
						return( def );
					}
				}
				
				return( result );
			}else{
				Debug.out( "Unknown key '" + key + "'" );
				return(0);
			}
		}
		
		public void
		setIntValue(
			String	key,
			int		value )
		{
			if ( key == MAX_UPLOAD ){
				for (int i=0;i<managers.length;i++){

					DownloadManager	manager = managers[i];
						
					if ( value != manager.getStats().getUploadRateLimitBytesPerSecond()/1024){
						
						manager.getStats().setUploadRateLimitBytesPerSecond(value*1024);
					}
				}
			}else if ( key == MAX_DOWNLOAD ){
				for (int i=0;i<managers.length;i++){

					DownloadManager	manager = managers[i];
						
					if ( value != manager.getStats().getDownloadRateLimitBytesPerSecond()/1024){
						
						manager.getStats().setDownloadRateLimitBytesPerSecond(value*1024);
					}
				}
			}else{
				Debug.out( "Unknown key '" + key + "'" );
			}
		}		
	}
	
	protected class
	downloadStateParameterAdapter
		extends GenericParameterAdapter
	{
		public int
		getIntValue(
			String	key )
		{
			return( getIntValue( key, 0 ));
		}
		
		public int
		getIntValue(
			String	key,
			int		def )
		{
			int	result = def;
			
			for (int i=0;i<managers.length;i++){
				int	val = managers[i].getDownloadState().getIntParameter( key );
				
				if ( i==0 ){
					result = val;
				}else if ( result != val ){
					return( def );
				}
			}
			
			return( result );
		}
		
		public void
		setIntValue(
			String	key,
			int		value )
		{
			for (int i=0;i<managers.length;i++){

				DownloadManager	manager = managers[i];
				
				if ( value != manager.getDownloadState().getIntParameter( key )){
				
					manager.getDownloadState().setIntParameter( key, value );
				}
			}
		}	
		
		public boolean
		getBooleanValue(
			String	key )
		{
			return( getBooleanValue(key,false));
		}
		
		public boolean
		getBooleanValue(
			String		key,
			boolean		def )
		{
			boolean	result = def;
			
			for (int i=0;i<managers.length;i++){
				boolean	val = managers[i].getDownloadState().getBooleanParameter( key );
				
				if ( i==0 ){
					result = val;
				}else if ( result != val ){
					return( def );
				}
			}
			
			return( result );		
		}
		
		public void
		setBooleanValue(
			String		key,
			boolean		value )
		{
			for (int i=0;i<managers.length;i++){

				DownloadManager	manager = managers[i];
				
				if ( value != manager.getDownloadState().getBooleanParameter( key )){
				
					manager.getDownloadState().setBooleanParameter( key, value );
				}
			}
		}
	}
	
	private void dataSourceChanged(Object newDataSource) {
		if (newDataSource instanceof DownloadManager) {
			multi_view = false;
			managers = new DownloadManager[] { (DownloadManager) newDataSource };
		} else if (newDataSource instanceof DownloadManager[]) {
			multi_view = true;
			managers = (DownloadManager[]) newDataSource;
		}
		if (parent != null) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					initialize(parent);
				}
			});
		}
	}

	public boolean eventOccurred(UISWTViewEvent event) {
    switch (event.getType()) {
      case UISWTViewEvent.TYPE_CREATE:
      	swtView = (UISWTView)event.getData();
      	swtView.setTitle(getFullTitle());
        break;

      case UISWTViewEvent.TYPE_DESTROY:
        delete();
        break;

      case UISWTViewEvent.TYPE_INITIALIZE:
        initialize((Composite)event.getData());
        break;

      case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
      	Messages.updateLanguageForControl(getComposite());
      	swtView.setTitle(getFullTitle());
        break;

      case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
      	dataSourceChanged(event.getData());
        break;
        
      case UISWTViewEvent.TYPE_FOCUSGAINED:
      	break;
        
      case UISWTViewEvent.TYPE_REFRESH:
        break;
    }

    return true;
  }
}
