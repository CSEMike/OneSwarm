/*
 * File    : Main.java
 * Created : 2 avr. 2004
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
package org.gudy.azureus2.ui.systray;

  import org.eclipse.swt.*;
  import org.eclipse.swt.graphics.*;
  import org.eclipse.swt.widgets.*;

  

  public class Main {

  public static void main(String[] args) {
    Display display = new Display ();
    Shell shell = new Shell (display);
    Image image = new Image (display, 16, 16);
    final Tray tray = display.getSystemTray ();
    final TrayItem item = new TrayItem (tray, SWT.NONE);
    item.setToolTipText("SWT TrayItem");
    item.addListener (SWT.Show, new Listener () {
      public void handleEvent (Event event) {
        System.out.println("show");
      }
    });
    item.addListener (SWT.Hide, new Listener () {
      public void handleEvent (Event event) {
        System.out.println("hide");
      }
    });
    item.addListener (SWT.Selection, new Listener () {
      public void handleEvent (Event event) {
        System.out.println("selection");
      }
    });
    item.addListener (SWT.DefaultSelection, new Listener () {
      public void handleEvent (Event event) {
        System.out.println("default selection");
      }
    });
    final Menu menu = new Menu (shell, SWT.POP_UP);
    for (int i = 0; i < 8; i++) {
      MenuItem mi = new MenuItem (menu, SWT.PUSH);
      mi.setText ("Item" + i);
    }
    item.addListener (SWT.MenuDetect, new Listener () {
      public void handleEvent (Event event) {
        menu.setVisible (true);
      }
    });
    item.setImage (image);
    shell.setBounds(50, 50, 300, 200);
    shell.open ();
    while (!shell.isDisposed ()) {
      if (!display.readAndDispatch ()) display.sleep ();
    }
    image.dispose ();
    display.dispose ();
  }
}
