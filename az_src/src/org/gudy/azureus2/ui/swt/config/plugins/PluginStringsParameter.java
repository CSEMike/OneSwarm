/*
 * File    : PluginStringParameter.java
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
package org.gudy.azureus2.ui.swt.config.plugins;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.pluginsimpl.local.ui.config.StringListParameterImpl;
import org.gudy.azureus2.ui.swt.Messages;

/**
 * @author Olivier
 *
 */
public class PluginStringsParameter implements PluginParameterImpl {
  
  Control[] controls;
  
  public PluginStringsParameter(Composite pluginGroup,StringListParameterImpl parameter) {
    controls = new Control[2];
           
    controls[0] = new Label(pluginGroup,SWT.NULL);
    Messages.setLanguageText(controls[0],parameter.getLabelKey());
    
    org.gudy.azureus2.ui.swt.config.StringListParameter slp =
    	new org.gudy.azureus2.ui.swt.config.StringListParameter(
    	    pluginGroup,
    	    parameter.getKey(),
					parameter.getDefaultValue(),
					parameter.getLabels(),
					parameter.getValues());
    controls[1] = slp.getControl();
    GridData gridData = new GridData();
    gridData.widthHint = 100;
    controls[1].setLayoutData(gridData);
    new Label(pluginGroup,SWT.NULL);
  }
  
  public Control[] getControls(){
    return controls;
  }

}
