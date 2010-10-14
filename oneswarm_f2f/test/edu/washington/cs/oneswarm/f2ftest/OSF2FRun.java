package edu.washington.cs.oneswarm.f2ftest;

import java.net.InetSocketAddress;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.ipc.IPCInterface;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.OSF2FMain;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;
import edu.washington.cs.oneswarm.plugins.PluginCallback;

public class OSF2FRun {
	private static AzureusCore core;

	public static void main(String[] args) {
		COConfigurationManager.addParameterListener("Max Upload Speed KBs",
				new ParameterListener() {

					public void parameterChanged(String parameterName) {
						System.out
								.println("upload speed changed: new speed "
										+ COConfigurationManager
												.getIntParameter("Max Upload Speed KBs"));
						// COConfigurationManager.setParameter("Max Upload Speed
						// KBs", 500);

					}
				});
		core = AzureusCoreImpl.create();

		core.start();
		COConfigurationManager.setParameter("TCP.Listen.Port", 12345);
		COConfigurationManager.setParameter("Max Upload Speed KBs", 50);
		COConfigurationManager.setParameter("Auto Upload Speed Enabled", 0);
		COConfigurationManager.setParameter("Auto Upload Speed Debug Enabled",
				0);
		COConfigurationManager.setParameter("AutoSpeed Available", 0);
		COConfigurationManager.setParameter("LAN Speed Enabled", 0);

		new OSF2FRun();

		System.out.println("CLIENT: done");
		core.requestStop();
	}

	public OSF2FRun() {
		try {
			IPCInterface ipc = core.getPluginManager().getPluginInterfaceByID(
					"osf2f").getIPC();
			Integer res = (Integer) ipc.invoke("add", new Object[] { 5, 7 });
			System.out.println("res=" + res);
			ipc.invoke("sendFileListRequest", new Object[] { new byte[10],
					new PluginCallback<byte[]>() {
						public void dataRecieved(long bytes) {
						}

						public void progressUpdate(int progress) {
						}

						public void requestCompleted(byte[] data) {
							System.out.println("got callback: " + data.length);
						}

						public void errorOccured(String error) {
							// TODO Auto-generated method stub

						}
					} });
		} catch (AzureusCoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IPCException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// Friend[] friends = main.getFriendManager().getFriends();
		// for (int i = 0; i < friends.length; i++) {
		// Friend f = friends[i];
		// main.getOverlayManager().createOutgoingConnection(
		// new ConnectionEndpoint(new InetSocketAddress(f
		// .getLastConnectIP(), f.getLastConnectPort())), f);
		// }
		try {
			System.out.println("sleeping");
			Thread.sleep(1000000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
