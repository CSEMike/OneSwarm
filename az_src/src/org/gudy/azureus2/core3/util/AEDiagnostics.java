/*
 * Created on 22-Sep-2004
 * Created by Paul Gardner
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

package org.gudy.azureus2.core3.util;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;

import com.aelitis.azureus.core.util.Java15Utils;

/**
 * @author parg
 *
 */

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

public class 
AEDiagnostics 
{
	// these can not be set true and have a usable AZ!
	public static final boolean	ALWAYS_PASS_HASH_CHECKS			= false;
	public static final boolean	USE_DUMMY_FILE_DATA				= false;
	public static final boolean	CHECK_DUMMY_FILE_DATA			= false;

	// these can safely be set true, things will work just slower
	public static final boolean	DEBUG_MONITOR_SEM_USAGE			= false;
    public static final boolean DEBUG_THREADS			        = true; // Leave this on by default for the moment

	public static final boolean	TRACE_DIRECT_BYTE_BUFFERS		= false;
	public static final boolean	TRACE_DBB_POOL_USAGE			= false;
	public static final boolean	PRINT_DBB_POOL_USAGE			= false;
  
    public static final boolean TRACE_TCP_TRANSPORT_STATS       = false;
    public static final boolean TRACE_CONNECTION_DROPS          = false;
    
	
	static{
		if ( ALWAYS_PASS_HASH_CHECKS ){
			System.out.println( "**** Always passing hash checks ****" );
		}
		if ( USE_DUMMY_FILE_DATA ){
			System.out.println( "**** Using dummy file data ****" );
		}
		if ( CHECK_DUMMY_FILE_DATA ){
			System.out.println( "**** Checking dummy file data ****" );
		}
		if ( DEBUG_MONITOR_SEM_USAGE ){
			System.out.println( "**** AEMonitor/AESemaphore debug on ****" );
		}
		if ( TRACE_DIRECT_BYTE_BUFFERS ){
			System.out.println( "**** DirectByteBuffer tracing on ****" );
		}
		if ( TRACE_DBB_POOL_USAGE ){
			System.out.println( "**** DirectByteBufferPool tracing on ****" );
		}
		if ( PRINT_DBB_POOL_USAGE ){
			System.out.println( "**** DirectByteBufferPool printing on ****" );
		}
		if ( TRACE_TCP_TRANSPORT_STATS ){
		  System.out.println( "**** TCP_TRANSPORT_STATS tracing on ****" );
		}
	}
	
	private static final int	MAX_FILE_SIZE	= 256*1024;	// get two of these per logger type
	
	private static final String	CONFIG_KEY	= "diagnostics.tidy_close";
	
	private static File	debug_dir;

	private static File	debug_save_dir;
	
	private static boolean	started_up;
	private static boolean	startup_complete;
	
	private static Map		loggers	= new HashMap();
	private static boolean	loggers_enabled;
	
	private static List		evidence_generators	= new ArrayList();
	
	private static boolean load_15_tried;
	
	public static synchronized void
	startup()
	{
		if ( started_up ){
			
			return;
		}
		
		started_up	= true;
		
		try{
			// Minimize risk of loading to much when in transitory startup mode
			boolean transitoryStartup = System.getProperty("transitory.startup", "0").equals("1");
			if (transitoryStartup) {
				// no vivaldi and Thread monitor for you!
				load_15_tried = true;
				// no xxx_?.log logging for you!
				loggers_enabled = false;
				// skip tidy check and more!
				return;
			}

			debug_dir		= FileUtil.getUserFile( "logs" );
			
			debug_save_dir	= new File( debug_dir, "save" );
			
			loggers_enabled = COConfigurationManager.getBooleanParameter( "Logger.DebugFiles.Enabled");

			boolean	was_tidy	= COConfigurationManager.getBooleanParameter( CONFIG_KEY );
			
			new AEThread2( "asyncify", true )
			{
				public void
				run()
				{
					SimpleTimer.addEvent("AEDiagnostics:logCleaner",SystemTime.getCurrentTime() + 60000
							+ (int) (Math.random() * 15000), new TimerEventPerformer() {
						public void perform(TimerEvent event) {
							cleanOldLogs();
						}
					});
				}
			}.start();

			if ( debug_dir.exists()){
				
				long	now = SystemTime.getCurrentTime();
				
				debug_save_dir.mkdir();
				
				File[] files = debug_dir.listFiles();
				
				if ( files != null ){
					
					boolean	file_copied	= false;
					
					for (int i=0;i<files.length;i++){
						
						File	file = files[i];
						
						if ( file.isDirectory()){
							
							continue;
						}
						
						if ( !was_tidy ){
				
							file_copied	= true;
							
							FileUtil.copyFile( file, new File( debug_save_dir, now + "_" + file.getName()));
						}
					}
					
					if ( file_copied ){
						
						Logger.logTextResource(new LogAlert(LogAlert.UNREPEATABLE,
								LogAlert.AT_WARNING, "diagnostics.log_found"),
								new String[] { debug_save_dir.toString() });
					}
				}
			}else{
				
				debug_dir.mkdir();
			}
		}catch( Throwable e ){
			
				// with webui we don't have the file stuff so this fails with class not found
			
			if ( !(e instanceof NoClassDefFoundError )){
				
				Debug.printStackTrace( e );
			}
		}finally{
			
			startup_complete	= true;
			
			load15Stuff();
		}
	}
	
	protected static void
	load15Stuff()
	{
		if ( load_15_tried ){
			
			return;
		}
		
		load_15_tried = true;
		
		// pull in the JDK1.5 monitoring stuff if present
			
		try{
			Class c = Class.forName( "com.aelitis.azureus.jdk15.Java15Initialiser" );
					
			if ( c != null ){
				
				Method m = c.getDeclaredMethod( "getUtilsProvider", new Class[0] );
				
				Java15Utils.Java15UtilsProvider provider = (Java15Utils.Java15UtilsProvider)m.invoke( null, new Object[0] );
				
				if ( provider != null ){
					
					 Java15Utils.setProvider( provider );
				}
			}
			// System.out.println( "**** AEThread debug on ****" );

		}catch( Throwable e ){
		}
	}
	
	/**
	 * 
	 */
	protected static void cleanOldLogs() {
		try {
			long now = SystemTime.getCurrentTime();

			// clear out any really old files in the save-dir

			File[] files = debug_save_dir.listFiles();

			if (files != null) {

				for (int i = 0; i < files.length; i++) {

					File file = files[i];

					if (!file.isDirectory()) {

						long last_modified = file.lastModified();

						if (now - last_modified > 10 * 24 * 60 * 60 * 1000L) {

							file.delete();
						}
					}
				}
			}

		} catch (Exception e) {
		}
	}

	public static boolean
	isStartupComplete()
	{
		return( startup_complete );
	}
	
	public static synchronized AEDiagnosticsLogger
	getLogger(
		String		name )
	{
		AEDiagnosticsLogger	logger = (AEDiagnosticsLogger)loggers.get(name);
		
		if ( logger == null ){
			
			startup();
			
			logger	= new AEDiagnosticsLogger( name );
			
			try{
				File	f1 = getLogFile( logger );
				
				logger.setFirstFile( false );
				
				File	f2 = getLogFile( logger );
				
				logger.setFirstFile( true );
	
					// if we were writing to the second file, carry on from there
				
				if ( f1.exists() && f2.exists()){
		
					if ( f1.lastModified() < f2.lastModified()){
						
						logger.setFirstFile( false );	
					}
				}
			}catch( Throwable ignore ){
				
			}
			
			loggers.put( name, logger );
			
		}
		
		return( logger );
	}

	public static void
	logWithStack(
		String	logger_name,
		String	str )
	{
		log( logger_name, str + ": " + Debug.getCompressedStackTrace());
	}
	
	public static void
	log(
		String	logger_name,
		String	str )
	{
		getLogger( logger_name ).log( str );
	}
	
	protected static synchronized void
	log(
		AEDiagnosticsLogger		logger,
		String					str )
	{
		if ( !loggers_enabled ){
			
			return;
		}
		
		try{
			
			File	log_file	= getLogFile( logger );
			
			/**
			 *  log_file.length will return 0 if the file doesn't exist, so we don't need
			 *  to explicitly check for its existence.
			 */
			if ( log_file.length() >= MAX_FILE_SIZE ){
				
				logger.setFirstFile(!logger.isFirstFile());
				
				log_file	= getLogFile( logger );
			
				// If the file doesn't exist, this will just return false.
				log_file.delete();
			}
			
			Calendar now = GregorianCalendar.getInstance();

			String timeStamp =
				"[" + format(now.get(Calendar.DAY_OF_MONTH))+format(now.get(Calendar.MONTH)+1) + " " + 
				format(now.get(Calendar.HOUR_OF_DAY))+ ":" + format(now.get(Calendar.MINUTE)) + ":" + format(now.get(Calendar.SECOND)) + "] ";        

			str = timeStamp + str;
	
			PrintWriter	pw = null;
	
			try{		
						
				pw = new PrintWriter(new FileWriter( log_file, true ));
				
				if (!logger.isWrittenToThisSession()) {
					logger.setWrittenToThisSession(true);
					pw.println("\n\n[" + now.get(Calendar.YEAR)
							+ "] Log File Opened for " + Constants.AZUREUS_NAME + " "
							+ Constants.AZUREUS_VERSION + "\n");
				}
			
				pw.println( str );
		 							
			}finally{
				
				if ( pw != null ){
										
					pw.close();
				}
			}
		}catch( Throwable ignore ){
			
		}
	}
	
	private static File
	getLogFile(
		AEDiagnosticsLogger		logger )
	{
		return( new File( debug_dir, logger.getName() + "_" + (logger.isFirstFile()?"1":"2") + ".log" ));
	}
	
	private static String 
	format(
		int 	n ) 
	{
		if (n < 10){
	   	
			return( "0" + n );
	   }
		
	   return( String.valueOf(n));
	}
	
	protected static void
	log(
		AEDiagnosticsLogger		logger,
		Throwable				e )
	{
		try{
			ByteArrayOutputStream	baos = new ByteArrayOutputStream();
			
			PrintWriter	pw = new PrintWriter( new OutputStreamWriter( baos ));
			
			e.printStackTrace( pw );
			
			pw.close();
			
			log( logger, baos.toString());
			
		}catch( Throwable ignore ){
			
		}
	}
	
	public static void
	markDirty()
	{
		try{

			COConfigurationManager.setParameter( CONFIG_KEY, false );
		
			COConfigurationManager.save();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}

	public static boolean
	isDirty()
	{
		return( !COConfigurationManager.getBooleanParameter( CONFIG_KEY ));
	}
	
	public static void
	markClean()
	{
		try{
			COConfigurationManager.setParameter( CONFIG_KEY, true );
			
			COConfigurationManager.save();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	private static final String[][]
	   	bad_dlls = { 
			{	"niphk", 			"y", },
	   		{	"nvappfilter", 		"y", },
	   		{	"netdog", 			"y", },
	   		{	"vlsp", 			"y", },
	   		{	"imon", 			"y", },
	   		{	"sarah", 			"y", },
	   		{	"MxAVLsp", 			"y", },
	   		{	"mclsp", 			"y", },
	   		{	"radhslib", 		"y", },
	   		{	"winsflt",			"y", },
	   		{	"nl_lsp",			"y", },
	   		{	"AxShlex",			"y", },
	   		{	"iFW_Xfilter",		"y", },
	   		{	"WSOCKHK",			"n", },
	};

	public static void
	checkDumpsAndNatives()
	{
		try{
			PlatformManager	p_man = PlatformManagerFactory.getPlatformManager();
			
			if ( 	p_man.getPlatformType() == PlatformManager.PT_WINDOWS &&
					p_man.hasCapability( PlatformManagerCapabilities.TestNativeAvailability )){	

				for (int i=0;i<bad_dlls.length;i++){
					
					String	dll 	= bad_dlls[i][0];
					String	load	= bad_dlls[i][1];
					
					if ( load.equalsIgnoreCase( "n" )){
						
						continue;
					}
					
					if ( !COConfigurationManager.getBooleanParameter( "platform.win32.dll_found." + dll, false )){
								
						try{
							if ( p_man.testNativeAvailability( dll + ".dll" )){
								
								COConfigurationManager.setParameter( "platform.win32.dll_found." + dll, true );
	
								String	detail = MessageText.getString( "platform.win32.baddll." + dll );
								
								Logger.logTextResource(
										new LogAlert(
												LogAlert.REPEATABLE, 
												LogAlert.AT_WARNING,
												"platform.win32.baddll.info" ),	
										new String[]{ dll + ".dll", detail });
							}
				
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
						}
					}
				}
			}
			
			File app_dir = new File( SystemProperties.getApplicationPath());
			
			if ( app_dir.canRead()){
				
				File[]	files = app_dir.listFiles();
				
				File	most_recent_dump 	= null;
				long	most_recent_time	= 0;
				
				long	now = SystemTime.getCurrentTime();
				
				long	one_week_ago = now - 7*24*60*60*1000;
				
				for (int i=0;i<files.length;i++){
					
					File	f = files[i];
					
					String	name = f.getName();
					
					if ( name.startsWith( "hs_err_pid" )){
						
						long	last_mod = f.lastModified();
						
						if ( last_mod > most_recent_time && last_mod > one_week_ago){
							
							most_recent_dump 	= f;
							most_recent_time	= last_mod;
						}
					}
				}
				
				if ( most_recent_dump!= null ){
					
					long	last_done = 
						COConfigurationManager.getLongParameter( "diagnostics.dump.lasttime", 0 ); 
					
					if ( last_done < most_recent_time ){
						
						COConfigurationManager.setParameter( "diagnostics.dump.lasttime", most_recent_time );
						
						analyseDump( most_recent_dump );
					}
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	protected static void
	analyseDump(
		File	file )
	{
		System.out.println( "Analysing " + file );
		
		try{
			LineNumberReader lnr = new LineNumberReader( new FileReader( file ));
			
			try{
				boolean	float_excep	= false;
				
				String[]	bad_dlls_uc = new String[bad_dlls.length];
				
				for (int i=0;i<bad_dlls.length;i++){
					
					String	dll 	= bad_dlls[i][0];

					bad_dlls_uc[i] = (dll + ".dll" ).toUpperCase();
				}
				
				String	alcohol_dll = "AxShlex";
				
				List	matches = new ArrayList();
				
				while( true ){
					
					String	line = lnr.readLine();
					
					if ( line == null ){
						
						break;
					}
					
					line = line.toUpperCase();
					
					if (line.indexOf( "EXCEPTION_FLT") != -1 ){
						
						float_excep	= true;
						
					}else{
						
						for (int i=0;i<bad_dlls_uc.length;i++){
							
							String b_uc = bad_dlls_uc[i];
							
							if ( line.indexOf( b_uc ) != -1 ){
								
								String	dll = bad_dlls[i][0];
								
								if ( dll.equals( alcohol_dll )){
									
									if ( float_excep ){
										
										matches.add( dll );
									}
									
								}else{
									
									matches.add( dll );
								}
							}
						}
					}
				}
				
				for (int i=0;i<matches.size();i++){
					
					String	dll = (String)matches.get(i);
					
					String	detail = MessageText.getString( "platform.win32.baddll." + dll );
					
					Logger.logTextResource(
							new LogAlert(
									LogAlert.REPEATABLE, 
									LogAlert.AT_WARNING,
									"platform.win32.baddll.info" ),	
							new String[]{ dll + ".dll", detail });
				}
			}finally{
				
				lnr.close();
			}
		}catch( Throwable e){
			
			Debug.printStackTrace( e );
		}
	}
	
	public static void
	addEvidenceGenerator(
		AEDiagnosticsEvidenceGenerator	gen )
	{
		synchronized( evidence_generators ){
			
			evidence_generators.add( gen );
		}
	}
	
	public static void
	generateEvidence(
		PrintWriter		_writer )
	{
		IndentWriter	writer = new IndentWriter( _writer );
		
		synchronized( evidence_generators ){

			for (int i=0;i<evidence_generators.size();i++){
				
				try{
					((AEDiagnosticsEvidenceGenerator)evidence_generators.get(i)).generate( writer );
					
				}catch( Throwable e ){
					
					e.printStackTrace( _writer );
				}
			}
		}
		
		writer.println( "Memory" );
		
		try{
			writer.indent();
			
			Runtime rt = Runtime.getRuntime();
			
			writer.println( "max=" + rt.maxMemory() + ",total=" + rt.totalMemory() + ",free=" + rt.freeMemory());
			
		}finally{
			
			writer.exdent();
		}
	}
}
