/*
 * File    : ConfigPanelServer.java
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.components.LinkLabel;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;

public class ConfigSectionConnection implements UISWTConfigSection {
	
	private static final String CFG_PREFIX = "ConfigView.section.connection.";
		
	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_ROOT;
	}

	public String configSectionGetName() {
		return ConfigSection.SECTION_CONNECTION;
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {
	}
	
	public int maxUserMode() {
		return 2;
	}


	public Composite configSectionCreate(final Composite parent) {
		Label label;
		GridData gridData;
		GridLayout layout;
		Composite cMiniArea;

		int userMode = COConfigurationManager.getIntParameter("User Mode");

		Composite cSection = new Composite(parent, SWT.NULL);
		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
				| GridData.HORIZONTAL_ALIGN_FILL);
		cSection.setLayoutData(gridData);
		layout = new GridLayout();
		cSection.setLayout(layout);

		///////////////////////

		cMiniArea = new Composite(cSection, SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		cMiniArea.setLayout(layout);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		cMiniArea.setLayoutData(gridData);
		
		final boolean	separate_ports = userMode > 1 || COConfigurationManager.getIntParameter( "TCP.Listen.Port" ) != COConfigurationManager.getIntParameter( "UDP.Listen.Port" );

		label = new Label(cMiniArea, SWT.NULL);
		Messages.setLanguageText(label, separate_ports?"ConfigView.label.tcplistenport":"ConfigView.label.serverport");
		gridData = new GridData();
		label.setLayoutData(gridData);

		final IntParameter tcplisten = new IntParameter(cMiniArea,
				"TCP.Listen.Port", 1, 65535);
		gridData = new GridData();
		tcplisten.setLayoutData(gridData);

		tcplisten.addChangeListener(new ParameterChangeAdapter() {
			public void intParameterChanging(Parameter p, int toValue) {
				if (toValue == 6880) {
					toValue = 6881;
					tcplisten.setValue(toValue);
				}

				if (!separate_ports) {
					COConfigurationManager.setParameter("UDP.Listen.Port", toValue);
					COConfigurationManager.setParameter("UDP.NonData.Listen.Port",
							toValue);
				}
			}
		});
		
		if ( separate_ports ){
			
			label = new Label(cMiniArea, SWT.NULL);
			Messages.setLanguageText(label, "ConfigView.label.udplistenport");
			gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
			label.setLayoutData(gridData);

			final IntParameter udp_listen = new IntParameter(cMiniArea,
					"UDP.Listen.Port", 1, 65535);
			gridData = new GridData();
			udp_listen.setLayoutData(gridData);

			final boolean MULTI_UDP = COConfigurationManager.ENABLE_MULTIPLE_UDP_PORTS && userMode > 1;
			
			udp_listen.addChangeListener(new ParameterChangeAdapter() {
				public void intParameterChanging(Parameter p, int toValue) {
					if (toValue == 6880) {
						toValue = 6881;
						udp_listen.setValue(toValue);
					}

					if (!MULTI_UDP) {
						COConfigurationManager.setParameter("UDP.NonData.Listen.Port",
								toValue);
					}
				}
			});
		
			if ( MULTI_UDP ){
			
				Composite cNonDataUDPArea = new Composite(cSection, SWT.NULL);
				layout = new GridLayout();
				layout.numColumns = 2;
				layout.marginHeight = 0;
				layout.marginWidth = 0;
				cNonDataUDPArea.setLayout(layout);
				gridData = new GridData(GridData.FILL_HORIZONTAL);
				cNonDataUDPArea.setLayoutData(gridData);
				
				final BooleanParameter commonUDP = 
					new BooleanParameter(cNonDataUDPArea, "UDP.NonData.Listen.Port.Same",	CFG_PREFIX + "nondata.udp.same");
				gridData = new GridData();
				gridData.horizontalIndent = 16;
				commonUDP.setLayoutData( gridData );
				
				final IntParameter non_data_udp_listen = new IntParameter(
						cNonDataUDPArea, "UDP.NonData.Listen.Port");
	
				non_data_udp_listen.addChangeListener(
					new ParameterChangeAdapter() 
					{
						public void intParameterChanging(Parameter p, int toValue) {
							if (toValue == 6880) {
								toValue = 6881;
								non_data_udp_listen.setValue(toValue);
							}
						}
					});
				
				udp_listen.addChangeListener(
						new ParameterChangeAdapter() 
						{
							public void parameterChanged(Parameter p, boolean caused_internally)
							{
								if ( commonUDP.isSelected()){
									
									int udp_listen_port = udp_listen.getValue();
			
									if ( udp_listen_port != 6880 ){
										
										COConfigurationManager.setParameter( "UDP.NonData.Listen.Port", udp_listen_port );
										
										non_data_udp_listen.setValue( udp_listen_port );
									}
								}
							}
						});
				
				gridData = new GridData();
				non_data_udp_listen.setLayoutData( gridData );
	
				commonUDP.setAdditionalActionPerformer(new ChangeSelectionActionPerformer( non_data_udp_listen.getControls(), true ));
				
				commonUDP.addChangeListener(
					new ParameterChangeAdapter() 
					{
						public void 
						parameterChanged(
							Parameter p, 
							boolean caused_internally) 
						{
							if ( commonUDP.isSelected()){
								
								int	udp_listen_port = COConfigurationManager.getIntParameter("UDP.Listen.Port");
								
								if ( COConfigurationManager.getIntParameter("UDP.NonData.Listen.Port") != udp_listen_port ){
									
									COConfigurationManager.setParameter( "UDP.NonData.Listen.Port", udp_listen_port );
									
									non_data_udp_listen.setValue( udp_listen_port );
								}
							}
						}
					});
				
				final BooleanParameter enable_tcp = 
					new BooleanParameter(cNonDataUDPArea, "TCP.Listen.Port.Enable",	CFG_PREFIX + "tcp.enable");
				gridData = new GridData();
				enable_tcp.setLayoutData( gridData );
				label = new Label(cNonDataUDPArea, SWT.NULL);
				
				final BooleanParameter enable_udp = 
					new BooleanParameter(cNonDataUDPArea, "UDP.Listen.Port.Enable",	CFG_PREFIX + "udp.enable");
				gridData = new GridData();
				enable_udp.setLayoutData( gridData );
				label = new Label(cNonDataUDPArea, SWT.NULL);
				
				enable_tcp.setAdditionalActionPerformer(
						new ChangeSelectionActionPerformer( tcplisten ));
				
				enable_udp.setAdditionalActionPerformer(
						new ChangeSelectionActionPerformer( udp_listen ));
			}
		}
		
		if ( userMode > 1 ){
		
			final BooleanParameter prefer_udp = 
				new BooleanParameter(cMiniArea, "peercontrol.prefer.udp",	CFG_PREFIX + "prefer.udp");
			gridData = new GridData();
			prefer_udp.setLayoutData( gridData );
			label = new Label(cMiniArea, SWT.NULL);
		}
		
		if (userMode < 2) {
			// wiki link
			label = new Label(cSection, SWT.NULL);
			gridData = new GridData();
			label.setLayoutData(gridData);
			label.setText(MessageText.getString("Utils.link.visit") + ":");

			final Label linkLabel = new Label(cSection, SWT.NULL);
			linkLabel.setText(MessageText
					.getString(CFG_PREFIX + "serverport.wiki"));
			linkLabel
					.setData("http://wiki.vuze.com/w/Why_ports_like_6881_are_no_good_choice");
			linkLabel.setCursor(linkLabel.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
			linkLabel.setForeground(Colors.blue);
			gridData = new GridData();
			linkLabel.setLayoutData(gridData);
			linkLabel.addMouseListener(new MouseAdapter() {
				public void mouseDoubleClick(MouseEvent arg0) {
					Utils.launch((String) ((Label) arg0.widget).getData());
				}

				public void mouseDown(MouseEvent arg0) {
					Utils.launch((String) ((Label) arg0.widget).getData());
				}
			});
		}

		if (userMode > 0) {
			/////////////////////// HTTP ///////////////////

			Group http_group = new Group(cSection, SWT.NULL);
			
			Messages.setLanguageText(http_group,CFG_PREFIX + "group.http");
			
			GridLayout http_layout = new GridLayout();
			
			http_layout.numColumns = 2;

			http_group.setLayout(http_layout);

			gridData = new GridData(GridData.FILL_HORIZONTAL);
			http_group.setLayoutData(gridData);

			label = new Label(http_group, SWT.WRAP);
			Messages.setLanguageText(label, CFG_PREFIX + "group.http.info");

			new LinkLabel(
					http_group, 
					"ConfigView.label.please.visit.here",
					"http://wiki.vuze.com/w/HTTP_Seeding");

			final BooleanParameter enable_http = 
				new BooleanParameter(http_group, "HTTP.Data.Listen.Port.Enable", CFG_PREFIX + "http.enable");
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			enable_http.setLayoutData( gridData );

			label = new Label(http_group, SWT.NULL);
			Messages.setLanguageText(label, CFG_PREFIX + "http.port" );

			IntParameter http_port = new IntParameter(http_group, "HTTP.Data.Listen.Port");

			gridData = new GridData();
			http_port.setLayoutData( gridData );

			label = new Label(http_group, SWT.NULL);
			Messages.setLanguageText(label, CFG_PREFIX + "http.portoverride" );

			IntParameter http_port_override = new IntParameter(http_group, "HTTP.Data.Listen.Port.Override");

			gridData = new GridData();
			http_port_override.setLayoutData( gridData );

			enable_http.setAdditionalActionPerformer( new ChangeSelectionActionPerformer( http_port ));
			enable_http.setAdditionalActionPerformer( new ChangeSelectionActionPerformer( http_port_override ));
		}
		
		if (userMode > 0) {
			/////////////////////// PEER SOURCES GROUP ///////////////////

			Group peer_sources_group = new Group(cSection, SWT.NULL);
			Messages.setLanguageText(peer_sources_group,
					CFG_PREFIX + "group.peersources");
			GridLayout peer_sources_layout = new GridLayout();
			peer_sources_group.setLayout(peer_sources_layout);

			gridData = new GridData(GridData.FILL_HORIZONTAL);
			peer_sources_group.setLayoutData(gridData);

			label = new Label(peer_sources_group, SWT.WRAP);
			Messages.setLanguageText(label,
					CFG_PREFIX + "group.peersources.info");
			gridData = new GridData();
			label.setLayoutData(gridData);

			for (int i = 0; i < PEPeerSource.PS_SOURCES.length; i++) {

				String p = PEPeerSource.PS_SOURCES[i];

				String config_name = "Peer Source Selection Default." + p;
				String msg_text = CFG_PREFIX + "peersource." + p;

				BooleanParameter peer_source = new BooleanParameter(peer_sources_group,
						config_name, msg_text);

				gridData = new GridData();
				peer_source.setLayoutData(gridData);
			}


			//////////////////////

			if (userMode > 1) {

				/////////////////////// NETWORKS GROUP ///////////////////

				Group networks_group = new Group(cSection, SWT.NULL);
				Messages.setLanguageText(networks_group,
						CFG_PREFIX + "group.networks");
				GridLayout networks_layout = new GridLayout();
				networks_group.setLayout(networks_layout);

				gridData = new GridData(GridData.FILL_HORIZONTAL);
				networks_group.setLayoutData(gridData);

				label = new Label(networks_group, SWT.NULL);
				Messages.setLanguageText(label,
						CFG_PREFIX + "group.networks.info");
				gridData = new GridData();
				label.setLayoutData(gridData);

				for (int i = 0; i < AENetworkClassifier.AT_NETWORKS.length; i++) {

					String nn = AENetworkClassifier.AT_NETWORKS[i];

					String config_name = "Network Selection Default." + nn;
					String msg_text = CFG_PREFIX + "networks." + nn;

					BooleanParameter network = new BooleanParameter(networks_group,
							config_name, msg_text);

					gridData = new GridData();
					network.setLayoutData(gridData);
				}

				label = new Label(networks_group, SWT.NULL);
				gridData = new GridData();
				label.setLayoutData(gridData);

				BooleanParameter network_prompt = new BooleanParameter(networks_group,
						"Network Selection Prompt",
						CFG_PREFIX + "networks.prompt");

				gridData = new GridData();
				network_prompt.setLayoutData(gridData);

			} // end userMode>1
		} // end userMode>0

		///////////////////////   

		return cSection;

	}
}
