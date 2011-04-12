package edu.washington.cs.oneswarm.ui.gwt.server.community;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONTokener;
import org.json.JSONWriter;

import com.google.common.base.Preconditions;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import edu.washington.cs.oneswarm.f2f.dht.CHTCallback;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;

public class CHTGetOp extends CommunityServerOperation {

	private static Logger logger = Logger.getLogger(CHTGetOp.class.getName());

	private final List<CHTCallback> callbacks;
	private final List<byte[]> keys;

	public CHTGetOp(CommunityRecord server, List<byte[]> keys, List<CHTCallback> callbacks) {
		super(server);

		Preconditions.checkArgument(keys.size() == callbacks.size(),
				"Mismatch between keys and callbacks sizese.");

		this.keys = keys;
		this.callbacks = callbacks;
	}

	@Override
	void doOp() {
		Preconditions.checkState(mRecord.isAllowAddressResolution(),
				"Attempting CHTGet on server without perms: " + mRecord.getBaseURL());

		String path = mRecord.getBaseURL();
		if (path.endsWith("/") == false) {
			path += "/";
		}
		path += mRecord.getCht_path() + "?get";
		try {
			URL url = new URL(path);
			HttpURLConnection conn = getConnection(url, "POST");

			// JSON array of key, value pairs.
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(baos);
			JSONWriter writer = new JSONWriter(outputStreamWriter);

			writer.array();
			for (int i = 0; i < keys.size(); i++) {
				String encodedKey = Base64.encode(keys.get(i));
				writer.value(encodedKey);
			}
			writer.endArray();
			outputStreamWriter.flush();

			System.out.println(baos.toString() + "\n");

			conn.getOutputStream().write(
					("q=" + URLEncoder.encode(baos.toString(), "UTF-8")).getBytes());

			System.out.println("CHT get response code: " + conn.getResponseCode() + " / "
					+ conn.getResponseMessage());

			// Collect and parse the get responses
			ByteArrayOutputStream response = new ByteArrayOutputStream();
			readLimitedInto(conn, 1024 * 1024, response);

			JSONTokener toks = new JSONTokener(new InputStreamReader(new ByteArrayInputStream(
					response.toByteArray())));
			JSONArray vals = (JSONArray) toks.nextValue();
			Preconditions.checkState(vals.length() == callbacks.size(),
					"Server returned " + vals.length() + " values, but we have " + callbacks.size()
							+ " CHT get callbacks.");
			for (int i = 0; i < vals.length(); i++) {
				CHTCallback callback = callbacks.remove(0);
				callback.valueReceived(keys.get(i), Base64.decode(vals.getString(i)));
			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.warning("Error during CHT get on server: " + mRecord.getBaseURL() + " / "
					+ e.toString());

			for (CHTCallback c : callbacks) {
				c.errorReceived(e);
			}
		}
	}
}

