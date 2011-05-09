package edu.washington.cs.oneswarm.watchdir;

/**
 * TODO: clean this up. create single type from this and
 * DirectoryWatcherListener
 */
public interface UpdatingFileTreeListener {
    public void broadcastChange(UpdatingFileTree path, boolean isDelete);
}
