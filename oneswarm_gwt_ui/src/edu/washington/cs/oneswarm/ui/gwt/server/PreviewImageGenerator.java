package edu.washington.cs.oneswarm.ui.gwt.server;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentException;
import org.gudy.azureus2.plugins.torrent.TorrentFile;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.google.gwt.dev.util.HttpHeaders;

import edu.washington.cs.oneswarm.ui.gwt.CoreInterface;
import edu.washington.cs.oneswarm.ui.gwt.CoreTools;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.FileTypeFilter;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.ImageConstants;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.FFMpegAsyncOperationManager;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.FFMpegAsyncOperationManager.DataNotAvailableException;

public class PreviewImageGenerator extends javax.servlet.http.HttpServlet {
    public static final int PREVIEW_MAX_WAIT_TIME = 2;

    public static final String AUDIO_INFO_FILE = "audio_info.xml";

    private static final long serialVersionUID = 1L;
    CoreInterface coreInterface = null;
    private static Logger logger = Logger.getLogger(PreviewImageGenerator.class.getName());

    // Map<String, Boolean> preview_fails = Collections.synchronizedMap(new
    // HashMap<String, Boolean>());
    // FileOutputStream preview_fails_file = null;

    public PreviewImageGenerator() {
        logger.info("started preview image generator servlet (PreviewImageGenerator)");

        // load_preview_fails();
    }

    // private void load_preview_fails() {
    // String path = SystemProperties.getMetaInfoPath() + "/preview_fails";
    // try {
    // FileInputStream fis = new FileInputStream(path);
    // BufferedReader in = new BufferedReader(new InputStreamReader(fis));
    // while (in.ready()) {
    // String hash = in.readLine();
    // // System.out.println("preview fails line: " + hash);
    // preview_fails.put(hash, false);
    // }
    // logger.fine("loaded " + preview_fails.size() + " failed hashes");
    // fis.close();
    // } catch (Exception e) {
    // logger.fine("Couldn't load preview image failures: " + e.toString());
    // try {
    // (new File(path)).createNewFile();
    // } catch (IOException e1) {
    // Debug.out("couldn't _create_ preview fails file: ", e1);
    // }
    // }
    //
    // try {
    // preview_fails_file = new FileOutputStream(path, true);
    // } catch (FileNotFoundException e) {
    // logger.fine("couldn't open preview fail file");
    // preview_fails_file = null;
    // }
    //
    // /**
    // * TODO: clean up this file sometimes. if( size > 3000 ) wait 2 minutes
    // * (until all previews requested) and then save all that were touched
    // */
    // }

    Set<String> processingHashes = Collections.synchronizedSet(new HashSet<String>());

