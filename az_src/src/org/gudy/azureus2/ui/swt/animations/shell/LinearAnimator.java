/*
 * File    : LinearAnimator.java
 * Created : 14 mars 2004
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
package org.gudy.azureus2.ui.swt.animations.shell;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.animations.Animator;

/**
 * @author Olivier Chalouhi
 *
 */
public class LinearAnimator extends Animator{
  
  private AnimableShell shell;
  private Display display;
  
  private int startX;
  private int startY;
  
  private int endX;
  private int endY;
  
  private int nbSteps;
  private int period;
  
  private boolean interrupted;
  
  public LinearAnimator(AnimableShell shell,Point start,Point end,int nbSteps,int period) {
    super("Linear Shell Animator");
    if(period < 20) period = 20;
    if(nbSteps <= 0) nbSteps = 1;
    this.shell = shell;
    this.display = shell.getShell().getDisplay();
    
    this.startX = start.x;
    this.startY = start.y;
    
    this.endX = end.x;
    this.endY = end.y;
    
    this.nbSteps = nbSteps;
    this.period = period;    
    
    this.interrupted = false;
  }
  
  public void runSupport() {
  	try {
	    shell.animationStarted(this);
	    int step = 0;
	    while(step <= nbSteps && !interrupted) {
	      setShellAtStep(step);
	      shell.reportPercent(100 * step  /nbSteps);
	      step++;
	      try {
	        Thread.sleep(period);
	      } catch(Exception e) {
	        //Stop animating
	        step = nbSteps;
	      }
	    }
  	} finally {
  		shell.animationEnded(this);
  	}
  }
  
  public void interrupt() {
    this.interrupted = true;
  }
  
  private void setShellAtStep(int step) {
    if(display == null || display.isDisposed())
      return;
    final int x = startX + ((endX - startX) * step ) / nbSteps;
    final int y = startY + ((endY - startY) * step ) / nbSteps;
    display.asyncExec(new AERunnable() {
      public void runSupport() {
        if(shell == null || shell.getShell() == null || shell.getShell().isDisposed())
          return;
        shell.getShell().setLocation(x,y);
      }    
    });
  }
}
