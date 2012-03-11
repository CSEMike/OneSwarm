/*
 * File    : ConfigPanel*.java
 * Created : 11 mar. 2004
 * By      : TuxPaper
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

package org.gudy.azureus2.ui.swt.views.configsections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

public class ConfigSectionSharing implements UISWTConfigSection {
  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_ROOT;
  }

	public String configSectionGetName() {
		return "sharing";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
  
	public int maxUserMode() {
		return 0;
	}

  

  public Composite configSectionCreate(final Composite parent) {
    GridData gridData;
	GridLayout layout;

    Composite gSharing = new Composite(parent, SWT.WRAP);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gSharing.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginHeight = 0;
    gSharing.setLayout(layout);

    	// row
    
	gridData = new GridData();
    Label protocol_lab = new Label(gSharing, SWT.NULL);
    Messages.setLanguageText(protocol_lab, "ConfigView.section.sharing.protocol");
	protocol_lab.setLayoutData( gridData );
	
	String[]	protocols = {"HTTP","HTTPS","UDP","DHT" };
    String[]	descs = {"HTTP","HTTPS (SSL)", "UDP", "Decentralised" };
    
	new StringListParameter(gSharing, "Sharing Protocol", "DHT", descs, protocols );

	// row
    
	GridData grid_data = new GridData();
	grid_data.horizontalSpan = 2;
	final BooleanParameter private_torrent = 
		new BooleanParameter(gSharing, 	"Sharing Torrent Private", 
                         			"ConfigView.section.sharing.privatetorrent");
	private_torrent.setLayoutData(grid_data);
	

	// row
    gridData = new GridData();
    gridData.horizontalSpan = 2;
	final BooleanParameter permit_dht = 
		new BooleanParameter(gSharing, "Sharing Permit DHT", 
                         "ConfigView.section.sharing.permitdht");
	permit_dht.setLayoutData( gridData );

	private_torrent.setAdditionalActionPerformer(new ChangeSelectionActionPerformer( permit_dht.getControls(), true ));

	private_torrent.addChangeListener(
		new ParameterChangeAdapter()
		{
			public void 
			parameterChanged(
				Parameter p, 
				boolean caused_internally )
			{
				if ( private_torrent.isSelected() ){
					
					permit_dht.setSelected( false );
				}
			}
	
		});
	
    	// row
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(gSharing, "Sharing Add Hashes", 
                         "wizard.createtorrent.extrahashes").setLayoutData( gridData );
    

    	// row
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    BooleanParameter rescan_enable = 
    	new BooleanParameter(gSharing, "Sharing Rescan Enable", 
    						"ConfigView.section.sharing.rescanenable");
    
	rescan_enable.setLayoutData( gridData );

    	//row
    gridData = new GridData();
	gridData.horizontalIndent = 25;
    Label period_label = new Label(gSharing, SWT.NULL );
    Messages.setLanguageText(period_label, "ConfigView.section.sharing.rescanperiod");
	period_label.setLayoutData( gridData );

    gridData = new GridData();
	IntParameter rescan_period = new IntParameter(gSharing, "Sharing Rescan Period");
    rescan_period.setMinimumValue(1);
    rescan_period.setLayoutData( gridData );
    
    rescan_enable.setAdditionalActionPerformer(new ChangeSelectionActionPerformer( rescan_period.getControls() ));
    rescan_enable.setAdditionalActionPerformer(new ChangeSelectionActionPerformer( new Control[]{period_label} ));
	
    	// comment
    
    Label comment_label = new Label(gSharing, SWT.NULL );
    Messages.setLanguageText(comment_label, "ConfigView.section.sharing.torrentcomment");
	
	new Label(gSharing, SWT.NULL);
	
	gridData = new GridData(GridData.FILL_HORIZONTAL);
	gridData.horizontalIndent = 25;
	gridData.horizontalSpan = 2;
    StringParameter torrent_comment = new StringParameter(gSharing, "Sharing Torrent Comment", "" ); 
    torrent_comment.setLayoutData(gridData);
    
    return gSharing;
       
  }
}
