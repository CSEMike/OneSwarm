package edu.washington.cs.oneswarm.ui.gwt.server.handlers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.torrent.TorrentFile;
import org.mortbay.jetty.servlet.ServletHandler;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.ui.gwt.CoreInterface;
import edu.washington.cs.oneswarm.ui.gwt.CoreTools;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;

public class BrowseHandler extends ServletHandler {
    private static Logger logger = Logger.getLogger(BrowseHandler.class.getName());

    private final static String BROWSE = OneSwarmConstants.BROWSE_SHARE_PATH;
    private final static String DOWNLOAD = OneSwarmConstants.DOWNLOAD_SHARE_PATH;

    public BrowseHandler() {

    }

    public void handle(String target, HttpServletRequest request, HttpServletResponse response,
            int dispatch) throws IOException, ServletException {
        if (target.startsWith(BROWSE)) {
            handleBrowse(target, request, response, dispatch);
        } else if (target.startsWith(DOWNLOAD)) {
            handleDownload(target, request, response, dispatch);
        } else {
            throw new IOException("not browse share path: " + target);
        }
    }

    public void handleBrowse(String target, HttpServletRequest request,
            HttpServletResponse response, int dispatch) throws IOException, ServletException {
        String path = target.substring(BROWSE.length());
        logger.finest("got browse request for: " + path);
        response.setCharacterEncoding("UTF-8");
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(),
                "UTF-8"));
        response.setContentType("text/html");
        if (path.equals("") || path.equals("/")) {
            /* return root */
            List<DownloadManager> dms = AzureusCoreImpl.getSingleton().getGlobalManager()
                    .getDownloadManagers();
            writeHeader("List of shared swarms", out);
            out.append("<body>\n");
            out.append("<table border='0'>\n");
            out.append("<tr><th>Swarm name</th><th>Swarm size</th></tr>\n");
            for (DownloadManager dm : dms) {
                try {
                    out.append("\t<tr>\n");

                    if (dm.getTorrent().getFiles().length == 1) {
                        out.append("\t\t<td><a href="
                                + OneSwarmConstants.DOWNLOAD_SHARE_PATH
                                + "/"
                                + Base32.encode(dm.getTorrent().getHash())
                                + "/"
                                + URLEncoder.encode(
                                        dm.getTorrent().getFiles()[0].getRelativePath(), "UTF-8")
                                + ">" + dm.getDisplayName() + "</a><td>\n");
                    } else {
                        out.append("\t\t<td><a href=" + OneSwarmConstants.BROWSE_SHARE_PATH + "/"
                                + Base32.encode(dm.getTorrent().getHash()) + "/" + ">"
                                + dm.getDisplayName() + "</a><td>\n");
                    }

                    out.append("\t\t<td>" + dm.getSize() + "</td>\n");
                    out.append("\t</tr>\n");
                } catch (TOTorrentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            out.append("</table>");
            out.append("</body>");
            out.close();

        } else {
            try {
                Download torrent = getTorrent(target);

                writeHeader(torrent.getName(), out);
                out.append("<body>\n");
                out.append("<h2>" + torrent.getName() + "</h2>\n");
                out.append("<table border='0'>\n");
                out.append("<tr><th>File name</th><th>File size</th></tr>\n");
                for (TorrentFile file : torrent.getTorrent().getFiles()) {
                    out.append("\t<tr>\n");
                    out.append("\t\t<td><a href=" + OneSwarmConstants.DOWNLOAD_SHARE_PATH + "/"
                            + Base32.encode(torrent.getTorrent().getHash()) + "/"
                            + URLEncoder.encode(file.getName(), "UTF-8") + ">" + file.getName()
                            + "</a><td>\n");
                    out.append("\t\t<td>" + file.getSize() + "</td>\n");
                    out.append("\t</tr>\n");
                }
                out.append("</table>\n");
                out.append("</body>\n");
                out.close();
            } catch (IOException e) {
                // expected sometimes, just go back to
                writeHeader("error", out);
                out.append("<body>");
                out.append(e.getMessage());
                out.append("</body>");
                out.close();
            }
        }

    }

    private static void writeHeader(String title, BufferedWriter out) throws IOException {
        out.append("<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN'\n");
        out.append("'http://www.w3.org/TR/html4/loose.dtd'>\n");
        out.append("<html>\n");
        out.append("<head>");
        out.append("<meta http-equiv='Content-Type' content='text/html'; charset='UTF-8'>\n");
        out.append("<title>" + title + "</title>\n");
        out.append("</head>\n\n");
    }

    public void handleDownload(String target, HttpServletRequest request,
            HttpServletResponse response, int dispatch) throws IOException, ServletException {
        Download download = getTorrent(target);
        DiskManagerFileInfo diskManagerFile = CoreTools.getDiskManagerFileInfo(
                getTorrentFile(target, download), download);
        new SharedFileHandler(diskManagerFile, download).process(response, request);
    }

    public static Download getTorrent(String target) throws IOException {
        logger.finest("locating torrent file for target=" + target);
        if (target.startsWith("/")) {
            target = target.substring(1);
        }
        if (target.endsWith("/")) {
            target = target.substring(0, target.length() - 1);
        }

        String[] split = target.split("/");
        if (split.length > 1) {
            logger.finest("torrent=" + split[1]);
            byte[] torrentHash = Base32.decode(split[1]);
            try {
                CoreInterface coreInterface = new CoreInterface(AzureusCoreImpl.getSingleton()
                        .getPluginManager().getDefaultPluginInterface());
                Download d = coreInterface.getDownloadManager().getDownload(torrentHash);
                if (d == null) {
                    throw new IOException("Torrent not found: " + split[1]);
                }
                return d;
            } catch (DownloadException e) {
                throw new IOException("Download problem: " + e.getMessage());
            }
        } else {
            throw new IOException("Invalid path");
        }
    }

    public static TorrentFile getTorrentFile(String target, Download download) throws IOException {
        logger.finest("target=" + target);
        for (int i = 0; i < 3; i++) {
            if (target.contains("/")) {
                target = target.substring(target.indexOf("/") + 1);
            } else {
                throw new IOException("unable to get path");
            }
        }
        String file = target;
        if (file == null) {
            throw new IOException("unable to get file target");
        }

        try {
            file = URLDecoder.decode(file, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IOException("URL decode problem");
        }
        logger.finest("file=" + file);

        if (download == null) {
            throw new IOException("torrent not found");
        }

        TorrentFile activeFile = CoreTools.getTorrentFile(download, file);
        if (activeFile == null) {
            throw new IOException("file not found in torrent");
        }
        return activeFile;
    }
}
