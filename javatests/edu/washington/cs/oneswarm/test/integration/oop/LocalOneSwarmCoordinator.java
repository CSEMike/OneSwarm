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

/** A OneSwarm experimental coordinator that manages a single LocalOneSwarm instance only. */
public class LocalOneSwarmCoordinator extends Thread {

	private static Logger logger = Logger.getLogger(LocalOneSwarmCoordinator.class.getName());

	/** The server socket on which to listen for connections. */
	ServerSocket serverSocket;

	/** Whether this thread is terminating. */
	boolean done = false;

	/** The local OneSwarm instance we are coordinating. */
	private final LocalOneSwarm instance;

	/** The set of pending commands for this instance. */
	private final List<String> pendingCommands =
		Collections.synchronizedList(new ArrayList<String>());

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

	/** Adds a pending command. */
	public void addCommand(String command) {
		pendingCommands.add(command);
	}

	/** Adds several pending commands. */
	public void addCommands(String [] commands){
		synchronized(pendingCommands) {
			for (String cmd : commands) {
				pendingCommands.add(cmd);
			}
		}
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

				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				InputStream in = socket.getInputStream();
				byte [] b = new byte[4096];
				while (in.available() > 0) {
					int c = in.read(b);
					bytes.write(b, 0, c);
				}

				BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes.toByteArray())));
				String lastLine = "";
				while (reader.ready()) {
					String line = reader.readLine();

					try {
						if (line.contains("?port=")) {
							port = Integer.parseInt(line.split("=")[1].split("\\s+")[0]);
						}
					} catch(NumberFormatException e) {
						e.printStackTrace();
					}

					if (line == null){
						break;
					}
					lastLine = line;
				}

				// The last line contains all the form parameters, if included.
				if (lastLine.startsWith("dlsummary=")) {
					logger.info("Got POST from instance " + instance.toString() + " / " + lastLine);
					String [] kvPairs = lastLine.split("&");
					for (String kv : kvPairs) {
						String [] toks = kv.split("=");
						if (toks[0].equals("key")) {
							encodedPublicKey = URLDecoder.decode(toks[1], "UTF-8");
						} else if (toks[0].equals("onlinefriends")) {
							onlineFriendCount = Integer.parseInt(toks[1]);
						} else if (toks[0].equals("friendConnectorAvailable")) {
							friendConnectorAvailable = Boolean.parseBoolean(toks[1]);
						}
					}
				}

				PrintStream out = new PrintStream(socket.getOutputStream());
				out.print("HTTP/1.1 200 OK\r\n\r\n");
				out.print("ok\r\n");
				synchronized(pendingCommands) {
					for (String cmd : pendingCommands) {
						out.println(cmd);
					}
					pendingCommands.clear();
				}
				out.close();

				logger.info("Got ping from local instance: " + socket);
			}
		} catch (IOException e) {
			e.printStackTrace();
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
}
