/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.views.piece;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerPieceListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.Legend;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.debug.UIDebugGenerator;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.AbstractIView;

/**
 * @author TuxPaper
 * @created Feb 26, 2007
 *
 */
public class PieceInfoView
	extends AbstractIView
	implements ObfusticateImage, DownloadManagerPieceListener
{

	private final static int BLOCK_FILLSIZE = 14;

	private final static int BLOCK_SPACING = 3;

	private final static int BLOCK_SIZE = BLOCK_FILLSIZE + BLOCK_SPACING;

	private final static int BLOCKCOLOR_HAVE = 0;

	private final static int BLOCKCOLORL_NOHAVE = 1;

	private final static int BLOCKCOLOR_TRANSFER = 2;

	private final static int BLOCKCOLOR_NEXT = 3;

	private final static int BLOCKCOLOR_AVAILCOUNT = 4;

	private Composite pieceInfoComposite;

	private ScrolledComposite sc;

	protected Canvas pieceInfoCanvas;

	private Color[] blockColors;

	private Label topLabel;

	private Label imageLabel;

	// More delay for this view because of high workload
	private int graphicsUpdate = COConfigurationManager.getIntParameter("Graphics Update") * 2;

	private int loopFactor = 0;

	private Font font = null;

	Image img = null;

	private Comparator compFindPEPiece;

	private DownloadManager dlm;

	/**
	 * Initialize
	 *
	 */
	public PieceInfoView() {
		blockColors = new Color[] {
			Colors.blues[Colors.BLUES_DARKEST],
			Colors.white,
			Colors.red,
			Colors.fadedRed,
			Colors.black
		};

		compFindPEPiece = new Comparator() {
			public int compare(Object arg0, Object arg1) {
				int arg0no = (arg0 instanceof PEPiece)
						? ((PEPiece) arg0).getPieceNumber() : ((Long) arg0).intValue();
				int arg1no = (arg1 instanceof PEPiece)
						? ((PEPiece) arg1).getPieceNumber() : ((Long) arg1).intValue();
				return arg1no - arg0no;
			}
		};
	}

	public void dataSourceChanged(Object newDataSource) {
		if (newDataSource instanceof DownloadManager) {
			if (dlm != null) {
				dlm.removePieceListener(this);
			}
			dlm = (DownloadManager)newDataSource;
			dlm.addPieceListener(this, false);
			fillPieceInfoSection();
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.views.AbstractIView#getData()
	 */
	public String getData() {
		return "PeersView.BlockView.title";
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.views.AbstractIView#initialize(org.eclipse.swt.widgets.Composite)
	 */
	public void initialize(Composite composite) {
		if (pieceInfoComposite != null && !pieceInfoComposite.isDisposed()) {
			Logger.log(new LogEvent(LogIDs.GUI, LogEvent.LT_ERROR,
					"PeerInfoView already initialized! Stack: "
							+ Debug.getStackTrace(true, false)));
			delete();
		}
		createPeerInfoPanel(composite);
		
		fillPieceInfoSection();
	}

	private Composite createPeerInfoPanel(Composite parent) {
		GridLayout layout;
		GridData gridData;

		// Peer Info section contains
		// - Peer's Block display
		// - Peer's Datarate
		pieceInfoComposite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		pieceInfoComposite.setLayout(layout);
		gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
		pieceInfoComposite.setLayoutData(gridData);

		imageLabel = new Label(pieceInfoComposite, SWT.NULL);
		gridData = new GridData();
		imageLabel.setLayoutData(gridData);

		topLabel = new Label(pieceInfoComposite, SWT.NULL);
		gridData = new GridData(SWT.FILL, SWT.DEFAULT, false, false);
		topLabel.setLayoutData(gridData);

		sc = new ScrolledComposite(pieceInfoComposite, SWT.V_SCROLL);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		layout = new GridLayout();
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		sc.setLayout(layout);
		gridData = new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1);
		sc.setLayoutData(gridData);
		sc.getVerticalBar().setIncrement(BLOCK_SIZE);

		pieceInfoCanvas = new Canvas(sc, SWT.NO_REDRAW_RESIZE | SWT.NO_BACKGROUND);
		gridData = new GridData(GridData.FILL, SWT.DEFAULT, true, false);
		pieceInfoCanvas.setLayoutData(gridData);
		pieceInfoCanvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				if (e.width <= 0 || e.height <= 0)
					return;
				try {
					Rectangle bounds = (img == null) ? null : img.getBounds();
					if (bounds == null) {
						e.gc.fillRectangle(e.x, e.y, e.width, e.height);
					} else {
						if (e.x + e.width > bounds.width)
							e.gc.fillRectangle(bounds.width, e.y, e.x + e.width
									- bounds.width + 1, e.height);
						if (e.y + e.height > bounds.height)
							e.gc.fillRectangle(e.x, bounds.height, e.width, e.y + e.height
									- bounds.height + 1);

						int width = Math.min(e.width, bounds.width - e.x);
						int height = Math.min(e.height, bounds.height - e.y);
						e.gc.drawImage(img, e.x, e.y, width, height, e.x, e.y, width,
								height);
					}
				} catch (Exception ex) {
				}
			}
		});
		Listener doNothingListener = new Listener() {
			public void handleEvent(Event event) {
			}
		};
		pieceInfoCanvas.addListener(SWT.KeyDown, doNothingListener);

		pieceInfoCanvas.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				// wrap in asyncexec because sc.setMinWidth (called later) doesn't work
				// too well inside a resize (the canvas won't size isn't always updated)
				e.widget.getDisplay().asyncExec(new AERunnable() {
					public void runSupport() {
						if (img != null) {
							int iOldColCount = img.getBounds().width / BLOCK_SIZE;
							int iNewColCount = pieceInfoCanvas.getClientArea().width
									/ BLOCK_SIZE;
							if (iOldColCount != iNewColCount)
								refreshInfoCanvas();
						}
					}
				});
			}
		});

		sc.setContent(pieceInfoCanvas);

		Legend.createLegendComposite(pieceInfoComposite,
				blockColors, new String[] {
					"PiecesView.BlockView.Have",
					"PiecesView.BlockView.NoHave",
					"PeersView.BlockView.Transfer",
					"PeersView.BlockView.NextRequest",
					"PeersView.BlockView.AvailCount"
				}, new GridData(SWT.FILL, SWT.DEFAULT, true, false, 2, 1));

		int iFontPixelsHeight = 10;
		int iFontPointHeight = (iFontPixelsHeight * 72)
				/ pieceInfoCanvas.getDisplay().getDPI().y;
		Font f = pieceInfoCanvas.getFont();
		FontData[] fontData = f.getFontData();
		fontData[0].setHeight(iFontPointHeight);
		font = new Font(pieceInfoCanvas.getDisplay(), fontData);

		return pieceInfoComposite;
	}

	private boolean alreadyFilling = false;

	public void fillPieceInfoSection() {
		if (alreadyFilling) {
			return;
		}
		alreadyFilling = true;
		Utils.execSWTThread(new AERunnable() {
			// @see org.gudy.azureus2.core3.util.AERunnable#runSupport()
			public void runSupport() {
				try {
  				if (imageLabel == null || imageLabel.isDisposed()) {
  					return;
  				}
  
  				if (imageLabel.getImage() != null) {
  					Image image = imageLabel.getImage();
  					imageLabel.setImage(null);
  					image.dispose();
  				}
  
  				refreshInfoCanvas();
				} finally {
					alreadyFilling = false;
				}
			}
		});
	}

	public void refresh() {
		super.refresh();

		if (loopFactor++ % graphicsUpdate == 0) {
			refreshInfoCanvas();
		}
	}

	protected void refreshInfoCanvas() {
		pieceInfoCanvas.layout(true);
		Rectangle bounds = pieceInfoCanvas.getClientArea();
		if (bounds.width <= 0 || bounds.height <= 0) {
			topLabel.setText("");
			return;
		}

		if (img != null && !img.isDisposed()) {
			img.dispose();
			img = null;
		}

		if (dlm == null) {
			GC gc = new GC(pieceInfoCanvas);
			gc.fillRectangle(bounds);
			gc.dispose();
			topLabel.setText("");

			return;
		}

		PEPeerManager pm = dlm.getPeerManager();

		DiskManager dm = dlm.getDiskManager();

		if (pm == null || dm == null) {
			GC gc = new GC(pieceInfoCanvas);
			gc.fillRectangle(bounds);
			gc.dispose();
			topLabel.setText("");

			return;
		}

		DiskManagerPiece[] dm_pieces = dm.getPieces();

		PEPiece[] currentDLPieces = pm.getPieces();
		byte[] uploadingPieces = new byte[dm_pieces.length];

		// find upload pieces
		PEPeer[] peers = (PEPeer[]) pm.getPeers().toArray(new PEPeer[0]);
		for (int i = 0; i < peers.length; i++) {
			PEPeer peer = peers[i];
			int[] peerRequestedPieces = peer.getIncomingRequestedPieceNumbers();
			if (peerRequestedPieces != null && peerRequestedPieces.length > 0) {
				int pieceNum = peerRequestedPieces[0];
				if(uploadingPieces[pieceNum] < 2)
					uploadingPieces[pieceNum] = 2;
				for (int j = 1; j < peerRequestedPieces.length; j++) {
					pieceNum = peerRequestedPieces[j];
					if(uploadingPieces[pieceNum] < 1)
						uploadingPieces[pieceNum] = 1;
				}
			}

		}

		int iNumCols = bounds.width / BLOCK_SIZE;
		int iNeededHeight = (((dm.getNbPieces() - 1) / iNumCols) + 1) * BLOCK_SIZE;
		if (sc.getMinHeight() != iNeededHeight) {
			sc.setMinHeight(iNeededHeight);
			sc.layout(true, true);
			bounds = pieceInfoCanvas.getClientArea();
		}

		int[] availability = pm.getAvailability();

		int minAvailability = Integer.MAX_VALUE;
		int minAvailability2 = Integer.MAX_VALUE;
		if (availability != null && availability.length > 0) {
			for (int i = 0; i < availability.length; i++) {
				if (availability[i] != 0 && availability[i] < minAvailability) {
					minAvailability2 = minAvailability;
					minAvailability = availability[i];
					if (minAvailability == 1) {
						break;
					}
				}
			}
		}

		img = new Image(pieceInfoCanvas.getDisplay(), bounds.width, iNeededHeight);
		GC gcImg = new GC(img);
		
		
		int iRow = 0;
		try {
			// use advanced capabilities for faster drawText
			gcImg.setAdvanced(true);
			
			gcImg.setBackground(pieceInfoCanvas.getBackground());
			gcImg.fillRectangle(0, 0, bounds.width, iNeededHeight);
			
			gcImg.setFont(font);

			int iCol = 0;
			for (int i = 0; i < dm_pieces.length; i++) {
				if (iCol >= iNumCols) {
					iCol = 0;
					iRow++;
				}

				int colorIndex;
				boolean done = (dm_pieces == null) ? false : dm_pieces[i].isDone();
				int iXPos = iCol * BLOCK_SIZE + 1;
				int iYPos = iRow * BLOCK_SIZE + 1;

				if (done) {
					colorIndex = BLOCKCOLOR_HAVE;

					gcImg.setBackground(blockColors[colorIndex]);
					gcImg.fillRectangle(iXPos, iYPos, BLOCK_FILLSIZE, BLOCK_FILLSIZE);
				} else {
					// !done
					boolean partiallyDone = (dm_pieces == null) ? false
							: dm_pieces[i].getNbWritten() > 0;

					int x = iXPos;
					int width = BLOCK_FILLSIZE;
					if (partiallyDone) {
						colorIndex = BLOCKCOLOR_HAVE;

						gcImg.setBackground(blockColors[colorIndex]);

						int iNewWidth = (int) (((float) dm_pieces[i].getNbWritten() / dm_pieces[i].getNbBlocks()) * width);
						if (iNewWidth >= width)
							iNewWidth = width - 1;
						else if (iNewWidth <= 0)
							iNewWidth = 1;

						gcImg.fillRectangle(x, iYPos, iNewWidth, BLOCK_FILLSIZE);
						width -= iNewWidth;
						x += iNewWidth;
					}

					colorIndex = BLOCKCOLORL_NOHAVE;

					gcImg.setBackground(blockColors[colorIndex]);
					gcImg.fillRectangle(x, iYPos, width, BLOCK_FILLSIZE);
				}

				if (currentDLPieces[i] != null && currentDLPieces[i].hasUndownloadedBlock()) {
					drawDownloadIndicator(gcImg, iXPos, iYPos, false);
				}

				if (uploadingPieces[i] > 0)
					drawUploadIndicator(gcImg, iXPos, iYPos, uploadingPieces[i] < 2);



				if (availability != null) {
					if (minAvailability == availability[i]) {
						gcImg.setForeground(blockColors[BLOCKCOLOR_AVAILCOUNT]);
						gcImg.drawRectangle(iXPos - 1, iYPos - 1, BLOCK_FILLSIZE + 1,
								BLOCK_FILLSIZE + 1);
					}
					if (minAvailability2 == availability[i]) {
						gcImg.setLineStyle(SWT.LINE_DOT);
						gcImg.setForeground(blockColors[BLOCKCOLOR_AVAILCOUNT]);
						gcImg.drawRectangle(iXPos - 1, iYPos - 1, BLOCK_FILLSIZE + 1,
								BLOCK_FILLSIZE + 1);
						gcImg.setLineStyle(SWT.LINE_SOLID);
					}

					String sNumber = String.valueOf(availability[i]);
					Point size = gcImg.stringExtent(sNumber);

					if (availability[i] < 100) {
						int x = iXPos + (BLOCK_FILLSIZE / 2) - (size.x / 2);
						int y = iYPos + (BLOCK_FILLSIZE / 2) - (size.y / 2);
						gcImg.setForeground(blockColors[BLOCKCOLOR_AVAILCOUNT]);
						gcImg.drawText(sNumber, x, y, true);
					}
				}

				iCol++;
			}
		} catch (Exception e) {
			Logger.log(new LogEvent(LogIDs.GUI, "drawing piece map", e));
		} finally {
			gcImg.dispose();
		}

		topLabel.setText(MessageText.getString("PiecesView.BlockView.Header",
				new String[] {
					"" + iNumCols,
					"" + (iRow + 1),
					"" + dm_pieces.length
				}));

		pieceInfoCanvas.redraw();
	}

	private void drawDownloadIndicator(GC gcImg, int iXPos, int iYPos,
			boolean small) {
		if (small) {
			gcImg.setBackground(blockColors[BLOCKCOLOR_NEXT]);
			gcImg.fillPolygon(new int[] {
				iXPos + 2,
				iYPos + 2,
				iXPos + BLOCK_FILLSIZE - 1,
				iYPos + 2,
				iXPos + (BLOCK_FILLSIZE / 2),
				iYPos + BLOCK_FILLSIZE - 1
			});
		} else {
			gcImg.setBackground(blockColors[BLOCKCOLOR_TRANSFER]);
			gcImg.fillPolygon(new int[] {
				iXPos,
				iYPos,
				iXPos + BLOCK_FILLSIZE,
				iYPos,
				iXPos + (BLOCK_FILLSIZE / 2),
				iYPos + BLOCK_FILLSIZE
			});
		}
	}

	private void drawUploadIndicator(GC gcImg, int iXPos, int iYPos, boolean small) {
		if (!small) {
			gcImg.setBackground(blockColors[BLOCKCOLOR_TRANSFER]);
			gcImg.fillPolygon(new int[] {
				iXPos,
				iYPos + BLOCK_FILLSIZE,
				iXPos + BLOCK_FILLSIZE,
				iYPos + BLOCK_FILLSIZE,
				iXPos + (BLOCK_FILLSIZE / 2),
				iYPos
			});
		} else {
			// Small Up Arrow each upload request
			gcImg.setBackground(blockColors[BLOCKCOLOR_NEXT]);
			gcImg.fillPolygon(new int[] {
				iXPos + 1,
				iYPos + BLOCK_FILLSIZE - 2,
				iXPos + BLOCK_FILLSIZE - 2,
				iYPos + BLOCK_FILLSIZE - 2,
				iXPos + (BLOCK_FILLSIZE / 2),
				iYPos + 2
			});
		}

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.views.AbstractIView#getComposite()
	 */
	public Composite getComposite() {
		return pieceInfoComposite;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.views.AbstractIView#delete()
	 */
	public void delete() {
		if (imageLabel != null && !imageLabel.isDisposed()
				&& imageLabel.getImage() != null) {
			Image image = imageLabel.getImage();
			imageLabel.setImage(null);
			image.dispose();
		}

		if (img != null && !img.isDisposed()) {
			img.dispose();
			img = null;
		}

		if (font != null && !font.isDisposed()) {
			font.dispose();
			font = null;
		}
		
		if(dlm != null)
			dlm.removePieceListener(this);

		super.delete();
	}

	public Image obfusticatedImage(Image image, Point shellOffset) {
		UIDebugGenerator.obfusticateArea(image, topLabel, shellOffset, "");
		return image;
	}

	// @see org.gudy.azureus2.core3.download.DownloadManagerPeerListener#pieceAdded(org.gudy.azureus2.core3.peer.PEPiece)
	public void pieceAdded(PEPiece piece) {
		fillPieceInfoSection();
	}

	// @see org.gudy.azureus2.core3.download.DownloadManagerPeerListener#pieceRemoved(org.gudy.azureus2.core3.peer.PEPiece)
	public void pieceRemoved(PEPiece piece) {
		fillPieceInfoSection();
	}
}
