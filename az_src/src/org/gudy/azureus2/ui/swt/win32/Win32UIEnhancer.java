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

package org.gudy.azureus2.ui.swt.win32;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.internal.win32.TCHAR;

/**
 * @author TuxPaper
 * @created Nov 29, 2006
 *
 * Note: You can safely exclude this class from the build path.
 * All calls to this class use (or at least should use) reflection
 */
public class Win32UIEnhancer
{
	static String findProgramKey(String extension) {
		if (extension == null)
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if (extension.length() == 0)
			return null;
		if (extension.charAt(0) != '.')
			extension = "." + extension; //$NON-NLS-1$
		/* Use the character encoding for the default locale */
		TCHAR key = new TCHAR(0, extension, true);
		int[] phkResult = new int[1];
		if (OS.RegOpenKeyEx(OS.HKEY_CLASSES_ROOT, key, 0, OS.KEY_READ, phkResult) != 0) {
			return null;
		}
		int[] lpcbData = new int[1];
		int result = OS.RegQueryValueEx(phkResult[0], null, 0, null, (TCHAR) null,
				lpcbData);
		if (result == 0) {
			TCHAR lpData = new TCHAR(0, lpcbData[0] / TCHAR.sizeof);
			result = OS.RegQueryValueEx(phkResult[0], null, 0, null, lpData, lpcbData);
			if (result == 0)
				return lpData.toString(0, lpData.strlen());
		}
		OS.RegCloseKey(phkResult[0]);
		return null;
	}

	public static ImageData getBigImageData(String extension) {
		String key = findProgramKey(extension);
		if (key == null) {
			return null;
		}

		/* Icon */
		String DEFAULT_ICON = "\\DefaultIcon"; //$NON-NLS-1$
		String iconName = getKeyValue(key + DEFAULT_ICON, true);
		if (iconName == null)
			iconName = ""; //$NON-NLS-1$

		int nIconIndex = 0;
		String fileName = iconName;
		int index = iconName.indexOf(',');
		if (index != -1) {
			fileName = iconName.substring(0, index);
			String iconIndex = iconName.substring(index + 1, iconName.length()).trim();
			try {
				nIconIndex = Integer.parseInt(iconIndex);
			} catch (NumberFormatException e) {
			}
		}
		/* Use the character encoding for the default locale */
		TCHAR lpszFile = new TCHAR(0, fileName, true);
		int[] phiconSmall = null, phiconLarge = new int[1];
		OS.ExtractIconEx(lpszFile, nIconIndex, phiconLarge, phiconSmall, 1);
		if (phiconLarge[0] == 0) {
			return null;
		}
//		Image image = Image.win32_new(null, SWT.ICON, phiconLarge[0]);
//		ImageData imageData = image.getImageData();
//		image.dispose();
//		return imageData;
		return null;
	}

	static String getKeyValue(String string, boolean expand) {
		/* Use the character encoding for the default locale */
		TCHAR key = new TCHAR(0, string, true);
		int[] phkResult = new int[1];
		if (OS.RegOpenKeyEx(OS.HKEY_CLASSES_ROOT, key, 0, OS.KEY_READ, phkResult) != 0) {
			return null;
		}
		String result = null;
		int[] lpcbData = new int[1];
		if (OS.RegQueryValueEx(phkResult[0], (TCHAR) null, 0, null, (TCHAR) null,
				lpcbData) == 0) {
			result = "";
			int length = lpcbData[0] / TCHAR.sizeof;
			if (length != 0) {
				/* Use the character encoding for the default locale */
				TCHAR lpData = new TCHAR(0, length);
				if (OS.RegQueryValueEx(phkResult[0], null, 0, null, lpData, lpcbData) == 0) {
					if (!OS.IsWinCE && expand) {
						length = OS.ExpandEnvironmentStrings(lpData, null, 0);
						if (length != 0) {
							TCHAR lpDst = new TCHAR(0, length);
							OS.ExpandEnvironmentStrings(lpData, lpDst, length);
							result = lpDst.toString(0, Math.max(0, length - 1));
						}
					} else {
						length = Math.max(0, lpData.length() - 1);
						result = lpData.toString(0, length);
					}
				}
			}
		}
		if (phkResult[0] != 0)
			OS.RegCloseKey(phkResult[0]);
		return result;
	}
}
