/*
 * File    : ScaledGraphic.java
 * Created : 15 d�c. 2003}
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
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
 */
package org.gudy.azureus2.ui.swt.components.graphics;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

/**
 * @author Olivier
 *
 */
public class ScaledGraphic extends BackGroundGraphic {

  protected Scale scale;
  protected ValueFormater formater;   
  
  protected Image bufferScale;
  private int lastMax;
  
  private int update_divider_width = 0;
  
  public ScaledGraphic(Scale scale,ValueFormater formater) {
    this.scale = scale;
    this.formater = formater;
  }
  
  public void setUpdateDividerWidth(int width) {
	  this.update_divider_width = width;
  }
  
  protected void drawScale(boolean sizeChanged) {
    if(drawCanvas == null || drawCanvas.isDisposed() || !drawCanvas.isVisible())
      return;
   
    drawBackGround(sizeChanged);
  	if (bufferBackground == null || bufferBackground.isDisposed()) {
  		return;
  	}
   
    boolean scaleChanged = lastMax != scale.getMax();
    
    if(sizeChanged || scaleChanged || bufferScale == null) {
      Rectangle bounds = drawCanvas.getClientArea();
      if(bounds.height < 30 || bounds.width  < 100)
        return;      
      
      if(bufferScale != null && ! bufferScale.isDisposed())
        bufferScale.dispose();
      
      bufferScale = new Image(drawCanvas.getDisplay(),bounds);
      
      GC gcBuffer = new GC(bufferScale);
      try {
      gcBuffer.drawImage(bufferBackground,0,0);
      gcBuffer.setForeground(Colors.black);
      //gcImage.setBackground(null);
      scale.setNbPixels(bounds.height - 16);
      int[] levels = scale.getScaleValues();
      for(int i = 0 ; i < levels.length ; i++) {
        int height = bounds.height - scale.getScaledValue(levels[i]) - 2;
        gcBuffer.drawLine(1,height,bounds.width - 70 ,height);
        gcBuffer.drawText(formater.format(levels[i]),bounds.width - 65,height - 12,true);
      }
      if (this.update_divider_width > 0) {
    	  for (int i=bounds.width - 70; i > 0; i-=this.update_divider_width) {
    		  gcBuffer.setForeground(Colors.grey);
    		  gcBuffer.drawLine(i, 0, i, bounds.height);
    	  }
      }
      } catch (Exception e) {
      	Debug.out(e);
      } finally {
      	gcBuffer.dispose();
      }
    }
  }   
  
  public void dispose() {
    super.dispose();
    if(bufferScale != null && ! bufferScale.isDisposed())
      bufferScale.dispose();
  }   
  
}
