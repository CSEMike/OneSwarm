/*
 * Created on 17 juil. 2003
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
package org.gudy.azureus2.ui.swt.views;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.download.DownloadManagerStateEvent;
import org.gudy.azureus2.core3.download.DownloadManagerStateListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.MessageBoxWindow;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.SimpleTextEntryWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.file.FileInfoView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.tableitems.files.*;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCoreOperation;
import com.aelitis.azureus.core.AzureusCoreOperationTask;
import com.aelitis.azureus.ui.common.RememberedDecisionsManager;
import com.aelitis.azureus.ui.common.table.*;

/**
 * @author Olivier
 * @author TuxPaper
 *         2004/Apr/23: extends TableView instead of IAbstractView
 */
public class FilesView
	extends TableViewTab
	implements TableDataSourceChangedListener, TableSelectionListener,
	TableViewSWTMenuFillListener, TableRefreshListener, DownloadManagerStateListener,
	TableLifeCycleListener
{
	boolean refreshing = false;

  private static final TableColumnCore[] basicItems = {
    new NameItem(),
    new PathItem(),
    new SizeItem(),
    new DoneItem(),
    new PercentItem(),
    new FirstPieceItem(),
    new PieceCountItem(),
    new RemainingPiecesItem(),
    new ProgressGraphItem(),
    new ModeItem(),
    new PriorityItem(),
    new StorageTypeItem(),
    new FileExtensionItem(), 
  };
  
  private DownloadManager manager = null;
  
  public static boolean show_full_path;

  static{
	  COConfigurationManager.addAndFireParameterListener(
			  "FilesView.show.full.path",
			  new ParameterListener()
			  {
				  public void 
				  parameterChanged(
					String parameterName) 
				  {
					  show_full_path = COConfigurationManager.getBooleanParameter( "FilesView.show.full.path" );
				  }
			  });
  }
  
  private MenuItem path_item;

  private TableViewSWT tv;
  

  /**
   * Initialize 
   */
	public FilesView() {
		tv = new TableViewSWTImpl(TableManager.TABLE_TORRENT_FILES, "FilesView",
				basicItems, "firstpiece", SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);
		setTableView(tv);
		tv.setRowDefaultIconSize(new Point(16, 16));
		tv.setEnableTabViews(true);
		tv.setCoreTabViews(new IView[] { new FileInfoView()
		});

		tv.addTableDataSourceChangedListener(this, true);
		tv.addRefreshListener(this, true);
		tv.addSelectionListener(this, false);
		tv.addMenuFillListener(this);
		tv.addLifeCycleListener(this);
	}

  
  // @see com.aelitis.azureus.ui.common.table.TableDataSourceChangedListener#tableDataSourceChanged(java.lang.Object)
  public void tableDataSourceChanged(Object newDataSource) {
	  DownloadManager old_manager = manager;
		if (newDataSource == null)
			manager = null;
		else if (newDataSource instanceof Object[])
			manager = (DownloadManager)((Object[])newDataSource)[0];
		else
			manager = (DownloadManager)newDataSource;
		
		if (old_manager != null) {
			old_manager.getDownloadState().removeListener(this);
		}
		if (manager != null) {
			manager.getDownloadState().addListener(this);
		}

		tv.removeAllTableRows();
	}
	
	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#deselected(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void deselected(TableRowCore[] rows) {
	}

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#focusChanged(com.aelitis.azureus.ui.common.table.TableRowCore)
	public void focusChanged(TableRowCore focus) {
	}

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#selected(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void selected(TableRowCore[] rows) {
	}

	// @see com.aelitis.azureus.ui.common.table.TableSelectionListener#defaultSelected(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void defaultSelected(TableRowCore[] rows) {
		DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) tv.getFirstSelectedDataSource();
		if (fileInfo != null
				&& fileInfo.getAccessMode() == DiskManagerFileInfo.READ)
			Utils.launch(fileInfo.getFile(true).toString());
	}

	// @see org.gudy.azureus2.ui.swt.views.TableViewSWTMenuFillListener#fillMenu(org.eclipse.swt.widgets.Menu)
	public void fillMenu(final Menu menu) {
		Shell shell = menu.getShell();
		Object[] infos = tv.getSelectedDataSources();
		boolean hasSelection = (infos.length > 0);

    final MenuItem itemOpen = new MenuItem(menu, SWT.PUSH);
    Messages.setLanguageText(itemOpen, "FilesView.menu.open");
    Utils.setMenuItemImage(itemOpen, "run");
    // Invoke open on enter, double click
    menu.setDefaultItem(itemOpen);

	// Explore  (Copied from MyTorrentsView)
	final boolean use_open_containing_folder = COConfigurationManager.getBooleanParameter("MyTorrentsView.menu.show_parent_folder_enabled");
	final MenuItem itemExplore = new MenuItem(menu, SWT.PUSH);
	Messages.setLanguageText(itemExplore, "MyTorrentsView.menu." + (use_open_containing_folder ? "open_parent_folder" : "explore"));
	itemExplore.addListener(SWT.Selection, new Listener() {
		public void handleEvent(Event event) {
		    Object[] dataSources = tv.getSelectedDataSources();
		    for (int i = dataSources.length - 1; i >= 0; i--) {
		    	DiskManagerFileInfo info = (DiskManagerFileInfo)dataSources[i];
		    	if (info != null) {
		    		File this_file = info.getFile(true);
		    		File parent_file = (use_open_containing_folder) ? this_file.getParentFile() : null;
		    		ManagerUtils.open((parent_file == null) ? this_file : parent_file);
		    	}
		    }
		}
	});
	itemExplore.setEnabled(hasSelection);

	MenuItem itemRenameOrRetarget = null, itemRename = null, itemRetarget = null;

	itemRenameOrRetarget = new MenuItem(menu, SWT.PUSH);
	Messages.setLanguageText(itemRenameOrRetarget, "FilesView.menu.rename");
	itemRenameOrRetarget.setData("rename", Boolean.valueOf(true));
	itemRenameOrRetarget.setData("retarget", Boolean.valueOf(true));
	
	itemRename = new MenuItem(menu, SWT.PUSH);
	itemRetarget = new MenuItem(menu, SWT.PUSH);
	Messages.setLanguageText(itemRename, "FilesView.menu.rename_only");
	Messages.setLanguageText(itemRetarget, "FilesView.menu.retarget");
	
	itemRename.setData("rename", Boolean.valueOf(true));
	itemRename.setData("retarget", Boolean.valueOf(false));
	itemRetarget.setData("rename", Boolean.valueOf(false));
	itemRetarget.setData("retarget", Boolean.valueOf(true));
		
    final MenuItem itemPriority = new MenuItem(menu, SWT.CASCADE);
    Messages.setLanguageText(itemPriority, "FilesView.menu.setpriority"); //$NON-NLS-1$
    
    final Menu menuPriority = new Menu(shell, SWT.DROP_DOWN);
    itemPriority.setMenu(menuPriority);
    
    final MenuItem itemHigh = new MenuItem(menuPriority, SWT.CASCADE);
    itemHigh.setData("Priority", new Integer(0));
    Messages.setLanguageText(itemHigh, "FilesView.menu.setpriority.high"); //$NON-NLS-1$
    
    final MenuItem itemLow = new MenuItem(menuPriority, SWT.CASCADE);
    itemLow.setData("Priority", new Integer(1));
    Messages.setLanguageText(itemLow, "FilesView.menu.setpriority.normal"); //$NON-NLS-1$
    
    final MenuItem itemSkipped = new MenuItem(menuPriority, SWT.CASCADE);
    itemSkipped.setData("Priority", new Integer(2));
    Messages.setLanguageText(itemSkipped, "FilesView.menu.setpriority.skipped"); //$NON-NLS-1$

    final MenuItem itemDelete = new MenuItem(menuPriority, SWT.CASCADE);
    itemDelete.setData("Priority", new Integer(3));
    Messages.setLanguageText(itemDelete, "wizard.multitracker.delete");	// lazy but we're near release

    new MenuItem(menu, SWT.SEPARATOR);

	if (!hasSelection) {
		itemOpen.setEnabled(false);
		itemPriority.setEnabled(false);
		itemRenameOrRetarget.setEnabled(false);
		itemRename.setEnabled(false);
		itemRetarget.setEnabled(false);
		return;
	}

	boolean open 				= true;
	boolean all_compact 		= true;
	boolean	all_skipped			= true;
	boolean	all_priority		= true;
	boolean	all_not_priority	= true;
		
	DiskManagerFileInfo[] dmi_array = new DiskManagerFileInfo[infos.length];
	System.arraycopy(infos, 0, dmi_array, 0, infos.length);
	int[] storage_types = manager.getStorageType(dmi_array);
		
		for (int i = 0; i < infos.length; i++) {
			
			DiskManagerFileInfo file_info = (DiskManagerFileInfo) infos[i];

			if (open && file_info.getAccessMode() != DiskManagerFileInfo.READ) {

				open = false;
			}

			if (all_compact && storage_types[i] != DiskManagerFileInfo.ST_COMPACT) {
				all_compact = false;
			}

			if (all_skipped || all_priority || all_not_priority) {
				if ( file_info.isSkipped()){
					all_priority		= false;
					all_not_priority	= false;
				}else{
					all_skipped = false;
					
					// Only do this check if we need to.
					if (all_not_priority || all_priority) {
						if (file_info.isPriority()){
							all_not_priority = false;
						}else{
							all_priority = false;
						}
					}
				}
			}
		}

		// we can only open files if they are read-only

		itemOpen.setEnabled(open);

		// can't rename files for non-persistent downloads (e.g. shares) as these
		// are managed "externally"

		itemRenameOrRetarget.setEnabled(manager.isPersistent());
		itemRename.setEnabled(manager.isPersistent());
		itemRetarget.setEnabled(manager.isPersistent());

		itemSkipped.setEnabled( !all_skipped );
	
		itemHigh.setEnabled( !all_priority );
		
		itemLow.setEnabled( !all_not_priority );
		
		itemDelete.setEnabled( !all_compact );

    itemOpen.addListener(SWT.Selection, new TableSelectedRowsListener(tv) {
      public void run(TableRowCore row) {
        DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)row.getDataSource(true);
        if (fileInfo.getAccessMode() == DiskManagerFileInfo.READ) {
        	Utils.launch(fileInfo.getFile(true).toString());
        }
      }
    });
    
    Listener rename_listener = new Listener() {
    	public void handleEvent(Event event) {
    		boolean rename_it = ((Boolean)event.widget.getData("rename")).booleanValue();
    		boolean retarget_it = ((Boolean)event.widget.getData("retarget")).booleanValue();
    		rename(tv.getSelectedRows(), rename_it, retarget_it);
    	}
    };
    
   	itemRenameOrRetarget.addListener(SWT.Selection, rename_listener);
   	itemRename.addListener(SWT.Selection, rename_listener);
   	itemRetarget.addListener(SWT.Selection, rename_listener);
    
    Listener priorityListener = new Listener() {
			public void handleEvent(Event event) {
				changePriority(((Integer) event.widget.getData("Priority")).intValue(),
						tv.getSelectedRows());
			}
    };

    itemHigh.addListener(SWT.Selection, priorityListener); 
    itemLow.addListener(SWT.Selection, priorityListener);
    itemSkipped.addListener(SWT.Selection, priorityListener); 
    itemDelete.addListener(SWT.Selection, priorityListener);
	}

	private String askForRenameFilename(DiskManagerFileInfo fileInfo) {
		SimpleTextEntryWindow dialog = new SimpleTextEntryWindow(Display.getDefault());
		dialog.setTitle("FilesView.rename.filename.title");
		dialog.setMessage("FilesView.rename.filename.text");
		dialog.setPreenteredText(fileInfo.getFile(true).getName(), false); // false -> it's not "suggested", it's a previous value
		dialog.allowEmptyInput(false);
		dialog.prompt();
		if (!dialog.hasSubmittedInput()) {return null;}
		return dialog.getSubmittedInput();
	}
	
	private String askForRetargetedFilename(DiskManagerFileInfo fileInfo) {
		FileDialog fDialog = new FileDialog(Utils.findAnyShell(), SWT.SYSTEM_MODAL | SWT.SAVE);  
		File existing_file = fileInfo.getFile(true);
		fDialog.setFilterPath(existing_file.getParent());
		fDialog.setFileName(existing_file.getName());
		fDialog.setText(MessageText.getString("FilesView.rename.choose.path"));
		return fDialog.open();
	}
	
	private String askForSaveDirectory(DiskManagerFileInfo fileInfo) {
		DirectoryDialog dDialog = new DirectoryDialog(Utils.findAnyShell(), SWT.SYSTEM_MODAL | SWT.SAVE);
		File current_dir = fileInfo.getFile(true).getParentFile();
		dDialog.setFilterPath(current_dir.getPath());
		dDialog.setText(MessageText.getString("FilesView.rename.choose.path.dir"));
		return dDialog.open();
	}
	
	private boolean askCanOverwrite(File file) {
		return MessageBoxWindow.open( 
				"FilesView.messagebox.rename.id",
				SWT.OK | SWT.CANCEL,
				SWT.OK, true,
				Display.getDefault(), 
				MessageBoxWindow.ICON_WARNING,
				MessageText.getString( "FilesView.rename.confirm.delete.title" ),
				MessageText.getString( "FilesView.rename.confirm.delete.text", new String[]{ file.toString()})) == SWT.OK;
	}
	
	// same code is used in tableitems.files.NameItem
	private void moveFile(final DiskManagerFileInfo fileInfo, final File target) {

		// this behaviour should be put further down in the core but I'd rather not
		// do so close to release :(
		final boolean[] result = { false };

		is_changing_links = true;
		FileUtil.runAsTask(new AzureusCoreOperationTask() {
			public void run(AzureusCoreOperation operation) {
					result[0] = fileInfo.setLink(target);
				}
			}
		);
		is_changing_links = false;

		if (!result[0]){
			MessageBox mb = new MessageBox(Utils.findAnyShell(), SWT.ICON_ERROR | SWT.OK);
			mb.setText(MessageText.getString("FilesView.rename.failed.title"));
			mb.setMessage(MessageText.getString("FilesView.rename.failed.text"));
			mb.open();	    					
		}

	}
  
	protected void rename(TableRowCore[] rows, boolean rename_it, boolean retarget_it) {
	 	if (manager == null) {return;}
	 	if (rows.length == 0) {return;}
	 	
	 	String save_dir = null;
	 	if (!rename_it && retarget_it) {
	 		save_dir = askForSaveDirectory((DiskManagerFileInfo)rows[0].getDataSource(true));
	 		if (save_dir == null) {return;}
	 	}
	 	
		boolean	paused = false;
		try {
			for (int i=0; i<rows.length; i++) {
				TableRowCore row = rows[i];
				DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)rows[i].getDataSource(true);
				File existing_file = fileInfo.getFile(true);
				File f_target = null;
				if (rename_it && retarget_it) {
					String s_target = askForRetargetedFilename(fileInfo);
					if (s_target != null)
						f_target = new File(s_target);
				}
				else if (rename_it) {
					String s_target = askForRenameFilename(fileInfo);
					if (s_target != null)
						f_target = new File(existing_file.getParentFile(), s_target);
				}
				else {
					// Parent directory has changed.
					f_target = new File(save_dir, existing_file.getName());
				}
				
				// So are we doing a rename?
				// If the user has decided against it - abort the op.
				if (f_target == null) {return;}
			
			    if (!paused) {paused = manager.pause();}
			    
    			if (f_target.exists()){
    				
    				// Nothing to do.
    				if (f_target.equals(existing_file))
    					continue;
    					
    				// A rewrite will occur, so we need to ask the user's permission.
    				else if (existing_file.exists() && !askCanOverwrite(existing_file))
    					continue;
    				
    				// If we reach here, then it means we are doing a real move, but there is
    				// no existing file.
    			}
    					
    			moveFile(fileInfo, f_target);
    			row.invalidate();
			}
		}
		finally {
			if (paused){manager.resume();}
		}
	}
	
	
  private void
  changePriority(
	  int				type ,
	  TableRowCore[]	rows )
  {
	  	if ( manager == null){
	  		
	  		return;
	  	}
  				
		boolean	paused = false;
		boolean has_tried_pausing = false;

		try{
	
			int	this_paused;
			
			for (int i=0;i<rows.length;i++){

				DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)rows[i].getDataSource(true);

				if ( type == 0){
					
		   			fileInfo.setPriority(true);
		   			
					this_paused = setSkipped( fileInfo, false, false, !has_tried_pausing);
					
				}else if ( type == 1 ){
					
		 			fileInfo.setPriority(false);
		 			
		 			this_paused = setSkipped( fileInfo, false, false, !has_tried_pausing);
					
				}else if ( type == 2 ){
					
					this_paused = setSkipped( fileInfo, true, false, !has_tried_pausing);
					
				}else{
					
					this_paused = setSkipped( fileInfo, true, true, !has_tried_pausing);
				}
				
				if (this_paused != 0) {has_tried_pausing = true;}
				paused = paused || (this_paused == 1);
			}
		}finally{
			
			if ( paused ){
			
				manager.resume();
			}
		}
  }
  
  // Returns:
  //   -1: Tried to pause, but it didn't need pausing.
  //    1: Tried to pause, and it was paused.
  //    0: Didn't attempt to pause.
  private int
  setSkipped(
	 DiskManagerFileInfo	info,
	 boolean				skipped,
	 boolean				delete_action,
	 boolean                try_to_pause)
  {
		// if we're not managing the download then don't do anything other than
		// change the file's priority
	
	if ( !manager.isPersistent()){
		
		info.setSkipped( skipped );

		return 0;
	}
	
	File	existing_file 			= info.getFile(true);	
	int		existing_storage_type	= info.getStorageType();
	
		// we can't ever have storage = COMPACT and !skipped
		
	int		new_storage_type;
	
	if ( existing_file.exists()){
		
		if (!skipped ){
			
			new_storage_type	= DiskManagerFileInfo.ST_LINEAR;
			
		}else{
	
			boolean	delete_file;
			
			if ( delete_action ){
				
				delete_file =
					MessageBoxWindow.open( 
						"FilesView.messagebox.delete.id",
						SWT.OK | SWT.CANCEL,
						SWT.OK, true,
						Display.getDefault(), 
						MessageBoxWindow.ICON_WARNING,
						MessageText.getString( "FilesView.rename.confirm.delete.title" ),
						MessageText.getString( "FilesView.rename.confirm.delete.text", new String[]{ existing_file.toString()})) == SWT.OK;
				
			}else{
				
					// OK, too many users have got confused over the option to truncate files when selecting
					// do-not-download so I'm removing it
				
				delete_file	= false;
				
				/*
				delete_file =
					MessageBoxWindow.open( 
						"FilesView.messagebox.skip.id",
						SWT.YES | SWT.NO,
						SWT.YES | SWT.NO,
						getComposite().getDisplay(), 
						MessageBoxWindow.ICON_WARNING,
						MessageText.getString( "FilesView.rename.confirm.delete.title" ),
						MessageText.getString( "FilesView.skip.confirm.delete.text", new String[]{ existing_file.toString()})) == SWT.YES;
				*/
			}

			if ( delete_file ){
				
				new_storage_type	= DiskManagerFileInfo.ST_COMPACT;

			}else{
				
				new_storage_type	= DiskManagerFileInfo.ST_LINEAR;
			}
		}
	}else{
		
		if ( skipped ){
			
			boolean compact_disabled = RememberedDecisionsManager.getRememberedDecision(
						"FilesView.messagebox.skip.id", SWT.YES | SWT.NO) == SWT.NO;

			if ( compact_disabled ){
				
				new_storage_type	= DiskManagerFileInfo.ST_LINEAR;
				
			}else{
				
				new_storage_type	= DiskManagerFileInfo.ST_COMPACT;
			}
		}else{
			
			new_storage_type	= DiskManagerFileInfo.ST_LINEAR;

		}
	}
	
	boolean	ok;
	
	int	paused	= 0; // Did we try.
	
	if ( existing_storage_type != new_storage_type ){
		
		if (try_to_pause && new_storage_type == DiskManagerFileInfo.ST_COMPACT ){
			
			paused = (manager.pause()) ? 1 : -1;
		}
		
		ok = info.setStorageType( new_storage_type );
		
	}else{
		
		ok = true;
	}
	
	if ( ok ){
		
		info.setSkipped( skipped );
	}
	
	return( paused );
  }

  // @see com.aelitis.azureus.ui.common.table.TableRefreshListener#tableRefresh()
  private boolean force_refresh = false;
  public void tableRefresh() {
  	if (refreshing)
  		return;

  	try {
	  	refreshing = true;
	    if (tv.isDisposed())
	      return;
	
	    DiskManagerFileInfo files[] = getFileInfo();

	    if (files != null && (this.force_refresh || !doAllExist(files))) {
	    	this.force_refresh = false;

	    	Object[] datasources = tv.getDataSources();
	    	if(datasources.length == files.length)
	    	{
	    		// check if we actually have to replace anything
	    		ArrayList toAdd = new ArrayList(Arrays.asList(files));
		    	ArrayList toRemove = new ArrayList();
		    	for(int i = 0;i < datasources.length;i++)
		    	{
		    		DiskManagerFileInfo info = (DiskManagerFileInfo)datasources[i];
		    		
		    		if(files[info.getIndex()] == info)
		    			toAdd.set(info.getIndex(), null);
		    		else
		    			toRemove.add(info);
		    	}
		    	tv.removeDataSources(toRemove.toArray());
		    	tv.addDataSources(toAdd.toArray());
		    	((TableViewSWTImpl)tv).tableInvalidate();
	    	} else
	    	{
		    	tv.removeAllTableRows();
	    		
			    Object filesCopy[] = new Object[files.length]; 
			    System.arraycopy(files, 0, filesCopy, 0, files.length);

			    tv.addDataSources(filesCopy);
	    	}

		    tv.processDataSourceQueue();
	    }
  	} finally {
  		refreshing = false;
  	}
  }
  
  /**
	 * @param files
	 * @return
	 *
	 * @since 3.0.0.7
	 */
	private boolean doAllExist(DiskManagerFileInfo[] files) {
		for (int i = 0; i < files.length; i++) {
			DiskManagerFileInfo fileinfo = files[i];

			// We can't just use tv.dataSourceExists(), since it does a .equals()
			// comparison, and we want a reference comparison
			TableRowCore row = tv.getRow(fileinfo);
			if (row == null) {
				return false;
			}
			// reference comparison
			if (row.getDataSource(true) != fileinfo) {
				return false;
			}
		}
		return true;
	}

  /* SubMenu for column specific tasks.
   */
  public void addThisColumnSubMenu(String sColumnName, Menu menuThisColumn) {

    if (sColumnName.equals("path")) {
      path_item = new MenuItem( menuThisColumn, SWT.CHECK );
      
      menuThisColumn.addListener( SWT.Show, new Listener() {
        public void handleEvent(Event e) {
          path_item.setSelection( show_full_path );
        }
      });
      
      Messages.setLanguageText(path_item, "FilesView.fullpath");
      
      path_item.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          show_full_path = path_item.getSelection();
          tv.columnInvalidate("path");
          tv.refreshTable(false);
          COConfigurationManager.setParameter( "FilesView.show.full.path", show_full_path );
        }
      });
      
    }
  }
  
  
  private DiskManagerFileInfo[]
  getFileInfo()
  {
  	if (manager == null)
  		return null;
	  return( manager.getDiskManagerFileInfo());
  }
  
  // Used to notify us of when we need to refresh - normally for external changes to the
  // file links.
  private boolean is_changing_links = false;
  public void stateChanged(DownloadManagerState state, DownloadManagerStateEvent event) {
	  if (is_changing_links) {return;}
	  if (!DownloadManagerState.AT_FILE_LINKS.equals(event.getData())) {return;}
	  if (event.getType() != DownloadManagerStateEvent.ET_ATTRIBUTE_WRITTEN) {return;}
	  this.force_refresh = true;
  }
  
  public void tableViewInitialized() {}
  public void tableViewDestroyed() {
	  if (manager != null) {
		  manager.getDownloadState().removeListener(this);
	  }
  }
  
}
