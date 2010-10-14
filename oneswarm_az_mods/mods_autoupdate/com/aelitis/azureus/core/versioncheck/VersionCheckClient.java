/*
 * Created on Dec 20, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004-2005 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.versioncheck;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.stats.transfer.impl.OverallStatsImpl;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.update.CoreUpdateChecker;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.clientmessageservice.ClientMessageService;
import com.aelitis.azureus.core.clientmessageservice.ClientMessageServiceClient;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminASN;
import com.aelitis.azureus.core.networkmanager.impl.osssl.OneSwarmSslKeyManager;
import com.aelitis.net.udp.uc.PRUDPPacketHandler;
import com.aelitis.net.udp.uc.PRUDPPacketHandlerFactory;
import com.aelitis.net.udp.uc.PRUDPReleasablePacketHandler;

import edu.washington.cs.oneswarm.f2f.Friend;

/**
 * Client for checking version information from a remote server.
 */
public class VersionCheckClient
{
	private static final LogIDs			 LOGID												= LogIDs.CORE;

	public static final String				REASON_UPDATE_CHECK_START		= "us";

	public static final String				REASON_UPDATE_CHECK_PERIODIC = "up";

	public static final String				REASON_CHECK_SWT						 = "sw";

	public static final String				REASON_DHT_EXTENDED_ALLOWED	= "dx";

	public static final String				REASON_DHT_ENABLE_ALLOWED		= "de";

	public static final String				REASON_EXTERNAL_IP					 = "ip";

	public static final String				REASON_RECOMMENDED_PLUGINS	 = "rp";

	public static final String				REASON_SECONDARY_CHECK			 = "sc";

	//private static final String			 AZ_MSG_SERVER_ADDRESS_V4		 = Constants.VERSION_SERVER_V4;
	private static final String			 AZ_MSG_SERVER_ADDRESS_V4		 = Constants.getVERSION_SERVER_V4();

	private static final int					AZ_MSG_SERVER_PORT					 = 27001;

	private static final String			 MESSAGE_TYPE_ID							= "AZVER";

	public static final String				HTTP_SERVER_ADDRESS_V4			 = AZ_MSG_SERVER_ADDRESS_V4;

	public static final int					 HTTP_SERVER_PORT						 = 80;

	public static final String				TCP_SERVER_ADDRESS_V4				= AZ_MSG_SERVER_ADDRESS_V4;

	public static final int					 TCP_SERVER_PORT							= 80;

	public static final String				UDP_SERVER_ADDRESS_V4				= AZ_MSG_SERVER_ADDRESS_V4;

	public static final int					 UDP_SERVER_PORT							= 2080;

//	public static final String				AZ_MSG_SERVER_ADDRESS_V6		 = Constants.VERSION_SERVER_V6;
	public static final String				AZ_MSG_SERVER_ADDRESS_V6		 = Constants.getVERSION_SERVER_V6();

	public static final String				HTTP_SERVER_ADDRESS_V6			 = AZ_MSG_SERVER_ADDRESS_V6;

	public static final String				TCP_SERVER_ADDRESS_V6				= AZ_MSG_SERVER_ADDRESS_V6;

	public static final String				UDP_SERVER_ADDRESS_V6				= AZ_MSG_SERVER_ADDRESS_V6;

	private static final long				 CACHE_PERIOD								 = 10 * 1000;
	
	private static boolean						secondary_check_done;

	static {
		VersionCheckClientUDPCodecs.registerCodecs();
	}

	private static final int					AT_V4												= 1;

	private static final int					AT_V6												= 2;

	private static final int					AT_EITHER										= 3;

	private static VersionCheckClient instance;

	/**
	 * Get the singleton instance of the version check client.
	 * @return version check client
	 */
	public static synchronized VersionCheckClient getSingleton() {
		if (instance == null) {

			instance = new VersionCheckClient();
		}

		return (instance);
	}

	private boolean				 prefer_v6;

	private Map						 last_check_data_v4 = null;

	private Map						 last_check_data_v6 = null;

	private final AEMonitor check_mon					= new AEMonitor(
																								 "versioncheckclient");

	private long						last_check_time_v4 = 0;

	private long						last_check_time_v6 = 0;

	private long						last_feature_flag_cache;

	private long						last_feature_flag_cache_time;

	private VersionCheckClient() {
		COConfigurationManager.addAndFireParameterListener("IPV6 Prefer Addresses",
				new ParameterListener() {
					public void parameterChanged(String name) {
						prefer_v6 = COConfigurationManager.getBooleanParameter(name);
					}
				});
	}

