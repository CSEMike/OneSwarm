/*
 * File    : ChangeSelectionActionPerformer.java
 * Created : 10 oct. 2003 15:38:53
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Olivier
 * 
 */
public class ChangeSelectionActionPerformer implements IAdditionalActionPerformer{

  boolean selected = false;
  boolean reverse_sense = false;
  
  Control[] controls;
  
  public ChangeSelectionActionPerformer(Control[] controls) {
	this.controls = controls;
  } 
  
  public ChangeSelectionActionPerformer(Control control) {
		this.controls = new Control[]{ control };
  } 
  
  public ChangeSelectionActionPerformer(Parameter p) {
		this.controls = p.getControls();
  } 
  public ChangeSelectionActionPerformer(Parameter p1, Parameter p2) {
	  this( new Parameter[]{ p1, p2 });
  }
  public ChangeSelectionActionPerformer(Parameter[] params ) {

	  List	c = new ArrayList();
	  
	  for (int i=0;i<params.length;i++){
		  Control[] x = params[i].getControls();
		  
		  for (int j=0;j<x.length;j++){
			  c.add( x[j] );
		  }
	  }
	  
	  controls = new Control[c.size()];
	  
	  c.toArray( controls );
  }
  
  public ChangeSelectionActionPerformer(Control[] controls, boolean _reverse_sense) {
  	this.controls = controls;
	reverse_sense = _reverse_sense;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.AdditionalActionPerformer#performAction()
   */
  public void performAction() {
    if(controls == null)
      return;
    controlsSetEnabled(controls, reverse_sense?!selected:selected);
  }
  
  private void controlsSetEnabled(Control[] controls, boolean bEnabled) {
    for(int i = 0 ; i < controls.length ; i++) {
      if (controls[i] instanceof Composite)
        controlsSetEnabled(((Composite)controls[i]).getChildren(), bEnabled);
      controls[i].setEnabled(bEnabled);
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.AdditionalActionPerformer#setIntValue(int)
   */
  public void setIntValue(int value) {    
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.AdditionalActionPerformer#setSelected(boolean)
   */
  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.AdditionalActionPerformer#setStringValue(java.lang.String)
   */
  public void setStringValue(String value) {    
  }

}
