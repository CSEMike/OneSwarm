/*
 * Created on Mar 21, 2006 3:09:00 PM
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
package org.gudy.azureus2.core3.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aelitis.net.magneturi.MagnetURIHandler;

/**
 * @author TuxPaper
 * @created Mar 21, 2006
 *
 */
public class UrlUtils
{
	private static final String[] prefixes = new String[] {
			"http://",
			"https://",
			"ftp://",
			"magnet:?",
			"magnet://?", 
			"oneswarm:?", 
			"oneswarm://?" };

	private static int MAGNETURL_STARTS_AT = 3;
	
	private static final Object[] XMLescapes = new Object[] {
		new String[] { "&", "&amp;" },
		new String[] { ">", "&gt;" },
		new String[] { "<", "&lt;" },
		new String[] { "\"", "&quot;" },
		new String[] { "'", "&apos;" },
	};

	/**
	 * test string for possibility that it's an URL.  Considers 40 byte hex 
	 * strings as URLs
	 * 
	 * @param sURL
	 * @return
	 */
	public static boolean isURL(String sURL) {
		return parseTextForURL(sURL, true) != null;
	}

	public static boolean isURL(String sURL, boolean bGuess) {
		return parseTextForURL(sURL, true, bGuess) != null;
	}

	public static String parseTextForURL(String text, boolean accept_magnets) {
		return parseTextForURL(text, accept_magnets, true);
	}

	public static String parseTextForURL(String text, boolean accept_magnets,
			boolean guess) {

		if (text == null || text.length() < 5) {
			return null;
		}

		String href = parseHTMLforURL(text);
		if (href != null) {
			return href;
		}

		try {
			text = text.trim();
			text = URLDecoder.decode(text);
		} catch (Exception e) {
			// sometimes fires a IllegalArgumentException
			// catch everything and ignore.
		}

		String textLower;
		try {
			textLower = text.toLowerCase();
		} catch (Throwable e) {
			textLower = text;
		}
		int max = accept_magnets ? prefixes.length : MAGNETURL_STARTS_AT;
		int end = -1;
		int start = textLower.length();
		String strURL = null;
		for (int i = 0; i < max; i++) {
			final int testBegin = textLower.indexOf(prefixes[i]);
			if (testBegin >= 0 && testBegin < start) {
				end = text.indexOf("\n", testBegin + prefixes[i].length());
				String strURLTest = (end >= 0) ? text.substring(testBegin, end - 1)
						: text.substring(testBegin);
				try {
					URL parsedURL = new URL(strURLTest);
					strURL = parsedURL.toExternalForm();
				} catch (MalformedURLException e1) {
					e1.printStackTrace();
					if (i >= MAGNETURL_STARTS_AT) {
						strURL = strURLTest;
					}
				}
			}
		}
		if (strURL != null) {
			return strURL;
		}
		
		if (new File(text).exists()) {
			return null;
		}

		// accept raw hash of 40 hex chars
		if (accept_magnets && text.matches("^[a-fA-F0-9]{40}$")) {
			// convert from HEX to raw bytes
			byte[] infohash = ByteFormatter.decodeString(text.toUpperCase());
			// convert to BASE32
			return "magnet:?xt=urn:btih:" + Base32.encode(infohash);
		}

		// accept raw hash of 32 base-32 chars
		if (accept_magnets && text.matches("^[a-zA-Z2-7]{32}$")) {
			return "magnet:?xt=urn:btih:" + text;
		}
		
		// javascript:loadOrAlert('WVOPRHRPFSCLAW7UWHCXCH7QNQIU6TWG')

		// accept raw hash of 32 base-32 chars, with garbage around it
		if (accept_magnets && guess) {
			Pattern pattern = Pattern.compile("[^a-zA-Z2-7][a-zA-Z2-7]{32}[^a-zA-Z2-7]");
			Matcher matcher = pattern.matcher(text);
			if (matcher.find()) {
				String hash = text.substring(matcher.start() + 1, matcher.start() + 33);
				return "magnet:?xt=urn:btih:" + hash;
			}

			pattern = Pattern.compile("[^a-fA-F0-9][a-fA-F0-9]{40}[^a-fA-F0-9]");
			matcher = pattern.matcher(text);
			if (matcher.find()) {
				String hash = text.substring(matcher.start() + 1, matcher.start() + 41);
				// convert from HEX to raw bytes
				byte[] infohash = ByteFormatter.decodeString(hash.toUpperCase());
				// convert to BASE32
				return "magnet:?xt=urn:btih:" + Base32.encode(infohash);
			}
		}

		return null;
	}

