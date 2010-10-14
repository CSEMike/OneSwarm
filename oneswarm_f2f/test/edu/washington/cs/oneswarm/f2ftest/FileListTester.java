package edu.washington.cs.oneswarm.f2ftest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.sun.corba.se.spi.orbutil.fsm.Input;

import edu.washington.cs.oneswarm.f2f.FileListManager;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;

public class FileListTester {
	private static AzureusCore core;

	public static void main(String[] args) {
		core = AzureusCoreImpl.create();
		core.start();

		new FileListTester();

		System.out.println("CLIENT: done");
		core.requestStop();
	}

	public FileListTester() {
		//try {
//			OSF2FMain main = OSF2FMain.getSingleton();
//			Friend[] friends = main.getFriendManager().getFriends();
//			// Friend f = friends[1];
			//
			// main.getOverlayManager().createOutgoingConnection(
			// new ConnectionEndpoint(new InetSocketAddress(f
			// .getLastConnectIP(), f.getLastConnectPort())), f);
//			Friend f = friends[0];
//
//			main.getOverlayManager().createOutgoingConnection(
//					new ConnectionEndpoint(new InetSocketAddress(f
//							.getLastConnectIP(), 57836)), f);
//
//			Thread.sleep(2000);
//			List<FriendConnection> conn = main.getOverlayManager()
//					.debugGetFriendConnections();
//			for (FriendConnection friendConnection : conn) {
//				friendConnection.sendFileListResponse(
//						OSF2FMessage.FILE_LIST_TYPE_DETAILED, 0);
//			}
//			Thread.sleep(600 * 1000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		//
		// FileListManager m = new FileListManager();
		// byte[] b =
		// m.getOwnFileList(OSF2FMessage.FILE_LIST_TYPE_COMPLETE,true);
		// try {
		// BufferedReader in = new BufferedReader(new InputStreamReader(new
		// GZIPInputStream(new ByteArrayInputStream(b))));
		// String line;
		// while((line=in.readLine())!= null){
		// System.out.println(line);
		// }
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		
	}
}
