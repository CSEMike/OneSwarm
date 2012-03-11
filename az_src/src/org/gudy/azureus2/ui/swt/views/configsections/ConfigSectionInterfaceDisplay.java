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

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

public class ConfigSectionInterfaceDisplay implements UISWTConfigSection {
	private final static String MSG_PREFIX = "ConfigView.section.style.";

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
		return 2;
	}


	public Composite configSectionCreate(final Composite parent) {
    int userMode = COConfigurationManager.getIntParameter("User Mode");
		boolean isAZ3 = COConfigurationManager.getStringParameter("ui").equals("az3");

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
		layout.numColumns = 1;
		cSection.setLayout(layout);

		new BooleanParameter(cSection, "Show Download Basket", MSG_PREFIX
				+ "showdownloadbasket");

		if (!isAZ3) {
  		new BooleanParameter(cSection, "IconBar.enabled", MSG_PREFIX
  				+ "showiconbar");
		}

		Group cStatusBar = new Group(cSection, SWT.NULL);
		Messages.setLanguageText(cStatusBar, MSG_PREFIX + "status");
		layout = new GridLayout();
		layout.numColumns = 1;
		cStatusBar.setLayout(layout);
		cStatusBar.setLayoutData(new GridData());

		new BooleanParameter(cStatusBar, "Status Area Show SR", MSG_PREFIX	+ "status.show_sr");
		new BooleanParameter(cStatusBar, "Status Area Show NAT",  MSG_PREFIX + "status.show_nat");
		new BooleanParameter(cStatusBar, "Status Area Show DDB", MSG_PREFIX + "status.show_ddb");
		new BooleanParameter(cStatusBar, "Status Area Show IPF", MSG_PREFIX + "status.show_ipf");
		new BooleanParameter(cStatusBar, "status.rategraphs", MSG_PREFIX + "status.show_rategraphs");
		
		new BooleanParameter(cSection, "Add URL Silently", MSG_PREFIX	+ "addurlsilently");

		new BooleanParameter(cSection, "suppress_file_download_dialog", "ConfigView.section.interface.display.suppress.file.download.dialog");

		new BooleanParameter(cSection, "show_torrents_menu", "Menu.show.torrent.menu");

		if (Constants.isWindowsXP) {
			final Button enableXPStyle = new Button(cSection, SWT.CHECK);
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

		if (Constants.isOSX) {
			new BooleanParameter(cSection, "enable_small_osx_fonts", MSG_PREFIX
					+ "osx_small_fonts");
		}

		if (userMode > 0) {
			Group cUnits = new Group(cSection, SWT.NULL);
			Messages.setLanguageText(cUnits, MSG_PREFIX + "units");
			layout = new GridLayout();
			layout.numColumns = 1;
			cUnits.setLayout(layout);
			cUnits.setLayoutData(new GridData());

			new BooleanParameter(cUnits, "config.style.useSIUnits", MSG_PREFIX
					+ "useSIUnits");

			new BooleanParameter(cUnits, "config.style.forceSIValues", MSG_PREFIX
					+ "forceSIValues");

			new BooleanParameter(cUnits, "config.style.useUnitsRateBits", MSG_PREFIX
					+ "useUnitsRateBits");

			new BooleanParameter(cUnits, "config.style.doNotUseGB", MSG_PREFIX
					+ "doNotUseGB");

			new BooleanParameter(cUnits, "config.style.dataStatsOnly", MSG_PREFIX
					+ "dataStatsOnly");

			new BooleanParameter(cUnits, "config.style.separateProtDataStats",
					MSG_PREFIX + "separateProtDataStats");
		}
		
    if( userMode > 1 ) {
  		final BooleanParameter fMoz = new BooleanParameter(cSection, "swt.forceMozilla",MSG_PREFIX + "forceMozilla");
  		Composite pArea = new Composite(cSection,SWT.NULL);
  		pArea.setLayout(new GridLayout(3,false));
  		pArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
  		Messages.setLanguageText(new Label(pArea,SWT.NONE), MSG_PREFIX+"xulRunnerPath");
  		final Parameter xulDir = new DirectoryParameter(pArea, "swt.xulRunner.path","");
  		fMoz.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(xulDir.getControls(), false));
    }
		
		Composite cArea = new Composite(cSection, SWT.NULL);
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
		IntParameter inactiveUpdate = new IntParameter(cArea, "Refresh When Inactive", 1,	-1);
		inactiveUpdate.setLayoutData(gridData);

		label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(label, MSG_PREFIX + "graphicsUpdate");
		gridData = new GridData();
		IntParameter graphicUpdate = new IntParameter(cArea, "Graphics Update", 1,	-1);
		graphicUpdate.setLayoutData(gridData);

		
		// Reuse the labels of the other menu actions.
		if (PlatformManagerFactory.getPlatformManager().hasCapability(PlatformManagerCapabilities.ShowFileInBrowser)) {
			BooleanParameter bp = new BooleanParameter(cSection, "MyTorrentsView.menu.show_parent_folder_enabled", MSG_PREFIX
					+ "use_show_parent_folder");
			Messages.setLanguageText(bp.getControl(), "ConfigView.section.style.use_show_parent_folder", new String[] {
				MessageText.getString("MyTorrentsView.menu.open_parent_folder"),
				MessageText.getString("MyTorrentsView.menu.explore"),
			});
			
			if (Constants.isOSX) {
				new BooleanParameter(cSection, "FileBrowse.usePathFinder", 
						MSG_PREFIX + "usePathFinder");
			}
		}
		
		if ( Constants.isOSX_10_5_OrHigher ){
			
			Composite cSWT = new Composite(cSection, SWT.NULL);
			layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			layout.numColumns = 2;
			cSWT.setLayout(layout);
			cSWT.setLayoutData(new GridData());
			
			label = new Label(cSWT, SWT.NULL);
			label.setText( "SWT Library" );
			String[] swtLibraries = { "carbon", "cocoa" };
					
			new StringListParameter(cSWT, MSG_PREFIX + "swt.library.selection", swtLibraries, swtLibraries);
		}
		
		return cSection;
	}
}
