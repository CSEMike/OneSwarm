/*
 * File    : ConfigPanelFileTorrents.java
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;

public class ConfigSectionFileTorrents implements UISWTConfigSection {
  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_FILES;
  }

	public String configSectionGetName() {
		return "torrents";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
		ImageLoader imageLoader = ImageLoader.getInstance();
		imageLoader.releaseImage("openFolderButton");
  }
  
	public int maxUserMode() {
		return 0;
	}

  

  public Composite configSectionCreate(final Composite parent) {
		ImageLoader imageLoader = ImageLoader.getInstance();
		Image imgOpenFolder = imageLoader.getImage("openFolderButton");

    GridData gridData;
    GridLayout layout;

    // Sub-Section: File -> Torrent
    // ----------------------------
    Composite cTorrent = new Composite(parent, SWT.NULL);

    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    cTorrent.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    cTorrent.setLayout(layout);
    
    int userMode = COConfigurationManager.getIntParameter("User Mode");
    
    
    // Save .Torrent files to..
    BooleanParameter saveTorrents = new BooleanParameter(cTorrent, "Save Torrent Files",
                                                         "ConfigView.label.savetorrents");

    Composite gSaveTorrents = new Composite(cTorrent, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalIndent = 25;
    gridData.horizontalSpan = 2;
    gSaveTorrents.setLayoutData(gridData);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 3;
    gSaveTorrents.setLayout(layout);

    Label lSaveDir = new Label(gSaveTorrents, SWT.NULL);
    Messages.setLanguageText(lSaveDir, "ConfigView.label.savedirectory");

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    final StringParameter torrentPathParameter = new StringParameter(gSaveTorrents,
                                                                     "General_sDefaultTorrent_Directory");
    torrentPathParameter.setLayoutData(gridData);

    Button browse2 = new Button(gSaveTorrents, SWT.PUSH);
    browse2.setImage(imgOpenFolder);
    imgOpenFolder.setBackground(browse2.getBackground());
    browse2.setToolTipText(MessageText.getString("ConfigView.button.browse"));

    browse2.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.APPLICATION_MODAL);
        dialog.setFilterPath(torrentPathParameter.getValue());
        dialog.setText(MessageText.getString("ConfigView.dialog.choosedefaulttorrentpath"));
        String path = dialog.open();
        if (path != null) {
          torrentPathParameter.setValue(path);
        }
      }
    });
    browse2.setLayoutData(new GridData());

    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(gSaveTorrents, "Save Torrent Backup",
                        "ConfigView.label.savetorrentbackup").setLayoutData(gridData);

    Control[] controls = new Control[]{ gSaveTorrents };
    IAdditionalActionPerformer grayPathAndButton1 = new ChangeSelectionActionPerformer(controls);
    saveTorrents.setAdditionalActionPerformer(grayPathAndButton1);

    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new BooleanParameter(
    		cTorrent, 
			"Default Start Torrents Stopped",
    		"ConfigView.label.defaultstarttorrentsstopped").setLayoutData(gridData);


    // Watch Folder
    BooleanParameter watchFolder = new BooleanParameter(cTorrent, "Watch Torrent Folder",
                                                        "ConfigView.label.watchtorrentfolder");

    Composite gWatchFolder = new Composite(cTorrent, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalIndent = 25;
    gridData.horizontalSpan = 2;
    gWatchFolder.setLayoutData(gridData);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 3;
    gWatchFolder.setLayout(layout);

    Label lImportDir = new Label(gWatchFolder, SWT.NULL);
    Messages.setLanguageText(lImportDir, "ConfigView.label.importdirectory");

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    final StringParameter watchFolderPathParameter = new StringParameter(gWatchFolder,
                                                                         "Watch Torrent Folder Path", "");
    watchFolderPathParameter.setLayoutData(gridData);

    Button browse4 = new Button(gWatchFolder, SWT.PUSH);
    browse4.setImage(imgOpenFolder);
    imgOpenFolder.setBackground(browse4.getBackground());
    browse4.setToolTipText(MessageText.getString("ConfigView.button.browse"));

    browse4.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.APPLICATION_MODAL);
        dialog.setFilterPath(watchFolderPathParameter.getValue());
        dialog.setText(MessageText.getString("ConfigView.dialog.choosewatchtorrentfolderpath"));
        String path = dialog.open();
        if (path != null) {
          watchFolderPathParameter.setValue(path);
        }
      }
    });

    Label lWatchTorrentFolderInterval = new Label(gWatchFolder, SWT.NULL);
    Messages.setLanguageText(lWatchTorrentFolderInterval, "ConfigView.label.watchtorrentfolderinterval");
    String	min = " " + MessageText.getString("ConfigView.section.stats.minutes");
    String	hr  = " " + MessageText.getString("ConfigView.section.stats.hours");
    
    int	[]	watchTorrentFolderIntervalValues = 
    	{ 1, 2, 3, 4, 5, 10, 15, 30, 60, 2*60, 4*60, 6*60, 8*60, 12*60, 16*60, 20*60, 24*60 };
    
    final String watchTorrentFolderIntervalLabels[] = new String[watchTorrentFolderIntervalValues.length];

    for (int i = 0; i < watchTorrentFolderIntervalValues.length; i++) {
      int mins 	= watchTorrentFolderIntervalValues[i];
      int hrs	= mins/60;
      
      watchTorrentFolderIntervalLabels[i] = " " + (hrs==0?(mins + min):(hrs + hr ));
    }
    
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    new IntListParameter(gWatchFolder, "Watch Torrent Folder Interval", 1, 
                         watchTorrentFolderIntervalLabels, 
                         watchTorrentFolderIntervalValues).setLayoutData(gridData);

    gridData = new GridData();
    gridData.horizontalSpan = 3;
    new BooleanParameter(gWatchFolder, "Start Watched Torrents Stopped",
                         "ConfigView.label.startwatchedtorrentsstopped").setLayoutData(gridData);

    controls = new Control[]{ gWatchFolder };
    watchFolder.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(controls));

    return cTorrent;
  }
}
