/*
 * Created on 29 juin 2003
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

import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;

import javax.imageio.ImageIO;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;

/**
 * @author Olivier
 *
 */
public class ImageRepository {
	private static boolean NO_IMAGES = false;

  private static Display display;
  private static final HashMap imagesToPath;
  private static final HashMap images;
  private static final HashMap registry;
  private static final String[] noCacheExtList = new String[] {".exe"};
  private static final boolean forceNoAWT = Constants.isOSX;

    static {
    images = new HashMap(150);
    imagesToPath = new HashMap(150);
    registry = new HashMap();
  }

  public static void loadImagesForSplashWindow(Display display) {
    ImageRepository.display = display; 
    addPath("org/gudy/azureus2/ui/icons/a16.png", "azureus");
    addPath("org/gudy/azureus2/ui/splash/azureus.jpg", "azureus_splash");
  }

  public static void loadImages(Display display) {
	  
	  	
	  	if(Constants.isOSX) {
	  		addPath("org/gudy/azureus2/ui/icons/az3_osx_tray_white.png", "azureus_white");
	    	addPath("org/gudy/azureus2/ui/icons/az3_osx_tray.png", "azureus_grey");
	  	}
	  	
	  	/**
	  	 * PIAMOD -- added notification icon
	  	 */
	  	addPath("org/gudy/azureus2/ui/icons/a16_notify.png", "trayicon_notify");
	  	// end
	  	
		addPath("org/gudy/azureus2/ui/icons/a32.png", "azureus32");
		addPath("org/gudy/azureus2/ui/icons/a64.png", "azureus64");
		addPath("org/gudy/azureus2/ui/icons/a128.png", "azureus128");
		addPath("org/gudy/azureus2/ui/icons/Azureus_big.png", "tray");
		addPath("org/gudy/azureus2/ui/icons/dragger.gif", "dragger");
		addPath("org/gudy/azureus2/ui/icons/folder.gif", "folder");
		addPath("org/gudy/azureus2/ui/icons/ipfilter.png", "ipfilter");
		addPath("org/gudy/azureus2/ui/icons/start.gif", "start");
		addPath("org/gudy/azureus2/ui/icons/stop.gif", "stop");
		addPath("org/gudy/azureus2/ui/icons/bar.gif", "downloadBar");
		addPath("org/gudy/azureus2/ui/icons/delete.gif", "delete");
		addPath("org/gudy/azureus2/ui/icons/lock.gif", "lock");
		addPath("org/gudy/azureus2/ui/icons/host.gif", "host");
		addPath("org/gudy/azureus2/ui/icons/publish.gif", "publish");
		addPath("org/gudy/azureus2/ui/icons/run.gif", "run");
		addPath("org/gudy/azureus2/ui/icons/details.gif", "details");
		addPath("org/gudy/azureus2/ui/icons/up.gif", "up");
		addPath("org/gudy/azureus2/ui/icons/down.gif", "down");
		addPath("org/gudy/azureus2/ui/icons/top.gif", "top");
		addPath("org/gudy/azureus2/ui/icons/bottom.gif", "bottom");
		addPath("org/gudy/azureus2/ui/icons/recheck.gif", "recheck");
		addPath("org/gudy/azureus2/ui/icons/export.gif", "export");
		addPath("org/gudy/azureus2/ui/icons/move.gif", "move");
		addPath("org/gudy/azureus2/ui/icons/add_tracker.gif", "add_tracker");
		addPath("org/gudy/azureus2/ui/icons/edit_trackers.gif", "edit_trackers");
		addPath("org/gudy/azureus2/ui/icons/columns.gif", "columns");
		addPath("org/gudy/azureus2/ui/icons/speed.gif", "speed");
		addPath("org/gudy/azureus2/ui/icons/openFolder16x12.gif","openFolderButton");
		addPath("org/gudy/azureus2/ui/icons/forcestart.gif", "forcestart");
		addPath("org/gudy/azureus2/ui/icons/greenled.gif", "greenled");
		addPath("org/gudy/azureus2/ui/icons/redled.gif", "redled");
		addPath("org/gudy/azureus2/ui/icons/yellowled.gif", "yellowled");
		addPath("org/gudy/azureus2/ui/icons/grayled.gif", "grayled");
		addPath("org/gudy/azureus2/ui/icons/donation.jpg", "donation");
		addPath("org/gudy/azureus2/ui/icons/popup.png", "popup");
		addPath("org/gudy/azureus2/ui/icons/error.gif", "error");
		addPath("org/gudy/azureus2/ui/icons/info.gif", "info");
		addPath("org/gudy/azureus2/ui/icons/warning.gif", "warning");
		addPath("org/gudy/azureus2/ui/icons/subitem.gif", "subitem");
		addPath("org/gudy/azureus2/ui/icons/comment.png", "comment");
		addPath("org/gudy/azureus2/ui/icons/foldersmall.png", "foldersmall");

		//ToolBar Icons

		addPath("org/gudy/azureus2/ui/icons/toolbar/open.gif", "cb_open");
		addPath("org/gudy/azureus2/ui/icons/toolbar/open_no_default.gif","cb_open_no_default");
		addPath("org/gudy/azureus2/ui/icons/toolbar/open_folder.gif","cb_open_folder");
		addPath("org/gudy/azureus2/ui/icons/toolbar/open_url.gif", "cb_open_url");
		addPath("org/gudy/azureus2/ui/icons/toolbar/new.gif", "cb_new");
		addPath("org/gudy/azureus2/ui/icons/toolbar/up.gif", "cb_up");
		addPath("org/gudy/azureus2/ui/icons/toolbar/down.gif", "cb_down");
		addPath("org/gudy/azureus2/ui/icons/toolbar/top.gif", "cb_top");
		addPath("org/gudy/azureus2/ui/icons/toolbar/bottom.gif", "cb_bottom");
		addPath("org/gudy/azureus2/ui/icons/toolbar/run.gif", "cb_run");
		addPath("org/gudy/azureus2/ui/icons/toolbar/start.gif", "cb_start");
		addPath("org/gudy/azureus2/ui/icons/toolbar/stop.gif", "cb_stop");
		addPath("org/gudy/azureus2/ui/icons/toolbar/remove.gif", "cb_remove");
		addPath("org/gudy/azureus2/ui/icons/toolbar/host.gif", "cb_host");
		addPath("org/gudy/azureus2/ui/icons/toolbar/publish.gif", "cb_publish");
		addPath("org/gudy/azureus2/ui/icons/toolbar/switchui.png", "cb_switch");

		//Status icons
		addPath("org/gudy/azureus2/ui/icons/status/ok.gif", "st_ok");
		addPath("org/gudy/azureus2/ui/icons/status/ko.gif", "st_ko");
		addPath("org/gudy/azureus2/ui/icons/status/stopped.gif", "st_stopped");
		addPath("org/gudy/azureus2/ui/icons/status/error.gif", "st_error");
		addPath("org/gudy/azureus2/ui/icons/status/no_tracker.gif", "st_no_tracker");
		addPath("org/gudy/azureus2/ui/icons/status/no_remote.gif", "st_no_remote");

		addPath("org/gudy/azureus2/ui/icons/status/ok_shared.gif", "st_ok_shared");
		addPath("org/gudy/azureus2/ui/icons/status/ko_shared.gif", "st_ko_shared");
		addPath("org/gudy/azureus2/ui/icons/status/error_shared.gif","st_error_shared");
		addPath("org/gudy/azureus2/ui/icons/status/stopped_shared.gif","st_stopped_shared");
		addPath("org/gudy/azureus2/ui/icons/status/no_tracker_shared.gif","st_no_tracker_shared");
		addPath("org/gudy/azureus2/ui/icons/status/no_remote_shared.gif","st_no_remote_shared");

		addPath("org/gudy/azureus2/ui/icons/status/explain.gif", "st_explain");
		addPath("org/gudy/azureus2/ui/icons/status/shared.gif", "st_shared");

		addPath("org/gudy/azureus2/ui/icons/statusbar/status_warning.gif","sb_warning");
		addPath("org/gudy/azureus2/ui/icons/statusbar/user_count.png", "sb_count");
		addPath("org/gudy/azureus2/ui/icons/statusbar/speed_up.png", "speed_up");
		addPath("org/gudy/azureus2/ui/icons/statusbar/speed_down.png", "speed_down");

		addPath("org/gudy/azureus2/ui/icons/smallx.png", "smallx");
		addPath("org/gudy/azureus2/ui/icons/smallx-gray.png", "smallx-gray");
		addPath("org/gudy/azureus2/ui/icons/working.gif", "working");
		addPath("org/gudy/azureus2/ui/icons/search.png", "search");
		
		//Progress icons
		addPath("org/gudy/azureus2/ui/icons/progress_viewer_has_error.png", "progress_error");
		addPath("org/gudy/azureus2/ui/icons/progress_viewer_has_info.png", "progress_info");
		addPath("org/gudy/azureus2/ui/icons/progress_viewer.png", "progress_viewer");
		
		if ( Utils.isAZ2UI()){ 
			addPath("org/gudy/azureus2/ui/icons/recheck.gif", "progress_retry");
			addPath("org/gudy/azureus2/ui/icons/stop.gif",    "progress_cancel");
			addPath("org/gudy/azureus2/ui/icons/delete.gif",  "progress_remove");
		}else{
			addPath("org/gudy/azureus2/ui/icons/progress_retry.png",  "progress_retry");
			addPath("org/gudy/azureus2/ui/icons/progress_cancel.png", "progress_cancel");
			addPath("org/gudy/azureus2/ui/icons/progress_remove.png", "progress_remove");
		}
		
		addPath("org/gudy/azureus2/ui/icons/transparent16x16.png", "transparent");
	}

