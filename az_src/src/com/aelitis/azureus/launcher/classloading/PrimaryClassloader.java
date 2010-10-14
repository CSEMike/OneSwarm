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
package com.aelitis.azureus.launcher.classloading;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * @author Aaron Grunthal
 * @create 28.12.2007
 */
public class PrimaryClassloader extends URLClassLoader implements PeeringClassloader {
	
	private ArrayList peersLoaders = new ArrayList();
	
	private static final String packageName = PrimaryClassloader.class.getPackage().getName(); 
	
	
	public PrimaryClassloader(ClassLoader parent)
	{
		super(generateURLs(),parent);
	}
	
	private static URL[] generateURLs()
	{
		String classpath = System.getProperty("java.class.path");
		
		String[] paths = classpath.split(File.pathSeparator);
		URL[] urls = new URL[paths.length+1];
		try
		{
			for(int i=0;i<paths.length;i++)
			{
				urls[i] = new File(paths[i]).getCanonicalFile().toURI().toURL();
				System.out.print(urls[i]+" ; ");
			}
				
			urls[urls.length-1] = new File(".").getCanonicalFile().toURI().toURL();
			System.out.println(urls[urls.length-1]);
		} catch (Exception e)
		{
			System.err.println("Invalid classpath detected\n");
			e.printStackTrace();
			System.exit(1);
		}

		return urls;
	}
	
	private PrimaryClassloader(URL[] urls,ClassLoader parent)
	{
		super(urls,parent);
	}
	
	/**
	 * altered class lookup order
	 * <ol>
	 * <li>check for loaded</li>
	 * <li>check for loaded by peers</li>
	 * <li>check/load classes belonging to the classloading package with the system class loader
	 * <li>try to load locally</li>
	 * <li>try to load from peers</li>
	 * <li>query parent, skip system class loader as we do not want to pollute it</li>
	 * </ol>
	 */
	protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
		//System.out.println("loading "+name);
		Class c = findLoadedClass(name);
		if(c == null)
			c = peerFindLoadedClass(name);
		if(name.startsWith("java.") || name.startsWith(packageName))
			try
			{
				c = getParent().loadClass(name);
			} catch (Exception e)
			{
				// continue
			}
		if(c == null)
			try
			{
				c = findClass(name);
			} catch (ClassNotFoundException e)
			{
				// continue with alternatives
			}
		if(c == null)
			c = peerLoadClass(name);
		if(c == null)
		{
			ClassLoader parentLoader = getParent();
			//while(parentLoader == getSystemClassLoader())
				//parentLoader = parentLoader.getParent();
			c = parentLoader.loadClass(name);
		}
		if(resolve)
			resolveClass(c);
		return c;
	}
	
	private Class peerFindLoadedClass(String className)
	{
		Class c = null;
		for(int i=0;i<peersLoaders.size()&&c==null;i++)
		{
			WeakReference ref = (WeakReference)peersLoaders.get(i);
			SecondaryClassLoader loader = (SecondaryClassLoader)ref.get();
			if(loader != null)
				c = loader.findLoadedClassHelper(className);
			else
				peersLoaders.remove(i--);
		}
		return c;
	}
	
	private Class peerLoadClass(String className)
	{
		Class c = null;
		for(int i=0;i<peersLoaders.size()&&c==null;i++)
		{
			WeakReference ref = (WeakReference)peersLoaders.get(i);
			SecondaryClassLoader loader = (SecondaryClassLoader)ref.get();
			if(loader != null) // no removal here, peerFindLoadedClass should take care of that anyway
				c = loader.findClassHelper(className);
		}
		return c;
	}
	
	synchronized void registerSecondaryClassloader(SecondaryClassLoader loader)
	{
		peersLoaders.add(new WeakReference(loader));
	}
	
	
	
	/**
	 * 
	 * @param toRun
	 */
	public static ClassLoader getBootstrappedLoader()
	{
		ClassLoader loader = ClassLoader.getSystemClassLoader();
		if(loader instanceof PrimaryClassloader)
			return loader;
		
		try
		{
			Constructor c = loader.loadClass(PrimaryClassloader.class.getName()).getDeclaredConstructor(new Class[] {ClassLoader.class});
			return (ClassLoader)c.newInstance(new Object[] {loader});
		} catch (Exception e)
		{
			System.err.println("Could not instantiate Classloader\n");
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}
}
