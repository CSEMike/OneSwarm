package edu.washington.cs.oneswarm.f2ftest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.aelitis.azureus.core.networkmanager.impl.osssl.OneSwarmSslKeyManager;
import com.aelitis.azureus.core.networkmanager.impl.osssl.OneSwarmSslTools;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHandshake;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;

public class SSLClient {

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            new SSLClient();
        } catch (KeyManagementException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeyStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public SSLClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
            CertificateException, UnrecoverableKeyException, IOException, InterruptedException {
        int port = 12345;// 34722;
        String hostname = "127.0.0.1";
        // String hostname = "84.55.67.11";

        // Create SSL context.
        SSLContext sslcontext = OneSwarmSslKeyManager.getInstance().getSSLContext();

        SSLSocketFactory socketFactory = sslcontext.getSocketFactory();

        try {

            // Socket socket = new Socket(hostname, port);
            Socket socket = socketFactory.createSocket(hostname, port);
            OutputStream out = socket.getOutputStream();

            ByteBuffer bb = ByteBuffer.allocate(40);
            bb.put((byte) OSF2FHandshake.ONESWARM_PROTOCOL.length());
            bb.put(OSF2FMessage.ONESWARM_PROTOCOL.getBytes());
            bb.put(OSF2FHandshake.OS_FLAGS);
            bb.flip();
            byte[] b = new byte[bb.remaining()];
            bb.get(b);
            System.out.println(OneSwarmSslTools.bytesToHex(b));
            // out.write(OSF2FMessage.ONESWARM_PROTOCOL.length());
            // out.write(OSF2FMessage.ONESWARM_PROTOCOL.getBytes());
            out.write(b);
            out.flush();
            // for (int i = 0; i < 100; i++) {
            // out.write((i + "bajs\n").getBytes());
            // Thread.sleep(100);
            // }
            out.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("client: server stream ended");

            Thread.sleep(5000);
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
