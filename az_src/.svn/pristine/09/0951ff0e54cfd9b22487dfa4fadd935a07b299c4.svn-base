package org.gudy.azureus2.ui.swt.views.configsections;

import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.views.stats.TransferStatsView;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.Constants;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.speedmanager.SpeedManager;
import com.aelitis.azureus.core.speedmanager.SpeedManagerLimitEstimate;
import com.aelitis.azureus.core.speedmanager.SpeedManagerListener;
import com.aelitis.azureus.core.speedmanager.impl.SpeedManagerImpl;


/**
 * Created on Jun 13, 2007
 * Created by Alan Snyder
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 * <p/>
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
 * <p/>
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

public class ConfigSectionTransferAutoSpeedSelect
    implements UISWTConfigSection
{

    private final String CFG_PREFIX = "ConfigView.section.transfer.autospeed.";

    StringListParameter versionList;

    BooleanParameter enableAutoSpeed;
    BooleanParameter enableAutoSpeedWhileSeeding;

    /**
     * Returns section you want your configuration panel to be under.
     * See SECTION_* constants.  To add a subsection to your own ConfigSection,
     * return the configSectionGetName result of your parent.<br>
     */
    public String configSectionGetParentSection() {
        return ConfigSection.SECTION_TRANSFER;
    }

    /**
     * In order for the plugin to display its section correctly, a key in the
     * Plugin language file will need to contain
     * <TT>ConfigView.section.<i>&lt;configSectionGetName() result&gt;</i>=The Section name.</TT><br>
     *
     * @return The name of the configuration section
     */
    public String configSectionGetName() {
        return "transfer.select";
    }

    /**
     * User selected Save.
     * All saving of non-plugin tabs have been completed, as well as
     * saving of plugins that implement org.gudy.azureus2.plugins.ui.config
     * parameters.
     */
    public void configSectionSave() {

    }

    /**
     * Config view is closing
     */
    public void configSectionDelete() {

    }
    
	public int maxUserMode() {
		return 0;
	}


    /**
     * Create your own configuration panel here.  It can be anything that inherits
     * from SWT's Composite class.
     * Please be mindfull of small screen resolutions
     *
     * @param parent The parent of your configuration panel
     * @return your configuration panel
     */

    public Composite configSectionCreate(final Composite parent) {

        GridData gridData;

        Composite cSection = new Composite(parent, SWT.NULL);
        
        if (!AzureusCoreFactory.isCoreRunning()) {
        	cSection.setLayout(new FillLayout());
        	Label lblNotAvail = new Label(cSection, SWT.WRAP);
        	Messages.setLanguageText(lblNotAvail, "core.not.available");
        	return cSection;
        }
        
        gridData = new GridData(GridData.VERTICAL_ALIGN_FILL|GridData.HORIZONTAL_ALIGN_FILL);
        cSection.setLayoutData(gridData);
        GridLayout subPanel = new GridLayout();
        subPanel.numColumns = 3;
        cSection.setLayout(subPanel);

        //V1, V2 ... drop down.

        //enable auto-speed beta
        ///////////////////////////////////
        // AutoSpeed Beta mode group
        ///////////////////////////////////
        //Beta-mode grouping.
        Group modeGroup = new Group(cSection, SWT.NULL);
        Messages.setLanguageText(modeGroup,"ConfigTransferAutoSpeed.algorithm.selector");
        GridLayout modeLayout = new GridLayout();
        modeLayout.numColumns = 3;
        modeGroup.setLayout(modeLayout);
        
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        modeGroup.setLayoutData(gridData);

        //Need a drop down to select which method will be used.
        Label label = new Label(modeGroup, SWT.NULL);
        Messages.setLanguageText(label,"ConfigTransferAutoSpeed.algorithm");
        gridData = new GridData();
        label.setLayoutData(gridData);

        String AutoSpeedClassic = MessageText.getString("ConfigTransferAutoSpeed.auto.speed.classic");
        String AutoSpeedBeta = MessageText.getString("ConfigTransferAutoSpeed.auto.speed.beta");
        String AutoSpeedNeural = MessageText.getString("ConfigTransferAutoSpeed.auto.speed.neural");
        
        String[] modeNames = {
                AutoSpeedClassic,
                AutoSpeedBeta,
                AutoSpeedNeural,
               };
        
        String[] modes = {
                "1",
                "2",
                "3",
        };

        //versionList = new StringListParameter(modeGroup,
        //        SpeedManagerImpl.CONFIG_VERSION_STR,
        //        "1",
        //        modeNames,modes,true);
        versionList = new StringListParameter(modeGroup,SpeedManagerImpl.CONFIG_VERSION_STR, modeNames, modes);
        long verNum = COConfigurationManager.getLongParameter( SpeedManagerImpl.CONFIG_VERSION );
        if( verNum==1 ){
            //SpeedManagerAlgorithmProviderV1
            versionList.setValue(modes[0]);
        }else if( verNum==2 ){
            //SpeedManagerAlgorithmProviderV2
            versionList.setValue(modes[1]);
        }else if( verNum==3 ){
            //SpeedManagerAlgorithmProviderV2
            versionList.setValue(modes[2]);
        }else{
            //Default is currently version ...V1.
            versionList.setValue(modes[0]);
            //ToDo: log this condition.
        }

        versionList.addChangeListener( new ConvertToLongChangeListener() );


        //spacer
        Label spacer = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=3;
        spacer.setLayoutData(gridData);

        //To enable the beta.
        gridData = new GridData();
        gridData.horizontalIndent = 20;
        gridData.horizontalSpan = 2;
        enableAutoSpeed = new BooleanParameter(modeGroup,
                TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY,CFG_PREFIX+"enableauto");
        enableAutoSpeed.setLayoutData(gridData);

        //enableAutoSpeed.addChangeListener( new GroupModeChangeListener() );

        spacer = new Label(modeGroup, SWT.NULL);

        //AutoSpeed while seeding enabled.
        enableAutoSpeedWhileSeeding = new BooleanParameter(modeGroup,
                "Auto Upload Speed Seeding Enabled",CFG_PREFIX+"enableautoseeding");
        gridData = new GridData();
        gridData.horizontalIndent = 20;
        gridData.horizontalSpan = 2;
        enableAutoSpeedWhileSeeding.setLayoutData(gridData);

		enableAutoSpeed.setAdditionalActionPerformer(
	    		new ChangeSelectionActionPerformer( enableAutoSpeedWhileSeeding.getControls(), true ));       


        spacer = new Label(modeGroup, SWT.NULL);

  	  	spacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=3;
        spacer.setLayoutData(gridData);

        	// NETWORK GROUP
        
        Group networkGroup = new Group(cSection, SWT.NULL);
        //networkGroup.addControlListener(new Utils.LabelWrapControlListener());

        Messages.setLanguageText(networkGroup,CFG_PREFIX+"networks");    
        GridLayout networksLayout = new GridLayout();
        networksLayout.numColumns = 5;
        networkGroup.setLayout(networksLayout);

        gridData = new GridData(GridData.FILL_HORIZONTAL);
        networkGroup.setLayoutData(gridData);

        	// asn
        
        label = new Label(networkGroup, SWT.NULL);
  	  	Messages.setLanguageText(label,"SpeedView.stats.asn");    
		
        final Label asn_label = new Label(networkGroup, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 4;
        gridData.grabExcessHorizontalSpace = true;
        asn_label.setLayoutData(gridData);

        	// up cap
        
        label = new Label(networkGroup, SWT.NULL);
  	  	Messages.setLanguageText(label,"SpeedView.stats.estupcap");    
        gridData = new GridData();
        gridData.horizontalIndent = 20;
        label.setLayoutData(gridData);

        final Label up_cap = new Label(networkGroup, SWT.NULL);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 4;
        up_cap.setLayoutData(gridData);

        	// down cap

        label = new Label(networkGroup, SWT.NULL);
  	  	Messages.setLanguageText(label,"SpeedView.stats.estdowncap");    
        gridData = new GridData();
        gridData.horizontalIndent = 20;
        label.setLayoutData(gridData);
        
        final Label down_cap = new Label(networkGroup, SWT.NULL);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 4;
        down_cap.setLayoutData(gridData);

        // Core avail: We check at top
        final SpeedManager sm = AzureusCoreFactory.getSingleton().getSpeedManager();  
        
        final TransferStatsView.limitToTextHelper	limit_to_text = new TransferStatsView.limitToTextHelper();
        
        asn_label.setText( sm.getASN());
        up_cap.setText( limit_to_text.getLimitText( sm.getEstimatedUploadCapacityBytesPerSec()));
        down_cap.setText( limit_to_text.getLimitText( sm.getEstimatedDownloadCapacityBytesPerSec()));
       
        	// space
        
 	  	spacer = new Label(networkGroup, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=5;
        spacer.setLayoutData(gridData);
        
        	// info
        
	    Label info_label = new Label(networkGroup, SWT.WRAP );
	    Messages.setLanguageText(
	    		info_label, CFG_PREFIX + "network.info",
	    		new String[]{ DisplayFormatters.getRateUnit( DisplayFormatters.UNIT_KB )});
	    info_label.setLayoutData(Utils.getWrappableLabelGridData(5, 0));
        
	    	// up set
	    
	    label = new Label(networkGroup, SWT.NULL);
	    Messages.setLanguageText(label,"SpeedView.stats.estupcap");    
	    gridData = new GridData();
	    gridData.horizontalIndent = 20;
	    label.setLayoutData(gridData);

        String co_up		= "AutoSpeed Network Upload Speed (temp)";
        String co_up_type 	= "AutoSpeed Network Upload Speed Type (temp)";

        SpeedManagerLimitEstimate up_lim = sm.getEstimatedUploadCapacityBytesPerSec();
        
        COConfigurationManager.setParameter( co_up, up_lim.getBytesPerSec()/1024 );
		COConfigurationManager.setParameter( co_up_type, limit_to_text.getSettableType( up_lim ));
		
		final IntParameter max_upload = new IntParameter(networkGroup, co_up );
	    	
		final Label upload_bits = new Label(networkGroup, SWT.NULL);
	    gridData = new GridData();
	    upload_bits.setLayoutData(gridData);
	    upload_bits.setText(getMBitLimit(limit_to_text,(up_lim.getBytesPerSec()/1024)*1024));
		
		final StringListParameter max_upload_type = 
			new StringListParameter(networkGroup, co_up_type, limit_to_text.getSettableTypes(),limit_to_text.getSettableTypes() );
			
		max_upload_type.addChangeListener( 
			new ParameterChangeAdapter()
			{
				public void 
				parameterChanged(
					Parameter 	p, 
					boolean 	caused_internally )
				{
					if ( max_upload_type.isDisposed()){
					
						return;
					}
					
					float type = limit_to_text.textToType( max_upload_type.getValue());
							
					SpeedManagerLimitEstimate existing = sm.getEstimatedUploadCapacityBytesPerSec();
					
					if ( existing.getEstimateType() != type ){
					
						sm.setEstimatedUploadCapacityBytesPerSec( existing.getBytesPerSec(), type );
					}
				}
			});
			
		max_upload.addChangeListener(
			new ParameterChangeAdapter()
			{
				public void 
				parameterChanged(
					Parameter 	p, 
					boolean 	caused_internally )
				{
					if ( max_upload.isDisposed()){
					
						return;
					}
										
					int	value = max_upload.getValue() * 1024;
								
					SpeedManagerLimitEstimate existing = sm.getEstimatedUploadCapacityBytesPerSec();
					
					if ( existing.getBytesPerSec() != value ){
					
						sm.setEstimatedUploadCapacityBytesPerSec( value, existing.getEstimateType());
					}
				}
			});			
		
	    label = new Label(networkGroup, SWT.NULL);

	    	// down set
	    
	    label = new Label(networkGroup, SWT.NULL);
	    Messages.setLanguageText(label,"SpeedView.stats.estdowncap");    
	    gridData = new GridData();
	    gridData.horizontalIndent = 20;
	    label.setLayoutData(gridData);

        SpeedManagerLimitEstimate down_lim = sm.getEstimatedDownloadCapacityBytesPerSec();

        String co_down			= "AutoSpeed Network Download Speed (temp)";
		String co_down_type 	= "AutoSpeed Network Download Speed Type (temp)";
 
        COConfigurationManager.setParameter( co_down, down_lim.getBytesPerSec()/1024 );
        COConfigurationManager.setParameter( co_down_type, limit_to_text.getSettableType( down_lim ));

		final IntParameter max_download = new IntParameter(networkGroup, co_down );
	    
		final Label download_bits = new Label(networkGroup, SWT.NULL);
	    gridData = new GridData();
	    download_bits.setLayoutData(gridData);
	    download_bits.setText(getMBitLimit(limit_to_text,(down_lim.getBytesPerSec()/1024)*1024));
	    
		final StringListParameter max_download_type = 
			new StringListParameter(networkGroup, co_down_type, limit_to_text.getSettableTypes(),limit_to_text.getSettableTypes() );

		max_download_type.addChangeListener( 
			new ParameterChangeAdapter()
			{
				public void 
				parameterChanged(
					Parameter 	p, 
					boolean 	caused_internally )
				{
					if ( max_download_type.isDisposed()){
					
						return;
					}
					
					float type = limit_to_text.textToType( max_download_type.getValue());
							
					SpeedManagerLimitEstimate existing = sm.getEstimatedDownloadCapacityBytesPerSec();
					
					if ( existing.getEstimateType() != type ){
					
						sm.setEstimatedDownloadCapacityBytesPerSec( existing.getBytesPerSec(), type );
					}
				}
			});
		
		max_download.addChangeListener(
			new ParameterChangeAdapter()
			{
				public void 
				parameterChanged(
					Parameter 	p, 
					boolean 	caused_internally )
				{
					if ( max_download.isDisposed()){
					
						return;
					}
										
					int	value = max_download.getValue() * 1024;
								
					SpeedManagerLimitEstimate existing = sm.getEstimatedDownloadCapacityBytesPerSec();
					
					if ( existing.getBytesPerSec() != value ){
					
						sm.setEstimatedDownloadCapacityBytesPerSec( value, existing.getEstimateType());
					}
				}
			});
		
	    label = new Label(networkGroup, SWT.NULL);

	    	// reset
	    
	    Label reset_label = new Label(networkGroup, SWT.NULL );
	    Messages.setLanguageText(reset_label, CFG_PREFIX + "resetnetwork");

	    Button reset_button = new Button(networkGroup, SWT.PUSH);

	    Messages.setLanguageText(reset_button, CFG_PREFIX + "reset.button" );

	    reset_button.addListener(SWT.Selection, 
    		new Listener() 
			{
		        public void 
				handleEvent(Event event) 
		        {
		        	sm.reset();
		        }
		    });
        
	   sm.addListener(
           	new SpeedManagerListener()
           	{
           		private final SpeedManagerListener	listener = this;
           		
           		public void 
           		propertyChanged(
           			final int property )  
           		{
           			Utils.execSWTThread(
           				new Runnable()
           				{
           					public void
           					run()
           					{
   			        			if ( asn_label.isDisposed()){
   			        				
   			        				sm.removeListener( listener );
   			        				
   			        			}else{
   			        				
   			        				if ( property == SpeedManagerListener.PR_ASN ){
   			        					
   			        					asn_label.setText( sm.getASN());
   			        					
   				       				}else if ( property == SpeedManagerListener.PR_UP_CAPACITY ){
   				        					
   				       					SpeedManagerLimitEstimate limit = sm.getEstimatedUploadCapacityBytesPerSec();
   				       					
   				        				up_cap.setText( limit_to_text.getLimitText( limit ));
   				        					
   				        				upload_bits.setText(getMBitLimit(limit_to_text, limit.getBytesPerSec()));
   				        				
   				        				max_upload.setValue( limit.getBytesPerSec()/1024 );
   				        				
   							        	max_upload_type.setValue( limit_to_text.getSettableType( limit ));
   							        	
   				       				}else if ( property == SpeedManagerListener.PR_DOWN_CAPACITY ){
   				    					
   				       					SpeedManagerLimitEstimate limit = sm.getEstimatedDownloadCapacityBytesPerSec();

   				    					down_cap.setText( limit_to_text.getLimitText( limit ));
   				    					
   				    					download_bits.setText(getMBitLimit(limit_to_text, limit.getBytesPerSec()));
   				        				
   				        				max_download.setValue( limit.getBytesPerSec()/1024 );
   				        				
   							        	max_download_type.setValue( limit_to_text.getSettableType( limit ));
   						
   			        				}
   			        			}
           					}
           				});
           		}
           	});
	           
        //Add listeners to disable setting when needed.
                

	      //spacer
       
	   spacer = new Label(cSection, SWT.NULL);
	   gridData = new GridData();
	   gridData.horizontalSpan=3;
	   spacer.setLayoutData(gridData);
       
		BooleanParameter debug_au = new BooleanParameter(
				cSection, "Auto Upload Speed Debug Enabled",
				CFG_PREFIX + "enabledebug" );
		gridData = new GridData();
		gridData.horizontalSpan = 3;
		debug_au.setLayoutData(gridData);	
		
        //spacer
        
        spacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=3;
        spacer.setLayoutData(gridData);

        /////////////////////////////////////////
        //Add group to link to Azureus Wiki page.
        /////////////////////////////////////////
        Group azWiki = new Group(cSection, SWT.WRAP);
        gridData = new GridData();
        azWiki.setLayoutData(gridData);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 1;
        layout.marginWidth = 20;
        azWiki.setLayout(layout);

        azWiki.setText(MessageText.getString("Utils.link.visit"));

        final Label linkLabel = new Label(azWiki, SWT.NULL);
        linkLabel.setText( Constants.APP_NAME + " Wiki AutoSpeed (beta)" );
        linkLabel.setData("http://wiki.vuze.com/w/Auto_Speed");
        linkLabel.setCursor(linkLabel.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        linkLabel.setForeground(Colors.blue);
        gridData = new GridData();
        linkLabel.setLayoutData( gridData );
	    linkLabel.addMouseListener(new MouseAdapter() {
	      public void mouseDoubleClick(MouseEvent arg0) {
	      	Utils.launch((String) ((Label) arg0.widget).getData());
	      }
	      public void mouseUp(MouseEvent arg0) {
	      	Utils.launch((String) ((Label) arg0.widget).getData());
	      }
	    });


        return cSection;
    }//configSectionCreate

    class ConvertToLongChangeListener implements ParameterChangeListener{

        public void parameterChanged(Parameter p, boolean caused_internally) {

            try{
                //StringList doesn't work with Long parameters, so need to convert here.
                String str = COConfigurationManager.getStringParameter(SpeedManagerImpl.CONFIG_VERSION_STR);
                long asLong = Long.parseLong( str );
                COConfigurationManager.setParameter(SpeedManagerImpl.CONFIG_VERSION, asLong );
            }catch(Throwable t){
                //ToDo: log an error.
                COConfigurationManager.setParameter(SpeedManagerImpl.CONFIG_VERSION, 1);
            }

        }

        /**
         * An int parameter is about to change.
         * <p/>
         * Not called when parameter set via COConfigurationManager.setParameter
         *
         * @param p -
         * @param toValue -
         */
        public void intParameterChanging(Parameter p, int toValue) {

        }

        /**
         * A boolean parameter is about to change.
         * <p/>
         * Not called when parameter set via COConfigurationManager.setParameter
         *
         * @param p -
         * @param toValue -
         */
        public void booleanParameterChanging(Parameter p, boolean toValue) {

        }

        /**
         * A String parameter is about to change.
         * <p/>
         * Not called when parameter set via COConfigurationManager.setParameter
         *
         * @param p -
         * @param toValue -
         */
        public void stringParameterChanging(Parameter p, String toValue) {

        }

        /**
         * A double/float parameter is about to change.
         * <p/>
         * Not called when parameter set via COConfigurationManager.setParameter
         *
         * @param owner -
         * @param toValue -
         */
        public void floatParameterChanging(Parameter owner, double toValue) {
        }
    }

    protected String
    getMBitLimit(
    	TransferStatsView.limitToTextHelper		helper,
    	long 									value )
    {
    	return("("+(value==0?helper.getUnlimited():DisplayFormatters.formatByteCountToBitsPerSec( value ))+")" );
    }
}