  public static void addPath(String path, String id) {
    imagesToPath.put(id,path);
  	// 2x longer
    //loadImage(display, path, id);
  }

  private static Image loadImage(Display display, String res, String name){
    return loadImage(display,res,name,255);
  }

  private static Image loadImage(Display display, String res, String name,int alpha) {
    return loadImage(ImageRepository.class.getClassLoader(),display,res,name,alpha);
  }

  static Image onlyOneImage = null;
  private static Image loadImage(ClassLoader loader,Display display, String res, String name,int alpha) {
  	if (NO_IMAGES) {
  		if (onlyOneImage == null) {
  			onlyOneImage = new Image(display, 1, 1);
  		}
  		return onlyOneImage;
  	}
    imagesToPath.put(name,res);
    Image im = getImage(name,false);
    if(null == im) {
      try{
	      InputStream is = loader.getResourceAsStream(res);
	      if(null != is) {
	      	try { 
		        if(alpha == 255) {
		          im = new Image(display, is);
		        } else {
		          ImageData icone = new ImageData(is);
		          icone.alpha = alpha;
		          im = new Image(display,icone);
		        }
		        images.put(name, im);
	      	} catch (SWTException e) {
	      		return null;
	      	}
	      } else {
	        Debug.outNoStack("ImageRepository:loadImage:: Resource not found: " + res);
			
			im = new Image( display, 1, 1 );
			
			images.put(name, im);
	      }
      }catch( Throwable e ){
    	  
    	  	// can get here is getResourceAsStream borks, which is known to do sometimes
    	  
    	  Debug.outNoStack("ImageRepository:loadImage:: Resource not found: " + res + " - " + Debug.getNestedExceptionMessage(e));
			
		  im = new Image( display, 1, 1 );
			
		  images.put(name, im);
      }
    }
    return im;
  }

