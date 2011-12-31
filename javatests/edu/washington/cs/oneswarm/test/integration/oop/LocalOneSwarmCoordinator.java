package edu.washington.cs.oneswarm.test.integration.oop;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Assert;

/**
 * A OneSwarm experimental coordinator that manages a single LocalOneSwarm
 * instance only.
 */
public class LocalOneSwarmCoordinator extends Thread {

    private static Logger logger = Logger.getLogger(LocalOneSwarmCoordinator.class.getName());

    /** The server socket on which to listen for connections. */
    ServerSocket serverSocket;

    /** Whether this thread is terminating. */
    boolean done = false;

    /** The local OneSwarm instance we are coordinating. */
    private final LocalOneSwarm instance;

    /** The set of pending commands for this instance. */
    private final List<String> pendingCommands = Collections
            .synchronizedList(new ArrayList<String>());

    /** The number of online friends for this instance. */
    int onlineFriendCount;

    /** The TCP listen port of this client. */
    int port;

    /** The key of this client. */
    String encodedPublicKey;

    /** Is the friend connector of this client available? */
    boolean friendConnectorAvailable = false;

    public LocalOneSwarmCoordinator(LocalOneSwarm instance) throws IOException {

        this.instance = instance;

        // Bind to any free port. Listen on localhost only.
        serverSocket = new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"));

        setDaemon(true);
        setName("LocalOneSwarmCoordinator. Instance: " + instance.getLabel());
    }

    /** Returns the bound local port of this coordinator. */
    public int getServerPort() {
        return serverSocket.getLocalPort();
    }

    /** Marks this thread for termination. */
    public void setDone() {
        done = true;
    }

    /** Adds a pending command. */
    public void addCommand(String command) {
        pendingCommands.add(command);
    }

    /** Adds several pending commands. */
    public void addCommands(String[] commands) {
        synchronized (pendingCommands) {
            for (String cmd : commands) {
                pendingCommands.add(cmd);
            }
        }
    }

    @Override
    public void run() {
        while (!done) {
            Socket socket = null;
            try {
                // Use a timeout so we can detect and cleanup expired threads.
                serverSocket.setSoTimeout(1000);

                // Since this is 1-1 with an instance, we only need to deal with
                // one connection
                // at a time.
                try {
                    socket = serverSocket.accept();
                    socket.setSoTimeout(1000);
                } catch (SocketTimeoutException e) {
                    continue;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Assert.fail();
            }

            try {
                instance.coordinatorReceivedHeartbeat();

                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                InputStream in = socket.getInputStream();
                byte[] b = new byte[4096];

                while (true) {
                    try {
                        int c = in.read(b);
                        if (c == -1) {
                            break;
                        }
                        bytes.write(b, 0, c);
                    } catch (SocketTimeoutException e) {
                        break;
                    }
                }

                logger.finer("Read " + bytes.size() + " bytes from client");
                if (bytes.size() == 0) {
                    continue;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream(bytes.toByteArray())));
                String lastLine = "";
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    if (line.contains("?port=")) {
                        port = Integer.parseInt(line.split("=")[1].split("\\s+")[0]);
                    }
                    lastLine = line;
                }

                // The last line contains all the form parameters, if included.
                logger.fine("Got POST from instance " + instance.toString() + " / " + lastLine);
                String[] kvPairs = lastLine.split("&");
                for (String kv : kvPairs) {
                    String[] toks = kv.split("=");
                    if (toks[0].equals("key")) {
                        encodedPublicKey = URLDecoder.decode(toks[1], "UTF-8");
                    } else if (toks[0].equals("onlinefriends")) {
                        onlineFriendCount = Integer.parseInt(toks[1]);
                    } else if (toks[0].equals("friendConnectorAvailable")) {
                        friendConnectorAvailable = Boolean.parseBoolean(toks[1]);
                    }
                    // TODO(willscott): Make this extensible, so tests can
                    // retrieve arbitrary responses.
                }

                PrintStream out = new PrintStream(socket.getOutputStream());
                out.print("HTTP/1.1 200 OK\r\n\r\n");
                out.print("ok\r\n");
                synchronized (pendingCommands) {
                    for (String cmd : pendingCommands) {
                        out.print(cmd + "\r\n");
                    }
                }
                out.flush();

                logger.fine("Got ping from local instance: " + socket);
                pendingCommands.clear();
            } catch (IOException e) {
                logger.warning("LocalOneSwarm coordinator stopped: " + e.toString());
                e.printStackTrace();
                Assert.fail();
            } finally {
                try {
                    socket.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public int getOnlineFriendCount() {
        return onlineFriendCount;
    }

    public String getEncodedPublicKey() {
        return encodedPublicKey;
    }

    public int getPort() {
        return port;
    }

    public boolean isFriendConnectorAvailable() {
        return friendConnectorAvailable;
    }

    public List<String> getPendingCommands() {
        return new ArrayList<String>(pendingCommands);
    }
}
