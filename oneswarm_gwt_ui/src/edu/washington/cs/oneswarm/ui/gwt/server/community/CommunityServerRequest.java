package edu.washington.cs.oneswarm.ui.gwt.server.community;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.StringList;
import org.gudy.azureus2.core3.util.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sun.nio.ch.PollSelectorProvider;

import com.aelitis.azureus.core.networkmanager.impl.osssl.OneSwarmSslKeyManager;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import edu.uw.cse.netlab.utils.ByteManip;
import edu.washington.cs.oneswarm.community.CommunityConstants;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.FriendsImportCommunityServer;
import edu.washington.cs.oneswarm.ui.gwt.rpc.BackendTask;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendList;
import edu.washington.cs.oneswarm.ui.gwt.server.BackendTaskManager;
import edu.washington.cs.oneswarm.ui.gwt.server.FriendInfoLiteFactory;
import edu.washington.cs.oneswarm.ui.gwt.server.BackendTaskManager.CancellationListener;

public class CommunityServerRequest extends Thread implements CancellationListener {

	private static final int MAX_READ_BYTES = 1 * 1024 * 1024;

	private static Logger logger = Logger.getLogger(CommunityServerRequest.class.getName());

	// private String mRecord.getUrl();
	// private String user;
	// private String pw;
	// private boolean need_auth;
	// private boolean manual_confirmation;
	// private boolean prune_using_server_deletes;
	// private String group;
	private boolean polling_refresh;
	// private int pruning_threshold =
	// CommunityServerAddPanel.DEFAULT_PRUNING_THRESHOLD;

	private String base64Key;
	private boolean cancelled;
	private int mTaskID;
	private BackendTask mTask;
	private String mOurNickname;
	private String refreshInterval;

	private CommunityRecord mRecord;

	public String getRefreshInterval() {
		return refreshInterval;
	}

	public CommunityServerRequest(CommunityRecord inRecord, boolean polling_refresh) {
		mRecord = inRecord;

		setDaemon(true);
		// this.mRecord.getUrl() = url;
		// this.user = user;
		// this.pw = pw;
		// this.need_auth = need_auth;
		this.polling_refresh = polling_refresh;
		// this.manual_confirmation = manual_confirmation;
		// this.prune_using_server_deletes = sync_deletes;
		// this.group = group;
		// this.pruning_threshold = pruning_threshold;
		base64Key = Base64.encode(OneSwarmSslKeyManager.getInstance().getOwnPublicKey().getEncoded()).replaceAll("\n", "");
		mOurNickname = COConfigurationManager.getStringParameter("Computer Name", "OneSwarm user");
	}

