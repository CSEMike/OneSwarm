package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.jflv.io;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;

public class ByteStreamWriter extends StreamWriter {

    private int pos;
    private boolean open;
    private boolean debug;
    private final byte[] array;
    private final byte[] readArray;

    public ByteStreamWriter(byte[] array, byte[] readArray) {
        super();
        this.array = array;
        this.readArray = readArray;
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

    public int write(ByteBuffer bbuf) {
        int len = bbuf.remaining();

        bbuf.get(array, pos, len);
        pos += len;
        return len;
    }

    public long writeDirect(long startOff, long len) {
        System.arraycopy(readArray, (int) startOff, array, pos, (int) len);
        pos += len;
        return len;
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
