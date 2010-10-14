/*
 * File    : AboutWindow.java
 * Created : 18 d�c. 2003}
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.Cursors;

import java.util.Properties;

/**
 * @author Olivier
 *
 */
public class AboutWindow {

  static Image image;
  static AEMonitor	class_mon	= new AEMonitor( "AboutWindow" );
  private static Shell instance;

  public static void show(final Display display) {
    if(instance != null)
    {
        instance.open();
        return;
    }

    Properties properties = new Properties();
    try {
      properties.load(AboutWindow.class.getClassLoader().getResourceAsStream("org/gudy/azureus2/ui/swt/about.properties"));
    }
    catch (Exception e1) {
    	Debug.printStackTrace( e1 );
      return;
    }
        
    final Shell window = ShellFactory.createMainShell((Constants.isOSX)
				? SWT.DIALOG_TRIM : (SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL));
    Utils.setShellIcon(window);

    window.setText(MessageText.getString("MainWindow.about.title") + " " + Constants.AZUREUS_VERSION); //$NON-NLS-1$
    GridData gridData;
    window.setLayout(new GridLayout(3, false));

    image = new Image(display,ImageRepository.getImage("azureus_splash"),SWT.IMAGE_GRAY);
    
    Group gDevelopers = new Group(window, SWT.NULL);
    gDevelopers.setLayout(new GridLayout());
    Messages.setLanguageText(gDevelopers, "MainWindow.about.section.developers"); //$NON-NLS-1$
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gDevelopers.setLayoutData(gridData);
    
    Label label = new Label(gDevelopers, SWT.LEFT);
    label.setText(properties.getProperty("developers")); //$NON-NLS-1$ //$NON-NLS-2$
    label.setLayoutData(gridData = new GridData());
    
    final Label labelImage = new Label(window, SWT.NONE);
    labelImage.setImage(image);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
    labelImage.setLayoutData(gridData);
  
    Group gTranslators = new Group(window, SWT.NULL);
    gTranslators.setLayout(new GridLayout());
    Messages.setLanguageText(gTranslators, "MainWindow.about.section.translators"); //$NON-NLS-1$
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gTranslators.setLayoutData(gridData);
  
    Text txtTrans = new Text(gTranslators, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.NO_FOCUS);
    txtTrans.setText(properties.getProperty("translators")); //$NON-NLS-1$ //$NON-NLS-2$
    gridData = new GridData(GridData.FILL_BOTH);
    txtTrans.setLayoutData(gridData);
    txtTrans.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
    
    Group gInternet = new Group(window, SWT.NULL);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 2;
    gridLayout.makeColumnsEqualWidth = true;
    gInternet.setLayout(gridLayout);
    Messages.setLanguageText(gInternet, "MainWindow.about.section.internet"); //$NON-NLS-1$
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gridData.horizontalSpan = 2;
    gInternet.setLayoutData(gridData);
  
    Group gSys = new Group(window, SWT.NULL);
    gSys.setLayout(new GridLayout());
    Messages.setLanguageText(gSys, "MainWindow.about.section.system"); //$NON-NLS-1$
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gridData.verticalSpan = 1;
    gSys.setLayoutData(gridData);

    Text txtSysInfo = new Text(gSys, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
    txtSysInfo.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
    txtSysInfo.setText("Java " + System.getProperty("java.version") + "\n "
				+ System.getProperty("java.vendor") + "\n"
				+ "SWT v" + SWT.getVersion() + ", " + SWT.getPlatform() + "\n"
				+ System.getProperty("os.name") + " v"
				+ System.getProperty("os.version") + ", "
				+ System.getProperty("os.arch"));
    txtSysInfo.setLayoutData(gridData = new GridData(GridData.FILL_BOTH));
    if (window.getCaret() != null)
    	window.getCaret().setVisible(false);

    final String[][] link =
      { { "homepage","forumdiscussion", "wiki" }, {
    	  "http://oneswarm.org/",
          "http://forum.oneswarm.org/",
          "http://wiki.oneswarm.org/"}
    };
  
    for (int i = 0; i < link[0].length; i++) {
      final CLabel linkLabel = new CLabel(gInternet, SWT.NULL);
      linkLabel.setText(MessageText.getString("MainWindow.about.internet." + link[0][i]));
      linkLabel.setData(link[1][i]);
      linkLabel.setCursor(Cursors.handCursor);
      linkLabel.setForeground(Colors.blue);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      gridData.horizontalSpan = 1;
      linkLabel.setLayoutData(gridData);
      linkLabel.addMouseListener(new MouseAdapter() {
        public void mouseDoubleClick(MouseEvent arg0) {
        	Utils.launch((String) ((CLabel) arg0.widget).getData());
        }
        public void mouseDown(MouseEvent arg0) {
        	Utils.launch((String) ((CLabel) arg0.widget).getData());
        }
      });
    }
    
    Listener keyListener =  new Listener() {
      public void handleEvent(Event e) {
        if(e.character == SWT.ESC) {
          window.dispose();                
        }
      }
    };
    
    window.addListener(SWT.KeyUp,keyListener);
  
    window.pack();
    txtSysInfo.setFocus();
    Utils.centreWindow(window);
    window.open();

    instance = window;
    window.addDisposeListener(new DisposeListener() {
        public void widgetDisposed(DisposeEvent event) {
            instance = null;
            disposeImage();
        }
    });

    Thread updater =  new AEThread("Splash Screen Updater") {
      public void runSupport() {        
        if(image == null || image.isDisposed())
          return;
        
        final boolean finished[] = new boolean[1];
        final int[] x = new int[1];
        final int maxX = image.getBounds().width;
        final int maxY = image.getBounds().height;
        final Image imgSrc = ImageRepository.getImage("azureus_splash");
        while(!finished[0]) {
          if(image == null || image.isDisposed()) {
            finished[0] = true;
            break;
          }
          if(display == null || display.isDisposed()) {
            finished[0] = true;
            break;
          }
          Utils.execSWTThread(new AERunnable() {
            public void runSupport() {
              if(labelImage.isDisposed())
                return;
              GC gcImage = new GC(labelImage);
              gcImage.setClipping(x[0],0,1,maxY);
              gcImage.drawImage(imgSrc,0,0);
              gcImage.dispose();
              x[0]++;
              if(x[0] >= maxX) {
                finished[0] = true;
                labelImage.setImage(imgSrc);
              }
            }
          });
          try {
            Thread.sleep(30);
          }catch(Exception e) {
          	Debug.printStackTrace( e );
          }
      }
    }};
    updater.start();
  }
  
  public static void 
  disposeImage() 
  {
  	try{
  		class_mon.enter();
      ImageRepository.unloadImage("azureus_splash");
	    if(image != null && ! image.isDisposed())
	      image.dispose();
	    image = null;
  	}finally{
  		
  		class_mon.exit();
  	}
  }

}