	/**
	 * Get the version check reply info.
	 * @return reply data, possibly cached, if the server was already checked within the last minute
	 */

	public Map getVersionCheckInfo(String reason) {
		return (getVersionCheckInfo(reason, AT_EITHER));
	}

	public Map getVersionCheckInfo(String reason, int address_type) {
		if (address_type == AT_V4) {

			return (getVersionCheckInfoSupport(reason, false, false, false));

		} else if (address_type == AT_V6) {

			return (getVersionCheckInfoSupport(reason, false, false, true));

		} else {

			Map reply = getVersionCheckInfoSupport(reason, false, false, prefer_v6);

			if (reply == null || reply.size() == 0) {

				reply = getVersionCheckInfoSupport(reason, false, false, !prefer_v6);
			}

			return (reply);
		}
	}

	protected Map getVersionCheckInfoSupport(String reason,
			boolean only_if_cached, boolean force, boolean v6) {
		if (v6) {

			try {
				check_mon.enter();

				long time_diff = SystemTime.getCurrentTime() - last_check_time_v6;

				force = force || time_diff > CACHE_PERIOD || time_diff < 0;

				if (last_check_data_v6 == null || last_check_data_v6.size() == 0
						|| force) {
					// if we've never checked before then we go ahead even if the "only_if_cached"
					// flag is set as its had not chance of being cached yet!
					if (only_if_cached && last_check_data_v6 != null) {
						return (new HashMap());
					}
					try {
						last_check_data_v6 = performVersionCheck(
								constructVersionCheckMessage(reason), true, true, true);

						if (last_check_data_v6 != null && last_check_data_v6.size() > 0) {

							COConfigurationManager.setParameter("versioncheck.cache.v6",
									last_check_data_v6);
						}
					} catch (SocketException t) {
						// internet is broken
						Debug.out(t.getClass().getName() + ": " + t.getMessage());
					} catch (UnknownHostException t) {
						// dns is broken
						Debug.out(t.getClass().getName() + ": " + t.getMessage());
					} catch (Throwable t) {
						Debug.out(t);
						last_check_data_v6 = new HashMap();
					}
				} else {
					Logger.log(new LogEvent(LOGID, "VersionCheckClient is using "
							+ "cached version check info. Using " + last_check_data_v6.size()
							+ " reply keys."));
				}
			} finally {
				check_mon.exit();
			}

			if (last_check_data_v6 == null)
				last_check_data_v6 = new HashMap();

			return last_check_data_v6;

		} else {

			try {
				check_mon.enter();

				long time_diff = SystemTime.getCurrentTime() - last_check_time_v4;

				force = force || time_diff > CACHE_PERIOD || time_diff < 0;

				if (last_check_data_v4 == null || last_check_data_v4.size() == 0
						|| force) {
					// if we've never checked before then we go ahead even if the "only_if_cached"
					// flag is set as its had not chance of being cached yet!
					if (only_if_cached && last_check_data_v4 != null) {
						return (new HashMap());
					}
					try {
						last_check_data_v4 = performVersionCheck(
								constructVersionCheckMessage(reason), true, true, false);

						if (last_check_data_v4 != null && last_check_data_v4.size() > 0) {

							COConfigurationManager.setParameter("versioncheck.cache.v4",
									last_check_data_v4);
						}

						// clear down any plugin-specific data that has successfully been sent to the version server

						try {
							if (AzureusCoreFactory.isCoreAvailable()) {

								//installed plugin IDs
								PluginInterface[] plugins = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaces();

								for (int i = 0; i < plugins.length; i++) {

									PluginInterface plugin = plugins[i];

									Map data = plugin.getPluginconfig().getPluginMapParameter(
											"plugin.versionserver.data", null);

									if (data != null) {

										plugin.getPluginconfig().setPluginMapParameter(
												"plugin.versionserver.data", new HashMap());
									}
								}
							}
						} catch (Throwable e) {
						}
					} catch (UnknownHostException t) {
						// no internet
						Debug.outNoStack("VersionCheckClient - " + t.getClass().getName()
								+ ": " + t.getMessage());
					} catch (IOException t) {
						// General connection problem.
						Debug.outNoStack("VersionCheckClient - " + t.getClass().getName()
								+ ": " + t.getMessage());
					} catch (Throwable t) {
						Debug.out(t);
						last_check_data_v4 = new HashMap();
					}
				} else {
					if (Logger.isEnabled())
						Logger.log(new LogEvent(LOGID, "VersionCheckClient is using "
								+ "cached version check info. Using "
								+ last_check_data_v4.size() + " reply keys."));
				}
			} finally {
				check_mon.exit();
			}

			if (last_check_data_v4 == null)
				last_check_data_v4 = new HashMap();

			return last_check_data_v4;
		}
	}

