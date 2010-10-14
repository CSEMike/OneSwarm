/*
 * Created on 22 juin 2005
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 * 
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.views.stats;

import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.ui.swt.views.AbstractIView;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.plugins.dht.DHTPlugin;

public class VivaldiView extends AbstractIView {
  
  DHT dht;  
  Composite panel;
  VivaldiPanel drawPanel;
	private final boolean autoAlpha;
  
  public VivaldiView() {
  	autoAlpha = false;
    init();
  }
  
  public VivaldiView(boolean autoAlpha) {
  	this.autoAlpha = autoAlpha;
		init();
  }
  
  private void init() {
    try {
      PluginInterface dht_pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );
    
      if ( dht_pi == null ){
    	   
    	return;
      }
       
      DHT[] dhts = ((DHTPlugin)dht_pi.getPlugin()).getDHTs();
    
      if (dhts.length == 0){
    	  return;
      }
      
      dht = dhts[dhts.length-1];
    } catch(Exception e) {
      Debug.printStackTrace( e );
    }
  }
  
  public void initialize(Composite composite) {
    panel = new Composite(composite,SWT.NULL);
    panel.setLayout(new FillLayout());    
    drawPanel = new VivaldiPanel(panel);    
  	drawPanel.setAutoAlpha(autoAlpha);
  }
  
  public Composite getComposite() {
   return panel;
  }
  
  public void refresh() {
  	if (dht == null) {
  		init();
  	}
  	
    if(dht != null) {
      List l = dht.getControl().getContacts();
      drawPanel.refreshContacts(l,dht.getControl().getTransport().getLocalContact());
    }
  }
  
  public String getData() {
    return "VivaldiView.title.full";
  }

  public String getFullTitle() {
    return MessageText.getString("VivaldiView.title.full"); //$NON-NLS-1$
  }
  
	public void delete() {
		drawPanel.delete();
		super.delete();
	}
}
