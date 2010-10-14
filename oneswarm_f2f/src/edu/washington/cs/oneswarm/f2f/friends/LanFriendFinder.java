package edu.washington.cs.oneswarm.f2f.friends;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.Debug;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.FriendInvitation;
import edu.washington.cs.oneswarm.f2f.OSF2FMain;
import edu.washington.cs.oneswarm.f2f.dht.DHTConnector;
import edu.washington.cs.oneswarm.f2f.invitations.InvitationManager;
import edu.washington.cs.oneswarm.f2f.xml.OSF2FXMLBeanReader;

public class LanFriendFinder {
	private static final String OSF2F_LAN_FRIEND_FINDER = "OSF2F.LanFriendFinder";

	private final static Logger logger = Logger.getLogger(LanFriendFinder.class.getName());

	private final static String MULTICAST_GROUP = "239.255.17.17";
	private final static int FRIEND_FINDER_PORT = 44811;

	private static final int REANNOUNCE_PERIOD = 5 * 1000;
	private static final int TRY_ALL_INTERFACES_EVERY_X_MINUTES = 5;
	private long lastAllInterfaceSend = 0;
	// remove friends from discovered set if we miss 3 packets
	private static final int FRIEND_EXPIRED_TIME = 3 * REANNOUNCE_PERIOD + 1000;

	private static final int MAX_FRIEND_CONNECT_FREQ = 30 * 1000;

	private HashMap<Integer, FriendFinderFriend> discoveredFriends = new HashMap<Integer, FriendFinderFriend>();
	private final DHTConnector friendConnector;
	private final FriendManager friendManager;
	private FriendFinderFriend me;
	private List<MulticastSocket> sendSockets = new LinkedList<MulticastSocket>();
	private FriendFinderAnnounceTask announceTask;

	private volatile boolean running = false;

	private Timer friendFinderAnnouncer;
	private FriendFinderListener friendFinderListener;
	private Thread friendFinderListenerThread;
	private final byte[] ownPublicKey;
	private final InvitationManager invitationManager;

	public LanFriendFinder(final DHTConnector friendConnector, final FriendManager friendManager, InvitationManager invitationManager, final byte[] ownPublicKey) throws IOException {
		this.friendConnector = friendConnector;
		this.friendManager = friendManager;
		this.ownPublicKey = ownPublicKey;
		this.invitationManager = invitationManager;

		COConfigurationManager.addParameterListener(OSF2F_LAN_FRIEND_FINDER, new ParameterListener() {
			public void parameterChanged(String parameterName) {
				boolean enabled = isLanFinderEnabled();
				if (enabled) {
					if (!running) {
						start();
					}
				} else {
					stop();
				}
			}
		});

		if (isLanFinderEnabled()) {
			start();
		}
	}

	public void start() {
		logger.fine("starting lan friend finder");
		running = true;
		announceTask = new FriendFinderAnnounceTask();
		friendFinderAnnouncer = new Timer("LANFriendFinderAnnouncer", true);
		friendFinderAnnouncer.schedule(announceTask, 0, REANNOUNCE_PERIOD);

		friendFinderListener = new FriendFinderListener();
		friendFinderListenerThread = new Thread(friendFinderListener);
		friendFinderListenerThread.setDaemon(true);
		friendFinderListenerThread.setName("LanFriendFinderListener");
		friendFinderListenerThread.start();
	}

	public void stop() {
		logger.fine("stoping lan friend finder");
		if (announceTask != null) {
			announceTask.cancel();
		}

		if (friendFinderAnnouncer != null) {
			friendFinderAnnouncer.cancel();
		}
		if (friendFinderListener != null) {
			friendFinderListener.quit();
		}
		if (friendFinderListenerThread != null) {
			try {
				friendFinderListenerThread.interrupt();
			} catch (Throwable t) {
			}
		}
		running = false;
	}

