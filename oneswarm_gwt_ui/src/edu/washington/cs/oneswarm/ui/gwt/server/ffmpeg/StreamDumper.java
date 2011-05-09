package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class dumps everything coming from an inputstream source (think pipe to
 * dev null)
 * 
 * @author isdal
 * 
 */

class StreamDumper implements Runnable {

    private InputStream source;

    private boolean print;

    private final StringBuffer store = new StringBuffer();
    private final int bytesToStore;

    public StreamDumper(InputStream source, int bytesToStore, boolean print) {

        this.source = source;
        this.print = print;
        this.bytesToStore = bytesToStore;

        Thread t = new Thread(this);
        t.setName("BufferDumper");
        t.setDaemon(true);
        t.start();
    }

    public void run() {
        try {
            int read = 0;
            long totalRead = 0;
            byte[] buffer = new byte[FFMpegWrapper.CHUNK_SIZE];
            while ((read = source.read(buffer, 0, buffer.length)) != -1) {
                if (print) {
                    System.out.println(new String(buffer, 0, read));
                }
                if (totalRead < bytesToStore) {
                    store.append(new String(buffer, 0, read));
                }
                totalRead += read;

            }
            source.close();
        } catch (IOException e) {
            System.out.println("Buffer dumper stopped");
        }
    }

    public String getStoredOutput() {
        return store.toString();
    }
}