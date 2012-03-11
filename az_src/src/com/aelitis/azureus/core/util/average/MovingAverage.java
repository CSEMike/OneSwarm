/*
 * Created on Oct 08, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
package com.aelitis.azureus.core.util.average;

/**
 * Implements a basic moving average.
 */
public class MovingAverage implements Average {
  
   private final int periods;
   private double data[];
   private int pos = 0;

   
   /**
    * Create a new moving average.
    */
   public MovingAverage(int periods) {
      this.periods = periods;
      this.data = new double[periods];
      reset();
   }
   
   	public void reset(){
   		pos = 0;
   		for (int i=0; i < periods; i++) { data[i] = 0.0; }
	}
   /**
    * Update average and return average-so-far.
    */
   public double update(final double newValue) {
      data[pos] = newValue;
      pos++;
      if (pos == periods) pos = 0;
      return calculateAve();
   }
   
   /**
    * Return average-so-far.
    */
   public double getAverage() { return calculateAve(); }
   
   
   private double calculateAve() {
      double sum = 0.0;
      for (int i=0; i < periods; i++) {
         sum += data[i];
      }
      return sum / periods;
   }

}
