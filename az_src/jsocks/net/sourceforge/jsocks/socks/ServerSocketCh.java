/**
 * SocketCh uses a SocketChannel for communication but appears to implement the same interface as java.net.ServerSocket.
 */

package net.sourceforge.jsocks.socks;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;

public class ServerSocketCh{
    private ServerSocketChannel channel;

    public ServerSocketCh() throws IOException {
        channel = ServerSocketChannel.open();
    }

    public ServerSocketCh(int port) throws IOException {
        this(port, 50, null);
    }

    public ServerSocketCh(int port, int backlog) throws IOException {
        this(port, backlog, null);
    }

    public ServerSocketCh(int port, int backlog, InetAddress bindAddr)
            throws IOException {
        channel = ServerSocketChannel.open();

        if (port < 0 || port > 0xFFFF)
            throw new IllegalArgumentException("Port value out of range: " + port);
        if (backlog < 1)
            backlog = 50;
        try {
            bind(new InetSocketAddress(bindAddr, port), backlog);
            channel.configureBlocking(true);
        } catch (SecurityException e) {
            close();
            throw e;
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    public void close() throws IOException {
        channel.close();
    }

    public ServerSocketChannel getServerSocketChannel() {
        return channel;
    }

    public void bind(SocketAddress local, int backlog) throws IOException {
        channel.bind(local, backlog);
    }

    public InetAddress getInetAddress() {
        return channel.socket().getInetAddress();
    }

    public int getLocalPort() {
        return channel.socket().getLocalPort();
    }

    public SocketCh accept() throws IOException {
        return new SocketCh(channel.accept());
    }

    public void setSoTimeout(int timeout) throws SocketException {
        channel.socket().setSoTimeout(timeout);
    }
}
