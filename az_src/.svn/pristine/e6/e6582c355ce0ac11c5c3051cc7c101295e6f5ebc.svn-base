/*
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

package org.gudy.azureus2.ui.swt.views.tableitems.mytracker;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.tracker.host.TRHostTorrent;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.ObfusticateCellText;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.ui.tables.*;

/**
 *
 * @author TuxPaper
 * @since 2.0.8.5
 */
public class NameItem extends CoreTableColumn implements
		TableCellRefreshListener, ObfusticateCellText
{
	private static boolean bShowIcon;

	static {
		COConfigurationManager.addAndFireParameterListener(
				"NameColumn.showProgramIcon", new ParameterListener() {
					public void parameterChanged(String parameterName) {
						bShowIcon = COConfigurationManager.getBooleanParameter("NameColumn.showProgramIcon");
					}
				});
	}

	/** Default Constructor */
	public NameItem() {
		super("name", POSITION_LAST, 250, TableManager.TABLE_MYTRACKER);
		setType(TableColumn.TYPE_TEXT);
	}

	public void refresh(TableCell cell) {
		TRHostTorrent item = (TRHostTorrent) cell.getDataSource();
		String name = (item == null) ? ""
				: TorrentUtils.getLocalisedName(item.getTorrent());
		//setText returns true only if the text is updated

		if (cell.setText(name) || !cell.isValid()) {
			if (item != null && item.getTorrent() != null && bShowIcon 
					&& (cell instanceof TableCellSWT)) {
				try {
	  				final TOTorrent torrent = item.getTorrent();
	  				final String path = torrent.getFiles()[0].getRelativePath();
	  				
	  				if ( path != null ){
	  					
	  					Image icon = null;
	  					
	  					final TableCellSWT _cell = (TableCellSWT)cell;

						// Don't ever dispose of PathIcon, it's cached and may be used elsewhere
						
						if ( Utils.isSWTThread()){
						
							icon = ImageRepository.getPathIcon(path, false, torrent != null
		  							&& !torrent.isSimpleTorrent());
						}else{	
								// happens rarely (seen of filtering of file-view rows
								// when a new row is added )
													
							Utils.execSWTThread(
								new Runnable()
								{
									public void
									run()
									{
										Image icon = ImageRepository.getPathIcon(path, false, torrent != null
					  							&& !torrent.isSimpleTorrent());
										
										_cell.setIcon(icon);
										
										_cell.redraw();
									}
								});
						}
	  					
	  					
						if ( icon != null ){
							
							_cell.setIcon(icon);
						}
	  				}
				} catch (Exception e) {
				}
			}
		}
	}

	public String getObfusticatedText(TableCell cell) {
		TRHostTorrent item = (TRHostTorrent) cell.getDataSource();
		String name = null;
		
		try {
			name = ByteFormatter.nicePrint(item.getTorrent().getHash(), true);
		} catch (TOTorrentException e) {
		}

		if (name == null)
			name = "";
		return name;
	}
}
