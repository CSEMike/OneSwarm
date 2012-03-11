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
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.logging.impl.FileLogging;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.FeatureManager;
import org.gudy.azureus2.plugins.utils.FeatureManager.FeatureDetails;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderFactoryImpl;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT.TriggerInThread;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.core.util.AZ3Functions;
import com.aelitis.azureus.ui.UserPrompterResultListener;

/**
 * @author TuxPaper
 * @created May 28, 2006
 *
 */
public class UIDebugGenerator
{
	public static class GeneratedResults
	{
		File file;

		String message;
		
		boolean sendNow;

		public String email;
	}

	public static void generate(final String sourceRef, String additionalText) {
		final GeneratedResults gr = generate(null, false,
				"UIDebugGenerator.messageask");
		if (gr != null) {
			AZ3Functions.provider az3 = AZ3Functions.getProvider();

			if (az3 != null && gr.sendNow) {
				
				if (gr.email != null && gr.email.length() > 0) {
					additionalText += "\n" + gr.email;
				}
				
				ResourceDownloaderFactory rdf = ResourceDownloaderFactoryImpl.getSingleton();
				String url = az3.getDefaultContentNetworkURL(az3.SERVICE_SITE_RELATIVE,
						new Object[] {
							"/debugSender.start",
							true
						});
				StringBuffer postData = new StringBuffer();

				PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getDefaultPluginInterface();
				FeatureManager featman = pi.getUtilities().getFeatureManager();

				if (featman != null) {
					FeatureDetails[] featureDetails = featman.getFeatureDetails("dvdburn");
					if (featureDetails != null && featureDetails.length > 0) {
						// Could walk through details and find the most valid..

						FeatureDetails bestDetails = featureDetails[0];
						postData.append("license=");
						postData.append(UrlUtils.encode(bestDetails.getLicence().getKey()));
						postData.append("&");
					}
				}

				postData.append("message=");
				postData.append(UrlUtils.encode(gr.message));
				postData.append("&error=");
				postData.append(UrlUtils.encode(additionalText));
				postData.append("&sourceRef=");
				postData.append(UrlUtils.encode(sourceRef));
				if (gr.email != null && gr.email.length() > 0) {
					postData.append("&email=");
					postData.append(UrlUtils.encode(gr.email));
				}
				postData.append("&debug_zip=");
				try {
					byte[] fileArray = FileUtil.readFileAsByteArray(gr.file);
					postData.append(UrlUtils.encode(new String(Base64.encode(fileArray))));

					ResourceDownloader rd = rdf.create(new URL(url), postData.toString());

					rd.addListener(new ResourceDownloaderListener() {

						public void reportPercentComplete(ResourceDownloader downloader,
								int percentage) {
						}

						public void reportAmountComplete(ResourceDownloader downloader,
								long amount) {
						}

						public void reportActivity(ResourceDownloader downloader,
								String activity) {
						}

						public void failed(ResourceDownloader downloader,
								ResourceDownloaderException e) {
							Debug.out(e);
						}

						public boolean completed(ResourceDownloader downloader,
								InputStream data) {
							try {
								int i = data.available();
								byte[] b = new byte[i];
								data.read(b);
							} catch (Throwable t) {

							}
							return true;
						}
					});

					rd.asyncDownload();
				} catch (Exception e) {
					Debug.out(e);
				}
			} else {

				MessageBoxShell mb = new MessageBoxShell(SWT.OK | SWT.CANCEL
						| SWT.ICON_INFORMATION | SWT.APPLICATION_MODAL,
						"UIDebugGenerator.complete", new String[] {
							gr.file.toString()
						});
				mb.open(new UserPrompterResultListener() {
					public void prompterClosed(int result) {
						if (result == SWT.OK) {
							try {
								PlatformManagerFactory.getPlatformManager().showFile(
										gr.file.getAbsolutePath());
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				});
			}
		}
	}

	public static GeneratedResults generate(File[] extraLogDirs,
			boolean allowEmpty, String msgPrefix) {
		Display display = Display.getCurrent();
		if (display == null) {
			return null;
		}
		
		Shell activeShell = display.getActiveShell();
		if (activeShell != null) {
			activeShell.setCursor(display.getSystemCursor(SWT.CURSOR_WAIT));
		}
		
		// make sure display is up to date
		while (display.readAndDispatch()) {
		}
		
		Shell[] shells = display.getShells();
		if (shells == null || shells.length == 0) {
			return null;
		}

		final File path = new File(SystemProperties.getUserPath(), "debug");
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
				
				if (shell.isDisposed() || !shell.isVisible()) {
					continue;
				}

				if (shell.getData("class") instanceof ObfusticateShell) {
					ObfusticateShell shellClass = (ObfusticateShell) shell.getData("class");

					try {
						image = shellClass.generateObfusticatedImage();
					} catch (Exception e) {
						Debug.out("Obfuscating shell " + shell, e);
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
					File file = new File(path, "image-" + i + ".vpg");
					String sFileName = file.getAbsolutePath();

					ImageLoader imageLoader = new ImageLoader();
					imageLoader.data = new ImageData[] {
						image.getImageData()
					};
					imageLoader.save(sFileName, SWT.IMAGE_JPEG);
				}

			} catch (Exception e) {
				Logger.log(new LogEvent(LogIDs.GUI, "Creating Obfusticated Image", e));
			}
		}

		GeneratedResults gr = new GeneratedResults();

		if (activeShell != null) {
			activeShell.setCursor(null);
		}
		promptUser(allowEmpty, gr);
		if (gr.message == null) {
			return null;
		}

		try {
			File fUserMessage = new File(path, "usermessage.txt");
			FileWriter fw;
			fw = new FileWriter(fUserMessage);

			fw.write(gr.message  + "\n" + gr.email);
			
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		CoreWaiterSWT.waitForCore(TriggerInThread.ANY_THREAD,
				new AzureusCoreRunningListener() {

					public void azureusCoreRunning(AzureusCore core) {
						core.createOperation(AzureusCoreOperation.OP_PROGRESS,
								new AzureusCoreOperationTask() {
									public void run(AzureusCoreOperation operation) {
										try {

											File fEvidence = new File(path, "evidence.log");
											PrintWriter pw = new PrintWriter(fEvidence, "UTF-8");

											AEDiagnostics.generateEvidence(pw);

											pw.close();

										} catch (IOException e) {

											Debug.printStackTrace(e);
										}
									}
								});
					}
				});

		try {
			final File outFile = new File(SystemProperties.getUserPath(), "debug.zip");
			if (outFile.exists()) {
				outFile.delete();
			}

			AEDiagnostics.flushPendingLogs();

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

			// recent OSX crashes
			File diagReportspath = new File(System.getProperty("user.home"), "Library"
					+ File.separator + "Logs" + File.separator + "DiagnosticReports");
			if (diagReportspath.isDirectory()) {
				files = diagReportspath.listFiles(new FileFilter() {
					public boolean accept(File pathname) {
						return (pathname.getName().endsWith("crash") && pathname.lastModified() > ago);
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
					addFilesToZip(out, new File[] {
						loggingFile
					});
				}
			}

			if (extraLogDirs != null) {
				for (File file : extraLogDirs) {
					files = file.listFiles(new FileFilter() {
						public boolean accept(File pathname) {
							return pathname.getName().endsWith("stackdump")
									|| pathname.getName().endsWith("log");
						}
					});
					addFilesToZip(out, files);
				}
			}

			out.close();

			if (outFile.exists()) {
				gr.file = outFile;
				return gr;
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	private static void promptUser(final boolean allowEmpty, GeneratedResults gr) {
		final Shell shell = ShellFactory.createShell(Utils.findAnyShell(), SWT.SHELL_TRIM);

		final String[] text = { null, null };
		final int[] sendMode = {-1};
		
		Utils.setShellIcon(shell);
		
		Messages.setLanguageText(shell, "UIDebugGenerator.messageask.title");
		
		shell.setLayout(new FormLayout());
		
		Label lblText = new Label(shell, SWT.NONE);
		Messages.setLanguageText(lblText, "UIDebugGenerator.messageask.text");
		
		final Text textMessage = new Text(shell, SWT.MULTI | SWT.BORDER | SWT.WRAP);
		final Text textEmail = new Text(shell, SWT.BORDER);
		
		textEmail.setMessage("optional@email.here");

		Composite cButtonsSuper = new Composite(shell, SWT.NONE);
		GridLayout gl = new GridLayout();
		cButtonsSuper.setLayout(gl);

		Composite cButtons = new Composite(cButtonsSuper, SWT.NONE);
		cButtons.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		cButtons.setLayout(new RowLayout());
		
		Button btnSendNow = new Button(cButtons, SWT.PUSH);
		btnSendNow.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (emptyCheck(textMessage, allowEmpty)) {
  				text[0] = textMessage.getText();
  				text[1] = textEmail.getText();
  				sendMode[0] = 0;
				}
				shell.dispose();
			}
		});
		Button btnSendLater = new Button(cButtons, SWT.PUSH);
		btnSendLater.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (emptyCheck(textMessage, allowEmpty)) {
  				text[0] = textMessage.getText();
  				text[1] = textEmail.getText();
  				sendMode[0] = 1;
				}
				shell.dispose();
			}
		});
		Button btnCancel = new Button(cButtons, SWT.PUSH);
		btnCancel.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				shell.dispose();
			}
		});

