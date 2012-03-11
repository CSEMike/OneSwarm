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

import java.util.*;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.RGB;

import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

/**
 * @author TuxPaper
 * @created Jun 30, 2006
 *
 */
public class ColorCache
{
	private final static boolean DEBUG = Constants.isCVSVersion();

	private final static Map<Long, Color> mapColors = new HashMap<Long, Color>();
	
	private final static int SYSTEMCOLOR_INDEXSTART = 17;
	private final static String[] systemColorNames = {
		"COLOR_WIDGET_DARK_SHADOW",
		"COLOR_WIDGET_NORMAL_SHADOW",
		"COLOR_WIDGET_LIGHT_SHADOW",
		"COLOR_WIDGET_HIGHLIGHT_SHADOW",
		"COLOR_WIDGET_FOREGROUND",
		"COLOR_WIDGET_BACKGROUND",
		"COLOR_WIDGET_BORDER",
		"COLOR_LIST_FOREGROUND",
		"COLOR_LIST_BACKGROUND",
		"COLOR_LIST_SELECTION",
		"COLOR_LIST_SELECTION_TEXT",
		"COLOR_INFO_FOREGROUND",
		"COLOR_INFO_BACKGROUND",
		"COLOR_TITLE_FOREGROUND",
		"COLOR_TITLE_BACKGROUND",
	};
	
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

	public static Color getSchemedColor(Device device, int red, int green, int blue) {
		ensureMapColorsInitialized(device);

		Long key = new Long(((long) red << 16) + (green << 8) + blue + 0x1000000l);

		Color color = mapColors.get(key);
		if (color == null || color.isDisposed()) {
			try {
				if (red < 0) {
					red = 0;
				} else if (red > 255) {
					red = 255;
				}
				if (green < 0) {
					green = 0;
				} else if (green > 255) {
					green = 255;
				}
				if (blue < 0) {
					blue = 0;
				} else if (blue > 255) {
					blue = 255;
				}
				
	      RGB rgb = new RGB(red, green, blue);
	      float[] hsb = rgb.getHSB();
	      hsb[0] += Colors.diffHue;
	      if (hsb[0] > 360) {
	      	hsb[0] -= 360;
	      } else if (hsb[0] < 0) {
	      	hsb[0] += 360;
	      }
	      hsb[1] *= Colors.diffSatPct;
	      //hsb[2] *= Colors.diffLumPct;
	      
	      color = getColor(device, hsb);
	      mapColors.put(key, color);
			} catch (IllegalArgumentException e) {
				Debug.out("One Invalid: " + red + ";" + green + ";" + blue, e);
			}
		}

		return color;
	}

	public static Color getColor(Device device, int red, int green, int blue) {
		if (device == null || device.isDisposed()) {
			return null;
		}
		ensureMapColorsInitialized(device);

		Long key = new Long(((long) red << 16) + (green << 8) + blue);

		Color color = mapColors.get(key);
		if (color == null || color.isDisposed()) {
			try {
				if (red < 0) {
					red = 0;
				} else if (red > 255) {
					red = 255;
				}
				if (green < 0) {
					green = 0;
				} else if (green > 255) {
					green = 255;
				}
				if (blue < 0) {
					blue = 0;
				} else if (blue > 255) {
					blue = 255;
				}
				color = new Color(device, red, green, blue);
			} catch (IllegalArgumentException e) {
				Debug.out("One Invalid: " + red + ";" + green + ";" + blue, e);
			}
			addColor(key, color);
		}

		return color;
	}

	private static void ensureMapColorsInitialized(Device device) {
		if (device == null || device.isDisposed()) {
			return;
		}
		if (mapColors.size() == 0) {
			for (int i = 1; i <= 16; i++) {
				Color color = device.getSystemColor(i);
				Long key = new Long(((long) color.getRed() << 16)
						+ (color.getGreen() << 8) + color.getBlue());
				addColor(key, color);
			}
			if (DEBUG) {
				SimpleTimer.addPeriodicEvent("ColorCacheChecker", 60000,
						new TimerEventPerformer() {
							public void perform(TimerEvent event) {
								Utils.execSWTThread(new AERunnable() {
									public void runSupport() {
										for (Iterator<Long> iter = mapColors.keySet().iterator(); iter.hasNext();) {
											Long key = iter.next();
											Color color = mapColors.get(key);
											if (color.isDisposed()) {
												Logger.log(new LogAlert(false, LogAlert.AT_ERROR,
														"Someone disposed of color "
																+ Long.toHexString(key.longValue())
																+ ". Please report this on the "
																+ "<A HREF=\"http://forum.vuze.com/forum.jspa?forumID=124\">forum</A>"));
												iter.remove();
											}
										}
									}
								});
							}
						});
			}
		}
	}

	public static Color getColor(Device device, String value) {
		return getColor(device, value, false);
	}

	public static Color getSchemedColor(Device device, String value) {
		return getColor(device, value, true);
	}

	private static Color getColor(Device device, String value, boolean useScheme) {
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
			} else if (value.indexOf(',') > 0) {
				StringTokenizer st = new StringTokenizer(value, ",");
				colors[0] = Integer.parseInt(st.nextToken());
				colors[1] = Integer.parseInt(st.nextToken());
				colors[2] = Integer.parseInt(st.nextToken());
			} else {
				value = value.toUpperCase();
				if (value.startsWith("COLOR_")) {
					for (int i = 0; i < systemColorNames.length; i++) {
						String name = systemColorNames[i];
						if (name.equals(value) && device != null && !device.isDisposed()) {
							return device.getSystemColor(i + SYSTEMCOLOR_INDEXSTART);
						}
					}
				} else if (value.startsWith("BLUE.FADED.")) {
					int idx = Integer.parseInt(value.substring(11));
					return Colors.faded[idx];
				} else if (value.startsWith("BLUE.")) {
					int idx = Integer.parseInt(value.substring(5));
					return Colors.blues[idx];
				} else if (value.equals("ALTROW")) {
					return Colors.colorAltRow;
				}
				return null;
			}
		} catch (Exception e) {
			Debug.out(value, e);
			return null;
		}

		if (!useScheme) {
			return getColor(device, colors[0], colors[1], colors[2]);
		}
		return getSchemedColor(device, colors[0], colors[1], colors[2]);
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
	
	public static Color getRandomColor() {
		if (mapColors.size() == 0) {
			return Colors.black;
		}
		int r = (int) (Math.random() * mapColors.size());
		return (Color) mapColors.values().toArray()[r];
	}

	/**
	 * @param display
	 * @param hsb
	 * @return 
	 *
	 * @since 3.1.1.1
	 */
	public static Color getColor(Device device, float[] hsb) {
		if (hsb[0] < 0) {
			hsb[0] = 0;
		} else if (hsb[0] > 360) {
			hsb[0] = 360;
		}
		if (hsb[1] < 0) {
			hsb[1] = 0;
		} else if (hsb[1] > 1) {
			hsb[1] = 1;
		}
		if (hsb[2] < 0) {
			hsb[2] = 0;
		} else if (hsb[2] > 1) {
			hsb[2] = 1;
		}
		RGB rgb = new RGB(hsb[0], hsb[1], hsb[2]);
		return getColor(device, rgb.red, rgb.green, rgb.blue);
	}

	/**
	 * @param device
	 * @param rgb
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	public static Color getColor(Device device, RGB rgb) {
		return getColor(device, rgb.red, rgb.green, rgb.blue);
	}
}
