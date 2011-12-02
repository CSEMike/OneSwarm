package edu.washington.cs.oneswarm.test.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

/**
 * Listens for incoming data and stores the last received data. Doesn't do anything
 * with this data but provides methods to verify that it was received.
 */
public class TestReceivedServer implements Runnable {
    public static Logger logger = Logger.getLogger(TestReceivedServer.class.getName());

    class BasicServerHandler implements Runnable {
        private final Socket client;
        private TestReceivedServer server;

        public BasicServerHandler(Socket client, TestReceivedServer server) {
            this.client = client;
            this.server = server;
        }

        @Override
        public void run() {
            InputStream in = null;
            try {
            	byte[] allData = null;
                byte[] buffer = new byte[1024];
                in = client.getInputStream();

                int read;
                
                while ((read = in.read(buffer)) != -1) {
                	appendData(buffer, read);
                    logger.finest("read " + read + " bytes");
                }
                server.setData(allData);
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
            }
        }
        
        // Append the first length bytes to allData
        private void appendData(byte[] bytes, int length) {
        	if (data == null) {
        		data = Arrays.copyOf(bytes, length);
        	} else {
        		int oldLen = data.length;
        		data = Arrays.copyOf(data, oldLen + bytes.length);
        		for (int i = 0; i < bytes.length; i++) {
        			data[i + oldLen] = bytes[i];
        		}
        	}
        }
        
    }

    
    private int port;
    private byte[] data;
    private final Semaphore started = new Semaphore(0);

    public TestReceivedServer(int port) {
        this.port = port;
        data = null;
    }

    public int waitForStart() throws InterruptedException {
        started.acquire();
        started.release();
        return port;
    }
    
    public boolean hasData() {
		// This should use a lock if server will actually be used with multiple clients
    	// simultaneously, not just one client for testing
    	return data != null;
    }
    
    public void clearData() {
		// This should use a lock if server will actually be used with multiple clients
    	// simultaneously, not just one client for testing
    	data = null;
    }
    
    public byte[] getLatestData() {
		// This should use a lock if server will actually be used with multiple clients
    	// simultaneously, not just one client for testing
    	return data;
    }
    
    private void setData(byte[] bytes) {
		// This should use a lock if server will actually be used with multiple clients
    	// simultaneously, not just one client for testing
    	data = bytes;
    }

    @Override
    public void run() {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(port);
            // If port 0 is specified the server will bind to a free port
            // when that is the case, update the port field so be accurate.
            this.port = ss.getLocalPort();
            started.release();
            while (true) {
                Socket client = ss.accept();
                logger.info("connection from: " + client.getRemoteSocketAddress());
                Thread t = new Thread(new BasicServerHandler(client, this));
                t.setDaemon(true);
                t.setName("TestReceivedServer_" + port);
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Thread startDaemonThread(boolean blockUntilStarted) throws InterruptedException {
        Thread serverThread = new Thread(this);
        serverThread.setName("Listen server");
        serverThread.setDaemon(true);
        serverThread.start();
        if (blockUntilStarted) {
            waitForStart();
        }
        return serverThread;
    }
}
