package edu.washington.cs.publickey.xmpp.client;

import java.io.File;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.washington.cs.publickey.CryptoHandler;
import edu.washington.cs.publickey.PublicKeyClient;
import edu.washington.cs.publickey.PublicKeyFriend;
import edu.washington.cs.publickey.xmpp.XMPPNetwork;

public class PublicKeyXmppClientTest {
    PublicKeyClient client;
    String netUid = "tomas.isdal2@gmail.com";

    @Before
    public void setUp() throws Exception {
	PublicKeyCreator.keystoreFileName = "keystore29";

	client = new PublicKeyXmppClient(new File("/tmp/knownfriends" + netUid.split("@")[0]), new LinkedList<byte[]>(), XMPPNetwork.GTALK, netUid, new char[] { 'a', 'b', 'c', '1', '2', '3', '4', '5' }, PublicKeyCreator.keystoreFileName, new CryptoHandler() {
	    public byte[] sign(byte[] data) throws Exception {
		System.out.println("signing");
		return PublicKeyCreator.getInstance().sign(data);
	    }

	    public SSLContext getSSLContext() throws Exception {
		return null;
	    }

	    public PublicKey getPublicKey() {
		return PublicKeyCreator.getInstance().getOwnPublicKey();
	    }
	});
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testConnect() {
	try {
	    client.connect();
	    Thread.sleep(1000);
	    client.disconnect();
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    @Test
    public void testGetKeys() {
	try {

	    client.connect();
	    client.updateFriends();
	    List<PublicKeyFriend> friends = client.getFriends();
	    System.out.println("got " + friends.size());
	    for (PublicKeyFriend f : friends) {
		System.out.println(f + "");
	    }
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }
}