	private boolean isVersionCheckDataValid(int address_type) {
		boolean v6_ok = last_check_data_v6 != null && last_check_data_v6.size() > 0;
		boolean v4_ok = last_check_data_v4 != null && last_check_data_v4.size() > 0;

		if (address_type == AT_V4) {

			return (v4_ok);

		} else if (address_type == AT_V6) {

			return (v6_ok);

		} else {

			return (v4_ok | v6_ok);
		}
	}

	protected Map getMostRecentVersionCheckData() {
		// currently we maintain v4 much more accurately than v6

		if (last_check_data_v4 != null) {

			return (last_check_data_v4);
		}

		Map res = COConfigurationManager.getMapParameter("versioncheck.cache.v4",
				null);

		if (res != null) {

			return (res);
		}

		if (last_check_data_v6 != null) {

			return (last_check_data_v6);
		}

		res = COConfigurationManager.getMapParameter("versioncheck.cache.v6", null);

		return (res);
	}

	public long getFeatureFlags() {
		long now = SystemTime.getCurrentTime();

		if (now > last_feature_flag_cache_time
				&& now - last_feature_flag_cache_time < 60000) {

			return (last_feature_flag_cache);
		}

		Map m = getMostRecentVersionCheckData();

		long result;

		if (m == null) {

			result = 0;

		} else {

			byte[] b_feat_flags = (byte[]) m.get("feat_flags");

			if (b_feat_flags != null) {

				try {

					result = Long.parseLong(new String((byte[]) b_feat_flags));

				} catch (Throwable e) {

					result = 0;
				}
			} else {

				result = 0;
			}
		}

		last_feature_flag_cache = result;
		last_feature_flag_cache_time = now;

		return (result);
	}

	public long getCacheTime(boolean v6) {
		return (v6 ? last_check_time_v6 : last_check_time_v4);
	}

	/**
	 * Get the ip address seen by the version check server.
	 * NOTE: This information may be cached, see getVersionCheckInfo().
	 * @return external ip address, or empty string if no address information found
	 */

	public String getExternalIpAddress(boolean only_if_cached, boolean v6) {
		Map reply = getVersionCheckInfoSupport(REASON_EXTERNAL_IP, only_if_cached,
				false, v6);

		byte[] address = (byte[]) reply.get("source_ip_address");
		if (address != null) {
			return new String(address);
		}

		return (null);
	}

	/**
	 * Is the DHT plugin allowed to be enabled.
	 * @return true if DHT can be enabled, false if it should not be enabled
	 */
	public boolean DHTEnableAllowed() {

		return true;

		// PIAMOD -- no more remote control of DHT 

		//		Map reply = getVersionCheckInfo(REASON_DHT_ENABLE_ALLOWED, AT_EITHER);
		//
		//		boolean res = false;
		//
		//		byte[] value = (byte[]) reply.get("enable_dht");
		//
		//		if (value != null) {
		//
		//			res = new String(value).equalsIgnoreCase("true");
		//		}
		//
		//		// we take the view that if the version check failed then we go ahead
		//		// and enable the DHT (i.e. we're being optimistic)
		//
		//		if (!res) {
		//			res = !isVersionCheckDataValid(AT_EITHER);
		//		}
		//
		//		return res;
	}

	/**
	 * Is the DHT allowed to be used by external plugins.
	 * @return true if extended DHT use is allowed, false if not allowed
	 */
	public boolean DHTExtendedUseAllowed() {

		return true;

		/**
		 * PIAMOD -- no more remote control of plugin use of DHT 
		 */

		//		Map reply = getVersionCheckInfo(REASON_DHT_EXTENDED_ALLOWED, AT_EITHER);
		//
		//		boolean res = false;
		//
		//		byte[] value = (byte[]) reply.get("enable_dht_extended_use");
		//		if (value != null) {
		//			res = new String(value).equalsIgnoreCase("true");
		//		}
		//
		//		// be generous and enable extended use if check failed
		//
		//		if (!res) {
		//			res = !isVersionCheckDataValid(AT_EITHER);
		//		}
		//
		//		return res;
	}

