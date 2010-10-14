/*
 * File    : TestWindow.java
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
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.animations.Animator;
import org.gudy.azureus2.ui.swt.shells.PopupShell;

/**
 * @author Olivier Chalouhi
 *
 */
public class TestWindow extends PopupShell implements AnimableShell {
  
  int nbAnimation = 0;
  int x0,y0,x1,y1;
  
  public TestWindow(Display display) {
    super(display);    
    
    layout();
    
    Rectangle bounds = display.getClientArea();    
    x0 = bounds.x + bounds.width - 250;
    x1 = bounds.x + bounds.width;

    y0 = bounds.y + bounds.height;
    y1 = bounds.y + bounds.height - 150;
    
    shell.setLocation(x0,y0);
    shell.open();
    new LinearAnimator(this,new Point(x0,y0),new Point(x0,y1),30,30).start();
  }

  
  public void animationEnded(Animator source) {
    if(nbAnimation == 0) {
      nbAnimation++;
      new LinearAnimator(this,new Point(x0,y1),new Point(x0,y1),1,3000).start();
      return;
    }
    if(nbAnimation == 1) {
      nbAnimation++;
      new LinearAnimator(this,new Point(x0,y1),new Point(x1,y1),50,30).start();
      return;
    }
    if(nbAnimation == 2) {
     shell.getDisplay().asyncExec(new AERunnable() {
      public void runSupport() {
        shell.dispose();
      }
    });
   }
  }

  public void animationStarted(Animator source) {
    
  }

  public Shell getShell() {
   return shell;
  }

  public void reportPercent(int percent) {    
  }
}
