package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.plugins.torrent.TorrentException;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.ui.gwt.CoreInterface;
import edu.washington.cs.oneswarm.ui.gwt.rpc.BackendTask;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants.InOrderType;
import edu.washington.cs.oneswarm.ui.gwt.server.BackendTaskManager;
import edu.washington.cs.oneswarm.ui.gwt.server.PreviewImageGenerator;
import edu.washington.cs.oneswarm.ui.gwt.server.BackendTaskManager.CancellationListener;
import edu.washington.cs.oneswarm.watchdir.MagicDirectoryManager;

public class FFMpegAsyncOperationManager {
    private static Logger logger = Logger.getLogger(FFMpegAsyncOperationManager.class.getName());

    private final static FFMpegAsyncOperationManager instance = new FFMpegAsyncOperationManager();

    public static FFMpegAsyncOperationManager getInstance() {
        return instance;
    }

    public static class DataNotAvailableException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    private final ExecutorService executors;

    protected FFMpegAsyncOperationManager() {
        executors = new ThreadPoolExecutor(1, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("FFMpeg worker thread");
                        t.setDaemon(true);
                        return t;
                    }
                });
    }

    private final HashSet<File> currentGetMovieInfoJobs = new HashSet<File>();
    private final HashSet<File> currentPreviewJobs = new HashSet<File>();

    public String getDebugInfo() {
        StringBuilder b = new StringBuilder();
        b.append("Queued movie stream info requests: ");
        synchronized (currentGetMovieInfoJobs) {
            for (File f : currentGetMovieInfoJobs) {
                b.append("  " + f);
            }
        }

        return b.toString();
    }

    /**
     * Non blocking method for getting movie info, if the data isn't available
     * an ffmpeg operation will be queued so that any subsequent calls will get
     * the data
     * 
     * @param file
     * @return
     * @throws DataNotAvailableException
     */
    public MovieStreamInfo getMovieInfo(final byte[] infohash, final File file, long timeout,
            TimeUnit unit) throws DataNotAvailableException {
        if (file == null) {
            throw new DataNotAvailableException();
        }
        if (infohash == null) {
            throw new DataNotAvailableException();
        }
        MovieStreamInfo cached = FFMpegTools.readCachedMovieInfo(infohash, file);
        if (cached != null) {
            return cached;
        }
        Future<MovieStreamInfo> job = null;
        synchronized (currentGetMovieInfoJobs) {
            /*
             * check if it is scheduled previously, if not queue it up
             */
            if (!currentGetMovieInfoJobs.contains(file)) {
                currentGetMovieInfoJobs.add(file);
                job = executors.submit(new Callable<MovieStreamInfo>() {
                    public MovieStreamInfo call() throws Exception {
                        try {
                            return FFMpegTools.createMovieInfo(infohash, file);
                        } finally {
                            synchronized (currentGetMovieInfoJobs) {
                                currentGetMovieInfoJobs.remove(file);
                            }
                        }
                    }
                });
            }
        }
        if (job != null) {
            try {
                return job.get(timeout, unit);
            } catch (Exception e) {
                // ok, didn't complete in time or got some error
                logger.finer("unable to get mediainfo in time: " + e.getMessage());
            }
        }
        throw new DataNotAvailableException();
    }

    /**
     * Tried to get a preview image from a media file
     * 
     * @param infohash
     * @param mediaFile
     * @return
     * @throws TorrentException
     * @throws DataNotAvailableException
     */
    public File getPreviewImage(final byte[] infohash, final File mediaFile, long timeout,
            TimeUnit unit) throws TorrentException, DataNotAvailableException {
        if (mediaFile == null) {
            throw new DataNotAvailableException();
        }
        if (infohash == null) {
            throw new DataNotAvailableException();
        }

        logger.fine("Trying to get preview for: " + mediaFile);
        /*
         * first, check if the file exists
         */
        String pathHash = Integer.toHexString(mediaFile.getPath().hashCode());
        final File imageFile = new File(CoreInterface.getMetaInfoDir(infohash), "preview_"
                + pathHash + ".png");
        if (imageFile.exists()) {
            logger.finer("returning cached file");
            return imageFile;
        }

        /*
         * lets check if ffmpeg can do something about it
         */
        Future<File> job = null;
        synchronized (currentPreviewJobs) {
            /*
             * check if it is scheduled previously, if not queue it up
             */
            if (!currentPreviewJobs.contains(mediaFile)) {
                currentPreviewJobs.add(mediaFile);
                logger.finest("submitting preview job: " + mediaFile);
                final int taskId = BackendTaskManager.get().createTask("Creating preview",
                        new CancellationListener() {
                            public void cancelled(int inID) {
                                // yeah, not actually stopping...
                                BackendTaskManager.get().removeTask(inID);
                            }
                        });
                final BackendTask task = BackendTaskManager.get().getTask(taskId);
                task.setSummary("Creating preview for: " + mediaFile.getName());
                job = executors.submit(new Callable<File>() {
                    public File call() throws Exception {
                        try {
                            task.setProgress("10%");
                            logger.finest("executing preview job: " + mediaFile);
                            createImageFile(infohash, mediaFile, imageFile);
                            logger.finest("preview job completed successfully: " + mediaFile);
                            return imageFile;
                        } finally {
                            synchronized (currentPreviewJobs) {
                                currentPreviewJobs.remove(mediaFile);
                                task.setProgress("100%");
                                BackendTaskManager.get().removeTask(taskId);
                            }

                        }
                    }
                });

            }
        }
        if (job != null) {
            try {
                if (timeout > 0) {
                    return job.get(timeout, unit);
                }
            } catch (Exception e) {
                // ok, didn't complete in time or got some error
                logger.finer("unable to get preview in time: " + e.getMessage());
            }
        }
        throw new DataNotAvailableException();
    }

    private static void createImageFile(byte[] infohash, File mediaFile, File imageFile)
            throws DataNotAvailableException {
        if (FFMpegTools.checkPrevImageGenerationFailed(imageFile)) {
            logger.finer("previous preview generation failed: " + mediaFile);
            throw new DataNotAvailableException();
        }

        /*
         * could be a image extension, try java image io
         */
        InOrderType type = InOrderType.getType(mediaFile.getName());
        if (type != null && type.jwPlayerType.equals("image")) {
            try {
                StreamReader r = new StreamReader(new FileInputStream(mediaFile));
                byte[] image = r.read();
                logger.finer("read image, size=" + image.length);

                // writeFullImage(image, imgFile);
                FFMpegTools
                        .writeTransformedImage(new ByteArrayInputStream(image), imageFile, false);
                return;

            } catch (Exception e) {
                logger.fine("unable to create image from: " + mediaFile + " using java image io");
            }
        }

        /*
         * ok, lets try the id3 tag stuff
         */
        logger.finest("trying to get id3 tag preview: " + mediaFile);
        boolean good = generate_audio_preview(mediaFile, imageFile);
        if (good) {
            logger.finest("got preview from id3 tag: " + mediaFile);
            DownloadManager real_dl = AzureusCoreImpl.getSingleton().getGlobalManager()
                    .getDownloadManager(new HashWrapper(infohash));
            TOTorrent realTorrent = real_dl.getTorrent();
            if (MagicDirectoryManager.generate_audio_info_xml(new File(real_dl.getSaveLocation()
                    .toString()), realTorrent, new File(imageFile.getParentFile(),
                    PreviewImageGenerator.AUDIO_INFO_FILE))) {
                MagicDirectoryManager.bind_audio_xml(real_dl);
            }
            // ok, all good
            return;
        }

        /*
         * no id3, lets try ffmpeg
         */
        try {
            logger.finest("trying to get preview from ffmpeg: " + mediaFile);
            FFMpegTools.createPreviewImage(infohash, mediaFile, imageFile);
            return;
        } catch (FFMpegException e) {
            // ok, got some error...
            logger.finer("unable to create preview with ffmpeg: " + e.getMessage());
        }

        // didn't work :-(
        FFMpegTools.setPrevImageGenerationFailed(imageFile);
        logger.finest("preview generation failed: " + mediaFile);
        throw new DataNotAvailableException();
    }

    private static boolean generate_audio_preview(File largestFile, File imageFile) {
        try {
            AudioFile f = AudioFileIO.read(largestFile);
            logger.finest("read audio file");
            byte[] binaryData = f.getTag().getFirstArtwork().getBinaryData();

            try {
                FFMpegTools.writeTransformedImage(new ByteArrayInputStream(binaryData), imageFile,
                        false);
                logger.fine("wrote preview from " + largestFile.getName() + " to "
                        + imageFile.getAbsolutePath());
                return true;
            } catch (Exception e) {
                logger.warning("error writing out preview: " + e.toString());
                throw e;
            }
        } catch (Exception e) {
            logger.finer("error reading audio tags during preview generation: " + e.toString());
        }

        return false;
    }
}
