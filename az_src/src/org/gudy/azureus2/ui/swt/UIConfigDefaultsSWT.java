/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationDefaults;
import org.gudy.azureus2.core3.util.Constants;

import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;

/**
 * @author TuxPaper
 * @created Nov 3, 2006
 *
 */
public class UIConfigDefaultsSWT
{

	/**
	 * 
	 */
	public static void initialize() {
		ConfigurationDefaults def = ConfigurationDefaults.getInstance();
		def.addParameter("useCustomTab", true);
		def.addParameter("GUI Refresh", 500);
		def.addParameter("Graphics Update", 4);
		def.addParameter("ReOrder Delay", 4);
		def.addParameter("Refresh When Inactive", 2);
		def.addParameter("Send Version Info", true);
		def.addParameter("Show Download Basket", false);
		def.addParameter("config.style.refreshMT", 0);
		def.addParameter("Open Details", false);
		def.addParameter("Open Seeding Details", false);
		def.addParameter("IconBar.enabled", true);

		def.addParameter("DefaultDir.BestGuess", true);
		def.addParameter("DefaultDir.AutoUpdate", true);
		def.addParameter("DefaultDir.AutoSave.AutoRename", true);
		def.addParameter("GUI_SWT_bFancyTab", true);
		def.addParameter("Colors.progressBar.override", false);
		def.addParameter("GUI_SWT_DisableAlertSliding", false);
		def.addParameter("NameColumn.showProgramIcon", !Constants.isWindowsVista);
		def.addParameter("DND Always In Incomplete", false);

		def.addParameter("Message Popup Autoclose in Seconds", 15);

		def.addParameter("Add URL Silently", false);
		def.addParameter("MyTorrents.SplitAt", 30);

		def.addParameter("Wizard Completed", false);
		def.addParameter("SpeedTest Completed", false);
		def.addParameter("Color Scheme.red", 0);
		def.addParameter("Color Scheme.green", 128);
		def.addParameter("Color Scheme.blue", 255);
		def.addParameter("Show Splash", true);
		def.addParameter("window.maximized", true);
		def.addParameter("window.rectangle", "");
		def.addParameter("Start Minimized", false);
		def.addParameter("Open Transfer Bar On Start", false);
		
        def.addParameter("Stats Graph Dividers", false);
		
		def.addParameter("Open Bar Incomplete", false);
		def.addParameter("Open Bar Complete", false);

		def.addParameter("Close To Tray", true);
		def.addParameter("Minimize To Tray", false);
		
		def.addParameter("Status Area Show SR", true);
		def.addParameter("Status Area Show NAT", true);
		def.addParameter("Status Area Show DDB", true);
		def.addParameter("Status Area Show IPF", true);
		def.addParameter("status.rategraphs", Utils.getUserMode() > 0);
		
		def.addParameter("GUI_SWT_share_count_at_close", 0 );

		def.addParameter("GUI_SWT_bOldSpeedMenu", false);
		
		def.addParameter("ui.toolbar.uiswitcher", false);
		def.addParameter("ui.systray.tooltip.enable", false);
		
		def.addParameter("Remember transfer bar location", true);
		
		if ( COConfigurationManager.getBooleanParameter( "Open Bar" )){
			
			COConfigurationManager.setParameter( "Open Bar Incomplete", true );
			COConfigurationManager.setParameter( "Open Bar Complete", true );
			
			COConfigurationManager.setParameter( "Open Bar", false );
		}
		
		def.addParameter("suppress_file_download_dialog", false);
		def.addParameter("auto_remove_inactive_items", false);
		def.addParameter("show_torrents_menu", true);
		
		def.addParameter("swt.forceMozilla",false);
		def.addParameter("swt.xulRunner.path","");
		
		String xulPath = COConfigurationManager.getStringParameter("swt.xulRunner.path");
		if(!xulPath.equals(""))
			System.setProperty("org.eclipse.swt.browser.XULRunnerPath", xulPath);
		
		
		def.addParameter("MyTorrentsView.table.style", 0);
		def.addParameter("MyTorrentsView.alwaysShowHeader", true);
		
		if (Constants.isOSX) {
			def.addParameter("ConfigView.section.style.swt.library.selection", "cocoa");
		}
		def.addParameter("v3.topbar.height", 60);
		def.addParameter("v3.topbar.show.plugin", false);
		def.addParameter("pluginbar.visible", false);
		def.addParameter("ui.toolbar.uiswitcher", false);
		def.addParameter("Table.extendedErase", false);
		def.addParameter("Table.useTree", false);

		if ("az2".equalsIgnoreCase(COConfigurationManager.getStringParameter("ui", "az3"))) {
			def.addParameter("v3.Show Welcome", false);

			def.addParameter("list.dm.dblclick", "1");
			def.addParameter(MultipleDocumentInterface.SIDEBAR_SECTION_LIBRARY + ".viewmode", 1);
  		def.addParameter(MultipleDocumentInterface.SIDEBAR_SECTION_LIBRARY_DL + "DL.viewmode", 1);
  		def.addParameter(MultipleDocumentInterface.SIDEBAR_SECTION_LIBRARY_CD + ".viewmode", 1);
		}
	}
}
