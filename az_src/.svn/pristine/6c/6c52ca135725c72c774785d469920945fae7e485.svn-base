/**
 * Created on Jan 5, 2009
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

package org.gudy.azureus2.ui.swt.views.table.utils;

import com.aelitis.azureus.ui.common.table.TableColumnCore;

import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;

/**
 * @author TuxPaper
 * @created Jan 5, 2009
 *
 */
public class TableColumnInfoImpl
	implements TableColumnInfo
{
	String[] categories;

	byte proficiency = TableColumnInfo.PROFICIENCY_INTERMEDIATE;

	private final TableColumnCore column;

	/**
	 * @param column
	 */
	public TableColumnInfoImpl(TableColumnCore column) {
		this.column = column;
	}

	public TableColumnCore getColumn() {
		return column;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.utils.TableColumnInfo#getCategories()
	public String[] getCategories() {
		return categories;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.utils.TableColumnInfo#setCategories(java.lang.String[])
	public void addCategories(String[] categories) {
		if (categories == null || categories.length == 0) {
			return;
		}
		int pos;
		String[] newCategories;
		if (this.categories == null) {
			newCategories = new String[categories.length];
			pos = 0;
		} else {
			newCategories = new String[categories.length + this.categories.length];
			pos = this.categories.length;
			System.arraycopy(this.categories, 0, newCategories, 0, pos);
		}
		System.arraycopy(categories, pos, newCategories, 0, categories.length);
		this.categories = newCategories;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.utils.TableColumnInfo#getProficiency()
	public byte getProficiency() {
		return proficiency;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.utils.TableColumnInfo#setProficiency(int)
	public void setProficiency(byte proficiency) {
		this.proficiency = proficiency;
	}
}
