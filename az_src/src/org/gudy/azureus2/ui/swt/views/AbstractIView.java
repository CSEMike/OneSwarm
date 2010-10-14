/*
 * Created on 29 juin 2003
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
package org.gudy.azureus2.ui.swt.views;

import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.ui.swt.Messages;

/**
 * @author René
 * 
 */

// XXX This class is used by plugins.  Don't remove any functions from it!
public abstract class AbstractIView implements IView {
	// XXX AEMonitor introduced to plugin interface..
	protected AEMonitor this_mon 	= new AEMonitor( "AbstractIView" );
	
	private String lastFullTitleKey = null;
	
	private String lastFullTitle = "";

  public void initialize(Composite composite){    
  }
  
  public Composite getComposite(){ return null; }
  public void refresh(){}
  
  /**
   * A basic implementation that disposes the composite
   * Should be called with super.delete() from any extending class.
   * Images, Colors and such SWT handles must be disposed by the class itself.
   */
  public void delete(){
    Composite comp = getComposite();
    if (comp != null && !comp.isDisposed())
      comp.dispose();
  }

  public String getData(){ return null; }

  /**
   * Called in order to set / update the title of this View.  When the view
   * is being displayed in a tab, the full title is used for the tooltip.
   * 
   * By default, this function will return text from the message bundles which
   * correspond to the key returned in #getData()
   * 
   * @return the full title for the view
   */
	public String getFullTitle() {
		String key = getData();
		if (key == null) {
			return "";
		}

		if ( lastFullTitle.length() > 0 && key.equals(lastFullTitleKey)) {
			return lastFullTitle;
		}

		lastFullTitleKey = key;

		if (MessageText.keyExists(key)) {
			lastFullTitle = MessageText.getString(key);
		} else {
			lastFullTitle = key.replace('.', ' '); // support old plugins
		}

		return lastFullTitle;
	}

  /**
   * Called in order to set / update the short title of this view.  When the 
   * view is being displayed in a tab, the short title is used for the tab's
   * text.
   * 
   * By default, this function will return the full title. If the full title
   * is over 30 characters, it will be trimmed and "..." will be added
   * 
   * @return A short title for the view
   */
  public final String getShortTitle() {
    String shortTitle = getFullTitle();
    if(shortTitle != null && shortTitle.length() > 30) {
      shortTitle = shortTitle.substring(0,30) + "...";
    }
    return shortTitle;
	}
  
  public void updateLanguage() {
	lastFullTitle = "";
    Messages.updateLanguageForControl(getComposite());
  }
  
  
  // IconBarEnabler
  public boolean isEnabled(String itemKey) {
    return false;
  }
  
  // IconBarEnabler
  public boolean isSelected(String itemKey) {
    return false;
  }

  // IconBarEnabler
  public void itemActivated(String itemKey) {   
  }

  public void
  generateDiagnostics(
	IndentWriter	writer )
  {
	  writer.println( "Diagnostics for " + this + " (" + getFullTitle()+ ")");
  }

  public void dataSourceChanged(Object newDataSource) {
  }
}