	public String[] getRecommendedPlugins() {

		/**
		 * PIAMOD -- no more recommended plugins. 
		 */

		return new String[] {};

		//		Map reply = getVersionCheckInfo(REASON_RECOMMENDED_PLUGINS, AT_EITHER);
		//
		//		List l = (List) reply.get("recommended_plugins");
		//
		//		if (l == null) {
		//
		//			return (new String[0]);
		//		}
		//
		//		String[] res = new String[l.size()];
		//
		//		for (int i = 0; i < l.size(); i++) {
		//
		//			res[i] = new String((byte[]) l.get(i));
		//		}
		//		
		//		for( String s : res ) {
		//			System.out.println("update instrumentation: recommended: " + s);
		//		}
		//
		//		return (res);
	}

	/**
	 * Perform the actual version check by connecting to the version server.
	 * @param data_to_send version message
	 * @return version reply
	 * @throws Exception if the server check connection fails
	 */
	private Map performVersionCheck(Map data_to_send, boolean use_az_message,
			boolean use_http, boolean v6)

	throws Exception {
		Exception error = null;
		Map reply = null;

		if (use_az_message) {

			try {
				reply = executeAZMessage(data_to_send, v6);

				reply.put("protocol_used", "AZMSG");
			} catch (IOException e) {
				error = e;
			} catch (Exception e) {
				Debug.printStackTrace(e);

				error = e;
			}
		}
		
		/**
		 * PIAMOD
		 * This will never work with our update server anyway. 
		 */
//		if (reply == null && use_http) {
//
//			try {
//				reply = executeHTTP(data_to_send, v6);
//
//				reply.put("protocol_used", "HTTP");
//
//				error = null;
//			} catch (IOException e) {
//				error = e;
//			} catch (Exception e) {
//				Debug.printStackTrace(e);
//				error = e;
//
//			}
//		}
		if (error != null) {

			throw (error);
		}

		if (Logger.isEnabled())
			Logger.log(new LogEvent(LOGID, "VersionCheckClient server "
					+ "version check successful. Received " + reply.size()
					+ " reply keys."));

		if (v6) {

			last_check_time_v6 = SystemTime.getCurrentTime();

		} else {

			last_check_time_v4 = SystemTime.getCurrentTime();
		}

		return reply;
	}

	private Map executeAZMessage(Map data_to_send, boolean v6)

	throws Exception {
		String host = v6 ? AZ_MSG_SERVER_ADDRESS_V6 : AZ_MSG_SERVER_ADDRESS_V4;

		if (Logger.isEnabled())
			Logger.log(new LogEvent(LOGID, "VersionCheckClient retrieving "
					+ "version information from " + host + ":" + AZ_MSG_SERVER_PORT));

		ClientMessageService msg_service = null;
		Map reply = null;

		try {
			msg_service = ClientMessageServiceClient.getServerService(host,
					AZ_MSG_SERVER_PORT, MESSAGE_TYPE_ID);

			msg_service.sendMessage(data_to_send); //send our version message

			reply = msg_service.receiveMessage(); //get the server reply

			preProcessReply(reply, v6);

		} finally {

			if (msg_service != null) {

				msg_service.close();
			}
		}

		return (reply);
	}

	private Map executeHTTP(Map data_to_send, boolean v6)

	throws Exception {
		String host = v6 ? HTTP_SERVER_ADDRESS_V6 : HTTP_SERVER_ADDRESS_V4;

		if (Logger.isEnabled())
			Logger.log(new LogEvent(LOGID, "VersionCheckClient retrieving "
					+ "version information from " + host + ":" + HTTP_SERVER_PORT
					+ " via HTTP"));

		String url_str = "http://" + host
				+ (HTTP_SERVER_PORT == 80 ? "" : (":" + HTTP_SERVER_PORT))
				+ "/version?";

		url_str += URLEncoder.encode(new String(BEncoder.encode(data_to_send),
				"ISO-8859-1"), "ISO-8859-1");

		URL url = new URL(url_str);

		HttpURLConnection url_connection = (HttpURLConnection) url.openConnection();

		url_connection.connect();

		try {
			InputStream is = url_connection.getInputStream();

			Map reply = BDecoder.decode(new BufferedInputStream(is));

			preProcessReply(reply, v6);

			return (reply);

		} finally {

			url_connection.disconnect();
		}
	}

	public String getHTTPGetString(boolean for_proxy, boolean v6) {
		return (getHTTPGetString(new HashMap(), for_proxy, v6));
	}

	private String getHTTPGetString(Map content, boolean for_proxy, boolean v6) {
		String host = v6 ? HTTP_SERVER_ADDRESS_V6 : HTTP_SERVER_ADDRESS_V4;

		String get_str = "GET "
				+ (for_proxy ? ("http://" + host + ":" + HTTP_SERVER_PORT) : "")
				+ "/version?";

		try {
			get_str += URLEncoder.encode(new String(BEncoder.encode(content),
					"ISO-8859-1"), "ISO-8859-1");

		} catch (Throwable e) {
		}

		get_str += " HTTP/1.1" + "\015\012" + "\015\012";

		return (get_str);
	}

