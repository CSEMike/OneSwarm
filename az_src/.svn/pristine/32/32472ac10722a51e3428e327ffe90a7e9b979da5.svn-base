/*
 * File    : PopupShell.java
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
package org.gudy.azureus2.ui.swt.shells;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

/**
 * @author Olivier Chalouhi
 *
 */
public class PopupShell {
  
  protected Shell shell;
    
  public static final String IMG_INFORMATION = "information";
  
  /**
   * Constructs an ON_TOP popup 
   * @param display
   */
  public PopupShell(Display display) {
    this(display,SWT.ON_TOP);
  }
      
  public PopupShell(Display display,int type) { 
    
    if ( display.isDisposed()){          
      return;
    }
    
    shell = new Shell(display,type);            
    
    shell.setSize(250,150);
    Utils.setShellIcon(shell);
    
    FormLayout layout = new FormLayout();
    layout.marginHeight = 0;
    layout.marginWidth= 0;
    try {
      layout.spacing = 0;
    } catch (NoSuchFieldError e) {
      /* Ignore for Pre 3.0 SWT.. */
    } catch (Throwable e) {
    	Debug.printStackTrace( e );
    }
    
    shell.setLayout(layout);
  }
  
  protected void layout() {
    shell.layout();
  }
}
