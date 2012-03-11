/**
 * Created on Jan 10, 2009
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

package org.gudy.azureus2.plugins.ui.tables;

import org.gudy.azureus2.plugins.ui.config.Parameter;


/**
 * @author TuxPaper
 * @created Jan 10, 2009
 *
 */
public interface TableColumnInfo
{
	public byte PROFICIENCY_BEGINNER = Parameter.MODE_BEGINNER;
	public byte PROFICIENCY_INTERMEDIATE = Parameter.MODE_INTERMEDIATE;
	public byte PROFICIENCY_ADVANCED = Parameter.MODE_ADVANCED;

	public String[] getCategories();

	public void addCategories(String[] categories);

	public byte getProficiency();

	public void setProficiency(byte proficiency);

	TableColumn getColumn();
}