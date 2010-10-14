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
package org.gudy.azureus2.core3.util;

import java.util.*;


/**
 * A lighter (on memory) hash map<br>
 * 
 * Please note the following performance drawbacks:
 * <ul>
 * <li>removal is implemented with thombstone-keys, this can significantly increase the lookup time if many values are removed. Use compactify() for scrubbing
 * <li>entry set iterators and thus transfers to other maps are slower than compareable implementations
 * <li>the map does not store hashcodes and relies on either the key-objects themselves caching them (such as strings) or a fast computation of hashcodes
 * </ul>
 * 
 * @author Aaron Grunthal
 * @create 28.11.2007
 */
public class LightHashMap extends AbstractMap implements Cloneable {
	private static final Object	THOMBSTONE			= new Object();
	private static final Object NULLKEY				= new Object();
	private static final float	DEFAULT_LOAD_FACTOR	= 0.75f;
	private static final int	DEFAULT_CAPACITY	= 8;

	public LightHashMap()
	{
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public LightHashMap(final int initialCapacity)
	{
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}

	public LightHashMap(final Map m)
	{
		this(0);
		if(m instanceof LightHashMap)
		{
			final LightHashMap lightMap = (LightHashMap)m;
			this.size = lightMap.size;
			this.data = (Object[])lightMap.data.clone();
		} else
			putAll(m);
	}
	
	public Object clone() {
		try
		{
			final LightHashMap newMap = (LightHashMap) super.clone();
			newMap.data = (Object[])data.clone();
			return newMap;
		} catch (CloneNotSupportedException e)
		{
			// should not ever happen
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public LightHashMap(int initialCapacity, final float loadFactor)
	{
		if (loadFactor > 1)
			throw new IllegalArgumentException("Load factor must not be > 1");
		this.loadFactor = loadFactor;
		int capacity = 1;
		while (capacity < initialCapacity)
			capacity <<= 1;
		data = new Object[capacity*2];
	}

	final float	loadFactor;
	int			size;
	Object[]	data;

	public Set entrySet() {
		return new EntrySet();
	}

	private abstract class HashIterator implements Iterator {
		protected int	nextIdx		= -2;
		protected int	currentIdx	= -2;

		public HashIterator()
		{
			findNext();
		}

		private void findNext() {
			do
				nextIdx+=2;
			while (nextIdx < data.length && (data[nextIdx] == null || data[nextIdx] == THOMBSTONE));
		}

		public void remove() {
			if (currentIdx == -2)
				new IllegalStateException("No entry to delete, use next() first");
			LightHashMap.this.removeForIndex(currentIdx);
			currentIdx = -2;
		}

		public boolean hasNext() {
			return nextIdx < data.length;
		}

		public Object next() {
			if (!hasNext())
				throw new IllegalStateException("No more entries");
			currentIdx = nextIdx;
			findNext();
			return nextIntern();
		}

		abstract Object nextIntern();
	}

	private class EntrySet extends AbstractSet {
		public Iterator iterator() {
			return new EntrySetIterator();
		}

		public int size() {
			return size;
		}

		private final class Entry implements Map.Entry {
			final int	entryIndex;

			public Entry(final int idx)
			{
				entryIndex = idx;
			}

			public Object getKey() {
				final Object key = data[entryIndex];
				return key != NULLKEY ? key : null;
			}

			public Object getValue() {
				return data[entryIndex+1];
			}

			public Object setValue(final Object value) {
				final Object oldValue = data[entryIndex+1];
				data[entryIndex+1] = value;
				return oldValue;
			}
		}

		private class EntrySetIterator extends HashIterator {
			public Object nextIntern() {
				return new Entry(currentIdx);
			}
		}
	}

	private class KeySet extends AbstractSet {
		public Iterator iterator() {
			return new KeySetIterator();
		}

		private class KeySetIterator extends HashIterator {
			Object nextIntern() {
				final Object key = data[currentIdx];
				return key != NULLKEY ? key : null;
			}
		}

		public int size() {
			return size;
		}
	}

	private class Values extends AbstractCollection {
		public Iterator iterator() {
			return new ValueIterator();
		}

		private class ValueIterator extends HashIterator {
			Object nextIntern() {
				return data[currentIdx+1];
			}
		}

		public int size() {
			return size;
		}
	}

	public Object put(final Object key, final Object value) {
		checkCapacity(1);
		return add(key, value, false);
	}

	public void putAll(final Map m) {
		checkCapacity(m.size());
		for (final Iterator it = m.entrySet().iterator(); it.hasNext();)
		{
			final Map.Entry entry = (Map.Entry) it.next();
			add(entry.getKey(), entry.getValue(),true);
		}
		// compactify in case we overestimated the new size due to redundant entries
		//compactify(0.f);
	}

	public Set keySet() {
		return new KeySet();
	}

	public Collection values() {
		return new Values();
	}
	
	public int capacity()
	{
		return data.length>>1;
	}

	public Object get(Object key) {
		if(key == null)
			key = NULLKEY; 
		return data[nonModifyingFindIndex(key)+1];
	}
	
	private Object add(Object key, final Object value, final boolean bulkAdd) {
		if(key == null)
			key = NULLKEY;
		final int idx = bulkAdd ? nonModifyingFindIndex(key) : findIndex(key);
		final Object oldValue = data[idx+1];
		if (data[idx] == null || data[idx] == THOMBSTONE)
		{
			data[idx] = key;
			size++;
		}
		data[idx+1] = value;
		return oldValue;
	}

	public Object remove(Object key) {
		if(size == 0)
			return null;
		if(key == null)
			key = NULLKEY;
		final int idx = findIndex(key);
		if (keysEqual(data[idx], key))
			return removeForIndex(idx);
		return null;
	}
	
	private Object removeForIndex(final int idx)
	{
		final Object oldValue = data[idx+1];
		data[idx] = THOMBSTONE;
		data[idx+1] = null;
		size--;
		return oldValue;
	}

	public void clear() {
		size = 0;
		int capacity = 1;
		while (capacity < DEFAULT_CAPACITY)
			capacity <<= 1;
		data = new Object[capacity*2];
	}

	public boolean containsKey(Object key) {
		if(size == 0)
			return false;
		if(key == null)
			key = NULLKEY;
		return keysEqual(key, data[nonModifyingFindIndex(key)]);
	}

	public boolean containsValue(final Object value) {
		if (value != null)
		{
			for (int i = 0; i < data.length; i+=2)
				if (value.equals(data[i+1]))
					return true;
		} else
			for (int i = 0; i < data.length; i+=2)
				if (data[i+1] == null && data[i] != null && data[i] != THOMBSTONE)
					return true;
		return false;
	}
	
	private boolean keysEqual(final Object o1, final Object o2) {
		if (o1 != null)
			if (o2 != null && o1.hashCode() != o2.hashCode())
				return false;
			else
				return o1.equals(o2);
		return o2 == null;
	}

	private int findIndex(final Object keyToFind) {
		final int hash = keyToFind.hashCode() << 1;
		/* hash ^= (hash >>> 20) ^ (hash >>> 12);
		 * hash ^= (hash >>> 7) ^ (hash >>> 4);
		 */
		int probe = 1;
		int newIndex = hash & (data.length - 1);
		int thombStoneIndex = -1;
		int thombStoneCount = 0;
		final int thombStoneThreshold = Math.min((data.length>>1)-size, 100);
		// search until we find a free entry or an entry matching the key to insert
		while (data[newIndex] != null && !keysEqual(data[newIndex], keyToFind))
		{
			if (data[newIndex] == THOMBSTONE)
			{
				if(thombStoneIndex == -1)
					thombStoneIndex = newIndex;
				thombStoneCount++;
				if(thombStoneCount * 2 > thombStoneThreshold)
				{
					compactify(0.f);
					thombStoneIndex = -1;
					probe = 0;
					thombStoneCount = 0; // not really necessary
				}
			}
				
			newIndex = (hash + probe + probe * probe) & (data.length - 1);
			probe++;
		}
		// if we didn't find an exact match then the first thombstone will do too for insert
		if (thombStoneIndex != -1 && !keysEqual(data[newIndex], keyToFind))
			return thombStoneIndex;
		return newIndex;
	}
	
	private int nonModifyingFindIndex(final Object keyToFind) {
		final int hash = keyToFind.hashCode() << 1;
		/* hash ^= (hash >>> 20) ^ (hash >>> 12);
		 * hash ^= (hash >>> 7) ^ (hash >>> 4);
		 */
		int probe = 1;
		int newIndex = hash & (data.length - 1);
		int thombStoneIndex = -1;
		// search until we find a free entry or an entry matching the key to insert
		while (data[newIndex] != null && !keysEqual(data[newIndex], keyToFind) && probe < (data.length>>1))
		{
			if(data[newIndex] == THOMBSTONE && thombStoneIndex == -1)
				thombStoneIndex = newIndex;
			newIndex = (hash + probe + probe * probe) & (data.length - 1);
			probe++;
		}
		if (thombStoneIndex != -1 && !keysEqual(data[newIndex], keyToFind))
			return thombStoneIndex;
		return newIndex;
	}
	

	private void checkCapacity(final int n) {
		final int currentCapacity = data.length>>1;
		if ((size + n) < currentCapacity * loadFactor)
			return;
		int newCapacity = currentCapacity;
		do
			newCapacity <<= 1;
		while (newCapacity * loadFactor < (size + n));
		adjustCapacity(newCapacity);
	}

	/**
	 * will shrink the internal storage size to the least possible amount,
	 * should be used after removing many entries for example
	 * 
	 * @param compactingLoadFactor
	 *            load factor for the compacting operation, use 0 to compact
	 *            with the load factor specified during instantiation
	 */
	public void compactify(float compactingLoadFactor) {
		int newCapacity = 1;
		if (compactingLoadFactor == 0.f)
			compactingLoadFactor = loadFactor;
		while (newCapacity * compactingLoadFactor < (size+1))
			newCapacity <<= 1;
		adjustCapacity(newCapacity);
	}

	private void adjustCapacity(final int newSize) {
		final Object[] oldData = data;
		data = new Object[newSize*2];
		size = 0;
		for (int i = 0; i < oldData.length; i+=2)
		{
			if (oldData[i] == null || oldData[i] == THOMBSTONE)
				continue;
			add(oldData[i], oldData[i+1], true);
		}
	}

	static void test() {
		final Random rnd = new Random();
		final byte[] buffer = new byte[5];
		final String[] fillData = new String[(int)((1<<21) * 0.93f)];
		for (int i = 0; i < fillData.length; i++)
		{
			rnd.nextBytes(buffer);
			fillData[i] = new String(buffer);
			fillData[i].hashCode();
		}
		long time;
		final Map m1 = new HashMap();
		final Map m2 = new LightHashMap();
		System.out.println("fill:");
		time = System.currentTimeMillis();
		for (int i = 0; i < fillData.length; i++)
			m1.put(fillData[i], buffer);
		System.out.println(System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
		for (int i = 0; i < fillData.length; i++)
			m2.put(fillData[i], buffer);
		System.out.println(System.currentTimeMillis() - time);
		System.out.println("replace-fill:");
		time = System.currentTimeMillis();
		for (int i = 0; i < fillData.length; i++)
			m1.put(fillData[i], buffer);
		System.out.println(System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
		for (int i = 0; i < fillData.length; i++)
			m2.put(fillData[i], buffer);
		System.out.println(System.currentTimeMillis() - time);
		System.out.println("get:");
		time = System.currentTimeMillis();
		for (int i = 0; i < fillData.length; i++)
			m1.get(fillData[i]);
		System.out.println(System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
		for (int i = 0; i < fillData.length; i++)
			m2.get(fillData[i]);
		System.out.println(System.currentTimeMillis() - time);
		System.out.println("compactify light map");
		time = System.currentTimeMillis();
		((LightHashMap) m2).compactify(0.90f);
		System.out.println(System.currentTimeMillis() - time);
		System.out.println("transfer to hashmap");
		time = System.currentTimeMillis();
		new HashMap(m1);
		System.out.println(System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
		new HashMap(m2);
		System.out.println(System.currentTimeMillis() - time);
		System.out.println("transfer to lighthashmap");
		time = System.currentTimeMillis();
		new LightHashMap(m1);
		System.out.println(System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
		new LightHashMap(m2);
		System.out.println(System.currentTimeMillis() - time);
		System.out.println("remove entry by entry");
		time = System.currentTimeMillis();
		for (int i = 0; i < fillData.length; i++)
			m1.remove(fillData[i]);
		System.out.println(System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
		for (int i = 0; i < fillData.length; i++)
			m2.remove(fillData[i]);
		System.out.println(System.currentTimeMillis() - time);
	}

	public static void main(final String[] args) {
		System.out.println("Call with -Xmx300m -Xcomp -server");
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		
		// some quadratic probing math test:
		/*
		boolean[] testArr = new boolean[1<<13];
		int hash = 0xc8d3 << 1;
		int position = hash & (testArr.length -1);
		int probe = 0;
		do
		{
			position = (hash + probe + probe * probe) & (testArr.length - 1);
			probe++;
			testArr[position] = true;
		} while (probe < (testArr.length>>1));
		
		for(int i = 0;i<testArr.length;i+=2)
		{
			if(testArr[i] != true)
				System.out.println("even element failed"+i);
			if(testArr[i+1] != false)
				System.out.println("uneven element failed"+(i+1));
		}
		*/
		
		
		try
		{
			Thread.sleep(300);
		} catch (final InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		test();
		System.out.println("-------------------------------------");
		System.gc();
		try
		{
			Thread.sleep(300);
		} catch (final InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		test();
		
		System.out.println("\n\nPerforming sanity tests");
		final Random rnd = new Random();
		final byte[] buffer = new byte[25];
		final String[] fillData = new String[1048];
		for (int i = 0; i < fillData.length; i++)
		{
			rnd.nextBytes(buffer);
			fillData[i] = new String(buffer);
			fillData[i].hashCode();
		}

		final Map m1 = new HashMap();
		final Map m2 = new LightHashMap();
		
		for(int i=0;i<fillData.length*10;i++)
		{
			int random = rnd.nextInt(fillData.length);			
			
			m1.put(null, fillData[i%fillData.length]);
			m2.put(null, fillData[i%fillData.length]);
			if(!m1.equals(m2))
				System.out.println("Error 0");
			m1.put(fillData[random], fillData[i%fillData.length]);
			m2.put(fillData[random], fillData[i%fillData.length]);
			if(!m1.equals(m2))
				System.out.println("Error 1");
		}
		
		// create thombstones, test removal
		for(int i=0;i<fillData.length/2;i++)
		{
			int random = rnd.nextInt(fillData.length);			
			m1.remove(fillData[random]);
			m2.remove(fillData[random]);
			if(!m1.equals(m2))
				System.out.println("Error 2");
		}
		
		// do some more inserting, this time with thombstones
		for(int i=0;i<fillData.length*10;i++)
		{
			int random = rnd.nextInt(fillData.length);			
			m1.put(fillData[random], fillData[i%fillData.length]);
			m1.put(null, fillData[i%fillData.length]);
			m2.put(fillData[random], fillData[i%fillData.length]);
			m2.put(null, fillData[i%fillData.length]);
			if(!m1.equals(m2))
				System.out.println("Error 3");
		}
		
		Iterator i1 = m1.entrySet().iterator();
		Iterator i2 = m2.entrySet().iterator();
		// now try removal with iterators
		while(i1.hasNext())
		{
			i1.next();
			i1.remove();
			i2.next();
			i2.remove();
		}
		
		if(!m1.equals(m2))
			System.out.println("Error 4");
		
		
		// test churn/thombstones
		m2.clear();
		/*
		for(int i=0;i<fillData.length*10;i++)
		{
			int random = rnd.nextInt(fillData.length);			

			m2.put(fillData[random], fillData[i%fillData.length]);
		}
		*/
		for(int i = 0;i<100000;i++)
		{
			rnd.nextBytes(buffer);
			String s = new String(buffer);
			m2.put(s, buffer);
			m2.containsKey(s);
			m2.remove(s);
		}
		
		System.out.println("checks done");
	}
}
