/*
 * File    : ConfigPanel*.java
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.impl.StringListImpl;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.plugins.platform.PlatformManagerException;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.LinkLabel;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.core3.util.TrackersUtil;

import java.util.HashMap;

public class ConfigSectionInterface implements UISWTConfigSection {
	private final static String KEY_PREFIX = "ConfigView.section.interface.";

	private final static String LBLKEY_PREFIX = "ConfigView.label.";

	private ParameterListener decisions_parameter_listener;

	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_ROOT;
	}

	public String configSectionGetName() {
		return ConfigSection.SECTION_INTERFACE;
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {

		if (decisions_parameter_listener != null) {

			COConfigurationManager.removeParameterListener(
					"MessageBoxWindow.decisions", decisions_parameter_listener);
		}
	}
	
	public int maxUserMode() {
		return 0;
	}


	public Composite configSectionCreate(final Composite parent) {
		GridData gridData;
		GridLayout layout;
		Label label;

		Composite cDisplay = new Composite(parent, SWT.NULL);

		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
				| GridData.HORIZONTAL_ALIGN_FILL);
		cDisplay.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		cDisplay.setLayout(layout);

		final PlatformManager platform = PlatformManagerFactory.getPlatformManager();
		
			// ***** auto open group
		
		Group gAutoOpen = new Group(cDisplay, SWT.NULL);
		Messages.setLanguageText(gAutoOpen, LBLKEY_PREFIX + "autoopen");
		layout = new GridLayout(3, false);
		gAutoOpen.setLayout(layout);
		gAutoOpen.setLayoutData(new GridData( GridData.FILL_HORIZONTAL ));


		label = new Label(gAutoOpen, SWT.NULL);
		Messages.setLanguageText(label, LBLKEY_PREFIX + "autoopen.detailstab");
		new BooleanParameter(gAutoOpen, "Open Details", LBLKEY_PREFIX
				+ "autoopen.dl");
		new BooleanParameter(gAutoOpen, "Open Seeding Details", LBLKEY_PREFIX
				+ "autoopen.cd");


		label = new Label(gAutoOpen, SWT.NULL);
		Messages.setLanguageText(label, LBLKEY_PREFIX + "autoopen.downloadbars");
		new BooleanParameter(gAutoOpen, "Open Bar Incomplete", LBLKEY_PREFIX + "autoopen.dl");
		new BooleanParameter(gAutoOpen, "Open Bar Complete", LBLKEY_PREFIX + "autoopen.cd");
		
			// **** 
		
		new BooleanParameter(cDisplay, "Remember transfer bar location", LBLKEY_PREFIX + "transferbar.remember_location");

		Group gSysTray = new Group(cDisplay, SWT.NULL);
		Messages.setLanguageText(gSysTray, LBLKEY_PREFIX + "systray");
		layout = new GridLayout();
		gSysTray.setLayout(layout);
		gSysTray.setLayoutData(new GridData( GridData.FILL_HORIZONTAL ));

		BooleanParameter est = new BooleanParameter(gSysTray, "Enable System Tray",
				KEY_PREFIX + "enabletray");

		BooleanParameter ctt = new BooleanParameter(gSysTray, "Close To Tray",
				LBLKEY_PREFIX + "closetotray");
		BooleanParameter mtt = new BooleanParameter(gSysTray, "Minimize To Tray",
				LBLKEY_PREFIX + "minimizetotray");
		BooleanParameter esttt = new BooleanParameter(gSysTray, "ui.systray.tooltip.enable",
				"ConfigView.label.enableSystrayToolTip");

		est.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
				ctt.getControls()));
		est.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
				mtt.getControls()));
		est.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
				esttt.getControls()));
		
        /**
         * Default download / upload limits available in the UI.
         */
        Group limit_group = new Group(cDisplay, SWT.NULL);
        Messages.setLanguageText(limit_group, LBLKEY_PREFIX + "set_ui_transfer_speeds");
        layout = new GridLayout();
        layout.numColumns = 2;
        limit_group.setLayout(layout);
        limit_group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        Label limit_group_label = new Label(limit_group, SWT.WRAP);
        limit_group_label.setLayoutData(Utils.getWrappableLabelGridData(2, GridData.GRAB_HORIZONTAL));
        Messages.setLanguageText(limit_group_label, LBLKEY_PREFIX + "set_ui_transfer_speeds.description");
        
        String[] limit_types = new String[] {"download", "upload"};
        final String limit_type_prefix = "config.ui.speed.partitions.manual.";
        for (int i=0; i<limit_types.length; i++) {
        	final BooleanParameter bp = new BooleanParameter(limit_group, limit_type_prefix + limit_types[i] + ".enabled", false, LBLKEY_PREFIX + "set_ui_transfer_speeds.description." + limit_types[i]);
        	final StringParameter sp = new StringParameter(limit_group, limit_type_prefix + limit_types[i] + ".values", "");
        	IAdditionalActionPerformer iaap = new GenericActionPerformer(new Control[] {}) {
        		public void performAction() {
        			sp.getControl().setEnabled(bp.isSelected());	
        		}
        	};
        	
            gridData = new GridData();
            gridData.widthHint = 150;
            sp.setLayoutData(gridData);
        	iaap.performAction();
        	bp.setAdditionalActionPerformer(iaap);
        }

		new BooleanParameter(cDisplay, "Send Version Info", LBLKEY_PREFIX
				+ "allowSendVersion");

		Composite cArea = new Composite(cDisplay, SWT.NULL);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		cArea.setLayout(layout);
		cArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		new LinkLabel(cArea, LBLKEY_PREFIX + "version.info.link",
				"http://wiki.vuze.com/w/Version.azureusplatform.com");

		if (!Constants.isOSX) {

			BooleanParameter confirm = new BooleanParameter(cArea,
					"confirmationOnExit",
					"ConfigView.section.style.confirmationOnExit");
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			confirm.setLayoutData(gridData);
		}
		
		cArea = new Composite(cDisplay, SWT.NULL);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		cArea.setLayout(layout);
		cArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// clear remembered decisions

		final Label clear_label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(clear_label, KEY_PREFIX + "cleardecisions");

		final Button clear_decisions = new Button(cArea, SWT.PUSH);
		Messages.setLanguageText(clear_decisions, KEY_PREFIX
				+ "cleardecisionsbutton");

		clear_decisions.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {

				COConfigurationManager.setParameter("MessageBoxWindow.decisions",
						new HashMap());
			}
		});

		final Label clear_tracker_label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(clear_tracker_label, KEY_PREFIX + "cleartrackers");

		final Button clear_tracker_button = new Button(cArea, SWT.PUSH);
		Messages.setLanguageText(clear_tracker_button, KEY_PREFIX
				+ "cleartrackersbutton");
		
		clear_tracker_button.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				TrackersUtil.getInstance().clearAllTrackers(true);
			}
		});
		
		final Label clear_save_path_label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(clear_save_path_label, KEY_PREFIX + "clearsavepaths");

		final Button clear_save_path_button = new Button(cArea, SWT.PUSH);
		Messages.setLanguageText(clear_save_path_button, KEY_PREFIX
				+ "clearsavepathsbutton");
		
		clear_save_path_button.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				COConfigurationManager.setParameter("saveTo_list", new StringListImpl());
			}
		});
		
		decisions_parameter_listener = new ParameterListener() {
			public void parameterChanged(String parameterName) {
				if (clear_decisions.isDisposed()) {

					// tidy up from previous incarnations

					COConfigurationManager.removeParameterListener(
							"MessageBoxWindow.decisions", this);

				} else {

					boolean enabled = COConfigurationManager.getMapParameter(
							"MessageBoxWindow.decisions", new HashMap()).size() > 0;

					clear_label.setEnabled(enabled);
					clear_decisions.setEnabled(enabled);
				}
			}
		};

		decisions_parameter_listener.parameterChanged(null);

		COConfigurationManager.addParameterListener("MessageBoxWindow.decisions",
				decisions_parameter_listener);

		// drag-drop
		/* gone to comply with whateverooo
		label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(label, "ConfigView.section.style.dropdiraction");

		String[] drop_options = {
				"ConfigView.section.style.dropdiraction.opentorrents",
				"ConfigView.section.style.dropdiraction.sharefolder",
				"ConfigView.section.style.dropdiraction.sharefoldercontents",
				"ConfigView.section.style.dropdiraction.sharefoldercontentsrecursive", };

		String dropLabels[] = new String[drop_options.length];
		String dropValues[] = new String[drop_options.length];
		for (int i = 0; i < drop_options.length; i++) {

			dropLabels[i] = MessageText.getString(drop_options[i]);
			dropValues[i] = "" + i;
		}
		new StringListParameter(cArea, "config.style.dropdiraction",
				dropLabels, dropValues);
		*/
		
		// reset associations

		if (platform.hasCapability(PlatformManagerCapabilities.RegisterFileAssociations)) {

			Composite cResetAssoc = new Composite(cArea, SWT.NULL);
			layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			layout.numColumns = 2;
			cResetAssoc.setLayout(layout);
			cResetAssoc.setLayoutData(new GridData());

			label = new Label(cResetAssoc, SWT.NULL);
			Messages.setLanguageText(label, KEY_PREFIX + "resetassoc");

			Button reset = new Button(cResetAssoc, SWT.PUSH);
			Messages.setLanguageText(reset, KEY_PREFIX + "resetassocbutton"); //$NON-NLS-1$

			reset.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {

					try {
						platform.registerApplication();

					} catch (PlatformManagerException e) {

						Logger.log(new LogAlert(LogAlert.UNREPEATABLE,
								"Failed to register application", e));
					}
				}
			});

			new BooleanParameter(cArea, "config.interface.checkassoc",
					KEY_PREFIX + "checkassoc");

		}

		return cDisplay;
	}

}
