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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

public class ConfigSectionConnectionProxy implements UISWTConfigSection {

	private final static String CFG_PREFIX = "ConfigView.section.proxy.";

	private final int REQUIRED_MODE = 2;
	
	public int maxUserMode() {
		return REQUIRED_MODE;
	}


	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_CONNECTION;
	}

	public String configSectionGetName() {
		return "proxy";
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {
	}

	public Composite configSectionCreate(final Composite parent) {
		GridData gridData;
		GridLayout layout;

		Composite cSection = new Composite(parent, SWT.NULL);

		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
				| GridData.HORIZONTAL_ALIGN_FILL);
		cSection.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 2;
		cSection.setLayout(layout);

		int userMode = COConfigurationManager.getIntParameter("User Mode");
		if (userMode < REQUIRED_MODE) {
			Label label = new Label(cSection, SWT.WRAP);
			gridData = new GridData();
			gridData.horizontalSpan = 2;
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

		//////////////////////  PROXY GROUP /////////////////
		
		Group gProxyTracker = new Group(cSection, SWT.NULL);
		Messages.setLanguageText(gProxyTracker, CFG_PREFIX + "group.tracker");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		gProxyTracker.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 2;
		gProxyTracker.setLayout(layout);
		
		final BooleanParameter enableProxy = new BooleanParameter(gProxyTracker,
				"Enable.Proxy", CFG_PREFIX + "enable_proxy");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		enableProxy.setLayoutData(gridData);

		final BooleanParameter enableSocks = new BooleanParameter(gProxyTracker,
				"Enable.SOCKS", CFG_PREFIX + "enable_socks");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		enableSocks.setLayoutData(gridData);

		Label lHost = new Label(gProxyTracker, SWT.NULL);
		Messages.setLanguageText(lHost, CFG_PREFIX + "host");
		StringParameter pHost = new StringParameter(gProxyTracker, "Proxy.Host", "");
		gridData = new GridData();
		gridData.widthHint = 105;
		pHost.setLayoutData(gridData);

		Label lPort = new Label(gProxyTracker, SWT.NULL);
		Messages.setLanguageText(lPort, CFG_PREFIX + "port");
		StringParameter pPort = new StringParameter(gProxyTracker, "Proxy.Port", "");
		gridData = new GridData();
		gridData.widthHint = 40;
		pPort.setLayoutData(gridData);

		Label lUser = new Label(gProxyTracker, SWT.NULL);
		Messages.setLanguageText(lUser, CFG_PREFIX + "username");
		StringParameter pUser = new StringParameter(gProxyTracker, "Proxy.Username" );
		gridData = new GridData();
		gridData.widthHint = 105;
		pUser.setLayoutData(gridData);

		Label lPass = new Label(gProxyTracker, SWT.NULL);
		Messages.setLanguageText(lPass, CFG_PREFIX + "password");
		StringParameter pPass = new StringParameter(gProxyTracker, "Proxy.Password", "");
		gridData = new GridData();
		gridData.widthHint = 105;
		pPass.setLayoutData(gridData);

		////////////////////////////////////////////////
		
		Group gProxyPeer = new Group(cSection, SWT.NULL);
		Messages.setLanguageText(gProxyPeer, CFG_PREFIX + "group.peer");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		gProxyPeer.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 2;
		gProxyPeer.setLayout(layout);

		final BooleanParameter enableSocksPeer = new BooleanParameter(gProxyPeer,
				"Proxy.Data.Enable", CFG_PREFIX + "enable_socks.peer");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		enableSocksPeer.setLayoutData(gridData);

		final BooleanParameter socksPeerInform = new BooleanParameter(gProxyPeer,
				"Proxy.Data.SOCKS.inform", CFG_PREFIX + "peer.informtracker");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		socksPeerInform.setLayoutData(gridData);

		Label lSocksVersion = new Label(gProxyPeer, SWT.NULL);
		Messages.setLanguageText(lSocksVersion, CFG_PREFIX + "socks.version");
		String[] socks_types = { "V4", "V4a", "V5" };
		String dropLabels[] = new String[socks_types.length];
		String dropValues[] = new String[socks_types.length];
		for (int i = 0; i < socks_types.length; i++) {
			dropLabels[i] = socks_types[i];
			dropValues[i] = socks_types[i];
		}
		final StringListParameter socksType = new StringListParameter(gProxyPeer,
				"Proxy.Data.SOCKS.version", "V4", dropLabels, dropValues);

		final BooleanParameter sameConfig = new BooleanParameter(gProxyPeer,
				"Proxy.Data.Same", CFG_PREFIX + "peer.same");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		sameConfig.setLayoutData(gridData);

		Label lDataHost = new Label(gProxyPeer, SWT.NULL);
		Messages.setLanguageText(lDataHost, CFG_PREFIX + "host");
		StringParameter pDataHost = new StringParameter(gProxyPeer,
				"Proxy.Data.Host", "");
		gridData = new GridData();
		gridData.widthHint = 105;
		pDataHost.setLayoutData(gridData);

		Label lDataPort = new Label(gProxyPeer, SWT.NULL);
		Messages.setLanguageText(lDataPort, CFG_PREFIX + "port");
		StringParameter pDataPort = new StringParameter(gProxyPeer,
				"Proxy.Data.Port", "");
		gridData = new GridData();
		gridData.widthHint = 40;
		pDataPort.setLayoutData(gridData);

		Label lDataUser = new Label(gProxyPeer, SWT.NULL);
		Messages.setLanguageText(lDataUser, CFG_PREFIX + "username");
		StringParameter pDataUser = new StringParameter(gProxyPeer,
				"Proxy.Data.Username");
		gridData = new GridData();
		gridData.widthHint = 105;
		pDataUser.setLayoutData(gridData);

		Label lDataPass = new Label(gProxyPeer, SWT.NULL);
		Messages.setLanguageText(lDataPass, CFG_PREFIX + "password");
		StringParameter pDataPass = new StringParameter(gProxyPeer,
				"Proxy.Data.Password", "");
		gridData = new GridData();
		gridData.widthHint = 105;
		pDataPass.setLayoutData(gridData);

		final Control[] proxy_controls = new Control[] { enableSocks.getControl(),
				lHost, pHost.getControl(), lPort, pPort.getControl(), lUser,
				pUser.getControl(), lPass, pPass.getControl(), };

		IAdditionalActionPerformer proxy_enabler = new GenericActionPerformer(
				new Control[] {}) {
			public void performAction() {
				for (int i = 0; i < proxy_controls.length; i++) {

					proxy_controls[i].setEnabled(enableProxy.isSelected());
				}
			}
		};

		final Control[] proxy_peer_controls = new Control[] { lDataHost,
				pDataHost.getControl(), lDataPort, pDataPort.getControl(), lDataUser,
				pDataUser.getControl(), lDataPass, pDataPass.getControl() };

		final Control[] proxy_peer_details = new Control[] {
				sameConfig.getControl(), socksPeerInform.getControl(),
				socksType.getControl(), lSocksVersion };

		IAdditionalActionPerformer proxy_peer_enabler = new GenericActionPerformer(
				new Control[] {}) {
			public void performAction() {
				for (int i = 0; i < proxy_peer_controls.length; i++) {

					proxy_peer_controls[i].setEnabled(enableSocksPeer.isSelected()
							&& !sameConfig.isSelected());
				}

				for (int i = 0; i < proxy_peer_details.length; i++) {

					proxy_peer_details[i].setEnabled(enableSocksPeer.isSelected());
				}
			}
		};

		enableSocks.setAdditionalActionPerformer(proxy_enabler);
		enableProxy.setAdditionalActionPerformer(proxy_enabler);
		enableSocksPeer.setAdditionalActionPerformer(proxy_peer_enabler);
		sameConfig.setAdditionalActionPerformer(proxy_peer_enabler);

		final BooleanParameter checkOnStart = new BooleanParameter(cSection,
				"Proxy.Check.On.Start", CFG_PREFIX + "check.on.start");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		checkOnStart.setLayoutData(gridData);

		Label label = new Label(cSection, SWT.WRAP);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		label.setLayoutData(gridData);
		label.setText(MessageText.getString(CFG_PREFIX+"username.info" ));
		
		return cSection;

	}
}
