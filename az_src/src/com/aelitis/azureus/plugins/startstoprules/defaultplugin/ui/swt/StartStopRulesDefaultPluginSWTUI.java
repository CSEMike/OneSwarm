/*
 * Created on 11-Sep-2005
 * Created by Paul Gardner
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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
 *
 */

package com.aelitis.azureus.plugins.startstoprules.defaultplugin.ui.swt;

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import com.aelitis.azureus.plugins.startstoprules.defaultplugin.DefaultRankCalculator;

import org.gudy.azureus2.plugins.PluginInterface;

public class StartStopRulesDefaultPluginSWTUI {
	public StartStopRulesDefaultPluginSWTUI(PluginInterface plugin_interface) {
		plugin_interface.addConfigSection(new ConfigSectionQueue());
		plugin_interface.addConfigSection(new ConfigSectionSeeding());
		plugin_interface.addConfigSection(new ConfigSectionSeedingAutoStarting());
		plugin_interface.addConfigSection(new ConfigSectionSeedingFirstPriority());
		plugin_interface.addConfigSection(new ConfigSectionSeedingIgnore());
	}

	public static void openDebugWindow(final DefaultRankCalculator dlData) {
		final Shell shell = new Shell(Display.getCurrent(), SWT.ON_TOP
				| SWT.SHELL_TRIM | SWT.TOOL | SWT.CLOSE);

		GridLayout layout = new GridLayout();
		layout.numColumns = 4;
		GridData gd;
		shell.setLayout(layout);

		shell.setText("Debug for " + dlData.getDownloadObject().getName());

		final Text txtFP = new Text(shell, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 4;
		txtFP.setLayoutData(gd);

		final Button btnAutoRefresh = new Button(shell, SWT.CHECK);
		btnAutoRefresh.setText("Auto-Refresh");
		btnAutoRefresh.setLayoutData(new GridData());

		final Button btnRefresh = new Button(shell, SWT.NONE);
		btnRefresh.setLayoutData(new GridData());
		btnRefresh.setText("Refresh");

		final Button btnToClip = new Button(shell, SWT.NONE);
		btnToClip.setLayoutData(new GridData());
		btnToClip.setText("To Clipboard");
		btnToClip.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event event) {
        new Clipboard(Display.getCurrent()).setContents(
						new Object[] { txtFP.getText() },
						new Transfer[] { TextTransfer.getInstance() });
			}
		});

		final Label lbl = new Label(shell, SWT.NONE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		lbl.setLayoutData(gd);

		final TimerTask task = new TimerTask() {
			String lastText = "";

			public String formatString() {
				return "FP:\n" + dlData.sExplainFP + "\n" + "SR:" + dlData.sExplainSR
						+ "\n" + "TRACE:\n" + dlData.sTrace;
			}

			public void setText(final String s) {
				lastText = s;

				txtFP.setText(s);
			}

			public void run() {
				if (shell.isDisposed())
					return;

				shell.getDisplay().syncExec(new Runnable() {
					public void run() {
						if (shell.isDisposed()) {
							return;
						}
						String s = formatString();
						if (s.compareTo(lastText) != 0) {
							if (lastText.length() == 0 || btnAutoRefresh.getSelection()
									|| btnRefresh.getData("Pressing") != null)
								setText(s);
							else
								lbl.setText("Information is outdated.  Press refresh.");
						} else {
							lbl.setText("");
						}
					}
				});
			}
		};
		btnAutoRefresh.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event event) {
				if (btnAutoRefresh.getSelection())
					lbl.setText("");
				task.run();
			}
		});

		btnRefresh.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event event) {
				btnRefresh.setData("Pressing", "1");
				task.run();
				btnRefresh.setData("Pressing", null);
			}
		});
		
		shell.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					shell.dispose();
				}
			}
		});

		shell.setSize(550, 350);
		shell.open();

		Timer timer = new Timer(true);
		timer.schedule(task, 0, 2000);
	}
}
