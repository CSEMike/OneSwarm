/*
 * Created on Sep 13, 2004
 * Created by Olivier Chalouhi
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.ui.swt.views.stats;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.graphics.SpeedGraphic;
import org.gudy.azureus2.ui.swt.views.AbstractIView;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.control.DHTControlActivity;
import com.aelitis.azureus.core.dht.control.DHTControlListener;
import com.aelitis.azureus.core.dht.control.DHTControlStats;
import com.aelitis.azureus.core.dht.db.DHTDBStats;
import com.aelitis.azureus.core.dht.nat.DHTNATPuncher;
import com.aelitis.azureus.core.dht.router.DHTRouterStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportFullStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportStats;
import com.aelitis.azureus.plugins.dht.DHTPlugin;

/**
 * 
 */
public class DHTView extends AbstractIView {
  
  public static final int DHT_TYPE_MAIN 	= DHT.NW_MAIN;
  public static final int DHT_TYPE_CVS  	= DHT.NW_CVS;
  public static final int DHT_TYPE_MAIN_V6 	= DHT.NW_MAIN_V6;

  DHT dht;
  
  Composite panel;
  
  String	yes_str;
  String	no_str;
  
  Label lblUpTime,lblNumberOfUsers;
  Label lblNodes,lblLeaves;
  Label lblContacts,lblReplacements,lblLive,lblUnknown,lblDying;
  Label lblRendezvous, lblReachable;
  Label lblKeys,lblValues;
  Label lblLocal,lblDirect,lblIndirect;
  Label lblDivFreq,lblDivSize;
  
  Label lblReceivedPackets,lblReceivedBytes;
  Label lblSentPackets,lblSentBytes;
    
  
  Label lblPings[] = new Label[4];
  Label lblFindNodes[] = new Label[4];
  Label lblFindValues[] = new Label[4];
  Label lblStores[] = new Label[4];
  Label lblData[] = new Label[4];
    
  Canvas  in,out;  
  SpeedGraphic inGraph,outGraph;
  
  boolean activityChanged;
  DHTControlListener controlListener;
  Table activityTable;
  DHTControlActivity[] activities;
  
  private final int dht_type;
  

  public DHTView( int dht_type ) {
    this.dht_type = dht_type;
    init();
  }
  
