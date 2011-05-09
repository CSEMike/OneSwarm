package edu.washington.cs.oneswarm.ui.gwt.server.handlers;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.mortbay.io.Buffer;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.MimeTypes;

public class SharedFileHandler {
    private static Logger logger = Logger.getLogger(SharedFileHandler.class.getName());
    private final DiskManagerFileInfo fileInfo;
    private final Download download;

    public SharedFileHandler(DiskManagerFileInfo fileInfo, Download download) {
        this.fileInfo = fileInfo;
        this.download = download;
    }

    public void process(HttpServletResponse response, HttpServletRequest request)
            throws IOException {
        StringBuilder headers = new StringBuilder();
        Enumeration headerNamesEnum = request.getHeaderNames();
        while (headerNamesEnum.hasMoreElements()) {
            String header = (String) headerNamesEnum.nextElement();
            headers.append(header + "=" + request.getHeader(header) + "\t");
        }
        logger.finest("incoming headers: " + headers.toString());
        boolean downloadCompleted = fileInfo.getDownloaded() == fileInfo.getLength()
                && fileInfo.getFile().exists();

        MimeTypes m = new MimeTypes();
        Buffer mimebuffer = m.getMimeByExtension(fileInfo.getFile().getName());
        String mime = "application/octet-stream";
        if (mimebuffer != null) {
            mime = new String(mimebuffer.asArray());
        }

        if (downloadCompleted) {
            createNewFileSender(response, mime, 0);
        } else {
            InputStream sourceStream = download.getStats().getFileStream(fileInfo,
                    getBitRate(request));
            createNewStreamSender(response, sourceStream, mime);
        }
    }

    private int getBitRate(HttpServletRequest request) {
        if (isRemoteAccess(request)) {
            /*
             * if it is remote access, just blast at full speed (1Gbit/s)
             */
            return (1000 * 1024 * 1024) / 8;
        } else {
            /*
             * else, keep it at 10 Mbit/s to decrease lag
             */
            return (10 * 1024 * 1024) / 8;
        }
    }

    public static boolean isRemoteAccess(HttpServletRequest request) {
        return request.getUserPrincipal() == null;
    }

    private void createNewFileSender(HttpServletResponse response, String mime, long startAtPos)
            throws IOException {
        logger.fine("sending completed file");
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileInfo.getFile()));
        long skipped = 0;
        if (startAtPos > fileInfo.getLength()) {
            throw new IOException("seek attempted to pos>file size");
        }
        while (skipped < startAtPos) {
            skipped += in.skip(startAtPos);
            logger.finer("seeking in completed file, pos= " + skipped + " target=" + startAtPos);
        }
        if (startAtPos == 0) {
            response.setContentType(mime);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Content-Disposition", "filename=\"" + fileInfo.getFile().getName()
                    + "\"");
            setContentLength(response, fileInfo.getLength());
        }
        ServletOutputStream responseStream = response.getOutputStream();

        int len = 0;
        while ((len = in.read(buffer)) != -1 && !quit) {
            responseStream.write(buffer, 0, len);
        }
        responseStream.close();
        in.close();
    }

    private boolean quit = false;

    private void createNewStreamSender(HttpServletResponse response, InputStream sourceStream,
            String mime) throws IOException {

        logger.fine("sending running running download");
        response.setContentType(mime);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Content-Disposition", "filename=\"" + fileInfo.getFile().getName()
                + "\"");
        setContentLength(response, fileInfo.getLength());

        ServletOutputStream responseStream = response.getOutputStream();
        int len = 0;
        long pos = 0;
        try {
            while ((len = sourceStream.read(buffer)) != -1 && !quit) {
                responseStream.write(buffer, 0, len);
                pos += len;
            }
        } catch (IOException e) {
            /*
             * the file might have completed, the exception was thrown because
             * the download stopped in that case, read the real file instead of
             * trying to read it through the disk manager
             */
            if (fileInfo.getDownloaded() == fileInfo.getLength()) {
                createNewFileSender(response, mime, pos);
                return;
            } else {
                throw e;
            }
        }
        responseStream.close();
        sourceStream.close();
    }

    private byte[] buffer = new byte[1024];

    public static void setContentLength(HttpServletResponse response, final double length) {
        if (length < Integer.MAX_VALUE) {
            int contentLengthGuess = (int) Math.round(length);
            response.setContentLength(contentLengthGuess);
        } else {
            response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(Math.round(length)));
        }

    }
}