	private Map executeTCP(Map data_to_send, InetAddress bind_ip, int bind_port,
			boolean v6)

	throws Exception {
		String host = v6 ? TCP_SERVER_ADDRESS_V6 : TCP_SERVER_ADDRESS_V4;

		if (Logger.isEnabled())
			Logger.log(new LogEvent(LOGID, "VersionCheckClient retrieving "
					+ "version information from " + host + ":" + TCP_SERVER_PORT
					+ " via TCP"));

		String get_str = getHTTPGetString(data_to_send, false, v6);

		Socket socket = null;

		try {
			socket = new Socket();

			if (bind_ip != null) {

				socket.bind(new InetSocketAddress(bind_ip, bind_port));

			} else if (bind_port != 0) {

				socket.bind(new InetSocketAddress(bind_port));
			}

			socket.setSoTimeout(10000);

			socket.connect(new InetSocketAddress(host, TCP_SERVER_PORT), 10000);

			OutputStream os = socket.getOutputStream();

			os.write(get_str.getBytes("ISO-8859-1"));

			os.flush();

			InputStream is = socket.getInputStream();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			byte[] buffer = new byte[1024];

			int total_len = 0;

			while (true) {

				int len = is.read(buffer);

				if (len <= 0) {

					break;
				}

				total_len += len;

				if (total_len > 16000) {

					throw (new IOException("reply too large"));
				}

				baos.write(buffer, 0, len);
			}

			byte[] reply_bytes = baos.toByteArray();

			for (int i = 3; i < reply_bytes.length; i++) {

				if (reply_bytes[i - 3] == (byte) '\015'
						&& reply_bytes[i - 2] == (byte) '\012'
						&& reply_bytes[i - 1] == (byte) '\015'
						&& reply_bytes[i - 0] == (byte) '\012') {

					Map reply = BDecoder.decode(new BufferedInputStream(
							new ByteArrayInputStream(reply_bytes, i + 1, reply_bytes.length
									- (i + 1))));

					preProcessReply(reply, v6);

					return (reply);
				}
			}

			throw (new Exception("Invalid reply: " + new String(reply_bytes)));

		} finally {

			if (socket != null) {

				try {
					socket.close();

				} catch (Throwable e) {

				}
			}
		}
	}

	private Map executeUDP(Map data_to_send, InetAddress bind_ip, int bind_port,
			boolean v6)

	throws Exception {
		String host = v6 ? UDP_SERVER_ADDRESS_V6 : UDP_SERVER_ADDRESS_V4;

		PRUDPReleasablePacketHandler handler = PRUDPPacketHandlerFactory.getReleasableHandler(bind_port);

		PRUDPPacketHandler packet_handler = handler.getHandler();

		long timeout = 5;

		Random random = new Random();

		try {
			Exception last_error = null;

			packet_handler.setExplicitBindAddress(bind_ip);

			for (int i = 0; i < 3; i++) {

				try {
					// connection ids for requests must always have their msb set...
					// apart from the original darn udp tracker spec....

					long connection_id = 0x8000000000000000L | random.nextLong();

					VersionCheckClientUDPRequest request_packet = new VersionCheckClientUDPRequest(
							connection_id);

					request_packet.setPayload(data_to_send);

					VersionCheckClientUDPReply reply_packet = (VersionCheckClientUDPReply) packet_handler.sendAndReceive(
							null, request_packet,
							new InetSocketAddress(host, UDP_SERVER_PORT), timeout);

					Map reply = reply_packet.getPayload();

					preProcessReply(reply, v6);

					return (reply);

				} catch (Exception e) {

					last_error = e;

					timeout = timeout * 2;
				}
			}

			if (last_error != null) {

				throw (last_error);
			}

			throw (new Exception("Timeout"));

		} finally {

			packet_handler.setExplicitBindAddress(null);

			handler.release();
		}
	}