  public static void unLoadImages() {
    Iterator iter;
    iter = images.values().iterator();
    while (iter.hasNext()) {
      Image im = (Image) iter.next();
      im.dispose();
    }

    iter = registry.values().iterator();
    while (iter.hasNext()) {
      Image im = (Image) iter.next();
      if(im != null)
        im.dispose();
    }
  }

  public static Image getImage(String name) {
  	if (NO_IMAGES) {
  		if (onlyOneImage == null) {
  			onlyOneImage = new Image(display, 1, 1);
  		}
  		return onlyOneImage;
  	}
    return getImage(name,true);
  }
  
  public static InputStream
  getImageAsStream(
	 String	name )
  {
	  String path = (String) imagesToPath.get(name);

	  if ( path == null ){
		  
		  System.out.println( "ImageRepository: Unknown image name '" + name  + "'" );
		  
		  return( null );
	  }
	  
	  return( ImageRepository.class.getClassLoader().getResourceAsStream( path ));
  }
  
  private static Image getImage(String name,boolean allowLoading) {
    Image result = (Image) images.get(name);
    if(allowLoading && result == null) {
      String path = (String) imagesToPath.get(name);
      if(path != null) {
        return loadImage(display,path,name);
      }
    }
    return result;
  }

  public static Image getIconFromExtension(String ext) {
  	return getIconFromExtension(ext, false, false);
  }