	public void run() {

		/**
		 * First, check capabilities once per server per execution.
		 */
		check_server_capabilities();

		/**
		 * Three step: 1) get challenge, 2) send reponse, 3) parse list
		 * 
		 * Special case is when the challenge returns a not authorized -- in
		 * this case, try a registration request. If this is successful, it's a
		 * public community server and we can reissue a challenge. Otherwise,
		 * it's private and we don't have credentials.
		 */
		try {
			String sep = "?";
			if (mRecord.getUrl().indexOf('?') > -1) {
				sep = "&";
			}

			String theURLString = mRecord.getUrl() + sep + CommunityConstants.BASE64_PUBLIC_KEY + "=" + URLEncoder.encode(base64Key, "UTF-8");
			logger.fine("Requesting community friend update: " + theURLString);

			URL url = new URL(theURLString);
			HttpURLConnection conn = getConnection(url);

			if (cancelled) {
				return;
			}

			if (mTask != null) {
				mTask.setSummary("Connecting...");
			}

			if (conn.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
				logger.fine("Initial request unauthorized to: " + mRecord.getUrl());
				if (attempt_registration() == false) {
					logger.finest("Registration request unauthorized to: " + mRecord.getUrl());
					throw new IOException("Unauthorized request denied (registration failed)");
				} else {
					logger.fine("Registration request appeared to succeed, continuing...");
					conn = getConnection(url);
				}
			}

			if (cancelled) {
				return;
			}

			if (conn.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new IOException("Unauthorized request denied (registration failed)");
			}

			logger.finest("Successful connection to: " + mRecord.getUrl());
			if (mTask != null) {
				mTask.setSummary("Connected.");
			}

			BufferedReader in = new BufferedReader(new InputStreamReader(getConnectionInputStream(conn)));
			String l = in.readLine();
			if (l != null) {
				logger.finest("[" + mRecord.getUrl() + "] got: " + l);
			}

			if (l.startsWith(CommunityConstants.CHALLENGE)) {
				String[] toks = l.split("\\s+");
				if (toks.length != 2) {
					throw new IOException("Received a malformed challenge");
				}

				long challenge = Long.parseLong(toks[1]);
				logger.finest("[" + mRecord.getUrl() + "] got challenge: " + challenge);

				reissueWithResponse(challenge);
			} else {
				ByteArrayOutputStream read = new ByteArrayOutputStream();
				read.write(l.getBytes());

				mTask.setSummary("Reading response...");

				readLimitedInto(conn, MAX_READ_BYTES, read);
				processAsXML(read);
			}

			if (mTask != null) {
				if (!polling_refresh) {
					logger.finest("[" + mRecord.getUrl() + "] non-polling refresh done");
					// TODO: do something with these results
					BackendTaskManager.get().removeTask(mTaskID);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.warning(e.toString());
			if (mTask != null) {
				mTask.setGood(false);
				mTask.setSummary(e.toString());
			}
		} finally {
			// someone should pick up these results in a few seconds of polling.
			// since we can't
			// rely on the browser to remove things, we need a timeout for these
			// results here
			if (mTask != null) {
				try {
					Thread.sleep(15 * 1000); // for now, 7 seconds
				} catch (Exception e2) {
				}
				logger.fine("Removing community server task " + mTaskID + " from manager");
				BackendTaskManager.get().removeTask(mTask.getTaskID());
			}
		}
	}

	static Set<String> sCheckedCommunityServers = Collections.synchronizedSet(new HashSet<String>());
	static {
		/**
		 * This will result in a few duplicate requests, but is needed to deal
		 * with the case of removing and adding again.
		 */
		COConfigurationManager.addParameterListener("oneswarm.community.servers", new ParameterListener() {
			public void parameterChanged(String parameterName) {
				sCheckedCommunityServers.clear();
			}
		});
	}

	private void check_server_capabilities() {
		URL url;
		try {
			url = new URL(mRecord.getUrl());
			/**
			 * this might be a legacy URL of form: server/community, and we need
			 * to strip everything off to get to the capabilities file
			 */
			url = new URL(getCommunityBase(url) + "/capabilities.xml");

			if (sCheckedCommunityServers.contains(url.toString()) && System.getProperty("oneswarm.always.check.capabilities") == null) {
				logger.finest("Already checked: " + url.toString() + " , skipping");
				return;
			}
			sCheckedCommunityServers.add(url.toString());

			/**
			 * Never use auth to get the capbilities file -- in case the auth
			 * info is wrong -- we still want to get the actual capabilities!
			 */
			HttpURLConnection conn = getConnection(url, "GET", false);

			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			readLimitedInto(conn, MAX_READ_BYTES, bytes);

			if (processCapabilitiesXML(bytes)) {
				logger.fine("Synchronizing community server capabilities given server response");
				addOrUpdateCommunityServerToSettings(mRecord);
			}

		} catch (MalformedURLException e) {
			System.err.println(e);
		} catch (IOException e) {
			System.err.println(e);
		}
	}

	public static String getCommunityBase(URL url) {
		return url.getProtocol() + "://" + url.getHost() + (url.getPort() == -1 ? "" : (":" + url.getPort()));
	}

	private boolean processCapabilitiesXML(ByteArrayOutputStream bytes) {

		ByteArrayInputStream input = new ByteArrayInputStream(bytes.toByteArray());
		boolean shouldSync = false;

		try {
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer xformer = factory.newTransformer();
			Source source = new StreamSource(input);

			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.newDocument();
			Result result = new DOMResult(doc);

			xformer.transform(source, result);

			NodeList root = doc.getElementsByTagName(CommunityConstants.CAPABILITIES_ROOT);
			Node response = root.item(0);
			NodeList firstLevel = response.getChildNodes();
			for (int nodeItr = 0; nodeItr < firstLevel.getLength(); nodeItr++) {
				Node kid = firstLevel.item(nodeItr);

				if (kid == null) {
					continue;
				}

				if (kid.getLocalName() == null) {
					continue;
				}

				if (kid.getLocalName().equals(CommunityConstants.PEERS)) {

					String communityPath = kid.getAttributes().getNamedItem(CommunityConstants.PATH_ATTRIB).getTextContent();
					if (mRecord.getCommunity_path() == null) {
						mRecord.setCommunity_path(communityPath);
						shouldSync = true;
					} else if (mRecord.getCommunity_path().equals(communityPath) == false) {
						mRecord.setCommunity_path(communityPath);
						shouldSync = true;
					}
					logger.finer("got peers path from contribs.xml: " + communityPath);

				} else if (kid.getLocalName().equals(CommunityConstants.PUBLISH)) {

					String publishPath = kid.getAttributes().getNamedItem(CommunityConstants.PATH_ATTRIB).getTextContent();
					if (mRecord.getSupports_publish() == null) {
						shouldSync = true;
						mRecord.setSupports_publish(publishPath);
					} else if (mRecord.getSupports_publish().equals(publishPath) == false) {
						shouldSync = true;
						mRecord.setSupports_publish(publishPath);
					}
					mRecord.setSupports_publish(publishPath);
					logger.finer("got publish path from contribs.xml: " + publishPath);

				} else if (kid.getLocalName().equals(CommunityConstants.ID)) {

					String serverName = kid.getAttributes().getNamedItem(CommunityConstants.NAME_ATTRIB).getTextContent();
					if (mRecord.getServer_name() == null) {
						shouldSync = true;
						mRecord.setServer_name(serverName);
					} else if (mRecord.getServer_name().equals(serverName) == false) {
						shouldSync = true;
						mRecord.setServer_name(serverName);
					}
					logger.finer("got server name from contribs.xml: " + serverName);

				} else if (kid.getLocalName().equals(CommunityConstants.SPLASH)) {

					String splashPath = kid.getAttributes().getNamedItem(CommunityConstants.PATH_ATTRIB).getTextContent();
					if (mRecord.getSplash_path() == null) {
						shouldSync = true;
						mRecord.setSplash_path(splashPath);
					} else if (mRecord.getSplash_path().equals(splashPath) == false) {
						shouldSync = true;
						mRecord.setSplash_path(splashPath);
					}
					mRecord.setSplash_path(splashPath);
					logger.finer("got splash path from contribs.xml: " + splashPath);

				} else if (kid.getLocalName().equals(CommunityConstants.SKIPSSL)) {

					// we can update this value only if it's >= 0. otherwise,
					// user has indicated they always want to use
					// SSL, regardless of the potential for cert errors
					if (mRecord.getNonssl_port() >= 0) {

						int port = Integer.parseInt(kid.getAttributes().getNamedItem(CommunityConstants.PORT_ATTRIB).getTextContent());

						if (mRecord.getNonssl_port() != port) {
							mRecord.setNonssl_port(port);
							shouldSync = true;
						}
						logger.finer("got nonssl port from server: " + port);
					}

				} else if (kid.getLocalName().equals(CommunityConstants.SEARCH_FILTER)) {

					if (mRecord.isAcceptFilterList()) {

						List<String> neuKeywords = new ArrayList<String>();
						NodeList kids = kid.getChildNodes();
						for (int kidItr = 0; kidItr < kids.getLength(); kidItr++) {
							Node k = kids.item(kidItr);

							String value = null;
							try {
								value = k.getFirstChild().getNodeValue();
							} catch (Exception e) {
								e.printStackTrace();
								logger.warning("Error during parse: " + e.toString());
							}
							if (value != null) {
								for (String s : value.split("\\s+")) {
									if (s.length() >= 3) {
										neuKeywords.add(s.toLowerCase());
									}
								}
							}
						}

						if (neuKeywords.size() > 0) {
							Set<String> existing = new HashSet<String>();
							StringList out = COConfigurationManager.getStringListParameter("oneswarm.search.filter.keywords");
							for (int i = 0; i < out.size(); i++) {
								try {
									existing.add(out.get(i).split("\\s+")[0]);
								} catch (Exception e) {
									logger.warning("Error parsing search filter keywords: " + e.toString());
									e.printStackTrace();
								}
							}

							for (String neu : neuKeywords) {
								if (existing.contains(neu) == false) {
									// will have server name if it has
									// capabilities
									out.add(neu + " (" + mRecord.getServer_name() + ")");
								}
							}

							COConfigurationManager.setParameter("oneswarm.search.filter.keywords", out);

						}
					} // if( accept filter list )

				} else {
					logger.warning("Unrecognized attribute: " + kid.getLocalName());
				}
			}

		} catch (ParserConfigurationException e) {
			// couldn't even create an empty doc
			logger.warning("Exception during XML processing: " + e.toString());
		} catch (TransformerException e) {
			logger.warning("Exception during XML processing: " + e.toString());
			logger.warning("bytes: " + new String(bytes.toByteArray()));
		} catch (NullPointerException e) {
			// basically means the file had bad structure
			e.printStackTrace();
			logger.warning("Null pointer exception while processing community server capabilities XML");
		}

		if (mRecord.getCommunity_path() != null) {
			if (mRecord.getRealUrl().equals(mRecord.getBaseURL()) == false) {
				mRecord.setUrl(mRecord.getBaseURL());
				logger.fine("Updating to base URL: " + mRecord.getRealUrl() + " (comm path: " + mRecord.getCommunity_path() + " )");
				shouldSync = true;
			}
		}

		return shouldSync;
	}

	private void processAsXML(ByteArrayOutputStream read) {

		ByteArrayInputStream input = new ByteArrayInputStream(read.toByteArray());

		try {
			TransformerFactory factory = TransformerFactory.newInstance();

			Transformer xformer = factory.newTransformer();

			Source source = new StreamSource(input);

			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.newDocument();
			Result result = new DOMResult(doc);

			xformer.transform(source, result);

			NodeList root = doc.getElementsByTagName(CommunityConstants.RESPONSE_ROOT);
			Node response = root.item(0);
			NodeList firstLevel = response.getChildNodes();
			for (int i = 0; i < firstLevel.getLength(); i++) {
				Node kid = firstLevel.item(i);
				if (kid.getLocalName().equals(CommunityConstants.REFRESH_INTERVAL)) {
					refreshInterval = kid.getTextContent();
					logger.finer("got refresh interval: " + refreshInterval);
				} else if (kid.getLocalName().equals(CommunityConstants.FRIEND_LIST)) {
					// these will appear as friend notifications
					List<String[]> parsed = parseFriendList(kid);
					if (polling_refresh == false) {
						logger.finer("automatic refresh, adding to community manager");
						// for( String [] entry : parsed ) {
						CommunityServerManager.get().feed(parsed, mRecord);
						// }
					} else { // these will be requested explicitly by the client
								// in the backend task
						logger.finer("polling refresh, adding to backend task");
						FriendInfoLite[] res = new FriendInfoLite[parsed.size()];
						for (int fItr = 0; fItr < res.length; fItr++) {
							res[fItr] = FriendInfoLiteFactory.createFromKeyAndNick(parsed.get(fItr)[0], parsed.get(fItr)[1], FriendsImportCommunityServer.FRIEND_NETWORK_COMMUNITY_NAME + " " + mRecord.getUrl());
							res[fItr].setGroup(mRecord.getGroup());
						}
						FriendList out = new FriendList();
						out.setFriendList(CommunityServerManager.get().filter(res));
						mTask.setResult(out);
						mTask.setGood(true);
						mTask.setProgress("100");
						mTask.setSummary("Success");

						/**
						 * Should also add this community server to our list if
						 * it isn't already there.
						 */
						addOrUpdateCommunityServerToSettings(mRecord);
					}
				}
			}

		} catch (ParserConfigurationException e) {
			// couldn't even create an empty doc
			logger.warning("Exception during XML processing: " + e.toString());
		} catch (TransformerException e) {
			logger.warning("Exception during XML processing: " + e.toString());
		} catch (NullPointerException e) {
			// basically means the file had bad structure
			e.printStackTrace();
			logger.warning("Null pointer exception while processing community server response");
		}

	}

	public static synchronized void addOrUpdateCommunityServerToSettings(CommunityRecord inRec) {
		StringList param = COConfigurationManager.getStringListParameter("oneswarm.community.servers");
		List<String> result = new ArrayList<String>();
		for (int i = 0; i < param.size(); i++) {
			result.add(param.get(i));
		}
		Set<String> existing = new HashSet<String>();
		ArrayList<String> out = new ArrayList<String>();

		try {
			existing.add(getCommunityBase(new URL(inRec.getUrl())));
			out.addAll(Arrays.asList(inRec.toTokens()));
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			return;
		}

		for (int i = 0; i < result.size() / 5; i++) {
			CommunityRecord rec = new CommunityRecord(result, 5 * i);

			if (existing.contains(rec.getUrl())) {
				continue;
			}

			try {
				if (existing.contains(getCommunityBase(new URL(rec.getUrl())))) {
					continue;
				}
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
				continue;
			}

			URL jurl = null;
			try {
				jurl = (new URL(rec.getUrl()));
			} catch (MalformedURLException e) {
				e.printStackTrace();
				continue;
			}
			String base = getCommunityBase(jurl);
			existing.add(base);

			out.addAll(Arrays.asList(rec.toTokens()));
		}

		COConfigurationManager.setParameter("oneswarm.community.servers", (List) out);
	}

	private List<String[]> parseFriendList(Node kid) {
		List<String[]> out = new ArrayList<String[]>();
		for (int i = 0; i < kid.getChildNodes().getLength(); i++) {
			Node entry = kid.getChildNodes().item(i);
			String key = entry.getAttributes().getNamedItem(CommunityConstants.KEY_ATTRIB).getTextContent();
			String nick = entry.getAttributes().getNamedItem(CommunityConstants.NICK_ATTRIB).getTextContent();

			logger.finest("parsed: " + key + " / " + nick);

			out.add(new String[] { key, nick });
		}
		return out;
	}

	private void readLimitedInto(HttpURLConnection conn, int limit, ByteArrayOutputStream read) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(getConnectionInputStream(conn)));
		String line = null;
		while ((line = in.readLine()) != null) {
			read.write(line.getBytes());
			if (read.size() > limit) {
				return;
			}
		}
	}

