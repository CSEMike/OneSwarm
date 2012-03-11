/*
 * File    : PeerInfoView.java
 * Created : Oct 2, 2005
 * By      : TuxPaper
 *
 * Copyright (C) 2005, 2006 Aelitis SAS, All rights Reserved
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

package org.gudy.azureus2.ui.swt.views.file;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.Legend;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;



public class FileInfoView
	implements UISWTViewCoreEventListener
{
	private final static int BLOCK_FILLSIZE = 14;

	private final static int BLOCK_SPACING = 2;

	private final static int BLOCK_SIZE = BLOCK_FILLSIZE + BLOCK_SPACING;

	private final static int BLOCKCOLOR_DONE 	= 0;
	private final static int BLOCKCOLOR_SKIPPED = 1;
	private final static int BLOCKCOLOR_ACTIVE 	= 2;
	private final static int BLOCKCOLOR_NEEDED 	= 3;


	private Composite fileInfoComposite;

	private ScrolledComposite sc;

	protected Canvas fileInfoCanvas;

	private Color[] blockColors;

	private Label topLabel;

	// More delay for this view because of high workload
	private int graphicsUpdate = COConfigurationManager
			.getIntParameter("Graphics Update") * 2;

	private int loopFactor = 0;

	private DiskManagerFileInfo file;

	private Font font = null;

	Image img = null;

	// accessed only in SWT Thread
	private boolean refreshInfoCanvasQueued;

	private UISWTView swtView;

	/**
	 * Initialize
	 *
	 */
	public FileInfoView() 
	{
		blockColors = new Color[] { 
				Colors.blues[Colors.BLUES_DARKEST],
				Colors.white,
				Colors.red,
				Colors.green,
			};

	}

	private void dataSourceChanged(Object newDataSource) {
		if (newDataSource == null)
			file = null;
		else if (newDataSource instanceof Object[])
			file = (DiskManagerFileInfo) ((Object[]) newDataSource)[0];
		else if (newDataSource instanceof DiskManagerFileInfo)
			file = (DiskManagerFileInfo) newDataSource;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				fillFileInfoSection();
			}
		});
	}

  private String getFullTitle() {
    return MessageText.getString("FileView.BlockView.title");
  }
	
  private void initialize(Composite composite) {
		if (fileInfoComposite != null && !fileInfoComposite.isDisposed()) {
			Logger.log(new LogEvent(LogIDs.GUI, LogEvent.LT_ERROR,
					"FileInfoView already initialized! Stack: "
							+ Debug.getStackTrace(true, false)));
			delete();
		}
		createFileInfoPanel(composite);
	}

	private Composite createFileInfoPanel(Composite parent) {
		GridLayout layout;
		GridData gridData;

		// Peer Info section contains
		// - Peer's Block display
		// - Peer's Datarate
		fileInfoComposite = new Composite(parent, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		fileInfoComposite.setLayout(layout);
		gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
		fileInfoComposite.setLayoutData(gridData);

		new Label(fileInfoComposite, SWT.NULL).setLayoutData(new GridData());

		topLabel = new Label(fileInfoComposite, SWT.NULL);
		gridData = new GridData(SWT.FILL, SWT.DEFAULT, false, false);
		topLabel.setLayoutData(gridData);

		sc = new ScrolledComposite(fileInfoComposite, SWT.V_SCROLL);
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

		fileInfoCanvas = new Canvas(sc, SWT.NO_REDRAW_RESIZE | SWT.NO_BACKGROUND);
		gridData = new GridData(GridData.FILL, SWT.DEFAULT, true, false);
		fileInfoCanvas.setLayoutData(gridData);
		fileInfoCanvas.addPaintListener(new PaintListener() {
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

		fileInfoCanvas.addMouseTrackListener(
			new MouseTrackAdapter()
			{
				public void 
				mouseHover(
					MouseEvent event ) 
				{
					showPieceDetails( event.x, event.y );
				}
			});

		Listener doNothingListener = new Listener() {
			public void handleEvent(Event event) {
			}
		};
		fileInfoCanvas.addListener(SWT.KeyDown, doNothingListener);

		fileInfoCanvas.addListener(SWT.Resize, new Listener() {
			
			public void handleEvent(Event e) {
				
				if (refreshInfoCanvasQueued) {
					return;
				}
				
				refreshInfoCanvasQueued = true;

					// wrap in asyncexec because sc.setMinWidth (called later) doesn't work
					// too well inside a resize (the canvas won't size isn't always updated)

				Utils.execSWTThreadLater(0, new AERunnable() {
					public void runSupport() {
						if (img != null) {
							int iOldColCount = img.getBounds().width / BLOCK_SIZE;
							int iNewColCount = fileInfoCanvas.getClientArea().width / BLOCK_SIZE;
							if (iOldColCount != iNewColCount)
								refreshInfoCanvas();
						}
					}
				});
			}
		});

		sc.setContent(fileInfoCanvas);

		Legend.createLegendComposite(fileInfoComposite,
				blockColors, 
				new String[] { 
						"FileView.BlockView.Done",
						"FileView.BlockView.Skipped",
						"FileView.BlockView.Active",
						"FileView.BlockView.Outstanding",
					}, 
				new GridData(SWT.FILL,SWT.DEFAULT, true, false, 2, 1));

		int iFontPixelsHeight = 10;
		int iFontPointHeight = (iFontPixelsHeight * 72)
				/ fileInfoCanvas.getDisplay().getDPI().y;
		Font f = fileInfoCanvas.getFont();
		FontData[] fontData = f.getFontData();
		fontData[0].setHeight(iFontPointHeight);
		font = new Font(fileInfoCanvas.getDisplay(), fontData);

		return fileInfoComposite;
	}

	private void fillFileInfoSection() {
		if (topLabel == null) {
			return;
		}
		topLabel.setText( "" );
		
		refreshInfoCanvas();
	}

	private void refresh() {
		if (loopFactor++ % graphicsUpdate == 0) {
			refreshInfoCanvas();
		}
	}

	protected void
	showPieceDetails(
		int		x,
		int		y )
	{
		Rectangle bounds = fileInfoCanvas.getClientArea();
		
		if (bounds.width <= 0 || bounds.height <= 0){
			
			return;
		}
		
		if ( file == null ){
			
			return;
		}
		
		DownloadManager	download_manager = file.getDownloadManager();

		if ( download_manager == null ){
			
			return;
		}
		
		DiskManager		disk_manager = download_manager.getDiskManager();
		PEPeerManager	peer_manager = download_manager.getPeerManager();

		if (disk_manager == null || peer_manager == null ){
			
			return;
		}
		
		DiskManagerPiece[] 	dm_pieces = disk_manager.getPieces();
		PEPiece[]			pe_pieces = peer_manager.getPieces();
		
		int	first_piece = file.getFirstPieceNumber();
		int	num_pieces	= file.getNbPieces();
		
		int iNumCols = bounds.width / BLOCK_SIZE;
	
		int	x_block = x/BLOCK_SIZE;
		int	y_block = y/BLOCK_SIZE;
		
		int	piece_number = y_block*iNumCols + x_block + first_piece;
		
		if ( piece_number >= first_piece && piece_number < first_piece + num_pieces ){
			
			DiskManagerPiece	dm_piece = dm_pieces[piece_number];
			PEPiece				pe_piece = pe_pieces[piece_number];
			
			String	text =  "Piece " + piece_number + ": " + dm_piece.getString();
			
			if ( pe_piece != null ){
				
				text += ", active: " + pe_piece.getString();

			}else{
				
				if ( dm_piece.isNeeded() && !dm_piece.isDone()){
					
					text += ", inactive: " + peer_manager.getPiecePicker().getPieceString( piece_number );
				}
			}
			
			topLabel.setText( text );
			
		}else{
			
			topLabel.setText( "" );
		}
	}
	
	protected void refreshInfoCanvas() {
		refreshInfoCanvasQueued = false;
		Rectangle bounds = fileInfoCanvas.getClientArea();
		if (bounds.width <= 0 || bounds.height <= 0)
			return;

		if (img != null && !img.isDisposed()) {
			img.dispose();
			img = null;
		}

		DownloadManager	download_manager = file==null?null:file.getDownloadManager();
		
		DiskManager		disk_manager = download_manager==null?null:download_manager.getDiskManager();
		PEPeerManager	peer_manager = download_manager==null?null:download_manager.getPeerManager();
		
		if ( file == null || disk_manager == null || peer_manager == null ) {
			GC gc = new GC(fileInfoCanvas);
			gc.fillRectangle(bounds);
			gc.dispose();

			return;
		}

		int	first_piece = file.getFirstPieceNumber();
		int	num_pieces	= file.getNbPieces();
		
		int iNumCols = bounds.width / BLOCK_SIZE;
		int iNeededHeight = (((num_pieces - 1) / iNumCols) + 1)
				* BLOCK_SIZE;
		if (sc.getMinHeight() != iNeededHeight) {
			sc.setMinHeight(iNeededHeight);
			sc.layout(true, true);
			bounds = fileInfoCanvas.getClientArea();
		}

		img = new Image(fileInfoCanvas.getDisplay(), bounds.width, iNeededHeight);
		GC gcImg = new GC(img);

		try {
			gcImg.setBackground(fileInfoCanvas.getBackground());
			gcImg.fillRectangle(0, 0, bounds.width, bounds.height);


			DiskManagerPiece[] 	dm_pieces = disk_manager.getPieces();
			PEPiece[]			pe_pieces = peer_manager.getPieces();

			int iRow = 0;
			int iCol = 0;
			
			for (int i = first_piece; i < first_piece+num_pieces; i++) {
			
				DiskManagerPiece	dm_piece = dm_pieces[i];
				PEPiece				pe_piece = pe_pieces[i];
				
				int colorIndex;
			
				int iXPos = iCol * BLOCK_SIZE;
				int iYPos = iRow * BLOCK_SIZE;

				if ( dm_piece.isDone()){

					colorIndex = BLOCKCOLOR_DONE;

				}else if ( !dm_piece.isNeeded()){
					
					colorIndex = BLOCKCOLOR_SKIPPED;

				}else if ( pe_piece != null ){
					
					colorIndex	= BLOCKCOLOR_ACTIVE;
					
				}else{
					
					colorIndex	= BLOCKCOLOR_NEEDED;
				}
				
				gcImg.setBackground(blockColors[colorIndex]);
				gcImg.fillRectangle(iXPos, iYPos, BLOCK_FILLSIZE, BLOCK_FILLSIZE);


				iCol++;
				if (iCol >= iNumCols) {
					iCol = 0;
					iRow++;
				}
			}
			
		} catch (Exception e) {
			Logger.log(new LogEvent(LogIDs.GUI, "drawing piece map", e));
		} finally {
			gcImg.dispose();
		}

		fileInfoCanvas.redraw();
	}

	private Composite getComposite() {
		return fileInfoComposite;
	}

	private void delete() {
		if (img != null && !img.isDisposed()) {
			img.dispose();
			img = null;
		}

		if (font != null && !font.isDisposed()) {
			font.dispose();
			font = null;
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
      	refreshInfoCanvas();
      	break;
        
      case UISWTViewEvent.TYPE_REFRESH:
        refresh();
        break;
        
    }

    return true;
  }
}
