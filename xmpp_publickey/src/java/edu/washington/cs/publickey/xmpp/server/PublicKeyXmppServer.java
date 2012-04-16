package edu.washington.cs.publickey.xmpp.server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;

import edu.washington.cs.publickey.Tools;
import edu.washington.cs.publickey.storage.PersistentStorage;
import edu.washington.cs.publickey.xmpp.XMPPNetwork;

public class PublicKeyXmppServer {

	public final static String key_username = "xmpp_username";
	public final static String key_password = "xmpp_password";
	public final static String key_xmpp_network = "xmpp_network";

	private String username;
	private String password;

	private final PersistentStorage storage;
	private static FileWriter log;
	private XMPPConnection connection;
	private final LinkedList<PublicKeyXmppServerProtocol> protocolHandlers = new LinkedList<PublicKeyXmppServerProtocol>();
	static {
		try {
			log = new FileWriter(new File("xmpp_server_debug.log"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
	}

	public PublicKeyXmppServer(Properties props, PersistentStorage storage) {
		loadProperties(props);
		log("Starting IM client for " + username, true);
		this.storage = storage;

		ConnectionConfiguration connConfig = new ConnectionConfiguration(network.getServerAddr(), network.getServerPort(), network.getServiceName());

		this.connect(connConfig);
		connection.addPacketListener(new PublicKeyListener(), new Tools.AcceptAllFilter());

		Thread protocolHandlerCleaner = new Thread(new Runnable() {
			public void run() {
				boolean quit = false;
				while (!quit) {
					synchronized (PublicKeyXmppServer.this) {
						for (Iterator<PublicKeyXmppServerProtocol> iter = protocolHandlers.iterator(); iter.hasNext();) {
							PublicKeyXmppServerProtocol p = iter.next();
							if (p.isTimedOut()) {
								p.close();
								iter.remove();
							}
						}
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						quit = true;
					}
				}
			}
		});
		protocolHandlerCleaner.setName("XMPP protocol listener killer");
		protocolHandlerCleaner.setDaemon(true);
		protocolHandlerCleaner.start();
	}

	private void connect(ConnectionConfiguration connConfig) {
		connection = new XMPPConnection(connConfig);

		// connect to the xmpp server
		try {
			connection.connect();
			log("Connected to " + connection.getHost(), true);
		} catch (XMPPException ex) {
			log("Failed to connect to " + connection.getHost(), true);
			return;
		}

		// login
		try {
			connection.login(username, password);
			log("Logged in as " + connection.getUser(), true);
		} catch (XMPPException ex) {
			log("Failed to log in as " + connection.getUser(), true);
			return;
		}

		// set presence
		Presence p = new Presence(Presence.Type.available);
		p.setMode(Presence.Mode.available);
		connection.sendPacket(p);

		// set allow messages from all users
		connection.getRoster().setSubscriptionMode(Roster.SubscriptionMode.accept_all);
		PacketFilter presenceFilter = new PacketTypeFilter(Presence.class);
		PresencePacketListener presencePacketListener = new PresencePacketListener();
		connection.addPacketListener(presencePacketListener, presenceFilter);
		return;
	}

	private XMPPNetwork network;

	private void loadProperties(Properties props) {
		try {
			this.username = props.getProperty(key_username);
			this.password = props.getProperty(key_password);
			this.network = XMPPNetwork.getFromName(props.getProperty(key_xmpp_network));
		} catch (Throwable t) {
			System.err.println("error loading properties: " + t.getMessage());
			t.printStackTrace();
			System.exit(1);
		}
	}

	public XMPPNetwork getNetwork() {
		return network;
	}

	public void log(String mesg, boolean toStdOut) {
		try {
			if (log != null) {
				log.write(mesg + "\n");
				log.flush();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (toStdOut) {
			System.out.println(mesg);
		}
	}

	private class PublicKeyListener implements PacketListener {
		public void processPacket(Packet packet) {
			Object clientHello = packet.getProperty(Tools.PUBLICKEY_PAYLOAD_KEY__PublicKeyFriend);
			// log("got message:" + packet.toXML(), false);

			if (clientHello != null) {
				log("got client hello from: " + packet.getFrom(), true);
				try {
					PublicKeyXmppServerProtocol handler = new PublicKeyXmppServerProtocol(packet, connection, storage, PublicKeyXmppServer.this);
					synchronized (PublicKeyXmppServer.this) {
						protocolHandlers.add(handler);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private class PresencePacketListener implements PacketListener {

		public void processPacket(Packet packet) {
			Presence presence = (Presence) packet;
			String from = presence.getFrom();
			// System.out.println("got packet from: " + from + "\n" +
			// packet.toXML());
			if (presence.getType() == Presence.Type.subscribe) {
				// Accept all subscription requests.
				Presence response = new Presence(Presence.Type.subscribed);
				response.setTo(from);
				response.setPacketID(packet.getPacketID());
				connection.sendPacket(response);
				System.out.println("got presense subscribe from: " + packet.getFrom());
			} else if (presence.getType() == Presence.Type.available) {
				// Send back that we are available as well
				Presence response = new Presence(Presence.Type.available);
				response.setMode(Mode.available);
				response.setTo(from);
				response.setPacketID(packet.getPacketID());
				connection.sendPacket(response);
				System.out.println("got presense available from: " + packet.getFrom());
				// System.out.println("sending presence available: \n" +
				// response.toXML());
			} else if (presence.getType() == Presence.Type.unsubscribe) {
				// Accept all unsubscription requests.
				Presence response = new Presence(Presence.Type.unsubscribed);
				response.setTo(from);
				response.setPacketID(packet.getPacketID());
				connection.sendPacket(response);
				System.out.println("got presense unsubscribe from: " + packet.getFrom());
				// System.out.println("sending presence unsubscribed: \n" +
				// response.toXML());
			}
		}

	}

	public static interface LimitedXMPPConnection {
		public void sendMessage();
	}

	public void shutdown() {
		if (connection.isConnected()) {
			log("logging off xmpp: " + username, true);
			connection.disconnect();
		}
	}

}
