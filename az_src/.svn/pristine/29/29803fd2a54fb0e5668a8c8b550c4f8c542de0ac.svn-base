/*
 * File : Wizard.java Created : 12 oct. 2003 14:30:57 By : Olivier
 * 
 * Azureus - a Java Bittorrent client
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details ( see the LICENSE file ).
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.gudy.azureus2.ui.swt.maketorrent;

import java.io.File;
import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.torrent.TOTorrentCreator;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.ui.swt.URLTransfer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.wizard.Wizard;

/**
 * @author Olivier
 *  
 */
public class 
NewTorrentWizard 
	extends Wizard 
{
	static final int	TT_LOCAL		= 1;
	static final int	TT_EXTERNAL		= 2;
	static final int	TT_DECENTRAL	= 3;
	  
	static final String	TT_EXTERNAL_DEFAULT 	= "http://";
	static final String	TT_DECENTRAL_DEFAULT	= TorrentUtils.getDecentralisedEmptyURL().toString();
	
	private static String	default_open_dir 	= COConfigurationManager.getStringParameter( "CreateTorrent.default.open", "" );
	private static String	default_save_dir 	= COConfigurationManager.getStringParameter( "CreateTorrent.default.save", "" );
	private static String	comment 			= COConfigurationManager.getStringParameter( "CreateTorrent.default.comment", "" );
	private static int 		tracker_type 		= COConfigurationManager.getIntParameter( "CreateTorrent.default.trackertype", TT_LOCAL );

	static{
			// default the default to the "save torrents to" location
		
		if ( default_save_dir.length() == 0 ){
			
			default_save_dir = COConfigurationManager.getStringParameter( "General_sDefaultTorrent_Directory", "" );
		}
	}
	
  //false : singleMode, true: directory
  boolean create_from_dir;
  String singlePath = "";
  String directoryPath = "";
  String savePath = "";
  
    
  String trackerURL = TT_EXTERNAL_DEFAULT;
  
  boolean computed_piece_size = true;
  long	  manual_piece_size;
  
  boolean 			useMultiTracker = false;
  boolean 			useWebSeed = false;
  
  private boolean 	addOtherHashes	= 	COConfigurationManager.getBooleanParameter( "CreateTorrent.default.addhashes", false );
  
  
  String multiTrackerConfig = "";
  List trackers = new ArrayList();
  
  String webSeedConfig = "";
  Map	webseeds = new HashMap();
  
  boolean autoOpen 			= false;
  boolean autoHost 			= false;
  boolean permitDHT			= true;
  boolean privateTorrent	= false;
  
  TOTorrentCreator creator = null;

  public 
  NewTorrentWizard(
	Display 		display) 
  {
    super("wizard.title");
    
    cancel.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        if(creator != null) creator.cancel();
      }
    });
    
    trackers.add(new ArrayList());
    trackerURL = Utils.getLinkFromClipboard(display,false);
    ModePanel panel = new ModePanel(this, null);
    createDropTarget(getWizardWindow());
    this.setFirstPanel(panel);
    
  }

  protected int
  getTrackerType()
  {
   	return( tracker_type );
  }
  
  protected void
  setTrackerType(
	int	type )
  {
	tracker_type = type;

	COConfigurationManager.setParameter( "CreateTorrent.default.trackertype", tracker_type );
  }
 
  protected String
  getDefaultOpenDir()
  {
  	return( default_open_dir );
  }
  
  protected void
  setDefaultOpenDir(
  	String		d )
  {
  	default_open_dir	= d;
  	
  	COConfigurationManager.setParameter( "CreateTorrent.default.open", default_open_dir );
  }
  
  protected String
  getDefaultSaveDir()
  {
  	return( default_save_dir );
  }
  
  protected void
  setDefaultSaveDir(
  	String		d )
  {
  	default_save_dir	= d;
  	
 	COConfigurationManager.setParameter( "CreateTorrent.default.save", default_save_dir );
  }
  
  void setComment(String s) {
    comment = s;
    
    COConfigurationManager.setParameter("CreateTorrent.default.comment",comment);
  }

  String getComment() {
    return (comment);
  }
  private void createDropTarget(final Control control) {
    DropTarget dropTarget = new DropTarget(control, DND.DROP_DEFAULT | DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
    dropTarget.setTransfer(new Transfer[] { URLTransfer.getInstance(), FileTransfer.getInstance()});
    dropTarget.addDropListener(new DropTargetAdapter() {
      public void dragOver(DropTargetEvent event) {
        if(URLTransfer.getInstance().isSupportedType(event.currentDataType)) {
          event.detail = getCurrentPanel() instanceof ModePanel ? DND.DROP_LINK : DND.DROP_NONE;
        }
      }
      public void drop(DropTargetEvent event) {
        if (event.data instanceof String[]) {
          String[] sourceNames = (String[]) event.data;
          if (sourceNames == null || sourceNames.length != 1)
            event.detail = DND.DROP_NONE;
          if (event.detail == DND.DROP_NONE)
            return;
          File droppedFile = new File(sourceNames[0]);
          if (getCurrentPanel() instanceof ModePanel) {
            if (droppedFile.isFile()) {
              singlePath = droppedFile.getAbsolutePath();
              ((ModePanel) getCurrentPanel()).activateMode(true);
            } else if (droppedFile.isDirectory()) {
              directoryPath = droppedFile.getAbsolutePath();
              ((ModePanel) getCurrentPanel()).activateMode(false);
            }
          } else if (getCurrentPanel() instanceof DirectoryPanel) {
            if (droppedFile.isDirectory())
              ((DirectoryPanel) getCurrentPanel()).setFilename(droppedFile.getAbsolutePath());
          } else if (getCurrentPanel() instanceof SingleFilePanel) {
            if (droppedFile.isFile())
              ((SingleFilePanel) getCurrentPanel()).setFilename(droppedFile.getAbsolutePath());
          }
         } else if (getCurrentPanel() instanceof ModePanel) {
           trackerURL = ((URLTransfer.URLType)event.data).linkURL;
           ((ModePanel) getCurrentPanel()).updateTrackerURL();
         }
       }
    });
  }

  protected void
  setPieceSizeComputed()
  {
  	computed_piece_size = true;
  }
  
  public boolean
  getPieceSizeComputed()
  {
  	return( computed_piece_size );
  }
  
  protected void
  setPieceSizeManual(
  	long	_value )
  {
  	computed_piece_size	= false;
  	manual_piece_size	= _value;
  }
  
  protected long
  getPieceSizeManual()
  {
  	return( manual_piece_size );
  }
  
  protected void
  setAddOtherHashes(
	boolean	o )
  {
	  addOtherHashes = o;
	  
	  COConfigurationManager.setParameter( "CreateTorrent.default.addhashes", addOtherHashes );
		 
  }
  
  protected boolean
  getAddOtherHashes()
  {
  	return( addOtherHashes );
  }
}