	public HttpURLConnection getConnection(URL url) throws IOException {
		return getConnection(url, "GET");
	}

	public HttpURLConnection getConnection(URL url, String method) throws IOException {
		return getConnection(url, method, mRecord.isAuth_required());
	}

	public HttpURLConnection getConnection(URL url, String method, boolean useAuth) throws IOException {
		return getConnection(url, method, useAuth, mRecord, null);
	}

	public static HttpURLConnection getConnection(URL url, String method, boolean useAuth, CommunityRecord inRecord, Map<String, String> headers) throws IOException {
		HttpURLConnection conn = null;

		if (useAuth) {
			String userpass = inRecord.getUsername() + ":" + inRecord.getPw();
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("Authorization", "Basic " + (new sun.misc.BASE64Encoder()).encode(userpass.getBytes("UTF-8")));
		} else {
			conn = (HttpURLConnection) url.openConnection();
		}
		conn.setConnectTimeout(15 * 1000); // 15 second timeouts
		conn.setReadTimeout(15 * 1000);
		conn.setRequestProperty("Accepting-Encoding", "gzip");

		conn.setRequestMethod(method);

		conn.setRequestProperty("User-Agent", Constants.AZUREUS_NAME + "/" + Constants.AZUREUS_VERSION);

		if (method.equals("POST")) {
			conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
			conn.setDoInput(true);
			conn.setDoOutput(true);
		}

		if (headers != null) {
			for (String k : headers.keySet()) {
				conn.setRequestProperty(k, headers.get(k));
			}
		}

		/**
		 * SSL processing --
		 * 
		 * We do a bit of voodoo here to accept self-signed certificates. By
		 * default, we accept everything and check the sha1 hash of the server
		 * certificate against an optional parameter in the URL. If there's a
		 * mismatch after connection, we can fail with certainty.
		 * 
		 * If that parameter isn't specified, we accept the initial certificate,
		 * store the hash, and fail if it ever changes in the future on refresh.
		 */
		if (conn instanceof HttpsURLConnection) {
			logger.finest("SSL processing: " + url);

			/**
			 * First set a socket factory that will accept anything
			 */
			try {
				((HttpsURLConnection) conn).setSSLSocketFactory(OneSwarmSslKeyManager.getInstance().getSSLContext().getSocketFactory());
			} catch (Exception e) {
				e.printStackTrace();
				throw new IOException(e.getMessage());
			}

			conn.connect();

			MessageDigest digest = null;
			try {
				digest = MessageDigest.getInstance("SHA-1");
			} catch (NoSuchAlgorithmException e) {
				throw new IOException(e.getMessage());
			}

			String urlEncodedBase64CertHash = getParameters(url).get("certhash");
			byte[] expectedCertHash = null;
			String urlStr = inRecord.getUrl();
			if (urlEncodedBase64CertHash != null) {
				logger.finest("Checking URL-specified hash for: " + urlStr);
				expectedCertHash = Base64.decode(URLDecoder.decode(urlEncodedBase64CertHash, "UTF-8"));
			} else {
				/**
				 * In this case we don't have a URL-specified hash, so we first
				 * check our local history to see if this matches a previously
				 * obtained hash
				 */
				logger.finest("Checking for existing hash for: " + urlStr);
				String base64Hash = CommunityServerManager.get().getBase64CommunityServerCertificateHash(urlStr);
				if (base64Hash != null) {
					expectedCertHash = Base64.decode(base64Hash);
				}
			}

			if (expectedCertHash != null) {
				try {
					digest.update(((HttpsURLConnection) conn).getServerCertificates()[0].getEncoded());
				} catch (CertificateEncodingException e) {
					throw new IOException(e.getMessage());
				}
				if (Arrays.equals(digest.digest(), expectedCertHash) == false) {
					throw new IOException("Server certificate doesn't match expected hash");
				}
				logger.finest("Passed certificate check");
			} else {
				/**
				 * In this case we haven't encountered this server before and
				 * there isn't an expected hash in the URL. Accept and store the
				 * provided hash for future use
				 */
				try {
					digest.update(((HttpsURLConnection) conn).getServerCertificates()[0].getEncoded());
					byte[] hash = digest.digest();
					CommunityServerManager.get().trustCommunityServerCertificateHash(urlStr, Base64.encode(hash));
					logger.info("Added community server hash: " + Base64.encode(hash) + " for " + urlStr);
				} catch (CertificateEncodingException e) {
					throw new IOException(e.getMessage());
				}
			}
		}

		return conn;
	}

