/**
 * Created on Jan 6, 2009
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package org.gudy.azureus2.ui.swt.views.configsections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.config.BooleanParameter;
import org.gudy.azureus2.ui.swt.config.ChangeSelectionActionPerformer;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;

/**
 * @author TuxPaper
 * @created Jan 6, 2009
 *
 */
public class ConfigSectionInterfaceLegacy
	implements UISWTConfigSection
{
	private final static String LBLKEY_PREFIX = "ConfigView.label.";

	private final int REQUIRED_MODE = 2;

	// @see org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection#configSectionCreate(org.eclipse.swt.widgets.Composite)
	public Composite configSectionCreate(Composite parent) {
		GridData gridData;
		GridLayout layout;
		Label label;

		Composite cSection = new Composite(parent, SWT.NULL);

		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
				| GridData.HORIZONTAL_ALIGN_FILL);
		cSection.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		cSection.setLayout(layout);

		int userMode = COConfigurationManager.getIntParameter("User Mode");
		if (userMode < REQUIRED_MODE) {
			label = new Label(cSection, SWT.WRAP);
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

		/**
		 * Old-style speed menus.
		 */
		new BooleanParameter(cSection, "GUI_SWT_bOldSpeedMenu", LBLKEY_PREFIX
				+ "use_old_speed_menus");

		BooleanParameter bpCustomTab = new BooleanParameter(cSection,
				"useCustomTab", "ConfigView.section.style.useCustomTabs");
		Control cFancyTab = new BooleanParameter(cSection, "GUI_SWT_bFancyTab",
				"ConfigView.section.style.useFancyTabs").getControl();

		Control[] controls = {
			cFancyTab
		};
		bpCustomTab.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
				controls));

		return cSection;
	}

	// @see org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection#maxUserMode()
	public int maxUserMode() {
		return REQUIRED_MODE;
	}

	// @see org.gudy.azureus2.plugins.ui.config.ConfigSection#configSectionDelete()
	public void configSectionDelete() {
		// TODO Auto-generated method stub

	}

	// @see org.gudy.azureus2.plugins.ui.config.ConfigSection#configSectionGetName()
	public String configSectionGetName() {
		return "interface.legacy";
	}

	// @see org.gudy.azureus2.plugins.ui.config.ConfigSection#configSectionGetParentSection()
	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_INTERFACE;
	}

	// @see org.gudy.azureus2.plugins.ui.config.ConfigSection#configSectionSave()
	public void configSectionSave() {
		// TODO Auto-generated method stub

	}

}
