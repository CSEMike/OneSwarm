package edu.washington.cs.oneswarm.watchdir;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.StringList;

import edu.washington.cs.oneswarm.ui.gwt.client.newui.FileTypeFilter;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.settings.MagicWatchType;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants.InOrderType;

public class MagicDecider {

	private static Logger logger = Logger.getLogger(MagicDecider.class.getName());
	
//	/**
//	 * placeholder
//	 */
//	public static enum FileTypeFilter {
//		Other,
//		Audio,
//		Videos
//	};
//	
//	/**
//	 * taken from edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants.InOrderType
//	 * remember to keep in sync
//	 */
//	public static enum InOrderType {
//		JPG("jpg", false, "image/jpeg", "image", FileTypeFilter.Other),
//
//		JPEG("jpeg", false, "image/jpeg", "image", FileTypeFilter.Other),
//
//		GIF("gif", false, "image/gif", "image", FileTypeFilter.Other),
//
//		PNG("png", false, "image/png", "image", FileTypeFilter.Other),
//
//		MP3("mp3", false, "audio/mpeg", "audio", FileTypeFilter.Audio),
//		M4A("m4a", false, "audio/mpeg", "audio", FileTypeFilter.Audio),
//		// MP3("mp3", true, "video/x-FLV", "audio"),
//
//		FLV("flv", false, "video/x-FLV", "video", FileTypeFilter.Audio),
//
//		FL_MP4("flash_ready.mp4", false, "video/mp4", "video", FileTypeFilter.Videos),
//
//		AAC("aac", false, "audio/x-aac", "audio", FileTypeFilter.Audio),
//
//		AVI("avi", true, "video/x-FLV", "video", FileTypeFilter.Videos),
//
//		MPEG("mpeg", true, "video/x-FLV", "video", FileTypeFilter.Videos),
//
//		MPG("mpg", true, "video/x-FLV", "video", FileTypeFilter.Videos),
//
//		XVID("xvid", true, "video/x-FLV", "video", FileTypeFilter.Videos),
//
//		DIVX("divx", true, "video/x-FLV", "video", FileTypeFilter.Videos),
//
//		MOV("mov", true, "video/x-FLV", "video", FileTypeFilter.Videos),
//
//		WMV("wmv", true, "video/x-FLV", "video", FileTypeFilter.Videos),
//
//		MKV("mkv", true, "video/x-FLV", "video", FileTypeFilter.Videos),
//
//		MP4("mp4", true, "video/x-FLV", "video", FileTypeFilter.Videos),
//		
//		M4V("m4v", true, "video/x-FLV", "video", FileTypeFilter.Videos),
//
//		WMA("wma", true, "video/x-FLV", "video", FileTypeFilter.Audio), ;
//
//		public final boolean convertNeeded;
//
//		public final String suffix;
//
//		public final String convertedMime;
//
//		public final String jwPlayerType;
//		public final FileTypeFilter type;
//
//		InOrderType(String suffix, boolean convertNeeded, String convertedMime, String jwPlayerType, FileTypeFilter type) {
//			this.suffix = suffix;
//			this.convertNeeded = convertNeeded;
//			this.convertedMime = convertedMime;
//			this.jwPlayerType = jwPlayerType;
//			this.type = type;
//		}
//		
//		public FileTypeFilter getFileTypeFilter()
//		{
//			return type;
//		}
//
//		public static InOrderType getType(String filename) {
//			if (filename != null) {
//				for (InOrderType type : values()) {
//					if (filename.toLowerCase().endsWith(type.suffix)) {
//						return type;
//					}
//				}
//			}
//			return null;
//		}
//	}
	
	public static List<File> decideTorrents( UpdatingFileTree root, MagicWatchType type, List<String> exclusions, StringList watchdirs )
	{
		List<File> out = new ArrayList<File>();
		boolean inspectKids = true;
		
		/**
		 * Simple file? 
		 */
		if( root.isDirectory() == false )
		{
			if( (acceptSingleFile(root) || type.equals(MagicWatchType.Everything)) && !isExcluded(exclusions, root) ) // at least 1 MB in size
			{
				out.add(root.thisFile);
			}
			
			return out;
		}
		
		boolean excludeThis = false;
		if( watchdirs != null ) {
			for( int i=0; i<watchdirs.size(); i++ ) {
				if( watchdirs.get(i) == null ) {
					continue;
				}
				
				if( (new File(watchdirs.get(i).substring(1))).equals(root.getThisFile()) ) {
					excludeThis = true;
				}
			}
		}
		
		/**
		 * movie directory?
		 */
		if( !excludeThis && checkMovieDirectory(root) && !isExcluded(exclusions, root) )
		{
			out.add(root.thisFile);
			logger.finer("movie dir: " + root.thisFile.getName());
			inspectKids = false;
		}
		/**
		 * ordered directory?
		 */
		else if( !excludeThis && checkOrderedDirectory(root) && !isExcluded(exclusions, root) )
		{
			out.add(root.thisFile);
			logger.finer("ordered dir: " + root.thisFile.getName());
			inspectKids = false;
		}
		/**
		 * audio directory?
		 */
		else if( !excludeThis && checkAudioDirectory(root) && !isExcluded(exclusions, root) )
		{
			out.add(root.thisFile);
			logger.finer("audio dir: " + root.thisFile.getName());
			inspectKids = false;
		}
		
		if( inspectKids == true )
		{
			logger.finer("not grouped dir: " + root.thisFile.getName());
			
			UpdatingFileTree [] kids = root.getChildren().toArray(new UpdatingFileTree[0]);
			for( UpdatingFileTree c : kids )
			{
				out.addAll(decideTorrents(c, type, exclusions, watchdirs));
			}
		}
		
		return out;
	}
	
