/**
 * Created on May 12, 2010
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

package org.eclipse.swt.widgets;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.Constants;

/**
 * Windows specific hack to remove expando<P>
 * 
 * This class can be safely excluded from non-Windows builds
 * 
 * @author TuxPaper
 * @created May 12, 2010
 *
 */
public class Tree2
	extends Tree
{
	static final Map<Integer, Integer> mapStyleToWidgetStyle = new HashMap<Integer, Integer>(
			0);

	public Tree2(Composite parent, int style) {
		super(parent, style);
	}

	protected void checkSubclass() {
		// skip check
	}

	// @see org.eclipse.swt.widgets.Tree#createHandle()
	void createHandle() {
		// lucky for us, other platforms have a super.createHandle too
		super.createHandle();
		//if (explorerTheme) {
		//	int bits2 = (int)/*64*/OS.SendMessage (handle, OS.TVM_GETEXTENDEDSTYLE, 0, 0);
		//	bits2 &= ~OS.TVS_EX_FADEINOUTEXPANDOS;
		//	OS.SendMessage (handle, OS.TVM_SETEXTENDEDSTYLE, 0, bits2);
		//}

		try {
			Class<?> claOS = Class.forName("org.eclipse.swt.internal.win32.OS");

			int TVM_GETEXTENDEDSTYLE = ((Number) claOS.getField(
					"TVM_GETEXTENDEDSTYLE").get(null)).intValue();
			int TVM_SETEXTENDEDSTYLE = ((Number) claOS.getField(
					"TVM_SETEXTENDEDSTYLE").get(null)).intValue();
			int TVS_EX_FADEINOUTEXPANDOS = ((Number) claOS.getField(
					"TVS_EX_FADEINOUTEXPANDOS").get(null)).intValue();

			Field fldHandle = this.getClass().getField("handle");
			Class<?> handleType = fldHandle.getType();
			if (handleType == int.class) {
				Method methSendMessage = claOS.getMethod("SendMessage", int.class,
						int.class, int.class, int.class);
				Number nbits2 = (Number) methSendMessage.invoke(null,
						fldHandle.get(this), TVM_GETEXTENDEDSTYLE, 0, 0);
				int bits2 = nbits2.intValue() & (~TVS_EX_FADEINOUTEXPANDOS);

				methSendMessage.invoke(null, ((Number) fldHandle.get(this)).intValue(),
						TVM_SETEXTENDEDSTYLE, 0, bits2);
			} else {
				Method methSendMessage = claOS.getMethod("SendMessage", long.class,
						int.class, long.class, long.class);

				Number nbits2 = (Number) methSendMessage.invoke(null,
						fldHandle.get(this), TVM_GETEXTENDEDSTYLE, 0, 0);
				long bits2 = nbits2.longValue() & (~TVS_EX_FADEINOUTEXPANDOS);

				methSendMessage.invoke(null,
						((Number) fldHandle.get(this)).longValue(), TVM_SETEXTENDEDSTYLE,
						0, bits2);
			}

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	int widgetStyle() {
		/* I was going to go with this code, but OSX doesn't ahve widgetStyle,
		    so compliling breaks on super call
		int oldStyle = super.widgetStyle();
		try {
			Class<?> claOS = Class.forName("org.eclipse.swt.internal.win32.OS");
			// & ~(OS.TVS_LINESATROOT | OS.TVS_HASBUTTONS)
			if (claOS != null) {
				int TVS_LINESATROOT = ((Number)claOS.getField("TVS_LINESATROOT").get(null)).intValue();
				int TVS_HASBUTTONS = ((Number)claOS.getField("TVS_HASBUTTONS").get(null)).intValue();
				oldStyle &= ~(TVS_HASBUTTONS | TVS_LINESATROOT);
			}
		} catch (Throwable e) {
		}
		return oldStyle;
		*/

		if (!Constants.isWindows) {
			return 0;
		}

		try {
			Integer widgetStyle = mapStyleToWidgetStyle.get(style);
			if (widgetStyle != null) {
				return widgetStyle.intValue();
			}

			Tree tree = new Tree(parent, style);
			Method method = Tree.class.getDeclaredMethod("widgetStyle");
			method.setAccessible(true);
			int oldStyle = ((Number) method.invoke(tree)).intValue();
			tree.dispose();

			Class<?> claOS = Class.forName("org.eclipse.swt.internal.win32.OS");
			// & ~(OS.TVS_LINESATROOT | OS.TVS_HASBUTTONS)
			if (claOS != null) {
				int TVS_LINESATROOT = ((Number) claOS.getField("TVS_LINESATROOT").get(
						null)).intValue();
				int TVS_HASBUTTONS = ((Number) claOS.getField("TVS_HASBUTTONS").get(
						null)).intValue();
				oldStyle &= ~(TVS_HASBUTTONS | TVS_LINESATROOT);
			}
			mapStyleToWidgetStyle.put(style, oldStyle);
			return oldStyle;
		} catch (Throwable e) {
			e.printStackTrace();
		}

		return 0;
	}
}
