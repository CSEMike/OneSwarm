/**
 * SocketCh uses a SocketChannel for communication but appears to implement the same interface as java.net.Socket.
 */
package net.sourceforge.jsocks.socks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SocketCh{
    private static final int BUFFER_SIZE = 1024;
    private SocketChannel channel;
    private InputStream input;

    public SocketCh() {
        try {
            channel = SocketChannel.open();
            createInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    SocketCh(SocketChannel channel) throws IOException {
        this.channel = channel;
        createInputStream();
    }

    public SocketCh(String host, int port) throws UnknownHostException, IOException {
        this(host != null ? new InetSocketAddress(host, port) : new InetSocketAddress(
                InetAddress.getByName(null), port), (SocketAddress) null, true);
    }

    public SocketCh(InetAddress address, int port) throws IOException {
        this(address != null ? new InetSocketAddress(address, port) : null, (SocketAddress) null,
                true);
    }

    public SocketCh(String host, int port, InetAddress localAddr, int localPort)
            throws IOException {
        this(host != null ? new InetSocketAddress(host, port) : new InetSocketAddress(
                InetAddress.getByName(null), port), new InetSocketAddress(localAddr, localPort),
                true);
    }

    public SocketCh(InetAddress address, int port, InetAddress localAddr, int localPort)
            throws IOException {
        this(address != null ? new InetSocketAddress(address, port) : null, new InetSocketAddress(
                localAddr, localPort), true);
    }

    private SocketCh(SocketAddress address, SocketAddress localAddr, boolean stream)
            throws IOException {
        try {
            if (address == null)
                throw new NullPointerException();
            channel = SocketChannel.open(address);

            if (localAddr != null)
                channel.bind(localAddr);

            createInputStream();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SocketChannel getSocketChannel() {
        return channel;
    }

    public InetAddress getInetAddress() {
        return channel.socket().getInetAddress();
    }

    public void close() throws IOException {
        channel.close();
    }

    public int getPort() {
        return channel.socket().getPort();
    }

    public boolean isConnected() {
        return channel.isConnected();
    }

    public void setSoLinger(boolean on, int val) throws SocketException {
        channel.socket().setSoLinger(on, val);
    }

    public int getSoLinger() throws SocketException {
        return channel.socket().getSoLinger();
    }

    public int getSoTimeout() throws SocketException {
        return channel.socket().getSoTimeout();
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        channel.socket().setTcpNoDelay(on);
    }

    public boolean getTcpNoDelay() throws SocketException {
        return channel.socket().getTcpNoDelay();
    }

    public InputStream getInputStream() throws IOException {
        return input;
    }

    public OutputStream getOutputStream() throws IOException {
        return channel.socket().getOutputStream();
    }

    public void setSoTimeout(int timeout) throws SocketException {
        channel.socket().setSoTimeout(timeout);
    }

    public InetAddress getLocalAddress() {
        return channel.socket().getLocalAddress();
    }

    public int getLocalPort() {
        return channel.socket().getLocalPort();
    }

    private void createInputStream() throws IOException {
        input = new ByteBufferCallbackInputStream(BUFFER_SIZE, new FillByteBufferCallback() {
            public int fillByteBuffer(ByteBuffer buf) throws IOException {
                return channel.read(buf);
            }
        });
    }

    public class ByteBufferCallbackInputStream extends InputStream {
        FillByteBufferCallback callback;
        ByteBuffer buf;

        public ByteBufferCallbackInputStream(int size, FillByteBufferCallback callback)
                throws IOException {
            this.callback = callback;
            buf = ByteBuffer.allocate(size);
            buf.limit(0);
        }

        @Override
        public int read() throws IOException {
            return ensureData(true) ? buf.get() : -1;
        }

        @Override
        public int available() {
            try {
                return ensureData(false) ? buf.remaining() : 0;
            } catch (IOException e) {
                return -1;
            }
        }

        private boolean ensureData(boolean blocking) throws IOException {
            if (buf.remaining() < 1) {
                buf.clear();
                int limit = 0;
                if (blocking)
                    while (limit == 0) {
                        limit = callback.fillByteBuffer(buf);
                    }
                else
                    limit = callback.fillByteBuffer(buf);
                if (limit == -1) {
                    buf.limit(0);
                    return false;
                }
                buf.limit(limit);
                buf.position(0);
            }
            return true;
        }
    }

    public interface FillByteBufferCallback {
        /**
         * Fills the buffer passed to it with data to feed the InputStream.
         * Returns the number of bytes actually read, or -1 if the end of stream
         * has been reached.
         * 
         * @param buf
         *            The buffer to fill with data
         * @return The number of bytes read, or -1 for EOS
         * @throws IOException
         *             (Dependent on implementation)
         */
        public int fillByteBuffer(ByteBuffer buf) throws IOException;
    }
}