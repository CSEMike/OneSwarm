package edu.washington.cs.oneswarm.f2ftest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;

import com.aelitis.azureus.core.networkmanager.impl.osssl.OneSwarmSslKeyManager;


public class SSLServerBlocking {
	/**
	 * @param args
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws CertificateException
	 */
	public static void main(String[] args) throws CertificateException,
			IOException, InterruptedException {
		try {
			new SSLServerBlocking();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public SSLServerBlocking() throws NoSuchAlgorithmException,
			KeyStoreException, UnrecoverableKeyException,
			KeyManagementException, CertificateException, IOException,
			InterruptedException {

		// Create SSL context.
		SSLContext sslcontext = OneSwarmSslKeyManager.getInstance().getSSLContext();

		try {
			int port = 8765;
			ServerSocketFactory ssocketFactory = sslcontext
					.getServerSocketFactory();

			SSLServerSocket ssocket = (SSLServerSocket) ssocketFactory
					.createServerSocket(port);
			// force the client to send it's cert to
			ssocket.setNeedClientAuth(true);
			// Listen for connections
			Socket socket;
			while ((socket = ssocket.accept()) != null) {

				// Create streams to securely send and receive data to the
				// client
				BufferedReader in = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				OutputStream out = socket.getOutputStream();

				out.write("12345 tomas\n".getBytes());
				out.flush();

				String line;
				while ((line = in.readLine()) != null) {
					System.out.println(line);

				}
				Thread.sleep(10000);
				in.close();
				out.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
