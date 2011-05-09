package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.ByteStreamWriter;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io.IOHelper;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata.EmbeddedData;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata.FlvHeader;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata.MetaDataGen;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.metadata.TagBroker;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.parse.ParseMeta;

/**
 * This class moves data from one InputStream to an OutputStream at a rate (int
 * Bytes/s) specified by dataRate, and also keeps the last position read less
 * than what is specified by the DownloadPosition supplied
 */
class FlvOutputBufferManager implements Runnable {
    private static Logger logger = Logger.getLogger(FlvOutputBufferManager.class.getName());

    private BufferedInputStream source;

    private OutputStream dest;

    private double dataRate;

    private volatile boolean quit = false;

    // private final int bufferSize = 1024 * 64;

    private final int bufferSize = 5487;

    private long total = 0;

    private final MovieStreamInfo movieInfo;

    private final double audiorate;
    private final double videorate;
    private final double startAtSecond;
    /*
     * limit the copy rate to make sure that ffmpeg doesn't use all available
     * CPU, but don't limit to much (audio can go faster than real time)
     */
    private final static long MIN_STREAM_RATE = 500 * 1000;

    public FlvOutputBufferManager(InputStream source, OutputStream dest, double audiorate,
            double videorate, boolean start, MovieStreamInfo movieInfo, double startAtSecond) {
        this.source = new BufferedInputStream(source);
        this.dest = dest;
        this.dataRate = Math.max((videorate + audiorate) / 8, MIN_STREAM_RATE);
        this.videorate = videorate;
        this.audiorate = audiorate;
        this.startAtSecond = startAtSecond;
        this.movieInfo = movieInfo;
        if (start) {
            Thread t = new Thread(this);

            t.setName("BufferHandler");
            t.setDaemon(true);
            t.start();
        }
    }

    public boolean isRunning() {
        return !quit;
    }

    public void quit() {
        this.quit = true;
    }

    private void handleMetaInfo() throws IOException {
        logger.fine("injecting metainfo");
        byte[] header = new byte[100 * 1000];
        byte[] newHeader = new byte[header.length * 2];
        int pos = 0;
        int len = 0;
        while (pos < header.length && (len = source.read(header, pos, header.length - pos)) != -1) {
            pos += len;
            logger.finest("done reading " + pos + " bytes from ffmpeg");
        }
        logger.finer("done reading " + pos + " bytes from ffmpeg");
        if (pos < header.length) {
            logger.finer("small file, no metadata inject. Writing " + pos + " bytes to web");
            dest.write(header, 0, pos);
            return;
        }
        /*
         * create the io helper that will read the buffer
         */
        IOHelper ioh = new IOHelper(header, newHeader);
        FlvHeader flvh = new FlvHeader(ioh);

        /*
         * read the original meta data
         */
        ParseMeta parm = new ParseMeta(ioh);
        parm.findMetaTag();
        HashMap<String, Object> originalMetaData = parm.getMetaData();
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("\nBEGIN original meta data:\n"
                    + EmbeddedData.prettyPrintData(originalMetaData) + "\nEND orginal meta data");
        }
        TagBroker tb = new TagBroker(ioh, flvh);

