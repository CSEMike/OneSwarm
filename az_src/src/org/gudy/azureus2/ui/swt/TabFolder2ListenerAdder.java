/*
 * File    : NewSWTCode.java
 * Created : 2004.04.03
 * By      : TuxPaper
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

package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;

/* CTabFolder2Adapter does not exist prior to SWT3, and may not exist after
 * SWT3.0 M8.  Caller should fallback to CTabFolderAdapter.
 * Note that for CTabFolder2Adapter Eclipse says:
 * "DO NOT USE - UNDER CONSTRUCTION" 
 * this suggests that CTabFolder2Adapter may replace CTabFolderAdapter once
 * it is complete
 */
public class TabFolder2ListenerAdder extends CTabFolder2Adapter {
  public static void add(CTabFolder folder) {
    folder.addCTabFolder2Listener(new TabFolder2ListenerAdder());
  }

  public void close(CTabFolderEvent event) {
    Tab.closed((CTabItem) event.item);
    event.doit = true;
  }
}
