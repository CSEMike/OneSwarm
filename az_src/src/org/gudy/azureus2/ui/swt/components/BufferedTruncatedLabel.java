/*
 * Created on 04-Jan-2005
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

package org.gudy.azureus2.ui.swt.components;

import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core3.util.DisplayFormatters;

/**
 * @author parg
 *
 */

public class 
BufferedTruncatedLabel 
	extends BufferedWidget 
{
	protected Label	label;
	protected int	width;
	
	protected String	value = "";
	
	public
	BufferedTruncatedLabel(
		Composite		composite,
		int				attrs,
		int				_width)
	{
		super( new Label( composite, attrs ));
		
		label 	= (Label)getWidget();
		width	= _width;
	}
		
	public boolean
	isDisposed()
	{
		return( label.isDisposed());
	}
	
	public void
	setLayoutData(
		GridData	gd )
	{
  	if (isDisposed()) {
  		return;
  	}
		label.setLayoutData( gd );
	}
	
	public void
	setText(
		String	new_value )
	{
		if ( label.isDisposed()){
			return;
		}
				
		if ( new_value == value ){
			
			return;
		}
		
		if (	new_value != null && 
				value != null &&
				new_value.equals( value )){
					
			return;
		}
		
		value = new_value;
		
			// '&' chars that occur in the text are treated as accelerators and, for example,
			// cause the nect character to be underlined on Windows. This is generally NOT
			// the desired behaviour of a label in Azureus so by default we escape them
		
		label.setText( value==null?"":DisplayFormatters.truncateString( value.replaceAll("&", "&&" ), width ));	
	}	
	
  public String getText() {
    return value==null?"":value;
  }
  
  public void addMouseListener(MouseListener listener) {
    label.addMouseListener(listener);
  }
  
  public void setForeground(Color color) {
  	if (isDisposed()) {
  		return;
  	}
    label.setForeground(color);
  }
  
  public void setCursor(Cursor cursor) {
  	if (isDisposed() || cursor == null || cursor.isDisposed()) {
  		return;
  	}
    label.setCursor(cursor);
  }
  
  public void setToolTipText(String toolTipText) {
  	if (isDisposed()) {
  		return;
  	}
    label.setToolTipText(toolTipText);
  }
      
}