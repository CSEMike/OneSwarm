/*
 * Created: 2004/May/26
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details (
 * see the LICENSE file ).
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * AELITIS, SAS au capital de 46,603.30 euros, 8 Alle Lenotre, La Grille Royale,
 * 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.ui.swt.components;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Image;

public abstract interface BufferedGraphicTableItem
{
	public abstract int getMarginWidth();

	public abstract int getMarginHeight();

	public abstract void setMargin(int width, int height);

	/** Orientation of cell.  SWT.LEFT, SWT.RIGHT, SWT.CENTER, or SWT.FILL.
	 * When SWT.FILL, update() will be called when the size of the cell has 
	 * changed.
	 */
	public abstract void setOrientation(int orientation);

	public abstract int getOrientation();

	public abstract Point getSize();

	public abstract boolean setGraphic(Image img);

	public abstract Image getGraphic();
}
