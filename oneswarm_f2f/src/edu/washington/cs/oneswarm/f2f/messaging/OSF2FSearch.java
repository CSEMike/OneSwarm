/**
 * 
 */
package edu.washington.cs.oneswarm.f2f.messaging;

/**
 * @author isdal
 * 
 */
public abstract class OSF2FSearch implements OSF2FMessage {
    // For internal accounting and queue debugging
    private final long objectCreatedTime;
    private final byte version;
    private final int searchID;

    public OSF2FSearch(byte version, int searchID) {
        this.version = version;
        this.searchID = searchID;
        this.objectCreatedTime = System.currentTimeMillis();
    }

    public abstract int getValueID();

    public abstract OSF2FSearch clone();

    public long getObjectCreatedTime() {
        return objectCreatedTime;
    }

    public int getSearchID() {
        return searchID;
    }

    public byte getVersion() {
        return version;
    }
}
