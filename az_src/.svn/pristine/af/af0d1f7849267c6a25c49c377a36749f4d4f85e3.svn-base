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

import com.aelitis.azureus.core.dht.netcoords.vivaldi.ver1.*;
import com.aelitis.azureus.core.dht.netcoords.vivaldi.ver1.impl.*;


public class VivaldiTest {
  
  private static final int MAX_HEIGHT = 50;
  private static final int ELEMENTS_X = 20;
  private static final int ELEMENTS_Y = 20;
  private static final int DISTANCE   = 10;
  private static final int MAX_ITERATIONS = 1000;
  private static final int NB_CONTACTS = 7;
  
  public VivaldiTest() {
    VivaldiPosition positions[][] = new VivaldiPosition[ELEMENTS_X][ELEMENTS_Y];
    Coordinates realCoordinates[][] = new Coordinates[ELEMENTS_X][ELEMENTS_Y];
    //Init all
    for(int i = 0 ; i < ELEMENTS_X ; i++) {
      for(int j = 0 ; j < ELEMENTS_Y ; j++) {
        realCoordinates[i][j] = new HeightCoordinatesImpl(i*DISTANCE,j*DISTANCE,MAX_HEIGHT);
        positions[i][j] = new VivaldiPositionImpl(new HeightCoordinatesImpl(0,0,0));
      }
    }
    
    //Main loop
    for(int iter = 0 ; iter < MAX_ITERATIONS ; iter++) {
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
            position.update(rtt,position1.getCoordinates(),position1.getErrorEstimate());
          }
          if(iter == MAX_ITERATIONS -1) {
            System.out.println(iter + " (" + i + "," + j + ") : " + realCoordinates[i][j] + " , " + position);
            //System.out.println(position.getCoordinates());
          }          
        }
      }
    }
  }
  
  public static void main(String args[]) {
    new VivaldiTest();
  }
}