  /**
     * Gets an image for a file associated with a given program
     *
     * @param program the Program
     */
  public static Image getIconFromExtension(String ext, boolean bBig, boolean minifolder) {
		Image image = null;

		try {
			String id = ext;
			
			if(bBig)
				id += "-big";
			if(minifolder)
				id += "-fold";
			
			image = (Image) images.get(id);

			if (image == null)
			{
				ImageData imageData = null;
				
				if (Constants.isWindows && bBig)
				{
					try
					{
						Class ehancerClass = Class.forName("org.gudy.azureus2.ui.swt.win32.Win32UIEnhancer");
						Method method = ehancerClass.getMethod("getBigImageData", new Class[] { String.class });
						imageData = (ImageData) method.invoke(null, new Object[] { ext });
					} catch (Exception e)
					{
						Debug.printStackTrace(e);
					}
				}
				
				if (imageData == null)
				{
					Program program = Program.findProgram(ext);
					if (program != null)
					{
						imageData = program.getImageData();
					}
				}
				
				if (imageData != null)
				{
					image = new Image(Display.getDefault(), imageData);
					if (!bBig)
						image = force16height(image);
					if(minifolder)
						image = minifolderize(image,bBig);

					images.put(id, image);
				}
			}
			if (image == null) {
				return getImage(minifolder ? "folder" : "transparent");
			}
		} catch (Throwable e) {
			// seen exceptions thrown here, due to images.get failing in Program.hashCode
			// ignore and use default icon
		}

		return image;
	}
  
  private static Image minifolderize(Image img, boolean big)
  {
		Image imgFolder = getImage(big ? "folder" : "foldersmall");
		Rectangle folderBounds = imgFolder.getBounds();
		Rectangle dstBounds = img.getBounds();
		Image tempImg = Utils.renderTransparency(Display.getCurrent(), img, imgFolder, new Point(dstBounds.width - folderBounds.width, dstBounds.height - folderBounds.height),204);
		if (tempImg != null)
		{
			img.dispose();
			img = tempImg;
		}
		return img;
  }
  
  private static Image force16height(Image image) {
  	if (image == null) {
  		return image;
  	}

  	Rectangle bounds = image.getBounds();
  	if (bounds.height != 16) {
  		Image newImage = new Image(image.getDevice(), 16, 16);
  		GC gc = new GC(newImage);
  		try {
  			if (!Constants.isUnix) {
  				// drawImage doesn't work on GTK when advanced is on
  				gc.setAdvanced(true);
  			}
  			
  			gc.drawImage(image, 0, 0, bounds.width, bounds.height, 0, 0, 16, 16);
  		} finally {
  			gc.dispose();
  		}
  		
  		image.dispose();
  		image = newImage;
  	}
  	
  	return image;
  }
  
  /**
   * @deprecated Does not account for custom or native folder icons
   * @see ImageRepository#getPathIcon(String)
   */
  public static Image
  getFolderImage()
  {
  	return getImage("folder", true);
  }

	public static Image getPathIcon(final String path) {
		return getPathIcon(path, false, false);
	}
	
	public static Image getPathIcon(final String path, boolean minifolder) {
		return getPathIcon(path, false, minifolder);
	}

    /**
	 * <p>Gets a small-sized iconic representation of the file or directory at the path</p>
	 * <p>For most platforms, the icon is a 16x16 image; weak-referencing caching is used to avoid abundant reallocation.</p>
	 * @param path Absolute path to the file or directory
	 * @return The image
	 */
	public static Image getPathIcon(final String path, boolean bBig, boolean minifolder) {
		if (path == null)
			return null;

		File file = null;
		boolean bDeleteFile = false;
		
		boolean noAWT = forceNoAWT || !bBig;
				
		try {
			file = new File(path);

			// workaround for unsupported platforms
			// notes:
			// Mac OS X - Do not mix AWT with SWT (possible workaround: use IPC/Cocoa)

			String key;
			if (file.isDirectory()) {
				if (noAWT)
					return getFolderImage();

				key = file.getPath();
			} else {
				final int lookIndex = file.getName().lastIndexOf(".");

				if (lookIndex == -1) {
					if (noAWT)
						return getFolderImage();

					key = "?!blank";
				} else {
					final String ext = file.getName().substring(lookIndex);
					key = ext;

					if (noAWT)
						return getIconFromExtension(ext, bBig, minifolder);

					// case-insensitive file systems
					for (int i = 0; i < noCacheExtList.length; i++) {
						if (noCacheExtList[i].equalsIgnoreCase(ext)) {
							key = file.getPath();
							break;
						}
					}
				}
			}
			
			if(bBig)
				key += "-big";
			if(minifolder)
				key += "-fold";

			// this method mostly deals with incoming torrent files, so there's less concern for
			// custom icons (unless user sets a custom icon in a later session)

			// other platforms - try sun.awt
			Image image = (Image) registry.get(key);
			if (image != null) {
				return image;
			}
			

			bDeleteFile = !file.exists();
			if (bDeleteFile) {
				file = File.createTempFile("AZ_", FileUtil.getExtension(path));
			}

			java.awt.Image awtImage = null;
			
			final Class sfClass = Class.forName("sun.awt.shell.ShellFolder");
			if (sfClass != null && file != null) {
				Method method = sfClass.getMethod("getShellFolder",
						new Class[] { File.class });
				if (method != null) {
					Object sfInstance = method.invoke(null, new Object[] { file });

					if (sfInstance != null) {
						method = sfClass.getMethod("getIcon", new Class[] { Boolean.TYPE });
						if (method != null) {
							awtImage = (java.awt.Image) method.invoke(sfInstance,
									new Object[] { new Boolean(bBig) });
						}
					}
				}
			}

			if (awtImage != null) {
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ImageIO.write((BufferedImage)awtImage, "png", outStream);
        final ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());

        image = new Image(Display.getDefault(), inStream);
				if (!bBig) {
					image = force16height(image);
				}
				if(minifolder)
					image = minifolderize(image,bBig);

				registry.put(key, image);

				if (bDeleteFile && file != null && file.exists()) {
					file.delete();
				}
				return image;
			}
		} catch (Exception e) {
			//Debug.printStackTrace(e);
		}
		
