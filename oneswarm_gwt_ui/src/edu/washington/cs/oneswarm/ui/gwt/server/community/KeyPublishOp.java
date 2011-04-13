package edu.washington.cs.oneswarm.ui.gwt.server.community;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.aelitis.azureus.core.networkmanager.impl.osssl.OneSwarmSslKeyManager;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import edu.uw.cse.netlab.utils.ByteManip;
import edu.washington.cs.oneswarm.community.CommunityConstants;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.FriendsImportCommunityServer;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendList;
import edu.washington.cs.oneswarm.ui.gwt.server.BackendTaskManager;
import edu.washington.cs.oneswarm.ui.gwt.server.FriendInfoLiteFactory;

/**
 * Publishes our local public key to the given community server and processes the list of keys in
 * the response.
 */
public class KeyPublishOp extends CommunityServerOperation {

	String refreshInterval;
	private final String base64Key;
	private final boolean polling_refresh;
	private final String mOurNickname;

	public KeyPublishOp(CommunityRecord inRecord, boolean polling_refresh) {
		super(inRecord);

		this.polling_refresh = polling_refresh;
		mOurNickname = COConfigurationManager.getStringParameter("Computer Name", "OneSwarm user");
		base64Key = Base64.encode(
				OneSwarmSslKeyManager.getInstance().getOwnPublicKey().getEncoded()).replaceAll(
				"\n", "");
	}

	private static Logger logger = Logger.getLogger(KeyPublishOp.class.getName());

	@Override
	void doOp() {
		/**
		 * First, check capabilities once per server per execution.
		 */
		check_server_capabilities();

		/**
		 * Three step: 1) get challenge, 2) send reponse, 3) parse list
		 * 
		 * Special case is when the challenge returns a not authorized -- in this case, try a
		 * registration request. If this is successful, it's a public community server and we can
		 * reissue a challenge. Otherwise, it's private and we don't have credentials.
		 */
		try {
			String sep = "?";
			if (mRecord.getUrl().indexOf('?') > -1) {
				sep = "&";
			}

			String theURLString = mRecord.getUrl() + sep + CommunityConstants.BASE64_PUBLIC_KEY
					+ "=" + URLEncoder.encode(base64Key, "UTF-8");
			logger.info("Requesting community friend update: " + theURLString);

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

			BufferedReader in = new BufferedReader(new InputStreamReader(
					getConnectionInputStream(conn)));
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

				out.append(URLEncoder.encode(name, "UTF-8") + "="
						+ URLEncoder.encode(value, "UTF-8"));
				if (params.hasNext()) {
					out.append("&");
				}
			}

			out.flush();

			BufferedReader in = new BufferedReader(new InputStreamReader(
					getConnectionInputStream(conn)));
			String line = null;
			while ((line = in.readLine()) != null) {
				// System.out.println("resp line: " + line);
				;
			}

			in.close();

			logger.fine("final status code: " + conn.getResponseCode() + " / "
					+ conn.getResponseMessage());
			return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
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
							res[fItr] = FriendInfoLiteFactory.createFromKeyAndNick(
									parsed.get(fItr)[0], parsed.get(fItr)[1],
									FriendsImportCommunityServer.FRIEND_NETWORK_COMMUNITY_NAME
											+ " " + mRecord.getUrl());
							res[fItr].setGroup(mRecord.getGroup());
						}
						FriendList out = new FriendList();
						out.setFriendList(CommunityServerManager.get().filter(res));
						mTask.setResult(out);
						mTask.setGood(true);
						mTask.setProgress("100");
						mTask.setSummary("Success");

						/**
						 * Should also add this community server to our list if it isn't already
						 * there.
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

	private void reissueWithResponse(long challenge) {
		try {

			byte[] encrypted_response = null;

			synchronized (OneSwarmSslKeyManager.getInstance()) {
				encrypted_response = OneSwarmSslKeyManager.getInstance().sign(
						ByteManip.ltob(challenge + 1));
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
			String urlStr = mRecord.getUrl() + sep + CommunityConstants.BASE64_PUBLIC_KEY + "="
					+ URLEncoder.encode(base64Key, "UTF-8") + "&"
					+ CommunityConstants.CHALLENGE_RESPONSE + "="
					+ URLEncoder.encode(Base64.encode(encrypted_response), "UTF-8");
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

	public String getRefreshInterval() {
		return refreshInterval;
	}

	private List<String[]> parseFriendList(Node kid) {
		List<String[]> out = new ArrayList<String[]>();
		for (int i = 0; i < kid.getChildNodes().getLength(); i++) {
			Node entry = kid.getChildNodes().item(i);
			String key = entry.getAttributes().getNamedItem(CommunityConstants.KEY_ATTRIB)
					.getTextContent();
			String nick = entry.getAttributes().getNamedItem(CommunityConstants.NICK_ATTRIB)
					.getTextContent();

			logger.finest("parsed: " + key + " / " + nick);

			out.add(new String[] { key, nick });
		}
		return out;
	}
}
