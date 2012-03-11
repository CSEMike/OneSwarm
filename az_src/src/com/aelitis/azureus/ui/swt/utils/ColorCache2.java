/*
 * Created on Nov 10, 2011
 * Created by Paul Gardner
 * 
 * Copyright 2011 Vuze, Inc.  All rights reserved.
 * 
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.ui.swt.utils;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.RGB;
import org.gudy.azureus2.core3.util.Debug;

public class 
ColorCache2 
{
	private static Map<RGB,CachedColorManaged>	color_map = new HashMap<RGB, CachedColorManaged>();
	
	public static CachedColor
	getColor(
		Color		c )
	{
		return( new CachedColorUnmanaged( c ));
	}

	public static CachedColor
	getColor(
		Device		device,
		RGB			rgb )
	{
		synchronized( color_map ){
			
			CachedColorManaged entry = color_map.get( rgb );
			
			if ( entry == null ){
		
				entry = new CachedColorManaged( new Color( device, rgb ));
				
				color_map.put( rgb, entry );
				
			}else{
				
				entry.addRef();
			}
			
			return( new CachedColorManagedFacade( entry ));
		}
	}
	
	private static class
	CachedColorManaged
	{
		private Color	color;
		private int		ref_count;
		
		private
		CachedColorManaged(
			Color	_color )
		{
			color		= _color;
			ref_count	= 1;
		}
		
		public Color
		getColor()
		{
			return( color );
		}
			
		private void
		addRef()
		{
			ref_count++;
			
			//System.out.println( "cc ++: color=" + color + ", refs=" + ref_count );
		}
		
		private void
		dispose()
		{
			ref_count--;
			
			//System.out.println( "cc --: color=" + color + ", refs=" + ref_count );
			
			if ( ref_count == 0 ){
												
				color_map.remove( color.getRGB());
				
				color.dispose();

			}else if ( ref_count < 0 ){
				
				Debug.out( "already disposed" );
			}
		}
	}
	
	private static class
	CachedColorManagedFacade
		implements CachedColor
	{
		private	CachedColorManaged	delegate;
		private boolean				disposed;
		
		private 
		CachedColorManagedFacade(
			CachedColorManaged	_delegate )
		{
			delegate = _delegate;
		}
		
		public Color
		getColor()
		{
			return( delegate.getColor());
		}
		
		public boolean
		isDisposed()
		{
			synchronized( color_map ){
				
				return( disposed );
			}
		}
		
		public void
		dispose()
		{
			synchronized( color_map ){
				
				if ( !disposed ){
				
					disposed = true;
			
					delegate.dispose();
				}
			}
		}
	}
	
	private static class
	CachedColorUnmanaged
		implements CachedColor
	{
		private Color	color;
		
		private
		CachedColorUnmanaged(
			Color	_color )
		{
			color	= _color;
		}
		
		public Color
		getColor()
		{
			return( color );
		}
		
		public boolean
		isDisposed()
		{
			return( color.isDisposed());
		}
		
		public void
		dispose()
		{
			color.dispose();
		}
	}
	
	public interface
	CachedColor
	{
		public Color
		getColor();

		public boolean
		isDisposed();

		public void
		dispose();
	}
}
