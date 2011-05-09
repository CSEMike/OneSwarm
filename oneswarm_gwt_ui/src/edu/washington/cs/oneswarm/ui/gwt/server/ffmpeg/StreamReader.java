package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * This class reads everything that comes from an inputstream source, and makes
 * is available as either a byte[] calls to the read funtions are blocking,
 * waiting for a read form the inputstream to return -1
 * 
 * @author isdal
 * 
 */

class StreamReader implements Runnable {

    private InputStream source;

    private Thread t;

    private ArrayList<byte[]> data;
    private final int max_size;

    /**
     * create a stream reader with default max size
     * 
     * @param source
     */
    public StreamReader(InputStream source) {
        this(source, 20 * 1024 * 1024);
    }

    public StreamReader(InputStream source, int max_size) {
        this.data = new ArrayList<byte[]>();
        this.max_size = max_size;
        this.source = source;

        t = new Thread(this);
        t.setName("BufferReader");
        t.setDaemon(true);
        t.start();
    }

    public void run() {
        try {
            int totalRead = 0;
            int read = 0;
            byte[] buffer = new byte[FFMpegWrapper.CHUNK_SIZE];
            while ((read = source.read(buffer, 0, buffer.length)) != -1) {
                totalRead += read;
                if (totalRead > max_size) {
                    throw new IOException("reading more than max size");
                }
                byte[] toSave = new byte[read];
                System.arraycopy(buffer, 0, toSave, 0, toSave.length);
                data.add(toSave);
            }
            source.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Buffer reader stopped");
        }
    }

    public byte[] read() throws InterruptedException {

        t.join();

        byte[] allData = new byte[this.length()];
        int pos = 0;
        for (int i = 0; i < data.size(); i++) {
            byte[] b = data.get(i);
            System.arraycopy(b, 0, allData, pos, b.length);
            pos += b.length;
        }
        return allData;
    }

    public int length() {
        int len = 0;

        for (byte[] element : data) {
            len += element.length;
        }
        return len;
    }
}