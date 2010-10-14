/*
 * File    : InputShell.java
 * Created : Oct 27, 2005
 * By      : TuxPaper
 *
 * Copyright (C) 2005, 2006 Aelitis SAS, All rights Reserved
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

package org.gudy.azureus2.ui.swt.shells;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThreadAlreadyInstanciatedException;

/**
 * @author TuxPaper
 *
 */
public class InputShell
{
	private String sTitleKey;

	private String[] p0;

	private String sLabelKey;

	private String[] p1;

	private String textValue;

	private boolean bMultiLine;

	private boolean bIsCanceled;

	public InputShell(String sTitleKey, String sLabelKey) {
		this(sTitleKey, null, sLabelKey, null, false);
	}

	public InputShell(String sTitleKey, String sLabelKey, boolean bMultiLine) {
		this(sTitleKey, null, sLabelKey, null, bMultiLine);
	}

	public InputShell(String sTitleKey, String[] p0, String sLabelKey, String[] p1) {
		this(sTitleKey, p0, sLabelKey, p1, false);
	}

	public InputShell(String sTitleKey, String[] p0, String sLabelKey,
			String[] p1, boolean bMultiLine) {
		this.sTitleKey = sTitleKey;
		this.p0 = p0;
		this.sLabelKey = sLabelKey;
		this.p1 = p1;
		this.bMultiLine = bMultiLine;
		this.bIsCanceled = true;

		this.setTextValue("");
	}

	public String open() {
		final Display display = SWTThread.getInstance().getDisplay();
		if (display == null)
			return null;

		final Shell shell = ShellFactory.createShell(display.getActiveShell(),
				SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
		Messages.setLanguageText(shell, sTitleKey, p0);
		Utils.setShellIcon(shell);

		GridLayout layout = new GridLayout();
		shell.setLayout(layout);

		Label label = new Label(shell, SWT.WRAP);
		Messages.setLanguageText(label, sLabelKey, p1);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(gridData);

		int style = SWT.BORDER;
		if (bMultiLine) {
			style |= SWT.MULTI;
		}
		final Text text = new Text(shell, style);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.widthHint = 300;
		if (bMultiLine) {
			gridData.heightHint = 100;
		}
		text.setLayoutData(gridData);
		text.setText(textValue);
		text.selectAll();

		Composite panel = new Composite(shell, SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 3;
		panel.setLayout(layout);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		gridData.horizontalAlignment = SWT.CENTER;
		gridData.verticalAlignment = SWT.BOTTOM;
		panel.setLayoutData(gridData);
		Button ok = new Button(panel, SWT.PUSH);
		ok.setText(MessageText.getString("Button.ok"));
		gridData = new GridData();
		gridData.widthHint = 70;
		ok.setLayoutData(gridData);
		shell.setDefaultButton(ok);
		ok.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				try {
					setTextValue(text.getText());
					bIsCanceled = false;
					shell.dispose();
				} catch (Exception e) {
					Debug.printStackTrace(e);
				}
			}
		});

		Button cancel = new Button(panel, SWT.PUSH);
		cancel.setText(MessageText.getString("Button.cancel"));
		gridData = new GridData();
		gridData.widthHint = 70;
		cancel.setLayoutData(gridData);
		cancel.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				shell.dispose();
			}
		});

		shell.pack();
		Utils.centreWindow(shell);
		Utils.createURLDropTarget(shell, text);
		setTextValue(null);
		shell.open();
		
		// PIAMOD -- make these appear in front of everything. 
		shell.forceActive();

		while (!shell.isDisposed())
			if (!display.readAndDispatch())
				display.sleep();

		return getTextValue();
	}

	/**
	 * @param textValue The textValue to set.
	 */
	public void setTextValue(String textValue) {
		this.textValue = textValue;
	}

	/**
	 * @return Returns the textValue.
	 */
	public String getTextValue() {
		return textValue;
	}

	public void setLabelParameters(String[] p1) {
		this.p1 = p1;
	}

	public boolean isMultiLine() {
		return bMultiLine;
	}

	public void setMultiLine(boolean multiLine) {
		bMultiLine = multiLine;
	}

	public boolean isCanceled() {
		return bIsCanceled;
	}

	public static void main(String[] args) {
		try {
			new Display();
			SWTThread.createInstance(null);
			InputShell shell = new InputShell("MyTorrentsView.dialog.setSpeed.title",
					new String[] { "111111111111"
					}, "MyTorrentsView.dialog.setNumber.text", new String[] {
						"222222",
						"3333333333"
					});
			shell.open();
		} catch (SWTThreadAlreadyInstanciatedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
