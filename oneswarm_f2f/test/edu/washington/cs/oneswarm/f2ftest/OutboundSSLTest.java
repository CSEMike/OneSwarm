package edu.washington.cs.oneswarm.f2ftest;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;

public class OutboundSSLTest {

	private static AzureusCore core;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		core = AzureusCoreImpl.create();
		core.start();

		new OutboundSSLTest();

		System.out.println("CLIENT: done");
		core.requestStop();

	}

	public OutboundSSLTest() {
		// OSF2FMain main = OSF2FMain.getSingleton();
		// Friend[] f = main.getFriendManager().getFriends();
		// try {
		// main.getOverlayManager().createOutgoingConnection(
		// new ConnectionEndpoint(new InetSocketAddress(InetAddress
		// .getByName("127.0.0.1"), 57836)), f[0]);
		// Thread.sleep(10000);
		// } catch (UnknownHostException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		//		}
	}
}
