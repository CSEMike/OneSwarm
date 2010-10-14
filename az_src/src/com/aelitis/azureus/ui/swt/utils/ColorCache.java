/*
 * Created on Jun 30, 2006 6:22:44 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.ui.swt.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;

import org.gudy.azureus2.core3.util.*;

/**
 * @author TuxPaper
 * @created Jun 30, 2006
 *
 */
public class ColorCache
{
	private final static Map mapColors = new HashMap();
	
	static {
		AEDiagnostics.addEvidenceGenerator(new AEDiagnosticsEvidenceGenerator() {
			public void generate(IndentWriter writer) {
				writer.println("Colors:");
				writer.indent();
				writer.println("# cached: " + mapColors.size());
				writer.exdent();
			}
		});
	}

	public static Color getColor(Device device, int red, int green, int blue) {
		if (mapColors.size() == 0) {
			for (int i = 1; i <= 16; i++) {
				Color color = device.getSystemColor(i);
				Long key = new Long(((long) color.getRed() << 16)
						+ (color.getGreen() << 8) + color.getBlue());
				addColor(key, color);
			}
		}

		Long key = new Long(((long) red << 16) + (green << 8) + blue);

		Color color = (Color) mapColors.get(key);
		if (color == null || color.isDisposed()) {
			try {
				color = new Color(device, red, green, blue);
			} catch (IllegalArgumentException e) {
				Debug.out("One Invalid: " + red + ";" + green + ";" + blue, e);
			}
			addColor(key, color);
		}

		return color;
	}

	public static Color getColor(Device device, String value) {
		int[] colors = new int[3];

		if (value == null || value.length() == 0) {
			return null;
		}

		try {
			if (value.charAt(0) == '#') {
				// hex color string
				long l = Long.parseLong(value.substring(1), 16);
				colors[0] = (int) ((l >> 16) & 255);
				colors[1] = (int) ((l >> 8) & 255);
				colors[2] = (int) (l & 255);
			} else {
				StringTokenizer st = new StringTokenizer(value, ",");
				colors[0] = Integer.parseInt(st.nextToken());
				colors[1] = Integer.parseInt(st.nextToken());
				colors[2] = Integer.parseInt(st.nextToken());
			}
		} catch (Exception e) {
			Debug.out(value, e);
			return null;
		}

		return getColor(device, colors[0], colors[1], colors[2]);
	}

	private static void addColor(Long key, Color color) {
		mapColors.put(key, color);
	}

	/**
	 * @param device
	 * @param color
	 * @return
	 *
	 * @since 3.0.4.3
	 */
	public static Color getColor(Device device, int[] rgb) {
		if (rgb == null || rgb.length < 3) {
			return null;
		}
		return getColor(device, rgb[0], rgb[1], rgb[2]);
	}
}