    public void doGet(HttpServletRequest request, HttpServletResponse response) {

        if (request.getParameter("path") != null) {
            processArbitraryPathRequest(request, response);
            return;
        }

        logger.finer("preview image generator servlet: get: " + request.getParameter("infohash"));

        String hint = request.getParameter("type");

        if (coreInterface == null)
            coreInterface = new CoreInterface(AzureusCoreImpl.getSingleton().getPluginManager()
                    .getDefaultPluginInterface());

        String hash = null;

        try {
            hash = request.getParameter("infohash");

            /**
             * this is not reentrant w.r.t. the same hash
             */
            synchronized (processingHashes) {
                while (processingHashes.contains(hash)) {
                    Thread.sleep(100);
                }
                processingHashes.add(hash);
            }

            InputStream inputstream;
            File imageFile = getImageFile(hash);
            // if (check_previous_preview_fail(hash) == false) {
            // imageFile = getImageFile(hash);
            // } else {
            // logger.finest("skipping presumed fail preview: " + hash);
            // }
            if (imageFile == null) {
                // check if we have gotten this from a friend
                imageFile = getFromFriendImageFile(hash);
                if (imageFile == null) {
                    logger.finest("get hash from friend: " + hash + " failed");
                    /**
                     * Don't try to generate a preview for this if we don't have
                     * it from a friend -- we don't have any data and the
                     * adapters will crash
                     */
                    if (coreInterface.isF2FHash(hash)) {
                        response.setStatus(302);
                        if (hint != null) {
                            if (hint.equals("audio")) {
                                response.setHeader("Location", ImageConstants.ICON_AUDIO_BROWSER);
                                return;
                            } else if (hint.equals("video")) {
                                response.setHeader("Location", ImageConstants.ICON_VIDEO_BROWSER);
                                return;
                            }
                        }
                        response.setHeader("Location", ImageConstants.ICON_DOCUMENT_CENTER);
                        return;
                    } else {
                        logger.finer(hash + " not f2f, trying preview generation");
                    }
                }
            }
            logger.finest("serving image: "
                    + (imageFile != null ? imageFile.getAbsolutePath() : "(null)") + " / " + hash);
            if (imageFile != null) {
                long last_modified = imageFile.lastModified();
                if (last_modified > 0) {
                    long if_modified = request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
                    if (if_modified > 0 && last_modified / 1000 <= if_modified / 1000) {
                        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                        // ((Request) request).setHandled(true);
                        logger.finest("not modified, " + last_modified);
                        return;
                    }
                    // ok, we have to serve the file, set the header
                    response.setDateHeader(HttpHeaders.LAST_MODIFIED, last_modified);
                    logger.finest("setting last modified: " + last_modified);
                }
                // and cache control
                int secondsToCache = 24 * 60 * 60;
                response.setDateHeader(HttpHeaders.EXPIRES, System.currentTimeMillis()
                        + (secondsToCache * 1000));
                response.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=" + secondsToCache
                        + ", private");

                inputstream = new BufferedInputStream(new FileInputStream(imageFile));
                response.setContentType("image/png");
                response.setStatus(HttpServletResponse.SC_OK);
                ServletOutputStream outputstream = response.getOutputStream();

                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputstream.read(buffer)) > 0) {
                    outputstream.write(buffer, 0, len);
                }
                outputstream.flush();
                outputstream.close();
                inputstream.close();
                // response.getWriter().println("<h1>Hello</h1>");
                // ((Request) request).setHandled(true);
            } else {

                logger.finest("trying to do a redirect instead: ");
                /**
                 * Check for audio types
                 */
                int audio_subfiles = 0, everything_else = 0;
                Download download = coreInterface.getDownload(hash);

                if (download == null) {
                    System.err.println("null download: " + (hash));

                    response.setStatus(302);
                    response.setHeader("Location", ImageConstants.ICON_DOCUMENT_CENTER);

                    return;
                }

                for (TorrentFile file : download.getTorrent().getFiles()) {
                    if (FileTypeFilter.match(file.getName(), FileTypeFilter.Audio)) {
                        audio_subfiles++;
                    } else {
                        everything_else++;
                    }
                }
                logger.finest("audio check : " + audio_subfiles + " / " + everything_else);

                // if (download.isComplete() == true) {
                // record_no_preview(hash);
                // }

                if (audio_subfiles > everything_else) {
                    response.setStatus(302);
                    response.setHeader("Location", ImageConstants.ICON_AUDIO_BROWSER);
                } else {
                    // System.out.println("trying to do a redirect instead: ");
                    response.setStatus(302);
                    if (hint != null) {
                        if (hint.equals("audio")) {
                            response.setHeader("Location", ImageConstants.ICON_AUDIO_BROWSER);
                            return;
                        } else if (hint.equals("video")) {
                            response.setHeader("Location", ImageConstants.ICON_VIDEO_BROWSER);
                            return;
                        }
                    }
                    response.setHeader("Location", ImageConstants.ICON_DOCUMENT_CENTER);
                }
                return;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (hash != null) {
                processingHashes.remove(hash);
            }
        }
    }

    private void processArbitraryPathRequest(HttpServletRequest request,
            HttpServletResponse response) {

        int scale = -1;
        if (request.getParameter("scale") != null) {
            scale = Integer.parseInt(request.getParameter("scale"));
        }

        try {
            response.setContentType("image/png");
            previewImageForArbitraryPath(URLDecoder.decode(request.getParameter("path"), "UTF-8"),
                    scale, response.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    public static void previewImageForArbitraryPath(String path, int scale, OutputStream outStream)
            throws IOException {
        FileInputStream fis = new FileInputStream(path);
        ImageIO.setUseCache(false);
        Image base = ImageIO.read(fis);
        if (base == null) {
            throw new IOException("unable to read image");
        }

        if (scale != -1) {
            double resizeFactorWidth = base.getWidth(null) / (double) scale;
            double resizeFactorHeight = base.getHeight(null) / (double) scale;
            double resizeFactor = Math.max(resizeFactorWidth, resizeFactorHeight);
            base = base.getScaledInstance((int) (base.getWidth(null) / resizeFactor),
                    (int) (base.getHeight(null) / resizeFactor), Image.SCALE_SMOOTH);
        }
        BufferedImage img = new BufferedImage(base.getWidth(null), base.getHeight(null),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graph = img.createGraphics();
        graph.drawImage(base, null, null);
        ImageIO.write(img, "png", outStream);
        img.flush();
        base.flush();
        graph.dispose();
    }

    // private synchronized void record_no_preview(String hash) {
    // if (preview_fails.containsKey(hash)) {
    // return;
    // }
    //
    // preview_fails.put(hash, true);
    //
    // if (preview_fails_file != null) {
    // try {
    // preview_fails_file.write((hash + "\n").getBytes());
    // preview_fails_file.flush();
    // } catch (IOException e) {
    // e.printStackTrace();
    // preview_fails_file = null;
    // }
    // } else {
    // Debug.out("preview fails file is null!");
    // }
    // }

    // private synchronized boolean check_previous_preview_fail(String hash) {
    // if (preview_fails.containsKey(hash)) {
    // preview_fails.put(hash, true);
    // return true;
    // }
    // return false;
    // }

    private File getFromFriendImageFile(String torrentID) throws TorrentException {
        torrentID = torrentID.substring(OneSwarmConstants.BITTORRENT_MAGNET_PREFIX.length());
        byte[] torrentHash = Base32.decode(torrentID);
        File metainfoDir = CoreInterface.getMetaInfoDir(torrentHash);

        File imageFile = new File(metainfoDir, "preview_friend.png");
        try {
            logger.finest("checking: " + imageFile.getCanonicalPath());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (imageFile.exists()) {
            return imageFile;
        } else {
            return null;
        }
    }

    public File getImageFile(String inInfohash) {

        Download download = coreInterface.getDownload(inInfohash);
        if (download == null) {
            return null;
        }
        if (!download.isComplete()) {
            return null;
        }

        try {
            Torrent torrent = download.getTorrent();

            TorrentFile activeFile = CoreTools.getBiggestPreviewableFile(download);

            if (activeFile == null) {
                logger.finest("no file found for " + inInfohash);
                return null;
            }
            DiskManagerFileInfo fileInfo = CoreTools.getDiskManagerFileInfo(activeFile, download);
            if (fileInfo == null) {
                logger.finer("no disk manager file info: " + inInfohash);
                return null;
            }
            try {
                return FFMpegAsyncOperationManager.getInstance().getPreviewImage(torrent.getHash(),
                        fileInfo.getFile(), PREVIEW_MAX_WAIT_TIME, TimeUnit.SECONDS);
            } catch (DataNotAvailableException e) {
                return null;
            }
        } catch (TorrentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    // private boolean checkPrevImageGenerationFailed(byte[] hash) {
    // String encoded = ByteFormatter.encodeString(hash);
    // if (preview_fails.containsKey(encoded)) {
    // preview_fails.put(encoded, true);
    // return true;
    // }
    // return false;
    // }

    // public static boolean createImageFile(Download download,
    // DiskManagerFileInfo fileInfo, File imageFile) throws FFMpegException,
    // IOException {
    // // ok, no file, lets create it if the torrent is downloaded
    //
    // boolean completed = false;
    //
    // // only cache "failures" if all files are completed
    // try {
    // if (download.isComplete(true)) {
    // completed = true;
    // }
    //
    // InOrderType type = InOrderType.getType(fileInfo.getFile().getName());
    // if (completed && type.type.equals(FileTypeFilter.Audio)) {
    // boolean good =
    // MagicDirectoryManager.generate_audio_preview(fileInfo.getFile(),
    // imageFile);
    // if (!good) {
    // FFMpegTools.setPrevImageGenerationFailed(imageFile);
    // } else {
    // logger.fine("generated audio preview: " + download.getName());
    // DownloadManager real_dl =
    // AzureusCoreImpl.getSingleton().getGlobalManager().getDownloadManager(new
    // HashWrapper(download.getTorrent().getHash()));
    // TOTorrent realTorrent = real_dl.getTorrent();
    // if (MagicDirectoryManager.generate_audio_info_xml(new
    // File(download.getSavePath()), realTorrent, new
    // File(imageFile.getParentFile(), AUDIO_INFO_FILE))) {
    // MagicDirectoryManager.bind_audio_xml(real_dl);
    // }
    // }
    // logger.finest("trying to generate on-the-fly preview for audio: " +
    // fileInfo.getFile().getName() + " good? " + good);
    // return good;
    // }
    //
    // if (completed) {
    // FFMpegTools.createImageFile(fileInfo.getFile(), imageFile, completed);
    //
    // if (imageFile.exists()) {
    // logger.fine("created image file: " + imageFile.getCanonicalPath());
    // return true;
    // } else {
    // if (completed) {
    // FFMpegTools.setPrevImageGenerationFailed(imageFile);
    // }
    // logger.fine("Image file creation failed: " +
    // imageFile.getCanonicalPath());
    // }
    //
    // }
    // } catch( Exception e ) {
    // e.printStackTrace();
    // }
    //
    // return false;
    // }

    // private Map<String, String> parseRequestString(String target) {
    // Map<String, String> map = new HashMap<String, String>();
    //
    // // remove the initial slash
    // if (target.length() > 0 &&
    // target.startsWith(OneSwarmConstants.videoImagePath)) {
    // target = target.substring(OneSwarmConstants.videoImagePath.length());
    // }
    // String[] split = target.split("/");
    // if (split.length > 0) {
    // String torrentID = split[0];
    // map.put(OneSwarmConstants.WEB_PARAM_TORRENT_ID, torrentID);
    //
    // if (split.length > 1) {
    // String path = target.substring((torrentID + "/").length());
    // map.put(OneSwarmConstants.WEB_PARAM_VIDEO_PATH, path);
    // }
    // }
    // return map;
    //
    // }
}
