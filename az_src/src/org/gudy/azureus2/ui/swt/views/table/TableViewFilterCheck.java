/**
 * Created on Oct 4, 2009
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

import com.aelitis.azureus.ui.common.table.TableView;


/**
 * @author TuxPaper
 * @created Oct 4, 2009
 *
 */
public interface TableViewFilterCheck<DATASOURCETYPE>
{
	public boolean filterCheck(DATASOURCETYPE ds, String filter, boolean regex);
	
	public void filterSet(String filter);
	
	public interface
	TableViewFilterCheckEx<DATASOURCETYPE>
		extends TableViewFilterCheck<DATASOURCETYPE>
	{
		public void
		viewChanged( TableView<DATASOURCETYPE> view );
	}
}
