/*
 * File    : HealthHelpWindow.java
 * Created : 18 déc. 2003}
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
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
 */
package org.gudy.azureus2.ui.swt.help;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

/**
 * @author Olivier
 *
 */
public class HealthHelpWindow
{

	public static void show(Display display) {
		final ArrayList<String> imagesToRelease = new ArrayList();

		final Shell window = org.gudy.azureus2.ui.swt.components.shell.ShellFactory.createShell(
				display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		Utils.setShellIcon(window);
		window.setText(MessageText.getString("MyTorrentsView.menu.health"));

		Map mapIDs = new LinkedHashMap();
		mapIDs.put("grey", "st_stopped");
		mapIDs.put("red", "st_ko");
		mapIDs.put("blue", "st_no_tracker");
		mapIDs.put("yellow", "st_no_remote");
		mapIDs.put("green", "st_ok");
		mapIDs.put("error", "st_error");
		mapIDs.put("share", "st_shared");

		GridLayout layout = new GridLayout();
		layout.marginHeight = 3;
		layout.marginWidth = 3;
		try {
			layout.verticalSpacing = 3;
		} catch (NoSuchFieldError e) {
			/* Ignore for Pre 3.0 SWT.. */
		}
		window.setLayout(layout);

		ImageLoader imageLoader = ImageLoader.getInstance();
		for (Iterator iter = mapIDs.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			String value = (String) mapIDs.get(key);

			
			Image img = imageLoader.getImage(value);
			imagesToRelease.add(value);

			CLabel lbl = new CLabel(window, SWT.NONE);
			lbl.setImage(img);
			lbl.setText(MessageText.getString("health.explain." + key));
		}

		// buttons

		Button btnOk = new Button(window, SWT.PUSH);
		btnOk.setText(MessageText.getString("Button.ok"));
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		gridData.widthHint = 70;
		btnOk.setLayoutData(gridData);

		btnOk.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				window.dispose();
			}
		});
		
		window.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					window.dispose();
				}
			}
		});

		window.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent arg0) {
				ImageLoader imageLoader = ImageLoader.getInstance();
				for (String id : imagesToRelease) {
					imageLoader.releaseImage(id);
				}
			}
		});

		window.pack();
		window.open();

	}
}
