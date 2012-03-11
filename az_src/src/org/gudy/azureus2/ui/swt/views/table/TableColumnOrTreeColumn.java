/**
 * Created on May 3, 2010
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

package org.gudy.azureus2.ui.swt.views.table;

import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;

/**
 * @author TuxPaper
 * @created May 3, 2010
 *
 */
public interface TableColumnOrTreeColumn
{

	public Image getImage();

	public String getText();

	public void addControlListener(ControlListener listener);

	public void addListener(int eventType, Listener listener);

	public void addSelectionListener(SelectionListener listener);

	public void addDisposeListener(DisposeListener listener);

	public boolean getMoveable();

	public TableOrTreeSWT getParent();

	public boolean getResizable();

	public String getToolTipText();

	public int getWidth();

	public void dispose();

	public boolean equals(Object obj);

	public int getAlignment();

	public Object getData();

	public Object getData(String key);

	public Display getDisplay();

	public Listener[] getListeners(int eventType);

	public int getStyle();

	public int hashCode();

	public void pack();

	public void removeControlListener(ControlListener listener);

	public void removeSelectionListener(SelectionListener listener);

	public void setAlignment(int alignment);

	public void setImage(Image image);

	public void setMoveable(boolean moveable);

	public void setResizable(boolean resizable);

	public void setText(String string);

	public void setToolTipText(String string);

	public boolean isDisposed();

	public boolean isListening(int eventType);

	public void setWidth(int width);

	public void notifyListeners(int eventType, Event event);

	public void removeListener(int eventType, Listener listener);

	public void removeDisposeListener(DisposeListener listener);

	public void setData(Object data);

	public void setData(String key, Object value);

	public String toString();

	public Item getColumn();

}