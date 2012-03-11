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

import java.util.Properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.mainwindow.*;
import org.gudy.azureus2.update.CorePatchLevel;

import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

/**
 * @author Olivier
 *
 */
public class AboutWindow {
	private final static String IMG_SPLASH = "azureus_splash";

  static Image image;
  static AEMonitor	class_mon	= new AEMonitor( "AboutWindow" );
  private static Shell instance;
	private static Image imgSrc;
	private static int paintColorTo = 0;

  public static void show() {
  	Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				_show();
			}
		});
  }

  private static void _show() {
    if(instance != null)
    {
        instance.open();
        return;
    }
    
    paintColorTo = 0;

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
    final Display display = window.getDisplay();

    window.setText(MessageText.getString("MainWindow.about.title") + " " + Constants.AZUREUS_VERSION); //$NON-NLS-1$
    GridData gridData;
    window.setLayout(new GridLayout(3, false));

    ImageLoader imageLoader = ImageLoader.getInstance();
    imgSrc = imageLoader.getImage(IMG_SPLASH);
    if (imgSrc != null) {
      int w = imgSrc.getBounds().width;
      int ow = w;
      if (w > 350) {
      	w = 350;
      }
      int h = imgSrc.getBounds().height;
      
      Image imgGray = new Image(display, imageLoader.getImage(IMG_SPLASH),
					SWT.IMAGE_GRAY);
      imageLoader.releaseImage(IMG_SPLASH);
      GC gc = new GC(imgGray);
      if (Constants.isOSX) {
      	gc.drawImage(imgGray, (w - ow) / 2, 0);
      } else {
      	gc.copyArea(0, 0, ow, h, (w - ow) / 2, 0);
      }
      gc.dispose();
      
      Image image2 = new Image(display, w, h);
      gc = new GC(image2);
      gc.setBackground(window.getBackground());
      gc.fillRectangle(image2.getBounds());
      gc.dispose();
      image = Utils.renderTransparency(display, image2, imgGray, new Point(0, 0), 180);
      image2.dispose();
      imgGray.dispose();
    }
    
    Group gDevelopers = new Group(window, SWT.NULL);
    gDevelopers.setLayout(new GridLayout());
    Messages.setLanguageText(gDevelopers, "MainWindow.about.section.developers"); //$NON-NLS-1$
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gDevelopers.setLayoutData(gridData);
    
    Label label = new Label(gDevelopers, SWT.LEFT);
    label.setText(properties.getProperty("developers")); //$NON-NLS-1$ //$NON-NLS-2$
    label.setLayoutData(gridData = new GridData());
    
    final Canvas labelImage = new Canvas(window, SWT.DOUBLE_BUFFERED);
    //labelImage.setImage(image);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
    Rectangle imgBounds = image.getBounds();
    gridData.widthHint = 300;
    labelImage.setLayoutData(gridData);
    labelImage.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				Rectangle boundsColor = imgSrc.getBounds();
				int ofs = (labelImage.getSize().x - boundsColor.width) / 2;
				if (paintColorTo > 0) {
					e.gc.drawImage(imgSrc, 0, 0, paintColorTo, boundsColor.height, ofs, 20, paintColorTo, boundsColor.height);
				}
				Rectangle imgBounds = image.getBounds();
				if (imgBounds.width - paintColorTo - 1 > 0) {
					e.gc.drawImage(image, 
							paintColorTo + 1, 0, imgBounds.width - paintColorTo - 1, imgBounds.height, 
							paintColorTo + 1 + ofs, 20, imgBounds.width - paintColorTo - 1, imgBounds.height);
				}
			}
		});
  
    Group gTranslators = new Group(window, SWT.NULL);
    GridLayout gl = new GridLayout();
    gl.marginHeight = 2;
    gl.marginWidth = 0;
    gTranslators.setLayout(gl);
    Messages.setLanguageText(gTranslators, "MainWindow.about.section.translators"); //$NON-NLS-1$
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gTranslators.setLayoutData(gridData);
  
    Text txtTrans = new Text(gTranslators, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.NO_FOCUS);
    txtTrans.setText(properties.getProperty("translators")); //$NON-NLS-1$ //$NON-NLS-2$
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.heightHint = txtTrans.computeSize(SWT.DEFAULT, SWT.DEFAULT).y + 10;
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
				+ System.getProperty("os.arch") + "\n"
				+ Constants.APP_NAME.charAt(0) + Constants.AZUREUS_VERSION + (Constants.AZUREUS_SUBVER.length()==0?"":("-"+Constants.AZUREUS_SUBVER)) + "/" + CorePatchLevel.getCurrentPatchLevel() + " " 
				+ COConfigurationManager.getStringParameter("ui"));
    txtSysInfo.setLayoutData(gridData = new GridData(GridData.FILL_BOTH));
    if (window.getCaret() != null)
    	window.getCaret().setVisible(false);

    final String[][] link =
      { { "homepage", "sourceforge", "sourceforgedownloads", "bugreports", "forumdiscussion", "wiki" }, {
          "http://www.vuze.com",
          "http://azureus.sourceforge.net",
          "http://sourceforge.net/project/showfiles.php?group_id=84122",
          "http://forum.vuze.com/forum.jspa?forumID=124",
          "http://forum.vuze.com",
          Constants.AZUREUS_WIKI }
    };
  
    for (int i = 0; i < link[0].length; i++) {
      final CLabel linkLabel = new CLabel(gInternet, SWT.NULL);
      linkLabel.setText(MessageText.getString("MainWindow.about.internet." + link[0][i]));
      linkLabel.setData(link[1][i]);
      linkLabel.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
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

    AEThread2 updater =  new AEThread2("Splash Screen Updater", true) {
      public void run() {        
        if(image == null || image.isDisposed())
          return;
        
        final int maxX = image.getBounds().width;
        final int maxY = image.getBounds().height;
        while(paintColorTo < maxX) {
          if(image == null || image.isDisposed()) {
            paintColorTo = maxX;
            break;
          }
          if(display.isDisposed()) {
            paintColorTo = maxX;
            break;
          }
          Utils.execSWTThread(new AERunnable() {
            public void runSupport() {
              if(labelImage.isDisposed())
                return;
              paintColorTo++;
      				Rectangle boundsColor = imgSrc.getBounds();
      				int ofs = (labelImage.getSize().x - boundsColor.width) / 2;
              labelImage.redraw(paintColorTo - 1 + ofs, 20, 2, maxY, true);
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
	    if(image != null && ! image.isDisposed()) {
	      image.dispose();
	    }
	    ImageLoader imageLoader = ImageLoader.getInstance();
	    imageLoader.releaseImage(IMG_SPLASH);
	    image = null;
	    imgSrc = null;
  	}finally{
  		
  		class_mon.exit();
  	}
  }

  public static void main(String[] args) {
  	try {
  		new Display();
  		Colors.getInstance();
			SWTThread.createInstance(null);
			show();
		} catch (SWTThreadAlreadyInstanciatedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