        /*
         * parse the first X MB and generate metainfo for that
         */
        logger.fine("parsing flv video files, generating meta data");
        MetaDataGen mdg = new MetaDataGen(tb, flvh);
        mdg.buildOnLastSecond();
        mdg.buildOnMetaData();
        logger.finer("first video tag:" + tb.getFirstVideoTag());
        logger.finer("last video tag:" + tb.getLastVideoTag());
        logger.finer("video res: " + tb.getVideoHeight() + "x" + tb.getVideoWidth());
        if (tb.getFirstVideoTag() != -1 && tb.getLastVideoTag() != -1 && tb.getVideoHeight() > 0) {
            /*
             * replace the meta info with the information for the entire file +
             * spoof key frames so we can support seeking
             */
            logger.fine("modifying metadata, adding keyframes");
            double audioRateKBitPerS = audiorate / 1000.0;
            double videoRateKbitPerS = videorate / 1000.0;
            mdg.spoofMetaData(startAtSecond, movieInfo.getDuration(), audioRateKBitPerS,
                    videoRateKbitPerS);
            mdg.sealMetaData(false, true);
        } else {
            /*
             * no video, just add the duration header and leave it at that...
             */
            originalMetaData.put("duration", new Double(movieInfo.getDuration()));
            cleanMap(originalMetaData);
            mdg.getMetaData().setData(originalMetaData);
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("\nBEGIN modified meta data:\n"
                        + EmbeddedData.prettyPrintData(originalMetaData)
                        + "\nEND modified meta data");
            }
            // skip the keep frame rate check, othervise it won't write out the
            // meta info when there are no key frames
            boolean keepFrameRate = false;
            mdg.sealMetaData(false, keepFrameRate);
        }

        /*
         * copy the data into the new header
         */
        tb.writeTags();

        /*
         * write the new header to the stream
         */
        // new header len is just the position in the stream
        int newHeaderLen = (int) ioh.getOutStream().getPos();

        dest.write(newHeader, 0, newHeaderLen);
        logger.finer("wrote " + newHeaderLen + " bytes of new header");

        /*
         * write the rest of the old header
         */
        // get the position after the last succesfully parsed tag
        int oldHeaderLastParsedByte = (int) tb.getLastTabReadAttemptPos() - 1;
        int oldHeaderRest = header.length - oldHeaderLastParsedByte;
        dest.write(header, oldHeaderLastParsedByte, oldHeaderRest);
        logger.finer("wrote " + oldHeaderRest + " bytes remaining in old header");

    }

    private static void cleanMap(HashMap<String, Object> map) {
        for (Iterator<String> iterator = map.keySet().iterator(); iterator.hasNext();) {
            String key = iterator.next();
            if (map.get(key) == null) {
                logger.finer("removing null value for key '" + key + "'");
                iterator.remove();
            }
        }
    }

    public void run() {
        if (dest == null) {
            return;
        }

        long startTime = System.currentTimeMillis();
        // buffer at most STREAM_BUFFER bytes of data
        byte[] buffer = new byte[bufferSize];

        try {

            /*
             * ffmpeg has some issues with adding the proper flv headers, lets
             * add some ourselves
             */
            handleMetaInfo();

            int len;
            while (!quit && (len = source.read(buffer)) != -1) {
                total += len;

                dest.write(buffer, 0, len);
                dest.flush();
                logger.finest("wrote " + len + " bytes (" + total + " total)");
                double converted = total / dataRate;
                long timeUsed = System.currentTimeMillis() - startTime;

                int diff = (int) Math.round(1000.0 * converted - timeUsed);

                long sleepTime = diff - FFMpegWrapper.ENCODE_BUFFER;
                if (sleepTime > 0) {
                    logger.finest("sleeping: " + sleepTime);
                    Thread.sleep(sleepTime);
                }
            }
        } catch (org.mortbay.jetty.EofException e) {
            logger.fine("connection closed");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            this.quit = true;
        }
    }

    public long getTotal() {
        return total;
    }

    // will check if the DURATION_BYTE_MATCH exists in the byte[] supplied and
    // return the position of the first byte after the duration tag, or -1 if
    // the pattern wasn't found
    // private int matchesDuration(byte[] buffer, int bufferLength) {
    // // System.out.println("checking " + bufferLength + " bytes starting at"
    // // + total);
    // // ok, merge this with the last buffer (in case the duration field is
    // // split between the last two reads
    // byte[] mergeBuffer = new byte[oldBuffer.length + bufferLength];
    // System.out.println("old buffer length=" + oldBuffer.length);
    // System.arraycopy(oldBuffer, 0, mergeBuffer, 0, oldBuffer.length);
    // System.arraycopy(buffer, 0, mergeBuffer, oldBuffer.length, bufferLength);
    //
    // for (int positionInBuffer = 0; positionInBuffer < mergeBuffer.length;
    // positionInBuffer++) {
    // byte b = mergeBuffer[positionInBuffer];
    //
    // if (b == DURATION_BYTE_MATCH[positioninPattern]) {
    // // ok, this byte matches as well
    // positioninPattern++;
    // System.out.println("found match " + positioninPattern + " on pos " +
    // positionInBuffer);
    // if (positioninPattern == DURATION_BYTE_MATCH.length) {
    // this.durationFieldSet = true;
    // int durationPosition = (positionInBuffer + 1 - oldBuffer.length);
    // System.out.println("found duration field, ends at byte: " +
    // durationPosition + "(" + Integer.toHexString(durationPosition) +
    // " hex) (" + Integer.toHexString((int) (durationPosition + total)) +
    // " in file)");
    // // for (int j = durationPosition; j < durationPosition + 8;
    // // j++) {
    // // System.out.println(j + "\t"
    // // + Integer.toHexString(buffer[j]));
    // // }
    //
    // return durationPosition;
    // }
    //
    // } else {
    // // in theory this could be the first byte, check that
    // positioninPattern = 0;
    // if (b == DURATION_BYTE_MATCH[0]) {
    // positioninPattern = 1;
    // }
    // // System.out.println("no match " + positionInBuffer);
    // }
    //
    // }
    //
    // // copy the buffer so we can use it next iteration
    // oldBuffer = new byte[bufferLength];
    // System.arraycopy(buffer, 0, oldBuffer, 0, oldBuffer.length);
    // return -1;
    // }

    // private void insertDuration(byte[] buffer, int position, double
    // movieLength) {
    // if (movieLength > 0) {
    // try {
    // ByteArrayOutputStream b = new ByteArrayOutputStream();
    // DataOutputStream d = new DataOutputStream(b);
    //
    // d.writeDouble(movieLength);
    //
    // d.flush();
    // byte[] bytes = b.toByteArray();
    // for (int i = 0; i < bytes.length; i++) {
    // byte c = bytes[i];
    //
    // buffer[position + i] = c;
    // System.out.println("inserting: " + Integer.toHexString(c) + " at " +
    // Integer.toHexString(position + i));
    // }
    //
    // } catch (IOException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // } else {
    // System.out.println("movie duration not known");
    // }
    // }
}