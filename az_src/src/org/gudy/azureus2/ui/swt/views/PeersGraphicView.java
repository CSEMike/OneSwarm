/*
 * Created on 19 nov. 2004
 * Created by Olivier Chalouhi
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
package org.gudy.azureus2.ui.swt.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.ui.swt.components.graphics.PieUtils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

/**
 * @author Olivier Chalouhi
 *
 */
public class PeersGraphicView extends AbstractIView implements DownloadManagerPeerListener {

  
  private DownloadManager manager = null;
  
  private static final int NB_ANGLES = 1000;  
  private double[] angles;
  private double[] deltaPerimeters;
  private double perimeter;
  private double[] rs;
  private double[] deltaXXs;
  private double[] deltaXYs;
  private double[] deltaYXs;
  private double[] deltaYYs;
  
  private Point oldSize;
  
  private List peers;
  private AEMonitor peers_mon = new AEMonitor( "PeersGraphicView:peers" );;
  private PeerComparator peerComparator;
  
  
  //UI Stuff
  private Display display;
  private Composite panel;
  private static final int PEER_SIZE = 15;
  private static final int PACKET_SIZE = 10;
  private static final int OWN_SIZE = 75;
  
  private boolean antiAliasingAvailable = true;
  
  //Comparator Class
  //Note: this comparator imposes orderings that are inconsistent with equals.
  class PeerComparator implements Comparator {
    public int compare(Object arg0, Object arg1) {
      PEPeer peer0 = (PEPeer) arg0;
      PEPeer peer1 = (PEPeer) arg1;

      //Then we sort on %, but depending on interested ...
      int percent0 = peer0.getPercentDoneInThousandNotation();
      int percent1 = peer1.getPercentDoneInThousandNotation();
      
      return percent0 - percent1;
    }
  }
  
  
  public PeersGraphicView() {
    angles = new double[NB_ANGLES];
    deltaPerimeters = new double[NB_ANGLES];
    rs = new double[NB_ANGLES];
    deltaXXs = new double[NB_ANGLES];
    deltaXYs = new double[NB_ANGLES];
    deltaYXs = new double[NB_ANGLES];
    deltaYYs = new double[NB_ANGLES];
    
    for(int i = 0 ; i < NB_ANGLES ; i++) {
      angles[i] = 2 * i * Math.PI / NB_ANGLES - Math.PI;
      deltaXXs[i] = Math.cos(angles[i]);
      deltaXYs[i] = Math.sin(angles[i]);
      deltaYXs[i] = Math.cos(angles[i]+Math.PI / 2);
      deltaYYs[i] = Math.sin(angles[i]+Math.PI / 2);
    }
    
    this.peers = new ArrayList();
    this.peerComparator = new PeerComparator();
  } 
  
	public void dataSourceChanged(Object newDataSource) {
  	if (manager != null)
  		manager.removePeerListener(this);

		if (newDataSource == null)
			manager = null;
		else if (newDataSource instanceof Object[])
			manager = (DownloadManager)((Object[])newDataSource)[0];
		else
			manager = (DownloadManager)newDataSource;

    if (manager != null)
    	manager.addPeerListener(this);
	}

  public void delete() {
  	if (manager != null)
  		manager.removePeerListener(this);
    super.delete();
  }

  public Composite getComposite() {    
    return panel;
  }
  
  public String getData() {
    return "PeersGraphicView.title";
  }

  public void initialize(Composite composite) {
    display = composite.getDisplay();
    panel = new Canvas(composite,SWT.NULL);
  }

  public void refresh() {
    doRefresh();
  }
  
  private void doRefresh() {
    //Comment the following line to enable the view
    //if(true) return;
    
    PEPeer[] sortedPeers;
    try {      
      peers_mon.enter();      
      List connectedPeers = new ArrayList();
      Iterator iter = peers.iterator();
      while(iter.hasNext()) {
        PEPeerTransport peer = (PEPeerTransport) iter.next();
        if(peer.getConnectionState() == PEPeerTransport.CONNECTION_FULLY_ESTABLISHED)
          connectedPeers.add(peer);
      }
      
      sortedPeers = (PEPeer[]) connectedPeers.toArray(new PEPeer[connectedPeers.size()]);      
    } finally {
      peers_mon.exit();
    }
    
    if(sortedPeers == null) return;
    Arrays.sort(sortedPeers,peerComparator);
    
    render(sortedPeers);
  }
  
