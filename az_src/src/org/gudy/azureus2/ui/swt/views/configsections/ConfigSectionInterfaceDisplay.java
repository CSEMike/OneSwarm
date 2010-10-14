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

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;

public class ConfigSectionInterfaceDisplay implements UISWTConfigSection {
	private final static String MSG_PREFIX = "ConfigView.section.style.";
	private final static String LBLKEY_PREFIX = "ConfigView.label.";

	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_INTERFACE;
	}

	public String configSectionGetName() {
		return "display";
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {
	}
	
	public int maxUserMode() {
		return 0;
	}


	public Composite configSectionCreate(final Composite parent) {
		// "Display" Sub-Section:
		// ----------------------
		// Any Look & Feel settings that don't really change the way the user 
		// normally interacts
		Label label;
		GridLayout layout;
		GridData gridData;
		Composite cLook = new Composite(parent, SWT.NULL);
		cLook.setLayoutData(new GridData(GridData.FILL_BOTH));
		layout = new GridLayout();
		layout.numColumns = 1;
		cLook.setLayout(layout);

		BooleanParameter bpCustomTab = new BooleanParameter(cLook, "useCustomTab", MSG_PREFIX + "useCustomTabs");
		Control cFancyTab = new BooleanParameter(cLook, "GUI_SWT_bFancyTab", 
				MSG_PREFIX + "useFancyTabs").getControl();

		Control[] controls = { cFancyTab };
		bpCustomTab
				.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
						controls));

		new BooleanParameter(cLook, "Show Download Basket", MSG_PREFIX
				+ "showdownloadbasket");

		new BooleanParameter(cLook, "IconBar.enabled", MSG_PREFIX
				+ "showiconbar");

		Composite cStatusBar = new Composite(cLook, SWT.NULL);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 5;
		cStatusBar.setLayout(layout);
		cStatusBar.setLayoutData(new GridData());

		label = new Label(cStatusBar, SWT.NULL);
		Messages.setLanguageText(label, MSG_PREFIX + "status");
		new BooleanParameter(cStatusBar, "Status Area Show SR", MSG_PREFIX	+ "status.show_sr");
		new BooleanParameter(cStatusBar, "Status Area Show NAT",  MSG_PREFIX + "status.show_nat");
		new BooleanParameter(cStatusBar, "Status Area Show DDB", MSG_PREFIX + "status.show_ddb");
		new BooleanParameter(cStatusBar, "Status Area Show IPF", MSG_PREFIX + "status.show_ipf");
		
		new BooleanParameter(cLook, "Add URL Silently", MSG_PREFIX	+ "addurlsilently");
		new BooleanParameter(cLook, "add_torrents_silently", "ConfigView.section.interface.display.add_torrents_silently");

		new BooleanParameter(cLook, "suppress_file_download_dialog", "ConfigView.section.interface.display.suppress.file.download.dialog");

		new BooleanParameter(cLook, "show_torrents_menu", "Menu.show.torrent.menu");

		if (Constants.isWindowsXP) {
			final Button enableXPStyle = new Button(cLook, SWT.CHECK);
			Messages.setLanguageText(enableXPStyle, MSG_PREFIX + "enableXPStyle");

			boolean enabled = false;
			boolean valid = false;
			try {
				File f = new File(System.getProperty("java.home")
						+ "\\bin\\javaw.exe.manifest");
				if (f.exists()) {
					enabled = true;
				}
				f = FileUtil.getApplicationFile("javaw.exe.manifest");
				if (f.exists()) {
					valid = true;
				}
			} catch (Exception e) {
				Debug.printStackTrace(e);
				valid = false;
			}
			enableXPStyle.setEnabled(valid);
			enableXPStyle.setSelection(enabled);
			enableXPStyle.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event arg0) {
					//In case we enable the XP Style
					if (enableXPStyle.getSelection()) {
						try {
							File fDest = new File(System.getProperty("java.home")
									+ "\\bin\\javaw.exe.manifest");
							File fOrigin = new File("javaw.exe.manifest");
							if (!fDest.exists() && fOrigin.exists()) {
								FileUtil.copyFile(fOrigin, fDest);
							}
						} catch (Exception e) {
							Debug.printStackTrace(e);
						}
					} else {
						try {
							File fDest = new File(System.getProperty("java.home")
									+ "\\bin\\javaw.exe.manifest");
							fDest.delete();
						} catch (Exception e) {
							Debug.printStackTrace(e);
						}
					}
				}
			});
		}

		if (Utils.isGTK) {
			// See Eclipse Bug #42416 ([Platform Inconsistency] GC(Table) has wrong origin)
			new BooleanParameter(cLook, "SWT_bGTKTableBug", MSG_PREFIX
					+ "verticaloffset");
		}

		if (Constants.isOSX) {
			new BooleanParameter(cLook, "enable_small_osx_fonts", MSG_PREFIX
					+ "osx_small_fonts");
		}

		new BooleanParameter(cLook, "GUI_SWT_bAlternateTablePainting", MSG_PREFIX
				+ "alternateTablePainting");

		new BooleanParameter(cLook, "config.style.useSIUnits", MSG_PREFIX
				+ "useSIUnits");
		new BooleanParameter(cLook, "config.style.useUnitsRateBits",
				MSG_PREFIX + "useUnitsRateBits");
		new BooleanParameter(cLook, "config.style.doNotUseGB", MSG_PREFIX
				+ "doNotUseGB");

		new BooleanParameter(cLook, "config.style.dataStatsOnly", MSG_PREFIX
				+ "dataStatsOnly");

		new BooleanParameter(cLook, "config.style.separateProtDataStats", MSG_PREFIX
				+ "separateProtDataStats");

		Composite cArea = new Composite(cLook, SWT.NULL);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		cArea.setLayout(layout);
		cArea.setLayoutData(new GridData());

		label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(label, MSG_PREFIX + "guiUpdate");
		int[] values = { 100, 250, 500, 1000, 2000, 5000, 10000, 15000 };
		String[] labels = { "100 ms", "250 ms", "500 ms", "1 s", "2 s", "5 s", "10 s", "15 s" };
		new IntListParameter(cArea, "GUI Refresh", 1000, labels, values);
		
		label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(label, MSG_PREFIX + "inactiveUpdate");
		gridData = new GridData();
		gridData.widthHint = 15;
		IntParameter inactiveUpdate = new IntParameter(cArea, "Refresh When Inactive", 1,	-1);
		inactiveUpdate.setLayoutData(gridData);

		label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(label, MSG_PREFIX + "graphicsUpdate");
		gridData = new GridData();
		gridData.widthHint = 15;
		IntParameter graphicUpdate = new IntParameter(cArea, "Graphics Update", 1,	-1);
		graphicUpdate.setLayoutData(gridData);

		label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(label, MSG_PREFIX + "reOrderDelay");
		gridData = new GridData();
		gridData.widthHint = 15;
		IntParameter reorderDelay = new IntParameter(cArea, "ReOrder Delay");
		reorderDelay.setLayoutData(gridData);

		label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(label, MSG_PREFIX + "defaultSortOrder");
		int[] sortOrderValues = { 0, 1, 2 };
		String[] sortOrderLabels = {
				MessageText.getString(MSG_PREFIX + "defaultSortOrder.asc"),
				MessageText.getString(MSG_PREFIX + "defaultSortOrder.desc"),
				MessageText.getString(MSG_PREFIX + "defaultSortOrder.flip") };
		new IntListParameter(cArea, "config.style.table.defaultSortOrder",
				sortOrderLabels, sortOrderValues);
		
		new BooleanParameter(cLook, "NameColumn.showProgramIcon", MSG_PREFIX
				+ "showProgramIcon");

		new BooleanParameter(cLook, "DND Always In Incomplete", MSG_PREFIX
				+ "DNDalwaysInIncomplete");
		
		// Reuse the labels of the other menu actions.
		if (PlatformManagerFactory.getPlatformManager().hasCapability(PlatformManagerCapabilities.ShowFileInBrowser)) {
			BooleanParameter bp = new BooleanParameter(cLook, "MyTorrentsView.menu.show_parent_folder_enabled", MSG_PREFIX
					+ "use_show_parent_folder");
			Messages.setLanguageText(bp.getControl(), "ConfigView.section.style.use_show_parent_folder", new String[] {
				MessageText.getString("MyTorrentsView.menu.open_parent_folder"),
				MessageText.getString("MyTorrentsView.menu.explore"),
			});
			
			if (Constants.isOSX) {
				new BooleanParameter(cLook, "FileBrowse.usePathFinder", 
						MSG_PREFIX + "usePathFinder");
			}
		}
		
		return cLook;
	}
}
