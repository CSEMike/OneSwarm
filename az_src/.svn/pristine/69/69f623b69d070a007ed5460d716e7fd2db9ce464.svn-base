/*
 * File    : IpFilterEditor.java
 * Created : 8 oct. 2003 13:18:42
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

package org.gudy.azureus2.ui.swt.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.ipfilter.IpRange;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

import com.aelitis.azureus.core.AzureusCore;

/**
 * @author Olivier
 * 
 */
public class IpFilterEditor {

  AzureusCore	azureus_core;
  
  IpRange range;

  boolean newRange;

  public 
  IpFilterEditor(
  		AzureusCore		_azureus_core,
  		Shell parent,
		final IpRange _range) 
  {
  	azureus_core	= _azureus_core;
    this.range = _range;
    if (range == null) {
      newRange = true;
      range = azureus_core.getIpFilterManager().getIPFilter().createRange(false);
    }

    final Shell shell = ShellFactory.createShell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    Messages.setLanguageText(shell,"ConfigView.section.ipfilter.editFilter");
    Utils.setShellIcon(shell);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    shell.setLayout(layout);

    Label label = new Label(shell, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.ipfilter.description");

    final Text textDescription = new Text(shell, SWT.BORDER);
    GridData gridData = new GridData();
    gridData.widthHint = 300;
    textDescription.setLayoutData(gridData);

    label = new Label(shell, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.ipfilter.start");

    final Text textStartIp = new Text(shell, SWT.BORDER);
    gridData = new GridData();
    gridData.widthHint = 120;
    textStartIp.setLayoutData(gridData);

    label = new Label(shell, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.ipfilter.end");

    final Text textEndIp = new Text(shell, SWT.BORDER);
    gridData = new GridData();
    gridData.widthHint = 120;
    textEndIp.setLayoutData(gridData);

    final Button ok = new Button(shell, SWT.PUSH);
    Messages.setLanguageText(ok, "Button.ok");
    shell.setDefaultButton(ok);

    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    gridData.widthHint = 100;
    ok.setLayoutData(gridData);
    ok.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        range.setDescription( textDescription.getText());
        range.setStartIp( textStartIp.getText());
        range.setEndIp( textEndIp.getText());
        range.checkValid();
        if (newRange) {
          azureus_core.getIpFilterManager().getIPFilter().addRange(range);
        }
         shell.dispose();
      }
    });   
    
    textStartIp.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent event) {
        range.setStartIp( textStartIp.getText());
        range.checkValid();
        if(range.isValid())
          ok.setEnabled(true);
        else
          ok.setEnabled(false);
      }
    });
    
    textEndIp.addModifyListener(new ModifyListener() {
          public void modifyText(ModifyEvent event) {
            range.setEndIp( textEndIp.getText());
            range.checkValid();
            if(range.isValid())
              ok.setEnabled(true);
            else
              ok.setEnabled(false);
          }
     });
     
    if (range != null) {
          textDescription.setText(range.getDescription());
          textStartIp.setText(range.getStartIp());
          textEndIp.setText(range.getEndIp());
    }

    shell.pack();
    Utils.centerWindowRelativeTo(shell, parent);
    shell.open();
  }

}
