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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Method;

import javax.imageio.ImageIO;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;

import com.aelitis.azureus.ui.skin.SkinProperties;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

/**
 * @author Olivier
 *
 */
public class ImageRepository
{
	private static final String[] noCacheExtList = new String[] {
		".exe"
	};

	private static final boolean forceNoAWT = Constants.isOSX || Constants.isWindows;

	/**public*/
	static void addPath(String path, String id) {
		SkinProperties[] skinProperties = ImageLoader.getInstance().getSkinProperties();
		if (skinProperties != null && skinProperties.length > 0) {
			skinProperties[0].addProperty(id, path);
		}
	}

	/** 
	 * @deprecated Use {@link ImageLoader#getImage(String)}
	 */
	public static Image getImage(String name) {
		return ImageLoader.getInstance().getImage(name);
	}

	/**
	   * Gets an image for a file associated with a given program
	   *
	   * @param program the Program
	   */
	public static Image getIconFromExtension(File file, String ext, boolean bBig,
			boolean minifolder) {
		Image image = null;

		try {
			String key = "osicon" + ext;

			if (bBig)
				key += "-big";
			if (minifolder)
				key += "-fold";

			image = ImageLoader.getInstance().getImage(key);
			if (ImageLoader.isRealImage(image)) { 
				return image;
			}

			ImageLoader.getInstance().releaseImage(key);
			image = null;

			ImageData imageData = null;

			if (Constants.isWindows) {
				try {
					//Image icon = Win32UIEnhancer.getFileIcon(new File(path), big);
					
					Class<?> enhancerClass = Class.forName("org.gudy.azureus2.ui.swt.win32.Win32UIEnhancer");
					Method method = enhancerClass.getMethod("getFileIcon",
							new Class[] {
								File.class,
								boolean.class
							});
					image = (Image) method.invoke(null, new Object[] {
						file,
						bBig
					});
					if (image != null) {
						if (!bBig)
							image = force16height(image);
						if (minifolder)
							image = minifolderize(file.getParent(), image, bBig);
						ImageLoader.getInstance().addImageNoDipose(key, image);
						return image;
					}
				} catch (Exception e) {
					Debug.printStackTrace(e);
				}
			} else if (Utils.isCocoa) {
				try {
					Class<?> enhancerClass = Class.forName("org.gudy.azureus2.ui.swt.osx.CocoaUIEnhancer");
					Method method = enhancerClass.getMethod("getFileIcon",
							new Class[] {
								String.class,
								int.class
							});
					image = (Image) method.invoke(null, new Object[] {
						file.getAbsolutePath(),
						(int) (bBig ? 128 : 16)
					});
					if (image != null) {
						if (!bBig)
							image = force16height(image);
						if (minifolder)
							image = minifolderize(file.getParent(), image, bBig);
						ImageLoader.getInstance().addImageNoDipose(key, image);
						return image;
					}
				} catch (Throwable t) {
					Debug.printStackTrace(t);
				}
			}

			if (imageData == null) {
				Program program = Program.findProgram(ext);
				if (program != null) {
					imageData = program.getImageData();
				}
			}

			if (imageData != null) {
				image = new Image(Display.getDefault(), imageData);
				if (!bBig)
					image = force16height(image);
				if (minifolder)
					image = minifolderize(file.getParent(), image, bBig);

				ImageLoader.getInstance().addImageNoDipose(key, image);
			}
		} catch (Throwable e) {
			// seen exceptions thrown here, due to images.get failing in Program.hashCode
			// ignore and use default icon
		}

		if (image == null) {
			return getImage(minifolder ? "folder" : "transparent");
		}
		return image;
	}

	private static Image minifolderize(String path, Image img, boolean big) {
		Image imgFolder =  getImage(big ? "folder" : "foldersmall");
		Rectangle folderBounds = imgFolder.getBounds();
		Rectangle dstBounds = img.getBounds();
		Image tempImg = Utils.renderTransparency(Display.getCurrent(), img,
				imgFolder, new Point(dstBounds.width - folderBounds.width,
						dstBounds.height - folderBounds.height), 204);
		if (tempImg != null) {
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
	* <p>Gets an iconic representation of the file or directory at the path</p>
	* <p>For most platforms, the icon is a 16x16 image; weak-referencing caching is used to avoid abundant reallocation.</p>
	* @param path Absolute path to the file or directory
	* @return The image
	*/
	public static Image getPathIcon(final String path, boolean bBig,
			boolean minifolder) {
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
				if (noAWT) {
					if (Constants.isWindows || Utils.isCocoa) {
						return getIconFromExtension(file, "-folder", bBig, false);
					}
					return getImage("folder");
				}

				key = file.getPath();
			} else {
				final int idxDot = file.getName().lastIndexOf(".");

				if (idxDot == -1) {
					if (noAWT) {
						return getImage("folder");
					}

					key = "?!blank";
				} else {
					final String ext = file.getName().substring(idxDot);
					key = ext;
					
					if (noAWT)
						return getIconFromExtension(file, ext, bBig, minifolder);

					// case-insensitive file systems
					for (int i = 0; i < noCacheExtList.length; i++) {
						if (noCacheExtList[i].equalsIgnoreCase(ext)) {
							key = file.getPath();
							break;
						}
					}
				}
			}

			if (bBig)
				key += "-big";
			if (minifolder)
				key += "-fold";
			
			key = "osicon" + key;

			// this method mostly deals with incoming torrent files, so there's less concern for
			// custom icons (unless user sets a custom icon in a later session)

			// other platforms - try sun.awt
			Image image = ImageLoader.getInstance().getImage(key);
			if (ImageLoader.isRealImage(image)) { 
				return image;
			}
			ImageLoader.getInstance().releaseImage(key);
			image = null;

			bDeleteFile = !file.exists();
			if (bDeleteFile) {
				file = File.createTempFile("AZ_", FileUtil.getExtension(path));
			}

			java.awt.Image awtImage = null;

			try {
  			final Class sfClass = Class.forName("sun.awt.shell.ShellFolder");
  			if (sfClass != null && file != null) {
  				Method method = sfClass.getMethod("getShellFolder", new Class[] {
  					File.class
  				});
  				if (method != null) {
  					Object sfInstance = method.invoke(null, new Object[] {
  						file
  					});
  
  					if (sfInstance != null) {
  						method = sfClass.getMethod("getIcon", new Class[] {
  							Boolean.TYPE
  						});
  						if (method != null) {
  							awtImage = (java.awt.Image) method.invoke(sfInstance,
  									new Object[] {
  										new Boolean(bBig)
  									});
  						}
  					}
  				}
  			}
			} catch (Throwable e) {
			}

			if (awtImage != null) {
				final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
				ImageIO.write((BufferedImage) awtImage, "png", outStream);
				final ByteArrayInputStream inStream = new ByteArrayInputStream(
						outStream.toByteArray());

				image = new Image(Display.getDefault(), inStream);
				if (!bBig) {
					image = force16height(image);
				}
				if (minifolder)
					image = minifolderize(file.getParent(), image, bBig);

				
				ImageLoader.getInstance().addImageNoDipose(key, image);

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
			return getImage("folder");
		}

		return getIconFromExtension(file, ext, bBig, minifolder);
	}

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display, SWT.SHELL_TRIM);
		shell.setLayout(new FillLayout(SWT.VERTICAL));

		final Label label = new Label(shell, SWT.BORDER);

		final Text text = new Text(shell, SWT.BORDER);
		text.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				Image pathIcon = getPathIcon(text.getText(), false, false);
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