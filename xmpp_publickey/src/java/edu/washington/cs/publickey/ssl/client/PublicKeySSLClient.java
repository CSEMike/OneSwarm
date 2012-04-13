package edu.washington.cs.publickey.ssl.client;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLSocket;

import edu.washington.cs.publickey.CryptoHandler;
import edu.washington.cs.publickey.PublicKeyClient;
import edu.washington.cs.publickey.PublicKeyFriend;

public class PublicKeySSLClient extends PublicKeyClient {
	private SSLSocket socket;
	private final String server;
	private final int port;
	private final CryptoHandler cryptoHandler;
	private final byte[] expectedRemoteKey;

	public PublicKeySSLClient(File existingFriendsFile, List<byte[]> knownKeys, String server, int port, byte[] expectedRemoteKey, CryptoHandler cryptoHandler) throws UnknownHostException, IOException, Exception {
		super(existingFriendsFile, knownKeys);
		this.server = server;
		this.port = port;
		this.cryptoHandler = cryptoHandler;
		this.expectedRemoteKey = expectedRemoteKey;

	}

	@Override
	public void connect() throws Exception {
		if (socket != null) {
			throw new Exception("Already connected");
		}

		socket = (SSLSocket) cryptoHandler.getSSLContext().getSocketFactory().createSocket(server, port);

		// check the certificate
		Certificate[] remoteCerts = socket.getSession().getPeerCertificates();
		if (remoteCerts.length != 1) {
			disconnect();
			throw new Exception("Unable to get remote certificate");
		}
		//System.out.println("SSL Client: remote public key: " + Base64.encodeBytes(remoteCerts[0].getPublicKey().getEncoded(), Base64.DONT_BREAK_LINES));
		if (!Arrays.equals(expectedRemoteKey, remoteCerts[0].getPublicKey().getEncoded())) {
			disconnect();
			throw new Exception("Remote server key verification failure! Potential remote security breach or man-in-the-middle attack!!!");
		}
		//System.out.println("SSLClient connected");
	}

	@Override
	public void disconnect() throws Exception {
		if (socket != null) {
			SSLSocket localSocket = socket;
			socket = null;
			localSocket.close();
		}
	}

	@Override
	public void updateFriends() throws Exception {

		DataOutputStream out = null;
		BufferedReader in = null;
		// ok, all seems fine
		// open the output stream
		try {
			out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			/*
			 * the protocol is, write an int specifying the number of known
			 * friends then the sha1s of their public keys
			 */
			List<byte[]> knownKeySha1s = super.getKnownKeySha1s();
			out.writeInt(knownKeySha1s.size());
			for (byte[] keySha : knownKeySha1s) {
				if (keySha.length != 20) {
					throw new Exception("Key sha1 length error, must be 20! (len=" + keySha.length + ")");
				}
				out.write(keySha);
			}
			out.flush();

			// now read the response, this is gzipped xml
			in = new BufferedReader(new InputStreamReader(new GZIPInputStream(socket.getInputStream())));
			StringBuilder b = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null) {
				b.append(line + "\n");
				//System.out.println(line);
			}

			PublicKeyFriend[] newFriends = PublicKeyFriend.deserialize(b.toString());
			addKnownFriends(Arrays.asList(newFriends));

		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
			disconnect();
		}
	}
}
