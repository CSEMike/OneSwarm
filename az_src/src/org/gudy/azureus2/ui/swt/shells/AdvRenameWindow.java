/*
 * Created on Sep 3, 2009 3:12:13 PM
 * Copyright (C) 2009 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.ui.swt.shells;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

import com.aelitis.azureus.ui.common.RememberedDecisionsManager;

/**
 * @author TuxPaper
 * @created Sep 3, 2009
 *
 */
public class AdvRenameWindow
{
	private DownloadManager dm;

	private Shell shell;

	private String newName = null;

	protected int renameDecisions;

	private static final int RENAME_DISPLAY = 0x1;

	private static final int RENAME_SAVEPATH = 0x2;

	private static final int RENAME_TORRENT = 0x4;

	public static void main(String[] args) {
		AdvRenameWindow window = new AdvRenameWindow();
		window.open(null);
		window.waitUntilDone();
	}

	public AdvRenameWindow() {
	}

	public void open(DownloadManager dm) {
		this.dm = dm;
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				openInSWT();
			}
		});
	}

	private void openInSWT() {
		shell = ShellFactory.createMainShell(SWT.DIALOG_TRIM | SWT.RESIZE);
		Utils.setShellIcon(shell);
		shell.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					shell.dispose();
				}
			}
		});

		Messages.setLanguageText(shell, "AdvRenameWindow.title");

		Label lblMessage = new Label(shell, SWT.WRAP);
		Messages.setLanguageText(lblMessage, "AdvRenameWindow.message");

		final Text txtInput = new Text(shell, SWT.BORDER);
		txtInput.setText(dm == null ? "" : dm.getDisplayName());

		final Button btnDisplayName = new Button(shell, SWT.CHECK);
		Messages.setLanguageText(btnDisplayName,
				"MyTorrentsView.menu.rename.displayed");

		final Button btnSavePath = new Button(shell, SWT.CHECK);
		Messages.setLanguageText(btnSavePath,
				"MyTorrentsView.menu.rename.save_path");

		final Button btnTorrent = new Button(shell, SWT.CHECK);
		Messages.setLanguageText(btnTorrent, "AdvRenameWindow.rename.torrent");

		Composite cButtons = new Composite(shell, SWT.NONE);
		RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
		rowLayout.fill = true;
		rowLayout.spacing = 5;
		cButtons.setLayout(rowLayout);
		
		Button btnReset = new Button(cButtons, SWT.PUSH);
		Messages.setLanguageText(btnReset, "Button.reset");
		btnReset.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				txtInput.setText(TorrentUtils.getLocalisedName(dm.getTorrent()));
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		Button btnOk = new Button(cButtons, SWT.PUSH);
		Messages.setLanguageText(btnOk, "Button.ok");
		shell.setDefaultButton(btnOk);
		btnOk.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				newName = txtInput.getText();

				renameDecisions = 0;
				if (btnDisplayName.getSelection()) {
					renameDecisions |= RENAME_DISPLAY;
				}
				if (btnSavePath.getSelection()) {
					renameDecisions |= RENAME_SAVEPATH;
				}
				if (btnTorrent.getSelection()) {
					renameDecisions |= RENAME_TORRENT;
				}
				RememberedDecisionsManager.setRemembered("adv.rename", renameDecisions);

				Utils.getOffOfSWTThread(new AERunnable() {
					public void runSupport() {
						doRename();
					}
				});

				shell.dispose();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		Button btnCancel = new Button(cButtons, SWT.PUSH);
		Messages.setLanguageText(btnCancel, "Button.cancel");
		btnCancel.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				shell.dispose();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		shell.setLayout(new FormLayout());

		FormData fd;
		fd = new FormData();
		fd.top = new FormAttachment(0, 3);
		fd.left = new FormAttachment(0, 3);
		fd.right = new FormAttachment(100, -3);
		lblMessage.setLayoutData(fd);

		fd = new FormData();
		fd.top = new FormAttachment(lblMessage, 5);
		fd.left = new FormAttachment(0, 3);
		fd.right = new FormAttachment(100, -3);
		fd.width = 300;
		txtInput.setLayoutData(fd);

		fd = new FormData();
		fd.top = new FormAttachment(txtInput, 5);
		fd.left = new FormAttachment(0, 8);
		fd.right = new FormAttachment(100, -3);
		btnDisplayName.setLayoutData(fd);

		fd = new FormData();
		fd.top = new FormAttachment(btnDisplayName, 2);
		fd.left = new FormAttachment(0, 8);
		fd.right = new FormAttachment(100, -3);
		btnSavePath.setLayoutData(fd);

		fd = new FormData();
		fd.top = new FormAttachment(btnSavePath, 2);
		fd.left = new FormAttachment(0, 8);
		fd.right = new FormAttachment(100, -3);
		btnTorrent.setLayoutData(fd);

		int renameDecisions = RememberedDecisionsManager.getRememberedDecision("adv.rename");
		if ((renameDecisions & RENAME_DISPLAY) > 0) {
			btnDisplayName.setSelection(true);
		}
		if ((renameDecisions & RENAME_SAVEPATH) > 0) {
			btnSavePath.setSelection(true);
		}
		if ((renameDecisions & RENAME_TORRENT) > 0) {
			btnTorrent.setSelection(true);
		}

		fd = new FormData();
		fd.top = new FormAttachment(btnTorrent, 5);
		fd.right = new FormAttachment(100, -3);
		fd.bottom = new FormAttachment(100, -3);
		cButtons.setLayoutData(fd);

		shell.pack();
		Utils.centreWindow(shell);
		shell.open();
	}
	
	private void waitUntilDone() {
		while (shell != null && !shell.isDisposed()) {
			if (!shell.getDisplay().readAndDispatch()) {
				shell.getDisplay().sleep();
			}
		}
	}

	private void doRename() {
		if (dm == null) {
			return;
		}
		if ((renameDecisions & RENAME_DISPLAY) > 0) {
			dm.getDownloadState().setDisplayName(newName);
		}
		if ((renameDecisions & RENAME_SAVEPATH) > 0) {
			try {
				dm.renameDownload(newName);
			} catch (Exception e) {
				Logger.log(new LogAlert(dm, LogAlert.REPEATABLE,
						"Download data rename operation failed", e));
			}
		}
		if ((renameDecisions & RENAME_TORRENT) > 0) {
			try {
				dm.renameTorrentSafe(newName);
  		} catch (Exception e) {
  			Logger.log(new LogAlert(dm, LogAlert.REPEATABLE,
  					"Torrent rename operation failed", e));
  		}
		}
	}
}
