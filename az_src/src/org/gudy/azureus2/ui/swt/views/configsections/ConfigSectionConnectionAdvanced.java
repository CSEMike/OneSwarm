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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
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
import org.gudy.azureus2.ui.swt.Utils;
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

		
		Label lmaxout = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lmaxout, "ConfigView.section.connection.network.max.simultaneous.connect.attempts");
		gridData = new GridData();
		lmaxout.setLayoutData( gridData );

		IntParameter max_connects = new IntParameter(gSocket,
				"network.max.simultaneous.connect.attempts", 1, 100);    
		gridData = new GridData();
		gridData.widthHint = 30;
		max_connects.setLayoutData(gridData);


		Label lbind = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lbind, "ConfigView.label.bindip" );
		lbind.setLayoutData(new GridData());
		
		StringParameter bindip = new StringParameter(gSocket, "Bind IP", "", false);
		gridData = new GridData();
		gridData.widthHint = 100;
		bindip.setLayoutData(gridData);

		Label lbind2 = new Label(gSocket, SWT.WRAP);
		Messages.setLanguageText(
				lbind2,
				"ConfigView.label.bindip.details",
				new String[] {NetworkAdmin.getSingleton().getNetworkInterfacesAsString() });
		lbind2.setLayoutData(Utils.getWrappableLabelGridData(2, 0));

		Label lpbind = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lpbind, CFG_PREFIX + "bind_port");
		final IntParameter port_bind = new IntParameter(gSocket,
				"network.bind.local.port", 0, 65535);
		gridData = new GridData();
		gridData.widthHint = 40;
		port_bind.setLayoutData(gridData);
		
		
		Label lmtu = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lmtu, CFG_PREFIX + "mtu");
		final IntParameter mtu_size = new IntParameter(gSocket,"network.tcp.mtu.size");
		mtu_size.setMaximumValue(512 * 1024);
		gridData = new GridData();
		gridData.widthHint = 40;
		mtu_size.setLayoutData(gridData);


		Label lsend = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lsend, CFG_PREFIX + "SO_SNDBUF");
		final IntParameter SO_SNDBUF = new IntParameter(gSocket,	"network.tcp.socket.SO_SNDBUF");
		gridData = new GridData();
		gridData.widthHint = 40;
		SO_SNDBUF.setLayoutData(gridData);


		Label lreceiv = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(lreceiv, CFG_PREFIX + "SO_RCVBUF");
		final IntParameter SO_RCVBUF = new IntParameter(gSocket,	"network.tcp.socket.SO_RCVBUF");
		gridData = new GridData();
		gridData.widthHint = 40;
		SO_RCVBUF.setLayoutData(gridData);
		

		Label ltos = new Label(gSocket, SWT.NULL);
		Messages.setLanguageText(ltos, CFG_PREFIX + "IPTOS");
		final StringParameter IPTOS = new StringParameter(gSocket,	"network.tcp.socket.IPTOS");
		gridData = new GridData();
		gridData.widthHint = 30;
		IPTOS.setLayoutData(gridData);


		//do simple input verification, and registry key setting for TOS field
		IPTOS.addChangeListener(new ParameterChangeAdapter() {

			final Color obg = IPTOS.getControl().getBackground();

			final Color ofg = IPTOS.getControl().getForeground();

			public void parameterChanged(Parameter p, boolean caused_internally) {
				String raw = IPTOS.getValue();
				int value = -1;

				try {
					value = Integer.decode(raw).intValue();
				} catch (Throwable t) {
				}

				if (value < 0 || value > 255) { //invalid or no value entered
					ConfigurationManager.getInstance().removeParameter(	"network.tcp.socket.IPTOS");

					if (raw != null && raw.length() > 0) { //error state
						IPTOS.getControl().setBackground(Colors.red);
						IPTOS.getControl().setForeground(Colors.white);
					} else { //no value state
						IPTOS.getControl().setBackground(obg);
						IPTOS.getControl().setForeground(ofg);
					}

					enableTOSRegistrySetting(false); //disable registry setting if necessary
				} else { //passes test
					IPTOS.getControl().setBackground(obg);
					IPTOS.getControl().setForeground(ofg);

					enableTOSRegistrySetting(true); //enable registry setting if necessary
				}
			}
		});
		
		new BooleanParameter( cSection, "IPV6 Prefer Addresses", "network.ipv6.prefer.addresses"  );
		
		new BooleanParameter(cSection, "Enforce Bind IP","network.enforce.ipbinding");
		
		//////////////////////////////////////////////////////////////////////////

		return cSection;

	}

	private void enableTOSRegistrySetting(boolean enable) {
		PlatformManager mgr = PlatformManagerFactory.getPlatformManager();

		if (mgr.hasCapability(PlatformManagerCapabilities.SetTCPTOSEnabled)) { 
			//see http://azureus.aelitis.com/wiki/index.php/AdvancedNetworkSettings
			try {
				mgr.setTCPTOSEnabled(enable);
			} catch (PlatformManagerException pe) {
				Debug.printStackTrace(pe);
			}
		}
	}

}
