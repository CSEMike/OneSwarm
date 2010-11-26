package edu.washington.cs.oneswarm.test.integration.util;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

/** A OneSwarm experimental coordinator that manages a single LocalOneSwarm instance only. */
class LocalOneSwarmCoordinator extends Thread {

	private static Logger logger = Logger.getLogger(LocalOneSwarmCoordinator.class.getName());

	/** The server socket on which to listen for connections. */
	ServerSocket serverSocket;

	/** Whether this thread is terminating. */
	boolean done = false;

	/** The local OneSwarm instance we are coordinating. */
	private final LocalOneSwarm instance;

	public LocalOneSwarmCoordinator(LocalOneSwarm instance) throws IOException {

		this.instance = instance;

		// Bind to any free port. Listen on localhost only.
		serverSocket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
	}

	/** Returns the bound local port of this coordinator. */
	public int getServerPort() {
		return serverSocket.getLocalPort();
	}

	/** Marks this thread for termination. */
	public void setDone() {
		done = true;
	}

	@Override
	public void run() {
		try {
			while (!done) {

				// Use a timeout so we can detect and cleanup expired threads.
				serverSocket.setSoTimeout(1000);

				// Since this is 1-1 with an instance, we only need to deal with one connection
				// at a time.
				Socket socket = null;
				try {
					socket = serverSocket.accept();
				} catch(SocketTimeoutException e) {
					continue;
				}

				instance.coordinatorReceivedHeartbeat();

				// TODO(piatek): Expand LocalOneSwarm to actually track state
				// and send commands here. For now, simply give an ok.

				PrintStream out = new PrintStream(socket.getOutputStream());
				out.print("HTTP/1.1 200 OK\r\n\r\n");
				out.print("ok\r\n");
				out.close();

				logger.info("Got ping from local instance: " + socket);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
