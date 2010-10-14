/*
 * Created on May 28, 2006 4:31:42 PM
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
package org.gudy.azureus2.ui.swt.debug;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.logging.impl.FileLogging;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.InputShell;

/**
 * @author TuxPaper
 * @created May 28, 2006
 *
 */
public class UIDebugGenerator
{
	public static void generate() {
		Display display = Display.getCurrent();
		if (display == null) {
			return;
		}

		// make sure display is up to date
		while (display.readAndDispatch()) {
		}

		Shell[] shells = display.getShells();
		if (shells == null || shells.length == 0) {
			return;
		}

		File path = new File(SystemProperties.getUserPath(), "debug");
		if (!path.isDirectory()) {
			path.mkdir();
		} else {
			try {
				File[] files = path.listFiles();
				for (int i = 0; i < files.length; i++) {
					files[i].delete();
				}
			} catch (Exception e) {
			}
		}

		for (int i = 0; i < shells.length; i++) {
			try {
				Shell shell = shells[i];
				Image image = null;

				if (shell.getData("class") instanceof ObfusticateShell) {
					ObfusticateShell shellClass = (ObfusticateShell) shell.getData("class");

					try {
						image = shellClass.generateObfusticatedImage();
					} catch (Exception e) {
						Debug.out("Obfusticating shell " + shell, e);
					}
				} else {

					Rectangle clientArea = shell.getClientArea();
					image = new Image(display, clientArea.width, clientArea.height);

					GC gc = new GC(shell);
					try {
						gc.copyArea(image, clientArea.x, clientArea.y);
					} finally {
						gc.dispose();
					}
				}

				if (image != null) {
					File file = new File(path, "image-" + i + ".jpg");
					String sFileName = file.getAbsolutePath();

					ImageLoader imageLoader = new ImageLoader();
					imageLoader.data = new ImageData[] { image.getImageData() };
					imageLoader.save(sFileName, SWT.IMAGE_JPEG);
				}
			} catch (Exception e) {
				Logger.log(new LogEvent(LogIDs.GUI, "Creating Obfusticated Image", e));
			}
		}

		InputShell inputShell = new InputShell("UIDebugGenerator.messageask.title",
				"UIDebugGenerator.messageask.text", true);
		String message = inputShell.open();
		if (inputShell.isCanceled()) {
			return;
		} 

		if (message == null || message.length() == 0) {
			Utils.openMessageBox(Utils.findAnyShell(), SWT.OK,
					"UIDebugGenerator.message.cancel", (String[]) null);
			return;
		}

		try {
			File fUserMessage = new File(path, "usermessage.txt");
			FileWriter fw;
			fw = new FileWriter(fUserMessage);

			fw.write(message);

			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			File fEvidence = new File(path, "evidence.log");
			FileWriter fw;
			fw = new FileWriter(fEvidence);
			PrintWriter pw = new PrintWriter(fw);

			AEDiagnostics.generateEvidence(pw);

			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			File outFile = new File(SystemProperties.getUserPath(), "debug.zip");
			if (outFile.exists()) {
				outFile.delete();
			}

			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFile));

