/**
 * File: PluginStateImpl.java
 * Date: 19 Aug 2008
 * Author: Allan Crooks
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the COPYING file ).
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.gudy.azureus2.pluginsimpl.local;

import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginState;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.pluginsimpl.local.installer.PluginInstallerImpl;
import org.gudy.azureus2.update.UpdaterUtils;

public class PluginStateImpl implements PluginState {

	private PluginInterfaceImpl pi;
	private PluginInitializer initialiser;
	private boolean	disabled;
	boolean operational;
	boolean failed;
	
	public PluginStateImpl(PluginInterfaceImpl pi, PluginInitializer initialiser) {
		this.pi = pi;
		this.initialiser = initialiser;
	}
	
	public void setLoadedAtStartup(boolean load_at_startup) {
		String param_name = "PluginInfo." + pi.getPluginID() + ".enabled";
		COConfigurationManager.setParameter(param_name, load_at_startup);
	}
	
	public boolean isLoadedAtStartup() {
		String param_name = "PluginInfo." + pi.getPluginID() + ".enabled";
		if (!COConfigurationManager.hasParameter(param_name, false)) {
			return true; // Load at startup by default.
		}
		return COConfigurationManager.getBooleanParameter(param_name);
	}
	
	public boolean hasFailed() {
		return failed;
	}

	public void	setDisabled(boolean	_disabled) {
		disabled = _disabled;
	}
	  
	public boolean isDisabled()	{
		return disabled;
	}
	
	public boolean isBuiltIn() {
		String dir = pi.getPluginDirectoryName();
		if (dir == null) {
			return PluginInitializer.isLoadingBuiltin();
		}
		return(
			dir.length() == 0 || 
			pi.getPluginID().equals( UpdaterUtils.AZUPDATER_PLUGIN_ID ) ||
			pi.getPluginID().equals( UpdaterUtils.AZUPDATERPATCHER_PLUGIN_ID ));
	}
	
	public boolean isMandatory() {
		if ( 	pi.getPluginID().equals( UpdaterUtils.AZUPDATER_PLUGIN_ID ) ||
				pi.getPluginID().equals( UpdaterUtils.AZUPDATERPATCHER_PLUGIN_ID )){
			
			return( true );
		}
		
		String mand = pi.getPluginProperties().getProperty("plugin.mandatory");
		return (mand != null && mand.trim().toLowerCase().equals("true"));
	}
	
  	void setOperational(boolean b, boolean reloading ) {
  		operational	= b;
  		
  		if ( !reloading ){
  			
  			initialiser.fireOperational( pi, operational );
  		}
  	}
  	
    public boolean isOperational() {
    	return operational;
    }
    
	public boolean isShared() {
		String shared_dir = FileUtil.getApplicationFile("plugins").toString(); 
		String plugin_dir = pi.getPluginDirectoryName();
		return plugin_dir.startsWith(shared_dir);
	}

	public boolean
	isInitialisationComplete()
	{
		return( initialiser.isInitialisationComplete());
	}
	
	public void reload() throws PluginException {
		// we use the "reload" method to load disabled plugins regardless of whether they are
		// unloadable. If currently disabled then no unloading to do anyway  
		if (isUnloadable() || isOperational()) {unload( true );}
	  	initialiser.reloadPlugin(this.pi);
	}
	  
	public void	uninstall() throws PluginException {
		PluginInstallerImpl.getSingleton(pi.getPluginManager()).uninstall(this.pi);
	}

	public boolean 
	isUnloaded() 
	{
		return( pi.class_loader == null );
	}
	
	public void unload() throws PluginException {
		unload( false );
	}
	
	protected void unload( boolean for_reload ) throws PluginException {
		if (!isUnloadable()) {
			throw new PluginException("Plugin isn't unloadable");
		}
		  	
		String dir = pi.getPluginDirectoryName();
		  	
		// if not dir based then just test this one
		if (dir == null || dir.length() == 0) {
			try{
				((UnloadablePlugin)pi.getPlugin()).unload();
			}catch( Throwable e ){
				Debug.out( "Plugin unload operation failed", e );
			}
			initialiser.unloadPlugin(this.pi);
		} else {
		  		
			// we must copy the list here as when we unload interfaces they will be
			// removed from the original list	
			List pis = new ArrayList(PluginInitializer.getPluginInterfaces());
			for (int i=0;i<pis.size();i++){
				PluginInterfaceImpl	pi = (PluginInterfaceImpl)pis.get(i);
				String other_dir = pi.getPluginDirectoryName();
		  		if (other_dir == null || other_dir.length() == 0) {continue;}
		  		if (dir.equals(other_dir)) {
		  			try{
		  				((UnloadablePlugin)pi.getPlugin()).unload();
		  			}catch( Throwable e ){
						Debug.out( "Plugin unload operation failed", e );
					}
		  			initialiser.unloadPlugin( pi );
		  		}
			}
		}
		  	
		for (int i=0;i<pi.children.size();i++){
			((PluginStateImpl)((PluginInterface)pi.children.get(i)).getPluginState()).unload( for_reload );
		}
		  	
		setOperational(false, for_reload );
		pi.destroy();
	}
		
	public boolean isUnloadable() {
		String dir = pi.getPluginDirectoryName();
		  	
  		// mechanism to override unloadability 
	   	boolean	disable_unload = pi.getPluginProperties().getProperty("plugin.unload.disabled", "").equalsIgnoreCase("true");
	  	if (disable_unload) {return false;}

	  	// if not dir based then just test this one
	  	if (dir == null || dir.length() == 0) {
		  	return pi.getPlugin() instanceof UnloadablePlugin;
		}
		  	
	  	List pis = PluginInitializer.getPluginInterfaces();
		for (int i=0;i<pis.size();i++) {
		  	PluginInterface	pi = (PluginInterface)pis.get(i);
	  		String other_dir = pi.getPluginDirectoryName();
	  		if (other_dir == null || other_dir.length() == 0) {continue;}
	  		if (dir.equals(other_dir)) {
		  		if (!(pi.getPlugin() instanceof UnloadablePlugin)) {
		  			return false;
		  		}  
	  		}
		}
		  	
		for (int i=0;i<pi.children.size();i++){
			if (!((PluginInterface)pi.children.get(i)).getPluginState().isUnloadable()){
				return false;
		  	}
		}
		  	
	  	return true;
	}
	
}
