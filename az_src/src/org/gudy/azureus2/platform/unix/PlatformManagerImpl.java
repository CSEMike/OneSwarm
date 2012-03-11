/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
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
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.platform.unix;

import java.io.File;
import java.net.InetAddress;
import java.util.HashSet;

import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.platform.*;

import org.gudy.azureus2.plugins.platform.PlatformManagerException;

import com.aelitis.azureus.core.AzureusCore;

/**
 * @author TuxPaper
 * @created Dec 18, 2006
 *
 */
public class PlatformManagerImpl implements PlatformManager
{
	private static final LogIDs LOGID = LogIDs.CORE;

	private static final String ERR_UNSUPPORTED = "Unsupported capability called on platform manager";

	protected static PlatformManagerImpl singleton;

	protected static AEMonitor class_mon = new AEMonitor("PlatformManager");

	private final HashSet capabilitySet = new HashSet();

	private static final Object migrate_lock = new Object();

	/**
	 * Gets the platform manager singleton, which was already initialized
	 */
	public static PlatformManagerImpl getSingleton() {
		return singleton;
	}

	static {
		initializeSingleton();
	}

	/**
	 * Instantiates the singleton
	 */
	private static void initializeSingleton() {
		try {
			class_mon.enter();
			singleton = new PlatformManagerImpl();
		} catch (Throwable e) {
			Logger.log(new LogEvent(LOGID, "Failed to initialize platform manager"
					+ " for Unix Compatable OS", e));
		} finally {
			class_mon.exit();
		}
	}

	/**
	 * Creates a new PlatformManager and initializes its capabilities
	 */
	public PlatformManagerImpl() {
		capabilitySet.add(PlatformManagerCapabilities.GetUserDataDirectory);
	}

	// @see org.gudy.azureus2.platform.PlatformManager#copyFilePermissions(java.lang.String, java.lang.String)
	public void copyFilePermissions(String from_file_name, String to_file_name)
			throws PlatformManagerException {
		throw new PlatformManagerException(ERR_UNSUPPORTED);
	}

	// @see org.gudy.azureus2.platform.PlatformManager#createProcess(java.lang.String, boolean)
	public void createProcess(String command_line, boolean inherit_handles)
			throws PlatformManagerException {
		throw new PlatformManagerException(ERR_UNSUPPORTED);
	}

	// @see org.gudy.azureus2.platform.PlatformManager#dispose()
	public void dispose() {
	}

	// @see org.gudy.azureus2.platform.PlatformManager#getApplicationCommandLine()
	public String getApplicationCommandLine() throws PlatformManagerException {
		throw new PlatformManagerException(ERR_UNSUPPORTED);
	}

	// @see org.gudy.azureus2.platform.PlatformManager#getPlatformType()
	public int getPlatformType() {
		return PT_UNIX;
	}

	// @see org.gudy.azureus2.platform.PlatformManager#getUserDataDirectory()
	public String getUserDataDirectory()
		throws PlatformManagerException
	{
		String userhome = System.getProperty("user.home");
		String temp_user_path = userhome + SystemProperties.SEP + "."
				+ SystemProperties.APPLICATION_NAME.toLowerCase()
				+ SystemProperties.SEP;

		synchronized (migrate_lock) {
			File home = new File(temp_user_path);
			if (!home.exists()) { //might be a fresh install or might be an old non-migrated install
				String old_home_path = userhome + SystemProperties.SEP + "."
						+ SystemProperties.APPLICATION_NAME + SystemProperties.SEP;
				File old_home = new File(old_home_path);
				if (old_home.exists()) { //migrate
					String msg = "Migrating unix user config dir [" + old_home_path
							+ "] ===> [" + temp_user_path + "]";
					System.out.println(msg);
					Logger.log(new LogEvent(LOGID,
							"SystemProperties::getUserPath(Unix): " + msg));
					try {
						old_home.renameTo(home);
					} catch (Throwable t) {
						t.printStackTrace();
						Logger.log(new LogEvent(LOGID, "migration rename failed:", t));
					}
				}
			}
		}

		return temp_user_path;
	}

	public String 
	getComputerName() 
	{
		String	host = System.getenv( "HOST" );
		
		if ( host != null && host.length() > 0 ){
			
			return( host );
		}
		
		return( null );
	}
	
	// @see org.gudy.azureus2.platform.PlatformManager#getVersion()
	public String getVersion() throws PlatformManagerException {
		throw new PlatformManagerException(ERR_UNSUPPORTED);
	}

	// @see org.gudy.azureus2.platform.PlatformManager#hasCapability(org.gudy.azureus2.platform.PlatformManagerCapabilities)
	public boolean hasCapability(PlatformManagerCapabilities capability) {
		return capabilitySet.contains(capability);
	}

	// @see org.gudy.azureus2.platform.PlatformManager#isApplicationRegistered()
	public boolean isApplicationRegistered() throws PlatformManagerException {
		throw new PlatformManagerException(ERR_UNSUPPORTED);
	}

	// @see org.gudy.azureus2.platform.PlatformManager#performRecoverableFileDelete(java.lang.String)
	public void performRecoverableFileDelete(String file_name)
			throws PlatformManagerException {
		throw new PlatformManagerException(ERR_UNSUPPORTED);
	}

