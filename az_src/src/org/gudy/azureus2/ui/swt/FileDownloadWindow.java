/*
 * File    : FileDownloadWindow.java
 * Created : 3 nov. 2003 12:51:53
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

package org.gudy.azureus2.ui.swt;

import java.net.URLDecoder;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.progress.*;


/**
 * @author Olivier
 * 
 */
public class FileDownloadWindow
	implements TorrentDownloaderCallBackInterface, IProgressReportConstants
{
	
	TorrentDownloader downloader;

	TorrentDownloaderCallBackInterface listener;

	IProgressReporter pReporter;

	Shell parent;

	String original_url;

	String decoded_url;

	String referrer;

	Map request_properties;


	String dirName = null;

	String shortURL = null;

	/**
	 * Create a file download window.  Add torrent when done downloading
	 *  
	 * @param _azureus_core
	 * @param parent
	 * @param url
	 * @param referrer
	 */
	public FileDownloadWindow(Shell parent, final String url,
			final String referrer, Map request_properties) {
		this(parent, url, referrer, request_properties, null);
	}

	/**
	 * Create a file download window.  If no listener is supplied, torrent will
	 * be added when download is complete.  If a listener is supplied, caller
	 * handles it
	 *   
	 * @param _azureus_core
	 * @param parent
	 * @param url
	 * @param referrer
	 * @param listener
	 */
	public FileDownloadWindow(final Shell parent, final String url,
			final String referrer, final Map request_properties,
			final TorrentDownloaderCallBackInterface listener) {

		this.parent = parent;
		this.original_url = url;
		this.referrer = referrer;
		this.listener = listener;
		this.request_properties = request_properties;

		try {
			decoded_url = URLDecoder.decode(original_url, "UTF8");
		} catch (Throwable e) {
			decoded_url = original_url;

		}
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				init();
			}
		});
	}

	private void init() {

		if (COConfigurationManager.getBooleanParameter("Save Torrent Files")) {
			try {
				dirName = COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory");
			} catch (Exception egnore) {
			}
		}
		if (dirName == null) {
			DirectoryDialog dd = new DirectoryDialog(parent, SWT.NULL);
			dd.setText(MessageText.getString("fileDownloadWindow.saveTorrentIn"));
			dirName = dd.open();
		}
		if (dirName == null)
			return;

		pReporter = ProgressReportingManager.getInstance().addReporter();
		setupAndShowDialog();

		downloader = TorrentDownloaderFactory.create(this, original_url, referrer,
				request_properties, dirName);
		downloader.setIgnoreReponseCode(true);
		downloader.start();
	}

	/**
	 * Initializes the reporter and show the download dialog if it is not suppressed 
	 */
	private void setupAndShowDialog() {
		if (null != pReporter) {
			pReporter.setName(MessageText.getString("fileDownloadWindow.state_downloading")
					+ ": " + getFileName(decoded_url));
			pReporter.appendDetailMessage(MessageText.getString("fileDownloadWindow.downloading")
					+ getShortURL(decoded_url));
			pReporter.setTitle(MessageText.getString("fileDownloadWindow.title"));
			pReporter.setIndeterminate(true);
			pReporter.setCancelAllowed(true);
			pReporter.setRetryAllowed(true);

			/*
			 * Listen to and respond to events from the reporters 
			 */
			pReporter.addListener(new IProgressReporterListener() {

				public int report(IProgressReport pReport) {

					switch (pReport.getReportType()) {
						case REPORT_TYPE_CANCEL:
							if (null != downloader) {
								downloader.cancel();

								//KN: correct logger id?
								Logger.log(new LogEvent(LogIDs.LOGGER, MessageText.getString(
										"FileDownload.canceled", new String[] {
											getShortURL(decoded_url)
										})));
							}
							break;
						case REPORT_TYPE_DONE:
							return RETVAL_OK_TO_DISPOSE;
						case REPORT_TYPE_RETRY:
							if (true == pReport.isRetryAllowed()) {
								downloader.cancel();
								downloader = TorrentDownloaderFactory.create(
										FileDownloadWindow.this, original_url, referrer,
										request_properties, dirName);
								downloader.setIgnoreReponseCode(true);
								downloader.start();
							}
							break;
						default:
							break;
					}

					return RETVAL_OK;
				}

			});

			ProgressReporterWindow.open(pReporter, ProgressReporterWindow.AUTO_CLOSE);
		}
	}

	public void TorrentDownloaderEvent(int state, TorrentDownloader inf) {
		if (listener != null)
			listener.TorrentDownloaderEvent(state, inf);
		update();
	}

	private void update() {
		int state = downloader.getDownloadState();
		int percentDone = downloader.getPercentDone();

		IProgressReport pReport = pReporter.getProgressReport();
		switch (state) {
			case TorrentDownloader.STATE_CANCELLED:
				if (false == pReport.isCanceled()) {
					pReporter.cancel();
				}
				return;
			case TorrentDownloader.STATE_DOWNLOADING:
				pReporter.setPercentage(percentDone, downloader.getStatus());
				break;
			case TorrentDownloader.STATE_ERROR:
				/*
				 * If the user has canceled then a call  to downloader.cancel() has already been made
				 * so don't bother prompting for the user to retry
				 */
				if (true == pReport.isCanceled()) {
					return;
				}

				pReporter.setErrorMessage(MessageText.getString("fileDownloadWindow.state_error")
						+ downloader.getError());
				return;
			case TorrentDownloader.STATE_FINISHED:
				pReporter.setDone();

				/*
				 * If the listener is present then it handle finishing up; otherwise open the torrent that
				 * was just downloaded
				 */
				if (listener == null) {
					TorrentOpener.openTorrent(downloader.getFile().getAbsolutePath());
				}
				return;
			default:
		}

	}

	/**
	 * Returns a shortened version of the given url
	 * @param url
	 * @return
	 */
	private String getShortURL(final String url) {
		if (null == shortURL) {
			shortURL = url;
			// truncate any url parameters for display. This has the benefit of hiding additional uninteresting
			// parameters added to urls to control the download process (e.g. "&pause_on_error" for magnet downloads")
			int amp_pos = shortURL.indexOf('&');
			if (amp_pos != -1) {
				shortURL = shortURL.substring(0, amp_pos + 1) + "...";
			}
			shortURL = shortURL.replaceAll("&", "&&");
		}

		return shortURL;
	}

	/**
	 * Brute-force extraction of the torrent file name or title from the given URL
	 * @param url
	 * @return
	 */
	private String getFileName(String url) {
		try {
			/*
			 * First try to retrieve the 'title' field if it has one
			 */
			
			final String[] titles = {
				"title",
				"dn"
			};
			// magnet:?xt=urn:btih:WENBTYBB7676MQWJ7NLTD4NGYDKYSQPO&dn=[DmzJ][School_Days][Vol.01-06][DVDRip]
			for (String toMatch : titles) {
				Matcher matcher = Pattern.compile("[?&]" + toMatch + "=(.*)&?",
						Pattern.CASE_INSENSITIVE).matcher(url);
				if (matcher.find()) {
					return matcher.group(1);
				}
			}
			
			/*
			 * If no 'title' field was found then just get the file name instead
			 * 
			 * This is not guaranteed to work in all cases because we are simply searching
			 * for the occurrence of ".torrent" and assuming that it is the extension of the file.
			 * This method will return inaccurate result if any other parameter of the URL also has the ".torrent"
			 * string in it.
			 */

			url = getShortURL(url);
			
			String lc_url = url.toLowerCase(MessageText.LOCALE_ENGLISH);

			if ( lc_url.startsWith( "magnet:") || lc_url.startsWith( "dht:" ) || lc_url.startsWith( "bc:" ) || lc_url.startsWith( "bctp:" )){
				
				return( url );
			}
			
			String tmp = url.substring(url.lastIndexOf('/') + 1);
			
			int pos = tmp.toLowerCase(MessageText.LOCALE_ENGLISH).lastIndexOf(".vuze");
			
			if ( pos > 0) {
				return( tmp.substring(0, pos + 5 ));			
			}

			pos = tmp.toLowerCase(MessageText.LOCALE_ENGLISH).lastIndexOf(".torrent");
			
			if (pos > 0) {
				tmp = tmp.substring(0, pos );
			}
			return tmp + ".torrent";
		} catch (Exception t) {
			// don't print debug, this code is just parsing lazyness
			//Debug.out(t);
		}

		return url;
	}
}
