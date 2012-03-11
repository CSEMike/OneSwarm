/*
 * Created on 20 mai 2004
 * Created by Olivier Chalouhi
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
package org.gudy.azureus2.ui.swt.updater2;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.swt.SWT;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.update.UpdatableComponent;
import org.gudy.azureus2.plugins.update.Update;
import org.gudy.azureus2.plugins.update.UpdateChecker;
import org.gudy.azureus2.plugins.update.UpdateInstaller;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderAdapter;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderFactoryImpl;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;

/**
 * @author Olivier Chalouhi
 *
 */
public class SWTUpdateChecker implements UpdatableComponent
{
  private static final LogIDs LOGID = LogIDs.GUI;

  private static final String     OSX_APP = "/" + SystemProperties.getApplicationName() + ".app";
  
  public static void
  initialize()
  {
  	PluginInitializer.getDefaultInterface().getUpdateManager().registerUpdatableComponent(new SWTUpdateChecker(),true);
  }
  
  public SWTUpdateChecker() {    
  }
  
  public void checkForUpdate(final UpdateChecker checker) {
  	try{
	    SWTVersionGetter versionGetter = new SWTVersionGetter( checker );
	    
     	boolean	update_required  = 	System.getProperty("azureus.skipSWTcheck") == null && versionGetter.needsUpdate();
    	
	    if ( update_required ){
        	    
	       	int	update_prevented_version = COConfigurationManager.getIntParameter( "swt.update.prevented.version", -1 );

	    	try{
		        URL	swt_url = SWT.class.getClassLoader().getResource("org/eclipse/swt/SWT.class");
		        
		        if ( swt_url != null ){
		        	
		        	String	url_str = swt_url.toExternalForm();
	
		        	if ( url_str.startsWith("jar:file:")){
	
		        		File jar_file = FileUtil.getJarFileFromURL(url_str);
		        		
		        		String	expected_location;
		        		
		        	    if ( Constants.isOSX ){
		        	        	  
		        	    	expected_location = checker.getCheckInstance().getManager().getInstallDir() + OSX_APP + "/Contents/Resources/Java";
		        	            
		        	    }else{ 
		        	        	  
		        	    	expected_location = checker.getCheckInstance().getManager().getInstallDir();
		        	    }
		        	    
		        	    File	expected_dir = new File( expected_location );
		        	    
		        	    File	jar_file_dir = jar_file.getParentFile();
		        	    
		        	    	// sanity check
		        	    
		        	    if ( expected_dir.exists() && jar_file_dir.exists() ){
		        	    	
		        	    	expected_dir	= expected_dir.getCanonicalFile();
		        	    	jar_file_dir	= jar_file_dir.getCanonicalFile();
		        	    	
		        	    	if ( expected_dir.equals( jar_file_dir )){
		        	    	
		        	    			// everything looks ok
		        	    		
		        	    		if ( update_prevented_version != -1 ){
		        	    			
		        	    			update_prevented_version	= -1;
		        	    			
			        	    		COConfigurationManager.setParameter( "swt.update.prevented.version", update_prevented_version );
		        	    		}
		        	    	}else{
		        	    		
		        	    			// we need to periodically remind the user there's a problem as they need to realise that
		        	    			// it is causing ALL updates (core/plugin) to fail
		        	    		
		        	    		String	alert = 
		        	    			MessageText.getString( 
		        	    					"swt.alert.cant.update", 
		        	    					new String[]{
			        	    					String.valueOf( versionGetter.getCurrentVersion()),
			        	    					String.valueOf( versionGetter.getLatestVersion()),
		        	    						jar_file_dir.toString(), 
		        	    						expected_dir.toString()});
		        	    		
		        	    		checker.reportProgress( alert );
		        	    		
		        	    		long	last_prompt = COConfigurationManager.getLongParameter( "swt.update.prevented.version.time", 0 );
		        	    		long	now			= SystemTime.getCurrentTime();
		        	    		
		        	    		boolean force = now < last_prompt || now - last_prompt > 7*24*60*60*1000;
		        	    		
		        	    		if ( !checker.getCheckInstance().isAutomatic()){
		        	    			
		        	    			force = true;
		        	    		}
		        	    		
		        		    	if ( force || update_prevented_version != versionGetter.getCurrentVersion()){
			        		    				        	    		
			        	     		Logger.log(	new LogAlert(LogAlert.REPEATABLE, LogEvent.LT_ERROR, alert ));
			        						
			        	     		update_prevented_version = versionGetter.getCurrentVersion();
			        	     		
			        	    		COConfigurationManager.setParameter( "swt.update.prevented.version", update_prevented_version );
			        	    		COConfigurationManager.setParameter( "swt.update.prevented.version.time", now );
		        		    	}
		        	    	}
		        	    }
		        	}
		        }
	    	}catch( Throwable e ){
		    	
		    	Debug.printStackTrace(e);		    	
	    	}
	    
		    if ( update_prevented_version == versionGetter.getCurrentVersion()){
			
		    	Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR, "SWT update aborted due to previously reported issues regarding its install location" ));
		    			
				checker.failed();
				
				checker.getCheckInstance().cancel();
				
				return;
		    }
	    	    	 
	      String[] mirrors = versionGetter.getMirrors();
	      
	      ResourceDownloader swtDownloader = null;
	      
          ResourceDownloaderFactory factory = ResourceDownloaderFactoryImpl.getSingleton();
          List downloaders =  new ArrayList();
          for(int i = 0 ; i < mirrors.length ; i++) {
            try {
              downloaders.add(factory.getSuffixBasedDownloader(factory.create(new URL(mirrors[i]))));
            } catch(MalformedURLException e) {
              //Do nothing
            	if (Logger.isEnabled())
								Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
										"Cannot use URL " + mirrors[i] + " (not valid)"));
            }
          }
          ResourceDownloader[] resourceDownloaders = 
            (ResourceDownloader[]) 
            downloaders.toArray(new ResourceDownloader[downloaders.size()]);
          
          swtDownloader = factory.getRandomDownloader(resourceDownloaders);
	      
	      	// get the size so its cached up
	      
	      try{
	      	swtDownloader.getSize();
	      	
	      }catch( ResourceDownloaderException e ){
	      
	      	Debug.printStackTrace( e );
	      }
	      
	      String	extra = "";
	      
	      if ( Constants.isWindows && Constants.is64Bit ){
	    	
	    	  extra = " (64-bit)";
	      }
	      
	      final Update update = 
	    	  checker.addUpdate("SWT Library for " + versionGetter.getPlatform() + extra,
		          new String[] {"SWT is the graphical library used by " + Constants.APP_NAME},
		          "" + versionGetter.getLatestVersion(),
		          swtDownloader,
		          Update.RESTART_REQUIRED_YES
	          );
	      
	      update.setDescriptionURL(versionGetter.getInfoURL());
	      
	      swtDownloader.addListener(new ResourceDownloaderAdapter() {
		        
		        public boolean 
		        completed(
		        	ResourceDownloader downloader, 
		        	InputStream data) 
		        {
		        		//On completion, process the InputStream to store temp files
		        	
		          return processData(checker,update,downloader,data);
		        }
		        
				public void
				failed(
					ResourceDownloader			downloader,
					ResourceDownloaderException e )
				{
					Debug.out( downloader.getName() + " failed", e );
					
					update.complete( false );
				}
		      });
	    }
  	}catch( Throwable e ){
  		Logger.log(new LogAlert(LogAlert.UNREPEATABLE,
					"SWT Version check failed", e));
  		
  		checker.failed();
  		
  	}finally{
  		
  		checker.completed();
  	}
    
  }
  
  private boolean 
  processData(
	UpdateChecker 		checker,
	Update				update,
	ResourceDownloader	rd,
	InputStream 		data ) 
  {
	ZipInputStream zip = null;
	
    try {
	  data = update.verifyData( data, true );
   	
	  rd.reportActivity( "Data verified successfully" );
	         
      UpdateInstaller installer = checker.createInstaller();
      
      zip = new ZipInputStream(data);
      
      ZipEntry entry = null;
      
      while((entry = zip.getNextEntry()) != null) {
    	  
        String name = entry.getName();
        
        	// all jars
        
        if ( name.endsWith( ".jar" )){
        	
          installer.addResource(name,zip,false);
          
          if ( Constants.isOSX ){
        	  
            installer.addMoveAction(name,installer.getInstallDir() + OSX_APP + "/Contents/Resources/Java/" + name);
            
          }else{ 
        	  
            installer.addMoveAction(name,installer.getInstallDir() + File.separator + name);
          }
        }else if ( name.endsWith(".jnilib") && Constants.isOSX ){
        	
        	  //on OS X, any .jnilib
        	
          installer.addResource(name,zip,false);
          
          installer.addMoveAction(name,installer.getInstallDir() + OSX_APP + "/Contents/Resources/Java/dll/" + name);
          
        }else if ( name.equals("java_swt")){
        	
            //on OS X, java_swt (the launcher to start SWT applications)
        	   
          installer.addResource(name,zip,false);
          
          installer.addMoveAction(name,installer.getInstallDir() + OSX_APP + "/Contents/MacOS/" + name);
          
          installer.addChangeRightsAction("755",installer.getInstallDir() + OSX_APP + "/Contents/MacOS/" + name);
          
        }else if( name.endsWith( ".dll" ) || name.endsWith( ".so" ) || name.indexOf( ".so." ) != -1 ) {
        	
           	// native stuff for windows and linux
        	 
          installer.addResource(name,zip,false);
          
          installer.addMoveAction(name,installer.getInstallDir() + File.separator + name);
  
        }else if ( name.equals("javaw.exe.manifest") || name.equals( "azureus.sig" )){
        	
        	// silently ignore this one 
        }else{
        	
    	   Debug.outNoStack( "SWTUpdate: ignoring zip entry '" + name + "'" );
       }
      }     
      
      update.complete( true );
      
    } catch(Throwable e) {
    	
    	update.complete( false );
    	
  		Logger.log(new LogAlert(LogAlert.UNREPEATABLE,
				"SWT Update failed", e));
      return false;
    }finally{
    	if ( zip != null ){
    		
    		try{
    			  zip.close();
    			  
    		}catch( Throwable e ){
    		}
    	}
    }
        
    return true;
  }
  
  public String
  getName()
  {
    return( "SWT library" );
  }
  
  public int
  getMaximumCheckTime()
  {
    return( 30 ); // !!!! TODO: fix this
  } 
}
