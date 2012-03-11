/*
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.platform.PlatformManagerException;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.components.LinkLabel;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;

public class ConfigSectionConnectionAdvanced implements UISWTConfigSection {

	private final String CFG_PREFIX = "ConfigView.section.connection.advanced.";
	
	private final int REQUIRED_MODE = 2;
	
	public int maxUserMode() {
		return REQUIRED_MODE;
	}

	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_CONNECTION;
	}

	public String configSectionGetName() {
		return "connection.advanced";
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {
	}

	public Composite configSectionCreate(final Composite parent) {
		GridData gridData;

		Composite cSection = new Composite(parent, SWT.NULL);

		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL + GridData.VERTICAL_ALIGN_FILL);
		cSection.setLayoutData(gridData);
		GridLayout advanced_layout = new GridLayout();
		cSection.setLayout(advanced_layout);

		int userMode = COConfigurationManager.getIntParameter("User Mode");
		if (userMode < REQUIRED_MODE) {
			Label label = new Label(cSection, SWT.WRAP);
			gridData = new GridData();
			label.setLayoutData(gridData);

			final String[] modeKeys = { "ConfigView.section.mode.beginner",
					"ConfigView.section.mode.intermediate",
					"ConfigView.section.mode.advanced" };

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

			return cSection;
		}
		
		new LinkLabel(cSection, gridData, CFG_PREFIX
				+ "info.link", MessageText.getString(CFG_PREFIX + "url"));

		///////////////////////   ADVANCED SOCKET SETTINGS GROUP //////////
		
		Group gSocket = new Group(cSection, SWT.NULL);
		Messages.setLanguageText(gSocket, CFG_PREFIX + "socket.group");
		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.FILL_HORIZONTAL);
		gSocket.setLayoutData(gridData);
		GridLayout glayout = new GridLayout();
		glayout.numColumns = 2;
		gSocket.setLayout(glayout);

			// max simultaneous
		
		Label lmaxout = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lmaxout, "ConfigView.section.connection.network.max.simultaneous.connect.attempts");
		gridData = new GridData();
		lmaxout.setLayoutData( gridData );

		IntParameter max_connects = new IntParameter(gSocket,
				"network.max.simultaneous.connect.attempts", 1, 100);    
		gridData = new GridData();
		max_connects.setLayoutData(gridData);

			// // max pending
		
		Label lmaxpout = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lmaxpout, "ConfigView.section.connection.network.max.outstanding.connect.attempts");
		gridData = new GridData();
		lmaxpout.setLayoutData( gridData );

		IntParameter max_pending_connects = new IntParameter(gSocket,
				"network.tcp.max.connections.outstanding", 1, 65536 );    
		gridData = new GridData();
		max_pending_connects.setLayoutData(gridData);
		
		

			// bind ip
		
		Label lbind = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lbind, "ConfigView.label.bindip" );
		lbind.setLayoutData(new GridData());
		
		StringParameter bindip = new StringParameter(gSocket, "Bind IP", "", false);
		gridData = new GridData();
		gridData.widthHint = 100;
		bindip.setLayoutData(gridData);

		Text lbind2 = new Text(gSocket, SWT.READ_ONLY | SWT.MULTI);
		lbind2.setTabs(8);
		Messages.setLanguageText(
				lbind2,
				"ConfigView.label.bindip.details",
				new String[] {NetworkAdmin.getSingleton().getNetworkInterfacesAsString() });
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		lbind2.setLayoutData(gridData);


		Label lpbind = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lpbind, CFG_PREFIX + "bind_port");
		final IntParameter port_bind = new IntParameter(gSocket,
				"network.bind.local.port", 0, 65535);
		gridData = new GridData();
		port_bind.setLayoutData(gridData);
		
		
		Label lmtu = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lmtu, CFG_PREFIX + "mtu");
		final IntParameter mtu_size = new IntParameter(gSocket,"network.tcp.mtu.size");
		mtu_size.setMaximumValue(512 * 1024);
		gridData = new GridData();
		mtu_size.setLayoutData(gridData);


		Label lsend = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lsend, CFG_PREFIX + "SO_SNDBUF");
		final IntParameter SO_SNDBUF = new IntParameter(gSocket,	"network.tcp.socket.SO_SNDBUF");
		gridData = new GridData();
		SO_SNDBUF.setLayoutData(gridData);


		Label lreceiv = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lreceiv, CFG_PREFIX + "SO_RCVBUF");
		final IntParameter SO_RCVBUF = new IntParameter(gSocket,	"network.tcp.socket.SO_RCVBUF");
		gridData = new GridData();
		SO_RCVBUF.setLayoutData(gridData);
		

		Label ltos = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(ltos, CFG_PREFIX + "IPDiffServ");
		final StringParameter IPDiffServ = new StringParameter(gSocket,	"network.tcp.socket.IPDiffServ");
		gridData = new GridData();
		gridData.widthHint = 100;
		IPDiffServ.setLayoutData(gridData);


		//do simple input verification, and registry key setting for TOS field
		IPDiffServ.addChangeListener(new ParameterChangeAdapter() {

			final Color obg = IPDiffServ.getControl().getBackground();

			final Color ofg = IPDiffServ.getControl().getForeground();

			public void parameterChanged(Parameter p, boolean caused_internally) {
				String raw = IPDiffServ.getValue();
				int value = -1;

				try {
					value = Integer.decode(raw).intValue();
				} catch (Throwable t) {
				}

				if (value < 0 || value > 255) { //invalid or no value entered
					ConfigurationManager.getInstance().removeParameter(	"network.tcp.socket.IPDiffServ");

					if (raw != null && raw.length() > 0) { //error state
						IPDiffServ.getControl().setBackground(Colors.red);
						IPDiffServ.getControl().setForeground(Colors.white);
					} else { //no value state
						IPDiffServ.getControl().setBackground(obg);
						IPDiffServ.getControl().setForeground(ofg);
					}

					enableTOSRegistrySetting(false); //disable registry setting if necessary
				} else { //passes test
					IPDiffServ.getControl().setBackground(obg);
					IPDiffServ.getControl().setForeground(ofg);

					enableTOSRegistrySetting(true); //enable registry setting if necessary
				}
			}
		});
		
			// read select
		
		Label lreadsel = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lreadsel, CFG_PREFIX + "read_select", new String[]{ String.valueOf( COConfigurationManager.getDefault("network.tcp.read.select.time"))});
		final IntParameter read_select = new IntParameter(gSocket,	"network.tcp.read.select.time", 10, 250);
		gridData = new GridData();
		read_select.setLayoutData(gridData);
		
		Label lreadselmin = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lreadselmin, CFG_PREFIX + "read_select_min", new String[]{ String.valueOf( COConfigurationManager.getDefault("network.tcp.read.select.min.time"))});
		final IntParameter read_select_min = new IntParameter(gSocket,	"network.tcp.read.select.min.time", 0, 100 );
		gridData = new GridData();
		read_select_min.setLayoutData(gridData);

			// write select
				
		Label lwritesel = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lwritesel, CFG_PREFIX + "write_select", new String[]{ String.valueOf( COConfigurationManager.getDefault("network.tcp.write.select.time"))});
		final IntParameter write_select = new IntParameter(gSocket,	"network.tcp.write.select.time", 10, 250);
		gridData = new GridData();
		write_select.setLayoutData(gridData);
		
		Label lwriteselmin = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lwriteselmin, CFG_PREFIX + "write_select_min", new String[]{ String.valueOf( COConfigurationManager.getDefault("network.tcp.write.select.min.time"))});
		final IntParameter write_select_min = new IntParameter(gSocket,	"network.tcp.write.select.min.time", 0, 100 );
		gridData = new GridData();
		write_select_min.setLayoutData(gridData);

		new BooleanParameter( cSection, "IPV6 Enable Support", "network.ipv6.enable.support"  );

		
		new BooleanParameter( cSection, "IPV6 Prefer Addresses", "network.ipv6.prefer.addresses"  );
		
		new BooleanParameter(cSection, "Enforce Bind IP","network.enforce.ipbinding");
		
		//////////////////////////////////////////////////////////////////////////

		return cSection;

	}

	private void enableTOSRegistrySetting(boolean enable) {
		PlatformManager mgr = PlatformManagerFactory.getPlatformManager();

		if (mgr.hasCapability(PlatformManagerCapabilities.SetTCPTOSEnabled)) { 
			//see http://wiki.vuze.com/w/AdvancedNetworkSettings
			try {
				mgr.setTCPTOSEnabled(enable);
			} catch (PlatformManagerException pe) {
				Debug.printStackTrace(pe);
			}
		}
	}

}
