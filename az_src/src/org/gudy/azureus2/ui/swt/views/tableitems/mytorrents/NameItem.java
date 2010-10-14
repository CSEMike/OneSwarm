/*
 * File    : NameItem.java
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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.ObfusticateCellText;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.ui.tables.*;

/** Torrent name cell for My Torrents.
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class NameItem extends CoreTableColumn implements
		TableCellLightRefreshListener, ObfusticateCellText, TableCellDisposeListener
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
	public NameItem(String sTableID) {
		super("name", POSITION_LAST, 250, sTableID);
		setObfustication(true);
		setRefreshInterval(INTERVAL_LIVE);
		setType(TableColumn.TYPE_TEXT);
		setMinWidth(100);
	}

	public void refresh(TableCell cell)
	{
		refresh(cell, false);
	}
	
	public void refresh(TableCell cell, boolean sortOnlyRefresh)
	{
		String name = null;
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		if (dm != null)
			name = dm.getDisplayName();
		if (name == null)
			name = "";

		//setText returns true only if the text is updated
		if ((cell.setText(name) || !cell.isValid())) {
			if (dm != null && bShowIcon && !sortOnlyRefresh
					&& (cell instanceof TableCellSWT)) {
				String path = dm.getDownloadState().getPrimaryFile();
				if (path != null) {
					// Don't ever dispose of PathIcon, it's cached and may be used elsewhere
					TOTorrent torrent = dm.getTorrent();
					Image icon = ImageRepository.getPathIcon(path,torrent != null && !torrent.isSimpleTorrent());
					((TableCellSWT) cell).setIcon(icon);
				}
			}
		}
	}

	public String getObfusticatedText(TableCell cell) {
		String name = null;
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		if (dm != null) {
			name = dm.toString();
			int i = name.indexOf('#');
			if (i > 0) {
				name = name.substring(i + 1);
			}
		}

		if (name == null)
			name = "";
		return name;
	}

	public void dispose(TableCell cell) {
		
	}

	private void disposeCellIcon(TableCell cell) {
		if (!(cell instanceof TableCellSWT)) {
			return;
		}
		final Image img = ((TableCellSWT) cell).getIcon();
		if (img != null) {
			((TableCellSWT) cell).setIcon(null);
			if (!img.isDisposed()) {
				img.dispose();
			}
		}
	}
}
