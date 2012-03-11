/*
 * Created on 10 juil. 2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.ui.swt.config;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.ui.swt.config.generic.GenericIntParameter;

/**
 * @author Olivier
 * 
 */
public class 
IntParameter 
	extends Parameter
{ 
  protected GenericIntParameter	delegate;
  
  public IntParameter(Composite composite, final String name) {
  	super(name);
	  delegate = new GenericIntParameter(config_adapter, composite, name);
  }

  /**
   * @deprecated defaultValue should be set via ConfigurationDefaults, not passed by the caller 
   */
  public IntParameter(Composite composite, final String name, int defaultValue) {
  	super(name);
	  delegate = new GenericIntParameter(config_adapter, composite, name,
				defaultValue);
  }
  
  
  public IntParameter(Composite composite,
                      final String name,
                      int minValue,
                      int maxValue) {
  	super(name);
	  delegate = new GenericIntParameter(config_adapter, composite, name,
				minValue, maxValue);
  }
  
  public boolean
  isInitialised()
  {
	  return( delegate != null );
  }
  
  public void
  setMinimumValue(
  	int		value )
  {
	  delegate.setMinimumValue( value );
  }
  public void
  setMaximumValue(
  	int		value )
  {
  	delegate.setMaximumValue( value );
  }
  
  public void
  setValue(
  	int		value )
  {
	  delegate.setValue( value );
  }
  
  public void
  resetToDefault()
  {
	  delegate.resetToDefault();
  }
  
  public int
  getValue()
  {
  	return( delegate.getValue());
  }
  
  public void setLayoutData(Object layoutData) {
   delegate.setLayoutData( layoutData );
  }
  
  public Control
  getControl()
  {
  	return( delegate.getControl());
  }

	public boolean isGeneratingIntermediateEvents() {
		return delegate.isGeneratingIntermediateEvents();
	}

	public void setGenerateIntermediateEvents(boolean generateIntermediateEvents) {
		delegate.setGenerateIntermediateEvents(generateIntermediateEvents);
	}

  public void setValue(Object value) {
  	if (value instanceof Number) {
  		setValue(((Number)value).intValue());
  	}
  }
  
  public Object getValueObject() {
  	return new Integer(getValue());
  }
}