  private void init() {
    try {
      PluginInterface dht_pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );
        
      if ( dht_pi == null ){
      	   
    	  return;
      }
         
      DHT[] dhts = ((DHTPlugin)dht_pi.getPlugin()).getDHTs();
      
      for (int i=0;i<dhts.length;i++){
    	  if ( dhts[i].getTransport().getNetwork() == dht_type ){
    		  dht = dhts[i];
    		  break;
    	  }
      }
	  
      if ( dht == null ){
    	  
    	  return;
      }
      
      controlListener = new DHTControlListener() {
        public void activityChanged(DHTControlActivity activity,int type) {
          activityChanged = true;
        }                
      };
      dht.getControl().addListener(controlListener);
      
    } catch(Exception e) {
      Debug.printStackTrace( e );
    }
  }
  
  public void initialize(Composite composite) {
    panel = new Composite(composite,SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    panel.setLayout(layout);
    
    yes_str = MessageText.getString( "Button.yes").replaceAll("&", "");
    no_str 	= MessageText.getString( "Button.no").replaceAll("&", "");
    
    initialiseGeneralGroup();
    initialiseDBGroup();
    
    initialiseTransportDetailsGroup();
    initialiseOperationDetailsGroup();
    
    initialiseActivityGroup();
  }
  
  private void initialiseGeneralGroup() {
    Group gGeneral = new Group(panel,SWT.NONE);
    Messages.setLanguageText(gGeneral, "DHTView.general.title" );
    
    GridData data = new GridData();
    data.verticalAlignment = SWT.BEGINNING;
    data.widthHint = 350;
    gGeneral.setLayoutData(data);
    
    GridLayout layout = new GridLayout();
    layout.numColumns = 6;
    gGeneral.setLayout(layout);
    
    
    Label label = new Label(gGeneral,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.general.uptime");    
    
    lblUpTime = new Label(gGeneral,SWT.NONE);
    lblUpTime.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gGeneral,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.general.users");    
    
    lblNumberOfUsers = new Label(gGeneral,SWT.NONE);
    lblNumberOfUsers.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gGeneral,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.general.reachable");    

    lblReachable = new Label(gGeneral,SWT.NONE);
    lblReachable.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gGeneral,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.general.nodes");    
    
    lblNodes = new Label(gGeneral,SWT.NONE);
    lblNodes.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gGeneral,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.general.leaves");    
    
    lblLeaves = new Label(gGeneral,SWT.NONE);
    lblLeaves.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gGeneral,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.general.rendezvous");    

    lblRendezvous = new Label(gGeneral,SWT.NONE);
    lblRendezvous.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    
    label = new Label(gGeneral,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.general.contacts");    
    
    lblContacts = new Label(gGeneral,SWT.NONE);
    lblContacts.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gGeneral,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.general.replacements");    
    
    lblReplacements = new Label(gGeneral,SWT.NONE);
    lblReplacements.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gGeneral,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.general.live");    
    
    lblLive= new Label(gGeneral,SWT.NONE);
    lblLive.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gGeneral,SWT.NONE);
    label = new Label(gGeneral,SWT.NONE);
    
    label = new Label(gGeneral,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.general.unknown");    
    
    lblUnknown = new Label(gGeneral,SWT.NONE);
    lblUnknown.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gGeneral,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.general.dying");    
    
    lblDying = new Label(gGeneral,SWT.NONE);
    lblDying.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));    
  }
  
  private void initialiseDBGroup() {
    Group gDB = new Group(panel,SWT.NONE);
    Messages.setLanguageText(gDB,"DHTView.db.title");
    
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    data.verticalAlignment = SWT.FILL;
    gDB.setLayoutData(data);
    
    GridLayout layout = new GridLayout();
    layout.numColumns = 6;    
    layout.makeColumnsEqualWidth = true;
    gDB.setLayout(layout);
    
    Label label = new Label(gDB,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.db.keys");    
    
    lblKeys = new Label(gDB,SWT.NONE);
    lblKeys.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gDB,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.db.values");    
    
    lblValues = new Label(gDB,SWT.NONE);
    lblValues.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gDB,SWT.NONE);
    label = new Label(gDB,SWT.NONE);
    
    
    label = new Label(gDB,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.db.local");    
    
    lblLocal = new Label(gDB,SWT.NONE);
    lblLocal.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gDB,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.db.direct");    
    
    lblDirect = new Label(gDB,SWT.NONE);
    lblDirect.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gDB,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.db.indirect");    
    
    lblIndirect = new Label(gDB,SWT.NONE);
    lblIndirect.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false)); 
    
    
    label = new Label(gDB,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.db.divfreq");    
    
    lblDivFreq = new Label(gDB,SWT.NONE);
    lblDivFreq.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gDB,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.db.divsize");    
    
    lblDivSize = new Label(gDB,SWT.NONE);
    lblDivSize.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
  }
  
  private void initialiseTransportDetailsGroup() {
    Group gTransport = new Group(panel,SWT.NONE);
    Messages.setLanguageText(gTransport,"DHTView.transport.title");
    
    GridData data = new GridData(GridData.FILL_VERTICAL);
    data.widthHint = 350;
    data.verticalSpan = 2;
    gTransport.setLayoutData(data);
    
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;    
    layout.makeColumnsEqualWidth = true;
    gTransport.setLayout(layout);
    
    
    Label label = new Label(gTransport,SWT.NONE);
    
    label = new Label(gTransport,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.transport.packets");
    label.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gTransport,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.transport.bytes");
    label.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gTransport,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.transport.received");
    
    lblReceivedPackets = new Label(gTransport,SWT.NONE);
    lblReceivedPackets.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    lblReceivedBytes = new Label(gTransport,SWT.NONE);
    lblReceivedBytes.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gTransport,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.transport.sent");
    
    lblSentPackets = new Label(gTransport,SWT.NONE);
    lblSentPackets.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    lblSentBytes = new Label(gTransport,SWT.NONE);
    lblSentBytes.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gTransport,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.transport.in");
    data = new GridData();
    data.horizontalSpan = 3;
    label.setLayoutData(data);
    
    
    in = new Canvas(gTransport,SWT.NO_BACKGROUND);
    data = new GridData(GridData.FILL_BOTH);
    data.horizontalSpan = 3;
    in.setLayoutData(data);
    inGraph = SpeedGraphic.getInstance();
    inGraph.initialize(in);
    
    label = new Label(gTransport,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.transport.out");
    data = new GridData();
    data.horizontalSpan = 3;
    label.setLayoutData(data);
    
    out = new Canvas(gTransport,SWT.NO_BACKGROUND);
    data = new GridData(GridData.FILL_BOTH);
    data.horizontalSpan = 3;
    out.setLayoutData(data);
    outGraph = SpeedGraphic.getInstance();
    outGraph.initialize(out);
  }
  
  private void initialiseOperationDetailsGroup() {
    Group gOperations = new Group(panel,SWT.NONE);
    Messages.setLanguageText(gOperations,"DHTView.operations.title");
    gOperations.setLayoutData(new GridData(SWT.FILL,SWT.BEGINNING,true,false));
    
    GridLayout layout = new GridLayout();
    layout.numColumns = 5;
    layout.makeColumnsEqualWidth = true;
    gOperations.setLayout(layout);
    
    
    Label label = new Label(gOperations,SWT.NONE);
    
    label = new Label(gOperations,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.operations.sent");
    label.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gOperations,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.operations.ok");
    label.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gOperations,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.operations.failed");
    label.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    label = new Label(gOperations,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.operations.received");
    label.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    
    
    label = new Label(gOperations,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.operations.ping");
    
    for(int i = 0 ; i < 4 ; i++) {
      lblPings[i] = new Label(gOperations,SWT.NONE);      
      lblPings[i].setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    }
    
    
    label = new Label(gOperations,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.operations.findNode");
    
    for(int i = 0 ; i < 4 ; i++) {
      lblFindNodes[i] = new Label(gOperations,SWT.NONE);      
      lblFindNodes[i].setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    }
    
    
    label = new Label(gOperations,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.operations.findValue");
    
    for(int i = 0 ; i < 4 ; i++) {
      lblFindValues[i] = new Label(gOperations,SWT.NONE);      
      lblFindValues[i].setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    }
    
    
    label = new Label(gOperations,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.operations.store");
    
    for(int i = 0 ; i < 4 ; i++) {
      lblStores[i] = new Label(gOperations,SWT.NONE);      
      lblStores[i].setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    }
    
    label = new Label(gOperations,SWT.NONE);
    Messages.setLanguageText(label,"DHTView.operations.data");
    
    for(int i = 0 ; i < 4 ; i++) {
      lblData[i] = new Label(gOperations,SWT.NONE);      
      lblData[i].setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
    }
  }
  
  private void initialiseActivityGroup() {
    Group gActivity = new Group(panel,SWT.NONE);
    Messages.setLanguageText(gActivity,"DHTView.activity.title");
    gActivity.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
    gActivity.setLayout(new GridLayout());
    
    activityTable = new Table(gActivity,SWT.VIRTUAL | SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
    activityTable.setLayoutData(new GridData(GridData.FILL_BOTH));
    
    final TableColumn colStatus =  new TableColumn(activityTable,SWT.LEFT);
    Messages.setLanguageText(colStatus,"DHTView.activity.status");
    colStatus.setWidth(80);
    
    final TableColumn colType =  new TableColumn(activityTable,SWT.LEFT);
    Messages.setLanguageText(colType,"DHTView.activity.type");
    colType.setWidth(80);
    
    final TableColumn colName =  new TableColumn(activityTable,SWT.LEFT);
    Messages.setLanguageText(colName,"DHTView.activity.target");
    colName.setWidth(80);
    
    final TableColumn colDetails =  new TableColumn(activityTable,SWT.LEFT);
    Messages.setLanguageText(colDetails,"DHTView.activity.details");
    colDetails.setWidth(300);
    colDetails.setResizable(false);
    
    
    activityTable.setHeaderVisible(true);
    Listener computeLastRowWidthListener = new Listener() {
    	// inUse flag to prevent a SWT stack overflow.  For some reason
    	// the setWidth call was triggering a resize.
    	boolean inUse = false;
      public void handleEvent(Event event) {
      	if (inUse) {
      		return;
      	}

      	inUse = true;
       	try {
          if(activityTable == null || activityTable.isDisposed()) return;
          int totalWidth = activityTable.getClientArea().width;
          int remainingWidth = totalWidth 
                                 - colStatus.getWidth()
                                 - colType.getWidth()
                                 - colName.getWidth();
          if(remainingWidth > 0)
            colDetails.setWidth(remainingWidth);

       	} finally {
      		inUse = false;
      	}
      }
    };
    activityTable.addListener(SWT.Resize, computeLastRowWidthListener);    
    colStatus.addListener(SWT.Resize,computeLastRowWidthListener);
    colType.addListener(SWT.Resize,computeLastRowWidthListener);
    colName.addListener(SWT.Resize,computeLastRowWidthListener);
    
    activityTable.addListener(SWT.SetData, new Listener() {
      public void handleEvent(Event event) {
        TableItem item = (TableItem) event.item;
        int index = activityTable.indexOf (item);
        item.setText (0,MessageText.getString("DHTView.activity.status." + activities[index].isQueued()));
        item.setText (1,MessageText.getString("DHTView.activity.type." + activities[index].getType()));
        item.setText (2,ByteFormatter.nicePrint(activities[index].getTarget()));
        item.setText (3,activities[index].getDescription());
      }
    });
    
  }
  

  public void delete() {
    Utils.disposeComposite(panel);
    if (dht != null) {
      dht.getControl().removeListener(controlListener);
    }
    outGraph.dispose();
    inGraph.dispose();
  }

  public String getFullTitle() {
	  if ( dht_type == DHT_TYPE_MAIN ){

		  return( "DHTView.title.full" );

	  }else if ( dht_type == DHT_TYPE_CVS ){

		  return( "DHTView.title.fullcvs" );
	  }else{

		  return( "DHTView.title.full_v6" );
	  }
  }
  
  public Composite getComposite() {
    return panel;
  }
  
  public void refresh() {    
    if(dht == null) { 
      init();
      return;
    }
    
    inGraph.refresh();
    outGraph.refresh();
    
    refreshGeneral();
    refreshDB();
    refreshTransportDetails();
    refreshOperationDetails();
    refreshActivity();
  }  
  
  private void refreshGeneral() {
    DHTControlStats controlStats = dht.getControl().getStats();
    DHTRouterStats routerStats = dht.getRouter().getStats();
    lblUpTime.setText(TimeFormatter.format(controlStats.getRouterUptime() / 1000));
    lblNumberOfUsers.setText("" + controlStats.getEstimatedDHTSize());
    lblReachable.setText(dht.getTransport().isReachable()?yes_str:no_str);
    
    DHTNATPuncher puncher = dht.getNATPuncher();
    
    String	puncher_str;
    
    if ( puncher == null ){
    	puncher_str = "";
    }else{
    	puncher_str = puncher.operational()?yes_str:no_str;
    }
    
    lblRendezvous.setText(dht.getTransport().isReachable()?"":puncher_str);
    long[] stats = routerStats.getStats();
    lblNodes.setText("" + stats[DHTRouterStats.ST_NODES]);
    lblLeaves.setText("" + stats[DHTRouterStats.ST_LEAVES]);
    lblContacts.setText("" + stats[DHTRouterStats.ST_CONTACTS]);
    lblReplacements.setText("" + stats[DHTRouterStats.ST_REPLACEMENTS]);
    lblLive.setText("" + stats[DHTRouterStats.ST_CONTACTS_LIVE]);
    lblUnknown.setText("" + stats[DHTRouterStats.ST_CONTACTS_UNKNOWN]);
    lblDying.setText("" + stats[DHTRouterStats.ST_CONTACTS_DEAD]);
  }
  
  private int refreshIter = 0;
  
  private void refreshDB() {    
    if(refreshIter == 0) {
	  DHTDBStats    dbStats = dht.getDataBase().getStats();
      lblKeys.setText("" + dbStats.getKeyCount());  
      int[] stats = dbStats.getValueDetails();
      lblValues.setText("" + stats[DHTDBStats.VD_VALUE_COUNT]);
      lblDirect.setText("" + stats[DHTDBStats.VD_DIRECT_SIZE]);
      lblIndirect.setText("" + stats[DHTDBStats.VD_INDIRECT_SIZE]);
      lblLocal.setText("" + stats[DHTDBStats.VD_LOCAL_SIZE]);
      lblDivFreq.setText("" + stats[DHTDBStats.VD_DIV_FREQ]);
      lblDivSize.setText("" + stats[DHTDBStats.VD_DIV_SIZE]);
    } else {
      refreshIter++;
      if(refreshIter == 100) refreshIter = 0;
    }

  }

  private void refreshTransportDetails() {
    DHTTransportStats   transportStats = dht.getTransport().getStats();
    lblReceivedBytes.setText(DisplayFormatters.formatByteCountToKiBEtc(transportStats.getBytesReceived()));
    lblSentBytes.setText(DisplayFormatters.formatByteCountToKiBEtc(transportStats.getBytesSent()));
    lblReceivedPackets.setText("" + transportStats.getPacketsReceived());
    lblSentPackets.setText("" + transportStats.getPacketsSent());
  }
  
  private void refreshOperationDetails() {   
    DHTTransportStats   transportStats = dht.getTransport().getStats();
    long[] pings = transportStats.getPings();
    for(int i = 0 ; i < 4 ; i++) {
      lblPings[i].setText("" + pings[i]);
    }
    
    long[] findNodes = transportStats.getFindNodes();
    for(int i = 0 ; i < 4 ; i++) {
      lblFindNodes[i].setText("" + findNodes[i]);
    }
    
    long[] findValues = transportStats.getFindValues();
    for(int i = 0 ; i < 4 ; i++) {
      lblFindValues[i].setText("" + findValues[i]);
    }
    
    long[] stores = transportStats.getStores();
    for(int i = 0 ; i < 4 ; i++) {
      lblStores[i].setText("" + stores[i]);
    }
    long[] data = transportStats.getData();
    for(int i = 0 ; i < 4 ; i++) {
      lblData[i].setText("" + data[i]);
    }
  }
  
  private void refreshActivity() {
    if(activityChanged) {
      activityChanged = false;
      activities = dht.getControl().getActivities();
      activityTable.setItemCount(activities.length);
      activityTable.clearAll();
      //Dunno if still needed?
      activityTable.redraw();  
    }    
  }
  
  public void periodicUpdate() {
    if(dht == null) return;
    
    DHTTransportFullStats fullStats = dht.getTransport().getLocalContact().getStats();
    inGraph.addIntValue((int)fullStats.getAverageBytesReceived());
    outGraph.addIntValue((int)fullStats.getAverageBytesSent());
  }
  
  public String getData() {
	  return( getFullTitle());
  }
}