	private static Map<String, String> getParameters(URL inURL) {
		Map<String, String> pmap = new HashMap<String, String>();
		if (inURL.getQuery() == null) {
			return pmap;
		}
		String[] params = inURL.getQuery().split("&");

		for (String t : params) {
			String[] kv = t.split("=");
			if (kv.length == 2) {
				pmap.put(kv[0], kv[1]);
			} else {
				pmap.put(kv[0], null);
			}
		}
		return pmap;
	}

	private InputStream getConnectionInputStream(HttpURLConnection conn) throws IOException {
		if (conn.getHeaderField("Content-Encoding") != null) {
			if (conn.getHeaderField("Content-Encoding").contains("gzip")) {
				logger.finest("using gzip encoding " + conn.getURL().toString());
				return new GZIPInputStream(conn.getInputStream());
			}
		}
		return conn.getInputStream();
	}

	private boolean attempt_registration() {
		try {

			if (mTask != null) {
				mTask.setSummary("Attempting registration...");
			}

			Map<String, String> requestHeaders = new HashMap<String, String>();
			Map<String, String> formParams = new HashMap<String, String>();

			formParams.put(CommunityConstants.BASE64_PUBLIC_KEY, base64Key);
			formParams.put(CommunityConstants.NICKNAME, mOurNickname);

			URL url = new URL(mRecord.getUrl());
			HttpURLConnection conn = getConnection(url, "POST");

			for (String head : requestHeaders.keySet()) {
				conn.setRequestProperty(head, requestHeaders.get(head));
			}

			// add url form parameters
			OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());

			Iterator<String> params = formParams.keySet().iterator();
			while (params.hasNext()) {
				String name = params.next();
				String value = formParams.get(name);

				out.append(URLEncoder.encode(name, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8"));
				if (params.hasNext()) {
					out.append("&");
				}
			}

			out.flush();

			BufferedReader in = new BufferedReader(new InputStreamReader(getConnectionInputStream(conn)));
			String line = null;
			while ((line = in.readLine()) != null) {
				// System.out.println("resp line: " + line);
				;
			}

			in.close();

			logger.fine("final status code: " + conn.getResponseCode() + " / " + conn.getResponseMessage());
			return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	private void reissueWithResponse(long challenge) {
		try {

			byte[] encrypted_response = null;

			synchronized (OneSwarmSslKeyManager.getInstance()) {
				encrypted_response = OneSwarmSslKeyManager.getInstance().sign(ByteManip.ltob(challenge + 1));
			}

			if (cancelled) {
				return;
			}

			if (mTask != null) {
				mTask.setSummary("Challenge/response...");
			}

			String sep = "?";
			if (mRecord.getUrl().indexOf('?') > -1) {
				sep = "&";
			}
			String urlStr = mRecord.getUrl() + sep + CommunityConstants.BASE64_PUBLIC_KEY + "=" + URLEncoder.encode(base64Key, "UTF-8") + "&" + CommunityConstants.CHALLENGE_RESPONSE + "=" + URLEncoder.encode(Base64.encode(encrypted_response), "UTF-8");
			// System.out.println("url str: " + urlStr);
			URL url = new URL(urlStr);
			HttpURLConnection conn = getConnection(url);

			if (cancelled) {
				return;
			}

			ByteArrayOutputStream bytes = new ByteArrayOutputStream();

			mTask.setSummary("Reading response...");

			readLimitedInto(conn, MAX_READ_BYTES, bytes);
			processAsXML(bytes);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void cancelled(int inID) {
		cancelled = true;
	}

	public void setTaskID(int taskID) {
		mTaskID = taskID;
		mTask = BackendTaskManager.get().getTask(mTaskID);
	}

	/**
	 * Test parse of a capabilities file.
	 */
	public static final void main(String[] args) throws Exception {

		ByteArrayOutputStream scratch = new ByteArrayOutputStream();
		RandomAccessFile fr = new RandomAccessFile("capabilities.xml", "r");

		byte[] b = new byte[(int) fr.length()];
		fr.read(b);

		scratch.write(b);

		System.out.println("read: ");
		System.out.println(scratch.toString("UTF8"));

		CommunityRecord rec = new CommunityRecord(null, null, null, null, false, true, false, true, 0, null, null, null, null, false, 0, false);
		CommunityServerRequest r = new CommunityServerRequest(rec, true);

		r.processCapabilitiesXML(scratch);

	}

	public ArrayList<String> getCategories() throws IOException {
		try {
			HttpURLConnection conn = (HttpURLConnection) getConnection(new URL(mRecord.getBaseURL() + "/categories.xml"));

			conn.setReadTimeout(1000);

			if (conn.getResponseCode() != HttpServletResponse.SC_OK) {
				logger.warning("Community server doesn't have categories.xml " + conn.getResponseCode() + " " + mRecord.toString());
				return null;
			}

			ArrayList<String> outCategories = new ArrayList<String>();
			ByteArrayOutputStream xmlBytes = new ByteArrayOutputStream();
			readLimitedInto(conn, 8 * 1024, xmlBytes);

			ByteArrayInputStream input = new ByteArrayInputStream(xmlBytes.toByteArray());

			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer xformer = factory.newTransformer();
			Source source = new StreamSource(input);

			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.newDocument();
			Result result = new DOMResult(doc);

			xformer.transform(source, result);

			NodeList root = doc.getElementsByTagName("categories");
			Node resp = root.item(0);
			NodeList firstLevel = resp.getChildNodes();
			for (int i = 0; i < firstLevel.getLength(); i++) {
				Node kid = firstLevel.item(i);

				if (kid == null) {
					continue;
				}
				if (kid.getNodeName() == null) {
					continue;
				}
				if (kid.getNodeName().equals("category")) {
					outCategories.add(kid.getAttributes().getNamedItem("name").getNodeValue());
				}
			}

			return outCategories;
		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException(e.toString());
		}
	}
}
