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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.ColorParameter;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

public class ConfigSectionInterfaceColor implements UISWTConfigSection {
	private static final String[] sColorsToOverride = { "progressBar", "error",
			"warning", "altRow" };

	private Color[] colorsToOverride = { Colors.colorProgressBar,
			Colors.colorError, Colors.colorWarning, Colors.colorAltRow };

	private Button[] btnColorReset = new Button[sColorsToOverride.length];

	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_INTERFACE;
	}

	public String configSectionGetName() {
		return "color";
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {
	}
	
	public int maxUserMode() {
		return 0;
	}


	public Composite configSectionCreate(final Composite parent) {
		Label label;
		GridLayout layout;
		GridData gridData;
		Composite cSection = new Composite(parent, SWT.NULL);
		cSection.setLayoutData(new GridData(GridData.FILL_BOTH));
		layout = new GridLayout();
		layout.numColumns = 1;
		cSection.setLayout(layout);

		Composite cArea = new Composite(cSection, SWT.NULL);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		cArea.setLayout(layout);
		cArea.setLayoutData(new GridData());

		label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(label, "ConfigView.section.color");
		ColorParameter colorScheme = new ColorParameter(cArea, "Color Scheme", 0,
				128, 255);
		gridData = new GridData();
		gridData.widthHint = 50;
		colorScheme.setLayoutData(gridData);

		Group cColorOverride = new Group(cArea, SWT.NULL);
		Messages.setLanguageText(cColorOverride,
				"ConfigView.section.style.colorOverrides");
		layout = new GridLayout();
		layout.numColumns = 3;
		cColorOverride.setLayout(layout);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gridData.horizontalSpan = 2;
		cColorOverride.setLayoutData(gridData);

		for (int i = 0; i < sColorsToOverride.length; i++) {
			if (Utils.TABLE_GRIDLINE_IS_ALTERNATING_COLOR && sColorsToOverride[i].equals("altRow")) {
				continue;
			}
			String sConfigID = "Colors." + sColorsToOverride[i];
			label = new Label(cColorOverride, SWT.NULL);
			Messages.setLanguageText(label, "ConfigView.section.style.colorOverride."
					+ sColorsToOverride[i]);
			ColorParameter colorParm = new ColorParameter(cColorOverride, sConfigID,
					colorsToOverride[i].getRed(), colorsToOverride[i].getGreen(),
					colorsToOverride[i].getBlue()) {
				public void newColorChosen() {
					COConfigurationManager.setParameter(sParamName + ".override", true);
					for (int i = 0; i < sColorsToOverride.length; i++) {
						if (sParamName.equals("Colors." + sColorsToOverride[i])) {
							btnColorReset[i].setEnabled(true);
							break;
						}
					}
				}
			};
			gridData = new GridData();
			gridData.widthHint = 50;
			colorParm.setLayoutData(gridData);
			btnColorReset[i] = new Button(cColorOverride, SWT.PUSH);
			Messages.setLanguageText(btnColorReset[i],
					"ConfigView.section.style.colorOverrides.reset");
			btnColorReset[i].setEnabled(COConfigurationManager.getBooleanParameter(
					sConfigID + ".override", false));
			btnColorReset[i].setData("ColorName", sConfigID);
			btnColorReset[i].addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					Button btn = (Button) event.widget;
					String sName = (String) btn.getData("ColorName");
					if (sName != null) {
						COConfigurationManager.setParameter(sName + ".override", false);
						btn.setEnabled(false);
					}
				}
			});
		}

		return cSection;
	}
}
