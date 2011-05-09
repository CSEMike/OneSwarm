package edu.washington.cs.oneswarm.ui.gwt.rpc;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Class used to transfer information about torrents between the ui and the core
 * 
 * @author isdal
 * 
 */
public class TorrentInfo implements IsSerializable {

    // taken from Download class

    /** waiting to be told to start preparing */
    public static final int ST_WAITING = 1;
    /** getting files ready (allocating/checking) */
    public static final int ST_PREPARING = 2;
    /** ready to be started if required */
    public static final int ST_READY = 3;
    /** downloading */
    public static final int ST_DOWNLOADING = 4;
    /** seeding */
    public static final int ST_SEEDING = 5;
    /** stopping */
    public static final int ST_STOPPING = 6;
    /** stopped, do not auto-start! */
    public static final int ST_STOPPED = 7;
    /** failed */
    public static final int ST_ERROR = 8;
    /** stopped, but ready for auto-starting */
    public static final int ST_QUEUED = 9;

    // error states
    public static final short NO_PERMISSIONS = 1;
    // TODO: missing files.

    // bitwise AND of above states.
    private short errorState;

    private int hashCode = 0;

    private int basicHashCode = 0;

    private String torrentID = "";

    private String name = "";

    private String comment = "";

    private long downloaded = 0;

    private long totalSize = 0;

    private int progress = 0;

    private String length = "";

    private long addedDate = 0;

    private int status = 0;

    private String defaultMovieName = "";

    private String remaining = "";

    private double downloadRate = 0;

    private double uploadRate = 0;

    private String statusText = "";

    private long totalUploaded = 0;

    private long totalDownloaded = 0;

    private int seeders = 0;
    private int seedersF2F = 0;

    private int totalSeeders = 0;

    private int leechers = 0;
    private int leechersF2f = 0;

    private long extraSourceSpeed = 0;

    public long getExtraSourceSpeed() {
        return extraSourceSpeed;
    }

    public void setExtraSourceSpeed(long extraSourceSpeed) {
        this.extraSourceSpeed = extraSourceSpeed;
    }

    // for things like artist / album
    private String extraKeywords = "";

    public short getErrorState() {
        return errorState;
    }

    public void setErrorState(short errorState) {
        this.errorState = errorState;
    }

    public int getSeedersF2F() {
        return seedersF2F;
    }

    public void setSeedersF2F(int seedersF2F) {
        this.seedersF2F = seedersF2F;
    }

    public int getLeechersF2f() {
        return leechersF2f;
    }

    public void setLeechersF2f(int leechersF2f) {
        this.leechersF2f = leechersF2f;
    }

    private int totalLeechers = 0;

    private double availability = 0;

    private int numFiles = 0;

    private int friendID = -2;

    private boolean shareWithFriends = false;
    private boolean sharePublic = false;

    public String getDefaultMovieName() {
        return defaultMovieName;
    }

    public void setDefaultMovieName(String defaultMovieName) {
        if (defaultMovieName == null) {
            defaultMovieName = "";
        }
        this.defaultMovieName = defaultMovieName;
    }

    public boolean isF2FOnly() {
        return mFileListLiteRep != null;
    }

    FileListLite mFileListLiteRep = null;

    public void setFileListLiteRep(FileListLite rep) {
        mFileListLiteRep = rep;
    }

    public FileListLite getFileListLiteRepresentation() {
        return mFileListLiteRep;
    }

    private String friendNick = null;

    public void setF2F_ID(int friendID, String nick) {
        this.friendID = friendID;
        this.friendNick = nick;
    }

    public int getF2F_ID() {
        return friendID;
    }

    public String getF2F_nick() {
        return friendNick;
    }

    public int getStatus() {
        return status;
    }

