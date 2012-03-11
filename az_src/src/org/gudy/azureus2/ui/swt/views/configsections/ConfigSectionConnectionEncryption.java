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
import org.gudy.azureus2.ui.swt.components.LinkLabel;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

public class ConfigSectionConnectionEncryption implements UISWTConfigSection {

	private final String CFG_PREFIX = "ConfigView.section.connection.encryption.";
	
	private final int REQUIRED_MODE = 1;
	
	public int maxUserMode() {
		return REQUIRED_MODE;
	}


	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_CONNECTION;
	}

	public String configSectionGetName() {
		return "connection.encryption";
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
		
		Group gCrypto = new Group(cSection, SWT.NULL);
		Messages.setLanguageText(gCrypto, CFG_PREFIX + "encrypt.group");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gCrypto.setLayoutData(gridData);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		gCrypto.setLayout(layout);
		
		Label lcrypto = new Label(gCrypto, SWT.WRAP);
		Messages.setLanguageText(lcrypto, CFG_PREFIX + "encrypt.info");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		gridData.widthHint = 200;  // needed for wrap
		lcrypto.setLayoutData(gridData);

		gridData = new GridData();
		gridData.horizontalSpan = 2;
		new LinkLabel(gCrypto, gridData, CFG_PREFIX
				+ "encrypt.info.link",
				"http://wiki.vuze.com/w/Avoid_traffic_shaping");
		
		final BooleanParameter require = new BooleanParameter(gCrypto,	"network.transport.encrypted.require", CFG_PREFIX + "require_encrypted_transport");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		require.setLayoutData(gridData);
		
		String[] encryption_types = { "Plain", "RC4" };
		String dropLabels[] = new String[encryption_types.length];
		String dropValues[] = new String[encryption_types.length];
		for (int i = 0; i < encryption_types.length; i++) {
			dropLabels[i] = encryption_types[i];
			dropValues[i] = encryption_types[i];
		}
		
		Composite cEncryptLevel = new Composite(gCrypto, SWT.NULL);
		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
		gridData.horizontalSpan = 2;
		cEncryptLevel.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		cEncryptLevel.setLayout(layout);
		
		Label lmin = new Label(cEncryptLevel, SWT.NULL);
		Messages.setLanguageText(lmin, CFG_PREFIX + "min_encryption_level");
		final StringListParameter min_level = new StringListParameter(cEncryptLevel,	"network.transport.encrypted.min_level", encryption_types[1], dropLabels, dropValues);
		
		Label lcryptofb = new Label(gCrypto, SWT.WRAP);
		Messages.setLanguageText(lcryptofb, CFG_PREFIX + "encrypt.fallback_info");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		gridData.widthHint = 200;  // needed for wrap
		lcryptofb.setLayoutData(gridData);

		BooleanParameter fallback_outgoing = new BooleanParameter(gCrypto, "network.transport.encrypted.fallback.outgoing", CFG_PREFIX + "encrypt.fallback_outgoing");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		fallback_outgoing.setLayoutData(gridData);
		
		final BooleanParameter fallback_incoming = new BooleanParameter(gCrypto, "network.transport.encrypted.fallback.incoming", CFG_PREFIX + "encrypt.fallback_incoming");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		fallback_incoming.setLayoutData(gridData);
		
		final BooleanParameter use_crypto_port = new BooleanParameter(gCrypto, "network.transport.encrypted.use.crypto.port", CFG_PREFIX + "use_crypto_port");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		use_crypto_port.setLayoutData(gridData);

		
		final Control[] ap_controls = {	min_level.getControl(), lmin, lcryptofb, fallback_outgoing.getControl(), fallback_incoming.getControl()};

      	IAdditionalActionPerformer iap = 
      		new GenericActionPerformer(new Control[] {}) 
      		{
	    		public void 
	    		performAction() 
	    		{	    
	    			boolean	required = require.isSelected();
	    			
	    			boolean	ucp_enabled = !fallback_incoming.isSelected() && required;
	    			
	    			use_crypto_port.getControl().setEnabled( ucp_enabled );	

	    			for (int i=0;i<ap_controls.length;i++){
	    				
	    				ap_controls[i].setEnabled( required );
	    			}
	    		}
	    	};
	    	
	   	fallback_incoming.setAdditionalActionPerformer( iap );
	   	
		require.setAdditionalActionPerformer( iap );
		
		///////////////////////   

		return cSection;

	}

}