	class FriendFinderAnnounceTask extends TimerTask {
		@Override
		public void run() {

			synchronized (discoveredFriends) {
				for (Iterator<FriendFinderFriend> iterator = discoveredFriends.values().iterator(); iterator.hasNext();) {
					FriendFinderFriend fff = (FriendFinderFriend) iterator.next();
					if (fff.isExpired()) {
						logger.finest("removing: " + fff);
						iterator.remove();
					}
				}
			}
			if (isLanFinderEnabled()) {
				logger.fine("Running LanFriendFinder");
				try {
					/*
					 * wait for the filelist
					 */
					OSF2FMain.getSingelton().getOverlayManager().getFilelistManager().waitForFileListCreation();

					long timeSinceLastAllIfaceSend = System.currentTimeMillis() - lastAllInterfaceSend;

					if (timeSinceLastAllIfaceSend > TRY_ALL_INTERFACES_EVERY_X_MINUTES * 60 * 1000) {
						lastAllInterfaceSend = System.currentTimeMillis();
						tryAllInterfaces(ownPublicKey);
					} else {
						DatagramPacket p = createPacket(ownPublicKey);
						// send it on all send sockets
						for (Iterator<MulticastSocket> iterator = sendSockets.iterator(); iterator.hasNext();) {
							MulticastSocket s = iterator.next();
							try {
								s.send(p);
								try {
									logger.finest("sent fff packet on: " + s.getNetworkInterface().getName());
								} catch (Throwable t) {
									logger.finest("sent fff packet on: " + s.getLocalSocketAddress());
								}
							} catch (Throwable t) {
								logger.fine("unable to send LanFriendFinder packet on " + s.getLocalAddress() + ", got error: " + t.getMessage());
								/*
								 * if we can't use the socket, close it and
								 * remove it
								 */
								try {
									s.close();
									iterator.remove();
								} catch (Exception e) {
								}

							}
						}
					}

				} catch (Exception e) {
					Debug.out("got error in LanFriendFinder-SendTimer,canceling future lan announces");
					e.printStackTrace();
					friendFinderAnnouncer.cancel();
				}
			} else {
				logger.fine("Not Running LanFriendFinder, disabled in config");
			}
		}
	}

