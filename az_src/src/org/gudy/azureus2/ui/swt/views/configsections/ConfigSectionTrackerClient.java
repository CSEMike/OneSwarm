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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

public class 
ConfigSectionTrackerClient 
	implements UISWTConfigSection 
{
  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_TRACKER;
  }

	public String configSectionGetName() {
		return "tracker.client";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
  
	public int maxUserMode() {
		return 2;
	}


  public Composite configSectionCreate(final Composite parent) {
    GridData gridData;
    GridLayout layout;
    Label  label;
    int userMode = COConfigurationManager.getIntParameter("User Mode");

    // extensions tab set up
    Composite gMainTab = new Composite(parent, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gMainTab.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 3;
    gMainTab.setLayout(layout);

    	//////////////////////SCRAPE GROUP ///////////////////
    
    Group scrapeGroup = new Group(gMainTab,SWT.NULL);
    Messages.setLanguageText(scrapeGroup,"ConfigView.group.scrape");
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 1;
    scrapeGroup.setLayout(gridLayout);
    
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    scrapeGroup.setLayoutData( gridData );
    
    label = new Label(scrapeGroup, SWT.WRAP);
    label.setLayoutData(Utils.getWrappableLabelGridData(1, GridData.FILL_HORIZONTAL));
    Messages.setLanguageText(label, "ConfigView.section.tracker.client.scrapeinfo");

    BooleanParameter	scrape = 
    	new BooleanParameter(scrapeGroup, "Tracker Client Scrape Enable",
    							"ConfigView.section.tracker.client.scrapeenable");
    
    BooleanParameter	scrape_stopped = 
    	new BooleanParameter(scrapeGroup, "Tracker Client Scrape Stopped Enable",
    							"ConfigView.section.tracker.client.scrapestoppedenable");
    
    scrape.setAdditionalActionPerformer(new ChangeSelectionActionPerformer( scrape_stopped.getControls()));

    new BooleanParameter(scrapeGroup, "Tracker Client Scrape Single Only",
    							"ConfigView.section.tracker.client.scrapesingleonly");
    
    /////////////////////////
    
    // row

    gridData = new GridData();
    gridData.horizontalSpan = 2;
  
    new BooleanParameter(gMainTab, "Tracker Client Send OS and Java Version",
                         "ConfigView.section.tracker.sendjavaversionandos").setLayoutData(gridData);

    label = new Label(gMainTab, SWT.NULL);
    

//////////////////////
    
    BooleanParameter enableUDP = new BooleanParameter(gMainTab, "Server Enable UDP", "ConfigView.section.server.enableudp");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    enableUDP.setLayoutData(gridData); 
    
    label = new Label(gMainTab, SWT.NULL);
  
//////////////////////
    
    BooleanParameter enableUDPProbe = new BooleanParameter(gMainTab, "Tracker UDP Probe Enable", "ConfigView.section.server.enableudpprobe");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    enableUDPProbe.setLayoutData(gridData); 
    
    label = new Label(gMainTab, SWT.NULL);
  
    enableUDP.setAdditionalActionPerformer(new ChangeSelectionActionPerformer( enableUDPProbe.getControls()));

    
//////////////////////
    
    BooleanParameter showWarnings = new BooleanParameter(gMainTab, "Tracker Client Show Warnings", "ConfigView.section.tracker.client.showwarnings" );
    gridData = new GridData();
    gridData.horizontalSpan = 2;
	showWarnings.setLayoutData(gridData); 
    
    label = new Label(gMainTab, SWT.NULL);
    
    if (userMode > 0) {
    
//////////////////////OVERRIDE GROUP ///////////////////
    
    Group overrideGroup = new Group(gMainTab,SWT.NULL);
    Messages.setLanguageText(overrideGroup,"ConfigView.group.override");
    gridLayout = new GridLayout();
    gridLayout.numColumns = 2;
    overrideGroup.setLayout(gridLayout);
    
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    overrideGroup.setLayoutData( gridData );
    
    
    label = new Label(overrideGroup, SWT.WRAP);
    label.setLayoutData(Utils.getWrappableLabelGridData(1, GridData.FILL_HORIZONTAL));
    Messages.setLanguageText(label, "ConfigView.label.overrideip");
    
    StringParameter overrideip = new StringParameter(overrideGroup, "Override Ip", "");
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    data.widthHint = 100;
    overrideip.setLayoutData(data);
    
    label = new Label(overrideGroup, SWT.WRAP);
    label.setLayoutData(Utils.getWrappableLabelGridData(1, GridData.FILL_HORIZONTAL));
    Messages.setLanguageText(label, "ConfigView.label.announceport");
    
    StringParameter tcpOverride = new StringParameter(overrideGroup, "TCP.Listen.Port.Override");
    data = new GridData();
    data.widthHint = 50;
    tcpOverride.setLayoutData(data);
    
    tcpOverride.addChangeListener(new ParameterChangeAdapter() {
    	public void stringParameterChanging(Parameter p, String toValue)
    	{
    		if(toValue == "")
    			return;
    		try
			{
    			int portVal = Integer.parseInt(toValue);
				if(portVal >= 0 && portVal <= 65535)
					return;
			} catch (NumberFormatException e) {}
			p.setValue("");
    	}
    });

    label = new Label(overrideGroup, SWT.WRAP);
    label.setLayoutData(Utils.getWrappableLabelGridData(1, GridData.FILL_HORIZONTAL));
    Messages.setLanguageText(label, "ConfigView.label.noportannounce");
    
    BooleanParameter noPortAnnounce = new BooleanParameter(overrideGroup,"Tracker Client No Port Announce");
    data = new GridData();
    noPortAnnounce.setLayoutData(data);
    
    label = new Label(overrideGroup, SWT.WRAP);
    label.setLayoutData(Utils.getWrappableLabelGridData(1, GridData.FILL_HORIZONTAL));
    Messages.setLanguageText(label, "ConfigView.label.maxnumwant");
    
    IntParameter numwant = new IntParameter(overrideGroup, "Tracker Client Numwant Limit",0,100);
    data = new GridData();
    numwant.setLayoutData(data);
    
    label = new Label(overrideGroup, SWT.WRAP);
    label.setLayoutData(Utils.getWrappableLabelGridData(1, GridData.FILL_HORIZONTAL));
    Messages.setLanguageText(label, "ConfigView.label.minannounce");
    
    IntParameter minmininterval = new IntParameter(overrideGroup, "Tracker Client Min Announce Interval");
    data = new GridData();
    minmininterval.setLayoutData(data);

    
    //////////////////////////
    
    if(userMode>1) {
    
    // row
    
    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label,  "ConfigView.section.tracker.client.connecttimeout");
    gridData = new GridData();
    IntParameter	connect_timeout = new IntParameter(gMainTab, "Tracker Client Connect Timeout" );
    connect_timeout.setLayoutData(gridData);
    label = new Label(gMainTab, SWT.NULL);

    // row
    
    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label,  "ConfigView.section.tracker.client.readtimeout");
    gridData = new GridData();
    IntParameter	read_timeout = new IntParameter(gMainTab, "Tracker Client Read Timeout" );
    read_timeout.setLayoutData(gridData);
    label = new Label(gMainTab, SWT.NULL);

    ////// main tab 
    
    // row

    gridData = new GridData();
    gridData.horizontalSpan = 2;
  
    new BooleanParameter(gMainTab, "Tracker Key Enable Client",
                         "ConfigView.section.tracker.enablekey").setLayoutData(gridData);

    label = new Label(gMainTab, SWT.NULL);
    
    
    // row

    gridData = new GridData();
    gridData.horizontalSpan = 2;
 
    new BooleanParameter(gMainTab, "Tracker Separate Peer IDs",
                         "ConfigView.section.tracker.separatepeerids").setLayoutData(gridData);
  
    label = new Label(gMainTab, SWT.WRAP);
    label.setLayoutData(Utils.getWrappableLabelGridData(1, GridData.FILL_HORIZONTAL));
    Messages.setLanguageText(label,  "ConfigView.section.tracker.separatepeerids.info");
    
    }
    }


    return gMainTab;
  }
}
