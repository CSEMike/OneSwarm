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
import org.gudy.azureus2.core3.tracker.host.TRHostTorrent;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.ui.swt.ImageRepository;
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
		TableCellRefreshListener, ObfusticateCellText, TableCellDisposeListener
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

			if (bShowIcon) {
				boolean folder_icon = false;

				// for non-simple torrents the target is always a directory

				if (item != null) {

					TOTorrent torrent = item.getTorrent();

					if (torrent != null && !torrent.isSimpleTorrent()) {

						folder_icon = true;
					}
				}

				if (folder_icon) {

					Image icon = ImageRepository.getFolderImage();

					((TableCellSWT) cell).setIcon(icon);

				} else {

					int sep = name.lastIndexOf('.');

					if (sep < 0)
						sep = 0;

					String ext = name.substring(sep);
					Image icon = ImageRepository.getIconFromExtension(ext);

					if (Constants.isWindows) {
						// recomposite to avoid artifacts - transparency mask does not work
						final Image dstImage = new Image(Display.getCurrent(),
								icon.getBounds().width, icon.getBounds().height);
						GC gc = new GC(dstImage);
						gc.drawImage(icon, 0, 0);
						gc.dispose();
						icon = dstImage;
					}

					// cheat for core, since we really know it's a TabeCellImpl and want to use
					// those special functions not available to Plugins
					((TableCellSWT) cell).setIcon(icon);
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

	public void dispose(TableCell cell) {
		if (bShowIcon && Constants.isWindows) {
			final Image img = ((TableCellSWT) cell).getIcon();
			Image icon = ImageRepository.getFolderImage();
			if (img != null && !img.equals(icon)) {
				((TableCellSWT) cell).setIcon(null);
				if (!img.isDisposed()) {
					img.dispose();
				}
			}
		}
	}
}
