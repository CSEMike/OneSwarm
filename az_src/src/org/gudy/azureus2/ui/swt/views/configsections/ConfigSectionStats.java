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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.core3.stats.StatsWriterPeriodic;

import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

public class ConfigSectionStats implements UISWTConfigSection {
	
  private static final int defaultStatsPeriod = 30;
  
  private static final int statsPeriods[] =
    {
      1, 2, 3, 4, 5, 10, 15, 20, 25, 30, 40, 50,
      60, 120, 180, 240, 300, 360, 420, 480, 540, 600,
      900, 1200, 1800, 2400, 3000, 3600,
      7200, 10800, 14400, 21600, 43200, 86400,
    };

  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_ROOT;
  }

	public String configSectionGetName() {
		return "stats";
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

    Composite gStats = new Composite(parent, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gStats.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 3;
    gStats.setLayout(layout);

    // row

    gridData = new GridData();
    gridData.horizontalSpan = 3;
    BooleanParameter enableStats = 
        new BooleanParameter(gStats, "Stats Enable",
                             "ConfigView.section.stats.enable");
    enableStats.setLayoutData(gridData);

    Control[] controls = new Control[14];

    // row

    Label lStatsPath = new Label(gStats, SWT.NULL);
    Messages.setLanguageText(lStatsPath, "ConfigView.section.stats.defaultsavepath"); //$NON-NLS-1$

    gridData = new GridData();
    gridData.widthHint = 150;
    final StringParameter pathParameter = new StringParameter(gStats, "Stats Dir", ""); //$NON-NLS-1$ //$NON-NLS-2$
    pathParameter.setLayoutData(gridData);
    controls[0] = lStatsPath;
    controls[1] = pathParameter.getControl();
    Button browse = new Button(gStats, SWT.PUSH);
    browse.setImage(imgOpenFolder);
    imgOpenFolder.setBackground(browse.getBackground());
    browse.setToolTipText(MessageText.getString("ConfigView.button.browse"));
    controls[2] = browse;
    browse.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.APPLICATION_MODAL);
      dialog.setFilterPath(pathParameter.getValue());
      dialog.setText(MessageText.getString("ConfigView.section.stats.choosedefaultsavepath")); //$NON-NLS-1$
      String path = dialog.open();
      if (path != null) {
        pathParameter.setValue(path);
      }
      }
    });

    // row

    Label lSaveFile = new Label(gStats, SWT.NULL);
    Messages.setLanguageText(lSaveFile, "ConfigView.section.stats.savefile"); //$NON-NLS-1$
    controls[3] = lSaveFile;
    
    gridData = new GridData();
    gridData.widthHint = 150;
    final StringParameter fileParameter = new StringParameter(gStats, "Stats File", StatsWriterPeriodic.DEFAULT_STATS_FILE_NAME );
    fileParameter.setLayoutData(gridData);
    controls[4] = fileParameter.getControl();
    new Label(gStats, SWT.NULL);

    // row

    Label lxslFile = new Label(gStats, SWT.NULL);
    Messages.setLanguageText(lxslFile, "ConfigView.section.stats.xslfile"); //$NON-NLS-1$
    controls[5] = lxslFile;
    
    gridData = new GridData();
    gridData.widthHint = 150;
    final StringParameter xslParameter = new StringParameter(gStats, "Stats XSL File", "" );
    xslParameter.setLayoutData(gridData);
    controls[6] = xslParameter.getControl();
    Label lxslDetails = new Label(gStats, SWT.NULL);
    Messages.setLanguageText(lxslDetails, "ConfigView.section.stats.xslfiledetails"); //$NON-NLS-1$
    final String linkFAQ = "http://plugins.vuze.com/faq.php#20";
    lxslDetails.setCursor(lxslDetails.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
    lxslDetails.setForeground(Colors.blue);
    lxslDetails.addMouseListener(new MouseAdapter() {
      public void mouseDoubleClick(MouseEvent arg0) {
      	Utils.launch(linkFAQ);
      }
      public void mouseDown(MouseEvent arg0) {
      	Utils.launch(linkFAQ);
      }
    });
    controls[7] = lxslDetails;
    
    // row

    Label lSaveFreq = new Label(gStats, SWT.NULL);

    Messages.setLanguageText(lSaveFreq, "ConfigView.section.stats.savefreq");
    controls[8] = lSaveFreq;
    
    final String spLabels[] = new String[statsPeriods.length];
    final int spValues[] = new int[statsPeriods.length];
    for (int i = 0; i < statsPeriods.length; i++) {
      int num = statsPeriods[i];

      if ( num%3600 == 0 )
        spLabels[i] = " " + (statsPeriods[i]/3600) + " " + 
                             MessageText.getString("ConfigView.section.stats.hours");

      else if ( num%60 == 0 )
        spLabels[i] = " " + (statsPeriods[i]/60) + " " + 
                             MessageText.getString("ConfigView.section.stats.minutes");

      else
        spLabels[i] = " " + statsPeriods[i] + " " + 
                            MessageText.getString("ConfigView.section.stats.seconds");

      spValues[i] = statsPeriods[i];
    }

    controls[9] = lSaveFreq;
    controls[10] = new IntListParameter(gStats, "Stats Period", defaultStatsPeriod, spLabels, spValues).getControl();
    new Label(gStats, SWT.NULL);

    	// ROW
    
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    BooleanParameter exportPeers = 
        new BooleanParameter(gStats, "Stats Export Peer Details",
                             "ConfigView.section.stats.exportpeers");
    exportPeers.setLayoutData(gridData);

    controls[11] = exportPeers.getControl();
    
 	// ROW
    
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    BooleanParameter exportFiles = 
        new BooleanParameter(gStats, "Stats Export File Details",
                             "ConfigView.section.stats.exportfiles");
    exportFiles.setLayoutData(gridData);

    controls[12] = exportFiles.getControl();

    // ROW
    
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    BooleanParameter graph_dividers = new BooleanParameter(gStats, "Stats Graph Dividers", "ConfigView.section.stats.graph_update_dividers");
    graph_dividers.setLayoutData(gridData);

    controls[13] = graph_dividers.getControl();
    
    	// control stuff
    
    enableStats.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(controls));

 
    return gStats;
  }
}
