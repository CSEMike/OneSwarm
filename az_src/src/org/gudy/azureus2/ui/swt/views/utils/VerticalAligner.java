/*
 * File    : VerticalAligner.java
 * Created : 22 dec. 2003
 * By      : Olivier
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
package org.gudy.azureus2.ui.swt.views.utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ScrollBar;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.ui.swt.Utils;

/** Workaround Eclipse Bug Bug 42416
 *   "[Platform Inconsistency] GC(Table) has wrong origin"
 *   
 *  Fixed in ~3226
 *
 */
public class VerticalAligner {
	private static boolean bFixGTKBug;

	static {
		COConfigurationManager.addAndFireParameterListener("SWT_bGTKTableBug",
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						// some people switch from motif to gtk & back again, so make this
						// only apply to GTK, even if it was enabled prior
						bFixGTKBug = COConfigurationManager
								.getBooleanParameter("SWT_bGTKTableBug")
								&& Utils.isGTK && SWT.getVersion() < 3226;
					}
				});
	}

	public static int getTableAdjustVerticalBy(Table t) {
		if (!bFixGTKBug || t == null || t.isDisposed())
			return 0;
		return -t.getHeaderHeight();
	}

	public static int getTableAdjustHorizontallyBy(Table t) {
		if (!bFixGTKBug || t == null || t.isDisposed())
			return 0;
		ScrollBar sb = t.getHorizontalBar();
		if (sb == null)
			return 0;
		return sb.getSelection();
	}

}
