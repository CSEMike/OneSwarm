/*
 * File    : BufferedLabel.java
 * Created : 24-Nov-2003
 * By      : parg
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

package org.gudy.azureus2.ui.swt.components;

/**
 * @author parg
 *
 */

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.GridData;

public class 
BufferedLabel
	extends BufferedWidget 
{
	protected Label	label;
	
	protected String	value = "";
	
	public
	BufferedLabel(
		Composite		composite,
		int			attrs )
	{
		super( new Label( composite, attrs ));
		
		label = (Label)getWidget();
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
		label.setLayoutData( gd );
	}
	
	public void
	setLayoutData(
		FormData	gd )
	{
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
		
		label.setText( value==null?"":value.replaceAll("&", "&&" ));	
	}	
	
  public String getText() {
    return value==null?"":value;
  }
  
  public void addMouseListener(MouseListener listener) {
    label.addMouseListener(listener);
  }
  
  public void setForeground(Color color) {
    label.setForeground(color);
  }
  
  public void setCursor(Cursor cursor) {
    label.setCursor(cursor);
  }
  
  public void setToolTipText(String toolTipText) {
    label.setToolTipText(toolTipText);
  }
      
}
