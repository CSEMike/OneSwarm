/*
 * File    : IWizardPanel.java
 * Created : 30 sept. 2003 00:20:26
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
 
package org.gudy.azureus2.ui.swt.wizard;

/**
 * @author Olivier
 * 
 */
public interface IWizardPanel {
  
  public void show();
  
  public IWizardPanel getNextPanel();
  public IWizardPanel getPreviousPanel();
  public IWizardPanel getFinishPanel();
  
  public boolean isPreviousEnabled();
  public boolean isNextEnabled();
  public boolean isFinishEnabled();
  
  /**
   * This method is called when the "finish" button is pressed. It allows operations to be
   * carried out before the "finish" panel is shown. 
   * @return true  - carry on and show the finish panel; false - operation failed,
   * leave on current panel
   */
  public boolean isFinishSelectionOK();
  
  public void cancelled();
  
  public void finish();
}
