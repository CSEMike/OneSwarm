package edu.washington.cs.oneswarm.f2f.dht;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;

public class CHTClientUDP implements CHTClientInterface {

	private static final int RECEIVE_TIME_OUT = 4000;
	private final static int MAX_QUEUE_LENGTH = 200;
	private final int serverPort;
	private final String server;
	private final Queue<CHTGetJob> jobs = new LinkedList<CHTGetJob>();
	private Thread jobExecutorThread;

	public CHTClientUDP(String server, int serverPort) throws UnknownHostException {
		this.server = server;
		this.serverPort = serverPort;
	}

	@Override
	public void put(byte[] key, byte[] value) throws IOException {
		if (key == null || key.length != 20) {
			throw new RuntimeException("Key length must be 20");
		}
		InetAddress serverAddr = InetAddress.getByName(server);
		DatagramSocket s = new DatagramSocket();
		byte[] payload = new byte[1 + 20 + value.length];
		payload[0] = 0;
		System.arraycopy(key, 0, payload, 1, key.length);
		System.arraycopy(value, 0, payload, 21, value.length);
		DatagramPacket p = new DatagramPacket(payload, payload.length, serverAddr, serverPort);
		s.send(p);
		s.close();
	}

	@Override
	public void get(final byte[] key, final CHTCallback callback) {
		if (key == null || key.length != 20) {
			throw new RuntimeException("Key length must be 20");
		}
		synchronized (jobs) {
			if (jobs.size() > MAX_QUEUE_LENGTH) {
				callback.errorReceived(new Exception("max queue length reached"));
			} else {
				// System.out.println("adding job to queue, size=" +
				// jobs.size());
				jobs.add(new CHTGetJob(key, callback));

				if (jobExecutorThread == null) {
					jobExecutorThread = new Thread(new Runnable() {
						@Override
						public void run() {
							// System.out.println("starting new thread");
							DatagramSocket s = null;
							try {
								s = new DatagramSocket();
								s.setSoTimeout(RECEIVE_TIME_OUT);
								CHTGetJob job;
								do {
									/*
									 * grab the first element
									 */
									synchronized (jobs) {
										job = jobs.poll();
										// System.out.println(
										// "grabbing job from queue, size=" +
										// jobs.size());
										if (job == null) {
											jobExecutorThread = null;
										}
									}

									if (job != null) {
										try {
											job.execute(s);
										} catch (Throwable t) {
											job.cb.errorReceived(t);
										}
									}

								} while (job != null);
								// System.out.println("Thread completed");
							} catch (Throwable t) {
								t.printStackTrace();
							} finally {
								if (s != null) {
									try {
										s.close();
									} catch (Throwable t) {

									}
								}
							}
						}
					});
					jobExecutorThread.setDaemon(true);
					jobExecutorThread.setName("CHT client");
					jobExecutorThread.start();
				}
			}
		}

	}

	class CHTGetJob {
		private final byte[] key;
		private final CHTCallback cb;

		public CHTGetJob(final byte[] key, final CHTCallback callback) {
			this.key = key;
			this.cb = callback;
		}

		public void execute(DatagramSocket s) throws IOException {
			long startTime = System.currentTimeMillis();

			InetAddress serverAddr = InetAddress.getByName(server);
			byte[] payload = new byte[1 + 20];
			payload[0] = 1;
			System.arraycopy(key, 0, payload, 1, key.length);
			DatagramPacket p = new DatagramPacket(payload, payload.length, serverAddr, serverPort);
			s.send(p);
			byte[] incomingBuffer = new byte[1500];
			DatagramPacket incomingPacket = new DatagramPacket(incomingBuffer, incomingBuffer.length);
			s.receive(incomingPacket);
			if (incomingPacket.getLength() > 0) {
				byte[] value = new byte[incomingPacket.getLength()];
				System.arraycopy(incomingPacket.getData(), 0, value, 0, value.length);
				cb.valueReceived(key, value);
			} else {
				cb.errorReceived(new Exception("Key not in CHT"));
			}
			/*
			 * rate limit the jobs, they need to take at least 200ms to avoid
			 * getting dropped at the server
			 */
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
			}
		}
	}
}
