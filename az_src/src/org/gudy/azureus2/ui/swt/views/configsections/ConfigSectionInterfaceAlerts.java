/*
 * File    : ConfigSectionInterfaceAlerts.java
 * Created : Dec 4, 2006
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

import java.applet.Applet;
import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.shells.MessageSlideShell;

import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

public class ConfigSectionInterfaceAlerts implements UISWTConfigSection
{
	private final static String INTERFACE_PREFIX = "ConfigView.section.interface.";

	private final static String LBLKEY_PREFIX = "ConfigView.label.";

	private final static String STYLE_PREFIX = "ConfigView.section.style.";

	private final int REQUIRED_MODE = 0;

	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_INTERFACE;
	}

	/* Name of section will be pulled from 
	 * ConfigView.section.<i>configSectionGetName()</i>
	 */
	public String configSectionGetName() {
		return "interface.alerts";
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {
		ImageLoader imageLoader = ImageLoader.getInstance();
		imageLoader.releaseImage("openFolderButton");
	}
	
	public int maxUserMode() {
		return REQUIRED_MODE;
	}


	public Composite configSectionCreate(final Composite parent) {
		Image imgOpenFolder = null;
		if (!Constants.isOSX) {
			ImageLoader imageLoader = ImageLoader.getInstance();
			imgOpenFolder = imageLoader.getImage("openFolderButton");			
		}

		GridData gridData;
		GridLayout layout;

		Composite cSection = new Composite(parent, SWT.NULL);
		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
				| GridData.HORIZONTAL_ALIGN_FILL);
		cSection.setLayoutData(gridData);
		layout = new GridLayout();
		layout.marginWidth = 0;
		//layout.numColumns = 2;
		cSection.setLayout(layout);

		int userMode = COConfigurationManager.getIntParameter("User Mode");
		if (userMode < REQUIRED_MODE) {
			Label label = new Label(cSection, SWT.WRAP);
			gridData = new GridData();
			label.setLayoutData(gridData);

			final String[] modeKeys = {
				"ConfigView.section.mode.beginner",
				"ConfigView.section.mode.intermediate",
				"ConfigView.section.mode.advanced"
			};

			String param1, param2;
			if (REQUIRED_MODE < modeKeys.length)
				param1 = MessageText.getString(modeKeys[REQUIRED_MODE]);
			else
				param1 = String.valueOf(REQUIRED_MODE);

			if (userMode < modeKeys.length)
				param2 = MessageText.getString(modeKeys[userMode]);
			else
				param2 = String.valueOf(userMode);

			label.setText(MessageText.getString("ConfigView.notAvailableForMode",
					new String[] {
						param1,
						param2
					}));

			return cSection;
		}

		Composite cArea = new Composite(cSection, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 4;
		cArea.setLayout(layout);
		cArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		BooleanParameter d_play_sound = new BooleanParameter(cArea,
				"Play Download Finished", LBLKEY_PREFIX + "playdownloadfinished");

		// OS X counterpart for alerts (see below for what is disabled)
		if (Constants.isOSX) {
			// download info 

			gridData = new GridData();
			gridData.horizontalSpan = 3;
			gridData.widthHint = 0;
			gridData.heightHint = 0;
			Composite d_filler = new Composite(cArea, SWT.NONE);
			d_filler.setSize(0, 0);
			d_filler.setLayoutData(gridData);

			final BooleanParameter d_speechEnabledParameter = new BooleanParameter(
					cArea, "Play Download Finished Announcement", LBLKEY_PREFIX
							+ "playdownloadspeech");

			final StringParameter d_speechParameter = new StringParameter(cArea,
					"Play Download Finished Announcement Text");
			gridData = new GridData();
			gridData.horizontalSpan = 3;
			gridData.widthHint = 150;
			d_speechParameter.setLayoutData(gridData);
			((Text) d_speechParameter.getControl()).setTextLimit(40);

			d_speechEnabledParameter.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
					d_speechParameter.getControls()));

			final Label d_speechInfo = new Label(cArea, SWT.NONE);
			gridData = new GridData();
			gridData.horizontalSpan = 4;
			gridData.horizontalIndent = 24;
			d_speechInfo.setLayoutData(gridData);

			Messages.setLanguageText(d_speechInfo, LBLKEY_PREFIX
					+ "playdownloadspeech.info");
		}

		//Option disabled on OS X, as impossible to make it work correctly
		if (!Constants.isOSX) {

			// download info

			gridData = new GridData(GridData.FILL_HORIZONTAL);

			final StringParameter d_pathParameter = new StringParameter(cArea,
					"Play Download Finished File", "");

			if (d_pathParameter.getValue().length() == 0) {

				d_pathParameter.setValue("<default>");
			}

			d_pathParameter.setLayoutData(gridData);

			Button d_browse = new Button(cArea, SWT.PUSH);

			d_browse.setImage(imgOpenFolder);

			imgOpenFolder.setBackground(d_browse.getBackground());

			d_browse.setToolTipText(MessageText.getString("ConfigView.button.browse"));

			d_browse.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					FileDialog dialog = new FileDialog(parent.getShell(),
							SWT.APPLICATION_MODAL);
					dialog.setFilterExtensions(new String[] { "*.wav"
					});
					dialog.setFilterNames(new String[] { "*.wav"
					});

					dialog.setText(MessageText.getString(INTERFACE_PREFIX + "wavlocation"));

					final String path = dialog.open();

					if (path != null) {

						d_pathParameter.setValue(path);

						new AEThread("SoundTest") {
							public void runSupport() {
								try {
									Applet.newAudioClip(new File(path).toURL()).play();

									Thread.sleep(2500);

								} catch (Throwable e) {

								}
							}
						}.start();
					}
				}
			});

			Label d_sound_info = new Label(cArea, SWT.WRAP);
			Messages.setLanguageText(d_sound_info, INTERFACE_PREFIX
					+ "wavlocation.info");
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.widthHint = 100;
			d_sound_info.setLayoutData(gridData);

			d_play_sound.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
					d_pathParameter.getControls()));
			d_play_sound.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
					new Control[] {
						d_browse,
						d_sound_info
					}));

			// 
		}

		BooleanParameter f_play_sound = new BooleanParameter(cArea,
				"Play File Finished", LBLKEY_PREFIX + "playfilefinished");

		// OS X counterpart for alerts (see below for what is disabled)

		if (Constants.isOSX) {

			// per-file info

			gridData = new GridData();
			gridData.horizontalSpan = 3;
			gridData.widthHint = 0;
			gridData.heightHint = 0;
			Composite f_filler = new Composite(cArea, SWT.NONE);
			f_filler.setSize(0, 0);
			f_filler.setLayoutData(gridData);

			final BooleanParameter f_speechEnabledParameter = new BooleanParameter(
					cArea, "Play File Finished Announcement", LBLKEY_PREFIX
							+ "playfilespeech");

			final StringParameter f_speechParameter = new StringParameter(cArea,
					"Play File Finished Announcement Text");
			gridData = new GridData();
			gridData.horizontalSpan = 3;
			gridData.widthHint = 150;
			f_speechParameter.setLayoutData(gridData);
			((Text) f_speechParameter.getControl()).setTextLimit(40);

			f_speechEnabledParameter.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
					f_speechParameter.getControls()));

			final Label speechInfo = new Label(cArea, SWT.NONE);
			gridData = new GridData();
			gridData.horizontalSpan = 4;
			gridData.horizontalIndent = 24;
			speechInfo.setLayoutData(gridData);

			Messages.setLanguageText(speechInfo, LBLKEY_PREFIX
					+ "playfilespeech.info");
		}

		//Option disabled on OS X, as impossible to make it work correctly
		if (!Constants.isOSX) {

			// file info

			gridData = new GridData(GridData.FILL_HORIZONTAL);

			final StringParameter f_pathParameter = new StringParameter(cArea,
					"Play File Finished File", "");

			if (f_pathParameter.getValue().length() == 0) {

				f_pathParameter.setValue("<default>");
			}

			f_pathParameter.setLayoutData(gridData);

			Button f_browse = new Button(cArea, SWT.PUSH);

			f_browse.setImage(imgOpenFolder);

			imgOpenFolder.setBackground(f_browse.getBackground());

			f_browse.setToolTipText(MessageText.getString("ConfigView.button.browse"));

			f_browse.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					FileDialog dialog = new FileDialog(parent.getShell(),
							SWT.APPLICATION_MODAL);
					dialog.setFilterExtensions(new String[] { "*.wav"
					});
					dialog.setFilterNames(new String[] { "*.wav"
					});

					dialog.setText(MessageText.getString(INTERFACE_PREFIX + "wavlocation"));

					final String path = dialog.open();

					if (path != null) {

						f_pathParameter.setValue(path);

						new AEThread("SoundTest") {
							public void runSupport() {
								try {
									Applet.newAudioClip(new File(path).toURL()).play();

									Thread.sleep(2500);

								} catch (Throwable e) {

								}
							}
						}.start();
					}
				}
			});

			Label f_sound_info = new Label(cArea, SWT.WRAP);
			Messages.setLanguageText(f_sound_info, INTERFACE_PREFIX
					+ "wavlocation.info");
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.widthHint = 100;
			f_sound_info.setLayoutData(gridData);

			f_play_sound.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
					f_pathParameter.getControls()));
			f_play_sound.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
					new Control[] {
						f_browse,
						f_sound_info
					}));
		}

		cArea = new Composite(cSection, SWT.NULL);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		cArea.setLayout(layout);
		cArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		BooleanParameter popup_dl_added = new BooleanParameter(cArea,
				"Popup Download Added", LBLKEY_PREFIX + "popupdownloadadded");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		popup_dl_added.setLayoutData(gridData);
		
		BooleanParameter popup_dl_completed = new BooleanParameter(cArea,
				"Popup Download Finished", LBLKEY_PREFIX + "popupdownloadfinished");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		popup_dl_completed.setLayoutData(gridData);

		BooleanParameter popup_file_completed = new BooleanParameter(cArea,
				"Popup File Finished", LBLKEY_PREFIX + "popupfilefinished");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		popup_file_completed.setLayoutData(gridData);

		BooleanParameter disable_sliding = new BooleanParameter(cArea,
				"GUI_SWT_DisableAlertSliding", STYLE_PREFIX + "disableAlertSliding");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		disable_sliding.setLayoutData(gridData);

		// Timestamps for popup alerts.
		BooleanParameter show_alert_timestamps = new BooleanParameter(cArea,
				"Show Timestamp For Alerts", LBLKEY_PREFIX + "popup.timestamp");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		show_alert_timestamps.setLayoutData(gridData);

		// Auto-hide popup setting.
		Label label = new Label(cArea, SWT.WRAP);
		Messages.setLanguageText(label, LBLKEY_PREFIX + "popup.autohide");
		label.setLayoutData(new GridData());
		IntParameter auto_hide_alert = new IntParameter(cArea,
				"Message Popup Autoclose in Seconds", 0, 86400);
		gridData = new GridData();
		gridData.horizontalSpan = 1;
		auto_hide_alert.setLayoutData(gridData);
		
		return cSection;
	}
}
