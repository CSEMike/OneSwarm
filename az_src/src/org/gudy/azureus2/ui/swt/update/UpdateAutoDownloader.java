/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.update;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.gudy.azureus2.core3.util.AEThread;

import org.gudy.azureus2.plugins.update.Update;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener;

/**
 * @author TuxPaper
 * @created Feb 25, 2007
 *
 */
public class UpdateAutoDownloader
	implements ResourceDownloaderListener
{
	private final Update[] updates;

	private ArrayList downloaders;

	private Iterator iterDownloaders;

	private final cbCompletion completionCallback;

	public static interface cbCompletion
	{
		public void allUpdatesComplete(boolean requiresRestart, boolean bHadMandatoryUpdates);
	}

	/**
	 * @param us
	 */
	public UpdateAutoDownloader(Update[] updates, cbCompletion completionCallback) {
		this.updates = updates;
		this.completionCallback = completionCallback;
		downloaders = new ArrayList();

		start();
	}

	private void start() {
		for (int i = 0; i < updates.length; i++) {
			Update update = updates[i];
			ResourceDownloader[] rds = update.getDownloaders();
			for (int j = 0; j < rds.length; j++) {
				ResourceDownloader rd = rds[j];
				downloaders.add(rd);
			}
		}

		iterDownloaders = downloaders.iterator();
		nextUpdate();
	}

	/**
	 * 
	 *
	 * @since 3.0.0.7
	 */
	private boolean nextUpdate() {
		if (iterDownloaders.hasNext()) {
			ResourceDownloader downloader = (ResourceDownloader) iterDownloaders.next();
			downloader.addListener(this);
			downloader.asyncDownload();
			return true;
		}
		return false;
	}

	/**
	 * 
	 *
	 * @since 3.0.0.7
	 */
	private void allDownloadsComplete() {
		boolean bRequiresRestart = false;
		boolean bHadMandatoryUpdates = false;
		
		for (int i = 0; i < updates.length; i++) {
			Update update = updates[i];
				// updates with no downloaders exist for admin purposes only
			if ( update.getDownloaders().length > 0){
				if (update.getRestartRequired() != Update.RESTART_REQUIRED_NO) {
					bRequiresRestart = true;
				}
				if ( update.isMandatory()){
					bHadMandatoryUpdates = true;
				}
			}
		}

		completionCallback.allUpdatesComplete(bRequiresRestart,bHadMandatoryUpdates);
	}

	// @see org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener#completed(org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader, java.io.InputStream)
	public boolean completed(ResourceDownloader downloader, InputStream data) {
		downloader.removeListener(this);
		if (!nextUpdate()) {
			// fire in another thread so completed function can exit
			AEThread thread = new AEThread("AllDownloadsComplete", true) {
				public void runSupport() {
					allDownloadsComplete();
				}
			};
			thread.start();
		}
		return true;
	}

	// @see org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener#failed(org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader, org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException)
	public void failed(ResourceDownloader downloader,
			ResourceDownloaderException e) {
		downloader.removeListener(this);
		iterDownloaders.remove();
		nextUpdate();
	}

	// @see org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener#reportActivity(org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader, java.lang.String)
	public void reportActivity(ResourceDownloader downloader, String activity) {
	}

	// @see org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener#reportAmountComplete(org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader, long)
	public void reportAmountComplete(ResourceDownloader downloader, long amount) {
	}

	// @see org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener#reportPercentComplete(org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader, int)
	public void reportPercentComplete(ResourceDownloader downloader,
			int percentage) {
	}
}