			// %USERDIR%\logs
			File logPath = new File(SystemProperties.getUserPath(), "logs");
			File[] files = logPath.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.getName().endsWith(".log");
				}
			});
			addFilesToZip(out, files);

			// %USERDIR%
			File userPath = new File(SystemProperties.getUserPath());
			files = userPath.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.getName().endsWith(".log");
				}
			});
			addFilesToZip(out, files);

			// %USERDIR%\debug
			files = path.listFiles();
			addFilesToZip(out, files);

			// recent errors from exe dir
			final long ago = SystemTime.getCurrentTime() - 1000L * 60 * 60 * 24 * 90;
			File azureusPath = new File(SystemProperties.getApplicationPath());
			files = azureusPath.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					return (pathname.getName().startsWith("hs_err") && pathname.lastModified() > ago);
				}
			});
			addFilesToZip(out, files);

			// recent errors from OSX java log dir
			File javaLogPath = new File(System.getProperty("user.home"), "Library"
					+ File.separator + "Logs" + File.separator + "Java");
			if (javaLogPath.isDirectory()) {
				files = javaLogPath.listFiles(new FileFilter() {
					public boolean accept(File pathname) {
						return (pathname.getName().endsWith("log") && pathname.lastModified() > ago);
					}
				});
				addFilesToZip(out, files);
			}

			boolean bLogToFile = COConfigurationManager.getBooleanParameter("Logging Enable");
			String sLogDir = COConfigurationManager.getStringParameter("Logging Dir",
					"");
			if (bLogToFile && sLogDir != null) {
				File loggingFile = new File(sLogDir, FileLogging.LOG_FILE_NAME);
				if (loggingFile.isFile()) {
					addFilesToZip(out, new File[] { loggingFile });
				}
			}

			out.close();

			if (outFile.exists()) {
				int result = Utils.openMessageBox(Utils.findAnyShell(), SWT.OK
						| SWT.CANCEL | SWT.ICON_INFORMATION | SWT.APPLICATION_MODAL,
						"UIDebugGenerator.complete", new String[] { outFile.toString() });

				if (result == SWT.OK) {
					try {
						PlatformManagerFactory.getPlatformManager().showFile(
								outFile.getAbsolutePath());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void addFilesToZip(ZipOutputStream out, File[] files) {
		byte[] buf = new byte[1024];
		if (files == null) {
			return;
		}

		for (int j = 0; j < files.length; j++) {
			File file = files[j];

			FileInputStream in;
			try {
				in = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				continue;
			}

			try {
				ZipEntry entry = new ZipEntry(file.getName());
				entry.setTime(file.lastModified());
				out.putNextEntry(entry);
				//	Transfer bytes from the file to the ZIP file
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}

				// Complete the entry
				out.closeEntry();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param image
	 * @param bounds
	 */
	// XXX After we swith to 3.2, display param can be removed, and 
	// image.getDevice() can be used
	public static void obfusticateArea(Display display, Image image,
			Rectangle bounds)
	{
		GC gc = new GC(image);
		try {
			gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
			gc.setForeground(display.getSystemColor(SWT.COLOR_RED));
			gc.fillRectangle(bounds);
			gc.drawRectangle(bounds);
			int x2 = bounds.x + bounds.width;
			int y2 = bounds.y + bounds.height;
			gc.drawLine(bounds.x, bounds.y, x2, y2);
			gc.drawLine(x2, bounds.y, bounds.x, y2);
		} finally {
			gc.dispose();
		}
	}

	/**
	 * @param image
	 * @param bounds
	 * @param text
	 */
	public static void obfusticateArea(Display display, Image image,
			Rectangle bounds, String text)
	{

		if (bounds.isEmpty())
			return;

		if (text == "") {
			obfusticateArea(display, image, bounds);
			return;
		}

		GC gc = new GC(image);
		try {
			gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
			gc.setForeground(display.getSystemColor(SWT.COLOR_RED));
			gc.fillRectangle(bounds);
			gc.drawRectangle(bounds);
			gc.setClipping(bounds);
			gc.drawText(text, bounds.x + 2, bounds.y + 1);
		} finally {
			gc.dispose();
		}
	}

	/**
	 * @param image
	 * @param control
	 * @param shellOffset 
	 * @param text 
	 */
	public static void obfusticateArea(Image image, Control control,
			Point shellOffset, String text)
	{
		Rectangle bounds = control.getBounds();
		Point offset = control.getParent().toDisplay(bounds.x, bounds.y);
		bounds.x = offset.x - shellOffset.x;
		bounds.y = offset.y - shellOffset.y;

		obfusticateArea(control.getDisplay(), image, bounds, text);
	}
}