		if (Constants.isOSX) {
			btnCancel.moveAbove(null);
		}
		Messages.setLanguageText(btnCancel, "Button.cancel");
		Messages.setLanguageText(btnSendNow, "Button.sendNow");
		Messages.setLanguageText(btnSendLater, "Button.sendManual");

		FormData fd;
		
		fd = new FormData();
		fd.top = new FormAttachment(0, 5);
		fd.left = new FormAttachment(0, 5);
		fd.right = new FormAttachment(100, -5);
		lblText.setLayoutData(fd);

		fd = new FormData();
		fd.top = new FormAttachment(lblText, 10);
		fd.left = new FormAttachment(0, 5);
		fd.right = new FormAttachment(100, -5);
		fd.bottom = new FormAttachment(textEmail, -10);
		textMessage.setLayoutData(fd);

		fd = new FormData();
		fd.left = new FormAttachment(0, 5);
		fd.right = new FormAttachment(100, -5);
		fd.bottom = new FormAttachment(cButtonsSuper, -2);
		textEmail.setLayoutData(fd);
		
		fd = new FormData();
		fd.left = new FormAttachment(0, 5);
		fd.right = new FormAttachment(100, -5);
		fd.bottom = new FormAttachment(100, -1);
		cButtonsSuper.setLayoutData(fd);

