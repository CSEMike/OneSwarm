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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

public class ConfigSectionInterfaceTables
	implements UISWTConfigSection
{
	private final static String MSG_PREFIX = "ConfigView.section.style.";

	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_INTERFACE;
	}

	public String configSectionGetName() {
		return "tables";
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {
	}

	public int maxUserMode() {
		return 2;
	}

	public Composite configSectionCreate(final Composite parent) {
		int userMode = COConfigurationManager.getIntParameter("User Mode");
		boolean isAZ3 = COConfigurationManager.getStringParameter("ui").equals(
				"az3");

		// "Display" Sub-Section:
		// ----------------------
		// Any Look & Feel settings that don't really change the way the user 
		// normally interacts
		Label label;
		GridLayout layout;
		GridData gridData;
		Composite cSection = new Composite(parent, SWT.NULL);
		cSection.setLayoutData(new GridData(GridData.FILL_BOTH));
		layout = new GridLayout();
		layout.numColumns = 2;
		cSection.setLayout(layout);

		label = new Label(cSection, SWT.NULL);
		Messages.setLanguageText(label, MSG_PREFIX + "defaultSortOrder");
		int[] sortOrderValues = {
			0,
			1,
			2
		};
		String[] sortOrderLabels = {
			MessageText.getString(MSG_PREFIX + "defaultSortOrder.asc"),
			MessageText.getString(MSG_PREFIX + "defaultSortOrder.desc"),
			MessageText.getString(MSG_PREFIX + "defaultSortOrder.flip")
		};
		new IntListParameter(cSection, "config.style.table.defaultSortOrder",
				sortOrderLabels, sortOrderValues);

		if (userMode > 0) {
			label = new Label(cSection, SWT.NULL);
			Messages.setLanguageText(label, MSG_PREFIX + "guiUpdate");
			int[] values = {
				100,
				250,
				500,
				1000,
				2000,
				5000,
				10000,
				15000
			};
			String[] labels = {
				"100 ms",
				"250 ms",
				"500 ms",
				"1 s",
				"2 s",
				"5 s",
				"10 s",
				"15 s"
			};
			new IntListParameter(cSection, "GUI Refresh", 1000, labels, values);

			label = new Label(cSection, SWT.NULL);
			Messages.setLanguageText(label, MSG_PREFIX + "graphicsUpdate");
			gridData = new GridData();
			IntParameter graphicUpdate = new IntParameter(cSection, "Graphics Update",
					1, -1);
			graphicUpdate.setLayoutData(gridData);

			label = new Label(cSection, SWT.NULL);
			Messages.setLanguageText(label, MSG_PREFIX + "reOrderDelay");
			gridData = new GridData();
			IntParameter reorderDelay = new IntParameter(cSection, "ReOrder Delay");
			reorderDelay.setLayoutData(gridData);

			new BooleanParameter(cSection, "NameColumn.showProgramIcon", MSG_PREFIX
					+ "showProgramIcon").setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false, 2, 1));
		}

		if (Constants.isWindows) {
  		new BooleanParameter(cSection, "Table.extendedErase", MSG_PREFIX
  				+ "extendedErase").setLayoutData(new GridData(SWT.FILL,
  				SWT.LEFT, true, false, 2, 1));
		}

		{
			Group cLibrary = new Group(cSection, SWT.NULL);
			Messages.setLanguageText(cLibrary, MSG_PREFIX + "library");
			layout = new GridLayout();
			layout.numColumns = 2;
			cLibrary.setLayout(layout);
			cLibrary.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));

			// double-click

			label = new Label(cLibrary, SWT.NULL);
			Messages.setLanguageText(label, "ConfigView.label.dm.dblclick");

			String[] dblclickOptions = {
				"ConfigView.option.dm.dblclick.play",
				"ConfigView.option.dm.dblclick.details",
				"ConfigView.option.dm.dblclick.show",
			};

			String dblclickLabels[] = new String[dblclickOptions.length];
			String dblclickValues[] = new String[dblclickOptions.length];

			for (int i = 0; i < dblclickOptions.length; i++) {

				dblclickLabels[i] = MessageText.getString(dblclickOptions[i]);
				dblclickValues[i] = "" + i;
			}
			new StringListParameter(cLibrary, "list.dm.dblclick", dblclickLabels,
					dblclickValues);


			new BooleanParameter(cLibrary, "Table.useTree", MSG_PREFIX
					+ "useTree").setLayoutData(new GridData(SWT.FILL,
							SWT.LEFT, true, false, 2, 1));

			if (userMode > 1) {
				new BooleanParameter(cLibrary, "DND Always In Incomplete", MSG_PREFIX
						+ "DNDalwaysInIncomplete").setLayoutData(new GridData(SWT.FILL,
								SWT.LEFT, true, false, 2, 1));
			}

			new BooleanParameter(cLibrary, "MyTorrentsView.alwaysShowHeader",
					"ConfigView.label.alwaysShowLibraryHeader").setLayoutData(new GridData(
							SWT.FILL, SWT.LEFT, true, false, 2, 1));

			if (isAZ3) {
				new BooleanParameter(cLibrary, "Library.CatInSideBar", MSG_PREFIX
						+ "CatInSidebar").setLayoutData(new GridData(SWT.FILL,
								SWT.LEFT, true, false, 2, 1));
			}
		}

		return cSection;
	}
}
