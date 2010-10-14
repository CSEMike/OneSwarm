package edu.washington.cs.oneswarm.f2ftest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.aelitis.azureus.core.networkmanager.impl.osssl.OneSwarmSslKeyManager;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelReset;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearchResp;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FSearchCancel;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FTextSearchResp;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHandshake;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessageFactory;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMetaInfoReq;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMetaInfoResp;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;

public class OSF2FMessagesTester {

	private static AzureusCore core;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		core = AzureusCoreImpl.create();
		core.start();
		// OSF2FMain.getSingleton();
		try {
			new OSF2FMessagesTester();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("CLIENT: done");
		core.requestStop();

	}

	public OSF2FMessagesTester() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException, UnrecoverableKeyException, IOException, InterruptedException {
		int port = 31935;// 34722;
		String hostname = "127.0.0.1";
		// String hostname = "84.55.67.11";

		// Create SSL context.
		SSLContext sslcontext = OneSwarmSslKeyManager.getInstance().getSSLContext();

		SSLSocketFactory socketFactory = sslcontext.getSocketFactory();

		try {
			Thread.sleep(2000);
			boolean doOut = true;
			if (doOut) {
				Socket socket = socketFactory.createSocket(hostname, port);
				OutputStream out = socket.getOutputStream();

				// test the handshake
				writeHandshake(out);
				// Thread.sleep(1000);

				writeSearch(out);

				writeSearchCancel(out);

				// test channel setup
				writeChannelSetup(out);
				// Thread.sleep(1000);

				// test channel msg
				writeChannelMsg(out);
				// Thread.sleep(1000);

				// test channel rst
				writeChannelRst(out);
				// Thread.sleep(1000);

				// test filelist request
				// writeFilelistReq(out);
				// Thread.sleep(1000);

				// test filelist resp
				writeFilelistResp(out);
				// Thread.sleep(1000);

				// test filelist resp
				writeTorrentReq(out);
				// Thread.sleep(1000);

				// test filelist resp
				writeTorrentResp(out);
				// Thread.sleep(1000);

				Thread.sleep(1000);
				out.close();

				socket.close();

			}
		} catch (java.net.SocketException e) {
			if (e.getMessage().contains("Connection reset")) {
				System.out.println("CLIENT: connection closed");
			} else {
				e.printStackTrace();
			}
		} catch (IOException e) {

			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Thread.sleep(10000);
	}

	private void writeHandshake(OutputStream out) throws IOException {
		DirectByteBuffer[] handshake = OSF2FMessageFactory.createOSF2FRawMessage(new OSF2FHandshake((byte) 1, new byte[8])).getRawData();
		ByteBuffer buf = ByteBuffer.allocate(2000);
		for (int i = 0; i < handshake.length; i++) {
			buf.put(handshake[i].getBuffer(DirectByteBuffer.SS_MSG));
		}
		buf.flip();
		System.out.println("CLIENT: writing handshake, " + buf.remaining() + " bytes");
		byte[] data = new byte[buf.remaining()];
		buf.get(data);
		out.write(data);

	}

	private void writeSearch(OutputStream out) throws IOException {
		DirectByteBuffer[] data = OSF2FMessageFactory.createOSF2FRawMessage(new OSF2FHashSearch((byte) 1, 0x4949494, 0x10101010)).getRawData();
		ByteBuffer buf = ByteBuffer.allocate(100);
		for (int i = 0; i < data.length; i++) {
			buf.put(data[i].getBuffer(DirectByteBuffer.SS_MSG));
		}
		buf.flip();
		System.out.println("CLIENT: writing search, " + buf.remaining() + " bytes");
		byte[] b = new byte[buf.remaining()];
		buf.get(b);
		out.write(b);

	}

	private void writeChannelSetup(OutputStream out) throws IOException {
		DirectByteBuffer[] data = OSF2FMessageFactory.createOSF2FRawMessage(new OSF2FHashSearchResp((byte) 1, 0x4949494, 0x10101010, 123456)).getRawData();
		ByteBuffer buf = ByteBuffer.allocate(100);
		for (int i = 0; i < data.length; i++) {
			buf.put(data[i].getBuffer(DirectByteBuffer.SS_MSG));
		}
		buf.flip();
		System.out.println("CLIENT: writing writeChannelSetup, " + buf.remaining() + " bytes");
		byte[] b = new byte[buf.remaining()];
		buf.get(b);
		out.write(b);

	}

	private void writeChannelMsg(OutputStream out) throws IOException {
		DirectByteBuffer dbuffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, 16 * 1024);
		while (dbuffer.hasRemaining(DirectByteBuffer.SS_NET)) {
			dbuffer.put(DirectByteBuffer.SS_NET, (byte) 17);
		}
		dbuffer.flip(DirectByteBuffer.SS_NET);

		DirectByteBuffer[] data = OSF2FMessageFactory.createOSF2FRawMessage(new OSF2FChannelDataMsg((byte) 1, 0x4949494, dbuffer)).getRawData();
		ByteBuffer buf = ByteBuffer.allocate(17000);
		for (int i = 0; i < data.length; i++) {
			buf.put(data[i].getBuffer(DirectByteBuffer.SS_MSG));
		}
		buf.flip();
		System.out.println("CLIENT: writing writeChannelMsg, " + buf.remaining() + " bytes");
		byte[] b = new byte[buf.remaining()];
		buf.get(b);
		out.write(b);

	}