	enum Tag {
		X("x"), 
		OF("of"),
		E("e");
		
		private String mKey;
		
		private Tag( String key ) {
			mKey = key.toLowerCase();
		}
		
		public boolean match( String fnameOrig ) {
			String fname = fnameOrig.toLowerCase();
			int offset = 0;
			int candidate = fname.indexOf(mKey, offset);
			while( candidate != -1 )
			{
				if( candidate > 0 && candidate < fname.length() - 1 )
				{
					if( Character.isDigit(fname.charAt(candidate-1)) && Character.isDigit(fname.charAt(candidate+1)) )
					{
						return true;
					}
				}	
				offset = candidate + 1;
				candidate = fname.indexOf(mKey, offset);
			}
			return false;
		}
	}
	
	private static boolean checkOrderedDirectory( UpdatingFileTree root ) {
		
		if( root.mHasDirectoryChildren == true )
			return false;
		
		/**
		 * just like movie dir...
		 */
		List<String> fnames = flatMovieFilenameList(root);
		if( fnames.size() == 0 )
			return false;
		
		String ext = null;
		Tag tag = null;
		
		for( String f : fnames )
		{
			if( f.length() < 5 ) // "<something>.{avi, mp4, mkv, etc}"
				return false;
			
			InOrderType t = InOrderType.getType(f);
			if( t == null )
			{
				return false;
			}
			else if( t.getFileTypeFilter().equals(FileTypeFilter.Videos) == false )
			{
				return false;
			}
			else
			{
				//System.out.println("file is video: " + f);
			}
			
//			if( ext == null )
//			{
//				ext = f.substring(f.length()-4); // we may or may not miss the dot depending on whether its .divx or .avi, but this is no big deal
//			}
//			else
//			{
//				if( f.substring(f.length()-4).equals(ext) == false )
//				{
//					return false;
//				}
//			}
//			
			for( Tag candidate : Tag.values() )
			{
				if( candidate.match(f) && tag == null )
					tag = candidate;
			}
		}
		
		if( tag == null )
		{
			return false;
		}
		
		/**
		 * except instead of edit distance, check that each file has a consistent tag
		 */
		int nonconforming = 0, conforming = 0;
		for( String f : fnames )
		{
			if( tag.match(f) == false )
			{
				nonconforming++;
			}
			else
			{
				conforming++;
			}
		}
		
		logger.finer(root.getThisFile().getName() + " conf: " + conforming + " nonconf: " + nonconforming);
		return ((double)nonconforming <= Math.max(((double)conforming * 0.1), 3));
	}

	static boolean isExcluded(List<String> exclusions, UpdatingFileTree root) {
		for( String s : exclusions.toArray(new String[0]) )
		{
			if( root.thisFile.getAbsolutePath().startsWith(s) )
			{
				//System.out.println(root + " excluded by " + s);
				return true;
			}
//			if( s.startsWith(root.thisFile.getAbsolutePath()) )
//			{
//				System.out.println(root + " excluded by " + s);
//				return true;
//			}
		}
		return false;
	}

	private static boolean acceptSingleFile(UpdatingFileTree root) {
		
		/**
		 * Special case user request: PDF files
		 */
		if( root.thisFile.getName().endsWith(".pdf") ) {
			return true;
		}
		
		if( root.thisFile.length() < 1 * 1048576 ) // at least 1 MB
		{
			return false;
		}
		
		InOrderType t = InOrderType.getType(root.thisFile.getName());
		if( t == null )
		{
			return false;
		}
		else if( t.getFileTypeFilter().equals(FileTypeFilter.Other) )
		{
			return false;
		}
		else if( t.getFileTypeFilter().equals(FileTypeFilter.Audio) )
		{
			/**
			 *  don't create single-file audio torrents automatically. good chance these are not intended
			 */
			return false; 
		}
		
		/**
		 * Don't create if a torrent file already exists in the same dir
		 */
		if( (new File(root.thisFile.getAbsolutePath()+".torrent")).exists() )
		{
			return false;
		}
		
		return true;
	}

