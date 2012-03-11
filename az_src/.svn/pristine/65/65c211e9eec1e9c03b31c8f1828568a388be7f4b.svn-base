/**
 * File    : ITableStructureModificationListener.java
 * Created : 26 nov. 2003
 * By      : Olivier
 *
 * Copyright (C) 2004-2007 Aelitis SAS, All rights Reserved
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

package com.aelitis.azureus.ui.common.table;

/**
 * @author Olivier
 *
 */
public interface TableStructureModificationListener<T>
{
	void tableStructureChanged(boolean columnAddedOrRemoved, Class forPluginDataSourceType );

	void columnOrderChanged(int[] iPositions);

	void columnSizeChanged(TableColumnCore tableColumn);

	void columnInvalidate(TableColumnCore tableColumn);
	
	void cellInvalidate(TableColumnCore tableColumn, T data_source);
}
