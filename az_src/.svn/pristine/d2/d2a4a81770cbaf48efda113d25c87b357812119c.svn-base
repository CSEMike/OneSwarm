/*
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.files;

import java.io.File;
import java.io.IOException;


import org.gudy.azureus2.plugins.ui.tables.*;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.ui.swt.views.FilesView;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;



public class PathItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  
  
  /** Default Constructor */
  public PathItem() {
    super("path", ALIGN_LEAD, POSITION_LAST, 200, TableManager.TABLE_TORRENT_FILES);
  }

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_CONTENT,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

  public void refresh(TableCell cell) {
    DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)cell.getDataSource();
    cell.setText(determinePath(fileInfo));
  }
  
  private static String determinePath(DiskManagerFileInfo fileInfo) {
    
    if( fileInfo == null ) {
    	return "";
    }
    
   	boolean has_link = fileInfo.getLink() != null;
   	boolean show_full_path = FilesView.show_full_path;
   	
  	DownloadManager dm = fileInfo.getDownloadManager();

   	File dl_save_path_file = dm.getAbsoluteSaveLocation();
   	
   	TOTorrent torrent = dm.getTorrent();
   	
   	if ( torrent != null && torrent.isSimpleTorrent()){
   		
   		dl_save_path_file = dl_save_path_file.getParentFile();
   	}
   	
   	String dl_save_path = dl_save_path_file.getPath();
   	if (!dl_save_path.endsWith(File.separator)) {
   		 dl_save_path += File.separator;
   	}

   	File file = fileInfo.getFile(true);
   	
   	/**
   	 * Figure out whether we should show the full path anyway.
   	 * We'll do this if the path is relative to he current
   	 * download save path.
   	 */
   	//  
   	if (has_link && !show_full_path) {
   		show_full_path = !file.getAbsolutePath().startsWith(dl_save_path);
   	}
   	String path = "";
  	
    if (show_full_path) { 
    	  
	      try {
	          path = file.getParentFile().getCanonicalPath();
	      }
	      catch( IOException e ) {
	          path = file.getParentFile().getAbsolutePath();
	      }
	      
	      if ( !path.endsWith( File.separator )){
	    	  
	    	  path += File.separator;
	      }
	      
    }else{
    	
    	path = file.getAbsolutePath().substring(dl_save_path.length());
    	if (path.length() == 0) {
    		path = File.separator;
    	}
    	else {
    		if (path.charAt(0) == File.separatorChar) {
    			path = path.substring(1);
    		}
    		int	pos = path.lastIndexOf(File.separator);
    	  
    		if (pos > 0 ) {
    			path = File.separator + path.substring( 0, pos );
    		}
    		else {
    			path = File.separator;
    		}
      }
    }
    
    return path;
  }
  
}
