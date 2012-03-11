/*
 * File    : NameItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
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

package org.gudy.azureus2.ui.swt.views.tableitems.files;

import java.io.File;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.MessageBox;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.ObfusticateCellText;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.*;

import com.aelitis.azureus.core.AzureusCoreOperation;
import com.aelitis.azureus.core.AzureusCoreOperationTask;

/** Torrent name cell for My Torrents.
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class NameItem extends CoreTableColumn implements
		TableCellLightRefreshListener, ObfusticateCellText, TableCellDisposeListener
{
	private static boolean bShowIcon;

	static {
		COConfigurationManager.addAndFireParameterListener(
				"NameColumn.showProgramIcon", new ParameterListener() {
					public void parameterChanged(String parameterName) {
						bShowIcon = COConfigurationManager.getBooleanParameter("NameColumn.showProgramIcon");
					}
				});
	}
	
	final TableContextMenuItem menuItem;

	/** Default Constructor */
	public NameItem() {
		super("name", ALIGN_LEAD, POSITION_LAST, 300,
				TableManager.TABLE_TORRENT_FILES);
		setInplaceEdit(true);
		setType(TableColumn.TYPE_TEXT);
		menuItem = addContextMenuItem("FilesView.name.fastRename");
		
		menuItem.setStyle(MenuItem.STYLE_CHECK);
		//menuItem.setText(MessageText.getString("FilesView.name.fastRename")); TODO make this work
		menuItem.setData(Boolean.valueOf(isInplaceEdit()));

		menuItem.addMultiListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				menu.setData(Boolean.valueOf(!isInplaceEdit())); // XXX broken, should toggle checkmarks
				setInplaceEdit(!isInplaceEdit());
			}
		});
	}

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_CONTENT,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}
	
	public void postConfigLoad() {
		setInplaceEdit(getUserData("noInplaceEdit") == null);
		menuItem.setData(Boolean.valueOf(isInplaceEdit()));
	}
	
	public void preConfigSave() {
		if(isInplaceEdit())
			removeUserData("noInplaceEdit");
		else
			setUserData("noInplaceEdit", new Integer(1));
	}
	
	public void refresh(TableCell cell, boolean sortOnlyRefresh)
	{
		final DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) cell.getDataSource();
		String name = (fileInfo == null) ? "" : fileInfo.getFile(true).getName();
		if (name == null)
			name = "";
		//setText returns true only if the text is updated
		if (cell.setText(name) || !cell.isValid()) {
			if (bShowIcon && !sortOnlyRefresh) {
				Image icon = null;
				
				final TableCellSWT _cell = (TableCellSWT)cell;
				
				if (fileInfo == null) {
					icon = null;
				} else {
					
					// Don't ever dispose of PathIcon, it's cached and may be used elsewhere
					
					if ( Utils.isSWTThread()){
					
						icon = ImageRepository.getPathIcon(fileInfo.getFile(true).getPath(),
								false, false);
					}else{	
							// happens rarely (seen of filtering of file-view rows
							// when a new row is added )
												
						Utils.execSWTThread(
							new Runnable()
							{
								public void
								run()
								{
									Image icon = ImageRepository.getPathIcon(fileInfo.getFile(true).getPath(),
											false, false);
									
									_cell.setIcon(icon);
									
									_cell.redraw();
								}
							});
					}
				}

				// cheat for core, since we really know it's a TabeCellImpl and want to use
				// those special functions not available to Plugins
				
				if ( icon != null ){
					_cell.setIcon(icon);
				}
			}
		}
	}

	public void refresh(TableCell cell)
	{
		refresh(cell, false);
	}

	public String getObfusticatedText(TableCell cell) {
		DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) cell.getDataSource();
		String name = (fileInfo == null) ? ""
				: Debug.secretFileName(fileInfo.getFile(true).getName());
		if (name == null)
			name = "";
		return name;
	}

	public void dispose(TableCell cell) {
	}

	private void disposeCellIcon(TableCell cell) {
		final Image img = ((TableCellSWT) cell).getIcon();
		if (img != null) {
			((TableCellSWT) cell).setIcon(null);
			if (!img.isDisposed()) {
				img.dispose();
			}
		}
	}
	
	public boolean inplaceValueSet(TableCell cell, String value, boolean finalEdit) {
		if (value.equalsIgnoreCase(cell.getText()) || "".equals(value) || "".equals(cell.getText()))
			return true;
		final DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) cell.getDataSource();
		final File target;
		
		try
		{
			target = new File(fileInfo.getFile(true).getParentFile(), value).getCanonicalFile();
		} catch (IOException e)
		{
			return false;
		}
			
		if(!finalEdit)
			return !target.exists();

		
		if(target.exists())
			return false;
		

		// code stolen from FilesView
		final boolean[] result = { false };
		boolean paused = fileInfo.getDownloadManager().pause();
		FileUtil.runAsTask(new AzureusCoreOperationTask()
		{
			public void run(AzureusCoreOperation operation) {
				result[0] = fileInfo.setLink(target);
			}
		});
		if(paused)
			fileInfo.getDownloadManager().resume();
		
		if (!result[0])
		{
			new MessageBoxShell(SWT.ICON_ERROR | SWT.OK, 
					MessageText.getString("FilesView.rename.failed.title"),
					MessageText.getString("FilesView.rename.failed.text")).open(null);
		}
		
		return true;
	}
}
