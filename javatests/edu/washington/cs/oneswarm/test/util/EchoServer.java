package edu.washington.cs.oneswarm.test.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

public class EchoServer implements Runnable {

    static class EchoServerHandler implements Runnable {
        private final Socket client;

        public EchoServerHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            InputStream in = null;
            OutputStream out = null;
            try {
                byte[] buffer = new byte[1024];
                in = client.getInputStream();
                out = client.getOutputStream();

                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * @param args
     * @throws IOException
     * @throws UnknownHostException
     */
    public static void main(String[] args) throws UnknownHostException, IOException {
        Thread server = new Thread(new EchoServer(12345));
        server.setDaemon(true);
        server.start();
        Socket socket = new Socket("127.0.0.1", 12345);
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();

        byte[] payload = ("fdilshfnmnoi2j0rfoisdnf wkqejh n2uirh . msdf sdlkfj sd l\n lkjdflksjd lksdjf h "
                + "   0pi23 rwe09uqhjw9 aueoih2q039puwjgreuigfdjs ghskjh fdsgjkh092qugjesi"
                + "lkgjhwo ero iuqwe0oiurewgi heror08 egrwiweib¨ h2qbriu wefiwb").getBytes();
        byte[] returned = new byte[payload.length];

        out.write(payload);

        int read = 0;
        while (read < payload.length) {
            read += in.read(returned, read, payload.length - read);
            System.out.println("read: " + read);
        }

        assert (Arrays.equals(payload, returned));
    }

    private final int port;
    private final Semaphore started = new Semaphore(0);

    public EchoServer(int port) {
        this.port = port;
    }

    public void waitForStart() throws InterruptedException {
        started.acquire();
        started.release();
    }

    @Override
    public void run() {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(port);
            started.release();
            while (true) {
                Socket client = ss.accept();
                System.out.println("connection from: " + client.getRemoteSocketAddress());
                Thread t = new Thread(new EchoServerHandler(client));
                t.setDaemon(true);
                t.setName("EchoServer_" + port);
                t.start();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public void startDeamonThread(boolean blockUntilStarted) throws InterruptedException {
        Thread echoServerThread = new Thread(this);
        echoServerThread.setName("Echo server");
        echoServerThread.setDaemon(true);
        echoServerThread.start();
        if (blockUntilStarted) {
            waitForStart();
        }
    }
}
