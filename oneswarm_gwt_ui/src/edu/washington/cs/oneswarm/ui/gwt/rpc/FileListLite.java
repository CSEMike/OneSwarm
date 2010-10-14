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
 * Created Jul 9, 2008 by isdal
 */
package edu.washington.cs.oneswarm.ui.gwt.rpc;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author isdal Simple class to handle the transfer of filelists between the
 *         core and the UI. The normal XML style list is too slow to be
 *         practical if the file lists reach 1000s of files
 * 
 *         This class probably needs to be rewritten to support the several
 *         (incompatible) ways in which we use it. A big problem is that for
 *         some of these uses, all the fields are not set (depends on what we're
 *         converting from)
 */
public class FileListLite implements IsSerializable, Serializable {

	private static final long serialVersionUID = -9096492081064473703L;
	private String fileName;
	private long fileSize;

	private String collentionId;
	private String collectionName;
	private String sha1;
	private String ed2k;

	public String getEd2kHash() {
		return ed2k;
	}

	public void setEd2kHash(String ed2k) {
		this.ed2k = ed2k;
	}

	public String getSha1Hash() {
		return sha1;
	}

	public void setSha1Hash(String sha1) {
		this.sha1 = sha1;
	}

	private long mTotalFilesInGroup;

	private long addedTimeUTC;
	// these are hacks.
	private int mTotalGroupsInList;
	private boolean skipped;
	private boolean finishedDL;
	private boolean oneSwarmNoShare = false;

	public void setOneSwarmNoShare(boolean val) {
		oneSwarmNoShare = val;
	}

	public boolean isOneSwarmNoShare() {
		return oneSwarmNoShare;
	}

	public static long getSerialVersionUID() {
		return serialVersionUID;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public void setCollentionId(String collentionId) {
		this.collentionId = collentionId;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	public void setTotalFileNum(long totalFileNum) {
		this.mTotalFilesInGroup = totalFileNum;
	}

	public FileListLite() {

	}

	public FileListLite(String collentionId, String collectionName, String fileName, long fileSize, long totalFiles, long addedTime, int inTotalGroupsInList, boolean skipped, boolean finishedDL) {
		this.collentionId = collentionId;
		this.collectionName = collectionName;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.mTotalFilesInGroup = totalFiles;
		this.addedTimeUTC = addedTime;
		this.mTotalGroupsInList = inTotalGroupsInList;

		this.skipped = skipped;
		this.finishedDL = finishedDL;
	}

	public long getTimeAddedUTC() {
		return addedTimeUTC;
	}

	public long getTotalFilesInGroup() {
		return mTotalFilesInGroup;
	}

	/**
	 * hack
	 * 
	 * @return
	 */
	public int getTotalGroupsInList() {
		return mTotalGroupsInList;
	}

	public String getFileName() {
		return fileName;
	}

	public long getFileSize() {
		return fileSize;
	}

	public String getCollectionId() {
		return collentionId;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public String toString() {
		return fileName + " " + fileSize + " " + collectionName;
	}

	public boolean isSkipped() {
		return skipped;
	}

	public void setSkipped(boolean skipped) {
		this.skipped = skipped;
	}

	public boolean isFinishedDL() {
		return finishedDL;
	}

	public void setFinishedDL(boolean finishedDL) {
		this.finishedDL = finishedDL;
	}
}
