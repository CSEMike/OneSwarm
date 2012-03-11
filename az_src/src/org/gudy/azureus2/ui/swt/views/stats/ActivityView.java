/*
 * File    : StatsView.java
 * Created : 15 d�c. 2003}
 * By      : Olivier
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
package org.gudy.azureus2.ui.swt.views.stats;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.Legend;
import org.gudy.azureus2.ui.swt.components.graphics.SpeedGraphic;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;

/**
 * @author Olivier
 *
 */
public class ActivityView 
	implements ParameterListener, UISWTViewCoreEventListener
{

  public static final String MSGID_PREFIX = "SpeedView";
  
	GlobalManager manager = null;
  GlobalManagerStats stats = null;
  
  Composite panel;
  
  Canvas downSpeedCanvas;
  SpeedGraphic downSpeedGraphic;
  
  Canvas upSpeedCanvas;
  SpeedGraphic upSpeedGraphic;

	private UISWTView swtView;  
  
  public ActivityView() {
    downSpeedGraphic = SpeedGraphic.getInstance();
    upSpeedGraphic = SpeedGraphic.getInstance();
  	AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				manager = core.getGlobalManager();
				stats = manager.getStats();
			}
		});
  }
  
  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.views.stats.PeriodicViewUpdate#periodicUpdate()
   */
  public void periodicUpdate() {
  	if (manager == null || stats == null) {
  		return;
  	}
	  
	int swarms_peer_speed = (int)stats.getTotalSwarmsPeerRate(true,false);
	
    downSpeedGraphic.addIntsValue(
    	new int[]{ 	stats.getDataReceiveRate()+stats.getProtocolReceiveRate(),
    				stats.getProtocolReceiveRate(),
    				COConfigurationManager.getIntParameter("Max Download Speed KBs") * 1024,
    				swarms_peer_speed });
   
    upSpeedGraphic.addIntsValue(
    	new int[]{	stats.getDataSendRate()+stats.getProtocolSendRate(),
    				stats.getProtocolSendRate(),
    				COConfigurationManager.getIntParameter(TransferSpeedValidator.getActiveUploadParameter( manager )) * 1024,
    				swarms_peer_speed });
  }
  
  private void initialize(Composite composite) {
    panel = new Composite(composite,SWT.NULL);
    panel.setLayout(new GridLayout());
    GridData gridData;
        
    Group gDownSpeed = new Group(panel,SWT.NULL);
    Messages.setLanguageText(gDownSpeed,"SpeedView.downloadSpeed.title");
    gridData = new GridData(GridData.FILL_BOTH);
    gDownSpeed.setLayoutData(gridData);    
    gDownSpeed.setLayout(new GridLayout());
    
    downSpeedCanvas = new Canvas(gDownSpeed,SWT.NO_BACKGROUND);
    gridData = new GridData(GridData.FILL_BOTH);
    downSpeedCanvas.setLayoutData(gridData);
    downSpeedGraphic.initialize(downSpeedCanvas);
    Color[] colors = downSpeedGraphic.colors;
    
    Group gUpSpeed = new Group(panel,SWT.NULL);
    Messages.setLanguageText(gUpSpeed,"SpeedView.uploadSpeed.title");
    gridData = new GridData(GridData.FILL_BOTH);
    gUpSpeed.setLayoutData(gridData);
    gUpSpeed.setLayout(new GridLayout());
    
    upSpeedCanvas = new Canvas(gUpSpeed,SWT.NO_BACKGROUND);
    gridData = new GridData(GridData.FILL_BOTH);
    upSpeedCanvas.setLayoutData(gridData);
    upSpeedGraphic.initialize(upSpeedCanvas);
    
    COConfigurationManager.addAndFireParameterListener("Stats Graph Dividers", this);
   
    upSpeedGraphic.setLineColors(colors);
  
	String[] colorConfigs = new String[] {
		"ActivityView.legend.peeraverage",
		"ActivityView.legend.achieved",
		"ActivityView.legend.overhead",
		"ActivityView.legend.limit",
		"ActivityView.legend.swarmaverage",
		"ActivityView.legend.trimmed"
	};

	Legend.createLegendComposite(panel, colors, colorConfigs);
  }
  
  private void delete() {    
    Utils.disposeComposite(panel);
    downSpeedGraphic.dispose();
    upSpeedGraphic.dispose();
    COConfigurationManager.removeParameterListener("Stats Graph Dividers", this);
  }

  private Composite getComposite() {
    return panel;
  }
  
  private void refresh() {
    downSpeedGraphic.refresh();
    upSpeedGraphic.refresh();
  }  
  
  public void parameterChanged(String param_name) {
	  boolean update_dividers = COConfigurationManager.getBooleanParameter("Stats Graph Dividers");
	  int update_divider_width = update_dividers ? 60 : 0;
      downSpeedGraphic.setUpdateDividerWidth(update_divider_width);
      upSpeedGraphic.setUpdateDividerWidth(update_divider_width);
  }

	public boolean eventOccurred(UISWTViewEvent event) {
    switch (event.getType()) {
      case UISWTViewEvent.TYPE_CREATE:
      	swtView = (UISWTView)event.getData();
      	swtView.setTitle(MessageText.getString(MSGID_PREFIX + ".title.full"));
        break;

      case UISWTViewEvent.TYPE_DESTROY:
        delete();
        break;

      case UISWTViewEvent.TYPE_INITIALIZE:
        initialize((Composite)event.getData());
        break;

      case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
      	Messages.updateLanguageForControl(getComposite());
        break;

      case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
        break;
        
      case UISWTViewEvent.TYPE_FOCUSGAINED:
      	break;
        
      case UISWTViewEvent.TYPE_REFRESH:
        refresh();
        break;

      case StatsView.EVENT_PERIODIC_UPDATE:
      	periodicUpdate();
      	break;
    }

    return true;
  }
}
