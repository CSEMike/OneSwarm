/*
 * Created on 17-Jun-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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



 /** @author parg
 *
 */

import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.ui.swt.Messages;


public class 
ButtonParameter
	extends Parameter
{
	Button	button;

  public 
  ButtonParameter(
  	Composite composite,
	final String name_resource ) 
  {
  	super(name_resource);
    button = new Button( composite, SWT.PUSH );
    
    Messages.setLanguageText(button, name_resource);

    button.addListener(SWT.Selection, new Listener() {
	      public void handleEvent(Event event) 
	      {
	    	  if (change_listeners == null) {return;}
	       	for (int i=0;i<change_listeners.size();i++){
        		
        		((ParameterChangeListener)change_listeners.get(i)).parameterChanged(ButtonParameter.this,false);
        	}
	      }
    });
  }

  public void setLayoutData(Object layoutData) {
    button.setLayoutData(layoutData);
  }

  public Control getControl() 
  {
	 return button;
  }

  public void setValue(Object value) {
  }
}