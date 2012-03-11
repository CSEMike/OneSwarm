/*
 * Created on Jun 7, 2006 2:31:26 PM
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
package com.aelitis.azureus.ui.swt.imageloader;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.skin.SkinProperties;
import com.aelitis.azureus.ui.skin.SkinPropertiesImpl;
import com.aelitis.azureus.ui.utils.ImageBytesDownloader;

/**
 * Loads images from a skinProperty object.  
 * <p>
 * Will look for special suffixes (over, down, disabled) and try to
 * load resources using base key and suffix. ie. loadImage("foo-over") when
 * foo=image.png, will load image-over.png
 * <p>
 * Will also create own disabled images if base image found and no disabled
 * image found.  Disabled opacity can be set via imageloader.disabled-opacity 
 * key
 * 
 * @author TuxPaper
 * @created Jun 7, 2006
 *
 */
public class ImageLoader
	implements AEDiagnosticsEvidenceGenerator
{
	private static ImageLoader instance;

	private static final boolean DEBUG_UNLOAD = false;

	private static final boolean DEBUG_REFCOUNT = false;

	private static final int GC_INTERVAL = 60 * 1000;

	private final String[] sSuffixChecks = {
		"-over",
		"-down",
		"-disabled",
		"-selected",
		"-gray",
	};

	private Display display;

	public static Image noImage;

	private final ConcurrentHashMap<String, ImageLoaderRefInfo> _mapImages;

	private final ArrayList<String> notFound;

	private CopyOnWriteArrayList<SkinProperties> skinProperties;

	//private final ClassLoader classLoader;

	private int disabledOpacity;

	private Set<String>		cached_resources = new HashSet<String>();
	
	private File cache_dir = new File(SystemProperties.getUserPath(), "cache" );
	

	public static ImageLoader getInstance() {
		if (ImageLoader.instance == null) {
			ImageLoader.instance = new ImageLoader(Display.getDefault(), null);
			// always add az2 icons to instance
			SkinPropertiesImpl skinProperties = new SkinPropertiesImpl(
					ImageRepository.class.getClassLoader(),
					"org/gudy/azureus2/ui/icons/", "icons.properties");
			ImageLoader.instance.addSkinProperties(skinProperties);
		}
		return ImageLoader.instance;
	}

	public ImageLoader(/*ClassLoader classLoader,*/Display display,
			SkinProperties skinProperties) {
		//this.classLoader = classLoader;
		
		File[]	files = cache_dir.listFiles();
		
		if ( files != null ){
			for (File f: files ){
				String	name = f.getName();
				if ( name.endsWith( ".ico" )){
					cached_resources.add( name );
				}
			}
		}
		
		_mapImages = new ConcurrentHashMap<String, ImageLoaderRefInfo>();
		notFound = new ArrayList<String>();
		this.display = display;
		this.skinProperties = new CopyOnWriteArrayList<SkinProperties>();
		addSkinProperties(skinProperties);

		AEDiagnostics.addEvidenceGenerator(this);
		if (GC_INTERVAL > 0) {
			SimpleTimer.addPeriodicEvent("GC_ImageLoader", GC_INTERVAL,
					new TimerEventPerformer() {
						public void perform(TimerEvent event) {
							collectGarbage();
						}
					});
		}
	}

	/*
	private Image loadImage(Display display, String key) {
		for (SkinProperties sp : skinProperties) {
			String value = sp.getStringValue(key);
			if (value != null) {
				return loadImage(display, sp.getClassLoader(), value, key);
			}
		}
		return loadImage(display, null, null, key);
	}
	*/

	private Image[] findResources(String sKey) {
		if (Collections.binarySearch(notFound, sKey) >= 0) {
			return null;
		}

		for (int i = 0; i < sSuffixChecks.length; i++) {
			String sSuffix = sSuffixChecks[i];

			if (sKey.endsWith(sSuffix)) {
				//System.out.println("YAY " + sSuffix + " for " + sKey);
				String sParentName = sKey.substring(0, sKey.length() - sSuffix.length());
				/*
				Image[] images = getImages(sParentName);
				if (images != null && images.length > 0 && isRealImage(images[0])) {
					return images;
				}
				*/
				/**/
				String[] sParentFiles = null;
				ClassLoader cl = null;
				for (SkinProperties sp : skinProperties) {
					sParentFiles = sp.getStringArray(sParentName);
					if (sParentFiles != null) {
						cl = sp.getClassLoader();
						break;
					}
				}
				if (sParentFiles != null) {
					boolean bFoundOne = false;
					Image[] images = parseValuesString(cl, sKey, sParentFiles, sSuffix);
					if (images != null) {
						for (int j = 0; j < images.length; j++) {
							Image image = images[j];
							if (isRealImage(image)) {
								bFoundOne = true;
							}
						}
						if (!bFoundOne) {
							for (int j = 0; j < images.length; j++) {
								Image image = images[j];
								if (isRealImage(image)) {
									image.dispose();
								}
							}
						} else {
							return images;
						}
					}
				} else {
					// maybe there's another suffix..
					Image[] images = findResources(sParentName);
					if (images != null) {
						return images;
					}
				}
				/**/
			}
		}

		int i = Collections.binarySearch(notFound, sKey) * -1 - 1;
		if (i >= 0) {
			notFound.add(i, sKey);
		}
		return null;
	}

	/**
	 * @param values
	 * @param suffix 
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	private Image[] parseValuesString(ClassLoader cl, String sKey,
			String[] values, String suffix) {
		Image[] images = null;

		int splitX = 0;
		int locationStart = 0;
		int useIndex = -1; // all
		if (values[0].equals("multi") && values.length > 2) {
			splitX = Integer.parseInt(values[1]);
			locationStart = 2;
		} else if (values[0].equals("multi-index") && values.length > 3) {
			splitX = Integer.parseInt(values[1]);
			useIndex = Integer.parseInt(values[2]);
			locationStart = 3;
		}

		if (locationStart == 0 || splitX <= 0) {
			images = new Image[values.length];
			for (int i = 0; i < values.length; i++) {
				int index = values[i].lastIndexOf('.');
				if (index > 0) {
					String sTryFile = values[i].substring(0, index) + suffix
							+ values[i].substring(index);
					images[i] = loadImage(display, cl, sTryFile, sKey);

					if (images[i] == null) {
						sTryFile = values[i].substring(0, index) + suffix.replace('-', '_')
								+ values[i].substring(index);
						images[i] = loadImage(display, cl, sTryFile, sKey);
					}
				}

				if (images[i] == null) {
					images[i] = getNoImage();
				}
			}
		} else {
			Image image = null;
			
			String image_key = null;	// if image in repository then this will be non-null

			String origFile = values[locationStart];
			int index = origFile.lastIndexOf('.');
						
			if (index > 0) {
				if (useIndex == -1) {
					String sTryFile = origFile.substring(0, index) + suffix
							+ origFile.substring(index);
					image = loadImage(display, cl, sTryFile, sKey);

						// not in repo
					
					if (image == null) {
						sTryFile = origFile.substring(0, index) + suffix.replace('-', '_')
								+ origFile.substring(index);
						image = loadImage(display, cl, sTryFile, sKey);
						
							// not in repo
					}
				} else {
					String sTryFile = origFile.substring(0, index) + suffix
							+ origFile.substring(index);

						// check the cache to see if full image is in there
						
					image = getImageFromMap( sTryFile );
					
					if ( image == null ){
						
						image = loadImage(display, cl, sTryFile, sTryFile);
						
						if ( isRealImage(image)){
							
							image_key = sTryFile;
							
							addImage(image_key, image);
							
						} else if (sTryFile.matches(".*[-_]disabled.*")) {
							
							String sTryFileNonDisabled = sTryFile.replaceAll("[-_]disabled", "");
							
							image = getImageFromMap(sTryFileNonDisabled);
							
							if (!isRealImage(image)) {
																
								image = loadImage(display, cl, sTryFileNonDisabled,
										sTryFileNonDisabled);
								
								if ( isRealImage(image)) {
									
									addImage(sTryFileNonDisabled, image);
						
								}
							}
							
							if ( isRealImage(image)){
																
								image = fadeImage(image);
								
								image_key = sTryFile;
								
								addImage(image_key, image);
								
								releaseImage(sTryFileNonDisabled);
							}
						}
					}else{
						
						image_key = sTryFile;
					}
				}
			}
			
			if ( !isRealImage(image)) {
				
				String	temp_key = sKey + "-[multi-load-temp]";
				
				image = getImageFromMap( temp_key );
				
				if ( isRealImage(image)){
					
					image_key = temp_key;
					
				}else{
					
					image = loadImage(display, cl, values[locationStart], sKey);
					
					if ( isRealImage(image)) {
						
						image_key = temp_key;
						
						addImage( image_key, image );
					}
				}
			}

			if (isRealImage(image)) {
				Rectangle bounds = image.getBounds();
				if (useIndex == -1) {
					images = new Image[(bounds.width + splitX - 1) / splitX];
					for (int i = 0; i < images.length; i++) {
						Image imgBG = Utils.createAlphaImage(display, splitX, bounds.height,
								(byte) 0);
						int pos = i * splitX;
						try {
							images[i] = Utils.blitImage(display, image, new Rectangle(pos, 0,
									Math.min(splitX, bounds.width - pos), bounds.height), imgBG,
									new Point(0, 0));
						} catch (Exception e) {
							Debug.out(e);
						}
						imgBG.dispose();
					}
				} else {
					images = new Image[1];
					Image imgBG = Utils.createAlphaImage(display, splitX, bounds.height, (byte) 0);
					try {
						int pos = useIndex * splitX;
						images[0] = Utils.blitImage(display, image, new Rectangle(pos, 0,
								Math.min(splitX, bounds.width - pos), bounds.height), imgBG,
								new Point(0, 0));
					} catch (Exception e) {
						Debug.out(e);
					}
					imgBG.dispose();
				}

				if ( image_key != null ){
					
					releaseImage(image_key);
					
				}else if ( image != null ){
					
					image.dispose();
				}
			}
		}

		return images;
	}

	private Image loadImage(Display display, ClassLoader cl, String res,
			String sKey) {
		Image img = null;
		
		//System.out.println("LoadImage " + sKey + " - " + res);
		if (res == null) {
			for (int i = 0; i < sSuffixChecks.length; i++) {
				String sSuffix = sSuffixChecks[i];

				if (sKey.endsWith(sSuffix)) {
					//System.out.println("Yay " + sSuffix + " for " + sKey);
					String sParentName = sKey.substring(0, sKey.length()
							- sSuffix.length());
					String sParentFile = null;
					for (SkinProperties sp : skinProperties) {
						sParentFile = sp.getStringValue(sParentName);
						if (sParentFile != null) {
							if (cl == null) {
								cl = sp.getClassLoader();
							}
							break;
						}
					}
					if (sParentFile != null) {
						int index = sParentFile.lastIndexOf('.');
						if (index > 0) {
							String sTryFile = sParentFile.substring(0, index) + sSuffix
									+ sParentFile.substring(index);
							img = loadImage(display, cl, sTryFile, sKey);

							if (img != null) {
								break;
							}

							sTryFile = sParentFile.substring(0, index)
									+ sSuffix.replace('-', '_') + sParentFile.substring(index);
							img = loadImage(display, cl, sTryFile, sKey);

							if (img != null) {
								break;
							}
						}
					}
				}
			}
		}

		if (img == null) {
			try {
				if (cl != null) {
					InputStream is = cl.getResourceAsStream(res);
					if (is != null) {
						img = new Image(display, is);
						
						//System.out.println("Loaded image from " + res + " via " + Debug.getCompressedStackTrace());
						is.close();
					}
				}

				if (img == null) {
					// don't do on sKey.endsWith("-disabled") because caller parseValueString
					// requires a failure so it can retry with _disabled.  If that fails,
					// we'll get here (stupid, I know)
					if (res.contains("_disabled.")) {
						String id = sKey.substring(0, sKey.length() - 9);
						Image imgToFade = getImage(id);
						if (isRealImage(imgToFade)) {
							img = fadeImage(imgToFade);
						}
						releaseImage(id);
					}else if (sKey.endsWith("-gray")) {
						String id = sKey.substring(0, sKey.length() - 5);
						Image imgToGray = getImage(id);
						if (isRealImage(imgToGray)) {
							img = new Image( display, imgToGray, SWT.IMAGE_GRAY );
						}
						releaseImage(id);
					}
					//System.err.println("ImageRepository:loadImage:: Resource not found: " + res);
				}
			} catch (Throwable e) {
				System.err.println("ImageRepository:loadImage:: Resource not found: "
						+ res + "\n" + e);
			}
		}

		return img;
	}

	private Image fadeImage(Image imgToFade) {
		ImageData imageData = imgToFade.getImageData();
		Image img;
		// decrease alpha
		if (imageData.alphaData != null) {
			if (disabledOpacity == -1) {
				for (int i = 0; i < imageData.alphaData.length; i++) {
					imageData.alphaData[i] = (byte) ((imageData.alphaData[i] & 0xff) >> 3);
				}
			} else {
				for (int i = 0; i < imageData.alphaData.length; i++) {
					imageData.alphaData[i] = (byte) ((imageData.alphaData[i] & 0xff)
							* disabledOpacity / 100);
				}
			}
			img = new Image(display, imageData);
		} else {
			Rectangle bounds = imgToFade.getBounds();
			Image bg = Utils.createAlphaImage(display, bounds.width,
					bounds.height, (byte) 0);

			img = Utils.renderTransparency(display, bg, imgToFade,
					new Point(0, 0), disabledOpacity == -1 ? 64
							: disabledOpacity * 255 / 100);
			bg.dispose();
		}
		return img;
	}

	public void unLoadImages() {
		if (DEBUG_UNLOAD) {
			for (String key : _mapImages.keySet()) {
				Image[] images = _mapImages.get(key).getImages();
				if (images != null) {
					for (int i = 0; i < images.length; i++) {
						Image image = images[i];
						if (isRealImage(image)) {
							System.out.println("dispose " + image + ";" + key);
							image.dispose();
						}
					}
				}
			}
		} else {
			for (ImageLoaderRefInfo imageInfo : _mapImages.values()) {
				Image[] images = imageInfo.getImages();
				if (images != null) {
					for (int i = 0; i < images.length; i++) {
						Image image = images[i];
						if (isRealImage(image)) {
							image.dispose();
						}
					}
				}
			}
		}
	}

	private ImageLoaderRefInfo
	getRefInfoFromImageMap(
		String		key )
	{
		return( _mapImages.get(key));
	}
	
	private void
	putRefInfoToImageMap(
		String				key,
		ImageLoaderRefInfo	info )
	{
		ImageLoaderRefInfo existing = _mapImages.put( key, info );
		
		if ( existing != null ){
			
			if ( existing.getImages().length > 0 ){
				
				Debug.out( "P: existing found! " + key + " -> " + existing.getString());
			}
		}
	}
	
	private ImageLoaderRefInfo
	putIfAbsentRefInfoToImageMap(
		String				key,
		ImageLoaderRefInfo	info )
	{
		ImageLoaderRefInfo x = _mapImages.putIfAbsent( key, info );
		
		if ( x != null ){
			
			if ( x.getImages().length > 0 ){
				
				Debug.out( "PIA: existing found! " + key + " -> " + x.getString());
			}
		}
		
		return( x );
	}
	
	protected Image getImageFromMap(String sKey) {
		Image[] imagesFromMap = getImagesFromMap(sKey);
		if (imagesFromMap.length == 0) {
			return null;
		}
		return imagesFromMap[0];
	}

	protected Image[] getImagesFromMap(String sKey) {
		if (sKey == null) {
			return new Image[0];
		}

		ImageLoaderRefInfo imageInfo = getRefInfoFromImageMap( sKey );
		if (imageInfo != null && imageInfo.getImages() != null) {
			imageInfo.addref();
			if (DEBUG_REFCOUNT) {
				logRefCount( sKey, imageInfo, true );
			}
			return imageInfo.getImages();
		}

		return new Image[0];
	}

	public Image[] getImages(String sKey) {
		//System.out.println("getImages " + sKey);
		if (sKey == null) {
			return new Image[0];
		}
		
		if (!Utils.isThisThreadSWT()) {
			Debug.out("getImages called on non-SWT thread");
			return new Image[0];
		}

		// ugly hack to show sidebar items that are disabled
		// note this messes up refcount (increments but doesn't decrement)
		if (sKey.startsWith("http://") && sKey.endsWith("-gray")) {
			sKey = sKey.substring(0, sKey.length() - 5);
		}

		ImageLoaderRefInfo imageInfo = getRefInfoFromImageMap(sKey);
		if (imageInfo != null && imageInfo.getImages() != null) {
			imageInfo.addref();
			if (DEBUG_REFCOUNT) {
				logRefCount( sKey, imageInfo, true );
			}
			return imageInfo.getImages();
		}

		Image[] images;
		String[] locations = null;
		ClassLoader cl = null;
		for (SkinProperties sp : skinProperties) {
			locations = sp.getStringArray(sKey);
			if (locations != null && locations.length > 0) {
				cl = sp.getClassLoader();
				break;
			}
		}
		//		System.out.println(sKey + "=" + properties.getStringValue(sKey)
		//				+ ";" + ((locations == null) ? "null" : "" + locations.length));
		if (locations == null || locations.length == 0) {
			images = findResources(sKey);

			if (images == null) {
				String	cache_key = sKey.hashCode() + ".ico";
				if ( cached_resources.contains( cache_key )){
					File cache = new File( cache_dir, cache_key );
					if (cache.exists()) {
						try {
							FileInputStream fis = new FileInputStream(cache);
	
							try {
								byte[] imageBytes = FileUtil.readInputStreamAsByteArray(fis);
								InputStream is = new ByteArrayInputStream(imageBytes);
	
								org.eclipse.swt.graphics.ImageLoader swtImageLoader = new org.eclipse.swt.graphics.ImageLoader();
								ImageData[] imageDatas = swtImageLoader.load(is);
								images = new Image[imageDatas.length];
								for (int i = 0; i < imageDatas.length; i++) {
									images[i] = new Image(Display.getCurrent(), imageDatas[i]);
								}
	
								try {
									is.close();
								} catch (IOException e) {
								}
							} finally {
								fis.close();
							}
						} catch (Throwable e) {
							Debug.printStackTrace(e);
						}
					}
				}else{
					cached_resources.remove( cache_key );
				}

				if (images == null) {
					images = new Image[0];
				}
			}

			for (int i = 0; i < images.length; i++) {
				if (images[i] == null) {
					images[i] = getNoImage();
				}
			}
		} else {
			images = parseValuesString(cl, sKey, locations, "");
		}

		ImageLoaderRefInfo info = new ImageLoaderRefInfo(images);
		putRefInfoToImageMap(sKey, info );
		if (DEBUG_REFCOUNT) {
			logRefCount( sKey, info, true );
		}

		return images;
	}

	public Image getImage(String sKey) {
		Image[] images = getImages(sKey);
		if (images == null || images.length == 0 || images[0].isDisposed()) {
			return getNoImage();
		}
		return images[0];
	}

	public long releaseImage(String sKey) {
		if (sKey == null) {
			return 0;
		}
		ImageLoaderRefInfo imageInfo = getRefInfoFromImageMap(sKey);
		if (imageInfo != null) {
			imageInfo.unref();
			if (false && imageInfo.getRefCount() < 0) {
				Image[] images = imageInfo.getImages();
				System.out.println("ImageLoader refcount < 0 for "
						+ sKey
						+ " by "
						+ Debug.getCompressedStackTrace()
						+ "\n  "
						+ (images == null ? "null"
								: ("" + images.length + ";" + (images.length == 0 ? "0" : ""
										+ (images[0] == noImage)))));
			}
			if (DEBUG_REFCOUNT) {
				logRefCount( sKey, imageInfo, false );
			}
			return imageInfo.getRefCount();
			// TODO: cleanup?
		}
		return 0;
	}

	/**
	 * Adds image to repository.  refcount will be 1, or if key already exists,
	 * refcount will increase.
	 * 
	 * @param key
	 * @param image
	 *
	 * @since 4.0.0.5
	 */
	public void addImage(String key, Image image) {
		if (!Utils.isThisThreadSWT()) {
			Debug.out("addImage called on non-SWT thread");
			return;
		}
		ImageLoaderRefInfo existing = putIfAbsentRefInfoToImageMap(key,
				new ImageLoaderRefInfo(image));
		if (existing != null) {
			// should probably fail if refcount > 0
			existing.setImages(new Image[] {
				image
			});
			existing.addref();
			if (DEBUG_REFCOUNT) {
				logRefCount( key, existing, true );
			}
		}
	}

	public void addImage(String key, Image[] images) {
		if (!Utils.isThisThreadSWT()) {
			Debug.out("addImage called on non-SWT thread");
			return;
		}
		ImageLoaderRefInfo existing = putIfAbsentRefInfoToImageMap(key,
				new ImageLoaderRefInfo(images));
		if (existing != null) {
			// should probably fail if refcount > 0
			existing.setImages(images);
			existing.addref();
			if (DEBUG_REFCOUNT) {
				logRefCount( key, existing, true );
			}
		}
	}
	
	private void
	logRefCount(
		String				key,
		ImageLoaderRefInfo	info,
		boolean				inc )
	{
		if ( true ){
			return;
		}
		if ( inc ){
			System.out.println("ImageLoader: ++ refcount to "
					+ info.getRefCount() + " for " + key + " via "
					+ Debug.getCompressedStackTraceSkipFrames(1));
		}else{
			System.out.println("ImageLoader: -- refcount to "
					+ info.getRefCount() + " for " + key + " via "
					+ Debug.getCompressedStackTraceSkipFrames(1));
		}
	}
	
	/*
	public void removeImage(String key) {
		// EEP!
		mapImages.remove(key);
	}
	*/
	
	public void addImageNoDipose(String key, Image image) {
		if (!Utils.isThisThreadSWT()) {
			Debug.out("addImageNoDispose called on non-SWT thread");
			return;
		}
		ImageLoaderRefInfo existing = putIfAbsentRefInfoToImageMap(key,
				new ImageLoaderRefInfo(image));
		if (existing != null) {
			existing.setNonDisposable();
			// should probably fail if refcount > 0
			existing.setImages(new Image[] {
				image
			});
			existing.addref();
			if (DEBUG_REFCOUNT) {
				logRefCount( key, existing, true );
			}
		}
	}

	private static Image getNoImage() {
		if (noImage == null) {
			Display display = Display.getDefault();
			final int SIZE = 10;
			noImage = new Image(display, SIZE, SIZE);
			GC gc = new GC(noImage);
			gc.setBackground(display.getSystemColor(SWT.COLOR_YELLOW));
			gc.fillRectangle(0, 0, SIZE, SIZE);
			gc.setBackground(display.getSystemColor(SWT.COLOR_RED));
			gc.drawRectangle(0, 0, SIZE - 1, SIZE - 1);
			gc.dispose();
		}
		return noImage;
	}

	public boolean imageExists(String name) {
		boolean exists = isRealImage(getImage(name));
		//if (exists) {	// getImage prety much always adds a ref for the 'name' so make sure
						// we do the corresponding unref here
			releaseImage(name);
		//}
		return exists;
	}

	public boolean imageAdded_NoSWT(String name) {
		return _mapImages.containsKey(name);
	}
	
	public boolean imageAdded(String name) {
		Image[] images = getImages(name);
		boolean added = images != null && images.length > 0;
		releaseImage(name);
		return added;
	}

	public static boolean isRealImage(Image image) {
		return image != null && image != getNoImage() && !image.isDisposed();
	}

	public int getAnimationDelay(String sKey) {
		for (SkinProperties sp : skinProperties) {
			int delay = sp.getIntValue(sKey + ".delay", -1);
			if (delay >= 0) {
				return delay;
			}
		}
		return 100;
	}

	public Image getUrlImage(final String url, final ImageDownloaderListener l) {
		if (!Utils.isThisThreadSWT()) {
			Debug.out("getUrlImage called on non-SWT thread");
			return null;
		}
		if (l == null || url == null) {
			return null;
		}

		if (imageExists(url)) {
			Image image = getImage(url);
			l.imageDownloaded(image, true);
			return image;
		}

		final String cache_key = url.hashCode() + ".ico";
		
		final File cache_file = new File( cache_dir, cache_key );

		if ( cached_resources.contains( cache_key )){
			
			if ( cache_file.exists()){
				try {
					FileInputStream fis = new FileInputStream(cache_file);
	
					try {
						byte[] imageBytes = FileUtil.readInputStreamAsByteArray(fis);
						InputStream is = new ByteArrayInputStream(imageBytes);
						Image image = new Image(Display.getCurrent(), is);
						try {
							is.close();
						} catch (IOException e) {
						}
						putRefInfoToImageMap(url, new ImageLoaderRefInfo(image));
						l.imageDownloaded(image, true);
						return image;
					} finally {
						fis.close();
					}
				} catch (Throwable e) {
					Debug.printStackTrace(e);
				}
			}else{
				
				cached_resources.remove( cache_key );
			}
		}

		ImageBytesDownloader.loadImage(url,
				new ImageBytesDownloader.ImageDownloaderListener() {
					public void imageDownloaded(final byte[] imageBytes) {
						Utils.execSWTThread(new AERunnable() {
							public void runSupport() {
									// no synchronization here - might have already been
									// downloaded
								if (imageExists(url)) {
									Image image = getImage(url);
									l.imageDownloaded(image, false);
									return;
								}
								FileUtil.writeBytesAsFile(cache_file.getAbsolutePath(), imageBytes);
								cached_resources.add( cache_key );
								InputStream is = new ByteArrayInputStream(imageBytes);
								Image image = new Image(Display.getCurrent(), is);
								try {
									is.close();
								} catch (IOException e) {
								}
								putRefInfoToImageMap(url, new ImageLoaderRefInfo(image));
								l.imageDownloaded(image, false);
							}
						});
					}
				});
		return null;
	}

	public static interface ImageDownloaderListener
	{
		public void imageDownloaded(Image image, boolean returnedImmediately);
	}

	// @see org.gudy.azureus2.core3.util.AEDiagnosticsEvidenceGenerator#generate(org.gudy.azureus2.core3.util.IndentWriter)
	public void generate(IndentWriter writer) {

		writer.println("ImageLoader for " + skinProperties);
		writer.indent();
		long[] sizeCouldBeFree = {
			0
		};
		long[] totalSizeEstimate = {
			0
		};
		try {
			writer.indent();
			try {
				writer.println("Non-Disposable:");
				writer.indent();
				for (String key : _mapImages.keySet()) {
					ImageLoaderRefInfo info = _mapImages.get(key);
					if (!info.isNonDisposable()) {
						continue;
					}
					writeEvidenceLine(writer, key, info, totalSizeEstimate,
							sizeCouldBeFree);
				}
				writer.exdent();
				writer.println("Disposable:");
				writer.indent();
				for (String key : _mapImages.keySet()) {
					ImageLoaderRefInfo info = _mapImages.get(key);
					if (info.isNonDisposable()) {
						continue;
					}
					writeEvidenceLine(writer, key, info, totalSizeEstimate,
							sizeCouldBeFree);
				}
				writer.exdent();
			} finally {
				writer.exdent();
			}
			if (totalSizeEstimate[0] > 0) {
				writer.println((totalSizeEstimate[0] / 1024)
						+ "k estimated used for images");
			}
			if (sizeCouldBeFree[0] > 0) {
				writer.println((sizeCouldBeFree[0] / 1024) + "k could be freed");
			}
		} finally {
			writer.exdent();
		}
	}

	/**
	 * @param writer
	 * @param info
	 */
	private void writeEvidenceLine(IndentWriter writer, String key,
			ImageLoaderRefInfo info, long[] totalSizeEstimate, long[] sizeCouldBeFree) {
		String line = info.getRefCount() + "] " + key;
		if (Utils.isThisThreadSWT()) {
			long sizeEstimate = 0;
			Image[] images = info.getImages();
			for (int i = 0; i < images.length; i++) {
				Image img = images[i];
				if (img != null) {
					if (img.isDisposed()) {
						line += "; *DISPOSED*";
					} else {
						Rectangle bounds = img.getBounds();
						long est = bounds.width * bounds.height * 4l;
						sizeEstimate += est;
						totalSizeEstimate[0] += est;
						if (info.canDispose()) {
							sizeCouldBeFree[0] += est;
						}
					}
				}
			}
			line += "; est " + sizeEstimate + " bytes";
		}
		writer.println(line);
	}

	public void addSkinProperties(SkinProperties skinProperties) {
		if (skinProperties == null) {
			return;
		}
		this.skinProperties.add(skinProperties);
		disabledOpacity = skinProperties.getIntValue(
				"imageloader.disabled-opacity", -1);
		notFound.clear();
	}

	/**
	 * 
	 *
	 * @since 4.0.0.5
	 */
	public void collectGarbage() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				int numRemoved = 0;
				for (Iterator<String> iter = _mapImages.keySet().iterator(); iter.hasNext();) {
					String key = iter.next();
					ImageLoaderRefInfo info = _mapImages.get(key);

					// no one can addref in between canDispose and dispose because
					// all our addrefs are in SWT threads.
					if (info != null && info.canDispose()) {
						if (DEBUG_UNLOAD) {
							System.out.println("dispose " + key);
						}
						iter.remove();
						numRemoved++;

						Image[] images = info.getImages();
						for (int j = 0; j < images.length; j++) {
							Image image = images[j];
							if (isRealImage(image)) {
								image.dispose();
							}
						}
					}
				}
				//System.out.println("ImageLoader: GC'd " + numRemoved);
			}
		});
	}

	/**
	 * @param label
	 * @param key
	 *
	 * @since 4.0.0.5
	 */
	public void setLabelImage(Label label, final String key) {
		Image bg = getImage(key);
		label.setImage(bg);
		label.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				releaseImage(key);
			}
		});
	}

	public void setButtonImage(Button btn, final String key) {
		Image bg = getImage(key);
		btn.setImage(bg);
		btn.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				releaseImage(key);
			}
		});
	}

	public void setBackgroundImage(Control control, final String key) {
		Image bg = getImage(key);
		control.setBackgroundImage(bg);
		control.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				releaseImage(key);
			}
		});
	}

	public SkinProperties[] getSkinProperties() {
		return skinProperties.toArray(new SkinProperties[0]);
	}

}
