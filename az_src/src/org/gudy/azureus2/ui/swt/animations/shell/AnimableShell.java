/*
 * File    : AnimableShell.java
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

import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.ui.swt.animations.Animator;

/**
 * @author Olivier Chalouhi
 *
 */
public interface AnimableShell {
  
  public Shell getShell();
  
  public void animationStarted(Animator source);
  public void reportPercent(int percent);
  public void animationEnded(Animator source);
    
}
