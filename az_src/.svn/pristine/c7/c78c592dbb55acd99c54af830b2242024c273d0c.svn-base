/*
 * File    : GenericParameter.java
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

import java.util.*;

import org.gudy.azureus2.pluginsimpl.local.PluginConfigImpl;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.config.*;
import org.gudy.azureus2.plugins.ui.config.EnablerParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;

/**
 * @author epall
 *
 */
public class 
ParameterImpl 
	implements EnablerParameter, org.gudy.azureus2.core3.config.ParameterListener
{
	protected 	PluginConfigImpl	config;
	private 	String 			key;
	private 	String 			labelKey;
	private 	String 			label;
	private		int				mode = MODE_BEGINNER;
	
	private	boolean	enabled							= true;
	private boolean	visible							= true;
	private boolean generate_intermediate_events	= true;
	
	private List toDisable	= new ArrayList();
	private List toEnable	= new ArrayList();
	  
	private List	listeners		= new ArrayList();
	private List	impl_listeners	= new ArrayList();
	
	private ParameterGroupImpl	parameter_group;
	
	public 
	ParameterImpl(
		PluginConfigImpl	_config,
		String 			_key, 
		String 			_label )
	{
		config	= _config;
		key		= _key;
		labelKey 	= _label;
		if ("_blank".equals(labelKey)) {
			labelKey = "!!";
		}
		label = MessageText.getString(labelKey);
	}
	/**
	 * @return Returns the key.
	 */
	public String getKey()
	{
		return key;
	}
	
	 public void addDisabledOnSelection(Parameter parameter) {
	    toDisable.add(parameter);
	  }
	  
	  public void addEnabledOnSelection(Parameter parameter) {    
	    toEnable.add(parameter);
	  }
	  
	  public List getDisabledOnSelectionParameters() {
	    return toDisable;
	  }
	  
	  public List getEnabledOnSelectionParameters() {
	    return toEnable;
	  }
		
	public void
	parameterChanged(
		String		key )
	{
		fireParameterChanged();
	}
	
	protected void
	fireParameterChanged()
	{
		// toArray() since listener trigger may remove listeners
		Object[] listenerArray = listeners.toArray();
		for (int i = 0; i < listenerArray.length; i++) {
			try {
				Object o = listenerArray[i];
				if (o instanceof ParameterListener) {

					((ParameterListener) o).parameterChanged(this);

				} else {

					((ConfigParameterListener) o).configParameterChanged(this);
				}
			} catch (Throwable f) {

				Debug.printStackTrace(f);
			}
		}
	}
	
	public void
	setEnabled(
		boolean	e )
	{
		enabled = e;

		// toArray() since listener trigger may remove listeners
		Object[] listenersArray = impl_listeners.toArray();
		for (int i = 0; i < listenersArray.length; i++) {
			try {
				ParameterImplListener l = (ParameterImplListener) listenersArray[i];
				l.enabledChanged(this);
			} catch (Throwable f) {

				Debug.printStackTrace(f);
			}
		}
	}
	
	public boolean
	isEnabled()
	{
		return( enabled );
	}
	
	public int 
	getMinimumRequiredUserMode() 
	{
		return( mode );
	}
	
	public void 
	setMinimumRequiredUserMode(
		int 	_mode )
	{
		mode	= _mode;
	}
	
	public void
	setVisible(
		boolean	_visible )
	{
		visible	= _visible;
	}
	
	public boolean
	isVisible()
	{
		return( visible );
	}
	
	public void
	setGenerateIntermediateEvents(
		boolean		b )
	{
		generate_intermediate_events = b;
	}
	
	public boolean
	getGenerateIntermediateEvents()
	{
		return( generate_intermediate_events );
	}
	
	public void
	setGroup(
		ParameterGroupImpl	_group )
	{
		parameter_group = _group;
	}
	
	public ParameterGroupImpl
	getGroup()
	{
		return( parameter_group );
	}
	
	public void
	addListener(
		ParameterListener	l )
	{
		listeners.add(l);
		
		if ( listeners.size() == 1 ){
			
			COConfigurationManager.addParameterListener( key, this );
		}
	}
			
	public void
	removeListener(
		ParameterListener	l )
	{
		listeners.remove(l);
		
		if ( listeners.size() == 0 ){
			
			COConfigurationManager.removeParameterListener( key, this );
		}
	}
	
	public void
	addImplListener(
		ParameterImplListener	l )
	{
		impl_listeners.add(l);
	}
				
	public void
	removeImplListener(
		ParameterImplListener	l )
	{
		impl_listeners.remove(l);
	}
		
	public void
	addConfigParameterListener(
		ConfigParameterListener	l )
	{
		listeners.add(l);
		
		if ( listeners.size() == 1 ){
			
			COConfigurationManager.addParameterListener( key, this );
		}
	}
			
	public void
	removeConfigParameterListener(
		ConfigParameterListener	l )
	{
		listeners.remove(l);
		
		if ( listeners.size() == 0 ){
			
			COConfigurationManager.removeParameterListener( key, this );
		}
	}
	
	public String getLabelText() {
		return label;
	}

	public void setLabelText(String sText) {
		labelKey = null;
		label = sText;

		triggerLabelChanged(sText, false);
	}

	public String getLabelKey() {
		return labelKey;
	}
	
	public void setLabelKey(String sLabelKey) {
		labelKey = sLabelKey;
		label = MessageText.getString(sLabelKey);

		triggerLabelChanged(labelKey, true);
	}
	
	private void triggerLabelChanged(String text, boolean isKey) {
		// toArray() since listener trigger may remove listeners
		Object[] listenersArray = impl_listeners.toArray();
		for (int i = 0; i < listenersArray.length; i++) {
			try {
				ParameterImplListener l = (ParameterImplListener) listenersArray[i];
				l.labelChanged(this, text, isKey);

			} catch (Throwable f) {

				Debug.printStackTrace(f);
			}
		}
	}
	
	public void
	destroy()
	{
		listeners.clear();
		impl_listeners.clear();
		toDisable.clear();
		toEnable.clear();
		
		COConfigurationManager.removeParameterListener( key, this );
	}
}
