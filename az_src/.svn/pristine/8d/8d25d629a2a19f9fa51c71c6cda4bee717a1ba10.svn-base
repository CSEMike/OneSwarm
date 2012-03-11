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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.PasswordParameter;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;

public class ConfigSectionInterfacePassword implements UISWTConfigSection
{
	private final static String LBLKEY_PREFIX = "ConfigView.label.";

	private final int REQUIRED_MODE = 0;

	Label passwordMatch;


	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_INTERFACE;
	}

	/* Name of section will be pulled from 
	 * ConfigView.section.<i>configSectionGetName()</i>
	 */
	public String configSectionGetName() {
		return "interface.password";
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {
	}
	
	public int maxUserMode() {
		return REQUIRED_MODE;
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
		layout.marginWidth = 0;
		layout.numColumns = 2;
		cSection.setLayout(layout);

		// password

		label = new Label(cSection, SWT.NULL);
		Messages.setLanguageText(label, LBLKEY_PREFIX + "password");

		gridData = new GridData();
		gridData.widthHint = 150;
		PasswordParameter pw1 = new PasswordParameter(cSection, "Password");
		pw1.setLayoutData(gridData);
		Text t1 = (Text) pw1.getControl();

		//password confirm

		label = new Label(cSection, SWT.NULL);
		Messages.setLanguageText(label, LBLKEY_PREFIX + "passwordconfirm");
		gridData = new GridData();
		gridData.widthHint = 150;
		PasswordParameter pw2 = new PasswordParameter(cSection, "Password Confirm");
		pw2.setLayoutData(gridData);
		Text t2 = (Text) pw2.getControl();

		// password activated

		label = new Label(cSection, SWT.NULL);
		Messages.setLanguageText(label, LBLKEY_PREFIX + "passwordmatch");
		passwordMatch = new Label(cSection, SWT.NULL);
		gridData = new GridData();
		gridData.widthHint = 150;
		passwordMatch.setLayoutData(gridData);
		refreshPWLabel();

		t1.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				refreshPWLabel();
			}
		});
		t2.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				refreshPWLabel();
			}
		});


		return cSection;
	}


	private void refreshPWLabel() {

		if (passwordMatch == null || passwordMatch.isDisposed())
			return;
		byte[] password = COConfigurationManager.getByteParameter("Password", ""
				.getBytes());
		COConfigurationManager.setParameter("Password enabled", false);
		if (password.length == 0) {
			passwordMatch.setText(MessageText.getString(LBLKEY_PREFIX
					+ "passwordmatchnone"));
		} else {
			byte[] confirm = COConfigurationManager.getByteParameter(
					"Password Confirm", "".getBytes());
			if (confirm.length == 0) {
				passwordMatch.setText(MessageText.getString(LBLKEY_PREFIX
						+ "passwordmatchno"));
			} else {
				boolean same = true;
				for (int i = 0; i < password.length; i++) {
					if (password[i] != confirm[i])
						same = false;
				}
				if (same) {
					passwordMatch.setText(MessageText.getString(LBLKEY_PREFIX
							+ "passwordmatchyes"));
					COConfigurationManager.setParameter("Password enabled", true);
				} else {
					passwordMatch.setText(MessageText.getString(LBLKEY_PREFIX
							+ "passwordmatchno"));
				}
			}
		}
	}
}
