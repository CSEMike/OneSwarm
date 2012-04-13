package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import net.sourceforge.jsocks.socks.InetRange;
import net.sourceforge.jsocks.socks.server.IdentAuthenticator;

public class ProxyServer implements Runnable {
    public static Logger logger = Logger.getLogger(ProxyServer.class.getName());

    /**
     * @param args
     * @throws IOException
     * @throws UnknownHostException
     */
    public static void main(String[] args) throws UnknownHostException, IOException {
        Thread server = new Thread(new ProxyServer(12345));
        server.setDaemon(true);
        server.start();
        Socket socket = new Socket("127.0.0.1", 12345);
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();

        byte[] payload = ("fdilshfnmnoi2j0rfoisdnf wkqejh n2uirh . msdf sdlkfj sd l\n lkjdflksjd lksdjf h "
                + "   0pi23 rwe09uqhjw9 aueoih2q039puwjgreuigfdjs ghskjh fdsgjkh092qugjesi"
                + "lkgjhwo ero iuqwe0oiurewgi heror08 egrwiweib� h2qbriu wefiwb").getBytes();
        byte[] returned = new byte[payload.length];

        out.write(payload);

        int read = 0;
        while (read < payload.length) {
            read += in.read(returned, read, payload.length - read);
            System.out.println("read: " + read);
        }
    }

    private final int port;
    private final Semaphore started = new Semaphore(0);

    public ProxyServer(int port) {
        this.port = port;
    }

    public int waitForStart() throws InterruptedException {
        started.acquire();
        started.release();
        return port;
    }

    @Override
    public void run() {
        IdentAuthenticator localAuth = new net.sourceforge.jsocks.socks.server.IdentAuthenticator();
        InetRange hostRange = new net.sourceforge.jsocks.socks.InetRange();
        hostRange.add("127.0.0.1");
        localAuth.add(hostRange, null);
        net.sourceforge.jsocks.socks.ProxyServer server = new net.sourceforge.jsocks.socks.ProxyServer(
                localAuth);
        started.release();
        server.start(port);
    }

    public Thread startDeamonThread(boolean blockUntilStarted) throws InterruptedException {
        Thread t = new Thread(this);
        t.setName("Socks server");
        t.setDaemon(true);
        t.start();
        if (blockUntilStarted) {
            waitForStart();
        }
        return t;
    }
}
