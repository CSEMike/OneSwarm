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

import java.net.URL;
import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.LocaleTorrentUtil;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLGroup;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLSet;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.download.DownloadTypeComplete;
import org.gudy.azureus2.plugins.download.DownloadTypeIncomplete;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.BufferedLabel;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;
import org.gudy.azureus2.ui.swt.views.table.impl.FakeTableCell;

import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;

public class TorrentInfoView
	implements UISWTViewCoreEventListener
{
	public static final String MSGID_PREFIX = "TorrentInfoView";
		
	private DownloadManager			download_manager;
		
	private Composite 		outer_panel;
	
	private Font 			headerFont;
	private FakeTableCell[] cells;

	private ScrolledComposite sc;

	private Composite parent;

	private UISWTView swtView;
	
	private void 
	initialize(
		Composite composite) 
	{
		this.parent = composite;
		
		if (download_manager == null) {
			return;
		}
		
		// cheap trick to allow datasource changes.  Normally we'd just
		// refill the components with new info, but I didn't write this and
		// I don't want to waste my time :) [tux]
		if (sc != null && !sc.isDisposed()) {
			sc.dispose();
		}

		sc = new ScrolledComposite(composite, SWT.V_SCROLL | SWT.H_SCROLL );
		sc.getVerticalBar().setIncrement(16);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true, 1, 1);
		sc.setLayoutData(gridData);	
		
		outer_panel = sc;
		
		Composite panel = new Composite(sc, SWT.NULL);
		
		sc.setContent( panel );
		
		
		
		GridLayout  layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 1;
		panel.setLayout(layout);

		//int userMode = COConfigurationManager.getIntParameter("User Mode");

			// header 
		
		Composite cHeader = new Composite(panel, SWT.BORDER);
		GridLayout configLayout = new GridLayout();
		configLayout.marginHeight = 3;
		configLayout.marginWidth = 0;
		cHeader.setLayout(configLayout);
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
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
		lHeader.setText( " " + MessageText.getString( "authenticator.torrent" ) + " : " + download_manager.getDisplayName().replaceAll("&", "&&"));
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		lHeader.setLayoutData(gridData);
		
		Composite gTorrentInfo = new Composite(panel, SWT.NULL);
		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
		gTorrentInfo.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 2;
		gTorrentInfo.setLayout(layout);

			// torrent encoding
		
		
		Label label = new Label(gTorrentInfo, SWT.NULL);
		gridData = new GridData();
		label.setLayoutData( gridData );
		label.setText( MessageText.getString( MSGID_PREFIX + ".torrent.encoding" ) + ": " );

		TOTorrent	torrent = download_manager.getTorrent();
		BufferedLabel blabel = new BufferedLabel(gTorrentInfo, SWT.NULL);
		gridData = new GridData();
		
		blabel.setLayoutData( gridData );
		blabel.setText(torrent==null?"":LocaleTorrentUtil.getCurrentTorrentEncoding( torrent ));
		
			// trackers
		
		label = new Label(gTorrentInfo, SWT.NULL);
		gridData = new GridData();
		label.setLayoutData( gridData );
		label.setText( MessageText.getString( "MyTrackerView.tracker" ) + ": " );

		String	trackers = "";
		
		if ( torrent != null ){
			
			TOTorrentAnnounceURLGroup group = torrent.getAnnounceURLGroup();
			
			TOTorrentAnnounceURLSet[]	sets = group.getAnnounceURLSets();
			
			List<String>	tracker_list = new ArrayList<String>();
			
			URL	url = torrent.getAnnounceURL();
			
			tracker_list.add( url.getHost() + (url.getPort()==-1?"":(":"+url.getPort())));
				
			for (int i=0;i<sets.length;i++){
										
				TOTorrentAnnounceURLSet	set = sets[i];
				
				URL[]	urls = set.getAnnounceURLs();
				
				for (int j=0;j<urls.length;j++){
				
					url = urls[j];
					
					String	str = url.getHost() + (url.getPort()==-1?"":(":"+url.getPort()));
					
					if ( !tracker_list.contains(str )){
						
						tracker_list.add(str);
					}
				}
			}
				
			TRTrackerAnnouncer announcer = download_manager.getTrackerClient();
			
			URL	active_url = null;
			
			if ( announcer != null ){
				
				active_url = announcer.getTrackerURL();
				
			}else{
				
				TRTrackerScraperResponse scrape = download_manager.getTrackerScrapeResponse();
				
				if ( scrape != null ){
					
					active_url = scrape.getURL();
				}
			}
			
			if ( active_url == null ){
				
				active_url = torrent.getAnnounceURL();
			}
			
			trackers = active_url.getHost() + (active_url.getPort()==-1?"":(":"+active_url.getPort()));
		
			tracker_list.remove( trackers );
			
			if ( tracker_list.size() > 0 ){
				
				trackers += " (";
				
				for (int i=0;i<tracker_list.size();i++){
					
					trackers += (i==0?"":", ") + tracker_list.get(i);
				}
				
				trackers += ")";
			}
		}
		
		blabel = new BufferedLabel(gTorrentInfo, SWT.NULL);
		gridData = new GridData();
		blabel.setLayoutData( gridData );
		blabel.setText( trackers );

		
			// columns
				 
		Group gColumns = new Group(panel, SWT.NULL);
		Messages.setLanguageText(gColumns, MSGID_PREFIX + ".columns" );
		gridData = new GridData(GridData.FILL_BOTH);
		gColumns.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 4;
		gColumns.setLayout(layout);
		
		Map<String, FakeTableCell>	usable_cols = new HashMap<String, FakeTableCell>();
		
		TableColumnManager col_man = TableColumnManager.getInstance();
		
		TableColumnCore[][] cols_sets = {
			col_man.getAllTableColumnCoreAsArray(DownloadTypeIncomplete.class,
					TableManager.TABLE_MYTORRENTS_INCOMPLETE),
			col_man.getAllTableColumnCoreAsArray(DownloadTypeComplete.class,
					TableManager.TABLE_MYTORRENTS_COMPLETE),
		};
				
		for (int i=0;i<cols_sets.length;i++){
			
			TableColumnCore[]	cols = cols_sets[i];
			
			for (int j=0;j<cols.length;j++){
				
				TableColumnCore	col = cols[j];
				
				String id = col.getName();
			
				if (usable_cols.containsKey(id)) {
					
					continue;
				}
				
				FakeTableCell fakeTableCell = null;
				try {
  				fakeTableCell = new FakeTableCell(col);
  				fakeTableCell.setOrentation(SWT.LEFT);
  				fakeTableCell.setWrapText(false);
  				fakeTableCell.setDataSource(download_manager);
					col.invokeCellAddedListeners(fakeTableCell);
  				// One refresh to see if it throws up
  				fakeTableCell.refresh();
  				usable_cols.put(id, fakeTableCell);
				} catch (Throwable t) {
					//System.out.println("not usable col: " + id + " - " + Debug.getCompressedStackTrace());
					try {
						if (fakeTableCell != null) {
							fakeTableCell.dispose();
						}
					} catch (Throwable t2) {
						//ignore;
					}
				}
			}
		}
		
		Collection<FakeTableCell> values = usable_cols.values();
		
		cells = new FakeTableCell[values.size()];
		
		values.toArray( cells );
		
		Arrays.sort( 
				cells,
				new Comparator<FakeTableCell>()
				{
					public int compare(FakeTableCell o1, FakeTableCell o2) {
						TableColumnCore	c1 = (TableColumnCore) o1.getTableColumn();
						TableColumnCore	c2 = (TableColumnCore) o2.getTableColumn();

						String key1 = MessageText.getString(c1.getTitleLanguageKey());
						String key2 = MessageText.getString(c2.getTitleLanguageKey());
						
						return key1.compareToIgnoreCase(key2);
					}
				});
						
		for (int i=0;i<cells.length;i++){
			
			final FakeTableCell	cell = cells[i];
			
			label = new Label(gColumns, SWT.NULL);
			gridData = new GridData();
			if ( i%2 == 1 ){
				gridData.horizontalIndent = 16;
			}
			label.setLayoutData( gridData );
			String key = ((TableColumnCore) cell.getTableColumn()).getTitleLanguageKey();
			label.setText(MessageText.getString(key) + ": ");
			label.setToolTipText(MessageText.getString(key + ".info", ""));

			final Composite c = new Composite(gColumns, SWT.NONE);
			gridData = new GridData( GridData.FILL_HORIZONTAL);
			gridData.heightHint = 16;
			c.setLayoutData(gridData);
			cell.setControl(c);
			cell.invalidate();
			cell.refresh();
			c.addListener(SWT.MouseHover, new Listener() {
				public void handleEvent(Event event) {
					Object toolTip = cell.getToolTip();
					if (toolTip instanceof String) {
						String s = (String) toolTip;
						c.setToolTipText(s);
					}
				}
			});
		}
		
		refresh();
		
		sc.setMinSize( panel.computeSize( SWT.DEFAULT, SWT.DEFAULT ));
	}
	
	private void
	refresh()
	{
		if ( cells != null ){
			
			for (int i=0;i<cells.length;i++){
				
				TableCellCore cell = cells[i];
				try {cell.refresh();}
				catch (Exception e) {Debug.printStackTrace(e, "Error refreshing cell: " + cells[i].getTableColumn().getName());}
			}
		}
	}

	
	private Composite 
	getComposite() 
	{
		return outer_panel;
	}
	
	private String 
	getFullTitle() 
	{
		return MessageText.getString("TorrentInfoView.title.full");
	}

	private void 
	delete()
	{
		if ( headerFont != null ){
			
			headerFont.dispose();
		}
		
		if ( cells != null ){
			
			for (int i=0;i<cells.length;i++){
				
				TableCellCore	cell = cells[i];
				
				cell.dispose();
			}
		}
	}
	
	private void dataSourceChanged(Object newDataSource) {
		if (newDataSource instanceof DownloadManager) {
			download_manager = (DownloadManager) newDataSource;
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (parent != null) {
					initialize(parent);
				}
			}
		});
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
        refresh();
        break;
    }

    return true;
  }
}