	private void writeChannelRst(OutputStream out) throws IOException {
		DirectByteBuffer[] data = OSF2FMessageFactory.createOSF2FRawMessage(new OSF2FChannelReset((byte) 1, 0x4949494)).getRawData();
		ByteBuffer buf = ByteBuffer.allocate(20);
		for (int i = 0; i < data.length; i++) {
			buf.put(data[i].getBuffer(DirectByteBuffer.SS_MSG));
		}
		buf.flip();
		System.out.println("CLIENT: writing writeChannelReset, " + buf.remaining() + " bytes");
		byte[] b = new byte[buf.remaining()];
		buf.get(b);
		out.write(b);
	}

	// private void writeFilelistReq(OutputStream out) throws IOException {
	// DirectByteBuffer[] data = OSF2FMessageFatory.createOSF2FRawMessage(
	// new OSF2FFilelistReq((byte) 1, 0,
	// OSF2FMessage.FILE_LIST_TYPE_DETAILED)).getRawData();
	// ByteBuffer buf = ByteBuffer.allocate(20);
	// for (int i = 0; i < data.length; i++) {
	// buf.put(data[i].getBuffer(DirectByteBuffer.SS_MSG));
	// }
	// buf.flip();
	// System.out.println("CLIENT: writing writeFilelistReq, "
	// + buf.remaining() + " bytes");
	// byte[] b = new byte[buf.remaining()];
	// buf.get(b);
	// out.write(b);
	//
	// }

	private void writeSearchCancel(OutputStream out) throws IOException {
		DirectByteBuffer[] data = OSF2FMessageFactory.createOSF2FRawMessage(new OSF2FSearchCancel((byte) 1, 0)).getRawData();
		ByteBuffer buf = ByteBuffer.allocate(40);
		for (int i = 0; i < data.length; i++) {
			buf.put(data[i].getBuffer(DirectByteBuffer.SS_MSG));
		}
		buf.flip();
		System.out.println("CLIENT: writing searchCancel, " + buf.remaining() + " bytes");
		byte[] b = new byte[buf.remaining()];
		buf.get(b);
		out.write(b);
	}

	private void writeFilelistResp(OutputStream out) throws IOException {
		DirectByteBuffer[] data = OSF2FMessageFactory.createOSF2FRawMessage(new OSF2FTextSearchResp((byte) 1, OSF2FMessage.FILE_LIST_TYPE_COMPLETE, 0, 0, new byte[0])).getRawData();
		ByteBuffer buf = ByteBuffer.allocate(1020);
		for (int i = 0; i < data.length; i++) {
			buf.put(data[i].getBuffer(DirectByteBuffer.SS_MSG));
		}
		buf.flip();
		System.out.println("CLIENT: writing writeFilelistResp, " + buf.remaining() + " bytes");
		byte[] b = new byte[buf.remaining()];
		buf.get(b);
		out.write(b);

	}

	private void writeTorrentReq(OutputStream out) throws IOException {
		DirectByteBuffer[] data = OSF2FMessageFactory.createOSF2FRawMessage(new OSF2FMetaInfoReq((byte) 1, 0, OSF2FMessage.METAINFO_TYPE_BITTORRENT, 0, new byte[20])).getRawData();
		ByteBuffer buf = ByteBuffer.allocate(40);
		for (int i = 0; i < data.length; i++) {
			buf.put(data[i].getBuffer(DirectByteBuffer.SS_MSG));
		}
		buf.flip();
		System.out.println("CLIENT: writing writeTorrentReq, " + buf.remaining() + " bytes");
		byte[] b = new byte[buf.remaining()];
		buf.get(b);
		out.write(b);

	}

	private void writeTorrentResp(OutputStream out) throws IOException {
		DirectByteBuffer[] data = OSF2FMessageFactory.createOSF2FRawMessage(new OSF2FMetaInfoResp((byte) 1, 0, OSF2FMessage.METAINFO_TYPE_BITTORRENT, 0, 0, 0, new byte[0])).getRawData();
		ByteBuffer buf = ByteBuffer.allocate(1020);
		for (int i = 0; i < data.length; i++) {
			buf.put(data[i].getBuffer(DirectByteBuffer.SS_MSG));
		}
		buf.flip();
		System.out.println("CLIENT: writing writeTorrentResp, " + buf.remaining() + " bytes");
		byte[] b = new byte[buf.remaining()];
		buf.get(b);
		out.write(b);

	}
}
