/*
 * File    : PluginConfigUIFactoryImpl.java
 * Created : Nov 21, 2003
 * By      : epall
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
 
package org.gudy.azureus2.pluginsimpl.local.ui.config;

import org.gudy.azureus2.plugins.ui.config.EnablerParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.PluginConfigUIFactory;

import org.gudy.azureus2.pluginsimpl.local.PluginConfigImpl;

/**
 * @author epall
 *
 */
public class PluginConfigUIFactoryImpl implements PluginConfigUIFactory
{

  String 		pluginKey;
  PluginConfigImpl	config;
  
  public 
  PluginConfigUIFactoryImpl(
  	PluginConfigImpl	_config,
  	String 			_pluginKey) 
  {
  	config		= _config;
    pluginKey 	= _pluginKey;
  }
  
  
  public Parameter createIntParameter(
    String key,
    String label,
    int defaultValue,
    int[] values,
    String[] labels) {
    return new IntsParameter(config,pluginKey + "." + key,label,defaultValue,values,labels);
  }

	public EnablerParameter createBooleanParameter(
		String key,
		String label,
		boolean defaultValue)
	{
	  return new BooleanParameterImpl(config,pluginKey + "." + key, label, defaultValue);
	}

	public Parameter createIntParameter(
		String key,
		String label,
		int defaultValue)
	{
	  return new IntParameterImpl(config,pluginKey + "." + key, label, defaultValue);
		
	}

	public Parameter createStringParameter(
		String key,
		String label,
		String defaultValue)
	{
		return new StringParameterImpl(config,pluginKey + "." + key, label, defaultValue);
	}
	
	public Parameter createStringParameter(
	    String key,
			String label,
			String defaultValue,
			String[] values,
			String[] labels) {
	  return new StringListParameterImpl(config,pluginKey + "." + key,label,defaultValue,values,labels);
	}

	public Parameter createFileParameter(
		String key,
		String label,
		String defaultValue)
	{
	  return new FileParameter(config,pluginKey + "." + key, label, defaultValue);
	}
	
	public Parameter createDirectoryParameter(
	    String key,
			String label,
			String defaultValue) {
	  return new DirectoryParameterImpl(config,pluginKey + "." + key, label, defaultValue);
	}

	public Parameter createColorParameter(
		String key,
		String label,
		int defaultValueRed,
    int defaultValueGreen,
    int defaultValueBlue)
	{
	  return new ColorParameter(config,pluginKey + "." + key,label,defaultValueRed,defaultValueGreen,defaultValueBlue);
	}

}
