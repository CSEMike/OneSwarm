package edu.washington.cs.oneswarm.ui.gwt.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.TorrentException;
import org.gudy.azureus2.plugins.torrent.TorrentFile;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.ui.gwt.CoreInterface;
import edu.washington.cs.oneswarm.ui.gwt.CoreTools;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants.InOrderType;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.FFMpegWrapper;
import edu.washington.cs.oneswarm.ui.gwt.server.handlers.BrowseHandler;

public class FlashVideoConverterPlayer extends javax.servlet.http.HttpServlet {
    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private static Logger logger = Logger.getLogger(FlashVideoConverterPlayer.class.getName());
    CoreInterface coreInterface = null;
    private final boolean remoteAccess;

    public FlashVideoConverterPlayer() {
        this(false);
    }

    public FlashVideoConverterPlayer(boolean remoteAccess) {
        this.remoteAccess = remoteAccess;

        logger.fine("started flash converter/player servlet");
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) {

        logger.finer("flash video converter / player get()");

        if (coreInterface == null)
            coreInterface = new CoreInterface(AzureusCoreImpl.getSingleton().getPluginManager()
                    .getDefaultPluginInterface());

        // if (target.startsWith(OneSwarmConstants.videoPath)) {

        // Map<String, String> parameters = parseRequestString(target);

        // Map<String, String> parameters = request.getParameterMap();
        String torrentID = request.getParameter(OneSwarmConstants.WEB_PARAM_TORRENT_ID);
        String target = request.getRequestURL().toString();
        logger.fine("got request: " + target);
        String requestUrl = target.substring(target.indexOf("/flv_movie"));
        // logger.finest("torrent=" + torrentID);

        Map parameters = request.getParameterMap();
        for (Object key : parameters.keySet()) {
            logger.finest("" + key + ": " + request.getParameter((String) key));
        }

        String file = request.getParameter(OneSwarmConstants.WEB_PARAM_VIDEO_PATH);

        try {
            if (file != null) {
                file = URLDecoder.decode(file, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        logger.finest("file=" + file);

        Download download;
        try {
            download = BrowseHandler.getTorrent(requestUrl);
            logger.finer("torrent found");
            try {
                TorrentFile activeFile = BrowseHandler.getTorrentFile(requestUrl, download);
                logger.finer("file found: " + activeFile.getName());
                DiskManagerFileInfo diskManagerFile = CoreTools.getDiskManagerFileInfo(activeFile,
                        download);

                /*
                 * check if we need to seek
                 */
                double startAtByte = 0;
                String start = request.getParameter("start");
                if (start != null) {
                    startAtByte = Double.parseDouble(start);
                }

                FFMpegWrapper ffmpeg = new FFMpegWrapper(coreInterface, diskManagerFile, download,
                        remoteAccess, startAtByte);
                ffmpeg.process(response, request);

            } catch (IOException e1) {
                logger.finest("unable to get file in torrent:" + e1.getMessage());
                handlePlayListRequest(download, torrentID, request, response);

            }
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (TorrentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void handlePlayListRequest(Download d, String torrent, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        StringBuilder b = new StringBuilder();
        logger.fine("playlist request");

        b.append("<playlist version='1' xmlns='http://xspf.org/ns/0/'>\n");
        b.append("\t<title>Playlist for '" + d.getName() + "'</title>\n");

        b.append("\t<tracklist>\n");
        TorrentFile[] files = d.getTorrent().getFiles();
        DiskManagerFileInfo[] fileinfo = d.getDiskManagerFileInfo();
        for (int i = 0; i < fileinfo.length; i++) {
            // don't play skipped files
            InOrderType type = InOrderType.getType(files[i].getName());
            if (!fileinfo[i].isSkipped() && type != null) {
                b.append(getTrack(files[i], d, type));
                logger.finer("appending to tracklist: " + files[i].getName());
            }
        }

        final String footer1 = "\t</tracklist>\n";
        final String footer2 = "</playlist>\n";
        b.append(footer1);
        b.append(footer2);
        byte[] bytes = b.toString().getBytes("UTF-8");
        OutputStream respStream = response.getOutputStream();
        response.setContentType("application/xml");
        response.setCharacterEncoding("UTF-8");
        response.setContentLength(bytes.length);

        respStream.write(bytes);
        respStream.close();
    }

    private String getTrack(TorrentFile fi, Download d, InOrderType type) {
        final String trackHeader = "\t\t<track>\n";
        StringBuilder b = new StringBuilder();
        b.append(trackHeader);
        b.append("\t\t\t<title>" + fi.getName() + "</title>\n");
        String urlEncodedFileName = null;
        try {
            // we need to url encode properly here since the flv player won't
            // handle UFT-8 URL encoding properly
            urlEncodedFileName = URLEncoder.encode(fi.getName(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }

        String location = "/oneswarmgwt/flv_movie/" + Base32.encode(d.getTorrent().getHash()) + "/"
                + urlEncodedFileName;
        boolean debug = false;// COConfigurationManager.getBooleanParameter("oneswarm.beta.updates");
        if (debug) {
            /*
             * playing around with seeking, doesn't work half the time....
             */
            // if (type.convertNeeded && d.isComplete()) {
            // // location += ".flv";
            // b.append("\t\t\t<meta rel='streamer'>" + "lighttpd" +
            // "</meta>\n");
            // } else if (InOrderType.FLV.equals(type) && d.isComplete()) {
            // b.append("\t\t\t<meta rel='streamer'>" + "lighttpd" +
            // "</meta>\n");
            // } else {
            b.append("\t\t\t<meta rel='type'>" + type.jwPlayerType + "</meta>\n");
            // }
        } else {
            b.append("\t\t\t<meta rel='type'>" + type.jwPlayerType + "</meta>\n");
        }
        b.append("\t\t\t<location>" + location + "</location>\n");

        final String trackFooter = "\t\t</track>\n";
        b.append(trackFooter);
        return b.toString();
    }
    // private Map<String, String> parseRequestString(String target) {
    //
    // Map<String, String> map = new HashMap<String, String>();
    //
    // // remove the initial slash
    // if (target.length() > OneSwarmConstants.videoPath.length()) {
    // target = target.substring(OneSwarmConstants.videoPath.length() + 1,
    // target.length());
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
    // }

}
