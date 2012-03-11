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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.logging.impl.FileLogging;
import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.mainwindow.ClipboardCopy;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.stats.AzureusCoreStats;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.update.UpdateException;
import org.gudy.azureus2.plugins.update.UpdateInstaller;
import org.gudy.azureus2.plugins.update.UpdateInstallerListener;
import org.gudy.azureus2.plugins.utils.StaticUtilities;

public class ConfigSectionLogging implements UISWTConfigSection {
  private static final LogIDs LOGID = LogIDs.GUI;
  private static final String CFG_PREFIX =  "ConfigView.section.logging.";
  private static final int logFileSizes[] =
     {
       1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 25, 30, 40, 50, 75, 100, 200, 300, 500
     };

  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_ROOT;
  }

	public String configSectionGetName() {
		return "logging";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
		ImageLoader imageLoader = ImageLoader.getInstance();
		imageLoader.releaseImage("openFolderButton");
  }
  
	public int maxUserMode() {
		return 2;
	}

  

  public Composite configSectionCreate(final Composite parent) {
		ImageLoader imageLoader = ImageLoader.getInstance();
		Image imgOpenFolder = imageLoader.getImage("openFolderButton");			

		GridData gridData;
    GridLayout layout;

    Composite gLogging = new Composite(parent, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gLogging.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    gLogging.setLayout(layout);
    
    int userMode = COConfigurationManager.getIntParameter("User Mode");

    
    BooleanParameter enable_logger = new BooleanParameter(gLogging, "Logger.Enabled", CFG_PREFIX + "loggerenable");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    enable_logger.setLayoutData(gridData);

    // row

    final BooleanParameter enableLogging = 
      new BooleanParameter(gLogging, 
                           "Logging Enable", 
                           CFG_PREFIX + "enable");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    enableLogging.setLayoutData(gridData);

    Composite cArea = new Composite(gLogging, SWT.NULL);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 3;
    cArea.setLayout(layout);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    cArea.setLayoutData(gridData);


    // row

    Label lStatsPath = new Label(cArea, SWT.NULL);
    Messages.setLanguageText(lStatsPath, CFG_PREFIX + "logdir"); //$NON-NLS-1$

    gridData = new GridData();
    gridData.widthHint = 150;
    final StringParameter pathParameter = new StringParameter(cArea, "Logging Dir"); //$NON-NLS-1$ //$NON-NLS-2$
    pathParameter.setLayoutData(gridData);
    Button browse = new Button(cArea, SWT.PUSH);
    browse.setImage(imgOpenFolder);
    imgOpenFolder.setBackground(browse.getBackground());
    browse.setToolTipText(MessageText.getString("ConfigView.button.browse"));
    browse.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
      DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.APPLICATION_MODAL);
        dialog.setFilterPath(pathParameter.getValue());
        dialog.setText(MessageText.getString(CFG_PREFIX + "choosedefaultsavepath")); //$NON-NLS-1$
        String path = dialog.open();
        if (path != null) {
        pathParameter.setValue(path);
        }
      }
    });

    Label lMaxLog = new Label(cArea, SWT.NULL);

    Messages.setLanguageText(lMaxLog, CFG_PREFIX + "maxsize");
    final String lmLabels[] = new String[logFileSizes.length];
    final int lmValues[] = new int[logFileSizes.length];
    for (int i = 0; i < logFileSizes.length; i++) {
      int  num = logFileSizes[i];
      lmLabels[i] = " " + num + " MB";
      lmValues[i] = num;
    }

    IntListParameter paramMaxSize = new IntListParameter(cArea, "Logging Max Size", lmLabels, lmValues);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    paramMaxSize.setLayoutData(gridData);
    
    if(userMode > 1)
    {
    	Label timeStampLbl = new Label(cArea, SWT.NULL);
    	Messages.setLanguageText(timeStampLbl, CFG_PREFIX+"timestamp");
    	timeStampLbl.setLayoutData(new GridData());
    	StringParameter timeStamp = new StringParameter(cArea,"Logging Timestamp");
    	gridData = new GridData();
    	gridData.horizontalSpan = 2;
    	gridData.widthHint = 150;
    	timeStamp.setLayoutData(gridData);
    }
    
    
    
    /** FileLogging filter, consisting of a List of types (info, warning, error)
     * and a checkbox Table of component IDs.
     */ 
    final String sFilterPrefix = CFG_PREFIX + "filter";
    Group gLogIDs = new Group(gLogging, SWT.NULL);
    Messages.setLanguageText(gLogIDs, sFilterPrefix);
    layout = new GridLayout();
    layout.numColumns = 2;
    gLogIDs.setLayout(layout);
    gridData = new GridData(SWT.BEGINNING, SWT.BEGINNING, true, true);
    gridData.horizontalSpan = 2;
    gLogIDs.setLayoutData(gridData);

    final List listLogTypes = new List(gLogIDs, SWT.BORDER | SWT.SINGLE
				| SWT.V_SCROLL);
    gridData = new GridData(SWT.NULL, SWT.BEGINNING, false, false);
    listLogTypes.setLayoutData(gridData);

    final int[] logTypes = { LogEvent.LT_INFORMATION, LogEvent.LT_WARNING,
				LogEvent.LT_ERROR };
		for (int i = 0; i < logTypes.length; i++)
			listLogTypes.add(MessageText.getString(CFG_PREFIX + "log" + i + "type"));
		listLogTypes.select(0);

		final LogIDs[] logIDs = FileLogging.configurableLOGIDs;
		//Arrays.sort(logIDs);
		final Table tableLogIDs = new Table(gLogIDs, SWT.CHECK | SWT.BORDER
				| SWT.SINGLE | SWT.FULL_SELECTION);
    gridData = new GridData(GridData.FILL_BOTH);
    tableLogIDs.setLayoutData(gridData);
    tableLogIDs.setLinesVisible (false);    
    tableLogIDs.setHeaderVisible(false);
    TableColumn column = new TableColumn(tableLogIDs, SWT.NONE);

    for (int i = 0; i < logIDs.length; i++) {
    	TableItem item = new TableItem(tableLogIDs, SWT.NULL);
			item.setText(0, MessageText.getString(sFilterPrefix + "." + logIDs[i],
					logIDs[i].toString()));
			item.setData(logIDs[i]);
			boolean checked = COConfigurationManager.getBooleanParameter("bLog."
					+ logTypes[0] + "." + logIDs[i], true);
			item.setChecked(checked);
    }
    column.pack();
    
    // Update table when list selection changes
		listLogTypes.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int index = listLogTypes.getSelectionIndex();
				if (index < 0 || index >= logTypes.length)
					return;
				TableItem[] items = tableLogIDs.getItems();
				for (int i = 0; i < items.length; i++) {
					boolean checked = COConfigurationManager.getBooleanParameter(
							"bLog." + logTypes[index] + "." + items[i].getData(),
							true);
					items[i].setChecked(checked);

				}
			}
		});
    
    // Save config when checkbox is clicked
    tableLogIDs.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (e.detail != SWT.CHECK)
					return;
				int index = listLogTypes.getSelectionIndex();
				if (index < 0 || index >= logTypes.length)
					return;
				TableItem item = (TableItem) e.item;
				COConfigurationManager.setParameter("bLog." + logTypes[index] + "."
						+ item.getData(), item.getChecked());
			}
		});
    
    
    final Control[] controls_main = { cArea, gLogIDs };
    final ChangeSelectionActionPerformer perf2 = new ChangeSelectionActionPerformer( controls_main );
    
    enableLogging.setAdditionalActionPerformer( perf2 );
    
    enable_logger.setAdditionalActionPerformer(
        new IAdditionalActionPerformer() {
          ChangeSelectionActionPerformer p1 = new ChangeSelectionActionPerformer(new Control[] {enableLogging.getControl() } );

          public void performAction() {
            p1.performAction();
          }
          public void setSelected(boolean selected) {
            p1.setSelected( selected );
            if( !selected && enableLogging.isSelected() )  enableLogging.setSelected( false );
          }
          public void setIntValue(int value) { /*nothing*/ }
          public void setStringValue(String value) { /*nothing*/ }
        }
    );

    /*
    BooleanParameter udp_transport = new BooleanParameter(gLogging, "Logging Enable UDP Transport", CFG_PREFIX + "udptransport");
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    udp_transport.setLayoutData(gridData);
    */
    
		// network diagnostics
	
	Label generate_net_info = new Label(gLogging, SWT.NULL);
	Messages.setLanguageText(generate_net_info, CFG_PREFIX + "netinfo");
	
	Button generate_net_button = new Button(gLogging, SWT.PUSH);
	Messages.setLanguageText(generate_net_button, CFG_PREFIX + "generatediagnostics");
	
	generate_net_button.addListener(
			SWT.Selection, 
			new Listener() 
			{
				public void 
				handleEvent(Event event) 
				{
					new AEThread2("GenerateNetDiag", true)
					{
						public void run() {
							StringWriter sw = new StringWriter();
							
							PrintWriter	pw = new PrintWriter( sw );
									
							IndentWriter iw = new IndentWriter( pw );
							
							NetworkAdmin admin = NetworkAdmin.getSingleton();
							
							admin.generateDiagnostics( iw );
							
							pw.close();
							
							final String	info = sw.toString();
							
							Logger.log( new LogEvent(LOGID, "Network Info:\n" + info));

							Utils.execSWTThread(
								new Runnable()
								{
									public void
									run()
									{
										ClipboardCopy.copyToClipBoard( info );
									}
								});
						}
					}.start();
				}
			});
    
	// stats
	
	Label generate_stats_info = new Label(gLogging, SWT.NULL);
	Messages.setLanguageText(generate_stats_info, CFG_PREFIX + "statsinfo");

	Button generate_stats_button = new Button(gLogging, SWT.PUSH);
	Messages.setLanguageText(generate_stats_button, CFG_PREFIX + "generatediagnostics");

	
	generate_stats_button.addListener(
			SWT.Selection, 
			new Listener() 
			{
				public void 
				handleEvent(Event event) 
				{
					java.util.Set	types = new HashSet();
					
					types.add( AzureusCoreStats.ST_ALL );
					
					Map	reply = AzureusCoreStats.getStats( types );
					
					Iterator	it = reply.entrySet().iterator();
					
					StringBuffer buffer = new StringBuffer(16000);
					
					while( it.hasNext()){
						
						Map.Entry	entry = (Map.Entry)it.next();
						
						buffer.append( entry.getKey() + " -> " + entry.getValue() + "\r\n" );
					}
					
					String	str = buffer.toString();
					
					ClipboardCopy.copyToClipBoard( str );

					Logger.log( new LogEvent(LOGID, "Stats Info:\n" + str));
				}
			});

        // diagnostics

    
	Label generate_info = new Label(gLogging, SWT.NULL);

	Messages.setLanguageText(generate_info, CFG_PREFIX + "generatediagnostics.info");

	Button generate_button = new Button(gLogging, SWT.PUSH);

	Messages.setLanguageText(generate_button, CFG_PREFIX + "generatediagnostics");

	generate_button.addListener(
			SWT.Selection, 
			new Listener() 
			{
				public void 
				handleEvent(Event event) 
				{
					StringWriter sw = new StringWriter();
					
					PrintWriter	pw = new PrintWriter( sw );
					
					AEDiagnostics.generateEvidence( pw );
					
					pw.close();
					
					String	evidence = sw.toString();
					
					ClipboardCopy.copyToClipBoard( evidence );

					Logger.log( new LogEvent(LOGID, "Evidence Generation:\n" + evidence));
				}
			});
	
	if ( false ){
		Button test_button = new Button(gLogging, SWT.PUSH);
	
		test_button.setText( "Test" );
	
		test_button.addListener(
				SWT.Selection, 
				new Listener() 
				{
					public void 
					handleEvent(Event event) 
					{
						try{
							PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getDefaultPluginInterface();
							
							UpdateInstaller installer = pi.getUpdateManager().createInstaller();
						
							installer.addMoveAction(
								"C:\\temp\\file1", "C:\\temp\\file2" );
						
							installer.installNow(
								new UpdateInstallerListener()
								{
									public void
									reportProgress(
										String		str )
									{
										System.out.println( str );
									}
									
									public void
									complete()
									{
										System.out.println( "complete" );
									}
									
									public void
									failed(
										UpdateException	e )
									{
										System.out.println( "failed" );
										
										e.printStackTrace();
										
									}
								});
							
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					}
				});
	}
	
    return gLogging;
  }
}
