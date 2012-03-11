/*
 * File    : StatusItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class StatusItem
	extends CoreTableColumn
	implements TableCellRefreshListener, TableCellMouseListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "status";

	private final static Object CLICK_KEY = new Object();
	private static final int[] BLUE = Utils.colorToIntArray( Colors.blue );
	
	private boolean changeRowFG;
	private boolean changeCellFG = true;
	
	private boolean	showTrackerErrors;
	
	public StatusItem(String sTableID, boolean changeRowFG) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_LEAD, 80, sTableID);
		this.changeRowFG = changeRowFG;
		setRefreshInterval(INTERVAL_LIVE);
	}

	public StatusItem(String sTableID) {
		this(sTableID, true);
	}

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_ESSENTIAL,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	public void 
	refresh(
		TableCell cell ) 
	{
		DownloadManager dm = (DownloadManager) cell.getDataSource();

		if ( dm == null ){
			
			return;
		}
		
		int state = dm.getState();
		
		String	text;
		
		if ( showTrackerErrors && dm.isUnauthorisedOnTracker() && state != DownloadManager.STATE_ERROR ){
			
			text = dm.getTrackerStatus();
			
		}else{
			
			text = DisplayFormatters.formatDownloadStatus(dm);
		}
		
		if ( cell.setText( text ) || !cell.isValid()) {
			
			boolean clickable = false;
			
			if ( cell instanceof TableCellSWT ){
								
				int cursor_id;
				
				if ( text.indexOf( "http://" ) == -1 ){
									
					dm.setUserData( CLICK_KEY, null );
					
					cursor_id = SWT.CURSOR_ARROW;
					
				}else{
					
					dm.setUserData( CLICK_KEY, text );
					
					cursor_id = SWT.CURSOR_HAND;
					
					clickable = true;
				}
				
				((TableCellSWT)cell).setCursorID( cursor_id );
			}
			
			if (!changeCellFG && !changeRowFG){
				
					// clickable, make it blue whatever
				
				cell.setForeground( clickable?BLUE:null);
				
				return;
			}
			
			TableRow row = cell.getTableRow();
			
			if (row != null ) {
				
				Color color = null;
				if (state == DownloadManager.STATE_SEEDING) {
					color = Colors.blues[Colors.BLUES_MIDDARK];
				} else if (state == DownloadManager.STATE_ERROR) {
					color = Colors.colorError;
				} else {
					color = null;
				}
				if (changeRowFG) {
					row.setForeground(Utils.colorToIntArray(color));
				} else if (changeCellFG) {
					cell.setForeground(Utils.colorToIntArray(color));
				}
				if ( clickable ){
					cell.setForeground( Utils.colorToIntArray( Colors.blue ));
				}

			}
		}
	}

	public boolean isChangeRowFG() {
		return changeRowFG;
	}

	public void setChangeRowFG(boolean changeRowFG) {
		this.changeRowFG = changeRowFG;
	}

	public boolean isChangeCellFG() {
		return changeCellFG;
	}

	public void setChangeCellFG(boolean changeCellFG) {
		this.changeCellFG = changeCellFG;
	}

	public void
	setShowTrackerErrors(
		boolean	s )
	{
		showTrackerErrors = s;
	}
	
	public void 
	cellMouseTrigger(
		TableCellMouseEvent event ) 
	{

		DownloadManager dm = (DownloadManager) event.cell.getDataSource();
		if (dm == null) {return;}
		
		String clickable = (String)dm.getUserData( CLICK_KEY );
		
		if ( clickable == null ){
			
			return;
		}
		
		event.skipCoreFunctionality = true;
		
		if ( event.eventType == TableCellMouseEvent.EVENT_MOUSEUP ){
		
			String url = UrlUtils.getURL( clickable );
			
			if ( url != null ){
				
				Utils.launch( url );
			}
		}
	}
}