		textMessage.setFocus();

		shell.setSize(500, 300);
		shell.layout();
		Utils.centreWindow(shell);
		shell.open();

		while (!shell.isDisposed()) {
			if (!shell.getDisplay().readAndDispatch()) {
				shell.getDisplay().sleep();
			}
		}
		if (sendMode[0] != -1) {
			gr.message = text[0];
			gr.email = text[1];
		}
		gr.sendNow = sendMode[0] == 0;
	}

	/**
	 * @param textMessage
	 * @param allowEmpty
	 * @return
	 *
	 * @since 4.5.0.3
	 */
	protected static boolean emptyCheck(Text textMessage, boolean allowEmpty) {
		if (allowEmpty) {
			return true;
		}
		if (textMessage.getText().length() > 0) {
			return true;
		}
		
		new MessageBoxShell(SWT.OK, "UIDebugGenerator.message.cancel",
				(String[]) null).open(null);

		return false;
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
	public static void obfusticateArea(Image image, Rectangle bounds) {
		GC gc = new GC(image);
		try {
			gc.setBackground(image.getDevice().getSystemColor(SWT.COLOR_WHITE));
			gc.setForeground(image.getDevice().getSystemColor(SWT.COLOR_RED));
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
	public static void obfusticateArea(Image image, Rectangle bounds, String text) {

		if (bounds.isEmpty())
			return;

		if (text == null || text.length() == 0) {
			obfusticateArea(image, bounds);
			return;
		}

		GC gc = new GC(image);
		try {
			Device device = image.getDevice();
			gc.setBackground(device.getSystemColor(SWT.COLOR_WHITE));
			gc.setForeground(device.getSystemColor(SWT.COLOR_RED));
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
	public static void obfusticateArea(Image image, Control control, String text) {
		Rectangle bounds = control.getBounds();
		Point location = Utils.getLocationRelativeToShell(control);
		bounds.x = location.x;
		bounds.y = location.y;

		obfusticateArea(image, bounds, text);
	}
}
