package edu.washington.cs.oneswarm.watchdir;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.FileUtil;

import edu.uw.cse.netlab.reputation.GloballyAwareOneHopUnchoker;
import edu.washington.cs.oneswarm.ui.gwt.BackendErrorLog;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.settings.MagicPath;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.settings.MagicWatchType;

public class DirectoryWatcher extends Thread implements UpdatingFileTreeListener {

    private static Logger logger = Logger.getLogger(DirectoryWatcher.class.getName());

    String mPath = null;

    UpdatingFileTree mTree = null;

    List<DirectoryWatcherListener> mListeners = Collections
            .synchronizedList(new LinkedList<DirectoryWatcherListener>());
    private int mPollWaitSecs = 60;

    private boolean mDone;

    MagicPath mMagicPath = null;

    private File watchFile;

    public DirectoryWatcher(MagicPath inMagicPath, int inPollWaitSecs) throws IOException {
        watchFile = new File(inMagicPath.getPath());
        if (watchFile.exists() == false) {
            throw new IOException("Watch directory doesn't exist!");
        }

        if (watchFile.isDirectory() == false) {
            throw new IOException("Watch directory needs to be a _directory_!");
        }

        mMagicPath = inMagicPath;

        mPath = inMagicPath.getPath();
        mPollWaitSecs = inPollWaitSecs;

        long start = System.currentTimeMillis();
        logger.fine("initial scan of: " + mPath);
        mTree = new UpdatingFileTree(watchFile, this, mMagicPath.getMaxDepth());
        logger.fine("initial creation took: " + (System.currentTimeMillis() - start) + " ms");

        setDaemon(true);
        setName("DirectoryWatcher: " + inMagicPath);
    }

    public String getPath() {
        return mPath;
    }

    public MagicWatchType getWatchType() {
        return mMagicPath.getType();
    }

    public void setDone() {
        mDone = true;
    }

    public void addListener(DirectoryWatcherListener inListener) {
        mListeners.add(inListener);
    }

    public void removeListener(DirectoryWatcherListener inListener) {
        mListeners.remove(inListener);
    }

    public void broadcastChange(UpdatingFileTree path, boolean isDelete) {
        synchronized (mListeners) {
            for (DirectoryWatcherListener listener : mListeners) {
                if (isDelete) {
                    listener.deleteFileObserved(this, path);
                } else {
                    listener.newFileObserved(this, path);
                }
            }
        }
    }

    /**
     * TODO: would be nice to use filesystem notifications here. should be
     * around in java7 (when will this reach macs? 2020?)
     */
    public void run() {
        long start = System.currentTimeMillis();

        while (!mDone) {
            try {
                File watchFile = new File(mPath);

                // deleted?
                if (watchFile.exists() == false) {
                    FileUtil.mkdirs(watchFile);
                    break;
                }

                start = System.currentTimeMillis();
                mTree.update();
                long refreshTime = (System.currentTimeMillis() - start);
                logger.fine("refresh took: " + refreshTime + " ("
                        + mTree.thisFile.getAbsolutePath() + ")");

                // no less than 10 seconds but not more than 5 minutes
                long waitTime = Math.min(Math.max(10 * 1000, 60 * refreshTime), 300 * 1000);
                logger.fine("wait time: " + waitTime);
                Thread.sleep(waitTime);
            } catch (Exception e) {
                e.printStackTrace();
                BackendErrorLog.get().logException(e);

                try {
                    Thread.sleep(1 * 1000);
                } catch (Exception e2) {
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        DirectoryWatcher w = new DirectoryWatcher(new MagicPath("/Volumes/x/watch_test"), 1);
        w.addListener(new DirectoryWatcherListener() {

            public void deleteFileObserved(DirectoryWatcher watcher, UpdatingFileTree inAbsolutePath) {
                logger.finer("deleted: " + inAbsolutePath.thisFile.getAbsolutePath());
            }

            public void newFileObserved(DirectoryWatcher watcher, UpdatingFileTree inAbsolutePath) {
                logger.finer("created: " + inAbsolutePath.thisFile.getAbsolutePath());
            }
        });
        w.run();
    }

}
