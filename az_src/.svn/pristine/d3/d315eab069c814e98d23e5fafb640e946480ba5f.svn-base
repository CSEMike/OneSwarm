/*
 * File : CompletionItem.java Created : 24 nov. 2003 By : Olivier
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details ( see the LICENSE file ).
 * 
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 * 
 * AELITIS, SAS au capital de 46,603.30 euros, 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le
 * Roi, France.
 */
package org.gudy.azureus2.ui.swt.views.tableitems.files;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * Torrent Completion Level Graphic Cell for My Torrents.
 * 
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class ProgressGraphItem extends CoreTableColumn implements TableCellAddedListener, TableCellDisposeListener, TableCellVisibilityListener {
	private static final int	borderWidth	= 1;

	/** Default Constructor */
	public ProgressGraphItem() {
		super("pieces", TableManager.TABLE_TORRENT_FILES);
		initializeAsGraphic(POSITION_LAST, 200);
		setMinWidth(100);
	}

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_PROGRESS,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	public void cellVisibilityChanged(TableCell cell, int visibility) {
		if(visibility == VISIBILITY_HIDDEN)
			dispose(cell);
	}

	public void dispose(TableCell cell) {
		// only dispose of image here, this method is reused in other methods
		Graphic graphic = cell.getGraphic();
		if (graphic instanceof UISWTGraphic)
		{
			final Image img = ((UISWTGraphic) graphic).getImage();
			if (img != null && !img.isDisposed())
				img.dispose();
		}
	}

	
	private class Cell implements TableCellLightRefreshListener {
		int				lastPercentDone	= 0;
		private long	last_draw_time	= SystemTime.getCurrentTime();
		private boolean	bNoRed			= false;
		private boolean	was_running		= false;

		public Cell(TableCell cell) {
			cell.setFillCell(true);
			cell.addListeners(this);
		}
		
		public void refresh(TableCell cell) {
			refresh(cell, false);
		}

		public void refresh(TableCell cell, boolean sortOnly) {
			final DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) cell.getDataSource();
			int percentDone = 0;
			if (fileInfo != null && fileInfo.getLength() != 0)
				percentDone = (int) ((1000 * fileInfo.getDownloaded()) / fileInfo.getLength());
			cell.setSortValue(percentDone);
			if (sortOnly)
			{
				dispose(cell);
				return;
			}
			
			//Compute bounds ...
			int newWidth = cell.getWidth();
			if (newWidth <= 0)
				return;
			final int newHeight = cell.getHeight();
			final int x1 = newWidth - borderWidth - 1;
			final int y1 = newHeight - borderWidth - 1;
			
			if (x1 < 10 || y1 < 3)
				return;
			
			final DiskManager manager = fileInfo.getDiskManager();
			// we want to run through the image part once one the transition from with a disk manager (running)
			// to without a disk manager (stopped) in order to clear the pieces view
			boolean running = manager != null;
			boolean hasGraphic = false;
			Graphic graphic = cell.getGraphic();
			if (graphic instanceof UISWTGraphic) {
				Image img = ((UISWTGraphic) graphic).getImage();
				hasGraphic = img != null && !img.isDisposed();
			}
			final boolean bImageBufferValid = (lastPercentDone == percentDone)
					&& cell.isValid() && bNoRed && running == was_running && hasGraphic;
			
			if (bImageBufferValid)
				return;
			
			was_running = running;
			lastPercentDone = percentDone;
			Image piecesImage = null;
			
			if (graphic instanceof UISWTGraphic)
				piecesImage = ((UISWTGraphic) graphic).getImage();
			if (piecesImage != null && !piecesImage.isDisposed())
				piecesImage.dispose();
			
			if (!running) {
				cell.setGraphic(null);
				return;
			}
			
			piecesImage = new Image(SWTThread.getInstance().getDisplay(), newWidth, newHeight);
			final GC gcImage = new GC(piecesImage);
			
			// dm may be null if this is a skeleton file view
			DownloadManager download_manager = fileInfo.getDownloadManager();
			PEPeerManager peer_manager = download_manager == null ? null : download_manager.getPeerManager();
			PEPiece[] pe_pieces = peer_manager == null ? null : peer_manager.getPieces();
			final long now = SystemTime.getCurrentTime();
			
			if (fileInfo != null && manager != null)
			{
				if (percentDone == 1000)
				{
					gcImage.setForeground(Colors.blues[Colors.BLUES_DARKEST]);
					gcImage.setBackground(Colors.blues[Colors.BLUES_DARKEST]);
					gcImage.fillRectangle(1, 1, newWidth - 2, newHeight - 2);
				} else
				{
					final int firstPiece = fileInfo.getFirstPieceNumber();
					final int nbPieces = fileInfo.getNbPieces();
					final DiskManagerPiece[] dm_pieces = manager.getPieces();
					bNoRed = true;
					for (int i = 0; i < newWidth; i++)
					{
						final int a0 = (i * nbPieces) / newWidth;
						int a1 = ((i + 1) * nbPieces) / newWidth;
						if (a1 == a0)
							a1++;
						if (a1 > nbPieces && nbPieces != 0)
							a1 = nbPieces;
						int nbAvailable = 0;
						boolean written = false;
						boolean partially_written = false;
						if (firstPiece >= 0)
							for (int j = a0; j < a1; j++)
							{
								final int this_index = j + firstPiece;
								final DiskManagerPiece dm_piece = dm_pieces[this_index];
								if (dm_piece.isDone())
									nbAvailable++;
								if (written)
									continue;
								if (pe_pieces != null)
								{
									PEPiece pe_piece = pe_pieces[this_index];
									if (pe_piece != null)
										written = written || (pe_piece.getLastDownloadTime(now) + 500) > last_draw_time;
								}
								if ((!written) && (!partially_written))
								{
									final boolean[] blocks = dm_piece.getWritten();
									if (blocks != null)
										for (int k = 0; k < blocks.length; k++)
											if (blocks[k])
											{
												partially_written = true;
												break;
											}
								}
							} // for j
						else
							nbAvailable = 1;
						gcImage.setBackground(written ? Colors.red : partially_written ? Colors.grey : Colors.blues[(nbAvailable * Colors.BLUES_DARKEST) / (a1 - a0)]);
						gcImage.fillRectangle(i, 1, 1, newHeight - 2);
						if (written)
							bNoRed = false;
					}
					gcImage.setForeground(Colors.grey);
				}
			} else
				gcImage.setForeground(Colors.grey);
			
			if (manager != null)
				gcImage.drawRectangle(0, 0, newWidth - 1, newHeight - 1);
			gcImage.dispose();
			
			last_draw_time = now;
			
			if (cell instanceof TableCellSWT)
				((TableCellSWT) cell).setGraphic(piecesImage);
			else
				cell.setGraphic(new UISWTGraphicImpl(piecesImage));
		}
	}
}
