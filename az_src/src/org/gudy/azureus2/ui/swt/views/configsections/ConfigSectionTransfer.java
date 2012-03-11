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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.components.LinkLabel;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.internat.MessageText;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

public class ConfigSectionTransfer implements UISWTConfigSection {
	
	public ConfigSectionTransfer() {
	}
	
	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_ROOT;
	}

	public String configSectionGetName() {
		return ConfigSection.SECTION_TRANSFER;
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {
		ImageLoader imageLoader = ImageLoader.getInstance();
		imageLoader.releaseImage("subitem");
	}
	
	public int maxUserMode() {
		return 2;
	}


	public Composite configSectionCreate(final Composite parent) {
		GridData gridData;
		GridLayout layout;
		Label label;

		Composite cSection = new Composite(parent, SWT.NULL);
		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
				| GridData.HORIZONTAL_ALIGN_FILL);
		cSection.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		cSection.setLayout(layout);

		int userMode = COConfigurationManager.getIntParameter("User Mode");

		//  store the initial d/l speed so we can do something sensible later
		final int[] manual_max_download_speed = { COConfigurationManager
				.getIntParameter("Max Download Speed KBs") };

		//  max upload speed
		gridData = new GridData();
		label = new Label(cSection, SWT.NULL);
		label.setLayoutData(gridData);
		Messages.setLanguageText(label, "ConfigView.label.maxuploadspeed");

		gridData = new GridData();
		final IntParameter paramMaxUploadSpeed = new IntParameter(cSection,
				"Max Upload Speed KBs", 0, -1);
		paramMaxUploadSpeed.setLayoutData(gridData);

		//  max upload speed when seeding
		final Composite cMaxUploadSpeedOptionsArea = new Composite(cSection, SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		cMaxUploadSpeedOptionsArea.setLayout(layout);
		gridData = new GridData();
		gridData.horizontalIndent = 15;
		gridData.horizontalSpan = 2;
		cMaxUploadSpeedOptionsArea.setLayoutData(gridData);

		ImageLoader imageLoader = ImageLoader.getInstance();
		Image img = imageLoader.getImage("subitem");

		label = new Label(cMaxUploadSpeedOptionsArea, SWT.NULL);
		img.setBackground(label.getBackground());
		gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gridData);
		label.setImage(img);

		gridData = new GridData();
		final BooleanParameter enable_seeding_rate = new BooleanParameter(
				cMaxUploadSpeedOptionsArea, "enable.seedingonly.upload.rate",
				"ConfigView.label.maxuploadspeedseeding");
		enable_seeding_rate.setLayoutData(gridData);

		gridData = new GridData();
		final IntParameter paramMaxUploadSpeedSeeding = new IntParameter(
				cMaxUploadSpeedOptionsArea, "Max Upload Speed Seeding KBs", 0, -1);
		paramMaxUploadSpeedSeeding.setLayoutData(gridData);
		enable_seeding_rate
				.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
						paramMaxUploadSpeedSeeding.getControl()));

		if (userMode < 2) {
			// wiki link

			Composite cWiki = new Composite(cSection, SWT.COLOR_GRAY);
			gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
					| GridData.HORIZONTAL_ALIGN_FILL);
			gridData.horizontalSpan = 2;
			cWiki.setLayoutData(gridData);
			layout = new GridLayout();
			layout.numColumns = 4;
			layout.marginHeight = 0;
			cWiki.setLayout(layout);

			gridData = new GridData();
			gridData.horizontalIndent = 6;
			gridData.horizontalSpan = 2;
			label = new Label(cWiki, SWT.NULL);
			label.setLayoutData(gridData);
			label.setText(MessageText.getString("Utils.link.visit") + ":");

			gridData = new GridData();
			gridData.horizontalIndent = 10;
			gridData.horizontalSpan = 2;
			new LinkLabel(cWiki, gridData, "ConfigView.section.transfer.speeds.wiki",
					"http://wiki.vuze.com/w/Good_settings");
		}

		if ( userMode > 1 ){
		
			gridData = new GridData();
			label = new Label(cSection, SWT.NULL);
			label.setLayoutData(gridData);
			Messages.setLanguageText(label, "ConfigView.label.maxuploadswhenbusymin" );

			gridData = new GridData();
			new IntParameter(cSection, "max.uploads.when.busy.inc.min.secs", 0, -1).setLayoutData(gridData);
		}
		
		// max download speed
		gridData = new GridData();
		label = new Label(cSection, SWT.NULL);
		label.setLayoutData(gridData);
		Messages.setLanguageText(label, "ConfigView.label.maxdownloadspeed");
		
		gridData = new GridData();
		final IntParameter paramMaxDownSpeed = new IntParameter(cSection,
				"Max Download Speed KBs", 0, -1);
		paramMaxDownSpeed.setLayoutData(gridData);
				
			// max upload/download limit dependencies
		
		Listener l = new Listener() {
	
			public void handleEvent(Event event) {
				boolean disableAuto = false;
				boolean disableAutoSeeding = false;
				
				if(enable_seeding_rate.isSelected())
				{
					disableAutoSeeding = event.widget == paramMaxUploadSpeedSeeding.getControl();
					disableAuto = event.widget == paramMaxDownSpeed.getControl() || event.widget == paramMaxUploadSpeed.getControl();
				} else
				{
					disableAuto = true;
					disableAutoSeeding = true;
				}
					
					
				if(disableAuto)
					COConfigurationManager.setParameter(TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY, false);
				if(disableAutoSeeding)
					COConfigurationManager.setParameter(TransferSpeedValidator.AUTO_UPLOAD_SEEDING_ENABLED_CONFIGKEY, false);
			}
		};
		
		paramMaxDownSpeed.getControl().addListener(SWT.Selection, l);
		paramMaxUploadSpeed.getControl().addListener(SWT.Selection, l);
		paramMaxUploadSpeedSeeding.getControl().addListener(SWT.Selection, l);
		
		
		paramMaxUploadSpeed.addChangeListener(new ParameterChangeAdapter() {
			ParameterChangeAdapter me = this;

			public void parameterChanged(Parameter p, boolean internal) {
				CoreWaiterSWT.waitForCoreRunning(new AzureusCoreRunningListener() {

					public void azureusCoreRunning(AzureusCore core) {
						if (paramMaxUploadSpeed.isDisposed()) {
							paramMaxUploadSpeed.removeChangeListener(me);
							return;
						}

						// we don't want to police these limits when auto-speed is running as
						// they screw things up bigtime

						if (TransferSpeedValidator.isAutoSpeedActive(core.getGlobalManager())) {

							return;
						}

						int up_val = paramMaxUploadSpeed.getValue();
						int down_val = paramMaxDownSpeed.getValue();

						if (up_val != 0
								&& up_val < COConfigurationManager.CONFIG_DEFAULT_MIN_MAX_UPLOAD_SPEED) {

							if ((down_val == 0) || down_val > (up_val * 2)) {

								paramMaxDownSpeed.setValue(up_val * 2);
							}
						} else {

							if (down_val != manual_max_download_speed[0]) {

								paramMaxDownSpeed.setValue(manual_max_download_speed[0]);
							}
						}
					}

				});
			};
		});

		paramMaxDownSpeed.addChangeListener(new ParameterChangeAdapter() {
			ParameterChangeAdapter me = this;

			public void parameterChanged(Parameter p, boolean internal) {
				CoreWaiterSWT.waitForCoreRunning(new AzureusCoreRunningListener() {

					public void azureusCoreRunning(AzureusCore core) {
						if (paramMaxDownSpeed.isDisposed()) {
							paramMaxDownSpeed.removeChangeListener(me);
							return;
						}

						// we don't want to police these limits when auto-speed is running as
						// they screw things up bigtime

						if (TransferSpeedValidator.isAutoSpeedActive(core.getGlobalManager())) {

							return;
						}

						int up_val = paramMaxUploadSpeed.getValue();
						int down_val = paramMaxDownSpeed.getValue();

						manual_max_download_speed[0] = down_val;

						if (up_val < COConfigurationManager.CONFIG_DEFAULT_MIN_MAX_UPLOAD_SPEED) {

							if (up_val != 0 && up_val < (down_val * 2)) {

								paramMaxUploadSpeed.setValue((down_val + 1) / 2);

							} else if (down_val == 0) {

								paramMaxUploadSpeed.setValue(0);
							}
						}
					}
				});
			}
		});
		
		if (userMode > 0) {
			
				// bias upload to incomplete
			
			BooleanParameter bias_upload = new BooleanParameter(
					cSection, 
					"Bias Upload Enable",
					"ConfigView.label.xfer.bias_up" );
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			bias_upload.setLayoutData(gridData);
			

			final Composite bias_slack_area = new Composite(cSection, SWT.NULL);
			layout = new GridLayout();
			layout.numColumns = 3;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			bias_slack_area.setLayout(layout);
			gridData = new GridData();
			gridData.horizontalIndent = 15;
			gridData.horizontalSpan = 2;
			bias_slack_area.setLayoutData(gridData);

			label = new Label(bias_slack_area, SWT.NULL);
			gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			label.setLayoutData(gridData);
			label.setImage(img);
			
			label = new Label(bias_slack_area, SWT.NULL);
			Messages.setLanguageText(label, "ConfigView.label.xfer.bias_slack");

			IntParameter bias_slack = new IntParameter(
					bias_slack_area, "Bias Upload Slack KBs", 1, -1);
			
			
			final Composite bias_unlimited_area = new Composite(cSection, SWT.NULL);
			layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			bias_unlimited_area.setLayout(layout);
			gridData = new GridData();
			gridData.horizontalIndent = 15;
			gridData.horizontalSpan = 2;
			bias_unlimited_area.setLayoutData(gridData);

			label = new Label(bias_unlimited_area, SWT.NULL);
			gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			label.setLayoutData(gridData);
			label.setImage(img);

			
			BooleanParameter bias_no_limit = new BooleanParameter(
					bias_unlimited_area, 
					"Bias Upload Handle No Limit",
					"ConfigView.label.xfer.bias_no_limit" );
						
			bias_upload.setAdditionalActionPerformer(
					new ChangeSelectionActionPerformer(
					new Parameter[]{ bias_slack, bias_no_limit} ));
		}

		if (userMode > 0) {
			
				// AUTO GROUP
			
			Group auto_group = new Group(cSection, SWT.NULL);
			
			Messages.setLanguageText(auto_group, "group.auto");
			
			GridLayout auto_layout = new GridLayout();
			
			auto_layout.numColumns = 2;

			auto_group.setLayout(auto_layout);

			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan = 2;
			auto_group.setLayoutData(gridData);

			BooleanParameter auto_adjust = new BooleanParameter(
					auto_group, 
					"Auto Adjust Transfer Defaults",
					"ConfigView.label.autoadjust" );
			
			gridData = new GridData();
			gridData.horizontalSpan = 2;

			auto_adjust.setLayoutData( gridData );

			// max uploads
			gridData = new GridData();
			label = new Label(auto_group, SWT.NULL);
			label.setLayoutData(gridData);
			Messages.setLanguageText(label, "ConfigView.label.maxuploads");

			gridData = new GridData();
			IntParameter paramMaxUploads = new IntParameter(auto_group, "Max Uploads",
					2, -1);
			paramMaxUploads.setLayoutData(gridData);

				// max uploads when seeding
			
			final Composite cMaxUploadsOptionsArea = new Composite(auto_group, SWT.NULL);
			layout = new GridLayout();
			layout.numColumns = 3;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			cMaxUploadsOptionsArea.setLayout(layout);
			gridData = new GridData();
			gridData.horizontalIndent = 15;
			gridData.horizontalSpan = 2;
			cMaxUploadsOptionsArea.setLayoutData(gridData);
			label = new Label(cMaxUploadsOptionsArea, SWT.NULL);
			img.setBackground(label.getBackground());
			gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			label.setLayoutData(gridData);
			label.setImage(img);
			
			gridData = new GridData();
			BooleanParameter enable_seeding_uploads = new BooleanParameter(
					cMaxUploadsOptionsArea, "enable.seedingonly.maxuploads",
					"ConfigView.label.maxuploadsseeding");
			enable_seeding_uploads.setLayoutData(gridData);

			gridData = new GridData();
			final IntParameter paramMaxUploadsSeeding = new IntParameter(
					cMaxUploadsOptionsArea, "Max Uploads Seeding", 2, -1);
			paramMaxUploadsSeeding.setLayoutData(gridData);

			
			
			////

			gridData = new GridData();
			label = new Label(auto_group, SWT.NULL);
			label.setLayoutData(gridData);
			Messages.setLanguageText(label, "ConfigView.label.max_peers_per_torrent");

			gridData = new GridData();
			IntParameter paramMaxClients = new IntParameter(auto_group,
					"Max.Peer.Connections.Per.Torrent");
			paramMaxClients.setLayoutData(gridData);

			
			/////
			
				// max peers when seeding
			
			final Composite cMaxPeersOptionsArea = new Composite(auto_group, SWT.NULL);
			layout = new GridLayout();
			layout.numColumns = 3;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			cMaxPeersOptionsArea.setLayout(layout);
			gridData = new GridData();
			gridData.horizontalIndent = 15;
			gridData.horizontalSpan = 2;
			cMaxPeersOptionsArea.setLayoutData(gridData);
			label = new Label(cMaxPeersOptionsArea, SWT.NULL);
			img.setBackground(label.getBackground());
			gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			label.setLayoutData(gridData);
			label.setImage(img);
			
			gridData = new GridData();
			BooleanParameter enable_max_peers_seeding = new BooleanParameter(
					cMaxPeersOptionsArea, "Max.Peer.Connections.Per.Torrent.When.Seeding.Enable",
					"ConfigView.label.maxuploadsseeding");
			enable_max_peers_seeding.setLayoutData(gridData);

			gridData = new GridData();
			final IntParameter paramMaxPeersSeeding = new IntParameter(
					cMaxPeersOptionsArea, "Max.Peer.Connections.Per.Torrent.When.Seeding", 0, -1);
			paramMaxPeersSeeding.setLayoutData(gridData);	
			
			/////

			gridData = new GridData();
			label = new Label(auto_group, SWT.NULL);
			label.setLayoutData(gridData);
			Messages.setLanguageText(label, "ConfigView.label.max_peers_total");

			gridData = new GridData();
			IntParameter paramMaxClientsTotal = new IntParameter(auto_group,
					"Max.Peer.Connections.Total");
			paramMaxClientsTotal.setLayoutData(gridData);
			
			gridData = new GridData();
			label = new Label(auto_group, SWT.NULL);
			label.setLayoutData(gridData);
			Messages.setLanguageText(label, "ConfigView.label.maxseedspertorrent");

			gridData = new GridData();
			IntParameter max_seeds_per_torrent = new IntParameter(auto_group,"Max Seeds Per Torrent");
			max_seeds_per_torrent.setLayoutData(gridData);

			final Parameter[] parameters = {
					paramMaxUploads, enable_seeding_uploads, paramMaxUploadsSeeding,
					paramMaxClients, enable_max_peers_seeding, paramMaxPeersSeeding,
					paramMaxClientsTotal, max_seeds_per_torrent,
			};
			
		    IAdditionalActionPerformer f_enabler =
		        new GenericActionPerformer( new Control[0])
		    	{
		        	public void 
		        	performAction()
		        	{
		        		boolean auto = COConfigurationManager.getBooleanParameter( "Auto Adjust Transfer Defaults" );
		        		
		        		for ( Parameter p: parameters ){
		        			
		        			Control[] c = p.getControls();
		        			
		        			for ( Control x: c ){
		        				
		        				x.setEnabled( !auto );
		        			}
		        		}
		        			
		        		if ( !auto ){
		        			
		        			paramMaxUploadsSeeding.getControl().setEnabled( COConfigurationManager.getBooleanParameter( "enable.seedingonly.maxuploads" ));
		        			
		        			paramMaxPeersSeeding.getControl().setEnabled(  COConfigurationManager.getBooleanParameter( "Max.Peer.Connections.Per.Torrent.When.Seeding.Enable" ));
		        		}
		        	}
		        };
			
		    f_enabler.performAction();
		    
			enable_seeding_uploads.setAdditionalActionPerformer( f_enabler );
			enable_max_peers_seeding.setAdditionalActionPerformer( f_enabler );
			auto_adjust.setAdditionalActionPerformer( f_enabler );
			
				// END AUTO GROUP
			
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			BooleanParameter useReqLimiting = new BooleanParameter(cSection, "Use Request Limiting",
				"ConfigView.label.userequestlimiting");
			useReqLimiting.setLayoutData(gridData);

			gridData = new GridData();
			gridData.horizontalSpan = 2;
			BooleanParameter useReqLimitingPrios = new BooleanParameter(cSection, "Use Request Limiting Priorities",
				"ConfigView.label.userequestlimitingpriorities");
			useReqLimitingPrios.setLayoutData(gridData);
			useReqLimiting
			.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
					useReqLimitingPrios.getControl()));
			
			

			gridData = new GridData();
			gridData.horizontalSpan = 2;
			BooleanParameter allowSameIP = new BooleanParameter(cSection,
					"Allow Same IP Peers", "ConfigView.label.allowsameip");
			allowSameIP.setLayoutData(gridData);

			// lazy bit field
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			BooleanParameter lazybf = new BooleanParameter(cSection,
					"Use Lazy Bitfield", "ConfigView.label.lazybitfield");
			lazybf.setLayoutData(gridData);

			// prioritise first/last pieces
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			BooleanParameter firstPiece = new BooleanParameter(cSection,
					"Prioritize First Piece",
					"ConfigView.label.prioritizefirstpiece");
			firstPiece.setLayoutData(gridData);

			// Further prioritize High priority files according to % complete and size of file
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			BooleanParameter mostCompletedFiles = new BooleanParameter(cSection,
					"Prioritize Most Completed Files",
					"ConfigView.label.prioritizemostcompletedfiles");
			mostCompletedFiles.setLayoutData(gridData);

			// ignore ports

			Composite cMiniArea = new Composite(cSection, SWT.NULL);
			layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			cMiniArea.setLayout(layout);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan = 2;
			cMiniArea.setLayoutData(gridData);

			gridData = new GridData();
			label = new Label(cMiniArea, SWT.NULL);
			label.setLayoutData(gridData);
			Messages.setLanguageText(label,
					"ConfigView.label.transfer.ignorepeerports");

			gridData = new GridData();
			gridData.widthHint = 125;
			StringParameter ignore_ports = new StringParameter(cMiniArea,
					"Ignore.peer.ports", "0");
			ignore_ports.setLayoutData(gridData);
		}
		
		return cSection;
	}
}
