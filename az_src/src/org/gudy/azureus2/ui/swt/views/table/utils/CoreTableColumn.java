/*
 * File    : CoreTableColumn.java
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
 
package org.gudy.azureus2.ui.swt.views.table.utils;

import org.eclipse.swt.SWT;

import org.gudy.azureus2.plugins.ui.tables.*;

import com.aelitis.azureus.ui.common.table.impl.TableColumnImpl;

/** This class  provides constructors for setting most of
 * the common column attributes and sets the column as a 'core' column.<p>
 *
 * @author TuxPaper
 */
public class CoreTableColumn 
       extends TableColumnImpl 
{
  /** Construct a new CoreTableColumn
   * Type will be TYPE_TEXT, Update Interval will be INTERVAL_INVALID_ONLY
   * <p>
   * TableCell listeners (Added, Refresh, Dispose, ToolTip) are added based on
   * whether the class is an instance of them.
   *
   * @param sName Unique ID for column 
   * @param iAlignment See {@link #getAlignment()}
   * @param iPosition See {@link TableColumn#setPosition(int)}
   * @param iWidth See {@link TableColumn#setWidth(int)}
   * @param sTableID See {@link TableManager}_TABLE*
   */
  public CoreTableColumn(String sName, int iAlignment,
                         int iPosition, int iWidth,
                         String sTableID) {
    super(sTableID, sName);
    super.initialize(iAlignment, iPosition, iWidth);
    setUseCoreDataSource(true);
    addListeners(this);
  }

  /** Construct a new CoreTableColumn.<p>
   * Alignment will be ALIGN_LEAD, Type will be TYPE_TEXT, 
   * Update Interval will be INTERVAL_INVALID_ONLY
   * <p>
   * TableCell listeners (Added, Refresh, Dispose, ToolTip) are added based on
   * whether the class is an instance of them.
   *
   * @param sName Unique ID for column 
   * @param iPosition See {@link TableColumn#setPosition(int)}
   * @param iWidth See {@link TableColumn#setWidth(int)}
   * @param sTableID See {@link TableManager}_TABLE*
   */
  public CoreTableColumn(String sName, int iPosition, int iWidth, 
                         String sTableID) {
    super(sTableID, sName);
    setPosition(iPosition);
    setWidth(iWidth);
    setUseCoreDataSource(true);
    addListeners(this);
  }

  /** Construct a new CoreTableColumn.<p>
   * Alignment will be ALIGN_LEAD, Type will be TYPE_TEXT, Position will be
   * POSITION_INVISIBLE, Update Interval will be INTERVAL_INVALID_ONLY
   * <p>
   * TableCell listeners (Added, Refresh, Dispose, ToolTip) are added based on
   * whether the class is an instance of them.
   *
   * @param sName Unique ID for column 
   * @param iWidth See {@link TableColumn#setWidth(int)}
   * @param sTableID See {@link TableManager}_TABLE*
   */
  public CoreTableColumn(String sName, int iWidth, String sTableID) {
    super(sTableID, sName);
    setWidth(iWidth);
    setUseCoreDataSource(true);
    addListeners(this);
  }

  /** Construct a new CoreTableColumn.<p>
   * Alignment will be ALIGN_LEAD, Type will be TYPE_TEXT, Position will be
   * POSITION_INVISIBLE, Width will be 50, Update Interval will be 
   * INTERVAL_INVALID_ONLY
   * <p>
   * TableCell listeners (Added, Refresh, Dispose, ToolTip) are added based on
   * whether the class is an instance of them.
   *
   * @param sName Unique ID for column 
   * @param sTableID See {@link TableManager}_TABLE*
   */
  public CoreTableColumn(String sName, String sTableID) {
    super(sTableID, sName);
    setUseCoreDataSource(true);
    addListeners(this);
  }
  
  public void initializeAsGraphic(int iPosition, int iWidth) {
    setPosition(iPosition);
    setWidth(iWidth);
    setType(TYPE_GRAPHIC);
    setRefreshInterval(INTERVAL_GRAPHIC);
    setAlignment(TableColumn.ALIGN_CENTER);
  }

  /** 
   * Convert the getAlignment() constant to a SWT constant
	 *
	 * @return SWT alignment constant
	 */
	public static int getSWTAlign(int alignment) {
    return alignment == ALIGN_LEAD ? SWT.LEAD
                                   : (alignment == ALIGN_CENTER) ? SWT.CENTER
                                                                 : SWT.TRAIL;
  }
}
