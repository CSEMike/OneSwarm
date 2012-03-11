/*
 * File    : PluginParameter.java
 * Created : 15 déc. 2003}
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

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.pluginsimpl.local.ui.config.BooleanParameterImpl;
import org.gudy.azureus2.pluginsimpl.local.ui.config.ColorParameter;
import org.gudy.azureus2.pluginsimpl.local.ui.config.DirectoryParameterImpl;
import org.gudy.azureus2.pluginsimpl.local.ui.config.FileParameter;
import org.gudy.azureus2.pluginsimpl.local.ui.config.IntParameterImpl;
import org.gudy.azureus2.pluginsimpl.local.ui.config.IntsParameter;
import org.gudy.azureus2.pluginsimpl.local.ui.config.StringParameterImpl;
import org.gudy.azureus2.pluginsimpl.local.ui.config.StringListParameterImpl;
import org.gudy.azureus2.ui.swt.config.IAdditionalActionPerformer;

/**
 * @author Olivier
 *
 */
public class PluginParameter {

  public PluginParameterImpl implementation;
  
  public PluginParameter(Composite pluginGroup,Parameter parameter) {
    if(parameter instanceof StringParameterImpl) {
      implementation = new PluginStringParameter(pluginGroup,(StringParameterImpl)parameter);
    } else if(parameter instanceof IntParameterImpl) {
      implementation = new PluginIntParameter(pluginGroup,(IntParameterImpl)parameter);
    } else if(parameter instanceof BooleanParameterImpl) {
      implementation = new PluginBooleanParameter(pluginGroup,(BooleanParameterImpl)parameter);
    } else if(parameter instanceof FileParameter) {
      implementation = new PluginFileParameter(pluginGroup,(FileParameter)parameter);
    } else if(parameter instanceof DirectoryParameterImpl) {
      implementation = new PluginDirectoryParameter(pluginGroup,(DirectoryParameterImpl)parameter);
    } else if(parameter instanceof IntsParameter) {
      implementation = new PluginIntsParameter(pluginGroup,(IntsParameter)parameter);
    } else if(parameter instanceof StringListParameterImpl) {
      implementation = new PluginStringsParameter(pluginGroup,(StringListParameterImpl)parameter);
    } else if(parameter instanceof ColorParameter) {
      implementation = new PluginColorParameter(pluginGroup,(ColorParameter)parameter);
    }
  }
  
  public Control[] getControls() {
    return implementation.getControls();
  }
  
  public void setAdditionalActionPerfomer(IAdditionalActionPerformer performer) {
    if(implementation instanceof PluginBooleanParameter) {
      ((PluginBooleanParameter)implementation).setAdditionalActionPerfomer(performer);
    }
  }
}
