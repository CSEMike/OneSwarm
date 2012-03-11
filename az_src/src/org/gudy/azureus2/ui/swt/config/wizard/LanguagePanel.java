/*
 * Created on Feb 28, 2006 8:54:43 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.ui.swt.config.wizard;

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.Wizard;

import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;

/**
 * @author TuxPaper
 * @created Feb 28, 2006
 *
 */
public class LanguagePanel extends AbstractWizardPanel {

	/**
	 * @param wizard
	 * @param previousPanel
	 */
	public LanguagePanel(Wizard wizard, IWizardPanel previousPanel) {
		super(wizard, previousPanel);
	}

	public void show() {
		GridData gridData;

		wizard.setTitleAsResourceID("configureWizard.welcome.title");

		Composite rootPanel = wizard.getPanel();
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		rootPanel.setLayout(layout);

		final Label lblChoose = new Label(rootPanel, SWT.WRAP);
		setChooseLabel(lblChoose);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		lblChoose.setLayoutData(gridData);

		final List lstLanguage = new List(rootPanel, SWT.BORDER | SWT.V_SCROLL
				| SWT.SINGLE);
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 350;
		lstLanguage.setLayoutData(gridData);

		final Locale[] locales = MessageText.getLocales(true);

		int iUsingLocale = -1;
		for (int i = 0; i < locales.length; i++) {
			Locale locale = locales[i];

			lstLanguage.add(buildName(locale));
			if (MessageText.isCurrentLocale(locale))
				iUsingLocale = i;
		}
		lstLanguage.select(iUsingLocale);

		lstLanguage.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				int index = lstLanguage.getSelectionIndex();
				if (index >= 0 && index < locales.length) {
					COConfigurationManager.setParameter("locale", locales[index]
							.toString());

					MessageText.loadBundle();
					DisplayFormatters.setUnits();
					DisplayFormatters.loadMessages();

					Shell shell = wizard.getWizardWindow();
					Messages.updateLanguageForControl(shell);
					setChooseLabel(lblChoose);

					shell.layout(true, true);

					lstLanguage.setRedraw(false);
					for (int i = 0; i < locales.length; i++) {
						lstLanguage.setItem(i, buildName(locales[i]));
					}
					lstLanguage.setRedraw(true);

					try {
						UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
						if (uiFunctions != null) {
							uiFunctions.refreshLanguage();
						}
					} catch (Exception ex) {
					}
				}
			}
		});

		FontData[] fontData = lstLanguage.getFont().getFontData();
		for (int i = 0; i < fontData.length; i++) {
			if (fontData[i].getHeight() < 10)
				fontData[i].setHeight(10);
		}
		final Font font = new Font(rootPanel.getDisplay(), fontData);
		lstLanguage.setFont(font);

		lstLanguage.getShell().addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {
				lstLanguage.showSelection();
			}
		});

		lstLanguage.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (font != null && !font.isDisposed())
					font.dispose();
			}
		});
	}

	/**
	 * @param lblChoose
	 */
	private void setChooseLabel(Label lblChoose) {
		String sLocaleChooseString = MessageText
				.getString("ConfigureWizard.language.choose");
		String sDefChooseString = MessageText
				.getDefaultLocaleString("ConfigureWizard.language.choose");
		if (sLocaleChooseString.equals(sDefChooseString)) {
			lblChoose.setText(sLocaleChooseString);
		} else {
			lblChoose.setText(sLocaleChooseString + "\n" + sDefChooseString);
		}
	}

	private String buildName(Locale locale) {
		StringBuffer sName = new StringBuffer();

		String sName1 = locale.getDisplayLanguage(locale);
		String sName2 = locale.getDisplayLanguage();
		sName.append(sName1);

		if (!sName1.equals(sName2)) {
			sName.append("/").append(sName2);
		}

		sName1 = locale.getDisplayCountry(locale);
		sName2 = locale.getDisplayCountry();
		if (sName1.length() > 0 || sName2.length() > 0) {
			sName.append(" (");
			if (sName1.length() > 0)
				sName.append(sName1);

			if (sName2.length() > 0 && !sName1.equals(sName2)) {
				sName.append("/").append(sName2);
			}

			sName1 = locale.getDisplayVariant(locale);
			sName2 = locale.getDisplayVariant();
			if (sName1.length() > 0 || sName2.length() > 0) {
				sName.append(", ");
				if (sName1.length() > 0)
					sName.append(sName1);

				if (sName2.length() > 0 && !sName1.equals(sName2)) {
					sName.append("/").append(sName2);
				}
			}

			sName.append(")");
		}

		return sName.toString();
	}

	public boolean isNextEnabled() {
		return true;
	}

	public IWizardPanel getNextPanel() {
		return new WelcomePanel(((ConfigureWizard) wizard), this);
	}
}
