package edu.washington.cs.publickey.ssl.client;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.LinkedList;

import javax.net.ssl.SSLContext;

import org.junit.Test;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import edu.washington.cs.publickey.CryptoHandler;
import edu.washington.cs.publickey.xmpp.client.PublicKeyCreator;

public class PublicKeySSLClientTest {
	static String SERVER_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDPHlPJCp9fOW+xoiGYBLDuNPdszd2wgHfM6VUUaQ3YTdL6Qtz68eQhIb53N3Z+hzgUZNF21DdFRsgAmnQyWTTgJmIf6Y4GF1sB5Mf7y4R/ombbVyvpXZ7cPjBC9BO8ciA9n18cpNBAaJjGGGt7W5c0FMEUo6VuWiLpWiTD5dl1CwIDAQAB";

	@Test
	public void testHandShake() throws UnknownHostException, IOException, Exception {
		File f = new File("/tmp/existingFriendsTest");
		PublicKeySSLClient publicKeySSLClient = new PublicKeySSLClient(f, new LinkedList<byte[]>(), "localhost", 12345, Base64.decode(SERVER_KEY), new CryptoHandler() {

			public PublicKey getPublicKey() {
				return PublicKeyCreator.getInstance().getOwnPublicKey();
			}

			public SSLContext getSSLContext() throws Exception {
				return PublicKeyCreator.getInstance().getSSLContext();
			}

			public byte[] sign(byte[] data) throws Exception {
				return null;
			}
		});
		
		publicKeySSLClient.connect();
		publicKeySSLClient.updateFriends();
	}

}
