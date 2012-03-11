/*
 * File    : TorrentUtils.java
 * Created : 13-Oct-2003
 * By      : stuff
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

package org.gudy.azureus2.core3.util;

/**
 * @author parg
 *
 */

import java.io.*;
import java.net.*;
import java.util.*;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.core.util.CopyOnWriteList;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.logging.LogRelation;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.download.*;


public class 
TorrentUtils 
{
	public static final int TORRENT_FLAG_LOW_NOISE	= 0x00000001;
	
	private static final String		TORRENT_AZ_PROP_DHT_BACKUP_ENABLE		= "dht_backup_enable";
	private static final String		TORRENT_AZ_PROP_DHT_BACKUP_REQUESTED	= "dht_backup_requested";
	private static final String		TORRENT_AZ_PROP_TORRENT_FLAGS			= "torrent_flags";
	private static final String		TORRENT_AZ_PROP_PLUGINS					= "plugins";
	
	public static final String		TORRENT_AZ_PROP_OBTAINED_FROM			= "obtained_from";
	public static final String		TORRENT_AZ_PROP_PEER_CACHE				= "peer_cache";
	public static final String		TORRENT_AZ_PROP_PEER_CACHE_VALID		= "peer_cache_valid";
	
	private static final String		MEM_ONLY_TORRENT_PATH		= "?/\\!:mem_only:!\\/?";
	
	private static final long		PC_MARKER = RandomUtils.nextLong();
	
	private static final List	created_torrents;
	private static final Set	created_torrents_set;
	
	private static ThreadLocal<Map<String,Object>>		tls	= 
		new ThreadLocal<Map<String,Object>>()
		{
			public Map<String,Object>
			initialValue()
			{
				return( new HashMap<String,Object>());
			}
		};
		
	private static volatile Set		ignore_set;
	
	private static boolean bSaveTorrentBackup;
	
	private static CopyOnWriteList	torrent_attribute_listeners = new CopyOnWriteList();
	
	static {
		COConfigurationManager.addAndFireParameterListener("Save Torrent Backup",
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						bSaveTorrentBackup = COConfigurationManager.getBooleanParameter(parameterName);
					}
				});
		
		created_torrents = COConfigurationManager.getListParameter( "my.created.torrents", new ArrayList());
		
		created_torrents_set	= new HashSet();
		
		Iterator	it = created_torrents.iterator();
		
		while( it.hasNext()){
			
			created_torrents_set.add( new HashWrapper((byte[])it.next()));
		}
	}

	public static TOTorrent
	readFromFile(
		File		file,
		boolean		create_delegate )
		
		throws TOTorrentException
	{
		return( readFromFile( file, create_delegate, false ));
	}
	
		/**
		 * If you set "create_delegate" to true then you must understand that this results
		 * is piece hashes being discarded and then re-read from the torrent file if needed
		 * Therefore, if you delete the original torrent file you're going to get errors
		 * if you access the pieces after this (and they've been discarded)
		 * @param file
		 * @param create_delegate
		 * @param force_initial_discard - use to get rid of pieces immediately
		 * @return
		 * @throws TOTorrentException
		 */
	
	public static ExtendedTorrent
	readDelegateFromFile(
		File		file,
		boolean		force_initial_discard )
		
		throws TOTorrentException
	{
		return((ExtendedTorrent)readFromFile( file, true, force_initial_discard ));
	}
	
	public static TOTorrent
	readFromFile(
		File		file,
		boolean		create_delegate,
		boolean		force_initial_discard )
		
		throws TOTorrentException
	{
		TOTorrent torrent;
   
		try{
			torrent = TOTorrentFactory.deserialiseFromBEncodedFile(file);
			
				// make an immediate backup if requested and one doesn't exist 
			
	    	if (bSaveTorrentBackup) {
	    		
	    		File torrent_file_bak = new File(file.getParent(), file.getName() + ".bak");

	    		if ( !torrent_file_bak.exists()){
	    			
	    			try{
	    				torrent.serialiseToBEncodedFile(torrent_file_bak);
	    				
	    			}catch( Throwable e ){
	    				
	    				Debug.printStackTrace(e);
	    			}
	    		}
	    	}
	    	
		}catch (TOTorrentException e){
      
			// Debug.outNoStack( e.getMessage() );
			
			File torrentBackup = new File(file.getParent(), file.getName() + ".bak");
			
			if( torrentBackup.exists()){
				
				torrent = TOTorrentFactory.deserialiseFromBEncodedFile(torrentBackup);
				
					// use the original torrent's file name so that when this gets saved
					// it writes back to the original and backups are made as required
					// - set below
			}else{
				
				throw e;
			}
		}
				
		torrent.setAdditionalStringProperty("torrent filename", file.toString());
		
		if ( create_delegate ){
			
			torrentDelegate	res = new torrentDelegate( torrent, file );
			
			if ( force_initial_discard ){
				
				res.discardPieces( SystemTime.getCurrentTime(), true );
			}
			
			return( res );
			
		}else{
			
			return( torrent );
		}
	}

	public static TOTorrent
	readFromBEncodedInputStream(
		InputStream		is )
		
		throws TOTorrentException
	{
		TOTorrent	torrent = TOTorrentFactory.deserialiseFromBEncodedInputStream( is );
		
			// as we've just imported this torrent we want to clear out any possible attributes that we
			// don't want such as "torrent filename"
		
		torrent.removeAdditionalProperties();
		
		return( torrent );
	}
	
	public static void
	setMemoryOnly(
		TOTorrent			torrent,
		boolean				mem_only )
	{
		if ( mem_only ){
			
			torrent.setAdditionalStringProperty("torrent filename", MEM_ONLY_TORRENT_PATH );
			
		}else{
			
			String s = torrent.getAdditionalStringProperty("torrent filename");
			
			if ( s != null && s.equals( MEM_ONLY_TORRENT_PATH )){
				
				torrent.removeAdditionalProperty( "torrent filename" );
			}
		}
	}
	
	public static void
	writeToFile(
		final TOTorrent		torrent )
	
		throws TOTorrentException 
	{
		writeToFile( torrent, false );
	}
	
	public static void
	writeToFile(
		TOTorrent		torrent,
		boolean			force_backup )
	
		throws TOTorrentException 
	{
	   try{
	   		torrent.getMonitor().enter();
	    		   		
	    	String str = torrent.getAdditionalStringProperty("torrent filename");
	    	
	    	if ( str == null ){
	    		
	    		throw (new TOTorrentException("TorrentUtils::writeToFile: no 'torrent filename' attribute defined", TOTorrentException.RT_FILE_NOT_FOUND));
	    	}
	    	
	    	if ( str.equals( MEM_ONLY_TORRENT_PATH )){
	    		
	    		return;
	    	}
	    	
	    		// save first to temporary file as serialisation may require state to be re-read from
	    		// the existing file first and if we rename to .bak first then this aint good
	    		    	
    		File torrent_file_tmp = new File(str + "._az");

	    	torrent.serialiseToBEncodedFile( torrent_file_tmp );

	    		// now backup if required
	    	
	    	File torrent_file = new File(str);
	    	
	    	if ( 	( force_backup ||COConfigurationManager.getBooleanParameter("Save Torrent Backup")) &&
	    			torrent_file.exists()) {
	    		
	    		File torrent_file_bak = new File(str + ".bak");
	    		
	    		try{
	    			
	    				// Will return false if it cannot be deleted (including if the file doesn't exist).
	    			
	    			torrent_file_bak.delete();
	    			
	    			torrent_file.renameTo(torrent_file_bak);
	    			
	    		}catch( SecurityException e){
	    			
	    			Debug.printStackTrace( e );
	    		}
	    	}
	      
	    		// now rename the temp file to required one
	    	
	    	if ( torrent_file.exists()){
	    		
	    		torrent_file.delete();
	    	}
	    	
	    	torrent_file_tmp.renameTo( torrent_file );
			
	   	}finally{
	   		
	   		torrent.getMonitor().exit();
	   	}
	}
	
	public static void
	writeToFile(
		TOTorrent		torrent,
		File			file )
	
		throws TOTorrentException 
	{
		writeToFile( torrent, file, false );
	}
	
	public static void
	writeToFile(
		TOTorrent		torrent,
		File			file,
		boolean			force_backup )
	
		throws TOTorrentException 
	{		
		torrent.setAdditionalStringProperty("torrent filename", file.toString());
		
		writeToFile( torrent, force_backup );
	}
	
	public static String
	getTorrentFileName(
		TOTorrent		torrent )
	
		throws TOTorrentException 
	{
    	String str = torrent.getAdditionalStringProperty("torrent filename");
    	
    	if ( str == null ){
    		
    		throw( new TOTorrentException("TorrentUtils::getTorrentFileName: no 'torrent filename' attribute defined", TOTorrentException.RT_FILE_NOT_FOUND));
    	}

    	if ( str.equals( MEM_ONLY_TORRENT_PATH )){
    		
    		return( null );
    	}
    	
		return( str );
	}
	
	public static void
	copyToFile(
		TOTorrent		torrent,
		File			file )

		throws TOTorrentException 
	{
	   	torrent.serialiseToBEncodedFile(file);
	}
	
	public static void
	delete(
		TOTorrent 		torrent )
	
		throws TOTorrentException 
	{
	   try{
	   		torrent.getMonitor().enter();
	    	
	    	String str = torrent.getAdditionalStringProperty("torrent filename");
	    	
	    	if ( str == null ){
	    		
	    		throw( new TOTorrentException("TorrentUtils::delete: no 'torrent filename' attribute defined", TOTorrentException.RT_FILE_NOT_FOUND));
	    	}
	    	
	    	if ( str.equals( MEM_ONLY_TORRENT_PATH )){
	    		
	    		return;
	    	}
	    	
	    	if ( !new File(str).delete()){
	    		
	    		throw( new TOTorrentException("TorrentUtils::delete: failed to delete '" + str + "'", TOTorrentException.RT_WRITE_FAILS));
	    	}
		
	    	new File( str + ".bak" ).delete();
	    	
	    }finally{
	    	
	    	torrent.getMonitor().exit();
	    }
	}
	
	public static void
	delete(
		File 		torrent_file,
		boolean		force_no_recycle )
	{
		if ( !FileUtil.deleteWithRecycle( torrent_file, force_no_recycle )){
			
    		Debug.out( "TorrentUtils::delete: failed to delete '" + torrent_file + "'" );
    	}
	
    	new File( torrent_file.toString() + ".bak" ).delete();
	}
	
	public static boolean
	move(
		File		from_torrent,
		File		to_torrent )
	{
		if ( !FileUtil.renameFile(from_torrent, to_torrent )){
			
			return( false );
		}
		
		if ( new File( from_torrent.toString() + ".bak").exists()){
			
			FileUtil.renameFile( 
				new File( from_torrent.toString() + ".bak"),
				new File( to_torrent.toString() + ".bak"));
		}
		
		return( true );
	}
		
	public static String
	exceptionToText(
		TOTorrentException	e )
	{
		String	errorDetail;
		
		int	reason = e.getReason();
  					
		if ( reason == TOTorrentException.RT_FILE_NOT_FOUND ){
 	     	        		 		
			errorDetail = MessageText.getString("DownloadManager.error.filenotfound" );
	        				
		}else if ( reason == TOTorrentException.RT_ZERO_LENGTH ){
	     
			errorDetail = MessageText.getString("DownloadManager.error.fileempty");
	        			
		}else if ( reason == TOTorrentException.RT_TOO_BIG ){
	 	     		
			errorDetail = MessageText.getString("DownloadManager.error.filetoobig");
			        
		}else if ( reason == TOTorrentException.RT_DECODE_FAILS ){
	 
			errorDetail = MessageText.getString("DownloadManager.error.filewithouttorrentinfo" );
	 		  			
		}else if ( reason == TOTorrentException.RT_UNSUPPORTED_ENCODING ){
	 	     		
			errorDetail = MessageText.getString("DownloadManager.error.unsupportedencoding");
							
		}else if ( reason == TOTorrentException.RT_READ_FAILS ){
	
			errorDetail = MessageText.getString("DownloadManager.error.ioerror");
					
		}else if ( reason == TOTorrentException.RT_HASH_FAILS ){
			
			errorDetail = MessageText.getString("DownloadManager.error.sha1");
					
		}else if ( reason == TOTorrentException.RT_CANCELLED ){
			
			errorDetail = MessageText.getString("DownloadManager.error.operationcancancelled");
						
		}else{
	 	     
			errorDetail = Debug.getNestedExceptionMessage(e);
		}
					
		String	msg = Debug.getNestedExceptionMessage(e);
				
		if ( errorDetail.indexOf( msg ) == -1){
				
			errorDetail += " (" + msg + ")";
		}
		
		return( errorDetail );
	}
	
	public static List<List<String>>
	announceGroupsToList(
		TOTorrent	torrent )
	{
		List<List<String>>	groups = new ArrayList<List<String>>();
		
		TOTorrentAnnounceURLGroup group = torrent.getAnnounceURLGroup();
		
		TOTorrentAnnounceURLSet[]	sets = group.getAnnounceURLSets();
		
		if ( sets.length == 0 ){
		
			List<String>	s = new ArrayList<String>();
			
			s.add( torrent.getAnnounceURL().toString());
			
			groups.add(s);
		}else{
			
			for (int i=0;i<sets.length;i++){
			
				List<String>	s = new ArrayList<String>();
								
				TOTorrentAnnounceURLSet	set = sets[i];
				
				URL[]	urls = set.getAnnounceURLs();
				
				for (int j=0;j<urls.length;j++){
				
					s.add( urls[j].toString());
				}
				
				if ( s.size() > 0 ){
					
					groups.add(s);
				}
			}
		}
		
		return( groups );
	}
	
	public static void
	listToAnnounceGroups(
		List<List<String>>		groups,
		TOTorrent				torrent )
	{
		try{
			TOTorrentAnnounceURLGroup tg = torrent.getAnnounceURLGroup();
			
			if ( groups.size() == 1 ){
				
				List	set = (List)groups.get(0);
				
				if ( set.size() == 1 ){
					
					torrent.setAnnounceURL( new URL((String)set.get(0)));
					
					tg.setAnnounceURLSets( new TOTorrentAnnounceURLSet[0]);
					
					return;
				}
			}
			
			
			Vector	g = new Vector();
			
			for (int i=0;i<groups.size();i++){
				
				List	set = (List)groups.get(i);
				
				URL[]	urls = new URL[set.size()];
				
				for (int j=0;j<set.size();j++){
				
					urls[j] = new URL((String)set.get(j));
				}
				
				if ( urls.length > 0 ){
					
					g.add( tg.createAnnounceURLSet( urls ));
				}
			}
			
			TOTorrentAnnounceURLSet[]	sets = new TOTorrentAnnounceURLSet[g.size()];
			
			g.copyInto( sets );
			
			tg.setAnnounceURLSets( sets );
			
			if ( sets.length == 0 ){
			
					// hmm, no valid urls at all
				
				torrent.setAnnounceURL( new URL( "http://no.valid.urls.defined/announce"));
			}
			
		}catch( MalformedURLException e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	public static void
	announceGroupsInsertFirst(
		TOTorrent	torrent,
		String		first_url )
	{
		try{
			
			announceGroupsInsertFirst( torrent, new URL( first_url ));
			
		}catch( MalformedURLException e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	public static void
	announceGroupsInsertFirst(
		TOTorrent	torrent,
		URL			first_url )
	{
		announceGroupsInsertFirst( torrent, new URL[]{ first_url });
	}
	
	public static void
	announceGroupsInsertFirst(
		TOTorrent	torrent,
		URL[]		first_urls )
	{
		TOTorrentAnnounceURLGroup group = torrent.getAnnounceURLGroup();
		
		TOTorrentAnnounceURLSet[] sets = group.getAnnounceURLSets();

		TOTorrentAnnounceURLSet set1 = group.createAnnounceURLSet( first_urls );
		
		
		if ( sets.length > 0 ){
			
			TOTorrentAnnounceURLSet[]	new_sets = new TOTorrentAnnounceURLSet[sets.length+1];
			
			new_sets[0] = set1;
			
			System.arraycopy( sets, 0, new_sets, 1, sets.length );
			
			group.setAnnounceURLSets( new_sets );
					
		}else{
			
			TOTorrentAnnounceURLSet set2 = group.createAnnounceURLSet(new URL[]{torrent.getAnnounceURL()});
			
			group.setAnnounceURLSets(
				new  TOTorrentAnnounceURLSet[]{ set1, set2 });
		}
	}
	
	public static void
	announceGroupsInsertLast(
		TOTorrent	torrent,
		URL[]		first_urls )
	{
		TOTorrentAnnounceURLGroup group = torrent.getAnnounceURLGroup();
		
		TOTorrentAnnounceURLSet[] sets = group.getAnnounceURLSets();

		TOTorrentAnnounceURLSet set1 = group.createAnnounceURLSet( first_urls );
		
		
		if ( sets.length > 0 ){
			
			TOTorrentAnnounceURLSet[]	new_sets = new TOTorrentAnnounceURLSet[sets.length+1];
			
			new_sets[sets.length] = set1;
			
			System.arraycopy( sets, 0, new_sets, 0, sets.length );
			
			group.setAnnounceURLSets( new_sets );
					
		}else{
			
			TOTorrentAnnounceURLSet set2 = group.createAnnounceURLSet(new URL[]{torrent.getAnnounceURL()});
			
			group.setAnnounceURLSets(
				new  TOTorrentAnnounceURLSet[]{ set2, set1 });
		}
	}
		
	public static void
	announceGroupsSetFirst(
		TOTorrent	torrent,
		String		first_url )
	{
		List	groups = announceGroupsToList( torrent );
		
		boolean	found = false;
	
		outer:
		for (int i=0;i<groups.size();i++){
			
			List	set = (List)groups.get(i);
			
			for (int j=0;j<set.size();j++){
		
				if ( first_url.equals(set.get(j))){
			
					set.remove(j);
					
					set.add(0, first_url);
					
					groups.remove(set);
					
					groups.add(0,set);
	
					found = true;
					
					break outer;
				}
			}
		}
		
		if ( !found ){
			
			System.out.println( "TorrentUtils::announceGroupsSetFirst - failed to find '" + first_url + "'" );
		}
		
		listToAnnounceGroups( groups, torrent );
	}
	
	public static boolean
	announceGroupsContainsURL(
		TOTorrent	torrent,
		String		url )
	{
		List	groups = announceGroupsToList( torrent );
		
		for (int i=0;i<groups.size();i++){
			
			List	set = (List)groups.get(i);
			
			for (int j=0;j<set.size();j++){
		
				if ( url.equals(set.get(j))){
			
					return( true );
				}
			}
		}
		
		return( false );
	}
	
	public static boolean
	mergeAnnounceURLs(
		TOTorrent 	new_torrent,
		TOTorrent	dest_torrent )
	{
		if ( new_torrent == null || dest_torrent == null ){
			
			return( false);
		}
		
		List	new_groups 	= announceGroupsToList( new_torrent );
		List 	dest_groups = announceGroupsToList( dest_torrent );
		
		List	groups_to_add = new ArrayList();
		
		for (int i=0;i<new_groups.size();i++){
			
			List new_set = (List)new_groups.get(i);
			
			boolean	match = false;
			
			for (int j=0;j<dest_groups.size();j++){
				
				List dest_set = (List)dest_groups.get(j);
				
				boolean same = new_set.size() == dest_set.size();
				
				if ( same ){
					
					for (int k=0;k<new_set.size();k++){
						
						String new_url = (String)new_set.get(k);
						
						if ( !dest_set.contains(new_url)){
							
							same = false;
							
							break;
						}
					}
				}
				
				if ( same ){
					
					match = true;
					
					break;
				}
			}
			
			if ( !match ){
		
				groups_to_add.add( new_set );
			}
		}
		
		if ( groups_to_add.size() == 0 ){
			
			return( false );
		}
		
		for (int i=0;i<groups_to_add.size();i++){
			
			dest_groups.add(i,groups_to_add.get(i));
		}
		
		listToAnnounceGroups( dest_groups, dest_torrent );
		
		return( true );
	}
	
	public static boolean
	replaceAnnounceURL(
		TOTorrent		torrent,
		URL				old_url,
		URL				new_url )
	{
		boolean	found = false;
		
		String	old_str = old_url.toString();
		String	new_str = new_url.toString();
		
		List	l = announceGroupsToList( torrent );
		
		for (int i=0;i<l.size();i++){
			
			List	set = (List)l.get(i);
			
			for (int j=0;j<set.size();j++){
		
				if (((String)set.get(j)).equals(old_str)){
					
					found	= true;
					
					set.set( j, new_str );
				}
			}
		}
		
		if ( found ){
			
			listToAnnounceGroups( l, torrent );
		}
		
		if ( torrent.getAnnounceURL().toString().equals( old_str )){
			
			torrent.setAnnounceURL( new_url );
			
			found	= true;
		}
		
		if ( found ){
			
			try{
				writeToFile( torrent );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
				
				return( false );
			}
		}
		
		return( found );
	}
	
	public static void
	setResumeDataCompletelyValid(
		DownloadManagerState	download_manager_state )
	{
		DiskManagerFactory.setResumeDataCompletelyValid( download_manager_state );
	}
	
	public static String
	getLocalisedName(
		TOTorrent		torrent )
	{
		if (torrent == null) {
			return "";
		}
		try{
			String utf8Name = torrent.getUTF8Name();
			if (utf8Name != null) {
				return utf8Name;
			}
			
			LocaleUtilDecoder decoder = LocaleTorrentUtil.getTorrentEncodingIfAvailable( torrent );
			
			if ( decoder == null ){
				
				return( new String(torrent.getName(),Constants.DEFAULT_ENCODING));
			}
			
			return( decoder.decodeString(torrent.getName()));
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			return( new String( torrent.getName()));
		}
	}
	
	public static void
	setTLSTorrentHash(
		HashWrapper		hash )
	{
		tls.get().put( "hash", hash );
	}
	
	public static TOTorrent
	getTLSTorrent()
	{
		HashWrapper	hash = (HashWrapper)(tls.get()).get("hash");
		
		if ( hash != null ){
			
			try{
				AzureusCore	core = AzureusCoreFactory.getSingleton();
				
				DownloadManager dm = core.getGlobalManager().getDownloadManager( hash );
				
				if ( dm != null ){
							
					return( dm.getTorrent());
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		return( null );
	}
	
	public static void
	setTLSDescription(
		String		desc )
	{
		tls.get().put( "desc", desc );
	}
	
	public static String
	getTLSDescription()
	{
		return((String)tls.get().get( "desc" ));
	}
	
		/**
		 * get tls for cloning onto another thread
		 * @return
		 */
	
	public static Object
	getTLS()
	{
		return( new HashMap<String,Object>(tls.get()));
	}
	
	public static void
	setTLS(
		Object	obj )
	{
		Map<String,Object>	m = (Map<String,Object>)obj;
		
		Map<String,Object> tls_map = tls.get();
		
		tls_map.clear();
		
		tls_map.putAll(m);
	}
	
	public static URL
	getDecentralisedEmptyURL()
	{
		try{
			return( new URL( "dht://" ));
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			return( null );
		}
	}
	
	public static URL
	getDecentralisedURL(
		TOTorrent	torrent )
	{
		try{
			return( new URL( "dht://" + ByteFormatter.encodeString( torrent.getHash()) + ".dht/announce" ));
			
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( getDecentralisedEmptyURL());
		}
	}

	public static void
	setDecentralised(
		TOTorrent	torrent )
	{
   		torrent.setAnnounceURL( getDecentralisedURL( torrent ));
	}
		
	public static boolean
	isDecentralised(
		TOTorrent		torrent )
	{
		if ( torrent == null ){
			
			return( false );
		}
		
		return( isDecentralised( torrent.getAnnounceURL()));
	}
	

	public static boolean
	isDecentralised(
		URL		url )
	{
		if ( url == null ){
			
			return( false );
		}
		
		return( url.getProtocol().equalsIgnoreCase( "dht" ));
	}
	
	private static Map
	getAzureusProperties(
		TOTorrent	torrent )
	{
		Map	m = torrent.getAdditionalMapProperty( TOTorrent.AZUREUS_PROPERTIES );
		
		if ( m == null ){
			
			m = new HashMap();
			
			torrent.setAdditionalMapProperty( TOTorrent.AZUREUS_PROPERTIES, m );
		}
		
		return( m );
	}
	
	private static Map
	getAzureusPrivateProperties(
		TOTorrent	torrent )
	{
		Map	m = torrent.getAdditionalMapProperty( TOTorrent.AZUREUS_PRIVATE_PROPERTIES );
		
		if ( m == null ){
			
			m = new HashMap();
			
			torrent.setAdditionalMapProperty( TOTorrent.AZUREUS_PRIVATE_PROPERTIES, m );
		}
		
		return( m );
	}
	
	public static void
	setObtainedFrom(
		File			file,
		String			str )
	{
		try{
			TOTorrent	torrent = readFromFile( file, false, false );
			
			setObtainedFrom( torrent, str );
			
			writeToFile( torrent );
			
		} catch (TOTorrentException e) {
			// ignore, file probably not torrent
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	public static void
	setObtainedFrom(
		TOTorrent		torrent,
		String			str )
	{
		Map	m = getAzureusPrivateProperties( torrent );
			
		try{
			m.put( TORRENT_AZ_PROP_OBTAINED_FROM, str.getBytes( "UTF-8" ));
			
			fireAttributeListener( torrent, TORRENT_AZ_PROP_OBTAINED_FROM, str );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	public static String
	getObtainedFrom(
		TOTorrent		torrent )
	{
		Map	m = getAzureusPrivateProperties( torrent );

		byte[]	from = (byte[])m.get( TORRENT_AZ_PROP_OBTAINED_FROM );
		
		if ( from != null ){
			
			try{
				return( new String( from, "UTF-8" ));
				
			}catch( Throwable e ){
			
				Debug.printStackTrace(e);
			}
		}
		
		return( null );
	}
	
	public static void
	setPeerCache(
		TOTorrent		torrent,
		Map				pc )
	{
		Map	m = getAzureusPrivateProperties( torrent );
			
		try{
			m.put( TORRENT_AZ_PROP_PEER_CACHE, pc );
						
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	public static void
	setPeerCacheValid(
		TOTorrent		torrent )
	{
		Map	m = getAzureusPrivateProperties( torrent );
			
		try{
			m.put( TORRENT_AZ_PROP_PEER_CACHE_VALID, new Long( PC_MARKER ));
						
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	public static Map
	getPeerCache(
		TOTorrent		torrent )
	{
		try{
			Map	m = getAzureusPrivateProperties( torrent );
	
			Long value = (Long)m.get( TORRENT_AZ_PROP_PEER_CACHE_VALID );
			
			if ( value != null && value == PC_MARKER ){
			
				Map	pc = (Map)m.get( TORRENT_AZ_PROP_PEER_CACHE );
	
				return( pc );
			}
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
		
		return( null );
	}
	
	public static void
	setFlag(
		TOTorrent		torrent,
		int				flag,
		boolean			value )
	{
		Map	m = getAzureusProperties( torrent );
		
		Long	flags = (Long)m.get( TORRENT_AZ_PROP_TORRENT_FLAGS );
		
		if ( flags == null ){
			
			flags = new Long(0);
		}		
		
		m.put( TORRENT_AZ_PROP_TORRENT_FLAGS, new Long(flags.intValue() | flag ));
	}
		
	public static boolean
	getFlag(
		TOTorrent		torrent,
		int				flag )
	{
		Map	m = getAzureusProperties( torrent );
		
		Long	flags = (Long)m.get( TORRENT_AZ_PROP_TORRENT_FLAGS );
		
		if ( flags == null ){
			
			return( false );
		}

		return(( flags.intValue() & flag ) != 0 );
	}
	
	public static void
	setPluginStringProperty(
		TOTorrent		torrent,
		String			name,
		String			value )
	{
		Map	m = getAzureusProperties( torrent );
		
		Object obj = m.get( TORRENT_AZ_PROP_PLUGINS );
		
		Map	p;
		
		if ( obj instanceof Map ){
			
			p = (Map)obj;
			
		}else{
			
			p = new HashMap();
			
			m.put( TORRENT_AZ_PROP_PLUGINS, p );
		}
		
		if ( value == null ){
			
			p.remove( name );
			
		}else{
			
			p.put( name, value.getBytes());
		}
	}
	
	public static String
	getPluginStringProperty(
		TOTorrent		torrent,
		String			name )
	{
		Map	m = getAzureusProperties( torrent );
		
		Object	obj = m.get( TORRENT_AZ_PROP_PLUGINS );
		
		if ( obj instanceof Map ){
		
			Map p = (Map)obj;
			
			obj = p.get( name );
			
			if ( obj instanceof byte[]){
			
				return( new String((byte[])obj));
			}
		}
		
		return( null );
	}
	
	public static void
	setPluginMapProperty(
		TOTorrent		torrent,
		String			name,
		Map				value )
	{
		Map	m = getAzureusProperties( torrent );
		
		Object obj = m.get( TORRENT_AZ_PROP_PLUGINS );
		
		Map	p;
		
		if ( obj instanceof Map ){
			
			p = (Map)obj;
			
		}else{
			
			p = new HashMap();
			
			m.put( TORRENT_AZ_PROP_PLUGINS, p );
		}
		
		if ( value == null ){
			
			p.remove( name );
			
		}else{
			
			p.put( name, value );
		}
	}
	
	public static Map
	getPluginMapProperty(
		TOTorrent		torrent,
		String			name )
	{
		Map	m = getAzureusProperties( torrent );
		
		Object	obj = m.get( TORRENT_AZ_PROP_PLUGINS );
		
		if ( obj instanceof Map ){
		
			Map p = (Map)obj;
			
			obj = p.get( name );
			
			if ( obj instanceof Map ){
		
				return((Map)obj);
			}
		}
		
		return( null );
	}
	
	public static void
	setDHTBackupEnabled(
		TOTorrent		torrent,
		boolean			enabled )
	{
		Map	m = getAzureusProperties( torrent );
		
		m.put( TORRENT_AZ_PROP_DHT_BACKUP_ENABLE, new Long(enabled?1:0));
	}
	
	public static boolean
	getDHTBackupEnabled(
		TOTorrent	torrent )
	{
			// missing -> true
		
		Map	m = getAzureusProperties( torrent );
		
		Object	obj = m.get( TORRENT_AZ_PROP_DHT_BACKUP_ENABLE );
		
		if ( obj instanceof Long ){
		
			return( ((Long)obj).longValue() == 1 );
		}
		
		return( true );
	}
	
	public static boolean
	isDHTBackupRequested(
		TOTorrent	torrent )
	{
			// missing -> false
		
		Map	m = getAzureusProperties( torrent );
		
		Object obj = m.get( TORRENT_AZ_PROP_DHT_BACKUP_REQUESTED );
		
		if ( obj instanceof Long ){
		
			return( ((Long)obj).longValue() == 1 );
		}
		
		return( false );
	}
	
	public static void
	setDHTBackupRequested(
		TOTorrent		torrent,
		boolean			requested )
	{
		Map	m = getAzureusProperties( torrent );
		
		m.put( TORRENT_AZ_PROP_DHT_BACKUP_REQUESTED, new Long(requested?1:0));
	}
		
	
	public static boolean isReallyPrivate(TOTorrent torrent) {
		if ( torrent == null ){
			
			return( false );
		}	
		
		if ( UrlUtils.containsPasskey( torrent.getAnnounceURL())){
				
			return torrent.getPrivate();
		}
		
		return false;
	}
	
	public static boolean
	getPrivate(
		TOTorrent		torrent )
	{
		if ( torrent == null ){
			
			return( false );
		}	
			
		return( torrent.getPrivate());
	}
	
	public static void
	setPrivate(
		TOTorrent		torrent,
		boolean			_private )
	{
		if ( torrent == null ){
			
			return;
		}
		
		try{
			torrent.setPrivate( _private );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	public static Set
	getIgnoreSet()
	{
		return(getIgnoreSetSupport(false));
	}
	
	public static synchronized Set
	getIgnoreSetSupport(
		boolean	force )
	{
		if ( ignore_set == null || force ){
			
			Set		new_ignore_set	= new HashSet();
		    
			String	ignore_list = COConfigurationManager.getStringParameter( "File.Torrent.IgnoreFiles", TOTorrent.DEFAULT_IGNORE_FILES );
			
			if ( ignore_set == null ){
				
					// first time - add the listener
				
				COConfigurationManager.addParameterListener(
					"File.Torrent.IgnoreFiles",
					new ParameterListener()
					{
						public void 
						parameterChanged(
							String parameterName)
						{
							getIgnoreSetSupport( true );
						}
					});
			}
			
			int	pos = 0;
			
			while(true){
				
				int	p1 = ignore_list.indexOf( ";", pos );
				
				String	bit;
				
				if ( p1 == -1 ){
					
					bit = ignore_list.substring(pos);
					
				}else{
					
					bit	= ignore_list.substring( pos, p1 );
					
					pos	= p1+1;
				}
				
				new_ignore_set.add(bit.trim().toLowerCase());
				
				if ( p1 == -1 ){
					
					break;
				}
			}
			
			ignore_set = new_ignore_set;
		}
		
		return( ignore_set );
	}
	
	
	
		// this class exists to minimise memory requirements by discarding the piece hash values
		// when "idle" 
	
	private static final int	PIECE_HASH_TIMEOUT	= 3*60*1000;
	
	private static Map	torrent_delegates = new WeakHashMap();
	
	static{
		SimpleTimer.addPeriodicEvent(
			"TorrentUtils:pieceDiscard",
			PIECE_HASH_TIMEOUT/2,
			new TimerEventPerformer()
			{
				public void
				perform(
					TimerEvent	event )
				{
					long	now = SystemTime.getCurrentTime();
					
					synchronized( torrent_delegates ){
						
						Iterator it = torrent_delegates.keySet().iterator();
						
						while( it.hasNext()){
							
							((torrentDelegate)it.next()).discardPieces(now,false);
						}
					}
				}
			});
	}
	
	private static HashSet	torrentFluffKeyset = new HashSet(2);
	private static Map		fluffThombstone = new HashMap(1);
	
	/**
	 * Register keys that are used for heavyweight maps that should be discarded when the torrent is not in use
	 * Make sure these keys are only ever used for Map objects!
	 */
	public static void
	registerMapFluff(
		String[]		fluff )
	{
		synchronized (TorrentUtils.class)
		{
			for (int i = 0; i < fluff.length; i++)
				torrentFluffKeyset.add(fluff[i]);
		}
	}
	
	public interface
	ExtendedTorrent
		extends TOTorrent
	{
		public byte[][]
		peekPieces()
		    		
			throws TOTorrentException;
		
		public void
		setDiscardFluff(
			boolean	discard );
	}
	
	private static class
	torrentDelegate
		extends LogRelation
		implements ExtendedTorrent
	{
		private TOTorrent		delegate;
		private File			file;
		
		private boolean			fluff_dirty;
		
		private long			last_pieces_read_time	= SystemTime.getCurrentTime();
		
		protected
		torrentDelegate(
			TOTorrent		_delegate,
			File			_file )
		{
			delegate		= _delegate;
			file			= _file;
			
			synchronized( torrent_delegates ){
				
				torrent_delegates.put( this, null );
			}
		}
		
		public void
		setDiscardFluff(
			boolean	discard )
		{
			if ( discard && !torrentFluffKeyset.isEmpty() ){
				
				//System.out.println( "Discarded fluff for " + new String(getName()));
				
				try{
			   		getMonitor().enter();
					
			   		try{
				   			// if file is out of sync with fluff then force a write
				   		
				   		if ( fluff_dirty ){
				   			
					   		boolean[]	restored = restoreState( true, true );
					   		
					   		delegate.serialiseToBEncodedFile( file );
					   		
					   		fluff_dirty = false;
					   		
					   		if ( restored[0] ){
					   			
					   			discardPieces( SystemTime.getCurrentTime(), true );
					   		}
				   		}
				   		
				   		for(Iterator it = torrentFluffKeyset.iterator();it.hasNext();){
				   			
				   			delegate.setAdditionalMapProperty( (String)it.next(), fluffThombstone );
				   		}
			   		}catch( Throwable e ){
			   		
			   			Debug.printStackTrace( e );
			   		}
				}finally{
					
					getMonitor().exit();
				}
			}
		}
		
		public byte[]
		getName()
		{
			return( delegate.getName());
		}
				
		public boolean
		isSimpleTorrent()
		{
			return( delegate.isSimpleTorrent());
		}
				
		public byte[]
		getComment()
		{
			return( delegate.getComment());		
		}

		public void
		setComment(
			String		comment )
		{
			delegate.setComment( comment );
		}
				
		public long
		getCreationDate()
		{
			return( delegate.getCreationDate());
		}
		
		public void
		setCreationDate(
			long		date )
		{
			delegate.setCreationDate( date );
		}
		
		public byte[]
		getCreatedBy()
		{
			return( delegate.getCreatedBy());
		}
		
	 	public void
		setCreatedBy(
			byte[]		cb )
	   	{
	  		delegate.setCreatedBy( cb );
	   	}
	 	
		public boolean
		isCreated()
		{
			return( delegate.isCreated());
		}
		
		public URL
		getAnnounceURL()
		{
			return( delegate.getAnnounceURL());
		}

		public boolean
		setAnnounceURL(
			URL		url )
		{
			return delegate.setAnnounceURL( url );
		}
			
		
		public TOTorrentAnnounceURLGroup
		getAnnounceURLGroup()
		{
			return( delegate.getAnnounceURLGroup());
		}
		 
		protected void
		discardPieces(
			long		now,
			boolean		force )
		{
				// handle clock changes backwards
			
			if ( now < last_pieces_read_time && !force ){
				
				last_pieces_read_time	= now;
				
			}else{
			
				try{
					if(		( now - last_pieces_read_time > PIECE_HASH_TIMEOUT || force ) &&
							delegate.getPieces() != null ){
						
						try{
							getMonitor().enter();
							
							// System.out.println( "clearing pieces for '" + new String(getName()) + "'");

							delegate.setPieces( null );
						}finally{
							
							getMonitor().exit();
						}
					}
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
		
		public byte[][]
		getPieces()
		
			throws TOTorrentException
		{
			byte[][]	res = delegate.getPieces();
			
			last_pieces_read_time	= SystemTime.getCurrentTime();
		
			if ( res == null ){
						 
				// System.out.println( "recovering pieces for '" + new String(getName()) + "'");
				
				try{
			   		getMonitor().enter();

			   		restoreState( true, false );
			   		
			   		res = delegate.getPieces();
			   		
				}finally{
					
					getMonitor().exit();
				}
			}
			
			return( res );
		}

		/**
		 * monitor must be held before calling me
		 * @param do_pieces
		 * @param do_fluff
		 * @throws TOTorrentException
		 */
		
		protected boolean[]
		restoreState(
			boolean		do_pieces,
			boolean		do_fluff )
		
			throws TOTorrentException
		{
	   		boolean	had_pieces = delegate.getPieces() != null;
	   		
	   		boolean	had_fluff = true; 
	   		
	   		for(Iterator it = torrentFluffKeyset.iterator();it.hasNext();){
	   			
	   			had_fluff &= delegate.getAdditionalMapProperty( (String)it.next() ) != fluffThombstone;
	   		}

	   		if ( had_pieces ){
	   			
	   			do_pieces = false;
	   		}
	   		
	   		if ( had_fluff ){
	   			
	   			do_fluff = false;
	   		}
	   		
	   		if ( do_pieces || do_fluff ){
	   		
		   		TOTorrent	temp = readFromFile( file, false );
				
		   		if ( do_pieces ){
		   		
		   			byte[][] res	= temp.getPieces();
				
		   			delegate.setPieces( res );
		   		}
		   		
		   		if ( do_fluff ){
		   			
		   			for (Iterator it = torrentFluffKeyset.iterator(); it.hasNext();){
					
						String fluffKey = (String) it.next();
						
							// only update the discarded entries as non-discarded may be out of sync
							// with the file contents
						
						if ( delegate.getAdditionalMapProperty( fluffKey ) == fluffThombstone ){
							
							delegate.setAdditionalMapProperty(fluffKey, temp.getAdditionalMapProperty(fluffKey));
						}
					}
		   		}
	   		}
	   		
	   		return( new boolean[]{ do_pieces, do_fluff });
		}
		
			/**
			 * peeks the pieces, will return null if they are discarded
			 * @return
			 */
		
		public byte[][]
		peekPieces()
		
			throws TOTorrentException
		{
			return( delegate.getPieces());
		}
		
		public void
		setPieces(
			byte[][]	pieces )
		
			throws TOTorrentException
		{
			throw( new TOTorrentException( "Unsupported Operation", TOTorrentException.RT_WRITE_FAILS ));
		}
		
		public long
		getPieceLength()
		{
			return( delegate.getPieceLength());
		}

		public int
		getNumberOfPieces()
		{
			return( delegate.getNumberOfPieces());
		}
		
		public long
		getSize()
		{
			return( delegate.getSize());
		}
		
		public TOTorrentFile[]
		getFiles()
		{
			return( delegate.getFiles());
		}
				 
		public byte[]
		getHash()
					
			throws TOTorrentException
		{
			return( delegate.getHash());
		}
		
		public HashWrapper
		getHashWrapper()
					
			throws TOTorrentException
		{
			return( delegate.getHashWrapper());
		}
		
	   	public void 
    	setHashOverride(
    		byte[] hash ) 
    	
    		throws TOTorrentException 
    	{
    		throw( new TOTorrentException( "Not supported", TOTorrentException.RT_HASH_FAILS ));
    	}
	   	
		public boolean
		getPrivate()
		{
			return( delegate.getPrivate());
		}
		
		public void
		setPrivate(
			boolean	_private )
		
			throws TOTorrentException
		{
				// don't support this as it changes teh torrent hash
			
			throw( new TOTorrentException( "Can't amend private attribute", TOTorrentException.RT_WRITE_FAILS ));
		}
		
		public boolean
		hasSameHashAs(
			TOTorrent		other )
		{
			return( delegate.hasSameHashAs( other ));
		}
				
		public void
		setAdditionalStringProperty(
			String		name,
			String		value )
		{
			delegate.setAdditionalStringProperty( name, value );
		}
			
		public String
		getAdditionalStringProperty(
			String		name )
		{
			return( delegate.getAdditionalStringProperty( name ));
		}
			
		public void
		setAdditionalByteArrayProperty(
			String		name,
			byte[]		value )
		{
			delegate.setAdditionalByteArrayProperty( name, value );
		}
			
		public byte[]
		getAdditionalByteArrayProperty(
			String		name )
		{
			return( delegate.getAdditionalByteArrayProperty( name ));
		}
		
		public void
		setAdditionalLongProperty(
			String		name,
			Long		value )
		{
			delegate.setAdditionalLongProperty( name, value );
		}
			
		public Long
		getAdditionalLongProperty(
			String		name )
		{
			return( delegate.getAdditionalLongProperty( name ));
		}
			
		
		public void
		setAdditionalListProperty(
			String		name,
			List		value )
		{
			delegate.setAdditionalListProperty( name, value );
		}
			
		public List
		getAdditionalListProperty(
			String		name )
		{
			return( delegate.getAdditionalListProperty( name ));
		}
			
		public void
		setAdditionalMapProperty(
			String		name,
			Map			value )
		{
			if ( torrentFluffKeyset.contains(name)){

				//System.out.println( "Set fluff for " + new String(getName()) + " to " + value );

				try{
					getMonitor().enter();

					delegate.setAdditionalMapProperty( name, value );
					
					fluff_dirty = true;
					
				}finally{
					
					getMonitor().exit();
				}
			}else{
				
				delegate.setAdditionalMapProperty( name, value );	
			}
		}
			
		public Map
		getAdditionalMapProperty(
			String		name )
		{
			if (torrentFluffKeyset.contains(name)){
				
				try{
					getMonitor().enter();

					Map	result = delegate.getAdditionalMapProperty( name );
					
					if ( result == fluffThombstone ){
					
						try{
							restoreState( false, true );
							
							Map res = delegate.getAdditionalMapProperty( name );

							//System.out.println( "Restored fluff for " + new String(getName()) + " to " + res );

							return( res );
							
						}catch( Throwable e ){
							
							Debug.out( "Property '" + name + " lost due to torrent read error", e );
						}
					}
				}finally{
					
					getMonitor().exit();
				}
			}
			
			return( delegate.getAdditionalMapProperty( name ));
		}
		
		public Object getAdditionalProperty(String name) {
			if (torrentFluffKeyset.contains(name))
			{
				try
				{
					getMonitor().enter();
					
					Object result = delegate.getAdditionalProperty(name);
					if (result == fluffThombstone)
					{
						try
						{
							restoreState(false, true);
							Object res = delegate.getAdditionalProperty(name);
							//System.out.println( "Restored fluff for " + new String(getName()) + " to " + res );
							
							return (res);
							
						} catch (Throwable e)
						{
							Debug.out("Property '" + name + " lost due to torrent read error", e);
						}
					}
				} finally
				{
					getMonitor().exit();
				}
			}
			
			return delegate.getAdditionalProperty(name);
		}

		public void
		setAdditionalProperty(
			String		name,
			Object		value )
		{
			if ( torrentFluffKeyset.contains(name)){

				//System.out.println( "Set fluff for " + new String(getName()) + " to " + value );

				try{
					getMonitor().enter();

					delegate.setAdditionalProperty( name, value );
					
					fluff_dirty = true;
					
				}finally{
					
					getMonitor().exit();
				}
			}else{
			
				delegate.setAdditionalProperty( name, value );
			}
		}
		
		public void
		removeAdditionalProperty(
			String name )
		{
			if(delegate.getAdditionalProperty(name) == null)
				return;
			
			if ( torrentFluffKeyset.contains(name)){

				//System.out.println( "Set fluff for " + new String(getName()) + " to " + value );

				try{
					getMonitor().enter();
					
					delegate.removeAdditionalProperty( name );
					
					fluff_dirty = true;
					
				}finally{
					
					getMonitor().exit();
				}
			}else{
				
				delegate.removeAdditionalProperty( name );
			}
		}	
		
		public void
		removeAdditionalProperties()
		{
			try{
				getMonitor().enter();

				delegate.removeAdditionalProperties();
				
				fluff_dirty = true;
				
			}finally{
				
				getMonitor().exit();
			}
		}		

		public void
		serialiseToBEncodedFile(
			File		target_file )
			  
			throws TOTorrentException
		{
				// make sure pieces are current
			
			try{
		   		getMonitor().enter();
		   				   		
		   		boolean[]	restored = restoreState( true, true );
		   		
		   		delegate.serialiseToBEncodedFile( target_file );
		   		
		   		if ( target_file.equals( file )){
		   			
		   			fluff_dirty = false;
		   		}
		   		
		   		if ( restored[0] ){
		   			
		   			discardPieces( SystemTime.getCurrentTime(), true );
		   		}
		   		
		   		if ( restored[1] ){
		   			
		   			for (Iterator it = torrentFluffKeyset.iterator(); it.hasNext();){
		   				
		   				delegate.setAdditionalMapProperty( (String)it.next(), fluffThombstone );
		   			}
		   		}
			}finally{
				
				getMonitor().exit();
			}
		}


		public Map
		serialiseToMap()
			  
			throws TOTorrentException
		{
				// make sure pieces are current
			
			try{
		   		getMonitor().enter();
		   		
		   		boolean[]	restored = restoreState( true, true );
			
		   		Map	result = delegate.serialiseToMap();
		   		
		   		if ( restored[0] ){
		   			
		   			discardPieces( SystemTime.getCurrentTime(), true );
		   		}
		   		
		   		if ( restored[1]){
				
					for (Iterator it = torrentFluffKeyset.iterator(); it.hasNext();){
					
						delegate.setAdditionalMapProperty((String) it.next(), fluffThombstone);
					}
				}
		   		
		   		return( result );
		   		
			}finally{
				
				getMonitor().exit();
			}
		}


		public void
		serialiseToXMLFile(
			File		target_file )
			  
		   throws TOTorrentException
		{
				// make sure pieces are current
			
			try{
		   		getMonitor().enter();
		   		
		   		boolean[]	restored = restoreState( true, true );
			
		   		delegate.serialiseToXMLFile( target_file );
			
		   		if ( restored[0] ){
		   			
		   			discardPieces( SystemTime.getCurrentTime(), true );
		   		}
		   		
		   		if ( restored[1]){
				
					for (Iterator it = torrentFluffKeyset.iterator(); it.hasNext();){
					
						delegate.setAdditionalMapProperty((String) it.next(), fluffThombstone);
					}
				}
			}finally{
				
				getMonitor().exit();
			}
		}

	 	public void
		addListener(
			TOTorrentListener		l )
		{
	 		delegate.addListener( l );
		}

		public void
		removeListener(
			TOTorrentListener		l )
		{
	 		delegate.removeListener( l );
		}
		
		public AEMonitor
		getMonitor()
		{
	   		return( delegate.getMonitor());
		}


		public void
		print()
		{
			delegate.print();
		}

		public String getRelationText() {
			if (delegate instanceof LogRelation)
				return ((LogRelation)delegate).getRelationText();
			return delegate.toString();
		}

		public Object[] getQueryableInterfaces() {
			if (delegate instanceof LogRelation)
				return ((LogRelation)delegate).getQueryableInterfaces();
			return super.getQueryableInterfaces();
		}

		public String getUTF8Name() {
			return delegate.getUTF8Name();
		}
	}

	/**
	 * Copy a file to the Torrent Save Directory, taking into account all the
	 * user config options related to that.
	 * <p>
	 * Also makes the directory if it doesn't exist.
	 *  
	 * @param f File to copy
	 * @param persistent Whether the torrent is persistent
	 * @return File after it's been copied (may be the same as f)
	 * @throws IOException
	 */
	public static File copyTorrentFileToSaveDir(File f, boolean persistent)
			throws IOException {
		File torrentDir;
		boolean saveTorrents = persistent
				&& COConfigurationManager.getBooleanParameter("Save Torrent Files");
		if (saveTorrents)
			torrentDir = new File(COConfigurationManager
					.getDirectoryParameter("General_sDefaultTorrent_Directory"));
		else
			torrentDir = new File(f.getParent());

		//if the torrent is already in the completed files dir, use this
		//torrent instead of creating a new one in the default dir
		boolean moveWhenDone = COConfigurationManager.getBooleanParameter("Move Completed When Done");
		String completedDir = COConfigurationManager.getStringParameter(
				"Completed Files Directory", "");
		if (moveWhenDone && completedDir.length() > 0) {
			File cFile = new File(completedDir, f.getName());
			if (cFile.exists()) {
				//set the torrentDir to the completedDir
				torrentDir = new File(completedDir);
			}
		}

		FileUtil.mkdirs(torrentDir);

		File fDest = new File(torrentDir, f.getName().replaceAll("%20", "."));
		if (fDest.equals(f)) {
			return f;
		}

		while (fDest.exists()) {
			fDest = new File(torrentDir, "_" + fDest.getName());
		}

		fDest.createNewFile();

		if (!FileUtil.copyFile(f, fDest)) {
			throw new IOException("File copy failed");
		}

		return fDest;
	}

	/**
	 * Get the DownloadManager related to a torrent's hashBytes
	 * 
	 * @param hashBytes
	 * @return
	 */
  public static DownloadManager getDownloadManager( HashWrapper	hash ) {
		try {
			return AzureusCoreFactory.getSingleton().getGlobalManager()
					.getDownloadManager(hash);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Deletes the given dir and all dirs underneath if empty.
	 * Don't delete default save path or completed files directory, however,
	 * allow deletion of their empty subdirectories
	 * Files defined to be ignored for the sake of torrent creation are automatically deleted
	 * For example, by default this includes thumbs.db
	 */
	public static void recursiveEmptyDirDelete(File f) {
		TorrentUtils.recursiveEmptyDirDelete(f, true);
	}

	/**
	 * Same as #recursiveEmptyDirDelete(File), except allows disabling of logging
	 * of any warnings
	 * 
	 * @param f Dir to delete
	 * @param log_warnings Whether to log warning
	 */
	public static void recursiveEmptyDirDelete(File f, boolean log_warnings) {
		Set ignore_map = getIgnoreSet();

		FileUtil.recursiveEmptyDirDelete(f, ignore_map, log_warnings);
	}

	/**
	 * A nice string of a Torrent's hash
	 * 
	 * @param torrent Torrent to fromat hash of
	 * @return Hash string in a nice format
	 */
	public static String nicePrintTorrentHash(TOTorrent torrent) {
		return nicePrintTorrentHash(torrent, false);
	}

	/**
	 * A nice string of a Torrent's hash
	 * 
	 * @param torrent Torrent to fromat hash of
	 * @param tight No spaces between groups of numbers
	 * 
	 * @return Hash string in a nice format
	 */
	public static String nicePrintTorrentHash(TOTorrent torrent, boolean tight) {
		byte[] hash;

		if (torrent == null) {

			hash = new byte[20];
		} else {
			try {
				hash = torrent.getHash();

			} catch (TOTorrentException e) {

				Debug.printStackTrace(e);

				hash = new byte[20];
			}
		}

		return (ByteFormatter.nicePrint(hash, tight));
	}

	/**
	 * Runs a file through a series of test to verify if it is a torrent.
	 * 
	 * @param filename File to test
	 * @return true - file is a valid torrent file
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static boolean isTorrentFile(String filename) throws FileNotFoundException, IOException {
	  File check = new File(filename);
	  if (!check.exists())
	    throw new FileNotFoundException("File "+filename+" not found.");
	  if (!check.canRead())
	    throw new IOException("File "+filename+" cannot be read.");
	  if (check.isDirectory())
	    throw new FileIsADirectoryException("File "+filename+" is a directory.");
	  try {
	    TOTorrentFactory.deserialiseFromBEncodedFile(check);
	    return true;
	  } catch (Throwable e) {
	    return false;
	  }
	}
	
	public static void
	addCreatedTorrent(
		TOTorrent		torrent )
	{
		synchronized( created_torrents ){
			
			try{
				byte[]	hash = torrent.getHash();
				
				//System.out.println( "addCreated:" + new String(torrent.getName()) + "/" + ByteFormatter.encodeString( hash ));
				
				if ( created_torrents.size() == 0 ){
					
					COConfigurationManager.setParameter( "my.created.torrents", created_torrents );
				}
				
				HashWrapper	hw = new HashWrapper( hash );
				
				if ( !created_torrents_set.contains( hw )){
					
					created_torrents.add( hash );
				
					created_torrents_set.add( hw );
				
					COConfigurationManager.setDirty();
				}
			}catch( TOTorrentException e ){
				
			}
		}
	}
	
	public static void
	removeCreatedTorrent(
		TOTorrent		torrent )
	{
		synchronized( created_torrents ){

			try{
				HashWrapper	hw = torrent.getHashWrapper();
				
				byte[]		hash	= hw.getBytes();
				
				//System.out.println( "removeCreated:" + new String(torrent.getName()) + "/" + ByteFormatter.encodeString( hash ));

				Iterator	it = created_torrents.iterator();
				
				while( it.hasNext()){
					
					byte[]	h = (byte[])it.next();
					
					if ( Arrays.equals( hash, h )){
						
						it.remove();
					}
				}
				
				COConfigurationManager.setDirty();

				created_torrents_set.remove( hw );
				
			}catch( TOTorrentException e ){
				
			}
		}
	}
	
	public static boolean
	isCreatedTorrent(
		TOTorrent		torrent )
	{
		synchronized( created_torrents ){

			try{
				HashWrapper	hw = torrent.getHashWrapper();
				
				boolean	res = created_torrents_set.contains( hw );
				
					// if we don't have a persistent record of creation, check the non-persisted version
				
				if ( !res ){
					
					res = torrent.isCreated();
				}
				
				// System.out.println( "isCreated:" + new String(torrent.getName()) + "/" + ByteFormatter.encodeString( hw.getBytes()) + " -> " + res );

				return( res );
				
			}catch( TOTorrentException e ){
				
				Debug.printStackTrace(e);
				
				return( false );
				
			}
		}
	}
		
	private static void
	fireAttributeListener(
		TOTorrent		torrent,	
		String			attribute,
		Object			value )
	{
		Iterator it = torrent_attribute_listeners.iterator();
		
		while( it.hasNext()){
			
			try{
				((torrentAttributeListener)it.next()).attributeSet(torrent, attribute, value);
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
		
	public static void
	addTorrentAttributeListener(
		torrentAttributeListener	listener )
	{
		torrent_attribute_listeners.add( listener );
	}
	
	public static void
	removeTorrentAttributeListener(
		torrentAttributeListener	listener )
	{
		torrent_attribute_listeners.remove( listener );
	}
	
	public interface
	torrentAttributeListener
	{
		public void
		attributeSet(
			TOTorrent	torrent,
			String		attribute,
			Object		value );
	}
}
