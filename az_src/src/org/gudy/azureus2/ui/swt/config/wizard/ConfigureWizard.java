/*
 * File    : ConfigureWizard.java
 * Created : 12 oct. 2003 16:06:44
 * By      : Olivier 
 * 
 * Azureus - a Java Bittorrent client
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
 */
 
package org.gudy.azureus2.ui.swt.config.wizard;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.Wizard;

import com.aelitis.azureus.ui.UserPrompterResultListener;

/**
 * @author Olivier
 * 
 */
public class ConfigureWizard extends Wizard {

	  public static final int WIZARD_MODE_FULL				= 0;
	  public static final int WIZARD_MODE_SPEED_TEST_AUTO	= 1;
	  public static final int WIZARD_MODE_SPEED_TEST_MANUAL	= 2;
  
	private int wizard_mode;
	
  //Transfer settings

  private int connectionUploadLimit;
  private boolean uploadLimitManual;
  private int uploadLimit;
  
  int maxActiveTorrents;
  int maxDownloads;
  
  //Server / NAT Settings
  int serverTCPListenPort = COConfigurationManager.getIntParameter( "TCP.Listen.Port" );
  int serverUDPListenPort = COConfigurationManager.getIntParameter( "UDP.Listen.Port" );
  //Files / Torrents
  private String 	_dataPath;
  private boolean 	_dataPathChanged;
  String torrentPath;
  
  boolean completed = false;
 

  public 
  ConfigureWizard(
	  boolean 	_modal,
	  int		_wizard_mode ) 
  {
    super("configureWizard.title",_modal);
    
    wizard_mode = _wizard_mode;
    
    IWizardPanel panel = wizard_mode==WIZARD_MODE_FULL?new LanguagePanel(this,null):new TransferPanel2( this, null );
    try  {
      torrentPath = COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory");
    } catch(Exception e) {
      torrentPath = ""; 
    }
    
    if ( COConfigurationManager.getBooleanParameter( "Use default data dir" )){
    
    	_dataPath = COConfigurationManager.getStringParameter( "Default save path" );
    	
    }else{
    	_dataPath = "";
    }
    
    this.setFirstPanel(panel);
  }
  
  public void onClose() {
		try {
			if (	!completed && 
					wizard_mode != WIZARD_MODE_SPEED_TEST_AUTO && 
					!COConfigurationManager.getBooleanParameter("Wizard Completed")){
				
				MessageBoxShell mb = new MessageBoxShell(
						MessageText.getString("wizard.close.confirmation"),
						MessageText.getString("wizard.close.message"), new String[] {
							MessageText.getString("Button.yes"),
							MessageText.getString("Button.no")
						}, 0);

				mb.open(new UserPrompterResultListener() {
					public void prompterClosed(int result) {
						if (result == 1) {
							COConfigurationManager.setParameter("Wizard Completed", true);
							COConfigurationManager.save();
						}
					}
				});

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		super.onClose();
	}
  
  protected String
  getDataPath()
  {
	  return( _dataPath );
  }
  
  protected void
  setDataPath(
	String	s )
  {
	  _dataPath 		= s;
	  _dataPathChanged 	= true;
  }
  
  protected boolean
  hasDataPathChanged()
  {
	  return( _dataPathChanged );
  }
  
  protected void
  setConnectionUploadLimit(
	int		rate,
	boolean	is_manual )
  {
	  connectionUploadLimit = rate;
	  
	  if ( connectionUploadLimit != 0 ){

		  uploadLimitManual = is_manual;
		  
		  uploadLimit = (connectionUploadLimit/5)*4;
		  
		  uploadLimit = (uploadLimit/1024)*1024;
		  
		  if ( uploadLimit < 5*1024 ){
			  
			  uploadLimit = 5*1024;
		  }
		  
		  int nbMaxActive = (int) (Math.pow(uploadLimit/1024,0.34) * 0.92);
		  int nbMaxUploads = (int) (Math.pow(uploadLimit/1024,0.25) * 1.68);
		  int nbMaxDownloads = (nbMaxActive * 4) / 5;

		  if (nbMaxDownloads == 0){
			  nbMaxDownloads = 1;
		  }
		  
		  if (nbMaxUploads > 50){
			  nbMaxUploads = 50;
		  }

		  maxActiveTorrents = nbMaxActive;
		  maxDownloads = nbMaxDownloads;
		  
	  }else{	  
		  
		  	// reset to defaults
		  
		  uploadLimitManual	= false;
		  uploadLimit 		= 0;
		  maxActiveTorrents	= 0;
		  maxDownloads		= 0;
	  }
  }
  
  protected int
  getConnectionUploadLimit()
  {
	  return( connectionUploadLimit );
  }
  
  protected int
  getUploadLimit()
  {
	  return( uploadLimit );
  }
  
  protected boolean
  isUploadLimitManual()
  {
	  return( uploadLimitManual );
  }
  
  protected int
  getWizardMode()
  {
	  return( wizard_mode );
  }
}
