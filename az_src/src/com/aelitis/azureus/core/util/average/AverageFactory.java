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
 * Generates different types of averages.
 */
public abstract class AverageFactory {
   
   /**
    * Create a simple running average.
    */ 
   public static RunningAverage RunningAverage() {
      return new RunningAverage();
   }
   
   /**
    * Create a moving average, that moves over the given number of periods.
    */
   public static MovingAverage MovingAverage(int periods) {
      return new MovingAverage(periods);
   }
   
   /**
    * Create a moving average, that moves over the given number of periods and gives immediate
    * results (i.e. after the first update of X the average will be X
    */
   
   public static MovingImmediateAverage MovingImmediateAverage(int periods) {
	      return new MovingImmediateAverage(periods);
	   }
   /** 
    * Create an exponential moving average, smoothing over the given number
    * of periods, using a default smoothing weight value of 2/(1 + periods).
    */
   public static ExponentialMovingAverage ExponentialMovingAverage(int periods) {
      return new ExponentialMovingAverage(periods);
   }
   
   /**
    * Create an exponential moving average, with the given smoothing weight.
    * Larger weigths (closer to 1.0) will give more influence to
    * recent data and smaller weights (closer to 0.00) will provide
    * smoother averaging (give more influence to older data). 
    */
   public static ExponentialMovingAverage ExponentialMovingAverage(float weight) {
      return new ExponentialMovingAverage(weight);
   }
   
}
