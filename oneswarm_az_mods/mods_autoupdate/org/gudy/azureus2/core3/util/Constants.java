/*
 * Created on 16 juin 2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.core3.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.StringTokenizer;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;

/**
 *
 * @author Olivier
 *
 */

public class
Constants
{
  public static final String SF_WEB_SITE			= "http://oneswarm.cs.washington.edu/";
  public static final String AELITIS_WEB_SITE   	= "http://oneswarm.cs.washington.edu/";
  public static final String GETAZUREUS_WEB_SITE	= "http://oneswarm.cs.washington.edu/";

  // TODO: Keep this is sync with GWT OneSwarmConstants.
	public final static int LOCAL_WEB_SERVER_PORT =
		System.getProperty("oneswarm.integration.web.ui.port") == null ? 29615 :
			Integer.parseInt(System.getProperty("oneswarm.integration.web.ui.port"));

	public final static int LOCAL_WEB_SERVER_PORT_AUTH = LOCAL_WEB_SERVER_PORT+1;

	public final static String ONESWARM_ENTRY_URL = "http://127.0.0.1:" + LOCAL_WEB_SERVER_PORT + "/";

//  public static final String AELITIS_TORRENTS		= "http://update.oneswarm.org/autoupdate/";
//  public static final String AELITIS_TORRENTS		= getAELITIS_TORRENTS();
//  public static final String AELITIS_FILES			= "http://update.oneswarm.org/files/";
//  public static final String AELITIS_FILES			= getAELITIS_FILES();

  public static final String AZUREUS_WIKI 			= "http://wiki.oneswarm.org/index.php/";

  public static String getAELITIS_TORRENTS() {
//	  System.out.println("getAELITIS_TORRENTS()");
	  if( COConfigurationManager.getBooleanParameter("oneswarm.beta.updates") ) {
		  return "http://update-dev.oneswarm.org/autoupdate/";
	  } else {
		  return "http://update.oneswarm.org/autoupdate/";
	  }
  }

  public static String getAELITIS_FILES() {
//	  System.out.println("getAELITIS_FILES()");
	  if( COConfigurationManager.getBooleanParameter("oneswarm.beta.updates") ) {
		  return "http://update-dev.oneswarm.org/files/";
	  } else {
		  return "http://update.oneswarm.org/files/";
	  }
  }

  public static String getVERSION_SERVER_V4() {
//	  System.out.println("getVERSION_SERVER_V4()");
	  if( COConfigurationManager.getBooleanParameter("oneswarm.beta.updates") ) {
		  return "update-dev.oneswarm.org";
	  } else {
		  return "update.oneswarm.org";
	  }
  }

  public static String getVERSION_SERVER_V6() {
		return getVERSION_SERVER_V4();
  }

//  public static final String  VERSION_SERVER_V4 	= "update.oneswarm.org";
//  public static final String  VERSION_SERVER_V6 	= "update.oneswarm.org";

  public static final String DHT_SEED_ADDRESS_V4	= "dht.aelitis.com";
  //public static final String DHT_SEED_ADDRESS_V6	= "dht6.azureusplatform.com"; // does not resolve
  public static final String DHT_SEED_ADDRESS_V6	= "dht.aelitis.com";

  public static final String NAT_TEST_SERVER		= "nettest.azureusplatform.com";
  public static final String NAT_TEST_SERVER_HTTP	= "http://nettest.azureusplatform.com/";

  public static final String SPEED_TEST_SERVER		= "speed.azureusplatform.com";

  public static final String[] AZUREUS_DOMAINS = { "azureusplatform.com", "azureus.com", "aelitis.com" };

  public static final String DEFAULT_ENCODING 	= "UTF8";
  public static final String BYTE_ENCODING 		= "ISO-8859-1";
  public static Charset	BYTE_CHARSET;
  public static Charset	DEFAULT_CHARSET;

  static{
	  try{
	  	BYTE_CHARSET 	= Charset.forName( Constants.BYTE_ENCODING );
	 	DEFAULT_CHARSET = Charset.forName( Constants.DEFAULT_ENCODING );

	}catch( Throwable e ){

		e.printStackTrace();
	}
  }

  public static final String INFINITY_STRING	= "\u221E"; // "oo";pa
  public static final int    INFINITY_AS_INT = 365*24*3600; // seconds (365days)
  public static final long   INFINITE_AS_LONG = 10000*365*24*3600; // seconds (10k years)

  	// keep the CVS style constant coz version checkers depend on it!
  	// e.g. 2.0.8.3
    //      2.0.8.3_CVS
    //      2.0.8.3_Bnn       // incremental build

//  public static final String AZUREUS_NAME	  = "Azureus";
  public static final String AZUREUS_NAME	  = "OneSwarm";
//  public static final String AZUREUS_VERSION  = "3.0.5.0";  //3.0.5.1_CVS
  public static final String AZUREUS_VERSION  = "0.7.1.0";
  public static final byte[] VERSION_ID       = ("-" + "OS" + "0710" + "-").getBytes();  //MUST be 8 chars long!


  public static final String  OSName = System.getProperty("os.name");

  public static final boolean isOSX				= OSName.toLowerCase().startsWith("mac os");
  public static final boolean isLinux			= OSName.equalsIgnoreCase("Linux");
  public static final boolean isSolaris			= OSName.equalsIgnoreCase("SunOS");
  public static final boolean isFreeBSD			= OSName.equalsIgnoreCase("FreeBSD");
  public static final boolean isWindowsXP		= OSName.equalsIgnoreCase("Windows XP");
  public static final boolean isWindowsVista 	= OSName.equalsIgnoreCase("Windows Vista");
  public static final boolean isWindows95		= OSName.equalsIgnoreCase("Windows 95");
  public static final boolean isWindows98		= OSName.equalsIgnoreCase("Windows 98");
  public static final boolean isWindowsME		= OSName.equalsIgnoreCase("Windows ME");
  public static final boolean isWindows9598ME	= isWindows95 || isWindows98 || isWindowsME;

  public static final boolean isWindows	= OSName.toLowerCase().startsWith("windows");
  // If it isn't windows or osx, it's most likely an unix flavor
  public static final boolean isUnix = !isWindows && !isOSX;


  public static final boolean isWindowsVistaOrHigher;

  static{
	  if ( isWindows ){

		  Float ver = null;

		  try{
			  ver = new Float( System.getProperty( "os.version" ));

		  }catch (Throwable e){
		  }

		  isWindowsVistaOrHigher = ver != null && ver.floatValue() >= 6;

	  }else{

		  isWindowsVistaOrHigher = false;
	  }
  }

  public static final boolean isOSX_10_5_OrHigher;
  public static final boolean isOSX_10_6_OrHigher;

  static{
	  if ( isOSX ){

		  int	first_digit 	= 0;
		  int	second_digit	= 0;

		  try{
			  String os_version = System.getProperty( "os.version" );

			  String[] bits = os_version.split( "\\." );

			  first_digit = Integer.parseInt( bits[0] );

			  if ( bits.length > 1 ){

				  second_digit = Integer.parseInt( bits[1] );
			  }
		  }catch( Throwable e ){

		  }

		  isOSX_10_5_OrHigher = first_digit > 10 || ( first_digit == 10 && second_digit >= 5 );
		  isOSX_10_6_OrHigher = first_digit > 10 || ( first_digit == 10 && second_digit >= 6 );

	  }else{

		  isOSX_10_5_OrHigher = false;
		  isOSX_10_6_OrHigher = false;
	  }
  }

  public static final String	JAVA_VERSION = System.getProperty("java.version");

  public static final String	FILE_WILDCARD = isWindows?"*.*":"*";

  	/**
  	 * Gets the current version, or if a CVS version, the one on which it is based
  	 * @return
  	 */

  public static String
  getBaseVersion()
  {
  	return( getBaseVersion( AZUREUS_VERSION ));
  }

  public static String
  getBaseVersion(
  	String	version )
  {
  	int	p1 = version.indexOf("_");	// _CVS or _Bnn

  	if ( p1 == -1 ){

  		return( version );
  	}

  	return( version.substring(0,p1));
  }

  	/**
  	 * is this a formal build or CVS/incremental
  	 * @return
  	 */

  public static boolean
  isCVSVersion()
  {
  	return( isCVSVersion( AZUREUS_VERSION ));
  }

  public static boolean
  isCVSVersion(
  	String	version )
  {
  	return( version.indexOf("_") != -1 );
  }

  	/**
  	 * For CVS builds this returns the incremental build number. For people running their own
  	 * builds this returns -1
  	 * @return
  	 */

  public static int
  getIncrementalBuild()
  {
  	return( getIncrementalBuild( AZUREUS_VERSION ));
  }

  public static int
  getIncrementalBuild(
  	String	version )
  {
  	if ( !isCVSVersion(version)){

  		return( 0 );
  	}

  	int	p1 = version.indexOf( "_B" );

  	if ( p1 == -1 ){

  		return( -1 );
  	}

  	try{
  		return( Integer.parseInt( version.substring(p1+2)));

  	}catch( Throwable e ){

  		System.out.println("can't parse version");

  		return( -1 );
  	}
  }

		/**
		 * compare two version strings of form n.n.n.n (e.g. 1.2.3.4)
		 * @param version_1
		 * @param version_2
		 * @return -ve -> version_1 lower, 0 = same, +ve -> version_1 higher
		 */

	public static int
	compareVersions(
		String		version_1,
		String		version_2 )
	{
		try{
			if ( version_1.startsWith("." )){
				version_1 = "0" + version_1;
			}
			if ( version_2.startsWith("." )){
				version_2 = "0" + version_2;
			}

			version_1 = version_1.replaceAll("[^0-9.]", ".");
			version_2 = version_2.replaceAll("[^0-9.]", ".");

			StringTokenizer	tok1 = new StringTokenizer(version_1,".");
			StringTokenizer	tok2 = new StringTokenizer(version_2,".");

			while( true ){
				if ( tok1.hasMoreTokens() && tok2.hasMoreTokens()){

					int	i1 = Integer.parseInt(tok1.nextToken());
					int	i2 = Integer.parseInt(tok2.nextToken());

					if ( i1 != i2 ){

						return( i1 - i2 );
					}
				}else if ( tok1.hasMoreTokens()){

					int	i1 = Integer.parseInt(tok1.nextToken());

					if ( i1 != 0 ){

						return( 1 );
					}
				}else if ( tok2.hasMoreTokens()){

					int	i2 = Integer.parseInt(tok2.nextToken());

					if ( i2 != 0 ){

						return( -1 );
					}
				}else{
					return( 0 );
				}
			}
		}catch( Throwable e ){

			Debug.printStackTrace(e);

			return( 0 );
		}
	}

	public static boolean
	isAzureusDomain(
		String	host )
	{
		host = host.toLowerCase();

		for (int i=0; i<AZUREUS_DOMAINS.length; i++) {

			String domain = AZUREUS_DOMAINS[i];

			if ( domain.equals( host )){

				return( true );
			}

			if ( host.endsWith("." + domain)){

				return( true );
			}
		}

		return( false );
	}

	//****************************************************
	/*
	 * EDIT, by isdal
	 *
	 * Added functions so we can check the version of our own mods too
	 */
	private static String OS_AZ_MODS_VERSION_KEY = "OS_AZ_MODS_VERSION";
	private static String oneSwarmAzureusModsVersion = null;

	public static String getOneSwarmAzureusModsVersion(){
		if(oneSwarmAzureusModsVersion == null){
			InputStream is = Thread.currentThread().getClass().getResourceAsStream("/OneSwarmAzMods.properties");
			/**
			 * PIAMOD -- check here so we can run in hosted mode, etc. during development
			 */
			if( is == null )
			{
				try {
					is = new FileInputStream("OneSwarmAzMods.properties");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					is = null;
				}
			}
			Properties p = new Properties();
			try {
				if(is != null){
				p.load(is);
				oneSwarmAzureusModsVersion = p.getProperty(OS_AZ_MODS_VERSION_KEY);
				System.err.println("getting os az mods version: " + oneSwarmAzureusModsVersion);
				} else {
					System.err.println("could not load az mods properties file");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return oneSwarmAzureusModsVersion;
	}

	public static String getF2FVersion() {
		if (AzureusCoreImpl.isCoreAvailable()) {
			PluginInterface f2fIf = AzureusCoreImpl.getSingleton().getPluginManager().getPluginInterfaceByID(
					"osf2f");
			if (f2fIf != null) {
				return f2fIf.getPluginVersion();
			}
		}
		return "";
	}

	public static String getWebUiVersion() {
		if (AzureusCoreImpl.isCoreAvailable()) {
			PluginInterface gwtIf = AzureusCoreImpl.getSingleton().getPluginManager().getPluginInterfaceByID(
					"osgwtui");
			if (gwtIf != null) {
				return gwtIf.getPluginVersion();
			}
		}
		return "";
	}
	//****************************************************

	public static void main(String[] args) {
		System.out.println(compareVersions("3.0.0.1", "3.0.0.0"));
		System.out.println(compareVersions("3.0.0.0_B1", "3.0.0.0"));
		System.out.println(compareVersions("3.0.0.0", "3.0.0.0_B1"));
		System.out.println(compareVersions("3.0.0.0_B1", "3.0.0.0_B4"));
		System.out.println(compareVersions("3.0.0.0..B1", "3.0.0.0_B4"));
	}
}
