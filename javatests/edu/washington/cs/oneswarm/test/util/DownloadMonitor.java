package edu.washington.cs.oneswarm.test.util;

import org.gudy.azureus2.core3.download.DownloadManager;

public interface DownloadMonitor {

    public abstract void downloadFinished(DownloadManager manager, long duration);

    public abstract void downloadBootstrapped(DownloadManager dm, long bootstrapInterval);

}