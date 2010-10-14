/*
 * Created on 15 avr. 2005
 * Created by Olivier Chalouhi
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
package org.gudy.azureus2.ui.swt.welcome;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderFactoryImpl;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

import com.aelitis.azureus.ui.swt.utils.ColorCache;

import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;

public class WelcomeWindow {
	
	private static final String URL_WHATSNEW = "http://web.azureusplatform.com/releasenotes";
  
  private static final String lineSeparator = System.getProperty ("line.separator");
  
  Display display;
  Shell shell;
  Color black,white,light,grey,green,blue,fg,bg;
  String sWhatsNew;
  Font monospace; 
  
  public WelcomeWindow(Shell parentShell) {
  	try {
  		init(parentShell);
  	} catch (Throwable t) {
  	}
  }

  public void init (Shell parentShell) {
    shell = ShellFactory.createShell(parentShell, SWT.BORDER | SWT.TITLE | SWT.CLOSE | SWT.RESIZE);
    Utils.setShellIcon(shell);
    if(Constants.isOSX)
    	monospace = new Font(shell.getDisplay(),"Courier",12,SWT.NORMAL);
    else 
    	monospace = new Font(shell.getDisplay(),"Courier New",8,SWT.NORMAL);
	
    shell.setText(MessageText.getString("window.welcome.title", new String[]{ Constants.AZUREUS_VERSION }));
    
    display = shell.getDisplay();
    
    GridLayout layout = new GridLayout();
    shell.setLayout(layout);
    
    GridData data;
    
    Composite cWhatsNew = new Composite(shell, SWT.BORDER);
    data = new GridData(GridData.FILL_BOTH);
    cWhatsNew.setLayoutData(data);
    cWhatsNew.setLayout(new FillLayout());
    
    Button bClose = new Button(shell,SWT.PUSH);
    bClose.setText(MessageText.getString("Button.close"));
    data = new GridData();
    data.widthHint = 70;
    data.horizontalAlignment = Constants.isOSX ? SWT.CENTER : SWT.RIGHT;
    bClose.setLayoutData(data);
    
    Listener closeListener = new Listener() {
      public void handleEvent(Event event) {
        close();
      }
    };
    
    bClose.addListener(SWT.Selection, closeListener);
    shell.addListener(SWT.Close,closeListener);
    
	shell.setDefaultButton( bClose );
	
	shell.addListener(SWT.Traverse, new Listener() {	
		public void handleEvent(Event e) {
			if ( e.character == SWT.ESC){
				close();
			}
		}
	});
	
    shell.setSize(750,500);
    Utils.centreWindow(shell);
    shell.layout();
    shell.open();    
    fillWhatsNew(cWhatsNew);
  }
  
  private void fillWhatsNew(Composite cWhatsNew) {
  	String helpFile;

  	Label label = new Label(cWhatsNew, SWT.CENTER);
  	label.setText(MessageText.getString("installPluginsWizard.details.loading"));
  	shell.layout(true, true);
  	shell.update();
  	
		// Support external URLs for what's new
		helpFile = MessageText.getString("window.welcome.file");
		if (sWhatsNew == null || sWhatsNew.length() == 0) {
			if (helpFile.toLowerCase().startsWith(Constants.SF_WEB_SITE)) {
				sWhatsNew = getWhatsNew(helpFile);
				if (shell.isDisposed()) {
					return;
				}
			}
		}

		if (sWhatsNew == null || sWhatsNew.length() == 0) {
  		helpFile = URL_WHATSNEW + "?version=" + Constants.AZUREUS_VERSION
  				+ "&locale=" + Locale.getDefault().toString() + "&ui="
  				+ COConfigurationManager.getStringParameter("ui");
  
  		sWhatsNew = getWhatsNew(helpFile);
  		if (shell.isDisposed()) {
  			return;
  		}
		}
		
		if (sWhatsNew == null || sWhatsNew.length() == 0) {
			InputStream stream;
			stream = getClass().getResourceAsStream(helpFile);
			if (stream == null) {
				String helpFullPath = "/org/gudy/azureus2/internat/whatsnew/" + helpFile;
				stream = getClass().getResourceAsStream(helpFullPath);
			}
			if (stream == null) {
				stream = getClass().getResourceAsStream("/ChangeLog.txt");
			}
			if (stream == null) {
				sWhatsNew = "Welcome Window: Error loading resource: " + helpFile;
			} else {
				try {
					sWhatsNew = FileUtil.readInputStreamAsString(stream, 65535, "utf8");
					stream.close();
				} catch (IOException e) {
					Debug.out(e);
				}
			}
		}

		if (sWhatsNew.indexOf("<html") >= 0 || sWhatsNew.indexOf("<HTML") >= 0) {
			try {
				Browser browser = new Browser(cWhatsNew, SWT.NONE);
				browser.setText(sWhatsNew);
			} catch (Throwable t) {
				try {
					File tempFile = File.createTempFile("AZU", ".html");
					tempFile.deleteOnExit();
					FileUtil.writeBytesAsFile(tempFile.getAbsolutePath(),
							sWhatsNew.getBytes("utf8"));
					Utils.launch(tempFile.getAbsolutePath());
					shell.dispose();
					return;
				} catch (IOException e) {
				}
			}
		} else {

			StyledText helpPanel = new StyledText(cWhatsNew, SWT.VERTICAL | SWT.HORIZONTAL);

			helpPanel.setEditable(false);
			try {
				helpPanel.setRedraw(false);
				helpPanel.setWordWrap(false);
				helpPanel.setFont(monospace);

				black = ColorCache.getColor(display, 0, 0, 0);
				white = ColorCache.getColor(display, 255, 255, 255);
				light = ColorCache.getColor(display, 200, 200, 200);
				grey = ColorCache.getColor(display, 50, 50, 50);
				green = ColorCache.getColor(display, 30, 80, 30);
				blue = ColorCache.getColor(display, 20, 20, 80);
				int style;
				boolean setStyle;

				helpPanel.setForeground(grey);

				String[] lines = sWhatsNew.split("\\r?\\n");
				for (int i = 0; i < lines.length; i++) {
					String line = lines[i];

					setStyle = false;
					fg = grey;
					bg = white;
					style = SWT.NORMAL;

					char styleChar;
					String text;

					if (line.length() < 2) {
						styleChar = ' ';
						text = " " + lineSeparator;
					} else {
						styleChar = line.charAt(0);
						text = line.substring(1) + lineSeparator;
					}

					switch (styleChar) {
						case '*':
							text = "  * " + text;
							fg = green;
							setStyle = true;
							break;
						case '+':
							text = "     " + text;
							fg = black;
							bg = light;
							style = SWT.BOLD;
							setStyle = true;
							break;
						case '!':
							style = SWT.BOLD;
							setStyle = true;
							break;
						case '@':
							fg = blue;
							setStyle = true;
							break;
						case '$':
							bg = blue;
							fg = white;
							style = SWT.BOLD;
							setStyle = true;
							break;
						case ' ':
							text = "  " + text;
							break;
							
						default:
							text = styleChar + text;
					}

					helpPanel.append(text);

					if (setStyle) {
						int lineCount = helpPanel.getLineCount() - 1;
						int charCount = helpPanel.getCharCount();
						//          System.out.println("Got Linecount " + lineCount + ", Charcount " + charCount);

						int lineOfs = helpPanel.getOffsetAtLine(lineCount - 1);
						int lineLen = charCount - lineOfs;
						//          System.out.println("Setting Style : " + lineOfs + ", " + lineLen);
						helpPanel.setStyleRange(new StyleRange(lineOfs, lineLen, fg, bg,
								style));
						helpPanel.setLineBackground(lineCount - 1, 1, bg);
					}
				}

				helpPanel.setRedraw(true);
			} catch (Exception e) {
				System.out.println("Unable to load help contents because:" + e);
				//e.printStackTrace();
			}
		}
		
		label.dispose();
		shell.layout(true, true);
	}
  
  private String getWhatsNew(final String url) {
		final String[] s = new String[1];
		new AEThread("getWhatsNew", true) {

			public void runSupport() {

				ResourceDownloaderFactory rdf = ResourceDownloaderFactoryImpl.getSingleton();
				try {
					ResourceDownloader rd = rdf.create(new URL(url));
					InputStream is = rd.download();

					int length = is.available();

					byte data[] = new byte[length];

					is.read(data);

					is.close();
					
					s[0] = new String(data);

				} catch (Exception e) {
					Debug.out(e);
					s[0] = "";
				}

				if (!shell.isDisposed()) {
					shell.getDisplay().wake();
				}
			}

		}.start();
		
		while (!shell.isDisposed() && s[0] == null) {
			if (!shell.getDisplay().readAndDispatch()) {
				shell.getDisplay().sleep();
			}
		}

		return s[0];
	}
  
  private void close() {
	monospace.dispose();
    shell.dispose();
  }
  
  public static void main(String[] args) {
  	//Locale.setDefault(new Locale("nl", "NL"));
  	//MessageText.changeLocale(new Locale("nl", "NL"));
  	System.out.println(Locale.getDefault().getCountry());
		new WelcomeWindow(null);
		Display display = Display.getDefault();
		while (true) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
}
