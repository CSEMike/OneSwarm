/*
 * File    : ConfigPanelFile.java
 * Created : 11 mar. 2004
 * By      : TuxPaper
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

package org.gudy.azureus2.ui.swt.views.configsections;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.components.LinkLabel;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;

public class ConfigSectionFile
	implements UISWTConfigSection
{
	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_ROOT;
	}

	public String configSectionGetName() {
		return ConfigSection.SECTION_FILES;
	}

	public void configSectionSave() {
	}

	public int maxUserMode() {
		return 2;
	}

	public void configSectionDelete() {
		ImageLoader imageLoader = ImageLoader.getInstance();
		imageLoader.releaseImage("openFolderButton");
	}

	public Composite configSectionCreate(final Composite parent) {
		ImageLoader imageLoader = ImageLoader.getInstance();
		Image imgOpenFolder = imageLoader.getImage("openFolderButton");

		GridData gridData;
		Label label;
		String sCurConfigID;
		final ArrayList allConfigIDs = new ArrayList();

		Composite gFile = new Composite(parent, SWT.NULL);

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		gFile.setLayout(layout);

		int userMode = COConfigurationManager.getIntParameter("User Mode");

		// Default Dir Sction
		Group gDefaultDir = new Group(gFile, SWT.NONE);
		Messages.setLanguageText(gDefaultDir,
				"ConfigView.section.file.defaultdir.section");
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 2;
		gDefaultDir.setLayout(layout);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		gDefaultDir.setLayoutData(gridData);

		// Save Path
		sCurConfigID = "Default save path";
		allConfigIDs.add(sCurConfigID);
		Label lblDefaultDir = new Label(gDefaultDir, SWT.NONE);
		Messages.setLanguageText(lblDefaultDir,
				"ConfigView.section.file.defaultdir.ask");
		lblDefaultDir.setLayoutData(new GridData());

		gridData = new GridData(GridData.FILL_HORIZONTAL);
		final StringParameter pathParameter = new StringParameter(gDefaultDir,
				sCurConfigID);
		pathParameter.setLayoutData(gridData);

		Button browse = new Button(gDefaultDir, SWT.PUSH);
		browse.setImage(imgOpenFolder);
		imgOpenFolder.setBackground(browse.getBackground());
		browse.setToolTipText(MessageText.getString("ConfigView.button.browse"));

		browse.addListener(SWT.Selection, new Listener() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
			 */
			public void handleEvent(Event event) {
				DirectoryDialog dialog = new DirectoryDialog(parent.getShell(),
						SWT.APPLICATION_MODAL);
				dialog.setFilterPath(pathParameter.getValue());
				dialog.setMessage(MessageText.getString("ConfigView.dialog.choosedefaultsavepath"));
				dialog.setText(MessageText.getString("ConfigView.section.file.defaultdir.ask"));
				String path = dialog.open();
				if (path != null) {
					pathParameter.setValue(path);
				}
			}
		});

		// def dir: autoSave
		sCurConfigID = "Use default data dir";
		allConfigIDs.add(sCurConfigID);
		BooleanParameter autoSaveToDir = new BooleanParameter(gDefaultDir,
				sCurConfigID, "ConfigView.section.file.defaultdir.auto");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		autoSaveToDir.setLayoutData(gridData);

		if (userMode > 0) {
			// def dir: autoSave -> auto-rename
			sCurConfigID = "DefaultDir.AutoSave.AutoRename";
			allConfigIDs.add(sCurConfigID);
			BooleanParameter autoSaveAutoRename = new BooleanParameter(gDefaultDir,
					sCurConfigID, "ConfigView.section.file.defaultdir.autorename");
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan = 3;
			gridData.horizontalIndent = 20;
			autoSaveAutoRename.setLayoutData(gridData);
			IAdditionalActionPerformer aapDefaultDirStuff3 = new ChangeSelectionActionPerformer(
					autoSaveAutoRename.getControls(), false);
			autoSaveToDir.setAdditionalActionPerformer(aapDefaultDirStuff3);

			// def dir: best guess
			sCurConfigID = "DefaultDir.BestGuess";
			allConfigIDs.add(sCurConfigID);
			BooleanParameter bestGuess = new BooleanParameter(gDefaultDir,
					sCurConfigID, "ConfigView.section.file.defaultdir.bestguess");
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan = 3;
			bestGuess.setLayoutData(gridData);

			IAdditionalActionPerformer aapDefaultDirStuff = new ChangeSelectionActionPerformer(
					bestGuess.getControls(), true);
			autoSaveToDir.setAdditionalActionPerformer(aapDefaultDirStuff);

			// def dir: auto update
			sCurConfigID = "DefaultDir.AutoUpdate";
			allConfigIDs.add(sCurConfigID);
			BooleanParameter autoUpdateSaveDir = new BooleanParameter(gDefaultDir,
					sCurConfigID, "ConfigView.section.file.defaultdir.lastused");
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan = 3;
			autoUpdateSaveDir.setLayoutData(gridData);

			IAdditionalActionPerformer aapDefaultDirStuff2 = new ChangeSelectionActionPerformer(
					autoUpdateSaveDir.getControls(), true);
			autoSaveToDir.setAdditionalActionPerformer(aapDefaultDirStuff2);
		}

		new Label(gFile, SWT.NONE);

		////////////////////

		sCurConfigID = "XFS Allocation";
		allConfigIDs.add(sCurConfigID);
		if (userMode > 0 && !Constants.isWindows) {
			BooleanParameter xfsAllocation = new BooleanParameter(gFile,
					sCurConfigID, "ConfigView.label.xfs.allocation");
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			xfsAllocation.setLayoutData(gridData);
		}

		BooleanParameter zeroNew = null;

		sCurConfigID = "Zero New";
		allConfigIDs.add(sCurConfigID);
		if (userMode > 0) {
			// zero new files
			zeroNew = new BooleanParameter(gFile, sCurConfigID,
					"ConfigView.label.zeronewfiles");
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			zeroNew.setLayoutData(gridData);
		}

		BooleanParameter pieceReorder = null;

		sCurConfigID = "Enable reorder storage mode";
		allConfigIDs.add(sCurConfigID);
		if (userMode > 0) {

			pieceReorder = new BooleanParameter(gFile, sCurConfigID,
					"ConfigView.label.piecereorder");
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			pieceReorder.setLayoutData(gridData);

			//Make the reorder checkbox (button) deselect when zero new is used
			Button[] btnReorder = {
				(Button) pieceReorder.getControl()
			};
			zeroNew.setAdditionalActionPerformer(new ExclusiveSelectionActionPerformer(
					btnReorder));

			//Make the zero new checkbox(button) deselct when reorder is used
			Button[] btnZeroNew = {
				(Button) zeroNew.getControl()
			};
			pieceReorder.setAdditionalActionPerformer(new ExclusiveSelectionActionPerformer(
					btnZeroNew));
		}

		sCurConfigID = "Reorder storage mode min MB";
		allConfigIDs.add(sCurConfigID);

		if (userMode > 0) {
			Label lblMinMB = new Label(gFile, SWT.NULL);
			Messages.setLanguageText(lblMinMB, "ConfigView.label.piecereorderminmb");
			gridData = new GridData();
			gridData.horizontalIndent = 25;
			lblMinMB.setLayoutData(gridData);

			IntParameter minMB = new IntParameter(gFile, sCurConfigID);
			gridData = new GridData();
			minMB.setLayoutData(gridData);

			pieceReorder.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
					lblMinMB));
			pieceReorder.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
					minMB));
		}

		sCurConfigID = "Enable incremental file creation";
		allConfigIDs.add(sCurConfigID);
		if (userMode > 0) {
			// incremental file creation
			BooleanParameter incremental = new BooleanParameter(gFile, sCurConfigID,
					"ConfigView.label.incrementalfile");
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			incremental.setLayoutData(gridData);

			//Make the incremental checkbox (button) deselect when zero new is used
			Button[] btnIncremental = {
				(Button) incremental.getControl()
			};
			zeroNew.setAdditionalActionPerformer(new ExclusiveSelectionActionPerformer(
					btnIncremental));

			//Make the zero new checkbox(button) deselct when incremental is used
			Button[] btnZeroNew = {
				(Button) zeroNew.getControl()
			};
			incremental.setAdditionalActionPerformer(new ExclusiveSelectionActionPerformer(
					btnZeroNew));
		}

		sCurConfigID = "File.truncate.if.too.large";
		allConfigIDs.add(sCurConfigID);
		if (userMode > 0) {
			// truncate too large
			BooleanParameter truncateLarge = new BooleanParameter(gFile,
					sCurConfigID, "ConfigView.section.file.truncate.too.large");
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			truncateLarge.setLayoutData(gridData);
		}

		sCurConfigID = "Check Pieces on Completion";
		allConfigIDs.add(sCurConfigID);
		if (userMode > 0) {
			// check on complete
			BooleanParameter checkOnComp = new BooleanParameter(gFile, sCurConfigID,
					"ConfigView.label.checkOncompletion");
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			checkOnComp.setLayoutData(gridData);
		}

		sCurConfigID = "Seeding Piece Check Recheck Enable";
		allConfigIDs.add(sCurConfigID);
		if (userMode > 0) {
			// check on complete
			BooleanParameter checkOnSeeding = new BooleanParameter(gFile,
					sCurConfigID, "ConfigView.label.checkOnSeeding");
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			checkOnSeeding.setLayoutData(gridData);
		}

		sCurConfigID = "File.strict.locking";
		allConfigIDs.add(sCurConfigID);
		if (userMode > 1) {

			BooleanParameter strictLocking = new BooleanParameter(gFile,
					sCurConfigID, "ConfigView.label.strictfilelocking");
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			strictLocking.setLayoutData(gridData);
		}

		if (userMode == 0) {
			allConfigIDs.add("Use Resume");
			sCurConfigID = "Save Resume Interval";
			allConfigIDs.add(sCurConfigID);
			sCurConfigID = "On Resume Recheck All";
			allConfigIDs.add(sCurConfigID);
			sCurConfigID = "File.save.peers.enable";
			allConfigIDs.add(sCurConfigID);
			sCurConfigID = "File.save.peers.max";
			allConfigIDs.add(sCurConfigID);
		} else {
			sCurConfigID = "Use Resume";
			allConfigIDs.add(sCurConfigID);
			// resume data
			final BooleanParameter bpUseResume = new BooleanParameter(gFile,
					sCurConfigID, "ConfigView.label.usefastresume");
			bpUseResume.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

			Composite cResumeGroup = new Composite(gFile, SWT.NULL);
			layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 4;
			layout.numColumns = 3;
			cResumeGroup.setLayout(layout);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalIndent = 25;
			gridData.horizontalSpan = 2;
			cResumeGroup.setLayoutData(gridData);

			sCurConfigID = "Save Resume Interval";
			allConfigIDs.add(sCurConfigID);
			Label lblSaveResumeInterval = new Label(cResumeGroup, SWT.NULL);
			Messages.setLanguageText(lblSaveResumeInterval,
					"ConfigView.label.saveresumeinterval");

			IntParameter paramSaveInterval = new IntParameter(cResumeGroup,
					sCurConfigID);
			gridData = new GridData();
			paramSaveInterval.setLayoutData(gridData);

			Label lblMinutes = new Label(cResumeGroup, SWT.NULL);
			Messages.setLanguageText(lblMinutes, "ConfigView.text.minutes");

			// save peers

			sCurConfigID = "On Resume Recheck All";
			allConfigIDs.add(sCurConfigID);
			final BooleanParameter recheck_all = new BooleanParameter(cResumeGroup,
					sCurConfigID, "ConfigView.section.file.resume.recheck.all");
			gridData = new GridData();
			gridData.horizontalSpan = 3;
			recheck_all.setLayoutData(gridData);
			// save peers

			sCurConfigID = "File.save.peers.enable";
			allConfigIDs.add(sCurConfigID);
			final BooleanParameter save_peers = new BooleanParameter(cResumeGroup,
					sCurConfigID, "ConfigView.section.file.save.peers.enable");
			gridData = new GridData();
			gridData.horizontalSpan = 3;
			save_peers.setLayoutData(gridData);

			// save peers max

			sCurConfigID = "File.save.peers.max";
			allConfigIDs.add(sCurConfigID);
			final Label lblSavePeersMax = new Label(cResumeGroup, SWT.NULL);
			Messages.setLanguageText(lblSavePeersMax,
					"ConfigView.section.file.save.peers.max");
			final IntParameter savePeersMax = new IntParameter(cResumeGroup,
					sCurConfigID);
			gridData = new GridData();
			savePeersMax.setLayoutData(gridData);
			final Label lblPerTorrent = new Label(cResumeGroup, SWT.NULL);
			Messages.setLanguageText(lblPerTorrent,
					"ConfigView.section.file.save.peers.pertorrent");

			final Control[] controls = {
				cResumeGroup
			};

			/*
			IAdditionalActionPerformer performer = new ChangeSelectionActionPerformer(controls);
			bpUseResume.setAdditionalActionPerformer(performer);
			*/

			IAdditionalActionPerformer f_enabler = new GenericActionPerformer(
					controls) {
				public void performAction() {
					controlsSetEnabled(controls, bpUseResume.isSelected());

					if (bpUseResume.isSelected()) {
						lblSavePeersMax.setEnabled(save_peers.isSelected());
						savePeersMax.getControl().setEnabled(save_peers.isSelected());
						lblPerTorrent.setEnabled(save_peers.isSelected());
					}
				}
			};

			bpUseResume.setAdditionalActionPerformer(f_enabler);
			save_peers.setAdditionalActionPerformer(f_enabler);

		} //end usermode>0

		if (userMode > 0) {
			sCurConfigID = "priorityExtensions";
			allConfigIDs.add(sCurConfigID);

			// Auto-Prioritize
			label = new Label(gFile, SWT.WRAP);
			gridData = new GridData();
			gridData.widthHint = 180;
			label.setLayoutData(gridData);
			Messages.setLanguageText(label, "ConfigView.label.priorityExtensions");

			Composite cExtensions = new Composite(gFile, SWT.NULL);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			cExtensions.setLayoutData(gridData);
			layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			layout.numColumns = 3;
			cExtensions.setLayout(layout);

			gridData = new GridData(GridData.FILL_HORIZONTAL);
			new StringParameter(cExtensions, sCurConfigID).setLayoutData(gridData);

			sCurConfigID = "priorityExtensionsIgnoreCase";
			allConfigIDs.add(sCurConfigID);
			new BooleanParameter(cExtensions, sCurConfigID,
					"ConfigView.label.ignoreCase");
		} else {
			sCurConfigID = "priorityExtensions";
			allConfigIDs.add(sCurConfigID);
			sCurConfigID = "priorityExtensionsIgnoreCase";
			allConfigIDs.add(sCurConfigID);
		}

		// rename incomplete

		if (userMode > 0) {
			sCurConfigID = "Rename Incomplete Files";
			allConfigIDs.add(sCurConfigID);

			gridData = new GridData();
			gridData.horizontalSpan = 1;
			BooleanParameter rename_incomplete = new BooleanParameter(gFile,
					sCurConfigID, "ConfigView.section.file.rename.incomplete");
			rename_incomplete.setLayoutData(gridData);

			sCurConfigID = "Rename Incomplete Files Extension";
			allConfigIDs.add(sCurConfigID);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			StringParameter rename_incomplete_ext = new StringParameter(gFile,
					sCurConfigID);
			rename_incomplete_ext.setLayoutData(gridData);

			IAdditionalActionPerformer incompFileAP = new ChangeSelectionActionPerformer(
					rename_incomplete_ext.getControls(), false);
			rename_incomplete.setAdditionalActionPerformer(incompFileAP);

			Label lIgnoreFiles = new Label(gFile, SWT.NULL);
			Messages.setLanguageText(lIgnoreFiles,
					"ConfigView.section.file.torrent.ignorefiles");

			gridData = new GridData(GridData.FILL_HORIZONTAL);
			new StringParameter(gFile, "File.Torrent.IgnoreFiles",
					TOTorrent.DEFAULT_IGNORE_FILES).setLayoutData(gridData);

		}

		Group gDeletion = new Group(gFile, SWT.NONE);
		Messages.setLanguageText(gDeletion,
				"ConfigView.section.file.deletion.section");
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 5;
		gDeletion.setLayout(layout);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		gDeletion.setLayoutData(gridData);
		
		if (userMode > 0) {
  		Composite c = new Composite(gDeletion, SWT.NONE);
  		layout = new GridLayout();
  		layout.numColumns = 2;
  		layout.marginHeight = 0;
  		layout.marginWidth = 0;
  		c.setLayout(layout);
  		gridData = new GridData(GridData.FILL_HORIZONTAL);
  		gridData.horizontalSpan = 2;
  		c.setLayoutData(gridData);
  		
  		sCurConfigID = "tb.confirm.delete.content";
  		label = new Label(c, SWT.NULL);
  		Messages.setLanguageText(label, "ConfigView.section.file.tb.delete");
  		int[] values = {
  			0,
  			1,
  			2,
  		};
  		String[] labels = {
  			MessageText.getString("ConfigView.tb.delete.ask"),
  			MessageText.getString("ConfigView.tb.delete.content"),
  			MessageText.getString("ConfigView.tb.delete.torrent"),
  		};
  		new IntListParameter(c, sCurConfigID, labels, values);

  		
  		sCurConfigID = "def.deletetorrent";
  		allConfigIDs.add(sCurConfigID);
  		gridData = new GridData();
  		gridData.horizontalSpan = 2;
  		new BooleanParameter(gDeletion, sCurConfigID, "ConfigView.section.file.delete.torrent").setLayoutData(gridData);
		}

		
		try {
			final PlatformManager platform = PlatformManagerFactory.getPlatformManager();

			if (platform.hasCapability(PlatformManagerCapabilities.RecoverableFileDelete)) {
				sCurConfigID = "Move Deleted Data To Recycle Bin";
				allConfigIDs.add(sCurConfigID);

				gridData = new GridData();
				gridData.horizontalSpan = 2;
				new BooleanParameter(gDeletion, sCurConfigID,
						"ConfigView.section.file.nativedelete").setLayoutData(gridData);

			}
		} catch (Throwable e) {

		}
		
		if (userMode > 0) {
			sCurConfigID = "File.delete.include_files_outside_save_dir";
			allConfigIDs.add(sCurConfigID);

			gridData = new GridData();
			gridData.horizontalSpan = 2;
			new BooleanParameter(gDeletion, sCurConfigID,
					"ConfigView.section.file.delete.include_files_outside_save_dir").setLayoutData(gridData);
		}

		if (userMode > 0) {
			Group gConfigSettings = new Group(gFile, SWT.NONE);
			Messages.setLanguageText(gConfigSettings,
					"ConfigView.section.file.config.section");
			layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginHeight = 5;
			gConfigSettings.setLayout(layout);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan = 2;
			gConfigSettings.setLayoutData(gridData);

			// Configuration directory information.
			Label config_label = new Label(gConfigSettings, SWT.NULL);
			Messages.setLanguageText(config_label,
					"ConfigView.section.file.config.currentdir");
			config_label.setLayoutData(new GridData());
			Label config_link = new Label(gConfigSettings, SWT.NULL);
			config_link.setText(SystemProperties.getUserPath());
			config_link.setLayoutData(new GridData());
			LinkLabel.makeLinkedLabel(config_link, SystemProperties.getUserPath());

			sCurConfigID = "Use Config File Backups";
			allConfigIDs.add(sCurConfigID);

			// check on complete
			BooleanParameter backupConfig = new BooleanParameter(gConfigSettings,
					sCurConfigID, "ConfigView.label.backupconfigfiles");
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			backupConfig.setLayoutData(gridData);
		}
		/*
		    Button buttonReset = new Button(gFile, SWT.PUSH);
		    Messages.setLanguageText(buttonReset, "Button.reset");
		    gridData = new GridData(GridData.FILL_VERTICAL | GridData.VERTICAL_ALIGN_END);
		  	gridData.horizontalSpan = 2;
		  	buttonReset.setLayoutData(gridData);
		  	buttonReset.addSelectionListener(new SelectionAdapter() {
		  		public void widgetSelected(SelectionEvent e) {
		  			for (Iterator iter = allConfigIDs.iterator(); iter.hasNext();) {
							String sConfigID = (String) iter.next();
							COConfigurationManager.removeParameter(sConfigID);
						}
		  		}
		  	});
		*/
		return gFile;
	}
}
