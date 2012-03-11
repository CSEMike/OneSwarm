/*
 * Created on 23-Jun-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.local.torrent;

/**
 * @author parg
 *
 */

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.utils.StaticUtilities;

import org.gudy.azureus2.core3.category.*;


public class TorrentAttributeCategoryImpl extends BaseTorrentAttributeImpl {

	protected
	TorrentAttributeCategoryImpl()
	{
		CategoryManager.addCategoryManagerListener(
				new CategoryManagerListener()
				{
					public void
					categoryAdded(
						final Category category )
					{
						TorrentAttributeEvent	ev = 
							new TorrentAttributeEvent()
							{
								public int
								getType()
								{
									return( TorrentAttributeEvent.ET_ATTRIBUTE_VALUE_ADDED );
								}
							
								public TorrentAttribute
								getAttribute()
								{
									return( TorrentAttributeCategoryImpl.this );
								}
								
								public Object
								getData()
								{
									return( category.getName());
								}
							};
							
							TorrentAttributeCategoryImpl.this.notifyListeners(ev);
					}
						
					public void categoryChanged(Category category) {	
					}
					
					public void
					categoryRemoved(
						final Category category )
					{
						TorrentAttributeEvent	ev = 
							new TorrentAttributeEvent()
							{
								public int
								getType()
								{
									return( TorrentAttributeEvent.ET_ATTRIBUTE_VALUE_REMOVED );
								}
							
								public TorrentAttribute
								getAttribute()
								{
									return( TorrentAttributeCategoryImpl.this );
								}
								
								public Object
								getData()
								{
									return( category.getName());
								}
							};
							
							TorrentAttributeCategoryImpl.this.notifyListeners(ev);
					}
				});
	}
	
	public String
	getName()
	{
		return( TA_CATEGORY );
	}
	
	public String[]
	getDefinedValues()
	{
		Category[] categories = CategoryManager.getCategories();
		
		List	v = new ArrayList();
		
		for (int i=0;i<categories.length;i++){
			
			Category cat = categories[i];
			
			if ( cat.getType() == Category.TYPE_USER ){
			
				v.add( cat.getName());
			}
		}
		
		String[]	res = new String[v.size()];
		
		v.toArray( res );
		
			// make it nice for clients
		
		Arrays.sort( res, StaticUtilities.getFormatters().getAlphanumericComparator( true ));
		
		return( res );
	}
	
	public void
	addDefinedValue(
		String		name )
	{
		CategoryManager.createCategory( name );
	}
	
	
	public void
	removeDefinedValue(
		String		name )
	{
		Category cat = CategoryManager.getCategory( name );
		
		if ( cat != null ){
			
			CategoryManager.removeCategory( cat );
		}
	}

}
