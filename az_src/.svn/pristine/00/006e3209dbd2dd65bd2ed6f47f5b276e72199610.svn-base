/*
 * File    : PiecesItem.java
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

package org.gudy.azureus2.ui.swt.views.tableitems.peers;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.peermanager.piecepicker.util.BitFlags;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/19: modified to TableCellAdapter)
 */
public class PiecesItem
	extends CoreTableColumn
	implements TableCellAddedListener, TableCellRefreshListener,
	TableCellDisposeListener, DiskManagerListener
{
	private final static int INDEX_COLOR_FADEDSTARTS = Colors.BLUES_DARKEST + 1;

	// only supports 0 or 1 border width
	private final static int borderHorizontalSize = 1;

	private final static int borderVerticalSize = 1;

	private final static int borderSplit = 1;

	private final static int completionHeight = 2;

	private int row_count;

	/** Default Constructor */
	public PiecesItem(String table_id) {
		super("pieces", table_id);
		initializeAsGraphic(POSITION_LAST, 200);
	}

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_CONTENT,
		});
	}

	public void cellAdded(TableCell cell) {
		synchronized (this) {
			row_count++;
		}
		cell.setFillCell(true);
		Object ds = cell.getDataSource();
		if (ds instanceof PEPeer) {
			PEPeer peer = (PEPeer) ds;
			DiskManager diskmanager = peer.getManager().getDiskManager();

			if (diskmanager.getRemaining() > 0) {
				if (!diskmanager.hasListener(this)) {
					diskmanager.addListener(this);
				}
			}
		}
	}

	public void dispose(TableCell cell) {
		synchronized (this) {
			row_count--;
		}
		// Named infoObj so code can be copied easily to the other PiecesItem
		PEPeer infoObj = (PEPeer) cell.getDataSource();
		if (infoObj == null)
			return;

		final Image img = (Image) infoObj.getData("PiecesImage");
		Utils.execSWTThread(new AERunnable() {

			public void runSupport() {
				if (img != null && !img.isDisposed())
					img.dispose();
			}
		});

		infoObj.setData("PiecesImageBuffer", null);
		infoObj.setData("PiecesImage", null);
	}

	public void refresh(final TableCell cell) {
		/* Notes:
		 * We store our image and imageBufer in PEPeer using
		 * setData & getData.
		 */

		// Named infoObj so code can be copied easily to the other PiecesItem
		final PEPeer infoObj = (PEPeer) cell.getDataSource();
		long lCompleted = (infoObj == null) ? 0
				: infoObj.getPercentDoneInThousandNotation();

		if (!cell.setSortValue(lCompleted) && cell.isValid()) {
			return;
		}

		if (infoObj == null)
			return;

		Utils.execSWTThread(new AERunnable() {

			public void runSupport() {

				//Compute bounds ...
				int newWidth = cell.getWidth();
				if (newWidth <= 0)
					return;
				int newHeight = cell.getHeight();

				int x0 = borderVerticalSize;
				int x1 = newWidth - 1 - borderVerticalSize;
				int y0 = completionHeight + borderHorizontalSize + borderSplit;
				int y1 = newHeight - 1 - borderHorizontalSize;
				int drawWidth = x1 - x0 + 1;
				if (drawWidth < 10 || y1 < 3)
					return;
				int[] imageBuffer = (int[]) infoObj.getData("PiecesImageBuffer");
				boolean bImageBufferValid = imageBuffer != null
						&& imageBuffer.length == drawWidth;

				Image image = (Image) infoObj.getData("PiecesImage");
				GC gcImage;
				boolean bImageChanged;
				Rectangle imageBounds;
				if (image == null || image.isDisposed()) {
					bImageChanged = true;
				} else {
					imageBounds = image.getBounds();
					bImageChanged = imageBounds.width != newWidth
							|| imageBounds.height != newHeight;
				}
				if (bImageChanged) {
					if (image != null && !image.isDisposed()) {
						image.dispose();
					}
					image = new Image(SWTThread.getInstance().getDisplay(), newWidth,
							newHeight);
					imageBounds = image.getBounds();
					bImageBufferValid = false;

					// draw border
					gcImage = new GC(image);
					gcImage.setForeground(Colors.grey);
					if (borderHorizontalSize > 0) {
						if (borderVerticalSize > 0) {
							gcImage.drawRectangle(0, 0, newWidth - 1, newHeight - 1);
						} else {
							gcImage.drawLine(0, 0, newWidth - 1, 0);
							gcImage.drawLine(0, newHeight - 1, newWidth - 1, newHeight - 1);
						}
					} else if (borderVerticalSize > 0) {
						gcImage.drawLine(0, 0, 0, newHeight - 1);
						gcImage.drawLine(newWidth - 1, 0, newWidth - 1, newHeight - 1);
					}

					if (borderSplit > 0) {
						gcImage.setForeground(Colors.white);
						gcImage.drawLine(x0, completionHeight + borderHorizontalSize, x1,
								completionHeight + borderHorizontalSize);
					}
				} else {
					gcImage = new GC(image);
				}

				final BitFlags peerHave = infoObj.getAvailable();
				boolean established = ((PEPeerTransport) infoObj).getConnectionState() == PEPeerTransport.CONNECTION_FULLY_ESTABLISHED;

				if (established && peerHave != null && peerHave.flags.length > 0) {
					if (imageBuffer == null || imageBuffer.length != drawWidth) {
						imageBuffer = new int[drawWidth];
					}
					final boolean available[] = peerHave.flags;
					try {

						int nbComplete = 0;
						int nbPieces = available.length;

						DiskManager disk_manager = infoObj.getManager().getDiskManager();
						DiskManagerPiece[] pieces = disk_manager == null ? null
								: disk_manager.getPieces();

						int a0;
						int a1 = 0;
						for (int i = 0; i < drawWidth; i++) {
							if (i == 0) {
								// always start out with one piece
								a0 = 0;
								a1 = nbPieces / drawWidth;
								if (a1 == 0)
									a1 = 1;
							} else {
								// the last iteration, a1 will be nbPieces
								a0 = a1;
								a1 = ((i + 1) * nbPieces) / (drawWidth);
							}

							int index;
							int nbNeeded = 0;

							if (a1 <= a0) {
								index = imageBuffer[i - 1];
							} else {
								int nbAvailable = 0;
								for (int j = a0; j < a1; j++) {
									if (available[j]) {
										if (pieces == null || !pieces[j].isDone()) {
											nbNeeded++;
										}
										nbAvailable++;
									}
								}
								nbComplete += nbAvailable;
								index = (nbAvailable * Colors.BLUES_DARKEST) / (a1 - a0);
								if (nbNeeded <= nbAvailable / 2)
									index += INDEX_COLOR_FADEDSTARTS;
							}

							if (imageBuffer[i] != index) {
								imageBuffer[i] = index;
								if (bImageBufferValid) {
									bImageChanged = true;
									if (imageBuffer[i] >= INDEX_COLOR_FADEDSTARTS)
										gcImage.setForeground(Colors.faded[index
												- INDEX_COLOR_FADEDSTARTS]);
									else
										gcImage.setForeground(Colors.blues[index]);
									gcImage.drawLine(i + x0, y0, i + x0, y1);
								}
							}
						}
						if (!bImageBufferValid) {
							if (established) {
								int iLastIndex = imageBuffer[0];
								int iWidth = 1;
								for (int i = 1; i < drawWidth; i++) {
									if (iLastIndex == imageBuffer[i]) {
										iWidth++;
									} else {
										if (iLastIndex >= INDEX_COLOR_FADEDSTARTS) {
											gcImage.setBackground(Colors.faded[iLastIndex
													- INDEX_COLOR_FADEDSTARTS]);
										} else
											gcImage.setBackground(Colors.blues[iLastIndex]);
										gcImage.fillRectangle(i - iWidth + x0, y0, iWidth, y1 - y0
												+ 1);
										iWidth = 1;
										iLastIndex = imageBuffer[i];
									}
								}
								if (iLastIndex >= INDEX_COLOR_FADEDSTARTS)
									gcImage.setBackground(Colors.faded[iLastIndex
											- INDEX_COLOR_FADEDSTARTS]);
								else
									gcImage.setBackground(Colors.blues[iLastIndex]);
								gcImage.fillRectangle(x1 - iWidth + 1, y0, iWidth, y1 - y0 + 1);
								bImageChanged = true;
							}
						}

						int limit = (drawWidth * nbComplete) / nbPieces;
						if (limit < drawWidth) {
							gcImage.setBackground(Colors.blues[Colors.BLUES_LIGHTEST]);
							gcImage.fillRectangle(limit + x0, borderHorizontalSize, x1
									- limit, completionHeight);
						}
						gcImage.setBackground(Colors.colorProgressBar);
						gcImage.fillRectangle(x0, borderHorizontalSize, limit,
								completionHeight);
					} catch (Exception e) {
						System.out.println("Error Drawing PiecesItem");
						Debug.printStackTrace(e);
					}
				} else {
					gcImage.setForeground(Colors.grey);
					gcImage.setBackground(Colors.grey);
					gcImage.fillRectangle(x0, y0, newWidth, y1);
				}
				gcImage.dispose();

				Image oldImage = null;
				Graphic graphic = cell.getGraphic();
				if (graphic instanceof UISWTGraphic) {
					oldImage = ((UISWTGraphic) graphic).getImage();
				}
				if (bImageChanged || image != oldImage || !cell.isValid()) {
					if (cell instanceof TableCellSWT) {
						((TableCellSWT) cell).setGraphic(image);
					} else {
						cell.setGraphic(new UISWTGraphicImpl(image));
					}
					if (bImageChanged || image != oldImage) {
						cell.invalidate();
					}
					infoObj.setData("PiecesImage", image);
					infoObj.setData("PiecesImageBuffer", imageBuffer);
				}
			}
		});
	}

	// @see org.gudy.azureus2.core3.disk.DiskManagerListener#fileAccessModeChanged(org.gudy.azureus2.core3.disk.DiskManagerFileInfo, int, int)
	public void fileAccessModeChanged(DiskManagerFileInfo file, int old_mode,
			int new_mode) {
	}

	// @see org.gudy.azureus2.core3.disk.DiskManagerListener#filePriorityChanged(org.gudy.azureus2.core3.disk.DiskManagerFileInfo)
	public void filePriorityChanged(DiskManagerFileInfo file) {
	}

	// @see org.gudy.azureus2.core3.disk.DiskManagerListener#pieceDoneChanged(org.gudy.azureus2.core3.disk.DiskManagerPiece)
	public void pieceDoneChanged(DiskManagerPiece piece) {
		DiskManager diskmanager = piece.getManager();

		boolean remove_listener;
		synchronized (this) {
			remove_listener = row_count == 0;
		}

		if (remove_listener) {
			diskmanager.removeListener(this);
		} else {
			invalidateCells();

			if (diskmanager.getRemaining() == 0) {

				diskmanager.removeListener(this);
			}
		}
	}

	// @see org.gudy.azureus2.core3.disk.DiskManagerListener#stateChanged(int, int)
	public void stateChanged(int oldState, int newState) {
	}

}
