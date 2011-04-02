package edu.washington.cs.oneswarm.ui.gwt.server.community;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

import com.aelitis.azureus.core.networkmanager.impl.osssl.OneSwarmSslKeyManager;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import edu.washington.cs.oneswarm.community.CommunityConstants;
import edu.washington.cs.oneswarm.ui.gwt.rpc.BackendTask;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;
import edu.washington.cs.oneswarm.ui.gwt.server.BackendTaskManager;
import edu.washington.cs.oneswarm.ui.gwt.server.BackendTaskManager.CancellationListener;

public abstract class CommunityServerOperation extends Thread implements CancellationListener {

	static final int MAX_READ_BYTES = 1 * 1024 * 1024;

	private static Logger logger = Logger.getLogger(CommunityServerOperation.class.getName());


	boolean cancelled;
	int mTaskID;
	BackendTask mTask;
	private final String mOurNickname;

	final CommunityRecord mRecord;

	public CommunityServerOperation(CommunityRecord inRecord) {
		mRecord = inRecord;

		setDaemon(true);
		mOurNickname = COConfigurationManager.getStringParameter("Computer Name", "OneSwarm user");
	}

	abstract void doOp();

	@Override
	public void run() {

		doOp();

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

	void check_server_capabilities() {
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

	boolean processCapabilitiesXML(ByteArrayOutputStream bytes) {

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

				if (kid.getLocalName().equals(CommunityConstants.CHT)) {

					String chtPath = kid.getAttributes()
							.getNamedItem(CommunityConstants.PATH_ATTRIB).getTextContent();
					if (mRecord.getCht_path() == null) {
						mRecord.setCht_path(chtPath);
						shouldSync = true;
					} else if (mRecord.getCht_path().equals(chtPath) == false) {
						mRecord.setCht_path(chtPath);
						shouldSync = true;
					}
					logger.finer("got cht path from contribs.xml: " + chtPath);

				} else if (kid.getLocalName().equals(CommunityConstants.PEERS)) {

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

		COConfigurationManager.setParameter("oneswarm.community.servers", out);
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

	void readLimitedInto(HttpURLConnection conn, int limit, ByteArrayOutputStream read)
			throws IOException {
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

	InputStream getConnectionInputStream(HttpURLConnection conn) throws IOException {
		if (conn.getHeaderField("Content-Encoding") != null) {
			if (conn.getHeaderField("Content-Encoding").contains("gzip")) {
				logger.finest("using gzip encoding " + conn.getURL().toString());
				return new GZIPInputStream(conn.getInputStream());
			}
		}
		return conn.getInputStream();
	}



	public void cancelled(int inID) {
		cancelled = true;
	}

	public void setTaskID(int taskID) {
		mTaskID = taskID;
		mTask = BackendTaskManager.get().getTask(mTaskID);
	}

	public ArrayList<String> getCategories() throws IOException {
		try {
			HttpURLConnection conn = getConnection(new URL(mRecord.getBaseURL() + "/categories.xml"));

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
