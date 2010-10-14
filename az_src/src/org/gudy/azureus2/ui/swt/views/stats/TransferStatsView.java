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


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.BufferedLabel;
import org.gudy.azureus2.ui.swt.components.Legend;
import org.gudy.azureus2.ui.swt.components.graphics.PingGraphic;
import org.gudy.azureus2.ui.swt.components.graphics.Plot3D;
import org.gudy.azureus2.ui.swt.components.graphics.ValueFormater;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.AbstractIView;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.speedmanager.SpeedManager;
import com.aelitis.azureus.core.speedmanager.SpeedManagerLimitEstimate;
import com.aelitis.azureus.core.speedmanager.SpeedManagerPingMapper;
import com.aelitis.azureus.core.speedmanager.SpeedManagerPingSource;
import com.aelitis.azureus.core.speedmanager.SpeedManagerPingZone;

/**
 * 
 */
public class TransferStatsView extends AbstractIView {

  GlobalManager manager;
  AzureusCore core;
  GlobalManagerStats stats;
  SpeedManager speedManager;
  
  OverallStats totalStats;
  
  Composite mainPanel;  
  
  Composite blahPanel;
  BufferedLabel asn,estUpCap,estDownCap;

  Composite generalPanel;
  BufferedLabel nowUp, nowDown, sessionDown, sessionUp, session_ratio, sessionTime, totalDown, totalUp, total_ratio, totalTime;
  
  Group autoSpeedPanel;
  StackLayout autoSpeedPanelLayout;
  Composite autoSpeedInfoPanel;
  Composite autoSpeedDisabledPanel;
  PingGraphic pingGraph;

  plotView[]	plot_views;
  zoneView[] 	zone_views;
  
  limitToTextHelper	limit_to_text = new limitToTextHelper();
  
  private final DecimalFormat formatter = new DecimalFormat( "##.#" );
  
  
  
  public TransferStatsView(GlobalManager manager,AzureusCore core) {
    this.core = core;
    this.manager = manager;
    this.stats = manager.getStats();
    this.totalStats = StatsFactory.getStats();
    
    speedManager = core.getSpeedManager();
  }
  
  public void initialize(Composite composite) {
    
    mainPanel = new Composite(composite,SWT.NULL);
    GridLayout mainLayout = new GridLayout();
    mainPanel.setLayout(mainLayout);
    
    createGeneralPanel();
    
    createBlahPanel();
    
    createAutoSpeedPanel();
  }