	private void tryAllInterfaces(final byte[] ownPublicKey) {

		logger.finer("probing for new network interfaces to send lan friend finder packets on");
		for (MulticastSocket s : sendSockets) {
			try {
				logger.finer("closing lan friend finder socket on: " + s.getInetAddress().getHostAddress());
				s.close();
			} catch (Exception e) {
			}
		}
		sendSockets.clear();
		try {
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			DatagramPacket p = createPacket(ownPublicKey);
			for (NetworkInterface netIf : Collections.list(nets)) {
				MulticastSocket s = null;
				try {
					s = new MulticastSocket();
					s.setNetworkInterface(netIf);
					s.setTimeToLive(1);
					s.send(p);
					logger.finest("sent fff packet on: " + netIf.getName());
					sendSockets.add(s);
				} catch (Throwable t) {
					logger.finest("unable to send LanFriendFinder packet on " + netIf.getName() + ", got: " + t.getMessage());
					/*
					 * if we can't use the socket, close it
					 */
					if (s != null) {
						try {
							s.close();
						} catch (Exception e) {
						}
					}
				}
			}
			/*
			 * if no socket worked
			 */
			if (sendSockets.size() == 0) {
				Debug.out("unable to bind the Lan Friend Finder sockets to any specific interfaces, trying default interface instead");
				MulticastSocket s = null;
				try {
					s = new MulticastSocket();
					s.setTimeToLive(1);
					s.send(p);
					logger.finest("sent fff packet on default interface");
					sendSockets.add(s);
				} catch (Throwable t) {
					Debug.out("unable to send LanFriendFinder packet on any interface, got: " + t.getMessage());
					t.printStackTrace();
					/*
					 * if we can't use the socket, close it
					 */
					if (s != null) {
						try {
							s.close();
						} catch (Exception e) {
						}
					}
				}

			}

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		logger.finer("Got " + sendSockets.size() + " usable interfaces for LanFriendFinder");

	}

	private DatagramPacket createPacket(final byte[] ownPublicKey) throws Exception, IOException, UnknownHostException {
		FriendFinderFriend me = new FriendFinderFriend(null, getComputerName(), getTcpPort(), ownPublicKey, getSecurityLevel());
		byte[] serialized = me.serialize();
		DatagramPacket p = new DatagramPacket(serialized, serialized.length, InetAddress.getByName(MULTICAST_GROUP), FRIEND_FINDER_PORT);
		return p;
	}

	private byte getSecurityLevel() {
		int level = COConfigurationManager.getIntParameter("security level");
		return (byte) level;
	}

	private String getComputerName() {
		try {
			String name = COConfigurationManager.getStringParameter("Computer Name", null);
			if (name == null) {
				InetAddress addr = InetAddress.getLocalHost();

				// Get hostname
				name = addr.getHostName();
				if (name != null && name.split("\\.").length > 0) {
					name = name.split("\\.")[0];
				}
				COConfigurationManager.setParameter("Computer Name", name);
			}
			return name;
		} catch (UnknownHostException e) {
		}
		return null;
	}

	private int getTcpPort() {
		return COConfigurationManager.getIntParameter("TCP.Listen.Port");
	}

	private boolean isLanFinderEnabled() {
		return COConfigurationManager.getBooleanParameter(OSF2F_LAN_FRIEND_FINDER);
	}

	private static class FriendFinderFriend {
		private static final int MAX_NICK_BYTES = 512;
		private static final int MAX_PUBKEY_BYTES = 512;
		InetAddress addr;
		int port;
		byte[] publicKey;
		String nickName;
		private final long createdDate;
		private final int hash;
		private final byte securityLevel;

		public int getSecurityLevel() {
			return securityLevel;
		}

		public FriendFinderFriend(InetAddress addr, String nickName, int port, byte[] publicKey, byte securityLevel) throws Exception {
			this.createdDate = System.currentTimeMillis();
			this.addr = addr;
			this.nickName = nickName;
			this.port = port;
			this.publicKey = publicKey;
			this.securityLevel = securityLevel;
			if (nickName.getBytes("UTF-8").length > MAX_NICK_BYTES) {
				throw new Exception("nick length must be less than " + MAX_NICK_BYTES + " bytes");
			}
			if (publicKey.length > MAX_PUBKEY_BYTES) {
				throw new Exception("publickey length must be less than " + MAX_PUBKEY_BYTES + " bytes");
			}
			this.hash = Arrays.hashCode(publicKey);
		}

		public FriendFinderFriend(DatagramPacket p) throws IOException {
			this.createdDate = System.currentTimeMillis();
			this.addr = p.getAddress();

			DataInputStream in = new DataInputStream(new ByteArrayInputStream(p.getData()));
			this.port = in.readInt();
			int nickLen = in.readInt();
			if (nickLen > MAX_NICK_BYTES) {
				throw new IOException("Unable to parse nickname in friend finder packet (nick len to large)");
			}
			byte[] nick = new byte[nickLen];
			in.read(nick, 0, nick.length);
			this.nickName = new String(nick);

			int pubKeyLen = in.readInt();
			if (pubKeyLen > MAX_PUBKEY_BYTES) {
				throw new IOException("Unable to parse publickey in friend finder packet (len to large)");
			}
			this.publicKey = new byte[pubKeyLen];
			in.read(publicKey, 0, publicKey.length);

			this.hash = Arrays.hashCode(publicKey);
			/*
			 * read the
			 */
			if (in.available() > 0) {
				this.securityLevel = in.readByte();
				// System.out.println("got lan friend packet from " + nick +
				// " with security: " + securityLevel);
			} else {
				this.securityLevel = 0;
			}
			in.close();
		}

		public String toString() {
			return "FriendFinderFriend: '" + nickName + "' at:" + addr.getHostAddress() + ":" + port;
		}

		public byte[] serialize() throws IOException {
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			DataOutputStream o = new DataOutputStream(byteOut);
			/*
			 * format of the packet:
			 * 
			 * 4 bytes oneswarm listening port
			 * 
			 * 4 bytes len of nickname
			 * 
			 * x bytes nickname
			 * 
			 * 4 bytes len of key
			 * 
			 * x bytes key
			 */

			o.writeInt(port);
			byte[] nick = nickName.getBytes("UTF-8");
			o.writeInt(nick.length);
			o.write(nick);
			o.writeInt(publicKey.length);
			o.write(publicKey);
			o.writeByte(securityLevel);
			o.close();
			return byteOut.toByteArray();
		}

		public boolean equals(Object o) {
			if (o instanceof FriendFinderFriend) {
				return Arrays.equals(publicKey, ((FriendFinderFriend) o).publicKey);
			}
			return false;
		}

		public int hashCode() {
			return hash;
		}

		public boolean isExpired() {
			long age = System.currentTimeMillis() - createdDate;
			return age > FRIEND_EXPIRED_TIME;
		}
	}

	public List<Friend> getNearbyUsers() {
		List<Friend> l = new LinkedList<Friend>();
		synchronized (discoveredFriends) {
			for (FriendFinderFriend fff : discoveredFriends.values()) {
				Friend f = new Friend("Lan", "(" + fff.nickName + ")", fff.publicKey, false);
				f.setBlocked(true);
				f.setRequestFileList(false);
				f.setLastConnectIP(fff.addr);
				f.setLastConnectPort(fff.port);
				l.add(f);
			}
		}
		return l;
	}

	private class FriendFinderListener implements Runnable {

		private boolean quit = false;

		public void run() {
			try {
				/*
				 * wait for the filelist
				 */
				OSF2FMain.getSingelton().getOverlayManager().getFilelistManager().waitForFileListCreation();

				/*
				 * We need to join the multicast group on all interfaces
				 */
				Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
				MulticastSocket socket = new MulticastSocket(FRIEND_FINDER_PORT);
				SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(MULTICAST_GROUP), FRIEND_FINDER_PORT);
				socket.joinGroup(InetAddress.getByName(MULTICAST_GROUP));
				for (NetworkInterface netIf : Collections.list(nets)) {
					try {
						socket.joinGroup(socketAddress, netIf);
					} catch (Throwable t) {
						logger.finer("unable to join LanFriendFinder multicast group on network interface: " + netIf.getDisplayName());
					}
				}
				while (!quit) {
					byte[] b = new byte[1500];
					DatagramPacket p = new DatagramPacket(b, b.length);
					socket.receive(p);
					try {
						FriendFinderFriend f = new FriendFinderFriend(p);
						logger.finest("got fff packet: " + f);

						// check if me
						if (!f.equals(me)) {
							// add to discovered friends
							synchronized (discoveredFriends) {
								discoveredFriends.put(f.hashCode(), f);
							}
							// ok, check if connected
							Friend friend = friendManager.getFriend(f.publicKey);
							if (friend != null) {
								Date lastConnectDate = friend.getLastConnectDate();
								long time = 0;
								if (lastConnectDate != null) {
									time = lastConnectDate.getTime();
								}
								if (friend != null && (friend.getStatus() == Friend.STATUS_OFFLINE || friend.getStatus() == Friend.STATUS_CONNECTING) && (System.currentTimeMillis() > time + MAX_FRIEND_CONNECT_FREQ)) {
									// try to connect
									logger.finest("fff packet is from friends, trying to connect: " + friend);
									friendConnector.connectToFriend(friend, f.addr, f.port);
								}
							} else {
								/*
								 * this is not a friend, check if it is an
								 * invitation
								 */
								FriendInvitation invitation = invitationManager.getInvitationFromPublicKey(f.publicKey);
								if (invitation != null) {
									if (invitation.hasConnectableStatus()) {
										invitation.setLastConnectIp(f.addr.getHostAddress());
										invitation.setLastConnectPort(f.port);
										friendConnector.connectToInvitation(invitation);
									}
								}
							}

						}

					} catch (Throwable t) {
						if (t instanceof InterruptedException) {
							quit = true;
						} else {
							Debug.out("got error in lan friend finder listener", t);
						}
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			logger.fine("LanFriendFinderListener stopped");
		}

		public void quit() {
			quit = true;
		}
	}

	public static void main(String[] args) {

		byte[] b = new byte[511];
		Arrays.fill(b, (byte) 'a');
		try {
			FriendFinderFriend f = new FriendFinderFriend(InetAddress.getByName("www.cs.washington.edu"), "testnickasdfdsf", 12345, b, (byte) 0);
			System.out.println(f);
			byte[] s = f.serialize();
			DatagramPacket p = new DatagramPacket(s, s.length);
			p.setAddress(InetAddress.getByName("www.cs.washington.edu"));
			FriendFinderFriend f2 = new FriendFinderFriend(p);
			System.err.println(f2);
			if (!f2.equals(f)) {
				System.err.println("decoding problem (!=)");
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
