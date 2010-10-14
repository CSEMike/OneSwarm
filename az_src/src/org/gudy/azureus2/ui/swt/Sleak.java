/*
 * Created on Sep 10, 2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.ui.swt;

/*
 * Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

/**
 * Code to detect swt leak
 *
 */
public class Sleak
{
	Display display;

	Shell shell;

	List list;

	Canvas canvas;

	Button start, stop, check;

	Text text;

	Text label;

	Object[] oldObjects = new Object[0];

	Error[] oldErrors = new Error[0];

	Object[] objects = new Object[0];

	Error[] errors = new Error[0];
	
	Map all = new HashMap();
	
	ArrayList oldNonResources = new ArrayList();

	public void open() {
		display = Display.getCurrent();
		shell = new Shell(display);
		shell.setText("S-Leak");
		list = new List(shell, SWT.BORDER | SWT.V_SCROLL);
		list.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				refreshObject();
			}
		});
		text = new Text(shell, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		canvas = new Canvas(shell, SWT.BORDER);
		canvas.addListener(SWT.Paint, new Listener() {
			public void handleEvent(Event event) {
				paintCanvas(event);
			}
		});
		check = new Button(shell, SWT.CHECK);
		check.setText("Stack");
		check.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				toggleStackTrace();
			}
		});
		start = new Button(shell, SWT.PUSH);
		start.setText("Snap");
		start.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				refreshAll();
			}
		});
		stop = new Button(shell, SWT.PUSH);
		stop.setText("Diff");
		stop.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				refreshDifference();
			}
		});
		label = new Text(shell, SWT.BORDER | SWT.READ_ONLY + SWT.MULTI);
		label.setText("0 object(s)");
		shell.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				layout();
			}
		});
		check.setSelection(false);
		text.setVisible(false);
		Point size = shell.getSize();
		shell.setSize(size.x / 2, size.y / 2);
		shell.open();
	}

	void refreshLabel() {
		int colors = 0, cursors = 0, fonts = 0, gcs = 0, images = 0, regions = 0, others = 0, composites = 0, labels = 0;
		for (int i = 0; i < objects.length; i++) {
			Object object = objects[i];
			if (object instanceof Color) {
				colors++;
			} else if (object instanceof Cursor) {
				cursors++;
			} else if (object instanceof Font) {
				fonts++;
			} else if (object instanceof GC) {
				gcs++;
			} else if (object instanceof Image) {
				images++;
			} else if (object instanceof Region) {
				regions++;
			} else if (object instanceof Composite) {
				composites++;
			} else if (object instanceof Label) {
				labels++;
			} else {
				others++;
			}
		}
		String string = "";
		if (colors != 0) {
			string += colors + " Color(s)\n";
		}
		if (cursors != 0) {
			string += cursors + " Cursor(s)\n";
		}
		if (fonts != 0) {
			string += fonts + " Font(s)\n";
		}
		if (gcs != 0) {
			string += gcs + " GC(s)\n";
		}
		if (images != 0) {
			string += images + " Image(s)\n";
		}
		if (composites != 0) {
			string += composites + " composite(s)\n";
		}
		if (labels != 0) {
			string += labels + " label(s)\n";
		}
		if (others != 0) {
			string += others + " Other(s)\n";
		}
		/* Currently regions are not counted. */
		//	if (regions != 0) string += regions + " Region(s)\n";
		if (string.length() != 0) {
			string = string.substring(0, string.length() - 1);
		}
		label.setText(string);
	}

	void refreshDifference() {
		DeviceData info = display.getDeviceData();
		if (!info.tracking) {
			MessageBox dialog = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
			dialog.setText(shell.getText());
			dialog.setMessage("Warning: Device is not tracking resource allocation");
			dialog.open();
		}
		Object[] newObjects = info.objects;
		Error[] newErrors = info.errors;
		Object[] diffObjects = new Object[newObjects.length];
		Error[] diffErrors = new Error[newErrors.length];
		int countResourceType = 0;
		for (int i = 0; i < newObjects.length; i++) {
			int index = 0;
			while (index < oldObjects.length) {
				if (newObjects[i] == oldObjects[index]) {
					break;
				}
				index++;
			}
			if (index == oldObjects.length) {
				diffObjects[countResourceType] = newObjects[i];
				diffErrors[countResourceType] = newErrors[i];
				countResourceType++;
			}
		}

		Shell[] shells = display.getShells();
		ArrayList nonResourceList = new ArrayList();
		for (int i = 0; i < shells.length; i++) {
			Shell shell = shells[i];
			if (shell != this.shell) {
				buildObjectList(shell, nonResourceList);
			}
		}
		oldNonResources = nonResourceList;
		Object[] nonResources = nonResourceList.toArray();
		int countNonResources = nonResources.length;

		int total = countResourceType + countNonResources;

		objects = new Object[total];
		errors = new Error[total];
		System.arraycopy(diffObjects, 0, objects, 0, countResourceType);
		System.arraycopy(diffErrors, 0, errors, 0, countResourceType);
		System.arraycopy(nonResources, 0, objects, countResourceType,
				countNonResources);
		list.removeAll();
		text.setText("");
		canvas.redraw();
		for (int i = 0; i < objects.length; i++) {
			list.add(objectName(objects[i]));
		}
		System.out.println(countResourceType);
		refreshLabel();
		layout();
	}

	/**
	 * @param shell2
	 * @param list2
	 */
	private void buildObjectList(Control control, ArrayList list) {
		if (!oldNonResources.contains(control)) {
			list.add(control);
		}

		if (control instanceof Composite) {
			Composite c = (Composite) control;
			Control[] children = c.getChildren();
			for (int i = 0; i < children.length; i++) {
				Control control2 = children[i];
				buildObjectList(control2, list);
			}
		}
	}

	String objectName(Object object) {
		Date timeAdded = (Date)all.get(object);
		if (timeAdded == null) {
			timeAdded = new Date();
			all.put(object, timeAdded);
		}

		String string = timeAdded + "] " + object.toString();
		if (object instanceof Resource) {
			return string;
		}

		int index = string.indexOf(" {");
		if (index == -1) {
			return string;
		}
		string = string.substring(0, index);
		if (object instanceof Composite) {
			Control[] children = ((Composite) object).getChildren();
			string += ": " + children.length + " kids";
		}
		return string;
	}

	void toggleStackTrace() {
		refreshObject();
		layout();
	}

	void paintCanvas(Event event) {
		canvas.setCursor(null);
		int index = list.getSelectionIndex();
		if (index == -1) {
			return;
		}
		GC gc = event.gc;
		Object object = objects[index];
		if (object instanceof Color) {
			if (((Color) object).isDisposed()) {
				gc.drawString("Color disposed", 0, 0);
				return;
			}
			gc.setBackground((Color) object);
			gc.fillRectangle(canvas.getClientArea());
			return;
		}
		if (object instanceof Cursor) {
			if (((Cursor) object).isDisposed()) {
				gc.drawString("Cursor disposed", 0, 0);
				return;
			}
			canvas.setCursor((Cursor) object);
			return;
		}
		if (object instanceof Font) {
			if (((Font) object).isDisposed()) {
				gc.drawString("Font disposed", 0, 0);
				return;
			}
			gc.setFont((Font) object);
			FontData[] array = gc.getFont().getFontData();
			String string = "";
			String lf = text.getLineDelimiter();
			for (int i = 0; i < array.length; i++) {
				FontData data = array[i];
				String style = "NORMAL";
				int bits = data.getStyle();
				if (bits != 0) {
					if ((bits & SWT.BOLD) != 0) {
						style = "BOLD ";
					}
					if ((bits & SWT.ITALIC) != 0) {
						style += "ITALIC";
					}
				}
				string += data.getName() + " " + data.getHeight() + " " + style + lf;
			}
			gc.drawString(string, 0, 0);
			return;
		}
		//NOTHING TO DRAW FOR GC
		//	if (object instanceof GC) {
		//		return;
		//	}
		if (object instanceof Image) {
			if (((Image) object).isDisposed()) {
				gc.drawString("Image disposed", 0, 0);
				return;
			}
			gc.drawImage((Image) object, 0, 0);
			return;
		}
		if (object instanceof Region) {
			if (((Region) object).isDisposed()) {
				return;
			}
			String string = ((Region) object).getBounds().toString();
			gc.drawString(string, 0, 0);
			return;
		}

		if (object instanceof Control) {
			gc.drawString(object.toString(), 0, 0);
			gc.drawString(((Control) object).getBounds().toString(), 0, 20);

			if (object instanceof Widget) {
				Object data = ((Widget)object).getData("sleak");
				if (data != null) {
					gc.drawString(data.toString(), 0, 35);
				}
			}
			return;
		}
	}

	void refreshObject() {
		int index = list.getSelectionIndex();
		if (index == -1) {
			return;
		}
		if (check.getSelection() && index < errors.length && errors[index] == null) {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			PrintStream s = new PrintStream(stream);
			errors[index].printStackTrace(s);
			text.setText(stream.toString());
			text.setVisible(true);
			canvas.setVisible(false);
		} else {
			canvas.setVisible(true);
			text.setVisible(false);
			canvas.redraw();
		}
	}

	void refreshAll() {
		oldObjects = new Object[0];
		oldErrors = new Error[0];
		oldNonResources = new ArrayList();
		refreshDifference();
		oldObjects = objects;
		oldErrors = errors;
	}

	void layout() {
		Rectangle rect = shell.getClientArea();
		int width = 0;
		String[] items = list.getItems();
		GC gc = new GC(list);
		for (int i = 0; i < objects.length; i++) {
			width = Math.max(width, gc.stringExtent(items[i]).x);
		}
		gc.dispose();
		Point size1 = start.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		Point size2 = stop.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		Point size3 = check.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		Point size4 = label.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		width = Math.max(size1.x, Math.max(size2.x, Math.max(size3.x, width)));
		width = Math.max(64, Math.max(size4.x,
				list.computeSize(width, SWT.DEFAULT).x));
		start.setBounds(0, 0, width, size1.y);
		stop.setBounds(0, size1.y, width, size2.y);
		check.setBounds(0, size1.y + size2.y, width, size3.y);
		label.setBounds(0, rect.height - size4.y, width, size4.y);
		int height = size1.y + size2.y + size3.y;
		list.setBounds(0, height, width, rect.height - height - size4.y);
		text.setBounds(width, 0, rect.width - width, rect.height);
		canvas.setBounds(width, 0, rect.width - width, rect.height);
	}

	public static void main(String[] args) {
    DeviceData data = new DeviceData();
    data.tracking = true;
    Display display = new Display (data);
		Sleak sleak = new Sleak();
    Main.main(args);
		sleak.open();
		while (!sleak.shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		try {
			if (!display.isDisposed()) {
				display.dispose();
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}