  private void createGeneralPanel() {
    generalPanel = new Composite(mainPanel,SWT.BORDER);
    GridData generalPanelData = new GridData(GridData.FILL_HORIZONTAL);
    generalPanel.setLayoutData(generalPanelData);
    
    GridLayout panelLayout = new GridLayout();
    panelLayout.numColumns = 5;
    panelLayout.makeColumnsEqualWidth = true;
    generalPanel.setLayout(panelLayout);
    
    GridData gridData;
    
    Label lbl = new Label(generalPanel,SWT.NULL);
    
    lbl = new Label(generalPanel,SWT.NULL);
    Messages.setLanguageText(lbl,"SpeedView.stats.downloaded");
    
    lbl = new Label(generalPanel,SWT.NULL);
    Messages.setLanguageText(lbl,"SpeedView.stats.uploaded");
    
    lbl = new Label(generalPanel,SWT.NULL);
    Messages.setLanguageText(lbl,"SpeedView.stats.ratio");
    
    lbl = new Label(generalPanel,SWT.NULL);
    Messages.setLanguageText(lbl,"SpeedView.stats.uptime");
    
    lbl = new Label(generalPanel,SWT.NULL);
    lbl = new Label(generalPanel,SWT.NULL);
    lbl = new Label(generalPanel,SWT.NULL);
    lbl = new Label(generalPanel,SWT.NULL);
    lbl = new Label(generalPanel,SWT.NULL);
    
    /////// NOW /////////
    lbl = new Label(generalPanel,SWT.NULL);
    Messages.setLanguageText(lbl,"SpeedView.stats.now");
    
    nowDown = new BufferedLabel(generalPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    nowDown.setLayoutData(gridData);
    
    nowUp = new BufferedLabel(generalPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    nowUp.setLayoutData(gridData);
    
    lbl = new Label(generalPanel,SWT.NULL);
    lbl = new Label(generalPanel,SWT.NULL);
    
    
    //////// SESSION ////////
    lbl = new Label(generalPanel,SWT.NULL);
    Messages.setLanguageText(lbl,"SpeedView.stats.session");
    sessionDown = new BufferedLabel(generalPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    sessionDown.setLayoutData(gridData);
    
    sessionUp = new BufferedLabel(generalPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    sessionUp.setLayoutData(gridData);
    
    session_ratio = new BufferedLabel(generalPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    session_ratio.setLayoutData(gridData);    
    
    sessionTime = new BufferedLabel(generalPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    sessionTime.setLayoutData(gridData);
    
    
    ///////// TOTAL ///////////
    lbl = new Label(generalPanel,SWT.NULL);
    Messages.setLanguageText(lbl,"SpeedView.stats.total");
    
    totalDown = new BufferedLabel(generalPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    totalDown.setLayoutData(gridData);
    
    totalUp = new BufferedLabel(generalPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    totalUp.setLayoutData(gridData);
    
    total_ratio = new BufferedLabel(generalPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    total_ratio.setLayoutData(gridData);
    
    totalTime = new BufferedLabel(generalPanel,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    totalTime.setLayoutData(gridData);
  }
  
  private void
  createBlahPanel()
  {
	  blahPanel = new Composite(mainPanel,SWT.NONE);
	  GridData blahPanelData = new GridData(GridData.FILL_HORIZONTAL);
	  blahPanel.setLayoutData(blahPanelData);

	  GridLayout panelLayout = new GridLayout();
	  panelLayout.numColumns = 8;
	  blahPanel.setLayout(panelLayout);


	  Label label;
	  GridData gridData;

	  label = new Label(blahPanel,SWT.NONE);
	  Messages.setLanguageText(label,"SpeedView.stats.asn");    
	  asn = new BufferedLabel(blahPanel,SWT.NONE);
	  gridData = new GridData(GridData.FILL_HORIZONTAL);
	  asn.setLayoutData(gridData);

	  label = new Label(blahPanel,SWT.NONE);
	  Messages.setLanguageText(label,"SpeedView.stats.estupcap");    
	  estUpCap = new BufferedLabel(blahPanel,SWT.NONE);
	  gridData = new GridData(GridData.FILL_HORIZONTAL);
	  estUpCap.setLayoutData(gridData);

	  label = new Label(blahPanel,SWT.NONE);
	  Messages.setLanguageText(label,"SpeedView.stats.estdowncap");    
	  estDownCap = new BufferedLabel(blahPanel,SWT.NONE);
	  gridData = new GridData(GridData.FILL_HORIZONTAL);
	  estDownCap.setLayoutData(gridData);

	  label = new Label(blahPanel,SWT.NONE);
	  label = new Label(blahPanel,SWT.NONE);
  }
  
  
  private void createAutoSpeedPanel() {
    autoSpeedPanel = new Group(mainPanel,SWT.NONE);
    GridData generalPanelData = new GridData(GridData.FILL_BOTH);
    autoSpeedPanel.setLayoutData(generalPanelData);
    Messages.setLanguageText(autoSpeedPanel,"SpeedView.stats.autospeed");
    
    
    autoSpeedPanelLayout = new StackLayout();
    autoSpeedPanel.setLayout(autoSpeedPanelLayout);
    
    autoSpeedInfoPanel = new Composite(autoSpeedPanel,SWT.NULL);
    autoSpeedInfoPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
    GridLayout layout = new GridLayout();
    layout.numColumns = 8;
    layout.makeColumnsEqualWidth = true;
    autoSpeedInfoPanel.setLayout(layout);
       
    Canvas pingCanvas = new Canvas(autoSpeedInfoPanel,SWT.NO_BACKGROUND);
    GridData gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 4;
    pingCanvas.setLayoutData(gridData);
    
    pingGraph = PingGraphic.getInstance();
    pingGraph.initialize(pingCanvas);
    
    TabFolder folder = new TabFolder(autoSpeedInfoPanel, SWT.LEFT);
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 4;
    folder.setLayoutData(gridData);
    folder.setBackground(Colors.background);
    
    ValueFormater speed_formatter = 
    	new ValueFormater()
	    {
	    	public String 
	    	format(
	    		int value) 
	    	{
	    		return( DisplayFormatters.formatByteCountToKiBEtc( value ));
	    	}
	    };
	    
    ValueFormater time_formatter = 
    	new ValueFormater()
	    {
	    	public String 
	    	format(
	    		int value) 
	    	{
	    		return( value + " ms" );
	    	}
	    };
		    
    ValueFormater[] formatters = new ValueFormater[]{ speed_formatter, speed_formatter, time_formatter };
    
    String[] labels = new String[]{ "up", "down", "ping" };
    
    SpeedManagerPingMapper[] mappers = speedManager.getMappers();
    
    plot_views	= new plotView[mappers.length];
    zone_views	= new zoneView[mappers.length];
    
    for (int i=0;i<mappers.length;i++){
    
    	SpeedManagerPingMapper mapper = mappers[i];
    	
	    TabItem plot_item = new TabItem(folder, SWT.NULL);
	    
	    plot_item.setText( "Plot " + mapper.getName());
	    
	    Canvas plotCanvas = new Canvas(folder,SWT.NO_BACKGROUND);
	    gridData = new GridData(GridData.FILL_BOTH);
	    plotCanvas.setLayoutData(gridData);
	    
	    plot_views[i] = new plotView( mapper, plotCanvas, labels, formatters );
	    
	    plot_item.setControl( plotCanvas );
	    
	    TabItem zones_item = new TabItem(folder, SWT.NULL);
	    zones_item.setText( "Zones " + mapper.getName() );
	    
	    Canvas zoneCanvas = new Canvas(folder,SWT.NO_BACKGROUND);
	    gridData = new GridData(GridData.FILL_BOTH);
	    zoneCanvas.setLayoutData(gridData);
	
	    zone_views[i] = new zoneView( mapper, zoneCanvas, labels, formatters );
	    
	    zones_item.setControl( zoneCanvas );
    }
    
    autoSpeedDisabledPanel = new Composite(autoSpeedPanel,SWT.NULL);
    autoSpeedDisabledPanel.setLayout(new GridLayout());
    Label disabled = new Label(autoSpeedDisabledPanel,SWT.NULL);
    disabled.setEnabled(false);
    Messages.setLanguageText(disabled,"SpeedView.stats.autospeed.disabled");
    disabled.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER | GridData.FILL_HORIZONTAL));
    
    SpeedManager speedManager = core.getSpeedManager();
    autoSpeedPanelLayout.topControl = speedManager.isAvailable() ? autoSpeedInfoPanel : autoSpeedDisabledPanel;
    
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 8;
    
	Legend.createLegendComposite(
			autoSpeedInfoPanel,
    		PingGraphic.colors,
    		new String[]{
        			"TransferStatsView.legend.pingaverage",        			
        			"TransferStatsView.legend.ping1",        			
        			"TransferStatsView.legend.ping2",
    				"TransferStatsView.legend.ping3" },
    		gridData );
  }
  
  public void delete() {
	Utils.disposeComposite(generalPanel);
	Utils.disposeComposite(blahPanel);
    pingGraph.dispose();
 
    for (int i=0;i<plot_views.length;i++){

    	plot_views[i].dispose();
    }

    for (int i=0;i<zone_views.length;i++){

    	zone_views[i].dispose();
    }
  }

  public String getFullTitle() {
    return MessageText.getString("SpeedView.title.full"); //$NON-NLS-1$
  }
  
  public Composite getComposite() {
    return mainPanel;
  }
  
  
  
  public void refresh() {

    refreshGeneral();
    
    refreshBlahPanel();
    
    refreshPingPanel();
    
  }

  private void refreshGeneral() {
    int now_prot_down_rate = stats.getProtocolReceiveRate();
    int now_prot_up_rate = stats.getProtocolSendRate();
    
    int now_total_down_rate = stats.getDataReceiveRate() + now_prot_down_rate;
    int now_total_up_rate = stats.getDataSendRate() + now_prot_up_rate;
    
    float now_perc_down = (float)(now_prot_down_rate *100) / (now_total_down_rate==0?1:now_total_down_rate);
    float now_perc_up = (float)(now_prot_up_rate *100) / (now_total_up_rate==0?1:now_total_up_rate);

    nowDown.setText(DisplayFormatters.formatByteCountToKiBEtcPerSec( now_total_down_rate ) +
                    "  (" + DisplayFormatters.formatByteCountToKiBEtcPerSec( now_prot_down_rate ) +
                    ", " +formatter.format( now_perc_down )+ "%)" );
    
    nowUp.setText(DisplayFormatters.formatByteCountToKiBEtcPerSec( now_total_up_rate ) +
                  "  (" + DisplayFormatters.formatByteCountToKiBEtcPerSec( now_prot_up_rate ) +
                  ", " +formatter.format( now_perc_up )+ "%)" );

    ///////////////////////////////////////////////////////////////////////

    long session_prot_received = stats.getTotalProtocolBytesReceived();
    long session_prot_sent = stats.getTotalProtocolBytesSent();
      
    long session_total_received = stats.getTotalDataBytesReceived() + session_prot_received;
    long session_total_sent = stats.getTotalDataBytesSent() + session_prot_sent;

    float session_perc_received = (float)(session_prot_received *100) / (session_total_received==0?1:session_total_received);
    float session_perc_sent = (float)(session_prot_sent *100) / (session_total_sent==0?1:session_total_sent);

    sessionDown.setText(DisplayFormatters.formatByteCountToKiBEtc( session_total_received ) +
                        "  (" + DisplayFormatters.formatByteCountToKiBEtc( session_prot_received ) +
                        ", " +formatter.format( session_perc_received )+ "%)" );
    
    sessionUp.setText(DisplayFormatters.formatByteCountToKiBEtc( session_total_sent ) +
                      "  (" + DisplayFormatters.formatByteCountToKiBEtc( session_prot_sent ) +
                      ", " +formatter.format( session_perc_sent )+ "%)" );
    
    ////////////////////////////////////////////////////////////////////////
    
    totalDown.setText(DisplayFormatters.formatByteCountToKiBEtc( totalStats.getDownloadedBytes() ));
    totalUp.setText(DisplayFormatters.formatByteCountToKiBEtc( totalStats.getUploadedBytes() ));

    sessionTime.setText( DisplayFormatters.formatETA( totalStats.getSessionUpTime() ) );
    totalTime.setText( DisplayFormatters.formatETA( totalStats.getTotalUpTime() ) );
    
    long dl_bytes = totalStats.getDownloadedBytes();
    
    long t_ratio_raw = (1000* totalStats.getUploadedBytes() / (dl_bytes==0?1:dl_bytes) );
    long s_ratio_raw = (1000* session_total_sent / (session_total_received==0?1:session_total_received) );
    
    String t_ratio = "";
    String s_ratio = "";

    String partial = String.valueOf(t_ratio_raw % 1000);
    while (partial.length() < 3) {
      partial = "0" + partial;
    }
    t_ratio = (t_ratio_raw / 1000) + "." + partial;
    
    partial = String.valueOf(s_ratio_raw % 1000);
    while (partial.length() < 3) {
      partial = "0" + partial;
    }
    s_ratio = (s_ratio_raw / 1000) + "." + partial;
    
    
    total_ratio.setText( t_ratio );
    session_ratio.setText( s_ratio );
  }  
  
  private void
  refreshBlahPanel()
  {
	  asn.setText(speedManager.getASN());

	  estUpCap.setText(limit_to_text.getLimitText(speedManager.getEstimatedUploadCapacityBytesPerSec()));

	  estDownCap.setText(limit_to_text.getLimitText(speedManager.getEstimatedDownloadCapacityBytesPerSec()));
  }
  
  private void refreshPingPanel() {
    SpeedManager speedManager = core.getSpeedManager();
    if(speedManager.isAvailable() && speedManager.isEnabled()) {
      autoSpeedPanelLayout.topControl = autoSpeedInfoPanel;
      autoSpeedPanel.layout();
   
      pingGraph.refresh();        
      for (int i=0;i<plot_views.length;i++){

    	  plot_views[i].refresh();
      }

      for (int i=0;i<zone_views.length;i++){

    	  zone_views[i].refresh();
      }

    } else {
      autoSpeedPanelLayout.topControl = autoSpeedDisabledPanel;
      autoSpeedPanel.layout();
    }  
  }
  
  public void periodicUpdate() {
    if(speedManager.isAvailable() && speedManager.isEnabled()) {
      SpeedManagerPingSource sources[] = speedManager.getPingSources();
      if(sources.length > 0) {
        int[] pings = new int[sources.length];
        for(int i = 0 ; i < sources.length ; i++) {
          pings[i] = sources[i].getPingTime();
        }
        pingGraph.addIntsValue(pings);
        
        for (int i=0;i<plot_views.length;i++){
        	
        	plot_views[i].update();
        }
        
        for (int i=0;i<zone_views.length;i++){
        	
        	zone_views[i].update();
        }
      }
    }
  }
  
  public String getData() {
    return "TransferStatsView.title.full";
  }

  protected String
  getMapperTitle(
		SpeedManagerPingMapper mapper )
  {
	  if ( mapper.isActive()){
		  
		  SpeedManagerLimitEstimate up_1 		= mapper.getEstimatedUploadLimit(false);
		  SpeedManagerLimitEstimate up_2 		= mapper.getEstimatedUploadLimit(true);
		  SpeedManagerLimitEstimate down_1 	= mapper.getEstimatedDownloadLimit(false);
		  SpeedManagerLimitEstimate down_2 	= mapper.getEstimatedDownloadLimit(true);
		  
		  return( "ul=" + DisplayFormatters.formatByteCountToKiBEtc(up_1.getBytesPerSec()) + ":" + DisplayFormatters.formatByteCountToKiBEtc(up_2.getBytesPerSec())+ 
				  ",dl=" + DisplayFormatters.formatByteCountToKiBEtc(down_1.getBytesPerSec()) + ":" + DisplayFormatters.formatByteCountToKiBEtc(down_2.getBytesPerSec()) + 
				  ",mr=" + DisplayFormatters.formatDecimal( mapper.getCurrentMetricRating(),2));
	  }
	  
	  return( "" );
  }
  
  class
  plotView
  {
	  private SpeedManagerPingMapper	mapper;
	  private Plot3D 					plotGraph;
	  
	  protected
	  plotView(
		  SpeedManagerPingMapper	_mapper,
		  Canvas					_canvas,
		  String[]					_labels,
		  ValueFormater[]			_formatters )
	  {
		  mapper	= _mapper;
		  
		  plotGraph = new Plot3D( _labels, _formatters );

		  plotGraph.initialize(_canvas);
	  }
	  
	  protected void
	  update()
	  {
		  int[][]	history = mapper.getHistory();

		  plotGraph.update(history);
		  
		  plotGraph.setTitle( getMapperTitle( mapper ));
	  }
	  
	  protected void
	  refresh()
	  {
		  plotGraph.refresh();
	  }
	  
	  protected void
	  dispose()
	  {
		  plotGraph.dispose();
	  }
  }
  
  class
  zoneView
  	implements ParameterListener
  {
	  private SpeedManagerPingMapper	mapper;
	  
	  private SpeedManagerPingZone[] zones = new SpeedManagerPingZone[0];
	  
	  private Canvas			canvas;
	  
	  private ValueFormater[] formatters;

	  private String[] labels;

	  private String	title = "";
	  
	  private int 	refresh_count;
	  private int 	graphicsUpdate;
	  private Point old_size;

	  protected Image buffer_image;

	  protected
	  zoneView(
		  SpeedManagerPingMapper		_mapper,
		  Canvas 						_canvas,
		  String[]						_labels,
		  ValueFormater[]				_formatters )
	  {
		  mapper		= _mapper;
		  canvas		= _canvas;
		  labels		= _labels;
		  formatters	= _formatters;
		  
		  COConfigurationManager.addAndFireParameterListener( "Graphics Update", this ); 
	  }

	  public void 
	  parameterChanged(
		  String name ) 
	  {
		  graphicsUpdate = COConfigurationManager.getIntParameter( name );

	  }

	  protected void
	  update()
	  {
		  zones	= mapper.getZones();
		  
		  title = getMapperTitle( mapper );
	  }
	  
	  private void
	  refresh()
	  {
		  if ( canvas.isDisposed()){

			  return;
		  }

		  Rectangle bounds = canvas.getClientArea();

		  if ( bounds.height < 30 || bounds.width  < 100 || bounds.width > 2000 || bounds.height > 2000 ){
			  
			  return;
		  }
		  
		  boolean size_changed = (old_size == null || old_size.x != bounds.width || old_size.y != bounds.height);

		  old_size = new Point(bounds.width,bounds.height);

		  refresh_count++;

		  if ( refresh_count > graphicsUpdate ){

			  refresh_count = 0;
		  }

		  if ( refresh_count == 0 || size_changed ){

			  if ( buffer_image != null && ! buffer_image.isDisposed()){

				  buffer_image.dispose();
			  }
			  
			  buffer_image = draw( bounds );
		  }

		  if ( buffer_image != null ){
			  
			  GC gc = new GC( canvas );
	
			  gc.drawImage( buffer_image, bounds.x, bounds.y );
	
			  gc.dispose();   
		  }
	  }
	  
	  private Image
	  draw(
		Rectangle	bounds )
	  {
		  final int	PAD_TOP		= 10;
		  final int	PAD_BOTTOM	= 10;
		  final int	PAD_RIGHT	= 10;
		  final int	PAD_LEFT	= 10;

		  int usable_width 	= bounds.width - PAD_LEFT - PAD_RIGHT;
		  int usable_height	= bounds.height - PAD_TOP - PAD_BOTTOM;

		  Image image = new Image( canvas.getDisplay(), bounds );

		  GC gc = new GC( image );

		  try {
		  	gc.setAntialias( SWT.ON );
		  } catch (Exception e) {
		  	// Ignore ERROR_NO_GRAPHICS_LIBRARY error or any others
		  }

		  int font_height 	= gc.getFontMetrics().getHeight();
		  int char_width 	= gc.getFontMetrics().getAverageCharWidth();


		  Color[] colours = plot_views[0].plotGraph.getColours();

		  int	max_x 		= 0;
		  int	max_y 		= 0;

		  if ( zones.length > 0 ){
			 
			  int	max_metric	= 0;
	
			  for (int i=0;i<zones.length;i++){
	
				  SpeedManagerPingZone zone = zones[i];
	
				  int	metric 	= zone.getMetric();
	
				  if ( metric > 0 ){
	
					  max_metric = Math.max( max_metric, metric );
	
					  max_x = Math.max( max_x, zone.getUploadEndBytesPerSec());
					  max_y = Math.max( max_y, zone.getDownloadEndBytesPerSec());
				  }
			  }

			  if ( max_x > 0 && max_y > 0 ){
			  
				  double x_ratio = (double)usable_width/max_x;
				  double y_ratio = (double)usable_height/max_y;
				  
				  List	texts = new ArrayList();
				  
				  for (int i=0;i<zones.length;i++){
		
					  SpeedManagerPingZone zone = zones[i];
		
					  int	metric 	= zone.getMetric();
					  int	x1		= zone.getUploadStartBytesPerSec();
					  int	y1 		= zone.getDownloadStartBytesPerSec();
					  int	x2 		= zone.getUploadEndBytesPerSec();
					  int	y2		= zone.getDownloadEndBytesPerSec();
							  
					  if ( metric > 0 ){
		
						  int	colour_index = (int)((float)metric*colours.length/max_metric);
		
						  if ( colour_index >= colours.length ){
		
							  colour_index = colours.length-1;
						  }
		
						  gc.setBackground( colours[colour_index] );
		
						  int	x 		= PAD_LEFT + (int)(x1*x_ratio);
						  int	y 		= PAD_TOP  + (int)(y1*y_ratio);
						  int	width 	= (int)Math.ceil((x2-x1+1)*x_ratio);
						  int	height	= (int)Math.ceil((y2-y1+1)*y_ratio );
						  
						  int	y_draw = usable_height + PAD_TOP + PAD_TOP - y - height;
						  
						  gc.fillRectangle( x, y_draw, width, height );
						  		
						  int	text_metric = zone.getMetric();
						  
						  String text = String.valueOf( metric );
							  
						  int	text_width = text.length()*char_width + 4;
						  
						  if ( width >= text_width && height >= font_height ){
							  

							  Rectangle text_rect = 
								new Rectangle(
										x + ((width-text_width)/2), 
										y_draw + ((height-font_height)/2), 
										text_width, font_height );
									
							  	// check for overlap with existing and delete older
							  
							  Iterator it = texts.iterator();
							  
							  while( it.hasNext()){
								  
								  Object[]	old = (Object[])it.next();
								  
								  Rectangle old_coords = (Rectangle)old[1];
								  
								  if ( old_coords.intersects( text_rect )){
									
									  it.remove();
								  }
							  }
							  
							  texts.add( new Object[]{ new Integer( text_metric ), text_rect });  
						  }
					  }
				  }
				  
				  	// only do the last 100 texts as things get a little cluttered
				  
				  int	text_num = texts.size();
				  
				  for (int i=(text_num>100?(text_num-100):0);i<text_num;i++){
					  
					  Object[]	entry = (Object[])texts.get(i);
					  
					  String	str = String.valueOf(entry[0]);
					  
					  Rectangle	rect = (Rectangle)entry[1];
					  
					  gc.drawText(str, rect.x, rect.y, SWT.DRAW_TRANSPARENT );
				  }
			  }
		  }
		  
		  	// x axis

		  int x_axis_left_x = PAD_LEFT;
		  int x_axis_left_y = usable_height + PAD_TOP;

		  int x_axis_right_x = PAD_LEFT + usable_width;
		  int x_axis_right_y = x_axis_left_y;


		  gc.drawLine( x_axis_left_x, x_axis_left_y, x_axis_right_x, x_axis_right_y );
		  gc.drawLine( usable_width, x_axis_right_y - 4, x_axis_right_x, x_axis_right_y );
		  gc.drawLine( usable_width, x_axis_right_y + 4, x_axis_right_x, x_axis_right_y );

		  for (int i=1;i<10;i++){
			  
			  int	x = x_axis_left_x + ( x_axis_right_x - x_axis_left_x )*i/10;
			  
			  gc.drawLine( x, x_axis_left_y, x, x_axis_left_y+4 );
		  }
		  
		  SpeedManagerLimitEstimate le = mapper.getEstimatedUploadLimit( false );
		  
		  if ( le != null ){
			  
			  gc.setForeground(Colors.grey );
			  
			  int[][] segs = le.getSegments();
			  
			  if ( segs.length > 0 ){
				  
				  int	max_metric 	= 0;
				  int	max_pos		= 0;
				  
				  for (int i=0;i<segs.length;i++){
					  
					  int[]	seg = segs[i];
					  
					  max_metric 	= Math.max( max_metric, seg[0] );
					  max_pos 		= Math.max( max_pos, seg[2] );
				  }
				  
				  double	metric_ratio 	= max_metric==0?1:((float)50/max_metric);
				  double	pos_ratio 		= max_pos==0?1:((float)usable_width/max_pos);
				  
				  int	prev_x	= 1;
				  int	prev_y	= 1;
				  
				  for (int i=0;i<segs.length;i++){
					  
					  int[]	seg = segs[i];
					  
					  int	next_x 	= (int)((seg[1] + (seg[2]-seg[1])/2)*pos_ratio) + 1;
					  int	next_y	= (int)((seg[0])*metric_ratio) + 1;
					  
					  gc.drawLine(
								x_axis_left_x + prev_x,
								x_axis_left_y - prev_y,
								x_axis_left_x + next_x,
								x_axis_left_y - next_y );
														
					  prev_x = next_x;
					  prev_y = next_y;
				  }
			  }
			  
			  gc.setForeground( Colors.black );
		  }
		  
		  SpeedManagerLimitEstimate[] bad_up = mapper.getBadUploadHistory();
		  
		  if ( bad_up.length > 0 ){
			  
			  gc.setLineWidth( 3 );
			  
			  gc.setForeground( Colors.red );

			  for (int i=0;i<bad_up.length;i++){
				  
				  int speed = bad_up[i].getBytesPerSec();
				  
				  int	x = max_x == 0?0:(speed * usable_width / max_x);
			  
				  gc.drawLine(
							x_axis_left_x + x,
							x_axis_left_y - 0,
							x_axis_left_x + x,
							x_axis_left_y - 10 );

			  }
			  
			  gc.setForeground( Colors.black );
			  
			  gc.setLineWidth( 1 );
		  }
		  
		  String x_text = labels[0] + " - " + formatters[0].format( max_x+1 );

		  gc.drawText( 	x_text, 
				  		x_axis_right_x - 20 - x_text.length()*char_width, 
				  		x_axis_right_y - font_height - 2, 
				  		SWT.DRAW_TRANSPARENT );

		  	// y axis

		  int y_axis_bottom_x = PAD_LEFT;
		  int y_axis_bottom_y = usable_height + PAD_TOP;

		  int y_axis_top_x 	= PAD_LEFT;
		  int y_axis_top_y 	= PAD_TOP;

		  gc.drawLine( y_axis_bottom_x, y_axis_bottom_y, y_axis_top_x, y_axis_top_y );

		  gc.drawLine( y_axis_top_x-4, y_axis_top_y+PAD_TOP,	y_axis_top_x, y_axis_top_y );
		  gc.drawLine( y_axis_top_x+4, y_axis_top_y+PAD_TOP,	y_axis_top_x, y_axis_top_y );

		  for (int i=1;i<10;i++){
			  
			  int	y = y_axis_bottom_y + ( y_axis_top_y - y_axis_bottom_y )*i/10;
			  
			  gc.drawLine( y_axis_bottom_x, y, y_axis_bottom_x-4, y );
		  }
		  
		  le = mapper.getEstimatedDownloadLimit( false );
		  
		  if ( le != null ){
			  
			  gc.setForeground(Colors.grey );
			  
			  int[][] segs = le.getSegments();
			  
			  if ( segs.length > 0 ){
				  
				  int	max_metric 	= 0;
				  int	max_pos		= 0;
				  
				  for (int i=0;i<segs.length;i++){
					  
					  int[]	seg = segs[i];
					  
					  max_metric 	= Math.max( max_metric, seg[0] );
					  max_pos 		= Math.max( max_pos, seg[2] );
				  }
				  
				  double	metric_ratio 	= max_metric==0?1:((float)50/max_metric);
				  double	pos_ratio 		= max_pos==0?1:((float)usable_height/max_pos);
				  
				  int	prev_x	= 1;
				  int	prev_y	= 1;
				  
				  for (int i=0;i<segs.length;i++){
					  
					  int[]	seg = segs[i];
					  
					  int	next_x	= (int)((seg[0])*metric_ratio) + 1;
					  int	next_y 	= (int)((seg[1] + (seg[2]-seg[1])/2)*pos_ratio) + 1;
					  
					  gc.drawLine(
							y_axis_bottom_x + prev_x,
							y_axis_bottom_y - prev_y,
							y_axis_bottom_x + next_x,
							y_axis_bottom_y - next_y );
														
					  prev_x = next_x;
					  prev_y = next_y;
				  }
			  }
			  
			  gc.setForeground( Colors.black );
		  }
		  
		  SpeedManagerLimitEstimate[] bad_down = mapper.getBadDownloadHistory();
		  
		  if ( bad_down.length > 0 ){
			  
			  gc.setForeground( Colors.red );

			  gc.setLineWidth( 3 );
			  
			  for (int i=0;i<bad_down.length;i++){
				  
				  int speed = bad_down[i].getBytesPerSec();
				  
				  int	y = max_y==0?0:( speed * usable_height / max_y );
			  
				  gc.drawLine(
						  	y_axis_bottom_x + 0,
						  	y_axis_bottom_y - y,
							y_axis_bottom_x + 10,
							y_axis_bottom_y - y );
			  }
			  
			  gc.setForeground( Colors.black );
			  
			  gc.setLineWidth( 1 );
		  }
		  
		  String	y_text = labels[1] + " - " + formatters[1].format( max_y+1 );

		  gc.drawText( y_text, y_axis_top_x+4, y_axis_top_y + 2, SWT.DRAW_TRANSPARENT );

		  gc.drawText( title, ( bounds.width - title.length()*char_width )/2, 1, SWT.DRAW_TRANSPARENT );
		  
		  gc.dispose();

		  return( image );
	  }
	  
	  protected void
	  dispose()
	  {
			if ( buffer_image != null && ! buffer_image.isDisposed()){
				
				buffer_image.dispose();
			}
			
			COConfigurationManager.removeParameterListener("Graphics Update",this);
	  }
  }
  
  public static class
  limitToTextHelper
  {
	  String	msg_text_unknown;
	  String	msg_text_estimate;
	  String	msg_text_choke_estimate;
	  String	msg_text_measured_min;
	  String	msg_text_measured;
	  String	msg_text_manual;
	  String	msg_unlimited;
	  
	  String[]	setable_types;
	  
	  public
	  limitToTextHelper()
	  {
		  msg_text_unknown			= MessageText.getString( "SpeedView.stats.unknown" );
		  msg_text_estimate			= MessageText.getString( "SpeedView.stats.estimate" );
		  msg_text_choke_estimate	= MessageText.getString( "SpeedView.stats.estimatechoke" );
		  msg_text_measured			= MessageText.getString( "SpeedView.stats.measured" );
		  msg_text_measured_min		= MessageText.getString( "SpeedView.stats.measuredmin" );
		  msg_text_manual			= MessageText.getString( "SpeedView.stats.manual" );
		  
		  msg_unlimited			= MessageText.getString( "ConfigView.unlimited" );
		  
		  setable_types =  new String[]{ "", msg_text_estimate, msg_text_measured, msg_text_manual };
	  }
	  
	  public String[]
	  getSettableTypes()
	  {
		  return( setable_types );
	  }
	  
	  public String
	  getSettableType(
		  SpeedManagerLimitEstimate limit )
	  {
		  float type = limit.getEstimateType();

		  String	text;
		  
		  if ( type == SpeedManagerLimitEstimate.TYPE_UNKNOWN){

			  text = "";
			  
		  }else if ( type == SpeedManagerLimitEstimate.TYPE_MANUAL){

			  text = msg_text_manual;

		  }else if ( type == SpeedManagerLimitEstimate.TYPE_MEASURED){

			  text = msg_text_measured;

		  }else if ( type == SpeedManagerLimitEstimate.TYPE_MEASURED_MIN){

			  text = msg_text_measured;

		  }else if ( type == SpeedManagerLimitEstimate.TYPE_CHOKE_ESTIMATED){

			  text = msg_text_estimate;

		  }else{

			  text = msg_text_estimate;
		  }
		  
		  return( text );  	  }
	  
	  public String
	  typeToText(
		float 	type )
	  {
		  String	text;
		  
		  if ( type == SpeedManagerLimitEstimate.TYPE_UNKNOWN){

			  text = msg_text_unknown;
			  
		  }else if ( type == SpeedManagerLimitEstimate.TYPE_MANUAL){

			  text = msg_text_manual;

		  }else if ( type == SpeedManagerLimitEstimate.TYPE_MEASURED){

			  text = msg_text_measured;

		  }else if ( type == SpeedManagerLimitEstimate.TYPE_MEASURED_MIN){

			  text = msg_text_measured_min;

		  }else if ( type == SpeedManagerLimitEstimate.TYPE_CHOKE_ESTIMATED){

			  text = msg_text_choke_estimate;

		  }else{

			  text = msg_text_estimate;
		  }
		  
		  return( text );  
	  }
	  
	  public float
	  textToType(
		String	text )
	  {
		  if ( text.equals( msg_text_estimate )){
			  
			  return( SpeedManagerLimitEstimate.TYPE_ESTIMATED);
			  
		  }else if ( text.equals( msg_text_choke_estimate )){
			  
			  return( SpeedManagerLimitEstimate.TYPE_CHOKE_ESTIMATED);
			  
		  }else if ( text.equals( msg_text_measured )){
			  
			  return( SpeedManagerLimitEstimate.TYPE_MEASURED);
			  
		  }else if ( text.equals( msg_text_manual )){
			  
			  return( SpeedManagerLimitEstimate.TYPE_MANUAL);
			  
		  }else{
			  
			  return( SpeedManagerLimitEstimate.TYPE_UNKNOWN);
		  }
	  }
	  
	  public String
	  getLimitText(
		 SpeedManagerLimitEstimate	limit )
	  {
		  float type = limit.getEstimateType();
		  
		  String	text = typeToText( type );
		  
		  int	l = limit.getBytesPerSec();
		  
		  if ( l == 0 ){
			  
			  return( msg_unlimited + " (" + text + ")");
			  
		  }else{
		  
			  return( DisplayFormatters.formatByteCountToKiBEtcPerSec( l ) + " (" + text + ")");
		  }
	  }
	  
	  public String
	  getUnlimited()
	  {
		  return( msg_unlimited );
	  }
  }
}

