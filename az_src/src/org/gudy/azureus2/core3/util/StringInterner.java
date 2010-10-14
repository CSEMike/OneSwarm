/*
 * Created on Jun 8, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package org.gudy.azureus2.core3.util;

import java.io.File;
import java.lang.ref.*;
import java.util.*;

import com.aelitis.azureus.core.util.HashCodeUtils;


public class 
StringInterner 
{
	private static final int SCHEDULED_CLEANUP_INTERVAL = 60*1000;
	
	private static final boolean TRACE_CLEANUP = false;
	private static final boolean TRACE_MULTIHITS = false;
	
	
	private static final int IMMEDIATE_CLEANUP_TRIGGER = 2000;
	private static final int IMMEDIATE_CLEANUP_GOAL = 1500;
	private static final int SCHEDULED_CLEANUP_TRIGGER = 1500;
	private static final int SCHEDULED_CLEANUP_GOAL = 1000;	
	private static final int SCHEDULED_AGING_THRESHOLD = 750;
	
	private static LightHashSet interningSet = new LightHashSet(800);
	
	private final static ReferenceQueue refQueue = new ReferenceQueue();
	
	private static final String[] COMMON_KEYS = {
		"src","port","prot","ip","udpport","azver","httpport","downloaded",
		"Content","Refresh On","path.utf-8","uploaded","completed","persistent","attributes","encoding",
		"azureus_properties","stats.download.added.time","networks","p1","resume data","dndflags","blocks","resume",
		"primaryfile","resumecomplete","data","peersources","name.utf-8","valid","torrent filename","parameters",
		"secrets","timesincedl","tracker_cache","filedownloaded","timesinceul","tracker_peers","trackerclientextensions","GlobalRating",
		"comment.utf-8","Count","String","stats.counted","Thumbnail","Plugin.<internal>.DDBaseTTTorrent::sha1","type","Title",
		"displayname","Publisher","Creation Date","Revision Date","Content Hash","flags","stats.download.completed.time","Description",
		"Progressive","Content Type","QOS Class","DRM","hash","ver","id",
		"body","seed","eip","rid","iip","dp2","tp","orig",
		"dp","Quality","private","dht_backup_enable","max.uploads","filelinks","Speed Bps","cdn_properties",
		"sha1","ed2k","DRM Key","Plugin.aeseedingengine.attributes","initial_seed","dht_backup_requested","ta","size",
		"DIRECTOR PUBLISH","Plugin.azdirector.ContentMap","dateadded","bytesin","announces","status","bytesout","scrapes",
		"passive",
	};
	
	private static final ByteArrayHashMap	byte_map = new ByteArrayHashMap( COMMON_KEYS.length );
	
	static{
		try{
			for (int i=0;i<COMMON_KEYS.length;i++){
				
				byte_map.put( COMMON_KEYS[i].getBytes(Constants.BYTE_ENCODING), COMMON_KEYS[i] );
				interningSet.add(new WeakStringEntry(COMMON_KEYS[i]));
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
		
			// initialisation nightmare - we have to create periodic event async to avoid
			// circular class loading issues when azureus.config is borkified
		
		new AEThread2( "asyncify", true )
		{
			public void
			run()
			{
				SimpleTimer.addPeriodicEvent("StringInterner:cleaner", SCHEDULED_CLEANUP_INTERVAL, new TimerEventPerformer() {
					public void perform(TimerEvent event) {
						sanitize(true);
					}
				});
			}
		}.start();
	}
		
	// private final static ReferenceQueue queue = new ReferenceQueue();

	
	public static String
	intern(
		byte[]	bytes )
	{
		String res = (String)byte_map.get( bytes );
		
		// System.out.println( new String( bytes ) + " -> " + res );
		
		return( res );
	}
	
	public static String intern(String toIntern) {
		
		if(toIntern == null)
			return null;
		
		String internedString;
		
		synchronized( StringInterner.class ){

			sanitize(false);


			WeakStringEntry checkEntry = new WeakStringEntry(toIntern);
			WeakStringEntry internedEntry = (WeakStringEntry) interningSet.get(checkEntry);

			if (internedEntry == null || (internedString = internedEntry.getString()) == null)
			{
				internedString = toIntern;
				if(!interningSet.add(checkEntry))
					System.out.println("unexpected modification");  // should not happen
			} else
			{
				internedEntry.incHits();
				checkEntry.destroy();
				if(TRACE_MULTIHITS && internedEntry.hits % 10 == 0)
					System.out.println("multihit "+internedEntry);
			}

		}
		
		// should not happen
		if(!toIntern.equals(internedString))
			System.err.println("mismatch");
		
		return internedString;
	}
	
	public static byte[] internBytes(byte[] toIntern) {
		
		if(toIntern == null)
			return null;
		
		byte[] internedArray;
		
		synchronized( StringInterner.class ){
			
			sanitize(false);
			
			WeakByteArrayEntry checkEntry = new WeakByteArrayEntry(toIntern);
			WeakByteArrayEntry internedEntry = (WeakByteArrayEntry) interningSet.get(checkEntry);
			
			if (internedEntry == null || (internedArray = internedEntry.getArray()) == null)
			{
				internedArray = toIntern;
				if(!interningSet.add(checkEntry))
					System.out.println("unexpected modification");  // should not happen
			} else
			{
				internedEntry.incHits();
				checkEntry.destroy();
				if(TRACE_MULTIHITS && internedEntry.hits % 10 == 0)
					System.out.println("multihit"+internedEntry);
			}
		}
		
		// should not happen
		if(!Arrays.equals(toIntern, internedArray))
			System.err.println("mismatch");
		
		return internedArray;
	}
	
	/**
	 * This is based on File.hashCode() and File.equals(), which can return different values for different representations of the same paths.
	 * Thus internFile should be used with canonized Files exclusively
	 */
	public static File internFile(File toIntern) {
		
		if(toIntern == null)
			return null;
		
		File internedFile;
		
		synchronized( StringInterner.class ){
			
			sanitize(false);
					
			WeakFileEntry checkEntry = new WeakFileEntry(toIntern);
			WeakFileEntry internedEntry = (WeakFileEntry) interningSet.get(checkEntry);
			
			if (internedEntry == null || (internedFile = internedEntry.getFile()) == null)
			{
				internedFile = toIntern;
				if(!interningSet.add(checkEntry))
					System.out.println("unexpected modification"); // should not happen
			} else
			{
				internedEntry.incHits();
				checkEntry.destroy();
				if(TRACE_MULTIHITS && internedEntry.hits % 10 == 0)
					System.out.println("multihit"+internedEntry);
			}
		}
		
		// should not happen
		if(!toIntern.equals(internedFile))
			System.err.println("mismatch");
		
		return internedFile;
	}
	
	
	private final static Comparator	savingsComp	= new Comparator()
												{
													public int compare(Object o1, Object o2) {
														WeakEntry w1 = (WeakEntry) o1;
														WeakEntry w2 = (WeakEntry) o2;
														return w1.hits * w1.size - w2.hits * w2.size;
													}
												};
	
	private static void sanitize(boolean scheduled)
	{
		synchronized( StringInterner.class ){
			
			WeakEntry ref;
			while((ref = (WeakEntry)(refQueue.poll())) != null)
			{
				if(!ref.isDestroyed())
				{
					interningSet.remove(ref);
					if(TRACE_CLEANUP && ref.hits > 30)
						System.out.println("queue remove:"+ref);
				} else
				{// should not happen
					System.err.println("double removal "+ref);					
				}
			}
				
			
			int currentSetSize = interningSet.size();
			
			aging:
			{
				cleanup:
				{
					// unscheduled cleanup/aging only in case of emergency
					if (currentSetSize < IMMEDIATE_CLEANUP_TRIGGER && !scheduled)
						break aging;
					
					if (TRACE_CLEANUP)
						System.out.println("Doing cleanup " + currentSetSize);
					
					ArrayList remaining = new ArrayList();
					
					// remove objects that aren't shared by multiple holders first (interning is useless)
					for (Iterator it = interningSet.iterator(); it.hasNext();)
					{
						if (interningSet.size() < IMMEDIATE_CLEANUP_GOAL && !scheduled)
							break aging;
						WeakEntry entry = (WeakEntry) it.next();
						if (entry.hits == 0)
						{
							if (TRACE_CLEANUP)
								System.out.println("0-remove: " + entry);
							it.remove();
						} else
							remaining.add(entry);
					}
					
					currentSetSize = interningSet.size();
					if (currentSetSize < SCHEDULED_CLEANUP_TRIGGER && scheduled)
						break cleanup;
					if (currentSetSize < IMMEDIATE_CLEANUP_GOAL && !scheduled)
						break aging;
					
					Collections.sort(remaining, savingsComp);
					// remove those objects that saved the least amount first
					weightedRemove: for (int i = 0; i < remaining.size(); i++)
					{
						currentSetSize = interningSet.size();
						if (currentSetSize < SCHEDULED_CLEANUP_GOAL && scheduled)
							break weightedRemove;
						if (currentSetSize < IMMEDIATE_CLEANUP_GOAL && !scheduled)
							break aging;
						WeakEntry entry = (WeakEntry) remaining.get(i);
						if (TRACE_CLEANUP)
							System.out.println("weighted remove: " + entry);
						interningSet.remove(entry);
					}
				}
			
			
				currentSetSize = interningSet.size();
				if (currentSetSize < SCHEDULED_AGING_THRESHOLD && scheduled)
					break aging;
				if (currentSetSize < IMMEDIATE_CLEANUP_GOAL && !scheduled)
					break aging;
				for (Iterator it = interningSet.iterator(); it.hasNext();)
					((WeakEntry) it.next()).decHits();
			}
			
			if(TRACE_CLEANUP && scheduled)
			{
				List weightTraceSorted = new ArrayList(interningSet);
				Collections.sort(weightTraceSorted,savingsComp);
				System.out.println("Remaining elements after cleanup:");
				for(Iterator it = weightTraceSorted.iterator();it.hasNext();)
					System.out.println("\t"+it.next());
			}
			
			if(scheduled && interningSet.capacity() > interningSet.size * 4)
				interningSet.compactify(0f);
				
		}
	}

	/*
	 * private static void tidy( boolean clear ) { if ( !clear ){
	 * 
	 * Iterator it = map.values().iterator();
	 * 
	 * while( it.hasNext()){
	 * 
	 * entryDetails entry = (entryDetails)it.next();
	 *  // random guess: size of an entry is // Object: 8 // Reference: 4*8 //
	 * entryDetails: 4 // Map.Entry: 2*8 + 4 // = say 90 bytes (testing shows 90
	 * :))
	 *  // a String is 24 bytes + chars
	 * 
	 * final int overhead = 90; final int str_size = 24 + entry.size;
	 * 
	 * if ( entry.hit_count * str_size < overhead ){
	 * 
	 * it.remove(); } }
	 * 
	 * if ( map.size() > MAX_MAP_SIZE / 2 ){
	 *  // didn't compact enough, dump the whole thing and start again!
	 * 
	 * clear = true; } }
	 * 
	 * if ( clear ){
	 * 
	 * map = new WeakHashMap( MAX_MAP_SIZE ); }
	 * 
	 * 
	 * System.out.println( "trimmed down to " + map.size());
	 * 
	 * List l = new ArrayList(map.values());
	 * 
	 * Collections.sort( l, new Comparator() { public int compare( Object o1,
	 * Object o2 ) { entryDetails e1 = (entryDetails)o1; entryDetails e2 =
	 * (entryDetails)o2;
	 * 
	 * return( e2.hit_count - e1.hit_count ); } });
	 * 
	 * String line = "";
	 * 
	 * for (int i=0;i<Math.min( 128, l.size());i++){
	 * 
	 * entryDetails e = (entryDetails)l.get(i);
	 * 
	 * line += "\"" + e.get() + "\",";
	 * 
	 * if ( (i+1) % 8 == 0 ){
	 * 
	 * System.out.println( line );
	 * 
	 * line = ""; } }
	 * 
	 * System.out.println( line );
	 *  }
	 */
	
	private static abstract class WeakEntry extends WeakReference
	{
		private final int hash;
		final short size;
		short hits;
		

		public WeakEntry(Object o, int hash, int size)
		{
			super(o,refQueue);
			this.hash = hash;
			this.size = (short)(size & 0x7FFF);
		}
		
		public int hashCode() {
			return hash;		
		}
		
		public void incHits()
		{
			if(hits < Short.MAX_VALUE)
				hits++;
		}
		
		public void decHits()
		{
			if(hits > 0)
				hits--;
		}
		
		public String toString() {
			return this.getClass().getName().replaceAll("^.*\\..\\w+$", "")+" h="+(int)hits+";s="+(int)size;
		}
		
		public void destroy()
		{
			hits = -1;
		}
		
		public boolean isDestroyed()
		{
			return hits == -1;
		}
	}
	
	private static class WeakByteArrayEntry extends WeakEntry
	{
		
		public WeakByteArrayEntry(byte[] array)
		{
			// byte-array object
			super(array,HashCodeUtils.hashCode(array),array.length+8);
		}
		
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj instanceof WeakByteArrayEntry)
			{
				byte[] myArray = getArray();
				byte[] otherArray = ((WeakByteArrayEntry)obj).getArray();
				return myArray == null ? false : Arrays.equals(myArray,otherArray);
			}
				
			return false;
		}

		public byte[] getArray() {
			return (byte[])get();
		}
		
		public String toString() {
			return super.toString()+" "+(getArray() == null?"null":new String(getArray()));
		}
	}
	
	private static class WeakStringEntry extends WeakEntry
	{
		public WeakStringEntry(String entry)
		{
			// string object with 2 fields, char-array object
			super(entry,entry.hashCode(),16+8+entry.length()*2);
		}
		
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj instanceof WeakStringEntry)
			{
				String myString = getString();
				String otherString = ((WeakStringEntry)obj).getString();
				return myString == null ? false : myString.equals(otherString);
			}
			return false;
		}
		
		public String getString()
		{
			return (String)get();
		}
		
		public String toString() {
			return super.toString()+" "+getString();
		}
	}
	
	private static class WeakFileEntry extends WeakEntry
	{
		public WeakFileEntry(File entry)
		{
			// file object with 2 fields, string object with 2 fields, char-array object
			super(entry,entry.hashCode(),16+16+8+entry.getPath().length()*2);
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj instanceof WeakFileEntry)
			{
				File myFile = getFile();
				File otherFile = ((WeakFileEntry)obj).getFile();
				return myFile == null ? otherFile == null : myFile.equals(otherFile);
			}
			return false;
		}
		
		public File getFile()
		{
			return (File)get();
		}
		
		public String toString() {
			return super.toString()+" "+getFile();
		}
	}

	
	/*
	public static void
	main(
		String[]	args )
	{
		WeakHashMap map = new WeakHashMap();
		
		String[] strings = new String[1000];
		
		System.gc();
		
		long used1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		
		int	entries;
		
		for (int i=0;i<4000;i++){
			
			Object	key 	= new Integer(i);
			//Object	value 	= new Integer(i);
			
			map.put( key, new entryDetails( key ));
		}
		
		entries = map.size();
		
		for (int i=0;i<strings.length;i++){
			
			strings[i] = new String("");
		}
		entries = strings.length;
		
		long used2 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		
		long diff = used2 - used1;
		
		System.out.println( "entries=" + entries + ", diff=" + diff + " -> " + (diff/entries));
	}
	*/
}