	public static String parseHTMLforURL(String text) {
		if (text == null) {
			return null;
		}

		// examples:
		// <A HREF=http://abc.om/moo>test</a>
		// <A style=cow HREF="http://abc.om/moo">test</a>
		// <a href="http://www.gnu.org/licenses/fdl.html" target="_top">moo</a>

		Pattern pat = Pattern.compile("<.*a\\s++.*href=\"?([^\\'\"\\s>]++).*",
				Pattern.CASE_INSENSITIVE);
		Matcher m = pat.matcher(text);
		if (m.find()) {
			String sURL = m.group(1);
			try {
				sURL = URLDecoder.decode(sURL);
			} catch (Exception e) {
				// sometimes fires a IllegalArgumentException
				// catch everything and ignore.
			}
			return sURL;
		}

		return null;
	}

	public static void main(String[] args) {
		
		MagnetURIHandler.getSingleton();
		byte[] infohash = ByteFormatter.decodeString("1234567890123456789012345678901234567890");
		String[] test = {
				"http://moo.com",
				"http%3A%2F/moo%2Ecom",
				"magnet:?moo",
				"magnet%3A%3Fxt=urn:btih:26",
				"magnet%3A//%3Fmooo",
				"magnet:?xt=urn:btih:" + Base32.encode(infohash),
				"aaaaaaaaaabbbbbbbbbbccccccccccdddddddddd",
				"magnet:?dn=OpenOffice.org_2.0.3_Win32Intel_install.exe&xt=urn:sha1:PEMIGLKMNFI4HZ4CCHZNPKZJNMAAORKN&xt=urn:tree:tiger:JMIJVWHCQUX47YYH7O4XIBCORNU2KYKHBBC6DHA&xt=urn:ed2k:1c0804541f34b6583a383bb8f2cec682&xl=96793015&xs=http://mirror.switch.ch/ftp/mirror/OpenOffice/stable/2.0.3/OOo_2.0.3_Win32Intel_install.exe"
				};
		for (int i = 0; i < test.length; i++) {
			System.out.println("decode: " + test[i] + " -> " + URLDecoder.decode(test[i]));
			System.out.println("isURL: " + test[i] + " -> " + isURL(test[i]));
			System.out.println("parse: " + test[i] + " -> " + parseTextForURL(test[i], true));
		}

	}

	/**
	 * Like URLEncoder.encode, except translates spaces into %20 instead of +
	 * @param s
	 * @return
	 */
	public static String encode(String s) {
		if (s == null) {
			return "";
		}
		try {
			return URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20");
		} catch (UnsupportedEncodingException e) {
			return URLEncoder.encode(s).replaceAll("\\+", "%20");
		}
	}
	
	public static String decode(String s) {
		if (s == null) {
			return "";
		}
		try {
			return( URLDecoder.decode(s, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			return( URLDecoder.decode(s));
		}
	}
	
	public static String escapeXML(String s) {
		if (s == null) {
			return "";
		}
		String ret = s;
		for (int i = 0; i < XMLescapes.length; i++) {
			String[] escapeEntry = (String[])XMLescapes[i];
			ret = ret.replaceAll(escapeEntry[0], escapeEntry[1]);
		}
		return ret;
	}

	public static String unescapeXML(String s) {
		if (s == null) {
			return "";
		}
		String ret = s;
		for (int i = 0; i < XMLescapes.length; i++) {
			String[] escapeEntry = (String[])XMLescapes[i];
			ret = ret.replaceAll(escapeEntry[1], escapeEntry[0]);
		}
		return ret;
	}
	
	public static String
	convertIPV6Host(
		String	host )
	{
		if ( host.indexOf(':') != -1 ){
			
			return( "[" + host + "]" );
		}
		
		return( host );
	}
	
	public static String
	expandIPV6Host(
		String	host )
	{
		if ( host.indexOf(':') != -1 ){
			
			try{
				return( InetAddress.getByAddress(InetAddress.getByName( host ).getAddress()).getHostAddress());
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		return( host );
	}
}
