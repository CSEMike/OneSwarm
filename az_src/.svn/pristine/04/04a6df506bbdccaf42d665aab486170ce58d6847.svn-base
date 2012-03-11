/**
 * Created on May 12, 2010
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package org.gudy.azureus2.ui.swt.views;

import java.io.File;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.plugins.sharing.ShareManager;
import org.gudy.azureus2.plugins.ui.UIInputReceiver;
import org.gudy.azureus2.plugins.ui.UIInputReceiverListener;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.SimpleTextEntryWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.sharing.ShareUtils;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCoreOperation;
import com.aelitis.azureus.core.AzureusCoreOperationTask;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableView;

/**
 * @author TuxPaper
 * @created May 12, 2010
 *
 */
public class FilesViewMenuUtil
{
	public static final Object PRIORITY_HIGH = Integer.valueOf(0);
	public static final Object PRIORITY_NORMAL = Integer.valueOf(1);
	public static final Object PRIORITY_NUMERIC = Integer.valueOf(99);
	public static final Object PRIORITY_SKIPPED = Integer.valueOf(2);
	public static final Object PRIORITY_DELETE = Integer.valueOf(3);

	public static void fillMenu(final TableView tv,
			final Menu menu, final DownloadManager manager,
			final Object[] data_sources) {
		Shell shell = menu.getShell();
		boolean hasSelection = (data_sources.length > 0);

		final MenuItem itemOpen = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemOpen, "FilesView.menu.open");
		Utils.setMenuItemImage(itemOpen, "run");
		// Invoke open on enter, double click
		menu.setDefaultItem(itemOpen);