    public boolean equals(Object rhs) {
        if (!(rhs instanceof TorrentInfo))
            return false;

        return torrentID.equals(((TorrentInfo) rhs).torrentID);
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(long addedDate) {
        this.addedDate = addedDate;
    }

    public TorrentInfo() {
    }

    public String getComment() {
        return comment;
    }

    public long getDownloaded() {
        return downloaded;
    }

    public String getName() {
        return name;
    }

    public int getProgress() {
        return progress;
    }

    public String getTorrentID() {
        return torrentID;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setDownloaded(long downloaded) {
        this.downloaded = downloaded;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void setTorrentID(String torrentID) {
        this.torrentID = torrentID;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public int hashCode() {
        return torrentID.hashCode();
    }

    public void calcHashCode() {
        calcDetailedHashCode();
        calcBasicHashCode();
    }

    public void calcDetailedHashCode() {
        this.hashCode += torrentID.hashCode();
        this.hashCode += name.hashCode();
        this.hashCode += comment.hashCode();
        this.hashCode += downloaded;
        this.hashCode += totalSize;
        this.hashCode += progress;
        this.hashCode += length.hashCode();
        this.hashCode += status;
        this.hashCode += defaultMovieName.hashCode();
        this.hashCode += remaining.hashCode();
        this.hashCode += statusText.hashCode();
        this.hashCode += totalUploaded;
        this.hashCode += totalDownloaded;
        this.hashCode += seeders;
        this.hashCode += totalSeeders;
        this.hashCode += leechers;
        this.hashCode += totalLeechers;
        this.hashCode += availability;
        // System.out.println("hashcode for " + name + " is " + hashCode);
    }

    public void calcBasicHashCode() {
        this.basicHashCode += torrentID.hashCode();
        this.basicHashCode += name.hashCode();
        this.basicHashCode += comment.hashCode();

        this.basicHashCode += progress;
        this.basicHashCode += length.hashCode();
        this.basicHashCode += status;

        this.basicHashCode += remaining.hashCode();

    }

    public double getDownloadRate() {
        return downloadRate;
    }

    public void setDownloadRate(double downloadRate) {
        this.downloadRate = downloadRate;
    }

    public int getBasicHashCode() {
        return basicHashCode;
    }

    public int getFullHashCode() {
        return hashCode;
    }

    public void setHashCode(int hashCode) {
        this.hashCode = hashCode;
    }

    public String getRemaining() {
        return remaining;
    }

    public void setRemaining(String remaining) {
        this.remaining = remaining;
    }

    public double getUploadRate() {
        return uploadRate;
    }

    public void setUploadRate(double uploadRate) {
        this.uploadRate = uploadRate;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public long getTotalUploaded() {
        return totalUploaded;
    }

    public void setTotalUploaded(long totalUploaded) {
        this.totalUploaded = totalUploaded;
    }

    public long getTotalDownloaded() {
        return totalDownloaded;
    }

    public void setTotalDownloaded(long totalDownloaded) {
        this.totalDownloaded = totalDownloaded;
    }

    public int getSeeders() {
        return seeders;
    }

    public void setSeeders(int seeders) {
        this.seeders = seeders;
    }

    public int getLeechers() {
        return leechers;
    }

    public void setLeechers(int leechers) {
        this.leechers = leechers;
    }

    public double getAvailability() {
        return availability;
    }

    public void setAvailability(double availability) {
        this.availability = availability;
    }

    public int getTotalSeeders() {
        return totalSeeders;
    }

    public void setTotalSeeders(int totalSeeders) {
        this.totalSeeders = totalSeeders;
    }

    public int getTotalLeechers() {
        return totalLeechers;
    }

    public void setTotalLeechers(int totalLeechers) {
        this.totalLeechers = totalLeechers;
    }

    public boolean isStarted() {
        int status = getStatus();
        return status == ST_PREPARING || status == ST_READY || status == ST_DOWNLOADING
                || status == ST_SEEDING;
    }

    public boolean getShareWithFriends() {
        return shareWithFriends;
    }

    public void setShareWithFriends(boolean shareWithFriends) {
        this.shareWithFriends = shareWithFriends;
    }

    public boolean getSharePublic() {
        return sharePublic;
    }

    public void setSharePublic(boolean sharePublic) {
        this.sharePublic = sharePublic;
    }

    public int getNumFiles() {
        return numFiles;
    }

    public void setNumFiles(int num) {
        this.numFiles = num;
    }

    public String toString() {
        return this.getName();
    }
}
