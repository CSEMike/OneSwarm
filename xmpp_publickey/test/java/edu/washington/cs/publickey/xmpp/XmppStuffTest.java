package edu.washington.cs.publickey.xmpp;

import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jivesoftware.smack.util.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.washington.cs.publickey.Tools;

public class XmppStuffTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetListSha1() {
	try {
	    String[] users = { "tomas.isdal@gmail.com/smack15", "publickey.cs.washington.edu@gmail.com", "tomas.isdal3@gmail.com" };
	    List<byte[]> sha1List = new LinkedList<byte[]>();
	    for (String user : users) {
		byte[] sha1 = Tools.getSha1(user);
		System.out.println(user + "->" + Base64.encodeBytes(sha1));
		sha1List.add(sha1);
	    }

	    String encoded = Tools.mergeSha1sAndBase64(sha1List);
	    List<byte[]> decoded = Tools.getListSha1(encoded);

	    for (int i = 0; i < decoded.size(); i++) {
		if (!Arrays.equals(decoded.get(i), sha1List.get(i))) {
		    fail("no match: " + Base64.encodeBytes(decoded.get(i)) + "!=" + Base64.encodeBytes(sha1List.get(i)));
		} else {
		    System.out.println("match: " + Base64.encodeBytes(decoded.get(i)));
		}
	    }
	} catch (NoSuchAlgorithmException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (UnsupportedEncodingException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

}
