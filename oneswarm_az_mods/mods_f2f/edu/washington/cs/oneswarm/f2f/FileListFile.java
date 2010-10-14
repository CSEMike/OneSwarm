/**
* Copyright (C) 2008 Tomas Isdal
* 
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*
* Created Jul 10, 2008 by isdal
*/
package edu.washington.cs.oneswarm.f2f;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.util.encoders.Base64;

/**
 * @author isdal
 *
 */
public class FileListFile
{
	public final static String KEY_SHA1_HASH = "sha1";

	public final static String KEY_ED2K_HASH = "ed2k";

	private String						 fileName;

	//private Map<String, String> hashes;
	private byte[]						 sha1Hash;

	private byte[]						 ed2kHash;

	private long							 length;

	private int								hashCode;

	public FileListFile() {

	}

	public FileListFile(String fileName, long length) {
		this.fileName = fileName;
		this.length = length;
	}

	public void setSha1Hash(byte[] hash) {
		this.sha1Hash = hash;
	}

	public boolean sha1HashMatch(byte[] hash) {
		if (sha1Hash != null && hash != null) {
			return Arrays.equals(sha1Hash, hash);
		}
		return false;
	}

	public void setEd2kHash(byte[] hash) {
		this.ed2kHash = hash;
	}

	public boolean ed2kHashMatch(byte[] hash) {
		if (ed2kHash != null && hash != null) {
			return Arrays.equals(ed2kHash, hash);
		}
		return false;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public long getLength() {
		return length;
	}

	public void setLength(long length) {
		this.length = length;
	}

	public boolean searchMatch(String searchString) {
		searchString = searchString.toLowerCase();
		String[] terms = FileCollection.quoteRespectingSplit(searchString);

		// check for keywords, if there are keywords it must match them
		if (FileCollection.containsKeyword(searchString)) {
			if (!keyWordMatch(terms)) {
				return false;
			}
		}
		String f = fileName.toLowerCase();
		for (String word : terms) {
			// ignore terms with keywords
			if (!FileCollection.containsKeyword(word)) {
				if (!f.contains(word)) {
					return false;
				}
			}
		}
		return true;
	}

	/*
	 * checks if the file matches a specific keyword
	 */
	public boolean keyWordMatch(String[] termSplit) {
		for (String s : termSplit) {
			for (String keyWordEnd : FileCollection.KEYWORDENDINGS) {
				if (s.contains("sha1" + keyWordEnd)) {
					String[] idSplit = s.split(keyWordEnd);
					if (idSplit.length == 2) {
						String id = idSplit[1];
						byte[] sha1 = Base64.decode(id);
						if (sha1HashMatch(sha1)) {
							//System.err.println("sha1 match");
							return true;
						}
					}
					return false;
				}

				if (s.contains("ed2k" + keyWordEnd)) {
					String[] idSplit = s.split(keyWordEnd);
					if (idSplit.length == 2) {
						String id = idSplit[1];
						byte[] ed2k = Base64.decode(id);
						if (ed2kHashMatch(ed2k)) {
							return true;
						}
					}
					return false;
				}
			}
		}
		return false;
	}

	public boolean equals(Object o) {
		if (!(o instanceof FileListFile)) {
			return false;
		}
		FileListFile f = (FileListFile) o;
		if (f.fileName == null) {
			return false;
		}
		if (fileName == null) {
			return false;
		}
		return f.fileName.equals(fileName);
	}

	public int hashCode() {
		if (hashCode == 0) {
			if (fileName != null) {
				hashCode = fileName.hashCode();
			}
		}
		return hashCode;
	}

	public byte[] getSha1Hash() {
		return sha1Hash;
	}

	public byte[] getEd2kHash() {
		return ed2kHash;
	}
}