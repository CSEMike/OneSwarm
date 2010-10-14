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
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.views.table.impl.FakeTableCell;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;

import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.common.table.TableColumnCore;

import org.gudy.azureus2.plugins.ui.tables.TableManager;

public class 
TorrentInfoView
	extends AbstractIView
{
	private static final String	TEXT_PREFIX	= "TorrentInfoView.";
		
	private DownloadManager			download_manager;
		
	private Composite 		outer_panel;
	
	private Font 			headerFont;
	private FakeTableCell[] cells;
	
	protected
	TorrentInfoView(
		DownloadManager		_download_manager )
	{
		download_manager	= _download_manager;
	}
	
	public void 
	initialize(
		Composite composite) 
	{
		ScrolledComposite sc = new ScrolledComposite(composite, SWT.V_SCROLL | SWT.H_SCROLL );
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1);
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
		label.setText( MessageText.getString( TEXT_PREFIX + "torrent.encoding" ) + ": " );

		TOTorrent	torrent = download_manager.getTorrent();
		label = new Label(gTorrentInfo, SWT.NULL);
		gridData = new GridData();
		
		label.setLayoutData( gridData );
		label.setText(torrent==null?"":LocaleTorrentUtil.getCurrentTorrentEncoding( torrent ));
		
			// trackers
		
		label = new Label(gTorrentInfo, SWT.NULL);
		gridData = new GridData();
		label.setLayoutData( gridData );
		label.setText( MessageText.getString( "MyTrackerView.tracker" ) + ": " );

		String	trackers = "";
		
		if ( torrent != null ){
			
			TOTorrentAnnounceURLGroup group = torrent.getAnnounceURLGroup();
			
			TOTorrentAnnounceURLSet[]	sets = group.getAnnounceURLSets();
			
			List	tracker_list = new ArrayList();
			
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
				
				active_url = announcer.getTrackerUrl();
				
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
		
		label = new Label(gTorrentInfo, SWT.NULL);
		gridData = new GridData();
		label.setLayoutData( gridData );
		label.setText( trackers );

		
			// columns
				 
		Group gColumns = new Group(panel, SWT.NULL);
		Messages.setLanguageText(gColumns, TEXT_PREFIX + "columns" );
		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
		gColumns.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 4;
		gColumns.setLayout(layout);
		
		Map	usable_cols = new HashMap();
		
		TableColumnManager col_man = TableColumnManager.getInstance();
		
		TableColumnCore[][] cols_sets = {
				col_man.getAllTableColumnCoreAsArray( TableManager.TABLE_MYTORRENTS_INCOMPLETE ),
				col_man.getAllTableColumnCoreAsArray( TableManager.TABLE_MYTORRENTS_COMPLETE ),
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
  				fakeTableCell.setDataSource(download_manager);
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
		
		Collection values = usable_cols.values();
		
		cells = new FakeTableCell[values.size()];
		
		values.toArray( cells );
		
		Arrays.sort( 
				cells,
				new Comparator()
				{
					public int
					compare(
						Object	o1,
						Object	o2 )
					{
						TableColumnCore	c1 = (TableColumnCore) ((TableCellCore)o1).getTableColumn();
						TableColumnCore	c2 = (TableColumnCore) ((TableCellCore)o2).getTableColumn();

						String key1 = MessageText.getString(c1.getTitleLanguageKey());
						String key2 = MessageText.getString(c2.getTitleLanguageKey());
						
						return key1.compareToIgnoreCase(key2);
					}
				});
						
		for (int i=0;i<cells.length;i++){
			
			FakeTableCell	cell = cells[i];
			
			label = new Label(gColumns, SWT.NULL);
			gridData = new GridData();
			if ( i%2 == 1 ){
				gridData.horizontalIndent = 16;
			}
			label.setLayoutData( gridData );
			String key = ((TableColumnCore) cell.getTableColumn()).getTitleLanguageKey();
			label.setText(MessageText.getString(key) + ": ");
			label.setToolTipText(MessageText.getString(key + ".info", ""));

			Composite c = new Composite(gColumns, SWT.NONE);
			gridData = new GridData( GridData.FILL_HORIZONTAL);
			gridData.heightHint = 16;
			c.setLayoutData(gridData);
			cell.setControl(c);
		}
		
		refresh();
		
		sc.setMinSize( panel.computeSize( SWT.DEFAULT, SWT.DEFAULT ));
	}
	
	public void
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

	
	public Composite 
	getComposite() 
	{
		return outer_panel;
	}
	
	public String 
	getFullTitle() 
	{
		return MessageText.getString("GeneralView.section.info");
	}

	public String 
	getData() 
	{
		return( "GeneralView.section.info" );
	}
	
	public void 
	delete()
	{
		super.delete();
		
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
}
