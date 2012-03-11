/**
 * Created on Apr 28, 2008
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.utils;

import java.io.InputStream;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderFactoryImpl;

import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderAdapter;

/**
 * @author TuxPaper
 * @created Apr 28, 2008
 *
 */
public class ImageBytesDownloader
{
	static final Map<String, List<ImageDownloaderListener>> map = new HashMap<String, List<ImageDownloaderListener>>();

	static final AEMonitor mon_map = new AEMonitor("ImageDownloaderMap");

	public static void loadImage(final String url, final ImageDownloaderListener l) {
		//System.out.println("download " + url);
		mon_map.enter();
		try {
			List<ImageDownloaderListener> list = map.get(url);
			if (list != null) {
				list.add(l);
				return;
			}

			list = new ArrayList<ImageDownloaderListener>(1);
			list.add(l);
			map.put(url, list);
		} finally {
			mon_map.exit();
		}

		try {
			ResourceDownloader rd = ResourceDownloaderFactoryImpl.getSingleton().create(
					new URL(url));
			rd.addListener(new ResourceDownloaderAdapter() {
				public boolean completed(ResourceDownloader downloader, InputStream is) {
					mon_map.enter();
					try {
						List<ImageDownloaderListener> list = map.get(url);

						if (list != null) {
							try {
								if (is != null && is.available() > 0) {
									byte[] newImageBytes = new byte[is.available()];
									is.read(newImageBytes);

									for (ImageDownloaderListener l : list) {
										try {
											l.imageDownloaded(newImageBytes);
										} catch (Exception e) {
											Debug.out(e);
										}
									}
								}
							} catch (Exception e) {
								Debug.out(e);
							}

						}

						map.remove(url);
					} finally {
						mon_map.exit();
					}

					return false;
				}
			});
			rd.asyncDownload();
		} catch (Exception e) {
			Debug.out(url, e);
		}
	}

	public static interface ImageDownloaderListener
	{
		public void imageDownloaded(byte[] image);
	}
}
