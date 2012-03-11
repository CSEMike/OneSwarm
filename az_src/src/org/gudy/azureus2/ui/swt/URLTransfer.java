/*
 * Created on 01.12.2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
 * 
 */
package org.gudy.azureus2.ui.swt;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.UrlUtils;

/**
 * URL Transfer type for Drag and Drop of URLs
 * Windows IDs are already functional.
 * 
 * Please use Win32TransferTypes to determine the IDs for other OSes!
 * 
 * @see org.gudy.azureus2.ui.swt.test.Win32TransferTypes
 * @author Rene Leonhardt
 * 
 * @author TuxPaper (require incoming string types have an URL prefix)
 * @author TuxPaper (UTF-8, UTF-16, BOM stuff)
 * 
 * TuxPaper's Notes:
 * This class is flakey.  It's better to use HTMLTransfer, and then parse
 * the URL from the HTML.  However, IE drag and drops do not support 
 * HTMLTransfer, so this class must stay
 * 
 * Windows
 * ---
 * TypeIDs seem to be assigned differently on different platform versions 
 * (or maybe even different installations!).   Here's some examples
 * 49314: Moz/IE 0x01 4-0x00 0x80 lots-of-0x00 "[D]URL" lots-more-0x00
 * 49315: Moz/IE Same as 49315, except unicode
 * 49313: Moz/IE URL in .url format  "[InternetShortcut]\nURL=%1"
 * 49324: Moz/IE URL in text format
 * 49395: Moz Same as 49324, except unicode
 * 49319: Moz Dragged HTML Fragment with position information
 * 49398: Moz Dragged HTML Fragment (NO position information, just HTML), unicode
 * 49396: Moz HTML.  Unknown.
 * 
 * There's probably a link to the ID and they type name in the registry, or
 * via a Windows API call.  We don't want to do that, and fortunately, 
 * SWT doesn't seem to pay attention to getTypeIds() on Windows, so we check
 * every typeid we get to see if we can parse an URL from it.
 * 
 * Also, dragging from the IE URL bar hangs SWT (sometimes for a very long 
 * time).  Fortunately, most people willdrag the URL from the actual content
 * window.
 * 
 * Dragging an IE bookmark is actually dragging the .url file, and should be
 * handled by the FileTranfer (and then opening it and extracting the URL).
 * Moz Bookmarks are processed as HTML.
 * 
 * Linux
 * ---
 * For Linux, this class isn't required.  
 * HTMLTransfer will take care of Gecko and Konquerer.
 * 
 * Opera
 * ---
 * As of 8.5, Opera still doesn't allow dragging outside of itself (at least on
 * windows)
 * 
 */

public class URLTransfer extends ByteArrayTransfer {
	/** We are in the process of checking a string to see if it's a valid URL */
	private boolean bCheckingString = false;
	
	private static boolean DEBUG = false;

  private static URLTransfer _instance = new URLTransfer();

  // Opera 7 LINK DRAG & DROP IMPOSSIBLE (just inside Opera)
  private static final String[] supportedTypes = new String[] {
			"CF_UNICODETEXT", 
			"CF_TEXT",
			"OEM_TEXT"
			};

	private static final int[] supportedTypeIds = new int[] { 
		13, 
		1, 
		17
		}; 

  public static URLTransfer getInstance() {
		return _instance;
	}

	public void javaToNative(Object object, TransferData transferData) {
		if (DEBUG)
			System.out.println("javaToNative called");

		if (object == null || !(object instanceof URLType[]))
			return;

		if (isSupportedType(transferData)) {
			URLType[] myTypes = (URLType[]) object;
			try {
				// write data to a byte array and then ask super to convert to pMedium
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				DataOutputStream writeOut = new DataOutputStream(out);
				for (int i = 0, length = myTypes.length; i < length; i++) {
					writeOut.writeBytes(myTypes[i].linkURL);
					writeOut.writeBytes("\n");
					writeOut.writeBytes(myTypes[i].linkText);
				}
				byte[] buffer = out.toByteArray();
				writeOut.close();

				super.javaToNative(buffer, transferData);

			} catch (IOException e) {
			}
		}
	}

	public Object nativeToJava(TransferData transferData) {
		if (DEBUG) System.out.println("nativeToJava called");
		try {
			if (isSupportedType(transferData)) {
				byte [] buffer = (byte[]) super.nativeToJava(transferData);
				return bytebufferToJava(buffer);
			}
		} catch (Exception e) {
			Debug.out(e);
		}

		return null;
	}
	
