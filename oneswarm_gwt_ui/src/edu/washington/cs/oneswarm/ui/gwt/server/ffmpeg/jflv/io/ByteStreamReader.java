package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io;

import java.io.FileNotFoundException;

public class ByteStreamReader extends StreamReader {

    final byte[] array;
    int pos;
    boolean open = false;
    boolean debug = true;

    public ByteStreamReader(byte[] array) {
        super();
        this.array = array;
    }

    public byte[] read(int len) throws EoflvException {
        if (pos + len > array.length) {
            close();
            throw new EoflvException();
        }
        byte[] ret = new byte[len];
        System.arraycopy(array, pos, ret, 0, len);
        pos += len;
        // System.out.println("read " + len + " bytes, pos=" + pos);
        return ret;
    }

    public void open() throws FileNotFoundException {
        open = true;
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
    }

    public void skip(int len) throws EoflvException {
        if (pos + len > array.length) {
            close();
            throw new EoflvException();
        }
        pos += len;
    }

    public long getPos() {
        return pos;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