  private void render(PEPeer[] sortedPeers) {
    if(panel == null || panel.isDisposed() || manager == null)
      return;
    Point panelSize = panel.getSize();
    int x0 = panelSize.x / 2;
    int y0 = panelSize.y / 2;  
    int a = x0 - 20;
    int b = y0 - 20;
    if(a < 10 || b < 10) return;
    
    if(oldSize == null || !oldSize.equals(panelSize)) {     
      oldSize = panelSize;      
      perimeter = 0;
      for(int i = 0 ; i < NB_ANGLES ; i++) {
        rs[i] = Math.sqrt(1/(deltaYXs[i] * deltaYXs[i] / (a*a) + deltaYYs[i] * deltaYYs[i] / (b * b)));
        perimeter += rs[i];
      }
    }
    Image buffer = new Image(display,panelSize.x,panelSize.y);
    GC gcBuffer = new GC(buffer);    
    gcBuffer.setBackground(Colors.white);   
    gcBuffer.setForeground(Colors.blue);
    gcBuffer.fillRectangle(0,0,panelSize.x,panelSize.y);

    if(SWT.getVersion() >= 3138 && antiAliasingAvailable) {
      try {
        //gcBuffer.setTextAntialias(SWT.ON);
        //gcBuffer.setAntialias(SWT.ON);
      } catch(Exception e) {
        antiAliasingAvailable = false;
      }
    }
    
    gcBuffer.setBackground(Colors.blues[Colors.BLUES_MIDLIGHT]);      
    
    int nbPeers = sortedPeers.length;
    
    int iAngle = 0;
    double currentPerimeter = 0;    
    double angle,r;   

    for(int i = 0 ; i < nbPeers ; i++) {
      PEPeer peer = sortedPeers[i];
      do {
        angle = angles[iAngle];
        r     = rs[iAngle];
        currentPerimeter += r;
        if(iAngle + 1 < NB_ANGLES) iAngle++;
      } while( currentPerimeter < i * perimeter / nbPeers);
            
      angle = (4 * i - nbPeers) * Math.PI  / (2 * nbPeers) - Math.PI / 2;
      
      int[] rectangle = new int[8];
      
      
      
      if(! peer.isChokedByMe() || ! peer.isChokingMe()) {
        gcBuffer.setForeground(Colors.blues[Colors.BLUES_MIDLIGHT]);
        int x1 = x0 + (int) ( r * deltaYXs[iAngle] );
        int y1 = y0 + (int) ( r * deltaYYs[iAngle] );        
        gcBuffer.drawLine(x0,y0,x1,y1);
        /*
        rectangle[0] = x0 + (int) (deltaXXs[iAngle] * 3 + 0.5);
        rectangle[1] = y0 + (int) (deltaXYs[iAngle] * 3 + 0.5);
        rectangle[2] = x0 - (int) (deltaXXs[iAngle] * 3 + 0.5);
        rectangle[3] = y0 - (int) (deltaXYs[iAngle] * 3 + 0.5);
        
        
        rectangle[4] = x0 - (int) (deltaXXs[iAngle] * 3 - r * deltaYXs[iAngle]+ 0.5);
        rectangle[5] = y0 - (int) (deltaXYs[iAngle] * 3 - r * deltaYYs[iAngle] + 0.5);
        rectangle[6] = x0 + (int) (deltaXXs[iAngle] * 3 + r * deltaYXs[iAngle] + 0.5);
        rectangle[7] = y0 + (int) (deltaXYs[iAngle] * 3 + r * deltaYYs[iAngle] + 0.5);
        gcBuffer.drawPolygon(rectangle);        
        */
      }    
      
      
      int percentSent = peer.getPercentDoneOfCurrentIncomingRequest();
      if(percentSent >= 0) {
        gcBuffer.setBackground(Colors.blues[Colors.BLUES_MIDDARK]);
        double r1 = r - r * percentSent / 100;
        rectangle[0] = (int) (x0 + r1 * deltaYXs[iAngle] + 0.5);
        rectangle[1] = (int) (y0 + r1 * deltaYYs[iAngle] + 0.5);
        rectangle[2] = (int) (x0 + deltaXXs[iAngle] * 4 + r1 * deltaYXs[iAngle] + 0.5);
        rectangle[3] = (int) (y0 + deltaXYs[iAngle] * 4 + r1 * deltaYYs[iAngle] + 0.5);
        
        
        rectangle[4] =  (int) (x0 + deltaXXs[iAngle] * 4 + (r1-10) * deltaYXs[iAngle] + 0.5);
        rectangle[5] =  (int) (y0 + deltaXYs[iAngle] * 4 + (r1-10) * deltaYYs[iAngle] + 0.5);
        rectangle[6] =  (int) (x0 + (r1-10) * deltaYXs[iAngle] + 0.5);
        rectangle[7] =  (int) (y0 + (r1-10) * deltaYYs[iAngle] + 0.5);
        gcBuffer.fillPolygon(rectangle); 
      }
      
      
      
      percentSent = peer.getPercentDoneOfCurrentOutgoingRequest();
      if(percentSent >= 0) {
        gcBuffer.setBackground(Colors.blues[Colors.BLUES_MIDLIGHT]);
        double r1 = r * percentSent / 100;
        rectangle[0] =  (int) (x0 + r1 * deltaYXs[iAngle] + 0.5);
        rectangle[1] =  (int) (y0 + r1 * deltaYYs[iAngle] + 0.5);
        rectangle[2] =  (int) (x0 - deltaXXs[iAngle] * 4 + r1 * deltaYXs[iAngle] + 0.5);
        rectangle[3] =  (int) (y0 - deltaXYs[iAngle] * 4 + r1 * deltaYYs[iAngle] + 0.5);
        
        
        rectangle[4] =  (int) (x0 - deltaXXs[iAngle] * 4 + (r1-10) * deltaYXs[iAngle] + 0.5);
        rectangle[5] =  (int) (y0 - deltaXYs[iAngle] * 4 + (r1-10) * deltaYYs[iAngle] + 0.5);
        rectangle[6] =  (int) (x0 + (r1-10) * deltaYXs[iAngle] + 0.5);
        rectangle[7] =  (int) (y0 + (r1-10) * deltaYYs[iAngle] + 0.5);
        gcBuffer.fillPolygon(rectangle); 
      }
      
      
      
      int x1 = x0 + (int) (r * deltaYXs[iAngle]);
      int y1 = y0 + (int) (r * deltaYYs[iAngle]);
      gcBuffer.setBackground(Colors.blues[Colors.BLUES_MIDDARK]);
      if(peer.isSnubbed()) {
        gcBuffer.setBackground(Colors.grey);
      }
      
      /*int PS = (int) (PEER_SIZE);      
        if (deltaXY == 0) {
          PS = (int) (PEER_SIZE * 2);
        } else {
          if (deltaYY > 0) {
            PS = (int) (PEER_SIZE / deltaXY);
          }
        }*/
      //PieUtils.drawPie(gcBuffer,(x1 - PS / 2),y1 - PS / 2,PS,PS,peer.getPercentDoneInThousandNotation() / 10);
      PieUtils.drawPie(gcBuffer,x1 - PEER_SIZE / 2,y1 - PEER_SIZE / 2,PEER_SIZE,PEER_SIZE,peer.getPercentDoneInThousandNotation() / 10);
      
      //gcBuffer.drawText(peer.getIp() , x1 + 8 , y1 , true);
    }
    
    gcBuffer.setBackground(Colors.blues[Colors.BLUES_MIDDARK]);
    PieUtils.drawPie(gcBuffer,x0 - OWN_SIZE / 2 ,y0 - OWN_SIZE / 2,OWN_SIZE,OWN_SIZE,manager.getStats().getCompleted() / 10);
    
    gcBuffer.dispose();
    GC gcPanel = new GC(panel);
    gcPanel.drawImage(buffer,0,0);
    gcPanel.dispose();
    buffer.dispose();   
  }
  
  public void peerManagerWillBeAdded( PEPeerManager	peer_manager ){}
  public void peerManagerAdded(PEPeerManager manager) {}
  public void peerManagerRemoved(PEPeerManager manager) {}
  
  public void peerAdded(PEPeer peer) {
    try {
      peers_mon.enter();
      peers.add(peer);
    } finally {
      peers_mon.exit();
    }
  }
  
  public void peerRemoved(PEPeer peer) {
    try {
      peers_mon.enter();
      peers.remove(peer);
    } finally {
      peers_mon.exit();
    }
  }
}
