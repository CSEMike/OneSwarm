/*
 * Created on 22 juin 2005
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
package com.aelitis.azureus.core.dht.netcoords.vivaldi.ver1.impl.tests;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.ui.swt.views.stats.VivaldiPanel;
import com.aelitis.azureus.core.dht.netcoords.vivaldi.ver1.*;
import com.aelitis.azureus.core.dht.netcoords.vivaldi.ver1.impl.*;


public class VivaldiVisualTest {
  
  private static final int MAX_HEIGHT = 20;
  private static final int ELEMENTS_X = 20;
  private static final int ELEMENTS_Y = 20;
  private static final int DISTANCE   = 50;
  private static final int MAX_ITERATIONS = 10000;
  private static final int NB_CONTACTS = 5;
  
  public VivaldiVisualTest() {
    final Display display = new Display();
    Shell shell = new Shell(display);
    final VivaldiPanel panel = new VivaldiPanel(shell);
    shell.setLayout(new FillLayout());
    shell.setSize(800,800);
    shell.setText("Vivaldi Simulator");
    shell.open();
    
    Thread runner = new Thread("Viviladi Simulator") {
      public void run() {
        VivaldiPosition positions[][] = new VivaldiPosition[ELEMENTS_X][ELEMENTS_Y];
        final List<VivaldiPosition> lPos = new ArrayList<VivaldiPosition>(ELEMENTS_X*ELEMENTS_Y);        
        HeightCoordinatesImpl realCoordinates[][] = new HeightCoordinatesImpl[ELEMENTS_X][ELEMENTS_Y];
        //Init all
        for(int i = 0 ; i < ELEMENTS_X ; i++) {
          for(int j = 0 ; j < ELEMENTS_Y ; j++) {
            realCoordinates[i][j] = new HeightCoordinatesImpl(i*DISTANCE-ELEMENTS_X * DISTANCE/2,j*DISTANCE-ELEMENTS_Y*DISTANCE/2,MAX_HEIGHT);
            if(i >= ELEMENTS_X / 2 && 1 == 0) {
              positions[i][j] = new VivaldiPositionImpl(realCoordinates[i][j]);
              positions[i][j].setErrorEstimate(0.01f);
            } else {
              positions[i][j] = new VivaldiPositionImpl(new HeightCoordinatesImpl(1000+DISTANCE*i,1000+DISTANCE*j,20)); 
            }
            
            lPos.add(positions[i][j]);
          }
        }
        
        //Main loop
        for(int iter = 0 ; iter < MAX_ITERATIONS ; iter++) {
          if(iter%100 == 0) System.out.println(iter);
          if(display.isDisposed()) return;
          display.syncExec( new Runnable() {
            public void run() {
              panel.refresh(lPos);
            }
          });
          try {
            //Thread.sleep(100);
          } catch (Exception e) {
            // TODO: handle exception
          }
          
          //For each node :
          for(int i = 0 ; i < ELEMENTS_X ; i++) {
            for(int j = 0 ; j < ELEMENTS_Y ; j++) {
              VivaldiPosition position = positions[i][j];
              //Pick N random nodes
              for(int k = 0 ; k < NB_CONTACTS ; k++) {
                int i1 = (int) (Math.random() * ELEMENTS_X);
                int j1 = (int) (Math.random() * ELEMENTS_Y);
                if(i1 == i && j1 ==j) continue;
                VivaldiPosition position1 = positions[i1][j1];
                float rtt = realCoordinates[i1][j1].distance(realCoordinates[i][j]);
                //rtt *= (Math.random() - 0.5)/20 + 1;  
                position.update(rtt,position1.getCoordinates(),position1.getErrorEstimate());
              }
              
            }
          }
          
        }
      }
    };
    runner.setDaemon(true);
    runner.start();
    
    while (!shell.isDisposed ()) {
      if (!display.readAndDispatch ()) display.sleep ();
    }
    display.dispose ();
  }
  
  public static void main(String args[]) {
    new VivaldiVisualTest();
  }
}