		if (bDeleteFile && file != null && file.exists()) {
			file.delete();
		}

		// Possible scenario: Method call before file creation
		String ext = FileUtil.getExtension(path);
		if (ext.length() == 0) {
			return getFolderImage();
		}

		return getIconFromExtension(ext, bBig, minifolder);
	}

    /**
     * <p>Gets an image with the specified canvas size</p>
     * <p>No scaling is performed on the original image, and a cached version will be used if found.</p>
     * @param name ImageRepository image resource name
     * @param canvasSize Size of image
     * @return The image
     */
    public static Image getImageWithSize(String name, Point canvasSize)
    {
        String key =
                new StringBuffer()
                    .append(name)
                    .append('.')
                    .append(canvasSize.x)
                    .append('.')
                    .append(canvasSize.y)
                .toString();

        Image newImage = (Image)images.get(key);

        if(newImage == null)
        {
            Image oldImage = getImage(name);

            if(oldImage == null)
                return null;

            newImage = new Image(Display.getCurrent(), canvasSize.x, canvasSize.y);
            GC gc = new GC(newImage);

            int x = Math.max(0, (canvasSize.x - oldImage.getBounds().width)/2);
            int y = Math.max(0, (canvasSize.y - oldImage.getBounds().height)/2);
            gc.drawImage(oldImage, x, y);

            gc.dispose();

            images.put(key, newImage);
        }

        return newImage;
    }
    
    public static void unloadImage(String name) {
      Image img = (Image) images.get(name);
      if(img != null) {
        images.remove(name);
        if(! img.isDisposed())
          img.dispose();        
      }
    }
    
    public static void unloadPathIcon(String path) {
     String key = getKey(path);
     Image img = (Image) registry.get(key);
     if(img != null) {
       registry.remove(key);
       if(! img.isDisposed())
         img.dispose();       
     }
    }
    
    private static String getKey(String path) {
      final File file = new File(path);

      String key;
      if(file.isDirectory())
      {
          key = file.getPath();
      }
      else
      {
          final int lookIndex = file.getName().lastIndexOf(".");
  
          if(lookIndex == -1)
          {
              key = "?!blank";
          }
          else
          {
              final String ext =  file.getName().substring(lookIndex);
              key = ext;
              
              // case-insensitive file systems
              for (int i = 0; i < noCacheExtList.length; i++)
              {
                  if(noCacheExtList[i].equalsIgnoreCase(ext))
                  {
                      key = file.getPath();
                  }
              }
          }
      } 
      
      return key;
    }
    
    public static void main(String[] args) {
			Display display = new Display();
			Shell shell = new Shell(display, SWT.SHELL_TRIM);
			shell.setLayout(new FillLayout(SWT.VERTICAL));
			
			final Label label = new Label(shell, SWT.BORDER);
			
			final Text text = new Text(shell, SWT.BORDER);
			text.addModifyListener(new ModifyListener() {
			
				public void modifyText(ModifyEvent e) {
					Image pathIcon = getPathIcon(text.getText(), false);
					label.setImage(pathIcon);
				}
			});
			
			
			shell.open();
			
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		}
}