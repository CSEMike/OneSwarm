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
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.auth.CertificateCreatorWindow;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.ipchecker.IpCheckerWizard;
import org.gudy.azureus2.ui.swt.ipchecker.IpSetterCallBack;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

public class 
ConfigSectionTrackerServer 
	implements UISWTConfigSection 
{
	private static final String	CFG_PREFIX = "ConfigView.section.";
	
	private final int REQUIRED_MODE = 1;
	
	public int maxUserMode() {
		return 2;
	}

	
	public ConfigSectionTrackerServer() {
	}
	
  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_TRACKER;
  }

	public String configSectionGetName() {
		return "tracker.server";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
  

  public Composite configSectionCreate(final Composite parent) {
    GridData gridData;
    GridLayout layout;
    Label label;
    
    // main tab set up
    Composite gMainTab = new Composite(parent, SWT.NULL);

    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gMainTab.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 4;
    gMainTab.setLayout(layout);
    
    int userMode = COConfigurationManager.getIntParameter("User Mode");
    
	if (userMode < REQUIRED_MODE) {
		label = new Label(gMainTab, SWT.WRAP);
		gridData = new GridData();
		label.setLayoutData(gridData);

		final String[] modeKeys = { CFG_PREFIX + "mode.beginner",
				CFG_PREFIX + "mode.intermediate",
				CFG_PREFIX + "mode.advanced" };

		String param1, param2;
		if (REQUIRED_MODE < modeKeys.length)
			param1 = MessageText.getString(modeKeys[REQUIRED_MODE]);
		else
			param1 = String.valueOf(REQUIRED_MODE);
				
		if (userMode < modeKeys.length)
			param2 = MessageText.getString(modeKeys[userMode]);
		else
			param2 = String.valueOf(userMode);

		label.setText(MessageText.getString("ConfigView.notAvailableForMode",
				new String[] { param1, param2 } ));

		return gMainTab;
	}
    
      // MAIN TAB DATA

   	// row
    
    if(userMode>0) { // XXX

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.ip");

    final StringParameter tracker_ip = new StringParameter(gMainTab, "Tracker IP", "" );

    gridData = new GridData();
    gridData.widthHint = 120;
    tracker_ip.setLayoutData( gridData );

    Button check_button = new Button(gMainTab, SWT.PUSH);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    check_button.setLayoutData(gridData);
    
    Messages.setLanguageText(check_button, CFG_PREFIX + "tracker.checkip"); //$NON-NLS-1$

    final Display display = gMainTab.getDisplay();

    check_button.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event event) {
          IpCheckerWizard wizard = new IpCheckerWizard();
          wizard.setIpSetterCallBack(new IpSetterCallBack() {
              public void setIp(final String ip) {
                if(display == null || display.isDisposed())
                  return;
                  display.asyncExec(new AERunnable(){
                  public void runSupport() {
                    if(tracker_ip != null)
                      tracker_ip.setValue(ip);
                  }
                });
              }
           }); // setIPSetterCallback
         }
    });


    // row

    final BooleanParameter nonsslEnable = 
        new BooleanParameter(gMainTab, "Tracker Port Enable", 
                             CFG_PREFIX + "tracker.port");

    IntParameter tracker_port = new IntParameter(gMainTab, "Tracker Port", 0, 65535);

    gridData = new GridData();
    tracker_port.setLayoutData( gridData );

    final StringParameter tracker_port_backup = new StringParameter(gMainTab, "Tracker Port Backups", "" );

    gridData = new GridData();
    gridData.widthHint = 100;
    tracker_port_backup.setLayoutData( gridData );
    
    Label tracker_port_backup_label = new Label(gMainTab, SWT.NULL );
    Messages.setLanguageText(tracker_port_backup_label, CFG_PREFIX + "tracker.portbackup");

    Control[] non_ssl_controls = new Control[3];
    non_ssl_controls[0] = tracker_port.getControl();
    non_ssl_controls[1] = tracker_port_backup.getControl();
    non_ssl_controls[2] = tracker_port_backup_label;

    nonsslEnable.setAdditionalActionPerformer(new ChangeSelectionActionPerformer( non_ssl_controls ));

 
    // row

    final BooleanParameter sslEnable = 
        new BooleanParameter(gMainTab, "Tracker Port SSL Enable",
                             CFG_PREFIX + "tracker.sslport");

    IntParameter tracker_port_ssl = new IntParameter(gMainTab,
					"Tracker Port SSL", 0, 65535);
    gridData = new GridData();
    tracker_port_ssl.setLayoutData( gridData );

    final StringParameter tracker_port_ssl_backup = new StringParameter(gMainTab, "Tracker Port SSL Backups", "" );

    gridData = new GridData();
    gridData.widthHint = 100;
    tracker_port_ssl_backup.setLayoutData( gridData );
    
    Label tracker_port_ssl_backup_label = new Label(gMainTab, SWT.NULL );
    Messages.setLanguageText(tracker_port_ssl_backup_label, CFG_PREFIX + "tracker.portbackup");

    	// create cert row

    Label cert_label = new Label(gMainTab, SWT.NULL );
    Messages.setLanguageText(cert_label, CFG_PREFIX + "tracker.createcert");

    Button cert_button = new Button(gMainTab, SWT.PUSH);

    Messages.setLanguageText(cert_button, CFG_PREFIX + "tracker.createbutton");

    cert_button.addListener(SWT.Selection, 
    		new Listener() 
			{
		        public void 
				handleEvent(Event event) 
		        {
		        	new CertificateCreatorWindow();
		        }
		    });
    
    Label ssl_faq_label = new Label(gMainTab, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    ssl_faq_label.setLayoutData(gridData);
    Messages.setLanguageText(ssl_faq_label, CFG_PREFIX + "tracker.sslport.info");
    final String linkFAQ = "http://plugins.vuze.com/faq.php#19";
    ssl_faq_label.setCursor(ssl_faq_label.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
    ssl_faq_label.setForeground(Colors.blue);
    ssl_faq_label.addMouseListener(new MouseAdapter() {
       public void mouseDoubleClick(MouseEvent arg0) {
      	 Utils.launch(linkFAQ);
       }
       public void mouseDown(MouseEvent arg0) {
      	 Utils.launch(linkFAQ);
       }
    });
    
    Control[] ssl_controls = { 	
    		tracker_port_ssl.getControl(),
    		tracker_port_ssl_backup.getControl(),
    		tracker_port_ssl_backup_label,
    		ssl_faq_label,
    		cert_label, 
    		cert_button };
 

    sslEnable.setAdditionalActionPerformer(new ChangeSelectionActionPerformer( ssl_controls ));

    
    // row

    gridData = new GridData();
    gridData.horizontalSpan = 1;
    new BooleanParameter(gMainTab, "Tracker Public Enable",
                         CFG_PREFIX + "tracker.publicenable").setLayoutData( gridData );

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.publicenable.info");
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    label.setLayoutData(gridData);
    
    // row
    
    BooleanParameter forcePortDetails = 
        new BooleanParameter(gMainTab,  "Tracker Port Force External",
                             CFG_PREFIX + "tracker.forceport");

    label = new Label(gMainTab, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    label.setLayoutData(gridData);
    
    
    Control[] f_controls = new Control[1];
    f_controls[0] = forcePortDetails.getControl();

    IAdditionalActionPerformer f_enabler =
      new GenericActionPerformer(f_controls) {
        public void performAction()
        {
          boolean selected =  nonsslEnable.isSelected() ||
          sslEnable.isSelected();
    
          controls[0].setEnabled( selected );
        }
      };

    nonsslEnable.setAdditionalActionPerformer(f_enabler);
    sslEnable.setAdditionalActionPerformer(f_enabler);
    
    // row
    	// add announce urls to hosted torrents
    BooleanParameter hostAddURLs = 
        new BooleanParameter(gMainTab,  "Tracker Host Add Our Announce URLs",
                             CFG_PREFIX + "tracker.host.addurls");
   
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    hostAddURLs.setLayoutData( gridData );
    
    label = new Label(gMainTab, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);
    
    // row

    gridData = new GridData();
    gridData.horizontalSpan = 1;
    final BooleanParameter passwordEnableWeb = 
        new BooleanParameter(gMainTab, "Tracker Password Enable Web", 
                             CFG_PREFIX + "tracker.passwordenableweb");
    passwordEnableWeb.setLayoutData( gridData );
    
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    final BooleanParameter passwordWebHTTPSOnly = 
        new BooleanParameter(gMainTab, "Tracker Password Web HTTPS Only", 
                             CFG_PREFIX + "tracker.passwordwebhttpsonly");
    passwordWebHTTPSOnly.setLayoutData( gridData );

    IAdditionalActionPerformer web_https_enabler =
        new GenericActionPerformer(passwordWebHTTPSOnly.getControls())
            {
	            public void performAction()
	            {
	              boolean selected =  	passwordEnableWeb.isSelected() &&
				  						sslEnable.isSelected();
	
	              for (int i=0;i<controls.length;i++){
	              	
	              	controls[i].setEnabled( selected );
	              }
	            }
            };

    passwordEnableWeb.setAdditionalActionPerformer(web_https_enabler);
    sslEnable.setAdditionalActionPerformer(web_https_enabler);

    // row

     final BooleanParameter passwordEnableTorrent = 
      new BooleanParameter(gMainTab, "Tracker Password Enable Torrent", 
                           CFG_PREFIX + "tracker.passwordenabletorrent");
 
    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.passwordenabletorrent.info");
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    label.setLayoutData( gridData );

     // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.username");

    final StringParameter tracker_username = new StringParameter(gMainTab, "Tracker Username", "" );

    gridData = new GridData();
    gridData.widthHint = 100;
    tracker_username.setLayoutData( gridData );

    label = new Label(gMainTab, SWT.NULL);
    label = new Label(gMainTab, SWT.NULL);
     // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.password");

    final PasswordParameter tracker_password = new PasswordParameter(gMainTab, "Tracker Password" );

    gridData = new GridData();
    gridData.widthHint = 100;
    tracker_password.setLayoutData( gridData );

    label = new Label(gMainTab, SWT.NULL);
    label = new Label(gMainTab, SWT.NULL);

    Control[] x_controls = new Control[2];
    x_controls[0] = tracker_username.getControl();
    x_controls[1] = tracker_password.getControl();

    IAdditionalActionPerformer enabler =
        new GenericActionPerformer(x_controls)
            {
            public void performAction()
            {
              boolean selected =  passwordEnableWeb.isSelected() ||
                        passwordEnableTorrent.isSelected();

              for (int i=0;i<controls.length;i++){

                controls[i].setEnabled( selected );
              }
            }
            };

    passwordEnableWeb.setAdditionalActionPerformer(enabler);
    passwordEnableTorrent.setAdditionalActionPerformer(enabler);
    

    
    	// Poll Group //
    
    Group gPollStuff = new Group(gMainTab, SWT.NULL);
    Messages.setLanguageText(gPollStuff, CFG_PREFIX + "tracker.pollinterval");
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gridData.horizontalSpan = 4;
    gPollStuff.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 4;
    gPollStuff.setLayout(layout);

    label = new Label(gPollStuff, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.pollintervalmin");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter pollIntervalMin = new IntParameter(gPollStuff, "Tracker Poll Interval Min");

    gridData = new GridData();
    pollIntervalMin.setLayoutData( gridData );

    label = new Label(gPollStuff, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.pollintervalmax");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter pollIntervalMax = new IntParameter(gPollStuff, "Tracker Poll Interval Max");

    gridData = new GridData();
    pollIntervalMax.setLayoutData( gridData );

    // row

    label = new Label(gPollStuff, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.pollintervalincby");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter pollIntervalIncBy = new IntParameter(gPollStuff, "Tracker Poll Inc By");

    gridData = new GridData();
    pollIntervalIncBy.setLayoutData( gridData );

    label = new Label(gPollStuff, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.pollintervalincper");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter pollIntervalIncPer = new IntParameter(gPollStuff, "Tracker Poll Inc Per");

    gridData = new GridData();
    pollIntervalIncPer.setLayoutData( gridData );

    
    // scrape + cache group

    Group gScrapeCache = new Group(gMainTab, SWT.NULL);
    Messages.setLanguageText(gScrapeCache, CFG_PREFIX + "tracker.scrapeandcache");
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gridData.horizontalSpan = 4;
    gScrapeCache.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 4;
    gScrapeCache.setLayout(layout);
    
    // row
    
    label = new Label(gScrapeCache, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.announcescrapepercentage");

    IntParameter scrapeannouncepercentage = new IntParameter(gScrapeCache, "Tracker Scrape Retry Percentage");

    gridData = new GridData();
    scrapeannouncepercentage.setLayoutData( gridData );
    
    label = new Label(gScrapeCache, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.scrapecacheperiod");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter scrapeCachePeriod = new IntParameter(gScrapeCache, "Tracker Scrape Cache");

    gridData = new GridData();
    scrapeCachePeriod.setLayoutData( gridData );
    
 
    // row

    label = new Label(gScrapeCache, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.announcecacheminpeers");

    IntParameter announceCacheMinPeers = new IntParameter(gScrapeCache, "Tracker Announce Cache Min Peers");

    gridData = new GridData();
    announceCacheMinPeers.setLayoutData( gridData );
    
    label = new Label(gScrapeCache, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.announcecacheperiod");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter announceCachePeriod = new IntParameter(gScrapeCache, "Tracker Announce Cache");

    gridData = new GridData();
    announceCachePeriod.setLayoutData( gridData );

    
    // main tab again
    // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.maxpeersreturned");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter maxPeersReturned = new IntParameter(gMainTab, "Tracker Max Peers Returned");

    gridData = new GridData();
    maxPeersReturned.setLayoutData( gridData );

    label = new Label(gMainTab, SWT.NULL);
    label = new Label(gMainTab, SWT.NULL);

    	// seed retention limit
    
    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.seedretention");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter seedRetentionLimit = new IntParameter(gMainTab, "Tracker Max Seeds Retained");

    gridData = new GridData();
    seedRetentionLimit.setLayoutData( gridData );

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.seedretention.info");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData( gridData );

    	// row

    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(gMainTab, "Tracker NAT Check Enable", 
                         CFG_PREFIX + "tracker.natcheckenable").setLayoutData( gridData );
    
    Composite gNATDetails = new Composite(gMainTab, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gridData.horizontalSpan = 2;
    gNATDetails.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginHeight=0;
    layout.marginWidth=0;
    gNATDetails.setLayout(layout);
    
    	// row
    
    label = new Label(gNATDetails, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.natchecktimeout");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter NATTimeout = new IntParameter(gNATDetails, "Tracker NAT Check Timeout");

    gridData = new GridData();
    NATTimeout.setLayoutData( gridData );

 
    // row
    
    gridData = new GridData();
    gridData.horizontalSpan = 4;
 
    new BooleanParameter(gMainTab, "Tracker Send Peer IDs",
                         CFG_PREFIX + "tracker.sendpeerids").setLayoutData(gridData);
    
    // row
 
    gridData = new GridData();
    gridData.horizontalSpan = 4;
 
    BooleanParameter	enable_udp = 
    	new BooleanParameter(gMainTab, "Tracker Port UDP Enable", 
                         CFG_PREFIX + "tracker.enableudp");

    enable_udp.setLayoutData(gridData);
    
    // row
    
    Label udp_version_label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(udp_version_label,  CFG_PREFIX + "tracker.udpversion");
    gridData = new GridData();
    IntParameter	udp_version = new IntParameter(gMainTab, "Tracker Port UDP Version");
    udp_version.setLayoutData(gridData);
    label = new Label(gMainTab, SWT.NULL);
    label = new Label(gMainTab, SWT.NULL);

    enable_udp.setAdditionalActionPerformer(
    		new ChangeSelectionActionPerformer( new Control[]{ udp_version_label, udp_version.getControl() }));

    // row
    
    gridData = new GridData();
    gridData.horizontalSpan = 4;
 
    new BooleanParameter(gMainTab, "Tracker Compact Enable",
                         CFG_PREFIX + "tracker.enablecompact").setLayoutData(gridData);
    
    // row

    gridData = new GridData();
    gridData.horizontalSpan = 4;
    BooleanParameter log_enable = 
    	new BooleanParameter(gMainTab, "Tracker Log Enable", 
                         CFG_PREFIX + "tracker.logenable");
    log_enable.setLayoutData( gridData );
    
    if(userMode>1) { // XXX
    
    // row

    gridData = new GridData();
    gridData.horizontalSpan = 4;
 
    new BooleanParameter(gMainTab, "Tracker Key Enable Server",
                         CFG_PREFIX + "tracker.enablekey").setLayoutData(gridData);

    // Networks Group //
    
    Group networks_group = new Group( gMainTab, SWT.NULL );
    Messages.setLanguageText( networks_group, CFG_PREFIX + "tracker.server.group.networks" );
    GridData    networks_layout = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    networks_layout.horizontalSpan = 4;
    networks_group.setLayoutData( networks_layout );
    layout = new GridLayout();
    layout.numColumns = 2;
    networks_group.setLayout(layout);
        
    label = new Label(networks_group, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.server.group.networks.info");
    GridData grid_data = new GridData();
    grid_data.horizontalSpan = 2;
    label.setLayoutData( grid_data );
    
    for (int i=0;i<AENetworkClassifier.AT_NETWORKS.length;i++){
		
		String	nn = AENetworkClassifier.AT_NETWORKS[i];
	
		String	config_name = "Tracker Network Selection Default." + nn;
		String	msg_text	= CFG_PREFIX + "connection.networks." + nn;
		 
		BooleanParameter network = new BooleanParameter(networks_group, config_name, msg_text );
				
	    grid_data = new GridData();
	    grid_data.horizontalSpan = 2;
	    network.setLayoutData( grid_data );
	}
    
    // processing limits group //

    Group gProcessing = new Group(gMainTab, SWT.NULL);
    Messages.setLanguageText(gProcessing, CFG_PREFIX + "tracker.processinglimits");
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gridData.horizontalSpan = 4;
    gProcessing.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 3;
    gProcessing.setLayout(layout);
    
    	// row annouce/scrape max process time
    
    label = new Label(gProcessing, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.maxgettime");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter maxGetTime = new IntParameter(gProcessing, "Tracker Max GET Time");
 
    gridData = new GridData();
    maxGetTime.setLayoutData( gridData );

    label = new Label(gProcessing, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.maxgettime.info");
   
  	// row post multiplier
    
    label = new Label(gProcessing, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.maxposttimemultiplier");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter maxPostTimeMultiplier = new IntParameter(gProcessing, "Tracker Max POST Time Multiplier");

    gridData = new GridData();
    maxPostTimeMultiplier.setLayoutData( gridData );

    label = new Label(gProcessing, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.maxposttimemultiplier.info");
   
   	// row max threads
    
    label = new Label(gProcessing, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.maxthreads");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter maxThreadsTime = new IntParameter(gProcessing, "Tracker Max Threads");
    maxThreadsTime.setMinimumValue(1);
    maxThreadsTime.setMaximumValue(4096);
    gridData = new GridData();
    maxThreadsTime.setLayoutData( gridData );

    label = new Label(gProcessing, SWT.NULL);
    
    
  	// non-blocking tracker group //
    
    Group gNBTracker = new Group(gMainTab, SWT.NULL);
    Messages.setLanguageText(gNBTracker, CFG_PREFIX + "tracker.nonblocking");
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gridData.horizontalSpan = 4;
    gNBTracker.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 3;
    gNBTracker.setLayout(layout);
    
    	// row

    gridData = new GridData();
    gridData.horizontalSpan = 3;
 
    BooleanParameter nb_enable =
    	new BooleanParameter(gNBTracker, "Tracker TCP NonBlocking",
                         CFG_PREFIX + "tracker.tcpnonblocking");
    nb_enable.setLayoutData(gridData);

 	// row max conc connections
    
    label = new Label(gNBTracker, SWT.NULL);
    Messages.setLanguageText(label, CFG_PREFIX + "tracker.nonblockingconcmax");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter maxConcConn = new IntParameter(gNBTracker, "Tracker TCP NonBlocking Conc Max" );
    gridData = new GridData();
    maxConcConn.setLayoutData( gridData );

    label = new Label(gNBTracker, SWT.NULL);
    
    nb_enable.setAdditionalActionPerformer(new ChangeSelectionActionPerformer( maxConcConn.getControls() ));
    
    }
  }
    
    return gMainTab;
  }
}
