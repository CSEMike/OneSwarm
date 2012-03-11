/*
 * Created on 2 juil. 2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

import java.text.DecimalFormat;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.TorrentUtil;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.BufferedLabel;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.debug.UIDebugGenerator;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.ui.common.ToolBarEnabler;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.selectedcontent.SelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;

/**
 * View of General information on the torrent
 * 
 * @author Olivier
 * 
 */
public class GeneralView
	implements ParameterListener, ObfusticateImage, UISWTViewCoreEventListener, UIPluginViewToolBarListener
{
	public static final String MSGID_PREFIX = "GeneralView";

	protected AEMonitor this_mon 	= new AEMonitor( MSGID_PREFIX );

  private Display display;
  private DownloadManager manager = null;
  boolean pieces[];
  int loopFactor;

  Composite genComposite;
  Composite gFile;
  Canvas piecesImage;
  Image pImage;
  BufferedLabel piecesPercent;
  Canvas availabilityImage;
  Image aImage;
  BufferedLabel availabilityPercent;
  Group gTransfer;
  BufferedLabel timeElapsed;
  BufferedLabel timeRemaining;
  BufferedLabel download;
  BufferedLabel downloadSpeed;
  //Text 			maxDLSpeed;
  BufferedLabel upload;
  BufferedLabel uploadSpeed;
  //Text 			maxULSpeed;
  //Text maxUploads;
  BufferedLabel totalSpeed;
  BufferedLabel ave_completion;
  BufferedLabel distributedCopies;
  BufferedLabel seeds;
  BufferedLabel peers;
  BufferedLabel completedLbl;
  Group gInfo;
  BufferedLabel fileName;
  BufferedLabel torrentStatus;
  BufferedLabel fileSize;
  BufferedLabel saveIn;
  BufferedLabel hash;
  
  BufferedLabel pieceNumber;
  BufferedLabel pieceSize;
  Control lblComment;
  BufferedLabel creation_date;
  BufferedLabel privateStatus;
  Control user_comment;
  BufferedLabel hashFails;
  BufferedLabel shareRatio;
  
  private int graphicsUpdate = COConfigurationManager.getIntParameter("Graphics Update");

  private Composite parent;
	private UISWTView swtView;

  /**
   * Initialize GeneralView
   */
  public GeneralView() {
  }

	public void dataSourceChanged(Object newDataSource) {
		if (newDataSource == null)
			manager = null;
		else if (newDataSource instanceof Object[])
			manager = (DownloadManager)((Object[])newDataSource)[0];
		else
			manager = (DownloadManager)newDataSource;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_refreshInfo();
			}
		});
	}

  public void initialize(Composite composite) {
  	parent = composite;

    genComposite = new Canvas(parent, SWT.NULL);


    GridLayout genLayout = new GridLayout();
    genLayout.marginHeight = 0;
    try {
    	genLayout.marginTop = 5;
    } catch (NoSuchFieldError e) {
    	// pre 3.1
    }
    genLayout.marginWidth = 2;
    genLayout.numColumns = 1;
    genComposite.setLayout(genLayout);

    Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				swt_refreshInfo();
			}
		});
    COConfigurationManager.addParameterListener("Graphics Update", this);
  }
  
  private void swt_refreshInfo() {
  	if (manager == null || parent == null)
  		return;
  	
  	Utils.disposeComposite(genComposite, false);
  	
    pieces = new boolean[manager.getNbPieces()];

    this.display = parent.getDisplay();

    gFile = new Composite(genComposite, SWT.SHADOW_OUT);
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    gFile.setLayoutData(gridData);
    GridLayout fileLayout = new GridLayout();
    fileLayout.marginHeight = 0;
    fileLayout.marginWidth = 10;
    fileLayout.numColumns = 3;
    gFile.setLayout(fileLayout);

    Label piecesInfo = new Label(gFile, SWT.LEFT);
    Messages.setLanguageText(piecesInfo, "GeneralView.section.downloaded");
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    piecesInfo.setLayoutData(gridData);

    piecesImage = new Canvas(gFile, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.widthHint = 150;
    gridData.heightHint = 25;
    piecesImage.setLayoutData(gridData);

    piecesPercent = new BufferedLabel(gFile, SWT.RIGHT);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
    gridData.widthHint = 50;
    piecesPercent.setLayoutData(gridData);

    Label availabilityInfo = new Label(gFile, SWT.LEFT);
    Messages.setLanguageText(availabilityInfo, "GeneralView.section.availability");
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    availabilityInfo.setLayoutData(gridData);

    availabilityImage = new Canvas(gFile, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.widthHint = 150;
    gridData.heightHint = 25;
    availabilityImage.setLayoutData(gridData);
    Messages.setLanguageText(availabilityImage, "GeneralView.label.status.pieces_available.tooltip");

    availabilityPercent = new BufferedLabel(gFile, SWT.RIGHT);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
    gridData.widthHint = 50;
    availabilityPercent.setLayoutData(gridData);
    Messages.setLanguageText(availabilityPercent.getWidget(), "GeneralView.label.status.pieces_available.tooltip");
    
    gTransfer = new Group(genComposite, SWT.SHADOW_OUT);
    Messages.setLanguageText(gTransfer, "GeneralView.section.transfer"); //$NON-NLS-1$
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gTransfer.setLayoutData(gridData);

    GridLayout layoutTransfer = new GridLayout();
    layoutTransfer.numColumns = 6;
    gTransfer.setLayout(layoutTransfer);

    Label label = new Label(gTransfer, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.timeelapsed"); //$NON-NLS-1$
    timeElapsed = new BufferedLabel(gTransfer, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    timeElapsed.setLayoutData(gridData);
    label = new Label(gTransfer, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.remaining"); //$NON-NLS-1$
    timeRemaining = new BufferedLabel(gTransfer, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    timeRemaining.setLayoutData(gridData);
    label = new Label(gTransfer, SWT.LEFT); //$NON-NLS-1$
    Messages.setLanguageText(label, "GeneralView.label.shareRatio");
    shareRatio = new BufferedLabel(gTransfer, SWT.LEFT); //$NON-NLS-1$
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    shareRatio.setLayoutData(gridData);

    label = new Label(gTransfer, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.downloaded"); //$NON-NLS-1$
    download = new BufferedLabel(gTransfer, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    download.setLayoutData(gridData);
    label = new Label(gTransfer, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.downloadspeed"); //$NON-NLS-1$
    downloadSpeed = new BufferedLabel(gTransfer, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    downloadSpeed.setLayoutData(gridData);
    label = new Label(gTransfer, SWT.LEFT); //$NON-NLS-1$
    Messages.setLanguageText(label, "GeneralView.label.hashfails");
    hashFails = new BufferedLabel(gTransfer, SWT.LEFT); //$NON-NLS-1$
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    hashFails.setLayoutData(gridData);
    
    label = new Label(gTransfer, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.uploaded"); //$NON-NLS-1$
    upload = new BufferedLabel(gTransfer, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    upload.setLayoutData(gridData);    
    label = new Label(gTransfer, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.uploadspeed"); //$NON-NLS-1$
    uploadSpeed = new BufferedLabel(gTransfer, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    uploadSpeed.setLayoutData(gridData);
    
    	// blah
    
    label = new Label(gTransfer, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.seeds");
    seeds = new BufferedLabel(gTransfer, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    seeds.setLayoutData(gridData);

    label = new Label(gTransfer, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.peers"); 
    peers = new BufferedLabel(gTransfer, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    peers.setLayoutData(gridData);
    
    label = new Label(gTransfer, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.completed"); 
    completedLbl = new BufferedLabel(gTransfer, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    completedLbl.setLayoutData(gridData);


    
    label = new Label(gTransfer, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.totalspeed"); 
    totalSpeed = new BufferedLabel(gTransfer, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    totalSpeed.setLayoutData(gridData);
    
    
    label = new Label(gTransfer, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.swarm_average_completion"); 
    ave_completion = new BufferedLabel(gTransfer, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    ave_completion.setLayoutData(gridData);
    
    label = new Label(gTransfer, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.distributedCopies"); 
    distributedCopies = new BufferedLabel(gTransfer, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    distributedCopies.setLayoutData(gridData);
    

    ////////////////////////

    gInfo = new Group(genComposite, SWT.SHADOW_OUT);
    Messages.setLanguageText(gInfo, "GeneralView.section.info"); 
    gridData = new GridData(GridData.FILL_BOTH);
    gInfo.setLayoutData(gridData);

    GridLayout layoutInfo = new GridLayout();
    layoutInfo.numColumns = 4;
    gInfo.setLayout(layoutInfo);

    label = new Label(gInfo, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.filename"); //$NON-NLS-1$
    fileName = new BufferedLabel(gInfo, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    fileName.setLayoutData(gridData);

    label = new Label(gInfo, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.status"); //$NON-NLS-1$
    torrentStatus = new BufferedLabel(gInfo, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    torrentStatus.setLayoutData(gridData);

    label = new Label(gInfo, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.savein"); //$NON-NLS-1$
    saveIn = new BufferedLabel(gInfo, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    saveIn.setLayoutData(gridData);

    label = new Label(gInfo, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.totalsize"); //$NON-NLS-1$
    fileSize = new BufferedLabel(gInfo, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    fileSize.setLayoutData(gridData);

    label = new Label(gInfo, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.numberofpieces"); //$NON-NLS-1$
    pieceNumber = new BufferedLabel(gInfo, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    pieceNumber.setLayoutData(gridData);

    label = new Label(gInfo, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.hash"); //$NON-NLS-1$
    hash = new BufferedLabel(gInfo, SWT.LEFT);
    Messages.setLanguageText(hash.getWidget(), "GeneralView.label.hash.tooltip", true);
    
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    hash.setLayoutData(gridData);
    	// click on hash -> copy to clipboard
    hash.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
    hash.setForeground(Colors.blue);
    label.addMouseListener(new MouseAdapter() {
    	public void mouseDoubleClick(MouseEvent arg0) {
    		String hash_str = hash.getText();
    		if(hash_str != null && hash_str.length() != 0)
    			new Clipboard(display).setContents(new Object[] {hash_str.replaceAll(" ","")}, new Transfer[] {TextTransfer.getInstance()});
    	}
    	public void mouseDown(MouseEvent arg0) {
    		String hash_str = hash.getText();
    		if(hash_str != null && hash_str.length() != 0)
    			new Clipboard(display).setContents(new Object[] {hash_str.replaceAll(" ","")}, new Transfer[] {TextTransfer.getInstance()});
    	}
    });
    hash.addMouseListener(new MouseAdapter() {
    	public void mouseDoubleClick(MouseEvent arg0) {
    		String hash_str = hash.getText();
    		if(hash_str != null && hash_str.length() != 0)
    			new Clipboard(display).setContents(new Object[] {hash_str.replaceAll(" ","")}, new Transfer[] {TextTransfer.getInstance()});
    	}
    	public void mouseDown(MouseEvent arg0) {
    		String hash_str = hash.getText();
    		if(hash_str != null && hash_str.length() != 0)
    			new Clipboard(display).setContents(new Object[] {hash_str.replaceAll(" ","")}, new Transfer[] {TextTransfer.getInstance()});
    	}
    });
    
    
    label = new Label(gInfo, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.size");
    pieceSize = new BufferedLabel(gInfo, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    pieceSize.setLayoutData(gridData);
    
    label = new Label(gInfo, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.creationdate");
    creation_date = new BufferedLabel(gInfo, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    creation_date.setLayoutData(gridData);
    
    label = new Label(gInfo, SWT.LEFT);
    Messages.setLanguageText(label, "GeneralView.label.private");
    privateStatus = new BufferedLabel(gInfo, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    privateStatus.setLayoutData(gridData);    

	// empty row
    label = new Label(gInfo, SWT.LEFT);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 4;
    label.setLayoutData(gridData);
    
    
    label = new Label(gInfo, SWT.LEFT);
    label.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
    label.setForeground(Colors.blue);
    Messages.setLanguageText(label, "GeneralView.label.user_comment");

    try {
    	user_comment = new Link(gInfo, SWT.LEFT | SWT.WRAP);
    	((Link)user_comment).addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					Utils.launch(e.text);
				}
			});
    } catch (Throwable e) {
    	user_comment = new Label(gInfo, SWT.LEFT | SWT.WRAP);
    }
    
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    user_comment.setLayoutData(gridData);
    
    label.addMouseListener(new MouseAdapter() {
    	private void editComment() {
    		TorrentUtil.promptUserForComment(new DownloadManager[] {manager});
    	}

        public void mouseDoubleClick(MouseEvent arg0) {editComment();}
        public void mouseDown(MouseEvent arg0) {editComment();}
      });

    label = new Label(gInfo, SWT.LEFT);
    gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "GeneralView.label.comment");
    
    try {
    	lblComment = new Link(gInfo, SWT.LEFT | SWT.WRAP);
    	((Link)lblComment).addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					Utils.launch(e.text);
				}
			});
    } catch (Throwable e) {
    	lblComment = new Label(gInfo, SWT.LEFT | SWT.WRAP);
    }
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 3;
    lblComment.setLayoutData(gridData);
 
    
    piecesImage.addListener(SWT.Paint, new Listener() {
      public void handleEvent(Event e) {
        if (e.count == 0 && e.width > 0 && e.height > 0) {
          updatePiecesInfo(true);
        }
      }
    });
    availabilityImage.addListener(SWT.Paint, new Listener() {
      public void handleEvent(Event e) {
        if (e.count == 0 && e.width > 0 && e.height > 0) {
          updateAvailability();
        }
      }
    });

    genComposite.layout();
    //Utils.changeBackgroundComposite(genComposite,MainWindow.getWindow().getBackground());
  }

  public Composite getComposite() {
    return genComposite;
  }

  public void refresh() {
    if(gFile == null || gFile.isDisposed() || manager == null)
      return;

    loopFactor++;
    if ((loopFactor % graphicsUpdate) == 0) {
      updateAvailability();
      updatePiecesInfo(false);      
    }
    
    
    
    DiskManager dm = manager.getDiskManager();
    
    String	remaining;
    String	eta			= DisplayFormatters.formatETA(manager.getStats().getETA());
    
    if ( dm != null ){
    	
    	long	rem = dm.getRemainingExcludingDND();
    	
    	String	data_rem = DisplayFormatters.formatByteCountToKiBEtc( rem );
    	
			// append data length unless we have an eta value and none left
    	 
    	if ( rem > 0 ){
    			
    		remaining = eta + (eta.length()==0?"":" ") + data_rem;
    		
    	}else{
    		
    			// no bytes left, don't show remaining bytes unless no eta
    		
    		if ( eta.length() == 0 ){
    			
    			remaining = data_rem;
    		}else{
    			
    			remaining = eta;
    		}
    	}
    }else{
    	
    		// only got eta value, just use that
    	
    	remaining = eta;
    }
    
    
    setTime(manager.getStats().getElapsedTime(), remaining );
            
    TRTrackerScraperResponse hd = manager.getTrackerScrapeResponse();
    String seeds_str = manager.getNbSeeds() +" "+ MessageText.getString("GeneralView.label.connected");
    String peers_str = manager.getNbPeers() +" "+ MessageText.getString("GeneralView.label.connected");
    String completed;
    if(hd != null && hd.isValid()) {
      seeds_str += " ( " + hd.getSeeds() +" "+ MessageText.getString("GeneralView.label.in_swarm") + " )";
      peers_str += " ( " + hd.getPeers() +" "+ MessageText.getString("GeneralView.label.in_swarm") + " )";
      completed = hd.getCompleted() > -1 ? Integer.toString(hd.getCompleted()) : "?";

    } else {
      completed = "?";
    }
    
    String _shareRatio = "";
    int sr = manager.getStats().getShareRatio();
    
    if(sr == -1) _shareRatio = Constants.INFINITY_STRING;
    if(sr >  0){ 
      String partial = "" + sr%1000;
      while(partial.length() < 3) partial = "0" + partial;
      _shareRatio = (sr/1000) + "." + partial;
    
    }
    
    DownloadManagerStats	stats = manager.getStats();
    
    String swarm_speed = DisplayFormatters.formatByteCountToKiBEtcPerSec( stats.getTotalAverage() ) + " ( " +DisplayFormatters.formatByteCountToKiBEtcPerSec( stats.getTotalAveragePerPeer())+ " " +MessageText.getString("GeneralView.label.averagespeed") + " )";    
    
    String swarm_completion = "";
    String distributedCopies = "0.000";
    String piecesDoneAndSum = ""+manager.getNbPieces();
    
    PEPeerManager pm = manager.getPeerManager();
    if( pm != null ) {
    	int comp = pm.getAverageCompletionInThousandNotation();
    	if( comp >= 0 ) {
    		swarm_completion = DisplayFormatters.formatPercentFromThousands( comp );
    	}
    	
    	piecesDoneAndSum = pm.getPiecePicker().getNbPiecesDone() + "/" + piecesDoneAndSum;
    	
    	distributedCopies = new DecimalFormat("0.000").format(pm.getPiecePicker().getMinAvailability()-pm.getNbSeeds()-(pm.isSeeding()&&stats.getDownloadCompleted(false)==1000?1:0));
    }
    
    

    setStats(
    		DisplayFormatters.formatDownloaded(stats),
    		DisplayFormatters.formatByteCountToKiBEtc(stats.getTotalDataBytesSent()),
    		DisplayFormatters.formatByteCountToKiBEtcPerSec(stats.getDataReceiveRate()),
    		DisplayFormatters.formatByteCountToKiBEtcPerSec(stats.getDataSendRate()),
    		swarm_speed,
    		""+manager.getStats().getDownloadRateLimitBytesPerSecond() /1024,
    		""+(manager.getStats().getUploadRateLimitBytesPerSecond() /1024),
      	seeds_str,
      	peers_str,
      	completed,
      	DisplayFormatters.formatHashFails(manager),
      	_shareRatio,
      	swarm_completion,
      	distributedCopies
    );
      
    TOTorrent	torrent = manager.getTorrent();
    
    String creation_date = DisplayFormatters.formatDate(manager.getTorrentCreationDate()*1000);
    byte[] created_by = torrent == null ? null : torrent.getCreatedBy();
    if (created_by != null) {
    	try {
    		creation_date = MessageText.getString("GeneralView.torrent_created_on_and_by", new String[] {
    			creation_date, new String(created_by, Constants.DEFAULT_ENCODING)
    		});
    	}
    	catch (java.io.UnsupportedEncodingException e) {/* forget it */}
    }
    
    setInfos(
      manager.getDisplayName(),
	  DisplayFormatters.formatByteCountToKiBEtc(manager.getSize()),
	  DisplayFormatters.formatDownloadStatus(manager),
      manager.getSaveLocation().toString(),
      TorrentUtils.nicePrintTorrentHash(torrent),
      piecesDoneAndSum,
      manager.getPieceLength(),
      manager.getTorrentComment(),
      creation_date,
      manager.getDownloadState().getUserComment(),
      MessageText.getString("GeneralView."+(torrent != null && torrent.getPrivate()?"yes":"no"))
      );
    
    
    //A special layout, for OS X and Linux, on which for some unknown reason
    //the initial layout fails.
    if (loopFactor == 2) {
      getComposite().layout(true);     
    }
  }

  public void delete() {
	if (aImage != null)
		aImage.dispose();
	aImage = null;
	if (pImage != null)
		pImage.dispose();
	pImage = null;
  Utils.disposeComposite(genComposite);    
    COConfigurationManager.removeParameterListener("Graphics Update", this);
  }

  private String getFullTitle() {
    return MessageText.getString(MSGID_PREFIX + ".title.full");
  }

  private void updateAvailability() {
  	if (manager == null)
  		return;

  	try{
  		this_mon.enter();
  	
  		final int[] available;
  		
  		PEPeerManager	pm = manager.getPeerManager();
  		
	    if (manager.getPeerManager() == null) {
	      if (availabilityPercent.getText() != "")
	      	
	        availabilityPercent.setText("");
	    
	      	available	= new int[manager.getNbPieces()];
	    }else{
	    	available	= pm.getAvailability();
	    }
	    
	    if (display == null || display.isDisposed())
	      return;
	
	    if (availabilityImage == null || availabilityImage.isDisposed()) {
	      return;
	    }
	    Rectangle bounds = availabilityImage.getClientArea();
	    
	    int xMax = bounds.width - 2;
	    
	    int yMax = bounds.height - 2;
	    
	    if (xMax < 10 || yMax < 5){
	        return;
	    }
	    
	    if (aImage != null && !aImage.isDisposed()){
	      aImage.dispose();
	    }
	    aImage = new Image(display, bounds.width, bounds.height);

	    GC gc = new GC(availabilityImage);
	    GC gcImage = new GC(aImage);
	    
	    try{
		    gcImage.setForeground(Colors.grey);
		    gcImage.drawRectangle(0, 0, bounds.width-1, bounds.height-1);
		    int allMin = 0;
		    int allMax = 0;
		    int total = 0;
		    String sTotal = "000";
		    if (available != null) {
		
		      allMin = available.length==0?0:available[0];
		      allMax = available.length==0?0:available[0];
		      int nbPieces = available.length;
		      for (int i = 0; i < nbPieces; i++) {
		        if (available[i] < allMin)
		          allMin = available[i];
		        if (available[i] > allMax)
		          allMax = available[i];
		      }
		      int maxAboveMin = allMax - allMin;
		      if (maxAboveMin == 0) {
		        // all the same.. easy paint
		        gcImage.setBackground(Colors.blues[allMin == 0 ? Colors.BLUES_LIGHTEST : Colors.BLUES_DARKEST]);
		        gcImage.fillRectangle(1, 1, xMax, yMax);
		      } else {
		        for (int i = 0; i < nbPieces; i++) {
		          if (available[i] > allMin)
		            total++;
		        }
		        total = (total * 1000) / nbPieces;
		        sTotal = "" + total;
		        if (total < 10) sTotal = "0" + sTotal;
		        if (total < 100) sTotal = "0" + sTotal;
		  
		        for (int i = 0; i < xMax; i++) {
		          int a0 = (i * nbPieces) / xMax;
		          int a1 = ((i + 1) * nbPieces) / xMax;
		          if (a1 == a0)
		            a1++;
		          if (a1 > nbPieces)
		            a1 = nbPieces;
		          int max = 0;
		          int min = available[a0];
		          int Pi = 1000;
		          for (int j = a0; j < a1; j++) {
		            if (available[j] > max)
		              max = available[j];
		            if (available[j] < min)
		              min = available[j];
		            Pi *= available[j];
		            Pi /= (available[j] + 1);
		          }
		          int pond = Pi;
		          if (max == 0)
		            pond = 0;
		          else {
		            int PiM = 1000;
		            for (int j = a0; j < a1; j++) {
		              PiM *= (max + 1);
		              PiM /= max;
		            }
		            pond *= PiM;
		            pond /= 1000;
		            pond *= (max - min);
		            pond /= 1000;
		            pond += min;
		          }
		          int index;
		          if (pond <= 0 || allMax == 0) {
		            index = 0;
		          } else {
		            // we will always have allMin, so subtract that
		            index = (pond - allMin) * (Colors.BLUES_DARKEST - 1) / maxAboveMin + 1;
		            // just in case?
		            if (index > Colors.BLUES_DARKEST) {
		              index = Colors.BLUES_DARKEST;
		            }
		          }
		            
		          gcImage.setBackground(Colors.blues[index]);
		          gcImage.fillRectangle(i+1, 1, 1, yMax);
		        }
		      }
		    }
		    if (availabilityPercent == null || availabilityPercent.isDisposed()) {
		      return;
		    }
		    availabilityPercent.setText(allMin + "." + sTotal);
		    gc.drawImage(aImage, bounds.x, bounds.y);
	    }finally{
	    	
		    gcImage.dispose();
		    gc.dispose();
	    }
  	}finally{
  		
  		this_mon.exit();
  	}
  }

  private void updatePiecesInfo(boolean bForce) {
  	if (manager == null)
  		return;

  	try{
  		this_mon.enter();
  
	    if (display == null || display.isDisposed())
	      return;
	
	    if (piecesImage == null || piecesImage.isDisposed())
	      return;
	    
	    DiskManager	dm = manager.getDiskManager();

	    boolean valid = !bForce;
	    
        boolean[] new_pieces = new boolean[manager.getNbPieces()];
	        	
	    if ( dm != null ){
	      		      	
	      	DiskManagerPiece[]	dm_pieces = dm.getPieces();
	      	
	 		for (int i=0;i<pieces.length;i++){
	      		 	
	 			new_pieces[i] = dm_pieces[i].isDone();
	       }
	    }

	    if ( pieces == null ){
	    	
	    	valid	= false;
	    	
	    }else{
	    	
		    for (int i = 0; i < pieces.length; i++) {
		    
		    	if (pieces[i] != new_pieces[i]){
		    		
		            valid = false;
		            
		            break;
		        }
		    }
	    }

	    pieces	= new_pieces;
	    
	    if (!valid) {
	      Rectangle bounds = piecesImage.getClientArea();
	      int xMax = bounds.width - 2;
	      int yMax = bounds.height - 2 - 6;
	      if (xMax < 10 || yMax < 5){
	        return;
	      }
	      
          int total = manager.getStats().getDownloadCompleted(true);
	      
	      if (pImage != null && !pImage.isDisposed()){
		        pImage.dispose();
	      }
	      
	      pImage = new Image(display, bounds.width, bounds.height);
	      
	      GC gcImage = new GC(pImage);
	      try{
		      gcImage.setForeground(Colors.grey);
		      gcImage.drawRectangle(0, 0, bounds.width-1, bounds.height-1);
		      gcImage.drawLine(1,6,xMax,6);
		
		      if (pieces != null && pieces.length != 0) {
		        int nbPieces = pieces.length;
		        
		        for (int i = 0; i < xMax; i++) {
		          int a0 = (i * nbPieces) / xMax;
		          int a1 = ((i + 1) * nbPieces) / xMax;
		          if (a1 == a0)
		            a1++;
		          if (a1 > nbPieces)
		            a1 = nbPieces;
		          int nbAvailable = 0;
		          for (int j = a0; j < a1; j++) {
		            if (pieces[j]) {
		              nbAvailable++;
		            }
		            int index = (nbAvailable * Colors.BLUES_DARKEST) / (a1 - a0);
		            gcImage.setBackground(Colors.blues[index]);
		            gcImage.fillRectangle(i+1,7,1,yMax);
		          }
		        }
		      }
		          
		      // draw file % bar above
		      int limit = (xMax * total) / 1000;
		      gcImage.setBackground(Colors.colorProgressBar);
		      gcImage.fillRectangle(1,1,limit,5);
		      if (limit < xMax) {
		        gcImage.setBackground(Colors.blues[Colors.BLUES_LIGHTEST]);
		        gcImage.fillRectangle(limit+1,1,xMax-limit,5);
		      }
	      }finally{
	
	    	  gcImage.dispose();
	      }
	
	      if (piecesPercent != null && !piecesPercent.isDisposed())
	        piecesPercent.setText(DisplayFormatters.formatPercentFromThousands(total));
	
	      if (pImage == null || pImage.isDisposed()) {
	        return;
	      }
	      
	      GC gc = new GC(piecesImage);
	      gc.drawImage(pImage, bounds.x, bounds.y);
	      gc.dispose();
	    }
  	}finally{
  		
  		this_mon.exit();
  	}
  }

  private void setTime(String elapsed, String remaining) {
    timeElapsed.setText( elapsed );
    timeRemaining.setText( remaining);
  }

  private void 
  setStats(
  	String dl, String ul, 
	String dls, String uls,
	String ts, 
	String dl_speed, String ul_speed,
	String s, 
	String p,
	String completed,
	String hash_fails,
	String share_ratio,
	String ave_comp,
	String distr_copies
	) 
  {
    if (display == null || display.isDisposed())
      return;
    
	download.setText( dl );
	downloadSpeed.setText( dls );
	upload.setText( ul );
	uploadSpeed.setText( uls );
	totalSpeed.setText( ts );
	ave_completion.setText( ave_comp );
	distributedCopies.setText(distr_copies);
	
	/*
	if ( !maxDLSpeed.getText().equals( dl_speed )){
		
		maxDLSpeed.setText( dl_speed );
	}
	
	if ( !maxULSpeed.getText().equals( ul_speed )){
		
		maxULSpeed.setText( ul_speed );
	}
	*/
	
	seeds.setText( s);
	peers.setText( p);
	completedLbl.setText(completed);
	hashFails.setText( hash_fails);
	shareRatio.setText( share_ratio);     
  }


  private void setInfos(
    final String _fileName,
    final String _fileSize,
    final String _torrentStatus,
    final String _path,
    final String _hash,
    final String _pieceData,
    final String _pieceLength,
    final String _comment,
	final String _creation_date,
	final String _user_comment,
	final String isPrivate) {
    if (display == null || display.isDisposed())
			return;
		Utils.execSWTThread(new AERunnable()
		{
			public void runSupport() {
				fileName.setText(_fileName);
				fileSize.setText(_fileSize);
				torrentStatus.setText(_torrentStatus);
				int pos = _torrentStatus.indexOf( "http://" );
				if ( pos > 0 ){
					torrentStatus.setLink( UrlUtils.getURL( _torrentStatus ));
				}else{
					torrentStatus.setLink( null );
				}
				saveIn.setText(_path);
				hash.setText(_hash);
				pieceNumber.setText(_pieceData); //$NON-NLS-1$
				pieceSize.setText(_pieceLength);
				creation_date.setText(_creation_date);
				privateStatus.setText(isPrivate);
				boolean do_relayout = false;
				do_relayout = setCommentAndFormatLinks(lblComment, _comment.length() > 5000 && Constants.isWindowsXP ? _comment.substring(0, 5000) : _comment ) | do_relayout;
				do_relayout = setCommentAndFormatLinks(user_comment, _user_comment) | do_relayout;
				if (do_relayout)
				{
					gInfo.layout();
				}
			}
		});
	}
 
  private static boolean setCommentAndFormatLinks(Control c, String new_comment) {
	  String old_comment = (String)c.getData("comment");
	  if (new_comment == null) {new_comment = "";}
	  if (new_comment.equals(old_comment)) {return false;}
	
	  c.setData("comment", new_comment);
	  if (c instanceof Label) {
		  ((Label) c).setText(new_comment);
	  } else if (c instanceof Link) {
						String sNewComment;
		  sNewComment = new_comment.replaceAll(
								"([^=\">][\\s]+|^)(http://[\\S]+)", "$1<A HREF=\"$2\">$2</A>");
						// need quotes around url
		  sNewComment = sNewComment.replaceAll("(href=)(htt[^\\s>]+)", "$1\"$2\"");

						// Examples:
						// http://cowbow.com/fsdjl&sdfkj=34.sk9391 moo
						// <A HREF=http://cowbow.com/fsdjl&sdfkj=34.sk9391>moo</a>
						// <A HREF="http://cowbow.com/fsdjl&sdfkj=34.sk9391">moo</a>
						// <A HREF="http://cowbow.com/fsdjl&sdfkj=34.sk9391">http://moo.com</a>
		  ((Link)c).setText(sNewComment);
			  	        	}
	  return true;
			  	    
			          }

  public void parameterChanged(String parameterName) {
    graphicsUpdate = COConfigurationManager.getIntParameter("Graphics Update");
  }

	public Image obfusticatedImage(Image image) {
		UIDebugGenerator.obfusticateArea(image, (Control) fileName.getWidget(),
				manager.toString());
		UIDebugGenerator.obfusticateArea(image, (Control) saveIn.getWidget(),
				Debug.secretFileName(saveIn.getText()));
		return image;
}

	public boolean eventOccurred(UISWTViewEvent event) {
    switch (event.getType()) {
      case UISWTViewEvent.TYPE_CREATE:
      	swtView = (UISWTView)event.getData();
      	swtView.setTitle(getFullTitle());
      	swtView.setToolBarListener(this);
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
      	String id = "DMDetails_General";
      	if (manager != null) {
      		if (manager.getTorrent() != null) {
  					id += "." + manager.getInternalName();
      		} else {
      			id += ":" + manager.getSize();
      		}
      	}
  
      	SelectedContentManager.changeCurrentlySelectedContent(id, new SelectedContent[] {
      		new SelectedContent(manager)
      	});
      	break;
        
      case UISWTViewEvent.TYPE_REFRESH:
        refresh();
        break;
    }

    return true;
  }

	public boolean toolBarItemActivated(ToolBarItem item, long activationType,
			Object datasource) {
		String itemKey = item.getID();

		if (itemKey.equals("run")) {
			ManagerUtils.run(manager);
			return true;
		}
		
		if (itemKey.equals("start")) {
			ManagerUtils.queue(manager, null);
			UIFunctionsManagerSWT.getUIFunctionsSWT().refreshIconBar();
			return true;
		}
		
		if (itemKey.equals("stop")) {
			ManagerUtils.stop(manager, null);
			UIFunctionsManagerSWT.getUIFunctionsSWT().refreshIconBar();
			return true;
		}
		
		if (itemKey.equals("remove")) {
			TorrentUtil.removeDownloads(new DownloadManager[] {
				manager
			}, null);
			return true;
		}
		
		return false;
	}

	public void refreshToolBarItems(Map<String, Long> list) {
		list.put("run", UIToolBarItem.STATE_ENABLED);
		list.put("start", ManagerUtils.isStartable(manager) ? UIToolBarItem.STATE_ENABLED : 0);
		list.put("startstop", UIToolBarItem.STATE_ENABLED);
		list.put("stop", ManagerUtils.isStopable(manager) ? UIToolBarItem.STATE_ENABLED : 0);
		list.put("remove", UIToolBarItem.STATE_ENABLED);
	}

}
