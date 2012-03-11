/*
 * Created on Jan 17, 2010 2:19:53 AM
 * Copyright (C) 2010 Aelitis, All Rights Reserved.
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
 */
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.FilesView;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.*;

import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.uiupdater.UIUpdaterSWT;

/**
 * @author TuxPaper
 * @created Jan 17, 2010
 *
 */
public class ColumnFileCount
	extends CoreTableColumn
	implements TableCellMouseListener, TableCellSWTPaintListener,
	TableCellAddedListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "filecount";

	public ColumnFileCount(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_TRAIL, 60, sTableID);
		setRefreshInterval(INTERVAL_INVALID_ONLY);
	}
	
	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_CONTENT,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener#cellAdded(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellAdded(TableCell cell) {
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		int sortVal = dm.getNumFileInfos();
		cell.setSortValue(sortVal);
	}

	public void cellMouseTrigger(final TableCellMouseEvent event) {

		if (Utils.getUserMode() < 2) { // remove prototype for now
			return;
		}
		final DownloadManager dm = (DownloadManager) event.cell.getDataSource();
		
		if (event.eventType == TableRowMouseEvent.EVENT_MOUSEENTER) {
			((TableCellCore) event.cell).setCursorID(SWT.CURSOR_HAND);
		} else if (event.eventType == TableRowMouseEvent.EVENT_MOUSEENTER) {
			((TableCellCore) event.cell).setCursorID(SWT.CURSOR_ARROW);
		} else if (event.eventType == TableRowMouseEvent.EVENT_MOUSEUP
				&& event.button == 1) {
			Utils.execSWTThreadLater(0, new AERunnable() {

				public void runSupport() {
					openFilesMiniView(dm, event.cell);
				}
			});
		}
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.ui.swt.views.table.TableCellSWT)
	public void cellPaint(GC gc, TableCellSWT cell) {
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		if (dm == null) {
			return;
		}

		int sortVal = dm.getNumFileInfos();
		Rectangle bounds = cell.getBounds();
		Rectangle printArea = new Rectangle(bounds.x, bounds.y, bounds.width - 6,
				bounds.height);
		GCStringPrinter.printString(gc, "" + sortVal, printArea, true, true,
				SWT.RIGHT);
	}

	private void openFilesMiniView(DownloadManager dm, TableCell cell) {
		Shell shell = ShellFactory.createShell(Utils.findAnyShell(), SWT.SHELL_TRIM);

		shell.setLayout(new FillLayout());

		Rectangle bounds = ((TableCellSWT) cell).getBoundsOnDisplay();
		bounds.y += bounds.height;
		bounds.width = 630;
		bounds.height = (16 * dm.getNumFileInfos()) + 60;
		Rectangle realBounds = shell.computeTrim(0, 0, bounds.width, bounds.height);
		realBounds.width -= realBounds.x;
		realBounds.height -= realBounds.y;
		realBounds.x = bounds.x;
		realBounds.y = bounds.y;
		if (bounds.height > 500) {
			bounds.height = 500;
		}
		shell.setBounds(realBounds);
		shell.setAlpha(230);

		Utils.verifyShellRect(shell, true);

		
		final FilesView view = new FilesView(false);
		view.dataSourceChanged(dm);

		view.initialize(shell);

		Composite composite = view.getComposite();
		//composite.setLayoutData(null);
		shell.setLayout(new FillLayout());

		view.viewActivated();
		view.refresh();

		final UIUpdatable viewUpdater = new UIUpdatable() {
			public void updateUI() {
				view.refresh();
			}

			public String getUpdateUIName() {
				return view.getFullTitle();
			}
		};
		UIUpdaterSWT.getInstance().addUpdater(viewUpdater);

		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				UIUpdaterSWT.getInstance().removeUpdater(viewUpdater);
			}
		});

		shell.layout(true, true);


		shell.setText(dm.getDisplayName());

		shell.open();
	}
}
