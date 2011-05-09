package edu.washington.cs.oneswarm.f2ftest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Iterator;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import com.aelitis.azureus.core.networkmanager.impl.osssl.OneSwarmSslTools;
import com.aelitis.azureus.core.networkmanager.impl.osssl.OneSwarmSslTransportHelperFilterStream;
import com.aelitis.azureus.core.networkmanager.impl.osssl.OneSwarmSslTransportHelperFilterStream.SslHandShakeMatch;

public class SSLServer {
    private final static int BUFFER_SIZE = 32;

    private final SSLEngine sslEngine;
    private final SSLContext sslContext;
    ByteBuffer inputBuffer;

    private int bytes_read = 0;
    private boolean verifiedSSL = false;

    /**
     * @param args
     */
    public static void main(String[] args) {

        try {
            new SSLServer();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeyManagementException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeyStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public SSLServer() throws IOException, KeyManagementException, NoSuchAlgorithmException,
            KeyStoreException, UnrecoverableKeyException, CertificateException,
            InterruptedException {
        int count = 0;
        inputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        // sslContext = OSKeyManager.getInstance().getSSLContext();
        // sslEngine = sslContext.createSSLEngine();

        ServerSocketChannel server;
        server = ServerSocketChannel.open();

        server.configureBlocking(false);
        server.socket().bind(new InetSocketAddress(8765));

        Selector selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            selector.select();

            Set<SelectionKey> keys = selector.selectedKeys();

            Iterator<SelectionKey> i = keys.iterator();
            while (i.hasNext()) {
                SelectionKey key = i.next();
                i.remove();

                if (key.isAcceptable()) {
                    SocketChannel client = server.accept();
                    client.configureBlocking(false);
                    System.out.println("got connection from: "
                            + client.socket().getInetAddress().getHostAddress());
                    client.register(selector, SelectionKey.OP_READ);
                    // client.register(selector, SelectionKey.OP_WRITE);

                    continue;
                }

                if (key.isReadable()) {
                    SocketChannel client = (SocketChannel) key.channel();

                    try {
                        bytes_read += client.read(inputBuffer);
                        System.out.println("pos: " + bytes_read);
                    } catch (IOException e) {
                        // client died
                        System.out.println("client died");
                        break;
                    }

                    if (bytes_read >= OneSwarmSslTools.SSL_HEADER_MIN_LENGTH) {
                        inputBuffer.flip();
                        byte[] data = new byte[inputBuffer.remaining()];

                        inputBuffer.get(data, 0, inputBuffer.remaining());
                        if (!verifiedSSL) {
                            verifiedSSL = true;
                            if (OneSwarmSslTransportHelperFilterStream.isSSLClientHello(data) == SslHandShakeMatch.SSL_CLIENT_CERT) {
                                System.out.println("SSL client-hello msg");
                            }
                        }
                        System.out.println(new String(data) + "\t"
                                + OneSwarmSslTools.bytesToHex(data));
                        inputBuffer.flip();
                    }
                    continue;
                }

                if (key.isWritable()) {
                    SocketChannel client = (SocketChannel) key.channel();

                    ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
                    buf.put(("sending stuff" + count++ + "\n").getBytes());
                    buf.flip();
                    try {
                        client.write(buf);
                    } catch (IOException e) {
                        // client died
                        System.out.println("client died");
                        break;
                    }
                    if (count == 10000) {
                        count = 0;
                        client.close();
                    }
                    continue;
                }

                // we should never get here...
                assert true : "key not handeled: " + key.toString();
            }
        }
    }

    private void doHandShake(SocketChannel client) {

    }
}