		// Explore  (Copied from MyTorrentsView)
		final boolean use_open_containing_folder = COConfigurationManager.getBooleanParameter("MyTorrentsView.menu.show_parent_folder_enabled");
		final MenuItem itemExplore = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemExplore, "MyTorrentsView.menu."
				+ (use_open_containing_folder ? "open_parent_folder" : "explore"));
		itemExplore.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				for (int i = data_sources.length - 1; i >= 0; i--) {
					DiskManagerFileInfo info = (DiskManagerFileInfo) data_sources[i];
					if (info != null) {
						ManagerUtils.open(info, use_open_containing_folder);
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

			// personal share
		
		final MenuItem itemPersonalShare = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemPersonalShare, "MyTorrentsView.menu.create_personal_share");
		
			// priority
		
		final MenuItem itemPriority = new MenuItem(menu, SWT.CASCADE);
		Messages.setLanguageText(itemPriority, "FilesView.menu.setpriority");

		final Menu menuPriority = new Menu(shell, SWT.DROP_DOWN);
		itemPriority.setMenu(menuPriority);

		final MenuItem itemHigh = new MenuItem(menuPriority, SWT.CASCADE);
		itemHigh.setData("Priority", PRIORITY_HIGH);
		Messages.setLanguageText(itemHigh, "FilesView.menu.setpriority.high"); 

		final MenuItem itemLow = new MenuItem(menuPriority, SWT.CASCADE);
		itemLow.setData("Priority", PRIORITY_NORMAL);
		Messages.setLanguageText(itemLow, "FilesView.menu.setpriority.normal");

		final MenuItem itemNumeric = new MenuItem(menuPriority, SWT.CASCADE);
		itemNumeric.setData("Priority", PRIORITY_NUMERIC);
		Messages.setLanguageText(itemNumeric, "FilesView.menu.setpriority.numeric"); 

		final MenuItem itemSkipped = new MenuItem(menuPriority, SWT.CASCADE);
		itemSkipped.setData("Priority", PRIORITY_SKIPPED);
		Messages.setLanguageText(itemSkipped, "FilesView.menu.setpriority.skipped"); 

		final MenuItem itemDelete = new MenuItem(menuPriority, SWT.CASCADE);
		itemDelete.setData("Priority", PRIORITY_DELETE);
		Messages.setLanguageText(itemDelete, "wizard.multitracker.delete"); // lazy but we're near release

		new MenuItem(menu, SWT.SEPARATOR);

		if (!hasSelection) {
			itemOpen.setEnabled(false);
			itemPriority.setEnabled(false);
			itemRenameOrRetarget.setEnabled(false);
			itemRename.setEnabled(false);
			itemRetarget.setEnabled(false);
			itemPersonalShare.setEnabled(false);
			
			return;
		}

		boolean open = true;
		boolean all_compact = true;
		boolean all_skipped = true;
		boolean all_priority = true;
		boolean all_not_priority = true;
		boolean	all_complete	 = true;
		
		final DiskManagerFileInfo[] dmi_array = new DiskManagerFileInfo[data_sources.length];

		System.arraycopy(data_sources, 0, dmi_array, 0, data_sources.length);

		int[] storage_types = manager.getStorageType(dmi_array);

		for (int i = 0; i < dmi_array.length; i++) {

			DiskManagerFileInfo file_info = dmi_array[i];

			if (open && file_info.getAccessMode() != DiskManagerFileInfo.READ) {

				open = false;
			}

			if (all_compact && storage_types[i] != DiskManagerFileInfo.ST_COMPACT && storage_types[i] != DiskManagerFileInfo.ST_REORDER_COMPACT ) {
				all_compact = false;
			}

			if (all_skipped || all_priority || all_not_priority) {
				if (file_info.isSkipped()) {
					all_priority = false;
					all_not_priority = false;
				} else {
					all_skipped = false;

					// Only do this check if we need to.
					if (all_not_priority || all_priority) {
						if (file_info.getPriority() > 0 ) {
							all_not_priority = false;
						} else {
							all_priority = false;
						}
					}
				}
			}
			
			if ( 	file_info.getDownloaded() != file_info.getLength() ||
					file_info.getFile( true ).length() != file_info.getLength()){
				
				all_complete = false;
			}
		}

		// we can only open files if they are read-only

		itemOpen.setEnabled(open);

		// can't rename files for non-persistent downloads (e.g. shares) as these
		// are managed "externally"

		itemRenameOrRetarget.setEnabled(manager.isPersistent());
		itemRename.setEnabled(manager.isPersistent());
		itemRetarget.setEnabled(manager.isPersistent());
		
			// only enable for single files - people prolly don't expect a multi-selection to result
			// in multiple shares, rather they would expect one share with the files they selected
			// which we don't support
		
		itemPersonalShare.setEnabled( all_complete && dmi_array.length == 1 );
		
		itemSkipped.setEnabled(!all_skipped);

		itemHigh.setEnabled(!all_priority);

		itemLow.setEnabled(!all_not_priority);

		itemDelete.setEnabled(!all_compact);

		itemOpen.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				for (int i = 0; i < data_sources.length; i++) {
					DiskManagerFileInfo info = (DiskManagerFileInfo) data_sources[i];
					if (info != null && info.getAccessMode() == DiskManagerFileInfo.READ) {
						Utils.launch(info);
					}
				}
			}
		});

		Listener rename_listener = new Listener() {
			public void handleEvent(Event event) {
				final boolean rename_it = ((Boolean) event.widget.getData("rename")).booleanValue();
				final boolean retarget_it = ((Boolean) event.widget.getData("retarget")).booleanValue();
				rename(tv, manager, data_sources, rename_it, retarget_it);
			}
		};

		itemRenameOrRetarget.addListener(SWT.Selection, rename_listener);
		itemRename.addListener(SWT.Selection, rename_listener);
		itemRetarget.addListener(SWT.Selection, rename_listener);

		itemPersonalShare.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				Map<String,String>	properties = new HashMap<String, String>();
				
				properties.put( ShareManager.PR_PERSONAL, "true" );
				
				for (int i = 0; i < dmi_array.length; i++) {

					DiskManagerFileInfo file_info = dmi_array[i];
				
					File file = file_info.getFile( true );
					
					if ( file.isFile()){
					
						ShareUtils.shareFile( file.getAbsolutePath(), properties );
						
					}else if ( file.isDirectory()){
						
						ShareUtils.shareDir( file.getAbsolutePath(), properties );
					}
				}
			}
		});
		
		
		Listener priorityListener = new Listener() {
			public void handleEvent(Event event) {
				final int priority = ((Integer) event.widget.getData("Priority")).intValue();
				Utils.getOffOfSWTThread(new AERunnable() {
					public void runSupport() {
						changePriority(priority, data_sources);
					}
				});
			}
		};

		itemNumeric.addListener(SWT.Selection, priorityListener);
		itemHigh.addListener(SWT.Selection, priorityListener);
		itemLow.addListener(SWT.Selection, priorityListener);
		itemSkipped.addListener(SWT.Selection, priorityListener);
		itemDelete.addListener(SWT.Selection, priorityListener);
	}

	public static void rename(final TableView tv, final DownloadManager manager,
			final Object[] datasources, boolean rename_it, boolean retarget_it) {
		if (manager == null) {
			return;
		}
		if (datasources.length == 0) {
			return;
		}

		String save_dir = null;
		if (!rename_it && retarget_it) {
			save_dir = askForSaveDirectory((DiskManagerFileInfo) datasources[0]);
			if (save_dir == null) {
				return;
			}
		}

		boolean paused = false;
		try {
			for (int i = 0; i < datasources.length; i++) {
				final DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) datasources[i];
				File existing_file = fileInfo.getFile(true);
				File f_target = null;
				if (rename_it && retarget_it) {
					String s_target = askForRetargetedFilename(fileInfo);
					if (s_target != null)
						f_target = new File(s_target);
				} else if (rename_it) {
					String s_target = askForRenameFilename(fileInfo);
					if (s_target != null)
						f_target = new File(existing_file.getParentFile(), s_target);
				} else {
					// Parent directory has changed.
					f_target = new File(save_dir, existing_file.getName());
				}

				// So are we doing a rename?
				// If the user has decided against it - abort the op.
				if (f_target == null) {
					return;
				}

				if (!paused) {
					paused = manager.pause();
				}

				if (f_target.exists()) {

					// Nothing to do.
					if (f_target.equals(existing_file))
						continue;

					// A rewrite will occur, so we need to ask the user's permission.
					else if (existing_file.exists() && !askCanOverwrite(existing_file))
						continue;

					// If we reach here, then it means we are doing a real move, but there is
					// no existing file.
				}

				final File ff_target = f_target;
				final TableRowCore row = tv == null ? null : tv.getRow(datasources[i]);
				Utils.getOffOfSWTThread(new AERunnable() {
					public void runSupport() {
						moveFile(manager, fileInfo, ff_target);
						if (row != null) {
							row.invalidate();
						}
					}
				});
			}
		} finally {
			if (paused) {
				manager.resume();
			}
		}
	}

	public static void changePriority(Object type, final Object[] datasources) {

		if (datasources == null || datasources.length == 0) {
			return;
		}

		if (type == PRIORITY_NUMERIC) {
			changePriorityManual(datasources);
			return;
		}

		Map<DownloadManager, ArrayList<DiskManagerFileInfo>> mapDMtoDMFI = new HashMap<DownloadManager, ArrayList<DiskManagerFileInfo>>();

		DiskManagerFileInfo[] file_infos = new DiskManagerFileInfo[datasources.length];
		for (int i = 0; i < datasources.length; i++) {
			file_infos[i] = (DiskManagerFileInfo) datasources[i];

			DownloadManager dm = file_infos[i].getDownloadManager();
			ArrayList<DiskManagerFileInfo> listFileInfos = mapDMtoDMFI.get(dm);
			if (listFileInfos == null) {
				listFileInfos = new ArrayList<DiskManagerFileInfo>(1);
				mapDMtoDMFI.put(dm, listFileInfos);
			}
			listFileInfos.add(file_infos[i]);

			if (type == PRIORITY_NORMAL || type == PRIORITY_HIGH) {
				file_infos[i].setPriority(type == PRIORITY_HIGH ? 1 : 0);
			}
		}
		boolean skipped = (type == PRIORITY_SKIPPED || type == PRIORITY_DELETE);
		boolean delete_action = (type == PRIORITY_DELETE);
		for (DownloadManager dm : mapDMtoDMFI.keySet()) {
			ArrayList<DiskManagerFileInfo> list = mapDMtoDMFI.get(dm);
			DiskManagerFileInfo[] fileInfos = list.toArray(new DiskManagerFileInfo[0]);
			boolean paused = setSkipped(dm, fileInfos, skipped, delete_action);

			if (paused) {

				dm.resume();
			}
		}
	}

	private static void changePriorityManual(final Object[] datasources) {

		SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow(
				"FilesView.dialog.priority.title",
				"FilesView.dialog.priority.text");
		entryWindow.prompt(new UIInputReceiverListener() {
			public void UIInputReceiverClosed(UIInputReceiver entryWindow) {
				if (!entryWindow.hasSubmittedInput()) {
					return;
				}
				String sReturn = entryWindow.getSubmittedInput();
				
				if (sReturn == null)
					return;
				
				int priority = -1;
				try {
					priority = Integer.valueOf(sReturn).intValue();
				} catch (NumberFormatException er) {
					// Ignore
				}
				
				if ( priority >= 0 ){
					Map<DownloadManager, ArrayList<DiskManagerFileInfo>> mapDMtoDMFI = new HashMap<DownloadManager, ArrayList<DiskManagerFileInfo>>();

					DiskManagerFileInfo[] file_infos = new DiskManagerFileInfo[datasources.length];
					for (int i = 0; i < datasources.length; i++) {
						file_infos[i] = (DiskManagerFileInfo) datasources[i];

						DownloadManager dm = file_infos[i].getDownloadManager();
						ArrayList<DiskManagerFileInfo> listFileInfos = mapDMtoDMFI.get(dm);
						if (listFileInfos == null) {
							listFileInfos = new ArrayList<DiskManagerFileInfo>(1);
							mapDMtoDMFI.put(dm, listFileInfos);
						}
						listFileInfos.add(file_infos[i]);

						file_infos[i].setPriority(priority);
					}

					for (DownloadManager dm : mapDMtoDMFI.keySet()) {
						ArrayList<DiskManagerFileInfo> list = mapDMtoDMFI.get(dm);
						DiskManagerFileInfo[] fileInfos = list.toArray(new DiskManagerFileInfo[0]);
						boolean paused = setSkipped(dm, fileInfos, false, false);

						if (paused) {

							dm.resume();
						}
					}

				}
			}
		});
	}

	private static String askForRenameFilename(DiskManagerFileInfo fileInfo) {
		SimpleTextEntryWindow dialog = new SimpleTextEntryWindow(
				"FilesView.rename.filename.title", "FilesView.rename.filename.text");
		dialog.setPreenteredText(fileInfo.getFile(true).getName(), false); // false -> it's not "suggested", it's a previous value
		dialog.allowEmptyInput(false);
		dialog.prompt();
		if (!dialog.hasSubmittedInput()) {
			return null;
		}
		return dialog.getSubmittedInput();
	}

	private static String askForRetargetedFilename(DiskManagerFileInfo fileInfo) {
		FileDialog fDialog = new FileDialog(Utils.findAnyShell(), SWT.SYSTEM_MODAL
				| SWT.SAVE);
		File existing_file = fileInfo.getFile(true);
		fDialog.setFilterPath(existing_file.getParent());
		fDialog.setFileName(existing_file.getName());
		fDialog.setText(MessageText.getString("FilesView.rename.choose.path"));
		return fDialog.open();
	}

	private static String askForSaveDirectory(DiskManagerFileInfo fileInfo) {
		DirectoryDialog dDialog = new DirectoryDialog(Utils.findAnyShell(),
				SWT.SYSTEM_MODAL | SWT.SAVE);
		File current_dir = fileInfo.getFile(true).getParentFile();
		dDialog.setFilterPath(current_dir.getPath());
		dDialog.setText(MessageText.getString("FilesView.rename.choose.path.dir"));
		return dDialog.open();
	}

	private static boolean askCanOverwrite(File file) {
		MessageBoxShell mb = new MessageBoxShell(SWT.OK | SWT.CANCEL,
				MessageText.getString("FilesView.rename.confirm.delete.title"),
				MessageText.getString("FilesView.rename.confirm.delete.text",
						new String[] {
							file.toString()
						}));
		mb.setDefaultButtonUsingStyle(SWT.OK);
		mb.setRememberOnlyIfButton(0);
		mb.setRemember("FilesView.messagebox.rename.id", true, null);
		mb.setLeftImage(SWT.ICON_WARNING);
		mb.open(null);
		return mb.waitUntilClosed() == SWT.OK;
	}

	// same code is used in tableitems.files.NameItem
	private static void moveFile(final DownloadManager manager,
			final DiskManagerFileInfo fileInfo, final File target) {

		// this behaviour should be put further down in the core but I'd rather not
		// do so close to release :(

		manager.setUserData("is_changing_links", true);
		try {
			final boolean[] result = {
				false
			};

			FileUtil.runAsTask(new AzureusCoreOperationTask() {
				public void run(AzureusCoreOperation operation) {
					result[0] = fileInfo.setLink(target);

					manager.setUserData("is_changing_links", false);
					if (!result[0]) {
						new MessageBoxShell(SWT.ICON_ERROR | SWT.OK,
								MessageText.getString("FilesView.rename.failed.title"),
								MessageText.getString("FilesView.rename.failed.text")).open(null);
					}
				}
			});
		} catch (Exception e) {
			manager.setUserData("is_changing_links", false);
		}
	}

	// Returns true if it was paused here.
	private static boolean setSkipped(DownloadManager manager,
			DiskManagerFileInfo[] infos, boolean skipped, boolean delete_action) {
		// if we're not managing the download then don't do anything other than
		// change the file's priority

		if (!manager.isPersistent()) {
			for (int i = 0; i < infos.length; i++) {
				infos[i].setSkipped(skipped);
			}
			return false;
		}
		int[] existing_storage_types = manager.getStorageType(infos);
		int nbFiles = manager.getDiskManagerFileInfoSet().nbFiles();
		boolean[] setLinear = new boolean[nbFiles];
		boolean[] setCompact = new boolean[nbFiles];
		boolean[] setReorder = new boolean[nbFiles];
		boolean[] setReorderCompact = new boolean[nbFiles];
		int compactCount = 0;
		int linearCount = 0;
		int reorderCount = 0;
		int reorderCompactCount = 0;

		if (infos.length > 1) {

		}
		// This should hopefully reduce the number of "exists" checks.
		File save_location = manager.getAbsoluteSaveLocation();
		boolean root_exists = save_location.isDirectory()
				|| (infos.length <= 1 && save_location.exists());

		boolean type_has_been_changed = false;
		boolean requires_pausing = false;

		for (int i = 0; i < infos.length; i++) {
			int existing_storage_type = existing_storage_types[i];
			int compact_target;
			int non_compact_target;
			if ( existing_storage_type == DiskManagerFileInfo.ST_COMPACT || existing_storage_type == DiskManagerFileInfo.ST_LINEAR ){
				compact_target 		= DiskManagerFileInfo.ST_COMPACT;
				non_compact_target	= DiskManagerFileInfo.ST_LINEAR;
			}else{
				compact_target 		= DiskManagerFileInfo.ST_REORDER_COMPACT;
				non_compact_target	= DiskManagerFileInfo.ST_REORDER;
			}
			int new_storage_type;
			if (skipped) {

				// Check to see if the file exists, but try to avoid doing an
				// actual disk check if possible.
				File existing_file = infos[i].getFile(true);

				// Avoid performing existing_file.exists if we know that it is meant
				// to reside in the default save location and that location does not
				// exist.
				boolean perform_check;
				if (root_exists) {
					perform_check = true;
				} else if (FileUtil.isAncestorOf(save_location, existing_file)) {
					perform_check = false;
				} else {
					perform_check = true;
				}

				if (perform_check && existing_file.exists()) {
					if (delete_action) {
						MessageBoxShell mb = new MessageBoxShell(SWT.OK | SWT.CANCEL,
								MessageText.getString("FilesView.rename.confirm.delete.title"),
								MessageText.getString("FilesView.rename.confirm.delete.text",
										new String[] {
											existing_file.toString()
										}));
						mb.setDefaultButtonUsingStyle(SWT.OK);
						mb.setRememberOnlyIfButton(0);
						mb.setRemember("FilesView.messagebox.delete.id", false, null);
						mb.setLeftImage(SWT.ICON_WARNING);
						mb.open(null);

						boolean wants_to_delete = mb.waitUntilClosed() == SWT.OK;

						if ( wants_to_delete ){
							
							new_storage_type = compact_target;
							
						}else{
							
							new_storage_type = non_compact_target;
						}
					}else{
						
						new_storage_type = non_compact_target;
					}
				}
				// File does not exist.
				else {
					new_storage_type = compact_target;
				}
			}else{
				new_storage_type = non_compact_target;
			}

			boolean has_changed = existing_storage_type != new_storage_type;
			type_has_been_changed |= has_changed;
			requires_pausing |= (has_changed && ( new_storage_type == DiskManagerFileInfo.ST_COMPACT || new_storage_type == DiskManagerFileInfo.ST_REORDER_COMPACT ));

			type_has_been_changed = existing_storage_type != new_storage_type;

			if (new_storage_type == DiskManagerFileInfo.ST_COMPACT) {
				setCompact[infos[i].getIndex()] = true;
				compactCount++;
			} else if (new_storage_type == DiskManagerFileInfo.ST_LINEAR) {
				setLinear[infos[i].getIndex()] = true;
				linearCount++;
			} else if (new_storage_type == DiskManagerFileInfo.ST_REORDER) {
				setReorder[infos[i].getIndex()] = true;
				reorderCount++;
			} else if (new_storage_type == DiskManagerFileInfo.ST_REORDER_COMPACT) {
				setReorderCompact[infos[i].getIndex()] = true;
				reorderCompactCount++;
			}
		}

		boolean ok = true;
		boolean paused = false;
		if (type_has_been_changed) {
			if (requires_pausing)
				paused = manager.pause();
			if (linearCount > 0)
				ok &= Arrays.equals(
						setLinear,
						manager.getDiskManagerFileInfoSet().setStorageTypes(setLinear,
								DiskManagerFileInfo.ST_LINEAR));
			if (compactCount > 0)
				ok &= Arrays.equals(
						setCompact,
						manager.getDiskManagerFileInfoSet().setStorageTypes(setCompact,
								DiskManagerFileInfo.ST_COMPACT));
			if (reorderCount > 0)
				ok &= Arrays.equals(
						setReorder,
						manager.getDiskManagerFileInfoSet().setStorageTypes(setReorder,
								DiskManagerFileInfo.ST_REORDER));
			if (reorderCompactCount > 0)
				ok &= Arrays.equals(
						setReorderCompact,
						manager.getDiskManagerFileInfoSet().setStorageTypes(setReorderCompact,
								DiskManagerFileInfo.ST_REORDER_COMPACT));
		}

		if (ok) {
			for (int i = 0; i < infos.length; i++) {
				infos[i].setSkipped(skipped);
			}
		}

		return paused;
	}

}