	protected void preProcessReply(Map reply, final boolean v6) {
		NetworkAdmin admin = NetworkAdmin.getSingleton();

		try {
			byte[] address = (byte[]) reply.get("source_ip_address");

			InetAddress my_ip = InetAddress.getByName(new String(address));

			NetworkAdminASN old_asn = admin.getCurrentASN();

			NetworkAdminASN new_asn = admin.lookupCurrentASN(my_ip);

			if (!new_asn.sameAs(old_asn)) {

				// kick off a secondary version check to communicate the new information

				if (!secondary_check_done) {

					secondary_check_done = true;

					new AEThread("Secondary version check", true) {
						public void runSupport() {
							getVersionCheckInfoSupport(REASON_SECONDARY_CHECK, false, true,
									v6);
						}
					}.start();
				}
			}
		} catch (Throwable e) {

			Debug.printStackTrace(e);
		}

		Long as_advice = (Long) reply.get("as_advice");

		if (as_advice != null) {

			NetworkAdminASN current_asn = admin.getCurrentASN();

			String asn = current_asn.getASName();

			if (asn != null) {

				long advice = as_advice.longValue();

				if (advice != 0) {

					// require crypto

					String done_asn = COConfigurationManager.getStringParameter(
							"ASN Advice Followed", "");

					if (!done_asn.equals(asn)) {

						COConfigurationManager.setParameter("ASN Advice Followed", asn);

						boolean change = advice == 1 || advice == 2;
						boolean alert = advice == 1 || advice == 3;

						if (!COConfigurationManager.getBooleanParameter("network.transport.encrypted.require")) {

							if (change) {

								COConfigurationManager.setParameter(
										"network.transport.encrypted.require", true);
							}

							if (alert) {

								String msg = MessageText.getString("crypto.alert.as.warning",
										new String[] {
											asn
										});

								Logger.log(new LogAlert(false, LogAlert.AT_WARNING, msg));
							}
						}
					}
				}
			}
		}

		// set ui.toolbar.uiswitcher based on instructions from tracker
		// Really shouldn't be in VersionCheck client, but instead have some 
		// listener and have the code elsewhere.  Simply calling 
		//getVersionCheckInfo from "code elsewhere" (to get the cached result) 
		//caused a deadlock at startup.
		Long lEnabledUISwitcher = (Long) reply.get("ui.toolbar.uiswitcher");
		if (lEnabledUISwitcher != null) {
			COConfigurationManager.setBooleanDefault("ui.toolbar.uiswitcher",
					lEnabledUISwitcher.longValue() == 1);
		}
	}

	public InetAddress getExternalIpAddressHTTP(boolean v6)

	throws Exception {
		Map reply = executeHTTP(new HashMap(), v6);

		byte[] address = (byte[]) reply.get("source_ip_address");

		return (InetAddress.getByName(new String(address)));
	}

	public InetAddress getExternalIpAddressTCP(InetAddress bind_ip,
			int bind_port, boolean v6)

	throws Exception {
		Map reply = executeTCP(new HashMap(), bind_ip, bind_port, v6);

		byte[] address = (byte[]) reply.get("source_ip_address");

		return (InetAddress.getByName(new String(address)));
	}

	public InetAddress getExternalIpAddressUDP(InetAddress bind_ip,
			int bind_port, boolean v6)

	throws Exception {
		Map reply = executeUDP(new HashMap(), bind_ip, bind_port, v6);

		byte[] address = (byte[]) reply.get("source_ip_address");

		return (InetAddress.getByName(new String(address)));
	}