	private static List<String> flatMovieFilenameList(UpdatingFileTree root)
	{
		List<String> out = new LinkedList<String>();
		for( UpdatingFileTree f : root.getChildren().toArray(new UpdatingFileTree[0]) )
		{
			String fname = f.thisFile.getName().toLowerCase();
			if( f.isDirectory() == false )
			{
				if( fname.endsWith(".info") ||
					fname.endsWith(".nfo") ||
					fname.endsWith(".srt") ||
					fname.equals("desktop.ini") ||
					fname.equals("thumbs.db") )
				{
					continue;
				}
				else
				{
					out.add(f.thisFile.getName());
				}
			}
			else
			{
				// skip standard dirs
				if( fname.equals("sample") ||
					fname.equals("subs") )
				{
					continue;
				}
				else
				{
					out.addAll(flatMovieFilenameList(f));
				}
			}
		}
		return out;
	}
	
	public static boolean checkAudioDirectory( UpdatingFileTree root )
	{
		if( root.getDirectoryChildren().size() > 0 )
			return false;
		
		List<UpdatingFileTree> kids = root.getChildren();
		if( kids.size() == 0 )
			return false;
		
		int covers = 0;
		
		for( UpdatingFileTree f : kids )
		{
			InOrderType t = InOrderType.getType(f.thisFile.getName());
			boolean bad = false;
			if( t == null )
			{
				bad = true;
			}
			else if( t.getFileTypeFilter() != FileTypeFilter.Audio )
			{
				bad = true;	
			}
			
			if( bad )
			{
				String n = f.thisFile.getName().toLowerCase();
				if( !(n.endsWith(".jpg") || 
					  n.endsWith(".png") ||
					  n.endsWith(".bmp") ||
					  n.equals("desktop.ini") ||
					  n.equals("thumbs.db") ||
					  n.equals(".ds_store") ||
					  n.endsWith(".sfv") ||
					  n.endsWith(".m3u") ||
					  n.endsWith(".nfo") )) // covers
				{
					return false;
				}
			}
		}
		
		return true;
	}
	
	private static boolean checkMovieDirectory( UpdatingFileTree root )
	{
		/**
		 * Get a flat list of all the files in all subdirectories and check 
		 * to see if they are recognized types
		 */
		List<String> fnames = flatMovieFilenameList(root);
		if( fnames.size() == 0 )
			return false;
		
		String ext = null;
		
		for( String f : fnames )
		{
			if( f.length() < 5 ) // "<something>.{avi, mp4, mkv, etc}"
				return false;
			
			InOrderType t = InOrderType.getType(f);
			if( t == null )
			{
				return false;
			}
			else if( t.getFileTypeFilter().equals(FileTypeFilter.Videos) == false )
			{
				return false;
			}
			else
			{
				logger.finest("file is video: " + f);
			}
			
			if( ext == null )
			{
				ext = f.substring(f.length()-4); // we may or may not miss the dot depending on whether its .divx or .avi, but this is no big deal
			}
			else
			{
				if( f.substring(f.length()-4).equals(ext) == false )
				{
					return false;
				}
			}
		}
		
		/**
		 * Finally check edit distance. most files should conform to: abc1.avi, abc2.avi, etc if multipart
		 */
		String tester = null;
		for( String f : fnames )
		{
			if( tester == null )
				tester = f;
			else
			{
				if( edit_distance(tester, f) > 3 )
					return false;
			}
		}
		
		return true;
	}
	
	// from: http://www.merriampark static .com/ld.htm
	public static int edit_distance(String s, String t) {
		int d[][]; // matrix
		int n; // length of s
		int m; // length of t
		int i; // iterates through s
		int j; // iterates through t
		char s_i; // ith character of s
		char t_j; // jth character of t
		int cost; // cost

		// Step 1
		n = s.length();
		m = t.length();
		if (n == 0) {
			return m;
		}
		if (m == 0) {
			return n;
		}
		d = new int[n + 1][m + 1];

		// Step 2
		for (i = 0; i <= n; i++) {
			d[i][0] = i;
		}

		for (j = 0; j <= m; j++) {
			d[0][j] = j;
		}

		// Step 3
		for (i = 1; i <= n; i++) {
			s_i = s.charAt(i - 1);
			// Step 4
			for (j = 1; j <= m; j++) {
				t_j = t.charAt(j - 1);

				// Step 5
				if (s_i == t_j) {
					cost = 0;
				} else {
					cost = 1;
				}

				// Step 6
				d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
			}

		}
		// Step 7
		return d[n][m];
	}
}
