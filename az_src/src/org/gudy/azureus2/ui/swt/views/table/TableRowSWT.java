/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.views.table;

import com.aelitis.azureus.ui.common.table.TableRowCore;
import org.eclipse.swt.graphics.*;

/**
 * SWT specifics interfaces for TableRow
 * 
 * @author TuxPaper
 * @created Jan 22, 2007
 *
 */
public interface TableRowSWT extends TableRowCore
{

	public boolean setIconSize(Point pt);

	/** Retreive the color of the row
	 *
	 * @return color of the row
	 */
	public Color getForeground();

	/** Set the color of the row
	 *
	 * @param c new color
	 */
	public void setForeground(Color c);

	public Color getBackground();

	/**
	 * @param cellName
	 * @return
	 */
	public TableCellSWT getTableCellSWT(String cellName);
	
	public Rectangle getBounds();
	
	public void setBackgroundImage(Image image);

	
	/**
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	public int getFontStyle();
	
	/**
	 * @param bold
	 *
	 * @since 3.1.1.1
	 */
	public boolean setFontStyle(int style);

	/**
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	public int getAlpha();

	/**
	 * @param i
	 *
	 * @since 3.1.1.1
	 */
	public boolean setAlpha(int alpha);

	/**
	 * @param selected
	 *
	 * @since 4.4.0.5
	 */
	void setWidgetSelected(boolean selected);
}
