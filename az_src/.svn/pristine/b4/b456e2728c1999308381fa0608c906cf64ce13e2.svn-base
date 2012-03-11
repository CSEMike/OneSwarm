/*
 * File    : ConfigSection*.java
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.LocaleUtil;
import org.gudy.azureus2.core3.internat.LocaleUtilDecoder;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.BooleanParameter;
import org.gudy.azureus2.ui.swt.config.StringListParameter;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

public class ConfigSectionFileTorrentsDecoding implements UISWTConfigSection
{
	private final int REQUIRED_MODE = 2;

	public String configSectionGetParentSection() {
		return "torrents";
	}
	
	public int maxUserMode() {
		return REQUIRED_MODE;
	}


	/* Name of section will be pulled from 
	 * ConfigView.section.<i>configSectionGetName()</i>
	 */
	public String configSectionGetName() {
		return "torrent.decoding";
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {
	}

	public Composite configSectionCreate(final Composite parent) {
		GridData gridData;
		GridLayout layout;
		Label label;

		Composite cSection = new Composite(parent, SWT.NULL);
		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
				| GridData.HORIZONTAL_ALIGN_FILL);
		cSection.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 2;
		cSection.setLayout(layout);

		int userMode = COConfigurationManager.getIntParameter("User Mode");
		if (userMode < REQUIRED_MODE) {
			label = new Label(cSection, SWT.WRAP);
			gridData = new GridData();
			label.setLayoutData(gridData);

			final String[] modeKeys = {
					"ConfigView.section.mode.beginner",
					"ConfigView.section.mode.intermediate",
					"ConfigView.section.mode.advanced" };

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
					new String[] { param1, param2 }));

			return cSection;
		}

		// locale decoder
		label = new Label(cSection, SWT.NULL);
		Messages.setLanguageText(label, "ConfigView.section.file.decoder.label");

		LocaleUtilDecoder[] decoders = LocaleUtil.getSingleton().getDecoders();

		String decoderLabels[] = new String[decoders.length + 1];
		String decoderValues[] = new String[decoders.length + 1];

		decoderLabels[0] = MessageText.getString("ConfigView.section.file.decoder.nodecoder");
		decoderValues[0] = "";

		for (int i = 1; i <= decoders.length; i++) {
			decoderLabels[i] = decoderValues[i] = decoders[i - 1].getName();
		}
		new StringListParameter(cSection, "File.Decoder.Default", "",
				decoderLabels, decoderValues);

		// locale always prompt

		gridData = new GridData();
		gridData.horizontalSpan = 2;
		new BooleanParameter(cSection, "File.Decoder.Prompt",
				"ConfigView.section.file.decoder.prompt").setLayoutData(gridData);

		// show lax decodings

		gridData = new GridData();
		gridData.horizontalSpan = 2;
		new BooleanParameter(cSection, "File.Decoder.ShowLax",
				"ConfigView.section.file.decoder.showlax").setLayoutData(gridData);

		// show all decoders

		gridData = new GridData();
		gridData.horizontalSpan = 2;
		new BooleanParameter(cSection, "File.Decoder.ShowAll",
				"ConfigView.section.file.decoder.showall").setLayoutData(gridData);

		return cSection;
	}
}
