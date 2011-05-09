/**
 * 
 */
package edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg;

import java.util.concurrent.ArrayBlockingQueue;

class ByteStreamBuffer {

    private ArrayBlockingQueue<byte[]> buffer;

    private final int maxElementSize;

    // private volatile int currentBufferSize = 0;

    private Object writeLock = new Object();

    private Object readLock = new Object();

    public ByteStreamBuffer(int bufferSize, int chunkSize) {
        this.maxElementSize = chunkSize;
        int queueLength = bufferSize / maxElementSize + 1;
        buffer = new ArrayBlockingQueue<byte[]>(queueLength);
    }

    public int read(byte[] buf) throws InterruptedException {
        synchronized (readLock) {

            if (buffer.peek() != null) {
                if (buf.length < buffer.peek().length) {
                    throw new RuntimeException("reading less than allowed");
                }
            }

            // this will block if the buffer is empty
            byte[] read = buffer.take();
            // currentBufferSize -= read.length;
            System.arraycopy(read, 0, buf, 0, read.length);
            // System.out.println("read from buffer (" + read.length + "/"
            // + (maxElementSize * buffer.size()) + ")");
            return read.length;
        }
    }

    public void write(byte[] buf, int len) throws InterruptedException {
        this.write(buf, len, true);
    }

    private void write(byte[] buf, int len, boolean copy) throws InterruptedException {
        if (buf == null) {
            throw new NullPointerException();
        }
        if (len < 0 || len > buf.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        synchronized (writeLock) {

            // check if we are writing more than maxEmementSize
            if (len > maxElementSize) {
                int pos = 0;

                while (pos < len) {

                    int bytesLeft = len - pos;
                    int bytesToCopy = Math.min(bytesLeft, maxElementSize);
                    // System.out.println("pos=" + pos + " len=" + len
                    // + " bytesLeft=" + bytesLeft + " bytesToCopy="
                    // + bytesToCopy);
                    byte[] tempBuf = new byte[bytesToCopy];
                    System.arraycopy(buf, pos, tempBuf, 0, tempBuf.length);
                    this.write(tempBuf, tempBuf.length, false);
                    pos += bytesToCopy;
                }
            } else {

                // check if we need to copy the byte array
                if (copy || buf.length != len) {
                    // ok, copy the content and put in the queue
                    byte[] tempBuf = new byte[len];
                    System.arraycopy(buf, 0, tempBuf, 0, len);

                    // this will block if the queue is full
                    buffer.put(tempBuf);
                } else {
                    buffer.put(buf);
                }
                // currentBufferSize += len;

                // System.out.println("wrote to buffer (" + len + "/"
                // + (maxElementSize * buffer.size()) + ")");
            }
        }
    }

    public boolean isEmpty() {
        return buffer.isEmpty();
    }
}