	// @see org.gudy.azureus2.platform.PlatformManager#ping(java.net.InetAddress, java.net.InetAddress, org.gudy.azureus2.platform.PlatformManagerPingCallback)
	public void ping(InetAddress interface_address, InetAddress target,
			PlatformManagerPingCallback callback) throws PlatformManagerException {
		throw new PlatformManagerException(ERR_UNSUPPORTED);
	}

	// @see org.gudy.azureus2.platform.PlatformManager#registerApplication()
	public void registerApplication() throws PlatformManagerException {
		throw new PlatformManagerException(ERR_UNSUPPORTED);
	}

	// @see org.gudy.azureus2.platform.PlatformManager#addListener(org.gudy.azureus2.platform.PlatformManagerListener)
	public void addListener(PlatformManagerListener listener) {
		// No Listener Functionality
	}

	// @see org.gudy.azureus2.platform.PlatformManager#removeListener(org.gudy.azureus2.platform.PlatformManagerListener)
	public void removeListener(PlatformManagerListener listener) {
		// No Listener Functionality
	}

	public File 
	getVMOptionFile() 
	
		throws PlatformManagerException 
	{
		throw new PlatformManagerException(ERR_UNSUPPORTED);
	}
	
	public String[]
	getExplicitVMOptions()
	          	
	 	throws PlatformManagerException
	{
		throw new PlatformManagerException(ERR_UNSUPPORTED);
	}
	 
	public void
	setExplicitVMOptions(
		String[]		options )
	          	
		throws PlatformManagerException
	{
		throw new PlatformManagerException(ERR_UNSUPPORTED);
	}
	
  	public boolean 
  	getRunAtLogin() 
  	
  		throws PlatformManagerException 
  	{
  		throw new PlatformManagerException(ERR_UNSUPPORTED);
  	}
  	
  	public void 
  	setRunAtLogin(
  		boolean run ) 
  	
  		throws PlatformManagerException 
  	{
  		throw new PlatformManagerException(ERR_UNSUPPORTED);
   	}
	  
	public void
	startup(
		AzureusCore		azureus_core )
	
		throws PlatformManagerException
	{	
	}
	
	public int
	getShutdownTypes()
	{
		return( 0 );
	}
	
	public void
	shutdown(
		int			type )
	
		throws PlatformManagerException
	{	
		 throw new PlatformManagerException("Unsupported capability called on platform manager");
	}
	
	// @see org.gudy.azureus2.platform.PlatformManager#setTCPTOSEnabled(boolean)
	public void setTCPTOSEnabled(boolean enabled) throws PlatformManagerException {
		throw new PlatformManagerException(ERR_UNSUPPORTED);
	}

	// @see org.gudy.azureus2.platform.PlatformManager#testNativeAvailability(java.lang.String)
	public boolean testNativeAvailability(String name)
			throws PlatformManagerException {
		throw new PlatformManagerException(ERR_UNSUPPORTED);
	}

	// @see org.gudy.azureus2.platform.PlatformManager#traceRoute(java.net.InetAddress, java.net.InetAddress, org.gudy.azureus2.platform.PlatformManagerPingCallback)
	public void traceRoute(InetAddress interface_address, InetAddress target,
			PlatformManagerPingCallback callback) throws PlatformManagerException {
		throw new PlatformManagerException(ERR_UNSUPPORTED);
	}

	// @see org.gudy.azureus2.plugins.platform.PlatformManager#getLocation(long)
	public File getLocation(long location_id) throws PlatformManagerException {
		switch ((int)location_id) {
			case LOC_USER_DATA:
				return( new File( getUserDataDirectory() ));
				
			case LOC_DOCUMENTS:
				return new File(System.getProperty("user.home"));
				
			case LOC_MUSIC:
				
			case LOC_VIDEO:

			default:
				return( null );
		}
	}

	// @see org.gudy.azureus2.plugins.platform.PlatformManager#isAdditionalFileTypeRegistered(java.lang.String, java.lang.String)
	public boolean isAdditionalFileTypeRegistered(String name, String type)
			throws PlatformManagerException {
		throw new PlatformManagerException(ERR_UNSUPPORTED);
	}

	// @see org.gudy.azureus2.plugins.platform.PlatformManager#registerAdditionalFileType(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	public void registerAdditionalFileType(String name, String description,
			String type, String content_type) throws PlatformManagerException {
		throw new PlatformManagerException(ERR_UNSUPPORTED);
	}

	// @see org.gudy.azureus2.plugins.platform.PlatformManager#showFile(java.lang.String)
	public void showFile(String file_name) throws PlatformManagerException {
		throw new PlatformManagerException(ERR_UNSUPPORTED);
	}

	// @see org.gudy.azureus2.plugins.platform.PlatformManager#unregisterAdditionalFileType(java.lang.String, java.lang.String)
	public void unregisterAdditionalFileType(String name, String type)
			throws PlatformManagerException {
		throw new PlatformManagerException(ERR_UNSUPPORTED);
	}

	// @see org.gudy.azureus2.platform.PlatformManager#getAzComputerID()
	public String getAzComputerID() throws PlatformManagerException {
    throw new PlatformManagerException(ERR_UNSUPPORTED);
	}

	public void requestUserAttention(int type, Object data) throws PlatformManagerException {
		throw new PlatformManagerException("Unsupported capability called on platform manager");
	}
}
