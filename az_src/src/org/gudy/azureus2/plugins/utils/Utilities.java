/*
 * File    : Utilities.java
 * Created : 24-Mar-2004
 * By      : parg
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

package org.gudy.azureus2.plugins.utils;

/**
 * @author parg
 *
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Map;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.plugins.utils.resourceuploader.ResourceUploaderFactory;
import org.gudy.azureus2.plugins.utils.search.SearchException;
import org.gudy.azureus2.plugins.utils.search.SearchInitiator;
import org.gudy.azureus2.plugins.utils.search.SearchProvider;
import org.gudy.azureus2.plugins.utils.security.*;
import org.gudy.azureus2.plugins.utils.subscriptions.SubscriptionException;
import org.gudy.azureus2.plugins.utils.subscriptions.SubscriptionManager;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.*;
import org.gudy.azureus2.plugins.utils.xml.rss.*;

public interface 
Utilities 
{
	public String
	getAzureusUserDir();
	
	public String
	getAzureusProgramDir();
	
	public boolean
	isCVSVersion();
	
	public boolean
	isWindows();
	
	public boolean
	isLinux();
	
	public boolean
	isSolaris();
	
	public boolean
	isOSX();
	

	/**
	 * @return Whether the OS is a unix flavor (linux, bsd, aix, etc)
	 * 
	 * @since 2.4.0.3
	 */
	boolean isUnix();

	/**
	 * @return Whether the OS is FreeBSD
	 * 
	 * @note Up to at least v2.4.0.2, the FreeBSD port has identified itself
	 *       to azureus as Linux and not FreeBSD
	 * 
	 * @since 2.4.0.3
	 */
	boolean isFreeBSD();
	

	public InputStream
	getImageAsStream(
		String	image_name );
	
	public Semaphore
	getSemaphore();
  
    public Monitor getMonitor();
	
	public ByteBuffer
	allocateDirectByteBuffer(
		int		size );
	
	public void
	freeDirectByteBuffer(
		ByteBuffer	buffer );
	
	public PooledByteBuffer
	allocatePooledByteBuffer(
		int		size );
	
	public PooledByteBuffer
	allocatePooledByteBuffer(
		byte[]	data  );
	
		/**
		 * 
		 * @param data	must be b-encodable
		 * @return
		 */
	
	public PooledByteBuffer
	allocatePooledByteBuffer(
		Map		data  )
	
		throws IOException;
	
	public Formatters
	getFormatters();
	
	public LocaleUtilities
	getLocaleUtilities();
	
	/**
	 * Creates a <code>UTTimer</code> instance. It will be configured for non-lightweight
	 * tasks by default.
	 * 
	 * @param name Name for the UTTimer object.
	 * @return A UTTimer instance.
	 */
	public UTTimer
	createTimer(
		String		name );

	/**
	 * Creates a <code>UTTimer</code> instance.
	 * 
	 * @param name Name for the UTTimer object.
	 * @param lightweight If <code>true</code>, it indicates that this timer will be used to
	 *   perform small lightweight tasks. If <code>false</code>, it indicates that
	 *   this timer will be used to perform expensive tasks. This allows Azureus to create
	 *   the appropriate amount of resources to manage this timer.
	 * @return A UTTimer instance.
	 */
	public UTTimer
	createTimer(
		String		name,
		boolean		lightweight );

	/**
	 * Creates a <code>UTTimer</code> instance.
	 * 
	 * @param name Name for the UTTimer object.
	 * @param priority The Thread.<i>XXX_</i>PRIORITY value to use.
	 * @return A UTTimer instance.
	 */
	public UTTimer createTimer(String name, int priority);

		/**
		 * create and run a thread for the target. This will be a daemon thread so that
		 * its existence doesn't interfere with Azureus closedown
		 * @param name
		 * @param target
		 */
	
	public void
	createThread(
		String		name,
		Runnable	target );
	
		/**
		 * create a child process and executes the supplied command line. The child process
		 * will not inherit any open handles on Windows, which does happen if Runtime is
		 * used directly. This relies on the Platform plugin, if this is not installed then
		 * this will fall back to using Runtime.exec 
		 * @param command_line
		 */
	
	public void
	createProcess(
		String		command_line )
	
		throws PluginException;
	
	public ResourceDownloaderFactory
	getResourceDownloaderFactory();
	
	public ResourceUploaderFactory
	getResourceUploaderFactory();

	public SESecurityManager
	getSecurityManager();
	
	public SimpleXMLParserDocumentFactory
	getSimpleXMLParserDocumentFactory();
	
	public RSSFeed
	getRSSFeed(
		InputStream		is )
	
		throws SimpleXMLParserDocumentException;

	public RSSFeed
	getRSSFeed(
		URL		feed_location )
	
		throws ResourceDownloaderException, SimpleXMLParserDocumentException;
	
	public RSSFeed
	getRSSFeed(
		ResourceDownloader	feed_location )
	
		throws ResourceDownloaderException, SimpleXMLParserDocumentException;
	
		/**
		 * Returns a public IP address of the machine or null if it can't be determined
		 */
	
	public InetAddress
	getPublicAddress();
	
	public InetAddress
	getPublicAddress(
		boolean		ipv6 );
	
		/**
		 * attempts a reverse DNS lookup of an address, null if it fails
		 * @param address
		 * @return
		 */
	
	public String
	reverseDNSLookup(
		InetAddress		address );
  
  
  /**
   * Get the current system time, like System.currentTimeMillis(),
   * only the time lookup is cached for performance reasons.
   * @return current system time
   */
	public long getCurrentSystemTime();
  
  	public ByteArrayWrapper
	createWrapper(
		byte[]		data );
  	
  	
  	/**
  	 * create a dispatcher that will queue runnable items until either the limit
  	 * is reached or the dispatcher hasn't had an entry added for the defined idle time
  	 * @param idle_dispatch_time	milliseconds
  	 * @param max_queue_size		0 -> infinite
  	 * @return
  	 */
  	
  	public AggregatedDispatcher
	createAggregatedDispatcher(
		long	idle_dispatch_time,
		long	max_queue_size );
  	
 	public AggregatedList
	createAggregatedList(
		AggregatedListAcceptor	acceptor,
		long					idle_dispatch_time,
		long					max_queue_size );
 	
 	public Map
 	readResilientBEncodedFile(
 		File	parent_dir,
 		String	file_name,
 		boolean	use_backup );
 	
	public void
 	writeResilientBEncodedFile(
 		File	parent_dir,
 		String	file_name,
 		Map		data,
 		boolean	use_backup );	
	
	public void
 	deleteResilientBEncodedFile(
 		File	parent_dir,
 		String	file_name,
 		boolean	use_backup );
	
	/**
	 * Compares two version strings for order. 
	 * Returns a negative integer, zero, or a positive integer as the first 
	 * argument is less than, equal to, or greater than the second.
	 * <p>
	 * Example:<br>
	 * compareVersions("1.1.0.0", "1.1.2.0"); // -
	 * compareVersions("1.1.0.0", "1.1.0"); // 0
	 * compareVersions("1.1.1.1", "1.1.1"); // +
	 * 
	 * @param v1 the first version string to be compared 
	 * @param v2 the second version string to be compared
	 * @return a negative integer, zero, or a positive integer as the first 
	 *          argument is less than, equal to, or greater than the second.
	 * 
	 * @since 2.3.0.7
	 */
	public int compareVersions(String v1, String v2);
	
	/**
	 * Converts a file name so that all characters in the file name are
	 * compatible with the underlying filesystem. This includes quote
	 * characters, back and forwarded slashes, newline characters and so on.
	 * 
	 * <p>
	 * 
	 * Note - this is only intended for file names, rather than file paths.
	 * 
	 * @param f_name File name to convert.
	 * @return Converted file name.
	 */
	public String normaliseFileName(String f_name);
	
	/**
	 * Adds a low priority task that will be scheduled at some point after existing tasks have
	 * completed. In particular a system task exists that will block subsequent ones until after
	 * UI initialisation is complete. Plugins can therefore use this to schedule initialisation 
	 * actions to occur after UI init is complete.
	 * 
	 * @since 3.0.5.3
	 * @return
	 */
	
	public DelayedTask createDelayedTask(Runnable r);
	
	public void
	registerSearchProvider(
		SearchProvider		provider )
	
		throws SearchException;

	public SearchInitiator
	getSearchInitiator()
	
		throws SearchException;
	
	public SubscriptionManager
	getSubscriptionManager()
	
		throws SubscriptionException;
	
	public FeatureManager
	getFeatureManager();
}


