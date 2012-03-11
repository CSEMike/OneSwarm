/*
 * OpenTorrentWindow.java
 *
 * Created on February 23, 2004, 4:09 PM
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

package org.gudy.azureus2.ui.swt;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.StringIterator;
import org.gudy.azureus2.core3.config.StringList;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerInitialisationAdapter;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.LocaleTorrentUtil;
import org.gudy.azureus2.core3.internat.LocaleUtilDecoder;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.ipfilter.IpFilterManagerFactory;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.shells.MessageSlideShell;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileComponent;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.uiupdater.UIUpdaterSWT;

/**
 * Torrent Opener Window.
 * 
 * @author TuxPaper
 * 
 * TODO Category Option
 */
public class OpenTorrentWindow
	implements TorrentDownloaderCallBackInterface, UIUpdatable
{

	/**
	 * We prevent users from unselecting small files to prevent them missing
	 * out "signature" files from trackers (.nfo file, readme file etc)
	 * 
	 * We define two constants to control this - one defines what a small file
	 * is, and the other defines whether we believe a torrent has signature
	 * files or not - we do this by seeing how many small files the torrent has.
	 * 
	 * If it has several small files, then it would be silly for us to assume
	 * that the torrent consists of multiple signature files.
	 * 
	 * Note: I (amc1) have disabled this now, because it can force users who may want
	 * to only download one file to download those small files, which may not be in
	 * an overlapping piece. Since I've now seen comments from people who've complained
	 * about this, I'm disabling it.
	 */
	//private final static int MIN_NODOWNLOAD_SIZE = 64 * 1024;
	//private final static int MAX_NODOWNLOAD_COUNT = 3;
	private final static int MIN_BUTTON_HEIGHT = -1;

	private final static String PARAM_DEFSAVEPATH = "Default save path";

	private final static String PARAM_MOVEWHENDONE = "Move Completed When Done";

	private static final String PARAM_VIEWMODE = "OpenTorrentWindow.viewMode";

	private final static String MSG_ALREADY_EXISTS = "OpenTorrentWindow.mb.alreadyExists";

	private final static String MSG_ALREADY_EXISTS_NAME = MSG_ALREADY_EXISTS
			+ ".default.name";

	private final static int STARTMODE_QUEUED = 0;

	private final static int STARTMODE_STOPPED = 1;

	private final static int STARTMODE_FORCESTARTED = 2;

	private final static int STARTMODE_SEEDING = 3;

	private final static int QUEUELOCATION_TOP = 0;

	private final static int QUEUELOCATION_BOTTOM = 1;

	private final static String[] startModes = {
		"queued",
		"stopped",
		"forceStarted",
		"seeding"
	};

	private final static String[] queueLocations = {
		"first",
		"last"
	};

	/** Only one window, since it can handle multiple torrents */
	private static OpenTorrentWindow stOpenTorrentWindow = null;

	// SWT Stuff
	private Shell shell;

	private Table dataFileTable;
	private TableEditor dataFileTableEditor;

	private Table torrentTable;

	private Button ok;

	private Combo cmbDataDir;

	private Composite cSaveTo;

	private Combo cmbStartMode = null;

	private Combo cmbQueueLocation = null;

	// Link to the outside
	private GlobalManager gm;

	// Internal Stuff

	/** TorrentFileInfo list.  All dataFiles currently in table, same order */
	private ArrayList dataFiles = new ArrayList();

	/** TorrentInfo list.  All torrents to open, same order as table */
	private ArrayList torrentList = new ArrayList();

	/** List of torrents being downloaded.  Stored so we don't close window
	 * until they are done/aborted.
	 */
	private ArrayList<TorrentDownloader> downloaders = new ArrayList<TorrentDownloader>();

	private boolean bOverrideStartModeToStopped = false;

	private boolean bDefaultForSeeding;

	/** Things to be disposed of when window closes */
	private ArrayList disposeList = new ArrayList();

	private boolean bClosed = false;

	/** Shell to use to open children (FileDialog, etc) */
	private Shell shellForChildren;

	private String sDestDir;

	protected boolean bSkipDataDirModify = false;

	private StringList dirList;

	private Label dataFileTableLabel;

	private Composite diskspaceComp;

	/**
	 * A counter to track torrent file downloads that are still active;
	 * this is purely used to enable/disable the OK button
	 */
	private int activeTorrentCount = 0;

	/**
	 * 
	 * @param parent
	 * @param gm
	 * @param sPathOfFilesToOpen
	 * @param sFilesToOpen
	 * @param bDefaultStopped
	 * @param bForSeeding 
	 * @param bPopupOpenURL 
	 */
	public synchronized static final void invoke(Shell parent, GlobalManager gm,
			String sPathOfFilesToOpen, String[] sFilesToOpen,
			boolean bDefaultStopped, boolean bForSeeding, boolean bPopupOpenURL) {

		String saveSilentlyDir = null;

		if (stOpenTorrentWindow == null) {
			boolean bMustOpen = (sPathOfFilesToOpen == null && sFilesToOpen == null)
					|| bForSeeding;
			if (!bMustOpen) {
				saveSilentlyDir = getSaveSilentlyDir();
				bMustOpen = saveSilentlyDir == null;
			}

			stOpenTorrentWindow = new OpenTorrentWindow(parent, gm, bMustOpen);
		} else {
			if (stOpenTorrentWindow.shell != null)
				stOpenTorrentWindow.shell.forceActive();
		}

		if (stOpenTorrentWindow != null) {
			// local var because static may get set o null
			OpenTorrentWindow openTorrentWindow = stOpenTorrentWindow;
			openTorrentWindow.bOverrideStartModeToStopped = bDefaultStopped;
			openTorrentWindow.bDefaultForSeeding = bForSeeding;
			if (sFilesToOpen != null || sPathOfFilesToOpen != null) {
				// If none of the files sent to us were valid files, don't open the 
				// window
				if (!bPopupOpenURL
						&& openTorrentWindow.addTorrents(sPathOfFilesToOpen, sFilesToOpen) == 0
						&& openTorrentWindow.torrentList.size() == 0
						&& openTorrentWindow.downloaders.size() == 0) {
					openTorrentWindow.close(true, true);
					return;
				}
			}

			if (bPopupOpenURL)
				openTorrentWindow.browseURL();

			if (saveSilentlyDir != null) {
				openTorrentWindow.sDestDir = saveSilentlyDir;
				for (int i = 0; i < openTorrentWindow.torrentList.size(); i++) {
					final TorrentInfo info = (TorrentInfo) openTorrentWindow.torrentList.get(i);
					info.renameDuplicates();
				}

				openTorrentWindow.openTorrents();
				openTorrentWindow.close(true, false);
			}
		}
	}

	/**
	 * 
	 * @param parent
	 * @param gm
	 */
	public synchronized static final void invoke(final Shell parent,
			GlobalManager gm) {
		invoke(parent, gm, null, null, false, false, false);
	}

	public synchronized static final void invokeURLPopup(final Shell parent,
			GlobalManager gm) {
		invoke(parent, gm, null, null, false, false, true);
	}

	/**
	 * 
	 * @param parent
	 * @param gm
	 * @param bOpenWindow 
	 */
	private OpenTorrentWindow(final Shell parent, GlobalManager gm,
			boolean bOpenWindow) {
		this.gm = gm;

		sDestDir = COConfigurationManager.getStringParameter(PARAM_DEFSAVEPATH);

		if (bOpenWindow)
			openWindow(parent);
		else
			shellForChildren = parent;
	}

	private void openWindow(Shell parent) {
		boolean bTorrentInClipboard = false;
		GridData gridData;
		Label label;
		Composite cArea;

		shell = ShellFactory.createShell(parent, SWT.RESIZE | SWT.DIALOG_TRIM);

		shellForChildren = shell;

		shell.setText(MessageText.getString("OpenTorrentWindow.title"));
		Utils.setShellIcon(shell);

		GridLayout layout = FixupLayout(new GridLayout(), false);
		shell.setLayout(layout);
		shell.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				resizeTables(3);
			}
		});

		Clipboard clipboard = new Clipboard(shell.getDisplay());

		String sClipText = (String) clipboard.getContents(TextTransfer.getInstance());
		if (sClipText != null)
			bTorrentInClipboard = addTorrentsFromTextList(sClipText, true) > 0;

		//    label = new Label(shell, SWT.BORDER | SWT.WRAP);
		//    Messages.setLanguageText(label, "OpenTorrentWindow.message");
		//    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		//    label.setLayoutData(gridData);

		// Torrents
		// ========

		Composite cButtons = new Composite(shell, SWT.NONE);
		RowLayout rLayout = new RowLayout(SWT.HORIZONTAL);
		rLayout.marginBottom = 0;
		rLayout.marginLeft = 0;
		rLayout.marginRight = 0;
		rLayout.marginTop = 0;
		cButtons.setLayout(rLayout);

		// Buttons for tableTorrents

		Button browseTorrent = new Button(cButtons, SWT.PUSH);
		Messages.setLanguageText(browseTorrent, "OpenTorrentWindow.addFiles");
		browseTorrent.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				FileDialog fDialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
				fDialog.setFilterExtensions(new String[] {
					"*.torrent",
					"*.tor",
					Constants.FILE_WILDCARD
				});
				fDialog.setFilterNames(new String[] {
					"*.torrent",
					"*.tor",
					Constants.FILE_WILDCARD
				});
				fDialog.setFilterPath(TorrentOpener.getFilterPathTorrent());
				fDialog.setText(MessageText.getString("MainWindow.dialog.choose.file"));
				String fileName = TorrentOpener.setFilterPathTorrent(fDialog.open());
				if (fileName != null) {
					addTorrents(fDialog.getFilterPath(), fDialog.getFileNames());
				}
			}
		});

		Utils.setGridData(cButtons, GridData.FILL_HORIZONTAL, browseTorrent,
				MIN_BUTTON_HEIGHT);

		Button browseURL = new Button(cButtons, SWT.PUSH);
		Messages.setLanguageText(browseURL, "OpenTorrentWindow.addFiles.URL");
		browseURL.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				browseURL();
			}
		});

		Button browseFolder = new Button(cButtons, SWT.PUSH);
		Messages.setLanguageText(browseFolder, "OpenTorrentWindow.addFiles.Folder");
		browseFolder.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				DirectoryDialog fDialog = new DirectoryDialog(shell, SWT.NULL);
				fDialog.setFilterPath(TorrentOpener.getFilterPathTorrent());
				fDialog.setMessage(MessageText.getString("MainWindow.dialog.choose.folder"));
				String path = TorrentOpener.setFilterPathTorrent(fDialog.open());
				if (path != null) {
					addTorrents(path, null);
				}
			}
		});

		if (bTorrentInClipboard) {
			Button pasteOpen = new Button(cButtons, SWT.PUSH);
			Messages.setLanguageText(pasteOpen,
					"OpenTorrentWindow.addFiles.Clipboard");
			pasteOpen.setToolTipText(sClipText);
			pasteOpen.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					Clipboard clipboard = new Clipboard(shell.getDisplay());

					String sClipText = (String) clipboard.getContents(TextTransfer.getInstance());
					if (sClipText != null) {
						addTorrentsFromTextList(sClipText.trim(), false);
					}
				}
			});
		}

		Group gTorrentsArea = new Group(shell, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gTorrentsArea.setLayoutData(gridData);
		layout = FixupLayout(new GridLayout(), true);
		gTorrentsArea.setLayout(layout);
		Messages.setLanguageText(gTorrentsArea, "OpenTorrentWindow.torrentLocation");

		Composite cTorrentList = new Composite(gTorrentsArea, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		cTorrentList.setLayoutData(gridData);

		createTorrentListArea(cTorrentList);

		Composite cTorrentOptions = new Composite(gTorrentsArea, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		cTorrentOptions.setLayoutData(gridData);
		layout = FixupLayout(new GridLayout(), true);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		cTorrentOptions.setLayout(layout);

		label = new Label(cTorrentOptions, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(gridData);
		Messages.setLanguageText(label, "OpenTorrentWindow.torrent.options");

		int userMode = COConfigurationManager.getIntParameter("User Mode");
		if (userMode > 0) {
			Composite cTorrentModes = new Composite(cTorrentOptions, SWT.NONE);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			cTorrentModes.setLayoutData(gridData);
			layout = new GridLayout();
			layout.numColumns = 4;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			cTorrentModes.setLayout(layout);

			label = new Label(cTorrentModes, SWT.NONE);
			gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
			label.setLayoutData(gridData);
			Messages.setLanguageText(label, "OpenTorrentWindow.startMode");

			cmbStartMode = new Combo(cTorrentModes, SWT.BORDER | SWT.READ_ONLY);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			cmbStartMode.setLayoutData(gridData);
			updateStartModeCombo();
			cmbStartMode.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					setSelectedStartMode(cmbStartMode.getSelectionIndex());
				}
			});

			label = new Label(cTorrentModes, SWT.NONE);
			gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
			label.setLayoutData(gridData);
			Messages.setLanguageText(label, "OpenTorrentWindow.addPosition");

			cmbQueueLocation = new Combo(cTorrentModes, SWT.BORDER | SWT.READ_ONLY);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			cmbQueueLocation.setLayoutData(gridData);
			updateQueueLocationCombo();
			cmbQueueLocation.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					setSelectedQueueLocation(cmbQueueLocation.getSelectionIndex());
				}
			});
		}

		// Save To..
		// =========

		cSaveTo = new Composite(cTorrentOptions, SWT.NONE);
		layout = FixupLayout(new GridLayout(), false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		layout.numColumns = 2;
		cSaveTo.setLayout(layout);

		Label lblDataDir = new Label(cSaveTo, SWT.NONE);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gridData.horizontalSpan = 2;
		lblDataDir.setLayoutData(gridData);
		Messages.setLanguageText(lblDataDir, "OpenTorrentWindow.dataLocation");

		cmbDataDir = new Combo(cSaveTo, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		cmbDataDir.setLayoutData(gridData);

		cmbDataDir.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				cmbDataDirChanged();
			}
		});
		cmbDataDir.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				cmbDataDirChanged();
			}
		});

		updateDataDirCombo();
		dirList = COConfigurationManager.getStringListParameter("saveTo_list");
		StringIterator iter = dirList.iterator();
		while (iter.hasNext()) {
			String s = iter.next();
			if (!s.equals(sDestDir)) {
				cmbDataDir.add(s);
			}
		}

		Button browseData = new Button(cSaveTo, SWT.PUSH);
		Messages.setLanguageText(browseData, "ConfigView.button.browse");

		browseData.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				String sSavePath;
				String sDefPath = cmbDataDir.getText();

				File f = new File(sDefPath);
				if (sDefPath.length() > 0) {
					while (!f.exists()) {
						f = f.getParentFile();
						if (f == null) {
							f = new File(sDefPath);
							break;
						}
					}
				}

				DirectoryDialog dDialog = new DirectoryDialog(shell, SWT.SYSTEM_MODAL);
				dDialog.setFilterPath(f.getAbsolutePath());
				dDialog.setMessage(MessageText.getString("MainWindow.dialog.choose.savepath_forallfiles"));
				sSavePath = dDialog.open();

				if (sSavePath != null) {
					cmbDataDir.setText(sSavePath);
				}
			}
		});

		gridData = new GridData(GridData.FILL_HORIZONTAL);
		cSaveTo.setLayoutData(gridData);

		// File List
		// =========

		Group gFilesArea = new Group(shell, SWT.NONE);
		gridData = new GridData(GridData.FILL_BOTH);
		gFilesArea.setLayoutData(gridData);
		layout = FixupLayout(new GridLayout(), true);
		gFilesArea.setLayout(layout);
		Messages.setLanguageText(gFilesArea, "OpenTorrentWindow.fileList");

		createTableDataFiles(gFilesArea);

		// Ok, cancel

		cArea = new Composite(shell, SWT.NULL);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.numColumns = 2;
		cArea.setLayout(layout);

		ok = new Button(cArea, SWT.PUSH);
		Messages.setLanguageText(ok, "Button.ok");
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		gridData.widthHint = 70;
		ok.setLayoutData(gridData);
		shell.setDefaultButton(ok);
		ok.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				okPressed();
			}
		});

		checkSeedingMode();

		Button cancel = new Button(cArea, SWT.PUSH);
		Messages.setLanguageText(cancel, "Button.cancel");
		gridData = new GridData();
		gridData.widthHint = 70;
		cancel.setLayoutData(gridData);
		cancel.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				close(true, true);
			}
		});

		Utils.setGridData(cArea, GridData.HORIZONTAL_ALIGN_END, ok,
				MIN_BUTTON_HEIGHT);

		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (!bClosed)
					close(false, true);
			}
		});

		shell.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					close(true, true);
				}
			}
		});

		KeyListener pasteKeyListener = new org.eclipse.swt.events.KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				int key = e.character;
				if ((e.stateMask & SWT.MOD1) != 0 && e.character <= 26
						&& e.character > 0)
					key += 'a' - 1;

				if ((key == 'v' && (e.stateMask & SWT.MOD1) > 0)
						|| (e.keyCode == SWT.INSERT && (e.stateMask & SWT.SHIFT) > 0)) {
					e.doit = false;

					// Paste
					Clipboard clipboard = new Clipboard(shell.getDisplay());

					String sClipText = (String) clipboard.getContents(TextTransfer.getInstance());
					if (sClipText != null) {
						addTorrentsFromTextList(sClipText, false);
					}
				}
			}
		};

		setPasteKeyListener(shell, pasteKeyListener);

		Utils.createTorrentDropTarget(shell, false);
		shell.pack();

		if (!Utils.linkShellMetricsToConfig(shell, "OpenTorrentWindow")) {
			Utils.centreWindow(shell);
		}
		resizeTables(3);
		shell.open();

		if (cSaveTo != null && !cSaveTo.isDisposed()) {
			cSaveTo.setFocus();
		}

		try {
			UIUpdaterSWT.getInstance().addUpdater(this);
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	protected void cmbDataDirChanged() {
		if (bSkipDataDirModify) {
			return;
		}
		sDestDir = cmbDataDir.getText();

		int[] indexes = torrentTable.getSelectionIndices();
		for (int i = 0; i < indexes.length; i++) {
			TorrentInfo info = (TorrentInfo) torrentList.get(indexes[i]);
			//if (!info.allFilesMoving())
			info.sDestDir = sDestDir;
		}

		torrentTable.clearAll();

		checkSeedingMode();

		if (!Utils.isCocoa || SWT.getVersion() > 3600) { // See Eclipse Bug 292449
  		File file = new File(sDestDir);
  		if (!file.isDirectory()) {
  			cmbDataDir.setBackground(Colors.colorErrorBG);
  		} else {
  			cmbDataDir.setBackground(null);
  		}
  		cmbDataDir.redraw();
  		cmbDataDir.update();
		}
		diskFreeInfoRefreshPending = true;
	}

	protected void okPressed() {
		if (bClosed) {
			return;
		}

		if ((torrentList.size() == 0 && downloaders.size() == 0)) {
			close(true, false);
			return;
		}

		File file = new File(cmbDataDir.getText());

		File fileDefSavePath = new File(
				COConfigurationManager.getStringParameter(PARAM_DEFSAVEPATH));

		if (file.equals(fileDefSavePath) && !fileDefSavePath.isDirectory()) {
			FileUtil.mkdirs(fileDefSavePath);
		}

		boolean isPathInvalid = cmbDataDir.getText().length() == 0 || file.isFile();
		if (!isPathInvalid && !file.isDirectory()) {
			MessageBoxShell mb = new MessageBoxShell(SWT.YES | SWT.NO
					| SWT.ICON_QUESTION, "OpenTorrentWindow.mb.askCreateDir",
					new String[] {
						file.toString()
					});
			mb.open(null);
			int doCreate = mb.waitUntilClosed();

			if (doCreate == SWT.YES)
				isPathInvalid = !FileUtil.mkdirs(file);
			else {
				cmbDataDir.setFocus();
				return;
			}
		}

		if (isPathInvalid) {
			MessageBoxShell mb = new MessageBoxShell(SWT.OK | SWT.ICON_ERROR,
					"OpenTorrentWindow.mb.noGlobalDestDir", new String[] {
						file.toString()
					});
			mb.open(null);
			cmbDataDir.setFocus();
			return;
		}

		String sExistingFiles = "";
		int iNumExistingFiles = 0;
		for (int i = 0; i < torrentList.size(); i++) {
			TorrentInfo info = (TorrentInfo) torrentList.get(i);

			file = new File(info.getDataDir());
			
			// Need to make directory now, or single file torrent will take the 
			// "dest dir" as their filename.  ie:
			// 1) Add single file torrent with named "hi.exe"
			// 2) type a non-existant directory c:\test\moo
			// 3) unselect the torrent
			// 4) change the global def directory to a real one
			// 5) click ok.  "hi.exe" will be written as moo in c:\test			
			if (!file.isDirectory() && !FileUtil.mkdirs(file)) {
				MessageBoxShell mb = new MessageBoxShell(SWT.OK | SWT.ICON_ERROR,
						"OpenTorrentWindow.mb.noDestDir", new String[] {
							file.toString(),
							info.getTorrentName()
						});
				mb.open(null);
				return;
			}

			if (!info.isValid) {
				MessageBoxShell mb = new MessageBoxShell(SWT.OK | SWT.ICON_ERROR,
						"OpenTorrentWindow.mb.notValid", new String[] {
							info.getTorrentName()
						});
				mb.open(null);
				return;
			}

			TorrentFileInfo[] files = info.getFiles();
			for (int j = 0; j < files.length; j++) {
				TorrentFileInfo fileInfo = files[j];
				if (fileInfo.getDestFileFullName().exists()) {
					sExistingFiles += fileInfo.orgFullName + " - " + info.getTorrentName()
							+ "\n";
					iNumExistingFiles++;
					if (iNumExistingFiles > 5) {
						// this has the potential effect of adding 5 files from the first 
						// torrent and then 1 file from each of the remaining torrents
						break;
					}
				}
			}
		}

		if (sExistingFiles.length() > 0) {
			if (iNumExistingFiles > 5) {
				sExistingFiles += MessageText.getString(
						"OpenTorrentWindow.mb.existingFiles.partialList", new String[] {
							"" + iNumExistingFiles
						})
						+ "\n";
			}

			MessageBoxShell mb = new MessageBoxShell(SWT.OK | SWT.CANCEL
					| SWT.ICON_WARNING, "OpenTorrentWindow.mb.existingFiles",
					new String[] {
						sExistingFiles
					});
			mb.open(null);
			if (mb.waitUntilClosed() != SWT.OK) {
				return;
			}
		}

		String sDefaultPath = COConfigurationManager.getStringParameter(PARAM_DEFSAVEPATH);
		if (!sDestDir.equals(sDefaultPath)) {
			// Move sDestDir to top of list

			// First, check to see if sDestDir is already in the list
			File fDestDir = new File(sDestDir);
			int iDirPos = -1;
			for (int i = 0; i < dirList.size(); i++) {
				String sDirName = dirList.get(i);
				File dir = new File(sDirName);
				if (dir.equals(fDestDir)) {
					iDirPos = i;
					break;
				}
			}

			// If already in list, remove it
			if (iDirPos > 0 && iDirPos < dirList.size())
				dirList.remove(iDirPos);

			// and add it to the top
			dirList.add(0, sDestDir);

			// Limit
			if (dirList.size() > 15)
				dirList.remove(dirList.size() - 1);

			// Temporary list cleanup
			try {
				for (int j = 0; j < dirList.size(); j++) {
					File dirJ = new File(dirList.get(j));
					for (int i = 0; i < dirList.size(); i++) {
						try {
							if (i == j)
								continue;

							File dirI = new File(dirList.get(i));

							if (dirI.equals(dirJ)) {
								dirList.remove(i);
								// dirList shifted up, fix indexes
								if (j > i)
									j--;
								i--;
							}
						} catch (Exception e) {
							// Ignore
						}
					}
				}
			} catch (Exception e) {
				// Ignore
			}

			COConfigurationManager.setParameter("saveTo_list", dirList);
			COConfigurationManager.save();
		}

		if (COConfigurationManager.getBooleanParameter("DefaultDir.AutoUpdate")
				&& !COConfigurationManager.getBooleanParameter("Use default data dir"))
			COConfigurationManager.setParameter(PARAM_DEFSAVEPATH, sDestDir);

		openTorrents();
		close(true, false);
	}

	/**
	 * @param layout
	 * @return
	 */
	private GridLayout FixupLayout(GridLayout layout, boolean bFixMargin) {
		if (Constants.isOSX) {
			layout.horizontalSpacing = 0;
			layout.verticalSpacing = 0;

			if (bFixMargin) {
				layout.marginHeight = 0;
				layout.marginWidth = 0;
			}
		}

		return layout;
	}

	private void updateDataDirCombo() {
		if (cmbDataDir == null) {
			return;
		}

		try {
			bSkipDataDirModify = true;

			int[] indexes = torrentTable.getSelectionIndices();

			if (indexes.length == 0) {
				if (cmbDataDir.getItemCount() == 0) {
					cmbDataDir.add(sDestDir);
				}
				cmbDataDir.setText(sDestDir);
				return;
			}

			boolean allSame = true;
			String lastDir = null;
			for (int i = 0; i < indexes.length; i++) {
				TorrentInfo info = (TorrentInfo) torrentList.get(indexes[i]);
				if (lastDir != null && !info.sDestDir.equals(lastDir)) {
					allSame = false;
					break;
				}
				lastDir = info.sDestDir;
			}

			if (allSame && lastDir != null) {
				cmbDataDir.setText(lastDir);
				sDestDir = lastDir;
			} else {
				cmbDataDir.setText("");
			}
		} finally {
			bSkipDataDirModify = false;
		}
	}

	private void updateStartModeCombo() {
		if (cmbStartMode == null)
			return;

		int[] indexes = torrentTable.getSelectionIndices();
		String[] sItemsText = new String[startModes.length];
		int iMaxMatches = 0;
		int iIndexToSelect = getDefaultStartMode();
		for (int i = 0; i < startModes.length; i++) {
			int iMatches = 0;
			for (int j = 0; j < indexes.length; j++) {
				TorrentInfo info = (TorrentInfo) torrentList.get(indexes[j]);
				if (info.iStartID == i)
					iMatches++;
			}

			if (iMatches > iMaxMatches) {
				iMaxMatches = iMatches;
				iIndexToSelect = i;
			}

			String sText = MessageText.getString("OpenTorrentWindow.startMode."
					+ startModes[i]);
			if (iMatches > 0)
				sText += " "
						+ MessageText.getString("OpenTorrentWindow.xOfTotal", new String[] {
							Integer.toString(iMatches),
							Integer.toString(indexes.length)
						});
			sItemsText[i] = sText;
		}
		cmbStartMode.setItems(sItemsText);
		cmbStartMode.select(iIndexToSelect);
		cmbStartMode.layout(true);
	}

	private void updateQueueLocationCombo() {
		if (cmbQueueLocation == null)
			return;

		int[] indexes = torrentTable.getSelectionIndices();
		String[] sItemsText = new String[queueLocations.length];
		int iMaxMatches = 0;
		int iIndexToSelect = QUEUELOCATION_BOTTOM;
		for (int i = 0; i < queueLocations.length; i++) {
			int iMatches = 0;
			for (int j = 0; j < indexes.length; j++) {
				TorrentInfo info = (TorrentInfo) torrentList.get(indexes[j]);
				if (info.iQueueLocation == i)
					iMatches++;
			}

			if (iMatches > iMaxMatches) {
				iMaxMatches = iMatches;
				iIndexToSelect = i;
			}

			String sText = MessageText.getString("OpenTorrentWindow.addPosition."
					+ queueLocations[i]);
			if (iMatches > 0)
				sText += " "
						+ MessageText.getString("OpenTorrentWindow.xOfTotal", new String[] {
							Integer.toString(iMatches),
							Integer.toString(indexes.length)
						});
			sItemsText[i] = sText;
		}
		cmbQueueLocation.setItems(sItemsText);
		cmbQueueLocation.select(iIndexToSelect);
	}

	/**
	 * @param c
	 * @param keyListener
	 */
	private void setPasteKeyListener(Control c, KeyListener keyListener) {
		if (!(c instanceof Text) && !(c instanceof Combo)
				&& !(c instanceof Composite) || (c instanceof Table)) {
			c.addKeyListener(keyListener);
		}
		if (c instanceof Composite) {
			Control[] controls = ((Composite) c).getChildren();
			for (int i = 0; i < controls.length; i++) {
				setPasteKeyListener(controls[i], keyListener);
			}
		}
	}

	private void browseURL() {
		new OpenUrlWindow(shellForChildren, null, null,
				OpenTorrentWindow.this);
	}

	private void close(boolean dispose, boolean bCancel) {
		stOpenTorrentWindow = null;
		// Can't rely on (stOpenTorrentWindow == null) to check if we are closed
		// since another thread may create another OpenTorrentWindow while
		// we are closing this one.
		bClosed = true;

		try {
			UIUpdaterSWT.getInstance().removeUpdater(this);
		} catch (Exception e) {
			Debug.out(e);
		}
		
		if (dispose && shell != null && !shell.isDisposed()) {
			// We won't be recalled by disposal hook because we set bClosed
			shell.dispose();
		}

		Utils.disposeSWTObjects(disposeList);

		if (bCancel) {
			
			List<TorrentDownloader> to_cancel;
			
			synchronized( downloaders ){
				
				to_cancel = new ArrayList<TorrentDownloader>( downloaders );
				
				downloaders.clear(); 
			}
			
			if (to_cancel.size() > 0){
				for (Iterator iter = to_cancel.iterator(); iter.hasNext();) {
					TorrentDownloader element = (TorrentDownloader) iter.next();
					element.cancel();
				}
			}

			for (Iterator iter = torrentList.iterator(); iter.hasNext();) {
				TorrentInfo info = (TorrentInfo) iter.next();
				if (info.bDeleteFileOnCancel) {
					File file = new File(info.sFileName);
					if (file.exists())
						file.delete();
				}
			}
			torrentList.clear();
		}
	}

	private void createTorrentListArea(Composite cArea) {
		GridData gridData;
		TableColumn tc;

		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		cArea.setLayout(layout);

		torrentTable = new Table(cArea, SWT.MULTI | SWT.BORDER
				| SWT.FULL_SELECTION | SWT.VIRTUAL);
		gridData = new GridData(GridData.FILL_HORIZONTAL
				| GridData.VERTICAL_ALIGN_FILL);
		gridData.heightHint = 50;
		gridData.widthHint = 450;
		torrentTable.setLayoutData(gridData);

		tc = new TableColumn(torrentTable, SWT.NULL);
		Messages.setLanguageText(tc, "OpenTorrentWindow.torrentTable.name");
		tc.setWidth(150);
		tc = new TableColumn(torrentTable, SWT.NULL);
		Messages.setLanguageText(tc, "OpenTorrentWindow.torrentTable.saveLocation");
		tc.setWidth(150);
		tc = new TableColumn(torrentTable, SWT.NULL);
		Messages.setLanguageText(tc, "OpenTorrentWindow.startMode");
		tc.setWidth(70);
		tc = new TableColumn(torrentTable, SWT.NULL);
		Messages.setLanguageText(tc, "OpenTorrentWindow.addPosition");
		tc.setWidth(80);

		if (Utils.LAST_TABLECOLUMN_EXPANDS)
			tc.setData("Width", new Long(80));

		torrentTable.addListener(SWT.SetData, new Listener() {
			public void handleEvent(Event event) {
				if (bClosed)
					return;

				TableItem item = (TableItem) event.item;
				int index = torrentTable.indexOf(item);
				if (index < 0)
					return;

				TorrentInfo info = (TorrentInfo) torrentList.get(index);

				item.setText(new String[] {
					info.getTorrentName(),
					info.getDataDir(),
					MessageText.getString("OpenTorrentWindow.startMode."
							+ startModes[info.iStartID]),
					MessageText.getString("OpenTorrentWindow.addPosition."
							+ queueLocations[info.iQueueLocation])
				});
				if (!info.isValid) {
					item.setForeground(Colors.red);
					Font font = item.getFont();
					FontData[] fd = font.getFontData();
					for (int i = 0; i < fd.length; i++) {
						fd[i].setStyle(SWT.ITALIC);
					}
					font = new Font(item.getDisplay(), fd);
					disposeList.add(font);
					item.setFont(font);
				}
				Utils.alternateRowBackground(item);
			}
		});

		torrentTable.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				dataFiles.clear();
				int[] indexes = torrentTable.getSelectionIndices();
				for (int i = 0; i < indexes.length; i++) {
					TorrentInfo info = (TorrentInfo) torrentList.get(indexes[i]);
					TorrentFileInfo[] files = info.getFiles();
					dataFiles.addAll(Arrays.asList(files));
				}

				updateDataDirCombo();
				updateStartModeCombo();
				updateQueueLocationCombo();

				dataFileTable.setItemCount(dataFiles.size());
				dataFileTable.clearAll();
				editCell(-1);
				updateSize();
				resizeTables(2);
			}
		});

		torrentTable.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.DEL) {
					deleteSelected(torrentTable, torrentList);
					e.doit = false;
				}
			}
		});

		torrentTable.setHeaderVisible(true);

		// Menu for tableTorrents

		String sTitle;
		Menu menu = new Menu(torrentTable.getShell());
		MenuItem item;
		sTitle = MessageText.getString("OpenTorrentWindow.startMode");

		int userMode = COConfigurationManager.getIntParameter("User Mode");
		for (int i = 0; i < startModes.length; i++) {
			if (i == STARTMODE_FORCESTARTED && userMode == 0)
				continue;

			item = new MenuItem(menu, SWT.PUSH);
			item.setData("Value", new Long(i));
			item.setText(sTitle
					+ ": "
					+ MessageText.getString("OpenTorrentWindow.startMode."
							+ startModes[i]));

			item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					Long l = (Long) e.widget.getData("Value");
					if (l != null) {
						setSelectedStartMode(l.intValue());
						checkSeedingMode();
					}
				}
			});
		}

		item = new MenuItem(menu, SWT.SEPARATOR);
		sTitle = MessageText.getString("OpenTorrentWindow.addPosition");

		for (int i = 0; i < queueLocations.length; i++) {
			item = new MenuItem(menu, SWT.PUSH);
			item.setData("Value", new Long(i));
			item.setText(sTitle
					+ ": "
					+ MessageText.getString("OpenTorrentWindow.addPosition."
							+ queueLocations[i]));

			item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					Long l = (Long) e.widget.getData("Value");
					if (l != null) {
						setSelectedQueueLocation(l.intValue());
					}
				}
			});
		}

		item = new MenuItem(menu, SWT.SEPARATOR);

			// ip filter mode
		
		final MenuItem ipf_enable = new MenuItem(menu, SWT.CHECK );
		// steal text
		Messages.setLanguageText(ipf_enable, "MyTorrentsView.menu.ipf_enable");
		ipf_enable.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int[] indexes = torrentTable.getSelectionIndices();
				for (int i = 0; i < indexes.length; i++){
					
					TorrentInfo info = (TorrentInfo) torrentList.get(indexes[i]);
					
					info.disableIPFilter = !ipf_enable.getSelection();
				}
			}
		});
		
		menu.addListener(
			SWT.Show, 
			new Listener()
			{
				public void
				handleEvent(Event e) 
				{
					int[] indexes = torrentTable.getSelectionIndices();

					boolean bEnabled = 	indexes.length > 0 &&
										IpFilterManagerFactory.getSingleton().getIPFilter().isEnabled();
					
					if ( bEnabled ){
						boolean allChecked 		= true;
						boolean allUnchecked	= true;
				
						for (int i = 0; i < indexes.length; i++){
							
							TorrentInfo info = (TorrentInfo) torrentList.get(indexes[i]);
							
							boolean b = info.disableIPFilter;
								
							if ( b ){
								allUnchecked 	= false;
							}else{
								allChecked 		= false;
							}
						}
			
						boolean	bChecked;
						
						if ( allUnchecked ){
							bChecked = true;
						}else if ( allChecked ){
							bChecked = false;
						}else{
							bChecked = false;
						}
						
						ipf_enable.setSelection(bChecked);
					}
					
					ipf_enable.setEnabled(bEnabled);	
				}
			});
		
		item = new MenuItem(menu, SWT.PUSH);
		// steal text
		Messages.setLanguageText(item, "MyTorrentsView.menu.remove");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				deleteSelected(torrentTable, torrentList);
			}
		});

		item = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(item,
				"OpenTorrentWindow.fileList.changeDestination");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int[] indexes = torrentTable.getSelectionIndices();
				String sDefPath = sDestDir;

				for (int i = 0; i < indexes.length; i++) {
					TorrentInfo info = (TorrentInfo) torrentList.get(indexes[i]);

					TorrentFileInfo[] files = info.getFiles();
					if (files.length == 1 && info.torrent.isSimpleTorrent()) {
						changeFileDestination(new int[] {
							0
						});
					} else {
						DirectoryDialog dDialog = new DirectoryDialog(shellForChildren,
								SWT.SYSTEM_MODAL);

						dDialog.setFilterPath(sDefPath);
						dDialog.setMessage(MessageText.getString("MainWindow.dialog.choose.savepath")
								+ " (" + info.getTorrentName() + ")");
						String sNewDir = dDialog.open();

						if (sNewDir == null)
							return;

						File newDir = new File(sNewDir).getAbsoluteFile();
						
						if(newDir.isDirectory())
							sDefPath = sNewDir;

						info.sDestDir = newDir.getParent();
						if (info.sDestDir == null)
							info.sDestDir = newDir.getPath();
						info.sDestSubDir = newDir.getName();

						for (int j = 0; j < files.length; j++) {
							TorrentFileInfo fileInfo = files[j];
							fileInfo.setDestFileName(null);
						}
					}

				} // for i

				checkSeedingMode();
				updateDataDirCombo();
				diskFreeInfoRefreshPending = true;
			}
		});

		torrentTable.setMenu(menu);

		Composite cTorrentListRight = new Composite(cArea, SWT.NONE);
		gridData = new GridData();
		cTorrentListRight.setLayoutData(gridData);
		RowLayout rLayout = new RowLayout(SWT.VERTICAL);
		rLayout.marginBottom = 0;
		rLayout.marginLeft = 0;
		rLayout.marginRight = 0;
		rLayout.marginTop = 0;
		if (!Constants.isOSX)
			rLayout.spacing = 0;
		rLayout.fill = true;
		cTorrentListRight.setLayout(rLayout);

		ImageLoader imageLoader = ImageLoader.getInstance();

		Button torMoveUp = new Button(cTorrentListRight, SWT.PUSH);
		imageLoader.setButtonImage(torMoveUp, "up");
		torMoveUp.setToolTipText(MessageText.getString("Button.moveUp"));
		torMoveUp.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				int[] indices = torrentTable.getSelectionIndices();
				if (indices.length == 0)
					return;

				Arrays.sort(indices);
				if (indices[0] == 0)
					return;

				for (int i = 0; i < indices.length; i++) {
					int pos = indices[i];
					Object save = torrentList.get(pos - 1);
					torrentList.set(pos - 1, torrentList.get(pos));
					torrentList.set(pos, save);

					indices[i]--;
				}
				torrentTable.setSelection(indices);
				torrentTable.clearAll();
			}
		});

		Button torMoveDown = new Button(cTorrentListRight, SWT.PUSH);
		imageLoader.setButtonImage(torMoveDown, "down");
		torMoveDown.setToolTipText(MessageText.getString("Button.moveDown"));
		torMoveDown.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				int[] indices = torrentTable.getSelectionIndices();
				if (indices.length == 0)
					return;

				Arrays.sort(indices);
				int max = indices.length - 1;
				if (indices[max] == torrentList.size() - 1)
					return;

				for (int i = max; i >= 0; i--) {
					int pos = indices[i];
					Object save = torrentList.get(pos + 1);
					torrentList.set(pos + 1, torrentList.get(pos));
					torrentList.set(pos, save);

					indices[i]++;
				}
				torrentTable.setSelection(indices);
				torrentTable.clearAll();
			}
		});

		Button torMoveRemove = new Button(cTorrentListRight, SWT.PUSH);
		torMoveRemove.setToolTipText(MessageText.getString("OpenTorrentWindow.torrent.remove"));
		imageLoader.setButtonImage(torMoveRemove, "delete");
		torMoveRemove.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				deleteSelected(torrentTable, torrentList);
			}
		});

	}

	/**
	 * @param iLocation
	 */
	protected void setSelectedQueueLocation(int iLocation) {
		int[] indices = torrentTable.getSelectionIndices();
		for (int i = 0; i < indices.length; i++) {
			TorrentInfo info = (TorrentInfo) torrentList.get(indices[i]);
			info.iQueueLocation = iLocation;
		}
		updateQueueLocationCombo();
		torrentTable.clear(indices);
	}

	/**
	 * @param iStartID
	 */
	protected void setSelectedStartMode(int iStartID) {
		int[] indices = torrentTable.getSelectionIndices();
		for (int i = 0; i < indices.length; i++) {
			TorrentInfo info = (TorrentInfo) torrentList.get(indices[i]);
			info.iStartID = iStartID;
		}

		checkSeedingMode();
		updateStartModeCombo();
		torrentTable.clear(indices);
	}

	private void checkSeedingMode() {
		// Check for seeding
		for (int i = 0; i < torrentList.size(); i++) {
			boolean bTorrentValid = true;
			TorrentInfo info = (TorrentInfo) torrentList.get(i);

			if (info.iStartID == STARTMODE_SEEDING) {
				// check if all selected files exist
				TorrentFileInfo[] files = info.getFiles();
				for (int j = 0; j < files.length; j++) {
					TorrentFileInfo fileInfo = files[j];
					if (!fileInfo.bDownload)
						continue;


					File file = fileInfo.getDestFileFullName();
					if (!file.exists()) {
						fileInfo.isValid = false;
						bTorrentValid = false;
					} else if (!fileInfo.isValid) {
						fileInfo.isValid = true;
					}
				}
			}

			info.isValid = bTorrentValid;
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (torrentTable != null && !torrentTable.isDisposed()) {
					torrentTable.clearAll();
				}
				if (dataFileTable != null && !dataFileTable.isDisposed()) {
					dataFileTable.clearAll();
					editCell(-1);
				}
			}
		});
	}

	private void deleteSelected(Table table, ArrayList list) {
		int[] indexes = table.getSelectionIndices();
		Arrays.sort(indexes);
		for (int i = indexes.length - 1; i >= 0; i--) {
			if (list.get(indexes[i]) instanceof TorrentInfo) {
				TorrentInfo info = (TorrentInfo) list.get(indexes[i]);
				if (info.bDeleteFileOnCancel) {
					File file = new File(info.sFileName);
					if (file.exists())
						file.delete();
				}
			}
			list.remove(indexes[i]);
		}
		table.setItemCount(list.size());
		table.clearAll();
		table.notifyListeners(SWT.Selection, new Event());
	}
	
	
	private void editCell(final int row)
	{
		Text oldEditor = (Text)dataFileTableEditor.getEditor();
		if(row < 0 || row >= dataFileTable.getItemCount())
		{
			if(oldEditor != null && !oldEditor.isDisposed())
				oldEditor.dispose();
			return;
		}
		
		
		final Text newEditor = oldEditor == null || oldEditor.isDisposed() ? new Text(dataFileTable,SWT.BORDER) : oldEditor;
		final TorrentFileInfo file = (TorrentFileInfo) dataFiles.get(row);
		final String uneditedName = file.getDestFileName();
		TableItem item = dataFileTable.getItem(row);
		TableColumn column = dataFileTable.getColumn(EDIT_COLUMN_INDEX);

		newEditor.setText(uneditedName);
		newEditor.selectAll();
		newEditor.forceFocus();
		Rectangle leftAlignedBounds = item.getBounds(EDIT_COLUMN_INDEX);
		leftAlignedBounds.width = dataFileTableEditor.minimumWidth = newEditor.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		if(leftAlignedBounds.intersection(dataFileTable.getClientArea()).equals(leftAlignedBounds))
			dataFileTableEditor.horizontalAlignment = SWT.LEFT;
		else
			dataFileTableEditor.horizontalAlignment = SWT.RIGHT;
		
		dataFileTable.deselectAll();
		dataFileTable.select(row);
		dataFileTable.showItem(item);
		dataFileTable.showColumn(column);
		
		class QuickEditListener implements ModifyListener, SelectionListener, KeyListener, TraverseListener {
			public void modifyText(ModifyEvent e) {
				file.setDestFileName(newEditor.getText());
				try
				{
					file.getDestFileFullName().getCanonicalFile();
					newEditor.setBackground(null);
				} catch (IOException e1)
				{
					newEditor.setBackground(Colors.colorErrorBG);
				}
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
				try
				{
					file.getDestFileFullName().getCanonicalFile();
				} catch (IOException e1)
				{
					file.setDestFileName(uneditedName);
				}
				move(row,1,(Text)e.widget);
			}
			
			public void widgetSelected(SelectionEvent e) {}
			public void keyReleased(KeyEvent e) {}
			
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == SWT.ARROW_DOWN || e.keyCode == SWT.ARROW_UP)
				{
					e.doit = false;
					move(row,e.keyCode == SWT.ARROW_DOWN ? 1 : -1,(Text)e.widget);
				}
			}
			
			public void keyTraversed(TraverseEvent e) {
				if(e.detail == SWT.TRAVERSE_ESCAPE || e.detail == SWT.TRAVERSE_RETURN)
					e.doit = false;
				if(e.detail == SWT.TRAVERSE_ESCAPE)
					editCell(-1);
			}
			
			private void move(int oldRow, int offset, Text current)
			{
				current.removeModifyListener(QuickEditListener.this);
				current.removeSelectionListener(QuickEditListener.this);
				current.removeKeyListener(QuickEditListener.this);
				current.removeTraverseListener(QuickEditListener.this);
				editCell(oldRow+offset);
				dataFileTable.clear(oldRow);
			}
			
		}
		
		QuickEditListener listener = new QuickEditListener();
		
		newEditor.addModifyListener(listener);
		newEditor.addSelectionListener(listener);
		newEditor.addKeyListener(listener);
		newEditor.addTraverseListener(listener);
		
		dataFileTableEditor.setEditor(newEditor, dataFileTable.getItem(row), EDIT_COLUMN_INDEX);
	}

	private static final int EDIT_COLUMN_INDEX = 1;
	
	
	private void createTableDataFiles(Composite cArea) {
		GridData gridData;
		TableColumn tc;

		dataFileTable = new Table(cArea, SWT.BORDER | SWT.CHECK
				| SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.MULTI);
		dataFileTableEditor = new TableEditor(dataFileTable);
		dataFileTableEditor.grabHorizontal = true;
		dataFileTableEditor.minimumWidth = 50;
		
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 80;
		gridData.widthHint = 100;
		dataFileTable.setLayoutData(gridData);
		
		

		tc = new TableColumn(dataFileTable, SWT.NULL);
		Messages.setLanguageText(tc, "OpenTorrentWindow.fileTable.fileName");
		tc.setWidth(150);
		tc = new TableColumn(dataFileTable, SWT.NULL);
		Messages.setLanguageText(tc, "OpenTorrentWindow.fileTable.destinationName");
		tc.setWidth(140);
		tc = new TableColumn(dataFileTable, SWT.NULL);
		Messages.setLanguageText(tc, "OpenTorrentWindow.fileTable.size");
		tc.setAlignment(SWT.TRAIL);
		tc.setWidth(90);

		if (Utils.LAST_TABLECOLUMN_EXPANDS)
			tc.setData("Width", new Long(90));

		dataFileTable.addListener(SWT.SetData, new Listener() {
			public void handleEvent(Event event) {
				if (bClosed)
					return;
				
				final TableItem item = (TableItem) event.item;
				
				int index = dataFileTable.indexOf(item);
				final TorrentFileInfo file = (TorrentFileInfo) dataFiles.get(index);

				item.setText(new String[] {
					file.orgFullName,
					file.isLinked() ? file.getDestFileFullName().toString() : file.getDestFileName(),
					DisplayFormatters.formatByteCountToKiBEtc(file.lSize)
				});
				if (!file.isValid) {
					item.setForeground(Colors.red);
					Font font = item.getFont();
					FontData[] fd = font.getFontData();
					for (int i = 0; i < fd.length; i++) {
						fd[i].setStyle(SWT.ITALIC);
					}
					font = new Font(item.getDisplay(), fd);
					disposeList.add(font);
					item.setFont(font);
				}
				Utils.alternateRowBackground(item);
				Utils.setCheckedInSetData(item, file.bDownload);

				item.setGrayed(!file.okToDisable());
			}
		});

		dataFileTable.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent event) {
				if (event.detail == SWT.CHECK) {
					TableItem item = (TableItem) event.item;
					int index = dataFileTable.indexOf(item);
					TorrentFileInfo file = (TorrentFileInfo) dataFiles.get(index);
					// don't allow disabling of small files
					// XXX Maybe warning prompt instead?
					if (!item.getChecked() && !file.okToDisable())
						item.setChecked(true);
					else
						file.bDownload = item.getChecked();

					updateSize();
				}
			}

		});
		
		dataFileTable.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				editCell(-1); // cleanup
				if(e.button != 1)
					return;

				TableItem[] items = dataFileTable.getItems();
				boolean found = false;
				int i;
				outer: for (i = 0; i < items.length; i++)
				{
					TableItem item = items[i];
					Rectangle rect = item.getBounds();
					if (e.y < rect.y || (rect.y + rect.height) < e.y)
						continue;
					for (int j = 0; j < dataFileTable.getColumnCount(); j++)
					{
						if (!item.getBounds(j).contains(e.x, e.y))
							continue;
						found = j == EDIT_COLUMN_INDEX;
						break outer;
					}
				}
				if(found)
					editCell(i);
			}
		});
		
		

		dataFileTable.setHeaderVisible(true);

		Menu menu = new Menu(dataFileTable);
		dataFileTable.setMenu(menu);

		MenuItem item;

		item = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(item,
				"OpenTorrentWindow.fileList.changeDestination");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int[] indexes = dataFileTable.getSelectionIndices();
				changeFileDestination(indexes);
			}
		});

		Composite cBottomArea = new Composite(cArea, SWT.NONE);
		GridLayout gLayout = new GridLayout();
		gLayout.marginHeight = 0;
		gLayout.marginWidth = 0;
		gLayout.numColumns = 2;
		gLayout.verticalSpacing = 0;
		cBottomArea.setLayout(gLayout);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		cBottomArea.setLayoutData(gridData);

		Composite cButtons = new Composite(cBottomArea, SWT.NONE);
		RowLayout rLayout = new RowLayout(SWT.HORIZONTAL);
		rLayout.wrap = false;
		rLayout.marginBottom = 0;
		rLayout.marginLeft = 0;
		rLayout.marginRight = 0;
		rLayout.marginTop = 0;
		cButtons.setLayout(rLayout);
		gridData = new GridData(SWT.END, SWT.BEGINNING, false, false);
		gridData.verticalSpan = 2;
		cButtons.setLayoutData(gridData);

		Button btnSelectAll = new Button(cButtons, SWT.PUSH);
		Messages.setLanguageText(btnSelectAll, "Button.selectAll");
		btnSelectAll.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				dataFileTable.selectAll();
			}
		});

		Button btnMarkSelected = new Button(cButtons, SWT.PUSH);
		Messages.setLanguageText(btnMarkSelected, "Button.markSelected");
		btnMarkSelected.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				int[] indexes = dataFileTable.getSelectionIndices();
				for (int i = 0; i < indexes.length; i++) {
					TorrentFileInfo file = (TorrentFileInfo) dataFiles.get(indexes[i]);
					file.bDownload = true;
				}
				dataFileTable.clearAll();
				updateSize();
			}
		});

		Button btnUnmarkSelected = new Button(cButtons, SWT.PUSH);
		Messages.setLanguageText(btnUnmarkSelected, "Button.unmarkSelected");
		btnUnmarkSelected.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				int[] indexes = dataFileTable.getSelectionIndices();
				for (int i = 0; i < indexes.length; i++) {
					TorrentFileInfo file = (TorrentFileInfo) dataFiles.get(indexes[i]);
					if (file.okToDisable())
						file.bDownload = false;
				}
				dataFileTable.clearAll();
				updateSize();
			}
		});

		dataFileTableLabel = new Label(cBottomArea, SWT.WRAP);
		dataFileTableLabel.setAlignment(SWT.RIGHT);
		gridData = new GridData(SWT.END, SWT.BEGINNING, true, false);
		dataFileTableLabel.setLayoutData(gridData);

		diskspaceComp = new Composite(cBottomArea, SWT.NONE);
		gLayout = new GridLayout(2, false);
		gLayout.marginHeight = gLayout.marginWidth = 1;
		gLayout.verticalSpacing = 0;
		gLayout.horizontalSpacing = 15;
		diskspaceComp.setLayout(gLayout);
		gridData = new GridData(SWT.END, SWT.BEGINNING, true, false);
		diskspaceComp.setLayoutData(gridData);

	}

	/**
	 * @param indexes
	 */
	protected void changeFileDestination(int[] indexes) {
		for (int i = 0; i < indexes.length; i++) {
			TorrentFileInfo fileInfo = (TorrentFileInfo) dataFiles.get(indexes[i]);
			int style = (fileInfo.parent.iStartID == STARTMODE_SEEDING) ? SWT.OPEN
					: SWT.SAVE;
			FileDialog fDialog = new FileDialog(shellForChildren, SWT.SYSTEM_MODAL
					| style);

			String sFilterPath = fileInfo.getDestPathName();
			String sFileName = fileInfo.orgFileName;

			File f = new File(sFilterPath);
			if (!f.isDirectory()) {
				// Move up the tree until we have an existing path
				while (sFilterPath != null) {
					String parentPath = f.getParent();
					if (parentPath == null)
						break;

					sFilterPath = parentPath;
					f = new File(sFilterPath);
					if (f.isDirectory())
						break;
				}
			}

			if (sFilterPath != null)
				fDialog.setFilterPath(sFilterPath);
			fDialog.setFileName(sFileName);
			fDialog.setText(MessageText.getString("MainWindow.dialog.choose.savepath")
					+ " (" + fileInfo.orgFullName + ")");
			String sNewName = fDialog.open();

			if (sNewName == null)
				return;

			if (fileInfo.parent.iStartID == STARTMODE_SEEDING) {
				File file = new File(sNewName);
				if (file.length() == fileInfo.lSize)
					fileInfo.setFullDestName(sNewName);
				else {
					MessageBoxShell mb = new MessageBoxShell(SWT.OK,
							"OpenTorrentWindow.mb.badSize", new String[] {
								file.getName(),
								fileInfo.orgFullName
							});
					mb.open(null);
				}
			} else
				fileInfo.setFullDestName(sNewName);

		} // for i

		checkSeedingMode();
		updateDataDirCombo();
		diskFreeInfoRefreshPending = true;
	}

	/**
	 * Add Torrent(s) to Window using a text list of files/urls/torrents
	 * 
	 * @param sClipText Text to parse
	 * @param bVerifyOnly Only check if there's potential torrents in the text,
	 *                     do not try to add the torrents.
	 * 
	 * @return Number of torrents added or found.  When bVerifyOnly, this number
	 *          may not be exact.
	 */
	private int addTorrentsFromTextList(String sClipText, boolean bVerifyOnly) {
		String[] lines = null;
		int iNumFound = 0;
		// # of consecutive non torrent lines
		int iNoTorrentLines = 0;
		// no use checking the whole clipboard (which may be megabytes)
		final int MAX_CONSECUTIVE_NONTORRENT_LINES = 100;

		final String[] splitters = {
			"\r\n",
			"\n",
			"\r",
			"\t"
		};

		for (int i = 0; i < splitters.length; i++)
			if (sClipText.indexOf(splitters[i]) >= 0) {
				lines = sClipText.split(splitters[i]);
				break;
			}

		if (lines == null)
			lines = new String[] {
				sClipText
			};

		// Check if URL, 20 byte hash, Dir, or file
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i].trim();
			if (line.startsWith("\"") && line.endsWith("\"")) {
				if (line.length() < 3) {
					line = "";
				} else {
					line = line.substring(1, line.length() - 2);
				}
			}

			boolean ok;

			if (line == "") {
				ok = false;
			} else if (UrlUtils.isURL(line)) {
				ok = true;
			} else {
				File file = new File(line);

				if (!file.exists()) {
					ok = false;
				} else if (file.isDirectory()) {
					if (bVerifyOnly) {
						// XXX Could do a file count here, but the number found is not
						//     expected to be an exact number anyway, since we aren't
						//     event verifying if they are torrents.
						ok = true;
					} else {
						iNumFound += addTorrents(lines[i], null);
						ok = false;
					}
				} else {
					ok = true;
				}
			}

			if (!ok) {
				iNoTorrentLines++;
				lines[i] = null;
				if (iNoTorrentLines > MAX_CONSECUTIVE_NONTORRENT_LINES)
					break;
			} else {
				iNumFound++;
				iNoTorrentLines = 0;
			}
		}

		if (bVerifyOnly) {
			return iNumFound;
		}

		return addTorrents(null, lines);
	}

	/**
	 * Add Torrent(s) to window
	 * 
	 * @param sTorrentFilePath
	 * @param sTorrentFilenames
	 * @return # torrents actually added to list (or downloading)
	 */
	private int addTorrents(String sTorrentFilePath, String[] sTorrentFilenames) {
		sTorrentFilePath = ensureTrailingSeparator(sTorrentFilePath);

		// Process Directory
		if (sTorrentFilePath != null && sTorrentFilenames == null) {
			File dir = new File(sTorrentFilePath);
			if (!dir.isDirectory())
				return 0;

			final File[] files = dir.listFiles(new FileFilter() {
				public boolean accept(File arg0) {
					if (FileUtil.getCanonicalFileName(arg0.getName()).endsWith(".torrent"))
						return true;
					if (FileUtil.getCanonicalFileName(arg0.getName()).endsWith(".tor"))
						return true;
					return false;
				}
			});

			if (files.length == 0)
				return 0;

			sTorrentFilenames = new String[files.length];
			for (int i = 0; i < files.length; i++)
				sTorrentFilenames[i] = files[i].getName();
		}

		int numAdded = 0;
		for (int i = 0; i < sTorrentFilenames.length; i++) {
			if (sTorrentFilenames[i] == null || sTorrentFilenames[i] == "")
				continue;

			// Process File
			String sFileName = ((sTorrentFilePath == null) ? "" : sTorrentFilePath)
					+ sTorrentFilenames[i];

			if (!new File(sFileName).exists()) {
				// Process URL
				String sURL = UrlUtils.parseTextForURL(sTorrentFilenames[i], true);
				if (sURL != null) {
					if (COConfigurationManager.getBooleanParameter("Add URL Silently")) {
						new FileDownloadWindow(shellForChildren, sURL, null, null, this);
					} else {
						new OpenUrlWindow(shellForChildren, sURL, null, this);
					}
					numAdded++;
					continue;
				}
			}

			if (addTorrent(sFileName, sFileName) != null)
				numAdded++;
		}

		if (numAdded > 0 && shell != null && torrentTable != null
				&& !shell.isDisposed()) {
			int iTotal = torrentList.size();
			torrentTable.setItemCount(iTotal);
			// select the ones we just added
			torrentTable.select(iTotal - numAdded, iTotal - 1);
			torrentTable.clearAll();
			// select doesn't notify listeners? Do it manually.
			torrentTable.notifyListeners(SWT.Selection, new Event());

			resizeTables(1);
			checkSeedingMode();
		}
		return numAdded;
	}

	private TorrentInfo addTorrent(String sFileName,
			final String sOriginatingLocation) {
		TorrentInfo info = null;
		TOTorrent torrent = null;
		File torrentFile;
		boolean bDeleteFileOnCancel = false;

		// Make a copy if user wants that.  We'll delete it when we cancel, if we 
		// actually made a copy.
		try {
			if (sFileName.startsWith("file://localhost/")) {
				sFileName = UrlUtils.decode(sFileName.substring(16));
			}

			final File fOriginal = new File(sFileName);

			if (!fOriginal.isFile() || !fOriginal.exists()) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (shell == null)
							new MessageSlideShell(Display.getCurrent(), SWT.ICON_ERROR,
									"OpenTorrentWindow.mb.openError", fOriginal.toString(), new String[] {
										UrlUtils.decode(sOriginatingLocation),
										"Not a File"
									}, -1 );
						else {
							MessageBoxShell mb = new MessageBoxShell(SWT.OK,
									"OpenTorrentWindow.mb.openError", new String[] {
										sOriginatingLocation,
										"Not a File"
									});
							mb.open(null);
						}
					}
				});
				return null;
			}

			if (fOriginal.length() > 20*1024*1024) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (shell == null)
							new MessageSlideShell(Display.getCurrent(), SWT.ICON_ERROR,
									"OpenTorrentWindow.mb.openError", fOriginal.toString(), new String[] {
										UrlUtils.decode(sOriginatingLocation),
										"Too large to be a torrent"
									}, -1 );
						else {
							MessageBoxShell mb = new MessageBoxShell(SWT.OK,
									"OpenTorrentWindow.mb.openError", new String[] {
										sOriginatingLocation,
										"Too large to be a torrent"
									});
							mb.open(null);
						}
					}
				});
				return null;
			}

			torrentFile = TorrentUtils.copyTorrentFileToSaveDir(fOriginal, true);
			bDeleteFileOnCancel = !fOriginal.equals(torrentFile);
			// TODO if the files are still equal, and it isn't in the save
			//       dir, we should copy it to a temp file in case something
			//       re-writes it.  No need to copy a torrent coming from the
			//       downloader though..
		} catch (IOException e1) {
			// Use torrent in wherever it is and hope for the best
			// XXX Should error instead?
			torrentFile = new File(sFileName);
		}

		VuzeFileHandler vfh = VuzeFileHandler.getSingleton();
		
		VuzeFile vf = vfh.loadVuzeFile( torrentFile );
		
		if ( vf != null ){
			
  			vfh.handleFiles( new VuzeFile[]{ vf }, VuzeFileComponent.COMP_TYPE_NONE );

  			return null;
		}
		
		// Do a quick check to see if it's a torrent
		if (!TorrentUtil.isFileTorrent(torrentFile, shellForChildren,
				torrentFile.getName())) {
			if (bDeleteFileOnCancel) {
				torrentFile.delete();
			}
			return null;
		}

		// Load up the torrent, see it it's real
		try {
			torrent = TorrentUtils.readFromFile(torrentFile, false);
		} catch (final TOTorrentException e) {
			
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					if (shell == null)
						new MessageSlideShell(Display.getCurrent(), SWT.ICON_ERROR,
								"OpenTorrentWindow.mb.openError", Debug.getStackTrace(e),
								new String[] {
									sOriginatingLocation,
									e.getMessage()
								}, -1 );
					else {
						MessageBoxShell mb = new MessageBoxShell(SWT.OK,
								"OpenTorrentWindow.mb.openError", new String[] {
									sOriginatingLocation,
									e.getMessage()
								});
						mb.open(null);
					}
				}
			});

			if (bDeleteFileOnCancel)
				torrentFile.delete();

			return null;
		}

		String sExistingName = null;
		try {
			HashWrapper hash = torrent.getHashWrapper();
			if (hash != null) {
				for (int i = 0; i < torrentList.size(); i++) {
					try {
						TorrentInfo existing = (TorrentInfo) torrentList.get(i);
						if (existing.torrent.getHashWrapper().equals(hash)) {
							//sExistingName = existing.sOriginatingLocation;

							// Exit without warning when it already exists in list
							if (bDeleteFileOnCancel)
								torrentFile.delete();

							return null;
						}
					} catch (Exception e) {
					}
				}
			}
		} catch (Exception e) {
		}

		DownloadManager existingDownload = null;
		if (sExistingName == null) {
			// Check if torrent already exists in gm, and add if not
			existingDownload = (gm == null) ? null : gm.getDownloadManager(torrent);
			if (existingDownload != null) {
				sExistingName = existingDownload.getDisplayName();
			}
		}

		if (sExistingName == null) {
			info = new TorrentInfo(torrentFile.getAbsolutePath(), torrent,
					bDeleteFileOnCancel);
			info.sOriginatingLocation = sOriginatingLocation;
			torrentList.add(info);

		} else {

			final String sfExistingName = sExistingName;
			final DownloadManager fExistingDownload = existingDownload;
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					Shell mainShell = UIFunctionsManagerSWT.getUIFunctionsSWT().getMainShell();
					if (Display.getDefault().getActiveShell() == null || !mainShell.isVisible() || mainShell.getMinimized() ) {
						new MessageSlideShell(Display.getCurrent(), SWT.ICON_INFORMATION,
								MSG_ALREADY_EXISTS, null, new String[] {
									":" + sOriginatingLocation,
									sfExistingName,
									MessageText.getString(MSG_ALREADY_EXISTS_NAME),
								}, new Object[] {
									fExistingDownload
								}, -1 );
					} else {
						MessageBoxShell mb = new MessageBoxShell(SWT.OK, MSG_ALREADY_EXISTS,
								new String[] {
									":" + sOriginatingLocation,
									sfExistingName,
									MessageText.getString(MSG_ALREADY_EXISTS_NAME),
								});
						mb.open(null);
					}
				}
			});

			if (bDeleteFileOnCancel)
				torrentFile.delete();
		}

		return info;
	}

	/**
	 * Resize the columns of the tables to fit without horizontal scrollbar
	 * 
	 * @param which bitwise field of which table to recalc
	 *         Bit 0: torrents table
	 *         Bit 1: Data Files Table
	 */
	private void resizeTables(int which) {
		try {
			TableColumn[] tcs;
			if ((which & 1) > 0 && torrentTable != null
					&& !torrentTable.isDisposed()) {
				tcs = torrentTable.getColumns();
				int newSize = torrentTable.getClientArea().width - 20;
				int iLength = tcs.length;
				if (Utils.LAST_TABLECOLUMN_EXPANDS) {
					iLength--;
					newSize -= ((Long) tcs[iLength].getData("Width")).intValue();
				}

				final int columnToExpand = 1;

				for (int i = 0; i < iLength; i++)
					if (i != columnToExpand)
						newSize -= tcs[i].getWidth();

				if (newSize > 10)
					tcs[columnToExpand].setWidth(newSize);
			}

			// Adjust only first column
			if ((which & 2) > 0 && dataFileTable != null
					&& !dataFileTable.isDisposed()) {
				tcs = dataFileTable.getColumns();
				int newSize = dataFileTable.getClientArea().width - 20;
				int iLength = tcs.length;
				if (Utils.LAST_TABLECOLUMN_EXPANDS) {
					iLength--;
					newSize -= ((Long) tcs[iLength].getData("Width")).intValue();
				}

				final int columnToExpand = 0;

				for (int i = 0; i < iLength; i++)
					if (i != columnToExpand)
						newSize -= tcs[i].getWidth();

				if (newSize > 10)
					tcs[columnToExpand].setWidth(newSize);
			}
		} catch (Exception e) {
			// ignore
			e.printStackTrace();
		}
	}

	/**
	 * Open the torrents already added based on user choices
	 * 
	 * @param sDataDir 
	 */
	private void openTorrents() {
		Utils.getOffOfSWTThread(new AERunnable() {
			public void runSupport() {
				_openTorrents();
			}
		});
	}

	private void _openTorrents() {
		ArrayList addedTorrentsTop = new ArrayList();

		for (int i = 0; i < torrentList.size(); i++) {
			final TorrentInfo info = (TorrentInfo) torrentList.get(i);
			try {
				if (info.torrent == null)
					continue;

				int iStartState = (info.iStartID == STARTMODE_STOPPED)
						? DownloadManager.STATE_STOPPED : DownloadManager.STATE_QUEUED;

				final TorrentFileInfo[] files = info.getFiles();

				byte[] hash = null;
				try {
					hash = info.torrent.getHash();
				} catch (TOTorrentException e1) {
				}

				DownloadManager dm = gm.addDownloadManager(info.sFileName, hash, info.sDestDir, info.sDestSubDir, iStartState, true, info.iStartID == STARTMODE_SEEDING, new DownloadManagerInitialisationAdapter()
				{
					public void initialised(DownloadManager dm) {
						DiskManagerFileInfo[] fileInfos = dm.getDiskManagerFileInfo();
						
						boolean	reorder_mode 		= COConfigurationManager.getBooleanParameter( "Enable reorder storage mode" );
						int		reorder_mode_min_mb = COConfigurationManager.getIntParameter( "Reorder storage mode min MB" );
						
						try
						{
							dm.getDownloadState().suppressStateSave(true);
							
							boolean[] toSkip 			= new boolean[fileInfos.length];
							boolean[] toCompact 		= new boolean[fileInfos.length];
							boolean[] toReorderCompact 	= new boolean[fileInfos.length];
							
							int	comp_num 			= 0;
							int reorder_comp_num	= 0;
							
							for (int iIndex = 0; iIndex < fileInfos.length; iIndex++)
							{
								DiskManagerFileInfo fileInfo = fileInfos[iIndex];
								if (iIndex >= 0 && iIndex < files.length && files[iIndex].lSize == fileInfo.getLength())
								{
									// Always pull destination file from fileInfo and not from
									// TorrentFileInfo because the destination may have changed
									// by magic code elsewhere
									File fDest = fileInfo.getFile(true);
									if (files[iIndex].isLinked()){
									
										fDest = files[iIndex].getDestFileFullName();
										
											// Can't use fileInfo.setLink(fDest) as it renames
											// the existing file if there is one
										
										dm.getDownloadState().setFileLink( fileInfo.getFile(false), fDest);
									}
									
									if (!files[iIndex].bDownload){
									
										toSkip[iIndex] = true;
										
										if (!fDest.exists()){
											
											if ( reorder_mode && ( fileInfo.getLength()/(1024*1024)) >= reorder_mode_min_mb ){
												
												toReorderCompact[iIndex] = true;
												
												reorder_comp_num++;
												
											}else{
												
												toCompact[iIndex] = true;
												
												comp_num++;
											}
										}
									}
								}
							}
							
							if ( comp_num > 0 ){
								
								dm.getDiskManagerFileInfoSet().setStorageTypes(toCompact, DiskManagerFileInfo.ST_COMPACT);
							}
							
							if ( reorder_comp_num > 0 ){
								
								dm.getDiskManagerFileInfoSet().setStorageTypes(toReorderCompact, DiskManagerFileInfo.ST_REORDER_COMPACT );
							}
							
							dm.getDiskManagerFileInfoSet().setSkipped(toSkip, true);
							
							if ( info.disableIPFilter ){
								
								dm.getDownloadState().setFlag( DownloadManagerState.FLAG_DISABLE_IP_FILTER, true );
							}
							
						}finally{
						
							dm.getDownloadState().suppressStateSave(false);
						}
					}
				});

				// If dm is null, most likely there was an error printed.. let's hope
				// the user was notified and skip the error quietly.
				// We don't have to worry about deleting the file (info.bDelete..)
				// since gm.addDown.. will handle it.
				if (dm == null)
					continue;

				if (info.iQueueLocation == QUEUELOCATION_TOP)
					addedTorrentsTop.add(dm);

				if (info.iStartID == STARTMODE_FORCESTARTED) {
					dm.setForceStart(true);
				}

			} catch (Exception e) {
				if (shell == null)
					new MessageSlideShell(Display.getCurrent(), SWT.ICON_ERROR,
							"OpenTorrentWindow.mb.openError", Debug.getStackTrace(e),
							new String[] {
								info.sOriginatingLocation,
								e.getMessage()
							}, -1 );
				else {
					MessageBoxShell mb = new MessageBoxShell(SWT.OK, "OpenTorrentWindow.mb.openError",
							new String[] {
								info.sOriginatingLocation,
								e.getMessage()
							});
					mb.open(null);
				}
			}
		}

		if (addedTorrentsTop.size() > 0) {
			DownloadManager[] dms = (DownloadManager[]) addedTorrentsTop.toArray(new DownloadManager[0]);
			gm.moveTop(dms);
		}

		torrentList.clear();
	}

	private int getDefaultStartMode() {
		if (bDefaultForSeeding)
			return STARTMODE_SEEDING;
		return (bOverrideStartModeToStopped || COConfigurationManager.getBooleanParameter("Default Start Torrents Stopped"))
				? STARTMODE_STOPPED : STARTMODE_QUEUED;
	}

	// TorrentDownloaderCallBackInterface
	public void TorrentDownloaderEvent(int state, final TorrentDownloader inf) {
		// This method is run even if the window is closed.

		// The default is to delete file on cancel
		// We set this flag to false if we detected the file was not a torrent
		if (!inf.getDeleteFileOnCancel()
				&& (state == TorrentDownloader.STATE_CANCELLED
						|| state == TorrentDownloader.STATE_ERROR
						|| state == TorrentDownloader.STATE_DUPLICATE || state == TorrentDownloader.STATE_FINISHED)) {

			activeTorrentCount--;
			enableControl(ok, activeTorrentCount < 1);
			
				// PARG - yes, this code sucks, added some sync here to prvent some errors but obviously
				// it needs a complete rewrite
			
			synchronized( downloaders ){
				if (!downloaders.contains(inf))
					return;
				downloaders.remove(inf);
			}
			
			File file = inf.getFile();
			// we already know it isn't a torrent.. we are just using the call
			// to popup the message
			TorrentUtil.isFileTorrent(file, shellForChildren, inf.getURL());
			if (file.exists()) {
				file.delete();
			}
			return;
		}

		if (state == TorrentDownloader.STATE_INIT) {
			activeTorrentCount++;
			enableControl(ok, activeTorrentCount < 1);
			synchronized( downloaders ){
				downloaders.add(inf);
			}
		} else if (state == TorrentDownloader.STATE_FINISHED) {
			activeTorrentCount--;
			enableControl(ok, activeTorrentCount < 1);

			// This can be called more than once for each inf..
			synchronized( downloaders ){
				if (!downloaders.contains(inf))
					return;
				downloaders.remove(inf);
			}
			
			File file = inf.getFile();
			if (addTorrent(file.getAbsolutePath(), inf.getURL()) == null) {
				// addTorrent may not delete it on error if the downloader saved it
				// to the place where user wants to store torrents (which is most 
				// likely) 
				if (file.exists())
					file.delete();

			} else {
				if (shell != null && !shell.isDisposed()) {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							torrentTable.setItemCount(torrentList.size());
							torrentTable.clearAll();

							// select the one we just added
							torrentTable.select(torrentList.size() - 1);
							// select doesn't notify listeners? Do it manually.
							torrentTable.notifyListeners(SWT.Selection, new Event());

							resizeTables(1);
						}
					});
				} else {
					String saveSilentlyDir = getSaveSilentlyDir();
					if (saveSilentlyDir != null) {
						sDestDir = saveSilentlyDir;
						for (int i = 0; i < torrentList.size(); i++) {
							final TorrentInfo info = (TorrentInfo) torrentList.get(i);
							info.renameDuplicates();
						}

						openTorrents();
					}
				}
			}

			checkSeedingMode();
		} else if (state == TorrentDownloader.STATE_CANCELLED
				|| state == TorrentDownloader.STATE_ERROR
				|| state == TorrentDownloader.STATE_DUPLICATE) {
			activeTorrentCount--;
			enableControl(ok, activeTorrentCount < 1);
			synchronized( downloaders ){
				downloaders.remove(inf);
			}
		} else if (state == TorrentDownloader.STATE_DOWNLOADING) {
			int count = inf.getLastReadCount();
			int numRead = inf.getTotalRead();

			if (!inf.getDeleteFileOnCancel() && numRead >= 16384) {
				inf.cancel();
			} else if (numRead == count && count > 0) {
				final byte[] bytes = inf.getLastReadBytes();
				if (bytes[0] != 'd') {
					inf.setDeleteFileOnCancel(false);
				}
			}
		} else {
			return;
		}
	}

	/**
	 * Class to store one Torrent file's info.  Used to populate table and store
	 * user's choices.
	 */
	private class TorrentInfo
	{
		/** Where the torrent came from.  Could be a file, URL, or some other text */
		String sOriginatingLocation;

		/** Filename the .torrent is saved to */
		String sFileName;

		String sDestDir;

		/** for multifiletorrents and change location */
		String sDestSubDir;

		TOTorrent torrent;

		int iStartID;

		int iQueueLocation;

		boolean isValid;

		boolean bDeleteFileOnCancel;

		private TorrentFileInfo[] files = null;

		boolean disableIPFilter = false;
		
		/**
		 * Init
		 * 
		 * @param sFileName
		 * @param torrent
		 * @param bDeleteFileOnCancel 
		 */
		public TorrentInfo(String sFileName, TOTorrent torrent,
				boolean bDeleteFileOnCancel) {
			this.bDeleteFileOnCancel = bDeleteFileOnCancel;
			this.sFileName = sFileName;
			this.sOriginatingLocation = sFileName;
			this.torrent = torrent;
			this.sDestDir = OpenTorrentWindow.this.sDestDir;

			iStartID = getDefaultStartMode();
			iQueueLocation = QUEUELOCATION_BOTTOM;
			isValid = true;

			// Force a check on the encoding, will prompt user if we dunno
			try {
				LocaleTorrentUtil.getTorrentEncoding(TorrentInfo.this.torrent);
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (getSaveSilentlyDir() == null
					&& COConfigurationManager.getBooleanParameter("DefaultDir.BestGuess")
					&& !COConfigurationManager.getBooleanParameter(PARAM_MOVEWHENDONE)) {
				this.sDestDir = getSmartDestDir();
			}
		}

		public String getParentDir() {
			return sDestDir;
		}
		
		public void setParentDir(String parentDir)
		{
			sDestDir = parentDir;
		}

		public String getDataDir() {
			if (torrent.isSimpleTorrent())
				return sDestDir;
			return new File(sDestDir, sDestSubDir == null ? FileUtil.convertOSSpecificChars(getTorrentName(), true) 
					: sDestSubDir).getPath();
		}

		public String getSmartDestDir() {
			String sSmartDir = sDestDir;
			try {
				String name = getTorrentName();
				String torrentFileName = new File(sFileName).getName().replaceFirst(
						"\\.torrent$", "");
				int totalSegmentsLengths = 0;

				String[][] segments = {
					name.split("[^a-zA-Z]+"),
					torrentFileName.split("[^a-zA-Z]+")
				};
				List downloadManagers = gm.getDownloadManagers();

				for (int x = 0; x < segments.length; x++) {
					String[] segmentArray = segments[x];
					for (int i = 0; i < segmentArray.length; i++) {
						int l = segmentArray[i].length();
						if (l <= 1) {
							continue;
						}
						segmentArray[i] = segmentArray[i].toLowerCase();
						totalSegmentsLengths += l;
					}
				}

				int maxMatches = 0;
				DownloadManager match = null;
				for (Iterator iter = downloadManagers.iterator(); iter.hasNext();) {
					DownloadManager dm = (DownloadManager) iter.next();

					if (dm.getState() == DownloadManager.STATE_ERROR) {
						continue;
					}

					int numMatches = 0;

					String dmName = dm.getDisplayName().toLowerCase();

					for (int x = 0; x < segments.length; x++) {
						String[] segmentArray = segments[x];
						for (int i = 0; i < segmentArray.length; i++) {
							int l = segmentArray[i].length();
							if (l <= 1) {
								continue;
							}

							String segment = segmentArray[i];

							if (dmName.indexOf(segment) >= 0) {
								numMatches += l;
							}
						}
					}

					if (numMatches > maxMatches) {
						maxMatches = numMatches;
						match = dm;
					}
				}
				if (match != null) {
					//System.out.println(match + ": " + (maxMatches * 100 / totalSegmentsLengths) + "%\n");
					int iMatchLevel = (maxMatches * 100 / totalSegmentsLengths);
					if (iMatchLevel >= 30) {
						File f = match.getSaveLocation();
						if (!f.isDirectory() || match.getDiskManagerFileInfo().length > 1) {
							// don't place data within another torrent's data dir
							f = f.getParentFile();
						}

						if (f != null && f.isDirectory()) {
							sSmartDir = f.getAbsolutePath();
						}
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			return sSmartDir;
		}

		public TorrentFileInfo[] getFiles() {
			if (files == null && torrent != null) {
				TOTorrentFile[] tfiles = torrent.getFiles();
				files = new TorrentFileInfo[tfiles.length];
				for (int i = 0; i < files.length; i++) {
					files[i] = new TorrentFileInfo(this, tfiles[i], i);
				}
			}

			return files;
		}

		public String getTorrentName() {
			return TorrentUtils.getLocalisedName(torrent);
		}

		public boolean allFilesMoving() {
			TorrentFileInfo[] files = getFiles();
			for (int j = 0; j < files.length; j++) {
				if (files[j].isLinked()) {
					return false;
				}
			}
			return true;
		}

		public boolean allFilesExist() {
			// check if all selected files exist
			TorrentFileInfo[] files = getFiles();
			for (int i = 0; i < files.length; i++) {
				TorrentFileInfo fileInfo = files[i];
				if (!fileInfo.bDownload)
					continue;

				File file = fileInfo.getDestFileFullName();
				if (!file.exists() || file.length() != fileInfo.lSize) {
					return false;
				}
			}
			return true;
		}

		public void renameDuplicates() {
			if (iStartID == STARTMODE_SEEDING
					|| !COConfigurationManager.getBooleanParameter("DefaultDir.AutoSave.AutoRename")
					|| allFilesExist()) {
				return;
			}

			if (!torrent.isSimpleTorrent()) {
				if (new File(getDataDir()).isDirectory()) {
					File f;
					int idx = 0;
					do {
						idx++;
						f = new File(getDataDir() + "-" + idx);
					} while (f.isDirectory());

					sDestSubDir = f.getName();
				}
			} else {
				// should only be one file
				TorrentFileInfo[] fileInfos = getFiles();
				for (int i = 0; i < fileInfos.length; i++) {
					TorrentFileInfo info = fileInfos[i];

					File file = info.getDestFileFullName();
					int idx = 0;
					while (file.exists()) {
						idx++;
						file = new File(info.getDestPathName(), idx + "-" + info.getDestFileName());
					}

					info.setDestFileName(file.getName());
				}
			}
		}

		/*
		private Boolean has_multiple_small_files = null; 
		private boolean hasMultipleSmallFiles() {
			TorrentFileInfo[] tfi_files = getFiles();
			if (tfi_files.length <= MAX_NODOWNLOAD_COUNT)
				return false;
			
			int small_files_counted = 0;
			for (int i=0; i<tfi_files.length; i++) {
				if (tfi_files[i].lSize < MIN_NODOWNLOAD_SIZE) {
					small_files_counted++;
					if (small_files_counted > MAX_NODOWNLOAD_COUNT) {
						return true;
					}
				}
			}
			
			return false;
		}
		*/

		// Indicates whether all files in this torrent can be deselected
		// (if not, then it occurs on a per-file basis).
		public boolean okToDisableAll() {
			return true;

			/*
			if (iStartID == STARTMODE_SEEDING)
				return true;
			
			// Do we have multiple small files? We'll allow all of them to
			// be disabled if we do.
			if (has_multiple_small_files == null) {
				has_multiple_small_files = new Boolean(hasMultipleSmallFiles());
			}
			
			// You can disable all files if there are lots of small files.
			return has_multiple_small_files.booleanValue();
			*/
		}

	}

	/**
	 * Class to store the file list of a Torrent.  Used to populate table and
	 * store user's choices
	 */
	private class TorrentFileInfo
	{
		/** relative path + full file name as specified by the torrent */
		final String orgFullName;
		
		final String orgFileName;

		long lSize;

		boolean bDownload;

		private String destFileName;
		private String destPathName;

		long iIndex;

		boolean isValid;

		final TorrentInfo parent;

		/**
		 * Init
		 * 
		 * @param parent 
		 * @param torrentFile
		 * @param iIndex
		 */
		public TorrentFileInfo(TorrentInfo parent, TOTorrentFile torrentFile,
				int iIndex) {
			this.parent = parent;
			lSize = torrentFile.getLength();
			this.iIndex = iIndex;
			bDownload = true;
			isValid = true;

			orgFullName = torrentFile.getRelativePath(); // translated to locale
			orgFileName = new File(orgFullName).getName();
		}
		
		public void setFullDestName(String newFullName)
		{
			if(newFullName == null)
			{
				setDestPathName(null);
				setDestFileName(null);
				return;
			}
				
			File newPath = new File(newFullName);
			setDestPathName(newPath.getParent());
			setDestFileName(newPath.getName());
		}
		
		public void setDestPathName(String newPath)
		{
			if(parent.torrent.isSimpleTorrent())
				parent.setParentDir(newPath);
			else
				destPathName = newPath;
		}
		
		public void setDestFileName (String newFileName)
		{
			if(orgFileName.equals(newFileName))
				destFileName = null;
			else
				destFileName = newFileName;			
		}

		public String getDestPathName() {
			if (destPathName != null)
				return destPathName;

			if (parent.torrent.isSimpleTorrent())
				return parent.getParentDir();

			return new File(parent.getDataDir(), orgFullName).getParent();
		}
		
		public String getDestFileName() {
			return destFileName == null ? orgFileName : destFileName;
		}

		public File getDestFileFullName() {
			String path = getDestPathName();
			String file = getDestFileName();
			return new File(path,file);
		}

		public boolean okToDisable() {
			return /* lSize >= MIN_NODOWNLOAD_SIZE	|| */parent.okToDisableAll();
		}
		
		public boolean isLinked()
		{
			return destFileName != null || destPathName != null;
		}
	}

	private String ensureTrailingSeparator(String sPath) {
		if (sPath == null || sPath.length() == 0 || sPath.endsWith(File.separator))
			return sPath;
		return sPath + File.separator;
	}

	/**
	 * 
	 * @return Null if user doesn't want to save silently, or if no path set
	 */
	private static String getSaveSilentlyDir() {
		boolean bUseDefault = COConfigurationManager.getBooleanParameter("Use default data dir");
		if (!bUseDefault)
			return null;

		String sDefDir = "";
		try {
			sDefDir = COConfigurationManager.getDirectoryParameter(PARAM_DEFSAVEPATH);
		} catch (IOException e) {
			return null;
		}

		return (sDefDir == "") ? null : sDefDir;
	}

	private final static class Partition
	{
		public Partition(File root) {
			this.root = root;
		}

		long bytesToConsume = 0;

		long freeSpace = 0;

		final File root;
	}

	private final static class FileStatsCacheItem
	{
		public FileStatsCacheItem(final File f) {
			exists = f.exists();
			if (exists)
				freeSpace = FileUtil.getUsableSpace(f);
			else
				freeSpace = -1;
		}

		boolean exists;

		long freeSpace;
	}

	private long getCachedDirFreeSpace(File directory) {
		FileStatsCacheItem item = (FileStatsCacheItem) fileStatCache.get(directory);
		if (item == null)
			fileStatCache.put(directory, item = new FileStatsCacheItem(directory));
		return item.freeSpace;
	}

	private boolean getCachedExistsStat(File directory) {
		FileStatsCacheItem item = (FileStatsCacheItem) fileStatCache.get(directory);
		if (item == null)
			fileStatCache.put(directory, item = new FileStatsCacheItem(directory));
		return item.exists;
	}

	private final Map fileStatCache = new WeakHashMap(20);

	private final Map parentToRootCache = new WeakHashMap(20);

	private volatile boolean diskFreeInfoRefreshPending = false;

	private volatile boolean diskFreeInfoRefreshRunning = false;

	// @see com.aelitis.azureus.ui.swt.uiupdater.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		return "OpenTorrentWindow";
	}

	// @see com.aelitis.azureus.ui.swt.uiupdater.UIUpdatable#updateUI()
	public void updateUI() {
		if (bClosed) {
			try {
				UIUpdaterSWT.getInstance().removeUpdater(this);
			} catch (Exception e) {
				Debug.out(e);
			}
			return;
		}

		if (diskFreeInfoRefreshPending && !diskFreeInfoRefreshRunning
				&& FileUtil.getUsableSpaceSupported()) {
			diskFreeInfoRefreshRunning = true;
			diskFreeInfoRefreshPending = false;

			final HashSet FSroots = new HashSet(Arrays.asList(File.listRoots()));
			final HashMap partitions = new HashMap();

			for (int i = 0; i < torrentList.size(); i++) {
				TorrentInfo tor = (TorrentInfo) torrentList.get(i);
				TorrentFileInfo[] files = tor.getFiles();
				for (int j = 0; j < files.length; j++) {
					TorrentFileInfo file = files[j];
					if (!file.bDownload)
						continue;

					// reduce each file to its partition root
					File root = file.getDestFileFullName().getAbsoluteFile();

					Partition part = (Partition) partitions.get((File) parentToRootCache.get(root.getParentFile()));

					if (part == null) {
						File next;
						while (true) {
							root = root.getParentFile();
							next = root.getParentFile();
							if (next == null)
								break;

							// bubble up until we hit an existing directory
							if (!getCachedExistsStat(root) || !root.isDirectory())
								continue;

							// check for mount points (different free space) or simple loops in the directory structure
							if (FSroots.contains(root) || root.equals(next)
									|| getCachedDirFreeSpace(next) != getCachedDirFreeSpace(root))
								break;
						}

						parentToRootCache.put(
								file.getDestFileFullName().getAbsoluteFile().getParentFile(), root);

						part = (Partition) partitions.get(root);

						if (part == null) {
							part = new Partition(root);
							part.freeSpace = getCachedDirFreeSpace(root);
							partitions.put(root, part);
						}
					}

					part.bytesToConsume += file.lSize;
				}
			}
			// clear child objects
			Control[] labels = diskspaceComp.getChildren();
			for (int i = 0; i < labels.length; i++)
				labels[i].dispose();

			// build labels
			Iterator it = partitions.values().iterator();
			while (it.hasNext()) {
				Partition part = (Partition) it.next();

				boolean filesTooBig = part.bytesToConsume > part.freeSpace;

				Label l;
				l = new Label(diskspaceComp, SWT.NONE);
				l.setForeground(filesTooBig ? Colors.colorError : null);
				l.setText(part.root.getPath());
				l.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));
				l = new Label(diskspaceComp, SWT.NONE);
				l.setForeground(filesTooBig ? Colors.colorError : null);
				l.setText(MessageText.getString("OpenTorrentWindow.diskUsage",
						new String[] {
							DisplayFormatters.formatByteCountToKiBEtc(part.bytesToConsume),
							DisplayFormatters.formatByteCountToKiBEtc(part.freeSpace)
						}));
				l.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));
			}

			diskspaceComp.update();
			diskspaceComp.getParent().getParent().getParent().layout(true, true);

			diskFreeInfoRefreshRunning = false;
		}

	}

	private void updateSize() {

		/*
		 * determine info for selected torrents only
		 */
		long totalSize = 0;
		long checkedSize = 0;

		for (int i = 0; i < dataFiles.size(); i++) {
			TorrentFileInfo file = (TorrentFileInfo) dataFiles.get(i);

			totalSize += file.lSize;

			if (file.bDownload) {
				checkedSize += file.lSize;
			}
		}

		// build string and set label
		if (totalSize == 0) {
			dataFileTableLabel.setText("");
		} else {
			dataFileTableLabel.setText(MessageText.getString(
					"OpenTorrentWindow.filesInfo", new String[] {
						DisplayFormatters.formatByteCountToKiBEtc(checkedSize),
						DisplayFormatters.formatByteCountToKiBEtc(totalSize)
					}));
		}
		dataFileTableLabel.update();
		dataFileTableLabel.getParent().getParent().layout(true, true);

		diskFreeInfoRefreshPending = true;

	}

	/**
	 * Convenience method for setting the enabled state of a control
	 * <P>This method may be called from any thread</p>
	 * @param control
	 * @param enabledState
	 */
	private void enableControl(final Control control, final boolean enabledState) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (control != null && false == control.isDisposed()) {
					control.setEnabled(enabledState);
				}
			}
		});
	}

	public static void main(String[] args) {
		AzureusCore core = AzureusCoreFactory.create();
		core.start();
		Display display = Display.getDefault();

		Colors.getInstance();

		invoke(null, core.getGlobalManager());
		//OpenTorrentWindow window = new OpenTorrentWindow(null, null, true);
		while (stOpenTorrentWindow != null && !stOpenTorrentWindow.bClosed) {
			if (!display.readAndDispatch())
				display.sleep();
		}

		core.stop();
	}
}