	/**
	 * Construct the default version check message.
	 * @return message to send
	 */
	private Map constructVersionCheckMessage(String reason) {
		Map message = new HashMap();

		//********************************************************
		/*
		 * EDIT: by isdal, adding version for our mods jar
		 */
		message.put(CoreUpdateChecker.KEY_ONE_SWARM_MOD_VERSION,
				Constants.getOneSwarmAzureusModsVersion());
		
		/*
		 * check if we should include the tcp port in the update
		 */
		if (COConfigurationManager.getLongParameter("Perform.NAT.Check", 1) > 0){
			message.put("tcp_port", new Integer(COConfigurationManager.getIntParameter("TCP.Listen.Port")));
			message.put("udp_port", new Integer(COConfigurationManager.getIntParameter("UDP.Listen.Port")));
		}
		long speedTest = COConfigurationManager.getLongParameter("Allow.Incoming.Speed.Check", 0);
		if (speedTest > 0) {
			message.put("speed_test", speedTest);
		}
		//***********************************************************
		message.put("appid", SystemProperties.getApplicationIdentifier());
		message.put("version", Constants.AZUREUS_VERSION);

		String id = COConfigurationManager.getStringParameter("ID", null);
		boolean send_info = COConfigurationManager.getBooleanParameter("Send Version Info");

		int last_send_time = COConfigurationManager.getIntParameter(
				"Send Version Info Last Time", -1);

		int current_send_time = (int) (SystemTime.getCurrentTime() / 1000);

		COConfigurationManager.setParameter("Send Version Info Last Time",
				current_send_time);

		if (id != null && send_info) {

			message.put("id", id);
			message.put("os", Constants.OSName);

			message.put("os_version", System.getProperty("os.version"));
			message.put("os_arch", System.getProperty("os.arch")); //see http://lopica.sourceforge.net/os.html

			if (last_send_time != -1 && last_send_time < current_send_time) {

				// tims since last

				message.put("tsl", new Long(current_send_time - last_send_time));
			}

			message.put("reason", reason);

			String java_version = System.getProperty("java.version");
			if (java_version == null) {
				java_version = "unknown";
			}
			message.put("java", java_version);

			String java_vendor = System.getProperty("java.vm.vendor");
			if (java_vendor == null) {
				java_vendor = "unknown";
			}
			message.put("javavendor", java_vendor);

			long max_mem = Runtime.getRuntime().maxMemory() / (1024 * 1024);
			message.put("javamx", new Long(max_mem));

			String java_rt_name = System.getProperty("java.runtime.name");
			if (java_rt_name != null) {
				message.put("java_rt_name", java_rt_name);
			}

			String java_rt_version = System.getProperty("java.runtime.version");
			if (java_rt_version != null) {
				message.put("java_rt_version", java_rt_version);
			}

			OverallStats stats = StatsFactory.getStats();

			if (stats != null) {

				long total_bytes_downloaded = stats.getDownloadedBytes();
				long total_bytes_uploaded = stats.getUploadedBytes();
				long total_uptime = stats.getTotalUpTime();

				message.put("total_bytes_downloaded", new Long(total_bytes_downloaded));
				message.put("total_bytes_uploaded", new Long(total_bytes_uploaded));
				message.put("total_uptime", new Long(total_uptime));
				//message.put( "dlstats", stats.getDownloadStats());

				// *******************************************************
				// Edit: by isdal
				// Adding performance data for the beta testing f2f stuff
				PluginInterface f2fIf = AzureusCoreImpl.getSingleton().getPluginManager().getPluginInterfaceByID(
						"osf2f");
				IPCInterface f2fIpc = null;
				if (f2fIf != null) {
					if (f2fIf.isOperational()) {
						f2fIpc = f2fIf.getIPC();
					} else {
						Debug.out("osf2f is not yet operational");
					}
				}
				if (f2fIpc != null) {
					try {

						List<Integer> textSearch = (List<Integer>) f2fIpc.invoke(
								"getAndClearTextSearchStats", new Object[0]);
						message.put("text_search_stats", textSearch);
						List<Integer> hashSearch = (List<Integer>) f2fIpc.invoke(
								"getAndClearHashSearchStats", new Object[0]);
						message.put("hash_search_stats", hashSearch);
						Integer forwarded = (Integer) f2fIpc.invoke(
								"getAndClearForwardedSearchNum", new Object[0]);
						message.put("forwarded_search_stats", forwarded);

						long totalF2FUploaded = 0;
						long totalF2FDownloaded = 0;

						Map<String, Integer> friendSourceCounts = new HashMap<String, Integer>();
						List<Long> datacounts = new LinkedList<Long>();
						List<Friend> friends = (List<Friend>) f2fIpc.invoke("getFriends",
								new Object[0]);
						for (Friend friend : friends) {
							totalF2FUploaded += friend.getTotalUploaded();
							totalF2FDownloaded += friend.getTotalDownloaded();
							datacounts.add(friend.getTotalDownloaded());
							/*
							 * sanity checks + no reason to send full string, first 2 chars is enough
							 */
							if (friend.getSourceNetwork() != null
									&& friend.getSourceNetwork().length() >= 2) {
								String friendSrc = friend.getSourceNetwork().substring(0, 2);
								if (!friendSourceCounts.containsKey(friendSrc)) {
									friendSourceCounts.put(friendSrc, 0);
								}
								friendSourceCounts.put(friendSrc,
										friendSourceCounts.get(friendSrc) + 1);
							}
						}
						
						OverallStatsImpl oStats = (OverallStatsImpl)stats;
						oStats.updateInitialF2FStats(totalF2FDownloaded, totalF2FUploaded);
						
						message.put("f2f_uploaded", oStats.getUploadedF2FBytes());
						message.put("f2f_downloaded", oStats.getDownloadedF2FBytes());
						message.put("add_friend_method", friendSourceCounts);
						message.put("f2f_counters", datacounts);

					} catch (Throwable e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					Debug.out("no osf2f plugin, skipping");
					System.err.println("plugin list: ");
					PluginInterface[] plugins = AzureusCoreImpl.getSingleton().getPluginManager().getPlugins();
					for (PluginInterface p : plugins) {
						System.err.print(p.getPluginID() + ",");
					}
					System.err.println("");
				}

				// ***********************************************************

			}

			try {
				NetworkAdminASN current_asn = NetworkAdmin.getSingleton().getCurrentASN();

				String as = current_asn.getAS();

				message.put("ip_as", current_asn.getAS());

				String asn = current_asn.getASName();

				if (asn.length() > 64) {

					asn = asn.substring(0, 64);
				}

				message.put("ip_asn", asn);

			} catch (Throwable e) {

				Debug.out(e);
			}

			String ui = COConfigurationManager.getStringParameter("ui");
			if (ui.length() > 0) {
				message.put("ui", ui);
			}

			// send locale, so we can determine which languages need attention
			message.put("locale", Locale.getDefault().toString());
			String originalLocale = System.getProperty("user.language") + "_"
					+ System.getProperty("user.country");
			String variant = System.getProperty("user.variant");
			if (variant != null && variant.length() > 0) {
				originalLocale += "_" + variant;
			}
			message.put("orig_locale", originalLocale);

			try {
				if (AzureusCoreFactory.isCoreAvailable()) {

					//installed plugin IDs
					PluginInterface[] plugins = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaces();
					List pids = new ArrayList();

					List vs_data = new ArrayList();

					for (int i = 0; i < plugins.length; i++) {

						PluginInterface plugin = plugins[i];

						String pid = plugin.getPluginID();

						String info = plugin.getPluginconfig().getPluginStringParameter(
								"plugin.info");

						// filter out built-in and core ones
						if ((info != null && info.length() > 0)
								|| (!pid.startsWith("<") && !pid.startsWith("azbp")
										&& !pid.startsWith("azupdater")
										&& !pid.startsWith("azplatform") && !pids.contains(pid))) {

							if (info != null && info.length() > 0) {

								if (info.length() < 256) {

									pid += ":" + info;

								} else {

									Debug.out("Plugin '" + pid
											+ "' reported excessive info string '" + info + "'");
								}
							}

							pids.add(pid);
						}

						Map data = plugin.getPluginconfig().getPluginMapParameter(
								"plugin.versionserver.data", null);

						if (data != null) {

							Map payload = new HashMap();

							byte[] data_bytes = BEncoder.encode(data);

							if (data_bytes.length > 16 * 1024) {

								Debug.out("Plugin '" + pid
										+ "' reported excessive version server data (length="
										+ data_bytes.length + ")");

								payload.put("error", "data too long: " + data_bytes.length);

							} else {

								payload.put("data", data_bytes);
							}

							payload.put("id", pid);
							payload.put("version", plugin.getPluginVersion());

							vs_data.add(payload);
						}
					}
					message.put("plugins", pids);

					if (vs_data.size() > 0) {

						message.put("plugin_data", vs_data);
					}
				}
			} catch (Throwable e) {

				Debug.out(e);
			}
		}

		//swt stuff
		try {
			Class c = Class.forName("org.eclipse.swt.SWT");

			String swt_platform = (String) c.getMethod("getPlatform", new Class[] {}).invoke(
					null, new Object[] {});
			message.put("swt_platform", swt_platform);

			if (send_info) {
				Integer swt_version = (Integer) c.getMethod("getVersion",
						new Class[] {}).invoke(null, new Object[] {});
				message.put("swt_version", new Long(swt_version.longValue()));

				c = Class.forName("org.gudy.azureus2.ui.swt.mainwindow.MainWindow");
				if (c != null) {
					c.getMethod("addToVersionCheckMessage", new Class[] {
						Map.class
					}).invoke(null, new Object[] {
						message
					});
				}
			}
		} catch (ClassNotFoundException e) { /* ignore */
		} catch (NoClassDefFoundError er) { /* ignore */
		} catch (InvocationTargetException err) { /* ignore */
		} catch (Throwable t) {
			t.printStackTrace();
		}

		boolean using_phe = COConfigurationManager.getBooleanParameter("network.transport.encrypted.require");
		message.put("using_phe", using_phe ? new Long(1) : new Long(0));

		return message;
	}

	public static void main(String[] args) {
		try {
			COConfigurationManager.initialise();

			boolean v6 = false;

			System.out.println("UDP:  "
					+ getSingleton().getExternalIpAddressUDP(null, 0, v6));
			System.out.println("TCP:  "
					+ getSingleton().getExternalIpAddressTCP(null, 0, v6));
			System.out.println("HTTP: " + getSingleton().getExternalIpAddressHTTP(v6));

		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
