/*
 * File    : CategoryImpl.java
 * Created : 09 feb. 2004
 * By      : TuxPaper
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

package org.gudy.azureus2.core3.category.impl;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.category.CategoryListener;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.util.ListenerManager;
import org.gudy.azureus2.core3.util.ListenerManagerDispatcher;

import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;

public class CategoryImpl implements Category, Comparable {
  private String sName;
  private int type;
  private List<DownloadManager> managers = new ArrayList<DownloadManager>();

  private int upload_speed;
  private int download_speed;

  private final Map<String,String>	attributes;
  
  private LimitedRateGroup upload_limiter = 
	  new LimitedRateGroup()
	  {
		  public String 
		  getName() 
		  {
			  return( "cat_up: " + sName);
		  }
		  public int 
		  getRateLimitBytesPerSecond()
		  {
			  return( upload_speed );
		  }
	  };
   
  private LimitedRateGroup download_limiter = 
	  new LimitedRateGroup()
  {
	  public String 
	  getName() 
	  {
		  return( "cat_down: " + sName);
	  }
	  public int 
	  getRateLimitBytesPerSecond()
	  {
		  return( download_speed );
	  }
  };  
  
  private static final int LDT_CATEGORY_DMADDED     = 1;
  private static final int LDT_CATEGORY_DMREMOVED   = 2;
	private ListenerManager	category_listeners = ListenerManager.createManager(
		"CatListenDispatcher",
		new ListenerManagerDispatcher()
		{
			public void
			dispatch(Object		_listener,
               int			type,
               Object		value )
			{
				CategoryListener target = (CategoryListener)_listener;

				if ( type == LDT_CATEGORY_DMADDED )
					target.downloadManagerAdded((Category) CategoryImpl.this, (DownloadManager)value);
				else if ( type == LDT_CATEGORY_DMREMOVED )
					target.downloadManagerRemoved(CategoryImpl.this, (DownloadManager)value);
			}
		});

  public CategoryImpl(String sName, int maxup, int maxdown, Map<String,String> _attributes ) {
    this.sName = sName;
    this.type = Category.TYPE_USER;
    upload_speed	= maxup;
    download_speed	= maxdown;
    attributes = _attributes;
  }

  public CategoryImpl(String sName, int type, Map<String,String> _attributes) {
    this.sName = sName;
    this.type = type;
    attributes = _attributes;
  }

	public void addCategoryListener(CategoryListener l) {
		category_listeners.addListener( l );
	}
	
	public void removeCategoryListener(CategoryListener l) {
		category_listeners.removeListener( l );
	}

  public String getName() {
    return sName;
  }
  
  public int getType() {
    return type;
  }
  
  public List<DownloadManager> getDownloadManagers(List<DownloadManager> all_dms) {
	  if ( type == Category.TYPE_USER ){
		  return managers;
	  }else if ( type == Category.TYPE_ALL ){
		  return all_dms;
	  }else{
		  List<DownloadManager> result = new ArrayList<DownloadManager>();
		  for (int i=0;i<all_dms.size();i++){
			  DownloadManager dm = all_dms.get(i);
			  Category cat = dm.getDownloadState().getCategory();
			  if ( cat == null || cat.getType() == Category.TYPE_UNCATEGORIZED){
				  result.add( dm );
			  }
		  }
		  
		  return( result );
	  }
  }
  
  public void addManager(DownloadManagerState manager_state) {
    if (manager_state.getCategory() != this) {
    	manager_state.setCategory(this);
      // we will be called again by CategoryManager.categoryChange
      return;
    }
    
    DownloadManager	manager = manager_state.getDownloadManager();
    
    	// can be null if called during downloadmanagerstate construction
    if ( manager == null ){
    	return;
    }
    
    if (!managers.contains(manager)) {
      managers.add(manager);
      
      manager.addRateLimiter( upload_limiter, true );
      manager.addRateLimiter( download_limiter, false );
      
      category_listeners.dispatch(LDT_CATEGORY_DMADDED, manager);
    }
  }

  public void removeManager(DownloadManagerState manager_state) {
    if (manager_state.getCategory() == this) {
    	manager_state.setCategory(null);
      // we will be called again by CategoryManager.categoryChange
      return;
    }
    DownloadManager	manager = manager_state.getDownloadManager();

   	// can be null if called during downloadmanagerstate construction
    if ( manager == null ){
    	return;
    }
    
    if (managers.contains(manager) || type != Category.TYPE_USER) {
      managers.remove(manager);
      
      manager.removeRateLimiter( upload_limiter, true );
      manager.removeRateLimiter( download_limiter, false );
 
      category_listeners.dispatch( LDT_CATEGORY_DMREMOVED, manager );
    }
  }

  public void
  setDownloadSpeed(
	int		speed )
  {
	  if ( download_speed != speed ){
		  
		  download_speed = speed;
		  
		  CategoryManagerImpl.getInstance().saveCategories(this);
	  }
  }
  
  public int
  getDownloadSpeed()
  {
	  return( download_speed );
  }
  
  public void
  setUploadSpeed(
	int		speed )
  {
	  if ( upload_speed != speed ){
		  
		  upload_speed	= speed;
	  
		  CategoryManagerImpl.getInstance().saveCategories(this);
	  }
  }
  
  public int
  getUploadSpeed()
  {
	  return( upload_speed );
  }
  
  protected void
  setAttributes(
	Map<String,String> a )
  {
	  attributes.clear();
	  attributes.putAll( a );
  }
  
  protected Map<String,String>
  getAttributes()
  {
	  return( attributes );
  }
  
  public String
  getStringAttribute(
	String		name )
  {
	  return( attributes.get(name));
  }
  
  public void
  setStringAttribute(
	String		name,
	String		value )
  {
	  String old = attributes.put( name, value );
	  
	  if ( old == null || !old.equals( value )){
	  
		  CategoryManagerImpl.getInstance().saveCategories(this);
	  }

  }
  
  public boolean
  getBooleanAttribute(
	String		name )
  {
	 String str = getStringAttribute( name );
	 
	 return( str != null && str.equals( "true" ));
  }
  
  public void
  setBooleanAttribute(
	String		name,
	boolean		value )
  {
	  String old = attributes.put( name, value?"true":"false" );
	  
	  if ( old == null || !old.equals( value )){
	  
		  CategoryManagerImpl.getInstance().saveCategories(this);
	  }

  }
  
  public int compareTo(Object b)
  {
    boolean aTypeIsUser = type == Category.TYPE_USER;
    boolean bTypeIsUser = ((Category)b).getType() == Category.TYPE_USER;
    if (aTypeIsUser == bTypeIsUser)
      return sName.compareToIgnoreCase(((Category)b).getName());
    if (aTypeIsUser)
      return 1;
    return -1;
  }
}
