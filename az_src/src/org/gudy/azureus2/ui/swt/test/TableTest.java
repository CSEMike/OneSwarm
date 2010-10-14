/*
 * Created on Jul 18, 2006 11:13:05 AM
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
package org.gudy.azureus2.ui.swt.test;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * @author TuxPaper
 * @created Jul 18, 2006
 *
 */
public class TableTest
{
	static Table tableNormal;

	private static Table tableVirtual;

	static Display display;

	public static void main(String[] args) {
		display = Display.getDefault();

		Shell shell = new Shell(display, SWT.SHELL_TRIM);
		shell.setLayout(new FillLayout());

		tableNormal = new Table(shell, SWT.BORDER);
		tableVirtual = new Table(shell, SWT.BORDER | SWT.VIRTUAL);

		Button btnStart = new Button(shell, SWT.PUSH);
		btnStart.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				runtest();
			}
		});

		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * 
	 */
	protected static void runtest() {
		tableNormal.clearAll();
		waitForComplete();
		runtest1();

		tableNormal.clearAll();
		waitForComplete();

		runtest2();

		tableNormal.clearAll();
		waitForComplete();
		runtest3();

		tableNormal.clearAll();
		waitForComplete();
		runtest4();
		
		tableNormal.clearAll();
		waitForComplete();
		runtest5();

		tableNormal.clearAll();
		waitForComplete();
		runtest6();

		tableNormal.clearAll();
		waitForComplete();
		runtest7();
	}

	static void runtest1() {
		long lStartTime = System.currentTimeMillis();

		for (int i = 0; i < 500; i++) {
			new TableItem(tableNormal, SWT.None);
		}
		waitForComplete();

		long lEndTime = System.currentTimeMillis();
		System.out.println("NVI: " + (lEndTime - lStartTime));
	}

	static void runtest2() {
		long lStartTime = System.currentTimeMillis();

		tableNormal.setItemCount(500);
		waitForComplete();

		long lEndTime = System.currentTimeMillis();
		System.out.println("NVS: " + (lEndTime - lStartTime));
		
	}

	static void runtest3() {
		long lStartTime = System.currentTimeMillis();

		for (int i = 0; i < 500; i++) {
			new TableItem(tableVirtual, SWT.None);
		}
		waitForComplete();

		long lEndTime = System.currentTimeMillis();
		System.out.println("VI:" + (lEndTime - lStartTime));
	}

	static void runtest4() {
		long lStartTime = System.currentTimeMillis();

		tableVirtual.setItemCount(500);
		
		waitForComplete();

		long lEndTime = System.currentTimeMillis();
		System.out.println("Vs:" + (lEndTime - lStartTime));
	}
	
	static void waitForComplete() {
		while (display.readAndDispatch()) {}
	}

	static void runtest5() {
		tableNormal.setItemCount(1000);
		waitForComplete();

		long lStartTime = System.currentTimeMillis();
		
		tableNormal.setItemCount(500);

		long lEndTime = System.currentTimeMillis();
		System.out.println("NVD1: " + (lEndTime - lStartTime));
		
	}

	static void runtest6() {
		tableNormal.setItemCount(1000);
		waitForComplete();

		long lStartTime = System.currentTimeMillis();
		
		for (int i = 0; i < 500; i++) {
			tableNormal.getItem(2).dispose();
		}

		long lEndTime = System.currentTimeMillis();
		System.out.println("NVD2: " + (lEndTime - lStartTime));
	}

	static void runtest7() {
		tableNormal.setItemCount(1000);
		waitForComplete();


		int indexes[] = new int[500];
		for (int i = 0; i < 200; i++) {
			indexes[i] = i * 2;
		}
		int x = 400;
		for (int i = 200; i < 500; i++) {
			indexes[i] = x++;
		}

		long lStartTime = System.currentTimeMillis();
		
		tableNormal.remove(indexes);

		long lEndTime = System.currentTimeMillis();
		System.out.println("NVD3: " + (lEndTime - lStartTime));
	}
}