	public URLType bytebufferToJava(byte[] buffer) {

		if (buffer == null) {
			if (DEBUG) System.out.println("buffer null");
			return null;
		}

		URLType myData = null;
		try {
			String data;
			if (buffer.length > 1) {
				if (DEBUG) {
					for (int i = 0; i < buffer.length; i++) {
						if (buffer[i] >= 32)
							System.out.print(((char) buffer[i]));
						else
							System.out.print("#");
					}
					System.out.println();
				}
				boolean bFirst0 = buffer[0] == 0;
				boolean bSecond0 = buffer[1] == 0;
				if (bFirst0 && bSecond0)
					// This is probably UTF-32 Big Endian.  
					// Let's hope default constructor can handle it (It can't)
					data = new String(buffer);
				else if (bFirst0)
					data = new String(buffer, "UTF-16BE");
				else if (bSecond0)
					data = new String(buffer, "UTF-16LE");
				else if (buffer[0] == (byte) 0xEF && buffer[1] == (byte) 0xBB
						&& buffer.length > 3 && buffer[2] == (byte) 0xBF)
					data = new String(buffer, 3, buffer.length - 3, "UTF-8");
				else if (buffer[0] == (byte) 0xFF || buffer[0] == (byte) 0xFE)
					data = new String(buffer, "UTF-16");
				else {
					data = new String(buffer);
				}
			} else {
				// Older Code:
				// Remove 0 values from byte array, messing up any Unicode strings 
				byte[] text = new byte[buffer.length];
				int j = 0;
				for (int i = 0; i < buffer.length; i++) {
					if (buffer[i] != 0)
						text[j++] = buffer[i];
				}

				data = new String(text, 0, j);
			}

			int iPos = data.indexOf("\nURL=");
			if (iPos > 0) {
				int iEndPos = data.indexOf("\r", iPos);
				if (iEndPos < 0) {
					iEndPos = data.length();
				}
				myData = new URLType();
				myData.linkURL = data.substring(iPos + 5, iEndPos);
				myData.linkText = "";
			} else {
				String[] split = data.split("[\r\n]+", 2);

				myData = new URLType();
				myData.linkURL = (split.length > 0) ? split[0] : "";
				myData.linkText = (split.length > 1) ? split[1] : "";
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return myData;
	}

	protected String[] getTypeNames() {
		return supportedTypes;
	}

	protected int[] getTypeIds() {
		return supportedTypeIds;
	}

	/**
	 * @param transferData
	 * @see org.eclipse.swt.dnd.Transfer#isSupportedType(org.eclipse.swt.dnd.TransferData)
	 * @return
	 */
	public boolean isSupportedType(TransferData transferData) {
		if (bCheckingString)
			return true;

		if (transferData == null)
			return false;

		// TODO: Check if it's a string list of URLs

		// String -- Check if URL, skip to next if not
		URLType url = null;

		if (DEBUG) System.out.println("Checking if type #" + transferData.type + " is URL");

		bCheckingString = true;
		try {
			byte[] buffer = (byte[]) super.nativeToJava(transferData);
			url = bytebufferToJava(buffer);
		} catch (Exception e) {
			Debug.out(e);
		} finally {
			bCheckingString = false;
		}

		if (url == null) {
			if (DEBUG) System.out.println("no, Null URL for type #" + transferData.type);
			return false;
		}

		if (UrlUtils.isURL(url.linkURL, false)) {
			if (DEBUG) System.out.println("Yes, " + url.linkURL + " of type #" + transferData.type);
			return true;
		}

		if (DEBUG) System.out.println("no, " + url.linkURL + " not URL for type #" + transferData.type);
		return false;
	}

	/**
	 * Sometimes, CF_Text will be in currentDataType even though CF_UNICODETEXT 
	 * is present.  This is a workaround until its fixed properly.
	 * <p>
	 * Place it in <code>dropAccept</code>
	 * 
	 * <pre>
	 *if (event.data instanceof URLTransfer.URLType)
	 *	event.currentDataType = URLTransfer.pickBestType(event.dataTypes, event.currentDataType);
	 * </pre>
	 * 
	 * @param dataTypes
	 * @param def
	 * @return
	 */
	public static TransferData pickBestType(TransferData[] dataTypes,
			TransferData def) {
		for (int i = 0; i < supportedTypeIds.length; i++) {
			int supportedTypeID = supportedTypeIds[i];
			for (int j = 0; j < dataTypes.length; j++) {
				try {
  				TransferData data = dataTypes[j];
  				if (supportedTypeID == data.type)
  					return data;
				} catch (Throwable t) {
					Debug.out("Picking Best Type", t);
				}
			}
		}
		return def;
	}

	public class URLType {
		public String linkURL;

		public String linkText;

		public String toString() {
			return linkURL + "\n" + linkText;
		}
	}

	/**
	 * Test for varioud UTF Strings
	 * BOM information from http://www.unicode.org/faq/utf_bom.html
	 * @param args
	 */
	public static void main(String[] args) {

		Map map = new LinkedHashMap();
		map.put("UTF-8", new byte[] { (byte) 0xEF, (byte) 0xbb, (byte) 0xbf, 'H',
				'i' });
		map.put("UTF-32 BE BOM", new byte[] { 0, 0, (byte) 0xFE, (byte) 0xFF, 'H',
				0, 0, 0, 'i', 0, 0, 0 });
		map.put("UTF-16 LE BOM", new byte[] { (byte) 0xFF, (byte) 0xFE, 'H', 0,
				'i', 0 });
		map.put("UTF-16 BE BOM", new byte[] { (byte) 0xFE, (byte) 0xFF, 0, 'H', 0,
				'i' });
		map.put("UTF-16 LE", new byte[] { 'H', 0, 'i', 0 });
		map.put("UTF-16 BE", new byte[] { 0, 'H', 0, 'i' });

		for (Iterator iterator = map.keySet().iterator(); iterator.hasNext();) {
			String element = (String) iterator.next();
			System.out.println(element + ":");
			byte[] buffer = (byte[]) map.get(element);

			boolean bFirst0 = buffer[0] == 0;
			boolean bSecond0 = buffer[1] == 0;
			String data = "";
			try {
				if (bFirst0 && bSecond0)
					// This is probably UTF-32 Big Endian.  
					// Let's hope default constructor can handle it (It can't)
					data = new String(buffer);
				else if (bFirst0)
					data = new String(buffer, "UTF-16BE");
				else if (bSecond0)
					data = new String(buffer, "UTF-16LE");
				else if (buffer[0] == (byte) 0xEF && buffer[1] == (byte) 0xBB
						&& buffer.length > 3 && buffer[2] == (byte) 0xBF)
					data = new String(buffer, 3, buffer.length - 3, "UTF-8");
				else if (buffer[0] == (byte) 0xFF || buffer[0] == (byte) 0xFE)
					data = new String(buffer, "UTF-16");
				else {
					data = new String(buffer);
				}
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println(data);
		}
	}

}
