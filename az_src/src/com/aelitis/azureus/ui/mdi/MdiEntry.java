/**
 * Created on Aug 13, 2008
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */
 
package com.aelitis.azureus.ui.mdi;

import org.gudy.azureus2.plugins.ui.UIPluginView;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarEnablerBase;

import com.aelitis.azureus.ui.common.ToolBarEnabler;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;


/**
 * @author TuxPaper
 * @created Aug 13, 2008
 *
 */
public interface MdiEntry
{

	public String getParentID();

	public Object getDatasource();

	public boolean isCloseable();

	public Class<? extends UIPluginView> getViewClass();

	public UIPluginView getView();

	public String getId();

	public MdiEntryVitalityImage addVitalityImage(String imageID);

	/**
	 * @param l
	 *
	 * @since 4.1.0.3
	 */
	void addListener(MdiCloseListener l);

	void addListener(MdiChildCloseListener l);

	/**
	 * @param l
	 *
	 * @since 4.1.0.3
	 */
	void removeListener(MdiCloseListener l);

	void removeListener(MdiChildCloseListener l);

	/**
	 * @param l
	 *
	 * @since 4.1.0.3
	 */
	void addListener(MdiEntryOpenListener l);

	/**
	 * @param l
	 *
	 * @since 4.1.0.3
	 */
	void removeListener(MdiEntryOpenListener l);

	public void setImageLeftID(String string);

	public void setCollapseDisabled(boolean b);

	public void addListener(MdiEntryDropListener listener);

	public void setDatasource(Object ds);

	public void setLogID(String logID);

	public boolean isAdded();

	public boolean isDisposed();

	public ViewTitleInfo getViewTitleInfo();

	public void setViewTitleInfo(ViewTitleInfo viewTitleInfo);

	public String getLogID();

	public MultipleDocumentInterface getMDI();

	public MdiEntryVitalityImage[] getVitalityImages();

	public boolean close(boolean forceClose);

	public void updateUI();

	public void redraw();

	public void addListener(MdiEntryLogIdListener l);

	public void removeListener(MdiEntryLogIdListener l);

	public void hide();

	public String getTitle();
	
	public void setTitle(String title);

	public void setTitleID(String titleID);

	public String getImageLeftID();

	public boolean isExpanded();

	public void setExpanded(boolean expanded);
	
	public void setDefaultExpanded(boolean defaultExpanded);
	
	public void expandTo();

	public void setParentID(String id);

	public UIToolBarEnablerBase[] getToolbarEnablers();

	/**
	 * @deprecated For azburn
	 */
	public void addToolbarEnabler(ToolBarEnabler enabler);

	public void addToolbarEnabler(UIToolBarEnablerBase enabler);

	public void removeToolbarEnabler(UIToolBarEnablerBase enabler);

	public boolean isSelectable();

	public void setSelectable(boolean selectable);

	public void setPreferredAfterID(String preferredAfterID);

	public String getPreferredAfterID();
}
