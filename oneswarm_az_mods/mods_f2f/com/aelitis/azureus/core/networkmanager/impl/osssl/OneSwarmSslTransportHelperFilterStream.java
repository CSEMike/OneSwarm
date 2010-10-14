package com.aelitis.azureus.core.networkmanager.impl.osssl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.logging.Logger;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;

import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.networkmanager.impl.TransportHelper;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelperFilterStream;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelper.selectListener;

public class OneSwarmSslTransportHelperFilterStream
	extends TransportHelperFilterStream
{
	public enum SslHandShakeMatch {
		NOT_ENOUGH_BYTES, NO_SSL, SSL_CLIENT_CERT, SSL_NO_CLIENT_CERT;
	};

	private static Logger									 logger											 = Logger.getLogger(OneSwarmSslTransportHelperFilterStream.class.getName());

	public static final String							SSL_NAME										 = "OneSwarm SSL: ";

	//private final static boolean						logToStdOut									= true;

	public static final String							REMOTE_SSL_PUBLIC_KEY				= "remote_ssl_public_key";

	public static final byte[]							ANY_KEY_ACCEPTED_BYTES			 = {
		1,
		2,
		3,
		4,
		5,
		6,
		7,
		8,
		9,
		0
																																			 };

	public static int											 SSL_APP_BUFFER_SIZE					= 33320;

	public static int											 SSL_NET_BUFFER_SIZE					= 33320;

	public final static int								 SSL_HEADER_MIN_LENGTH				= 2 + 1
																																					 + 2
																																					 + 2
																																					 + 2
																																					 + 2;

	//public final static int								 SSL_HEADER_MIN_LENGTH = 100;

	private final SslHandShakeMatch				 incomingHandshake;

	private SSLEngineResult.HandshakeStatus handshakeStatus;

	private ByteBuffer											handshakeBuffer;

	private ByteBuffer											decryptedDataForApp;

	private DirectByteBuffer								decryptedDataForApp_db;

	private ByteBuffer											encryptedDataForApp;

	private DirectByteBuffer								encryptedDataForApp_db;

	private ByteBuffer											encryptedDataForNetwork;

	private DirectByteBuffer								encryptedDataForNetwork_db;

	private boolean												 handshakeCompleted					 = false;

	private final SSLEngine								 sslEngine;

	private final String										remoteHost;

	private final int											 remotePort;

	private final TransportHelper					 transport;

	public final static String							SHARED_SECRET_FOR_SSL_STRING = "SSL";

	private long														totalNetWritten							= 0;

	private long														totalNetRead								 = 0;

	private long														totalDataWritten						 = 0;

	private long														totalDataRead								= 0;

	// private final ByteBuffer tempDecryptBuffer;

	public OneSwarmSslTransportHelperFilterStream(TransportHelper _transport,
			boolean outbound, SslHandShakeMatch isClientHello)
			throws KeyManagementException, NoSuchAlgorithmException,
			KeyStoreException, CertificateException, UnrecoverableKeyException,
			IOException, InterruptedException {
		super(_transport);
		this.transport = _transport;
		this.incomingHandshake = isClientHello;
		this.remoteHost = _transport.getAddress().getAddress().getHostAddress();
		this.remotePort = _transport.getAddress().getPort();

		this.sslEngine = OneSwarmSslKeyManager.getInstance().getSSLContext().createSSLEngine();
		//		this.sslEngine = OneSwarmSslKeyManager.getInstance().getSSLContext().createSSLEngine(
		//				remoteHost, remotePort);

		if (outbound) {
			sslEngine.setUseClientMode(true);
		} else {

			sslEngine.setUseClientMode(false);
			sslEngine.setNeedClientAuth(isClientHello == SslHandShakeMatch.SSL_CLIENT_CERT);
		}

		SSLSession sslSession = sslEngine.getSession();
		SSL_APP_BUFFER_SIZE = sslSession.getApplicationBufferSize() * 2;
		SSL_NET_BUFFER_SIZE = sslSession.getPacketBufferSize() * 2;

		// we will need this during handshake, allocate here
		decryptedDataForApp_db = DirectByteBufferPool.getBuffer(
				DirectByteBuffer.AL_NET_CRYPT, SSL_APP_BUFFER_SIZE);
		decryptedDataForApp = decryptedDataForApp_db.getBuffer(DirectByteBuffer.SS_NET);

		encryptedDataForApp_db = DirectByteBufferPool.getBuffer(
				DirectByteBuffer.AL_NET_CRYPT, SSL_NET_BUFFER_SIZE);
		encryptedDataForApp = encryptedDataForApp_db.getBuffer(DirectByteBuffer.SS_NET);
		encryptedDataForApp.clear();

		logger.fine("Starting handshake");
		sslEngine.beginHandshake();
		handshakeStatus = sslEngine.getHandshakeStatus();

		handshakeBuffer = ByteBuffer.allocate(0);

		logger.fine("created new ssl helper: " + transport.getAddress()
				+ " outbound: " + outbound + " clientAuth: "
				+ sslEngine.getNeedClientAuth());

		/*
		 * We will have to manually register for a write select for the underlying transport
		 * this handles the case when we have buffered encrypted data, but the layers above
		 * think they wrote everything to the transport already
		 */
		//		transport.registerForWriteSelects(new selectListener() {
		//
		//			public void selectFailure(TransportHelper helper, Object attachment,
		//					Throwable msg) {
		//			}
		//
		//			public boolean selectSuccess(TransportHelper helper, Object attachment) {
		//				// only do stuff if we have buffered data
		//				if (encryptedDataForNetwork != null
		//						&& encryptedDataForNetwork.hasRemaining()) {
		//					/*
		//					 * we are listening to these select to be able to handle the case when
		//					 * we have buffered data ready for the transport
		//					 */
		//					try {
		//						long writtenToTranport = write(new ByteBuffer[] {}, 0, 0);
		//						logger.fine("got manual select for underlying transport, wrote "
		//								+ writtenToTranport);
		//						final boolean done = writtenToTranport > 0;
		//						if(done){
		//							transport.cancelWriteSelects();
		//							return true;
		//						}
		//					} catch (IOException e) {
		//						// TODO Auto-generated catch block
		//						e.printStackTrace();
		//					}
		//				}
		//				return false;
		//			}
		//		}, null);
	}

	protected void cryptoIn(ByteBuffer source_buffer, ByteBuffer target_buffer)
			throws IOException {
		throw new RuntimeException("not implemented, use the read() function");
	}

	protected void cryptoOut(ByteBuffer source_buffer, ByteBuffer target_buffer)
			throws IOException {
		throw new RuntimeException("not implemented, use the write() function");
	}

	//	private int cryptoOut(ByteBuffer[] source_buffer, ByteBuffer target_buffer)
	//			throws IOException {
	//		// byte[] data = new byte[source_buffer.remaining()];
	//		// System.arraycopy(source_buffer.array(), source_buffer.position(),
	//		// data,
	//		// 0, data.length);
	//		// System.out.println("SSL: encrypting '" + new String(data) + "'");
	//		if (!handshakeCompleted) {
	//			Debug.out("SSL: handshake needed for encrypt");
	//			return 0;
	//		} else {
	//
	//			SSLEngineResult result = sslEngine.wrap(source_buffer, target_buffer);
	//			totalDataWritten += result.bytesConsumed();
	//			totalNetWritten += result.bytesProduced();
	//			if (result.getStatus() == Status.CLOSED) {
	//				logger.fine("SSL engine closed");
	//				throw new IOException("SSL engine closed");
	//			} else if (result.getStatus() != Status.OK) {
	//				throw new IOException("wrapping, got error:\n" + result);
	//			}
	//			return result.bytesConsumed();
	//		}
	//	}

	public boolean hasBufferedWrite() {
		return encryptedDataForNetwork != null
				&& encryptedDataForNetwork.hasRemaining();
	}

	public boolean hasBufferedRead() {
		return decryptedDataForApp != null && decryptedDataForApp.hasRemaining();
	}

	/**
	 * rewrite of the method in TransportHelperFilterStream, we need behave a
	 * bit differently here
	 * 
	 * 
	 */
	public long read(ByteBuffer[] buffers, int array_offset, int length)
			throws IOException {

		// ************** check if we have anything left from last round
		int total_read = 0;
		if (decryptedDataForApp != null) {
			int leftovers = decryptedDataForApp.remaining();
			if (leftovers > 0) {
				int copied = this.putInBuffers(buffers, array_offset, length,
						decryptedDataForApp);
				logger.finest("found " + leftovers + " old bytes, copied: " + copied);
				total_read += copied;

				if (decryptedDataForApp.hasRemaining()) {
					totalDataRead += total_read;
					logger.finest("returning decoded " + total_read + " bytes, total: "
							+ totalDataRead);
					return total_read;
				}
			}
			// return the buffer to the pool
			// log("returning encr app buffer to pool");
			decryptedDataForApp_db.returnToPool();
			decryptedDataForApp = null;
			decryptedDataForApp_db = null;
		}

		// *********** check how much we can read
		int spaceInBuffers = 0;
		for (int i = array_offset; i < array_offset + length; i++) {
			spaceInBuffers += buffers[i].remaining();
		}
		if (spaceInBuffers == 0) {
			logger.finest("returning decoded " + total_read + " bytes");
			return total_read;
		}

		// *********** read from the network, prepare buffer for reading
		if (encryptedDataForApp == null) {
			// allocate first
			encryptedDataForApp_db = DirectByteBufferPool.getBuffer(
					DirectByteBuffer.AL_NET_CRYPT, SSL_NET_BUFFER_SIZE);
			encryptedDataForApp = encryptedDataForApp_db.getBuffer(DirectByteBuffer.SS_NET);
			encryptedDataForApp.clear();
		}
		int readFromTransport = transport.read(encryptedDataForApp);
		if (readFromTransport == -1) {
			encryptedDataForApp_db.returnToPool();
			encryptedDataForApp_db = null;
			encryptedDataForApp = null;
			if (decryptedDataForApp != null) {
				decryptedDataForApp_db.returnToPool();
				decryptedDataForApp = null;
				decryptedDataForApp_db = null;
			}
			throw new IOException("transport closed (returned -1)");
		}
		totalNetRead += readFromTransport;
		logger.finest("read " + readFromTransport
				+ " bytes from transport, total: " + totalNetRead);
		encryptedDataForApp.flip();

		// ********** decrypt the data, if the input buffers have enough
		// space we can do it straight away, otherwise we have to do it to a
		// temp buffer
		SSLEngineResult result;
		if (spaceInBuffers > SSL_APP_BUFFER_SIZE) {
			// log("using normal buffer");
			result = sslEngine.unwrap(encryptedDataForApp, buffers, array_offset,
					length);
			total_read += result.bytesProduced();
		} else {
			// log("using temp buffer");
			if (decryptedDataForApp == null) {
				// must allocate first, lets get it from the pool
				decryptedDataForApp_db = DirectByteBufferPool.getBuffer(
						DirectByteBuffer.AL_NET_CRYPT, SSL_APP_BUFFER_SIZE);
				decryptedDataForApp = decryptedDataForApp_db.getBuffer(DirectByteBuffer.SS_NET);
				decryptedDataForApp.clear();

			}
			result = sslEngine.unwrap(encryptedDataForApp, decryptedDataForApp);

			// copy the data into the supplied buffers
			decryptedDataForApp.flip();
			int copied = this.putInBuffers(buffers, array_offset, length,
					decryptedDataForApp);
			total_read += copied;

			// check if we could fit everything, otherwise it will be taken
			// care
			// of the next iter
			if (decryptedDataForApp.remaining() > 0) {
				logger.finest("got excess data after decrypt, buffering: "
						+ decryptedDataForApp.remaining());
			}
		}
		if (decryptedDataForApp != null && !decryptedDataForApp.hasRemaining()) {
			// empty buffer, can return to the pool
			// Log.log("returning encr app buffer to pool", logToStdOut);
			decryptedDataForApp_db.returnToPool();
			decryptedDataForApp_db = null;
			decryptedDataForApp = null;
		}

		// done with the network buffer, prepare it for for reading
		encryptedDataForApp.compact();
		int networkDataAvailable = encryptedDataForApp.position();
		if (networkDataAvailable > 0) {
			// Log.log("Data left in incoming network buffer: "
			// + networkDataAvailable, logToStdOut);
		} else {
			// we can return the buffer to the pool
			encryptedDataForApp_db.returnToPool();
			encryptedDataForApp_db = null;
			encryptedDataForApp = null;
		}

		// check the status to see what happend
		if (result.getStatus() == Status.CLOSED) {
			logger.fine("remote side closed connection: " + transport.getAddress());
			return -1;
		} else if (result.getStatus() == Status.BUFFER_UNDERFLOW) {
			//log("Underflow, need to read more, available:" + networkDataAvailable);
		} else if (result.getStatus() != Status.OK) {

			Debug.out("unwrapping, got error:\n" + result);
		}
		totalDataRead += total_read;
		logger.finest("returning decoded " + total_read + " bytes, total: "
				+ totalDataRead);
		return total_read;
	}

	private int putInBuffers(ByteBuffer targets[], int array_offset, int length,
			ByteBuffer source) {

		int copied = 0;
		for (int i = array_offset; i < array_offset + length; i++) {
			// int start = copied;
			ByteBuffer t = targets[i];
			if (source.remaining() == 0) {
				break;
			}
			if (t.remaining() == 0) {
				continue;
			}
			int numBytesToCopy = Math.min(t.remaining(), source.remaining());
			if (t.remaining() < source.remaining()) {
				// we need to set the limit to avoid buffer overflow
				int oldLimit = source.limit();
				source.limit(source.position() + t.remaining());
				t.put(source);
				source.limit(oldLimit);
			} else {
				t.put(source);
			}
			copied += numBytesToCopy;
		}
		return copied;
	}

	long		unaccountedDataWritten = 0;

	boolean bufferPositionChanged	= false;

	/**
	 * override the function specified by TransportHelperFilter. We need buffers
	 * of different lengths
	 */
	public long write(ByteBuffer[] buffers, int array_offset, int length)
			throws IOException {
		int total_written = 0;

		// first check if we have anything that is encrypted already and ready
		// to write out
		if (encryptedDataForNetwork != null) {

			if (encryptedDataForNetwork.hasRemaining()) {
				long writtenToTransport = transport.write(encryptedDataForNetwork, true);

				totalNetWritten += writtenToTransport;
				if (encryptedDataForNetwork.hasRemaining()) {
					// we need to write more next write select
					return 0;
				}
			}
			encryptedDataForNetwork.clear();
		} else {
			// we need this buffer for some time
			encryptedDataForNetwork_db = DirectByteBufferPool.getBuffer(
					DirectByteBuffer.AL_NET_CRYPT,
					OneSwarmSslTransportHelperFilterStream.SSL_NET_BUFFER_SIZE);
			encryptedDataForNetwork = encryptedDataForNetwork_db.getBuffer(DirectByteBuffer.SS_NET);
			encryptedDataForNetwork.clear();
		}
		// ok, lets count to see how much we need to write, also, if we moved the buffer position earlier, move it back
		int size = 0;
		for (int i = array_offset; i < array_offset + length; i++) {
			/*
			 * checking if we need to more the position
			 */
			if (bufferPositionChanged) {
				if (buffers[i].position() < buffers[i].limit()) {
					buffers[i].position(buffers[i].position() + 1);
					bufferPositionChanged = false;
					logger.finer("restoring buffer pos, new pos: "
							+ buffers[i].position() + " limit: " + buffers[i].limit());
				}
			}
			size += buffers[i].remaining();
		}

		// there could be a lot of data to be written, it might required
		// multiple iterations of cryptoOut
		while (total_written < size) {

			// we need to make sure that the handshake is completed, 
			// otherwise, return 0 to indicate that the calling transport try again back later
			if (!handshakeCompleted) {
				Debug.out("SSL: handshake needed for encrypt");
				return 0;
			}

			SSLEngineResult result = sslEngine.wrap(buffers, array_offset, length,
					encryptedDataForNetwork);
			totalDataWritten += result.bytesConsumed();
			totalNetWritten += result.bytesProduced();
			if (result.getStatus() == Status.CLOSED) {
				logger.fine("SSL engine closed");
				throw new IOException("SSL engine closed");
			} else if (result.getStatus() != Status.OK) {
				throw new IOException("wrapping, got error:\n" + result);
			}
			int bytesConsumed = result.bytesConsumed();

			total_written += bytesConsumed;
			encryptedDataForNetwork.flip();

			boolean partial_write;
			if (total_written < size) {
				partial_write = true;
			} else {
				partial_write = false;
			}

			int written = transport.write(encryptedDataForNetwork, partial_write);
			if (written == -1) {
				encryptedDataForNetwork_db.returnToPool();
				encryptedDataForNetwork_db = null;
				encryptedDataForNetwork = null;
				return -1;
			}
			logger.finest("THF: wrote: " + bytesConsumed + " partial write: "
					+ partial_write + " total: " + totalDataWritten);
			if (encryptedDataForNetwork.hasRemaining()) {
				logger.finest("encrypted data left to be written out, buffering "
						+ encryptedDataForNetwork.remaining()
						+ " forcing calling transport to call us again on next write select");
				/*
				 * this case is a bit tricky, we need the transport to call us again 
				 * so we can write out the buffered data, but the calling transport might think it
				 * is done since it wrote all the data it wanted to write. If that is the case, 
				 * fool it by saying it has 1 byte left 
				*/
				int left = 0;
				int lastEmptyBufferIdx = 0;
				ByteBuffer lastEmptyBuffer = null;
				for (int i = array_offset; i < array_offset + length; i++) {
					if (!buffers[i].hasRemaining() && buffers[i].position() > 0) {
						lastEmptyBufferIdx = i;
						lastEmptyBuffer = buffers[i];
					} else {
						left += buffers[i].remaining();
						break;
					}
				}
				/*
				 * if there is no data left we need to to magic stuff
				 */
				if (left == 0 && lastEmptyBuffer != null) {
					logger.finer("data is buffered in ssl transport, setting buffer position to trigger one more select");
					logger.finer("buffer:" + lastEmptyBufferIdx + " old pos: "
							+ lastEmptyBuffer.position());
					buffers[lastEmptyBufferIdx].position(lastEmptyBuffer.position() - 1);
					bufferPositionChanged = true;
					unaccountedDataWritten = total_written;
					/*
					 * return 0 to trigger one more select
					 */
					return 0;
				}
				/*
				 * if there is data left we can safely return how much we wrote, 
				 * the transport will call us again
				 */
				return total_written;
			} else {
				encryptedDataForNetwork.clear();
			}
		}

		// if we are here it means that we wrote everything to the transport
		// return the buffer to the pool
		encryptedDataForNetwork_db.returnToPool();
		encryptedDataForNetwork_db = null;
		encryptedDataForNetwork = null;

		if (total_written > 0 && unaccountedDataWritten > 0) {
			/* 
			 * we might have some unaccounted data that we wrote earlier, count it now
			 */
			total_written += unaccountedDataWritten;
			unaccountedDataWritten = 0;
		}
		return (total_written);
	}

	public void doHandshake(ByteBuffer read_buffer, ByteBuffer write_buffer)
			throws IOException {
		SSLEngineResult result;

		while (handshakeStatus != HandshakeStatus.FINISHED) {
			logger.finer("handshake: " + handshakeStatus.toString());
			if (handshakeStatus == HandshakeStatus.NEED_UNWRAP) {
				while (handshakeStatus == HandshakeStatus.NEED_UNWRAP) {
					read_buffer.flip();
					result = sslEngine.unwrap(read_buffer, decryptedDataForApp);
					logger.finer("Unwrapping:\n" + result + " " + result.bytesConsumed()
							+ " input bytes=" + read_buffer.remaining());
					read_buffer.compact();
					handshakeStatus = result.getHandshakeStatus();

					if (result.getStatus() == Status.BUFFER_UNDERFLOW) {
						return;
					}

					switch (result.getStatus()) {
						case OK:
							switch (handshakeStatus) {
								case NOT_HANDSHAKING:
									throw new IOException("No Handshake");

								case NEED_TASK:
									handshakeStatus = executeDelegatedTask();
									break;

								case FINISHED:
									return;
							}
							break;
						case BUFFER_OVERFLOW:
							Debug.out("buffer overflow during handshake, should never happen");
							break;
						default:
							throw new IOException("Handshake exception: "
									+ result.getStatus());
					}
				}
			} else if (handshakeStatus == HandshakeStatus.NEED_WRAP) {

				write_buffer.clear();

				result = sslEngine.wrap(handshakeBuffer, write_buffer);
				write_buffer.flip();

				handshakeStatus = result.getHandshakeStatus();
				logger.finer("after wrap: " + handshakeStatus);
				switch (result.getStatus()) {
					case OK:
						if (handshakeStatus == HandshakeStatus.NEED_TASK) {
							handshakeStatus = executeDelegatedTask();
						}

						// The data is in the output_buffer, will get written when
						// we exit
						logger.finer("result=ok, returning. pos: "
								+ write_buffer.position());
						return;
					default:
						throw new IOException("Handshaking error: " + result.getStatus());
				}
			} else {
				throw new RuntimeException("Invalid Handshaking State"
						+ handshakeStatus);
			}
		}
		handshakeCompleted = true;
		logger.finer("handshake completed");
		if (incomingHandshake == SslHandShakeMatch.SSL_CLIENT_CERT) {
			Certificate[] remoteCerts = sslEngine.getSession().getPeerCertificates();
			if (remoteCerts.length == 1) {
				try {
					logger.finer("remote public key="
							+ new String(
									Base64.encode(remoteCerts[0].getPublicKey().getEncoded())));
					transport.setUserData(REMOTE_SSL_PUBLIC_KEY,
							remoteCerts[0].getPublicKey().getEncoded());
				} catch (Exception e) {
					Debug.out("OSSSL THF: error getting public key", e);
				}
			}
		}
		decryptedDataForApp.flip();
		read_buffer.flip();
		encryptedDataForApp.put(read_buffer);

		logger.finer("buffered " + decryptedDataForApp.remaining()
				+ " bytes, encrypted data stored for next read: "
				+ encryptedDataForApp.position() + " write_buffer: "
				+ write_buffer.remaining());
		if (write_buffer.hasRemaining()) {
			logger.finer("write buffer has data left after handshake, writing "
					+ write_buffer.remaining() + " bytes to ssl engine");
			write(new ByteBuffer[] {
				write_buffer
			}, 0, 1);
			if (write_buffer.remaining() > 0) {
				Debug.out("write_buffer still has data left!, fix if this ever happens!");
			}
		}

		return;
	}

	private HandshakeStatus executeDelegatedTask() {
		Runnable task;
		while ((task = sslEngine.getDelegatedTask()) != null) {
			//System.out.println("running task: " + task);
			task.run();
		}
		handshakeStatus = sslEngine.getHandshakeStatus();
		//System.out.println("new status: " + handshakeStatus);
		return handshakeStatus;
	}

	public String getName() {

		return (SSL_NAME + remoteHost + ":" + remotePort);
	}

	public boolean isEncrypted() {
		return (true);
	}

	public boolean isHandshakeCompleted() {
		return handshakeCompleted;
	}

	public static SslHandShakeMatch isSSLClientHello(byte[] data) {
		logger.finer("checking for ssl hello: '" + new String(Hex.encode(data))
				+ "'");
		SslHandShakeMatch oneswarmMatch = isOneSwarmSSLClientHello(data);
		logger.finer("OneSwarm match: " + oneswarmMatch.name());
		if (oneswarmMatch == SslHandShakeMatch.SSL_CLIENT_CERT) {
			return oneswarmMatch;
		}

		SslHandShakeMatch browserMatch = isBrowserSslClientHello(data);
		logger.finer("browser match: " + browserMatch.name());
		if (browserMatch == SslHandShakeMatch.SSL_NO_CLIENT_CERT) {
			return browserMatch;
		}

		return SslHandShakeMatch.NO_SSL;
	}

	private static SslHandShakeMatch isOneSwarmSSLClientHello(byte[] data) {
		// DataInputStream in = new DataInputStream(new
		// ByteArrayInputStream(data));

		if (data.length < SSL_HEADER_MIN_LENGTH) {
			return SslHandShakeMatch.NOT_ENOUGH_BYTES;
		}

		// from: http://wp.netscape.com/eng/ssl3/draft302.txt
		//		byte[] length_b = {
		//			data[0],
		//			data[1]
		//		};
		//int length = OneSwarmSslTools.unsignedShortToInt(length_b);
		short msg_type = OneSwarmSslTools.unsignedByteToShort(data[2]);

		short version_major = OneSwarmSslTools.unsignedByteToShort(data[3]);
		//short version_minor = OneSwarmSslTools.unsignedByteToShort(data[4]);
		byte[] cipher_spec_length_b = {
			data[5],
			data[6]
		};
		int cipher_spec_length = OneSwarmSslTools.unsignedShortToInt(cipher_spec_length_b);
		byte[] session_id_length_b = {
			data[7],
			data[8]
		};
		int session_id_length = OneSwarmSslTools.unsignedShortToInt(session_id_length_b);
		byte[] challenge_length_b = {
			data[9],
			data[10]
		};
		int challenge_length = OneSwarmSslTools.unsignedShortToInt(challenge_length_b);

		//		System.err.println("length: " + length);
		//		System.err.println("msg_type: " + msg_type);
		//		System.err.println("version_major: " + version_major);
		//		System.err.println("version_minor: " + version_minor);
		//		System.err.println("cipher_spec_length: " + cipher_spec_length);
		//		System.err.println("session_id_length: " + session_id_length);
		//		System.err.println("challenge_length: " + challenge_length);

		// msg_type is always 1
		if (msg_type != 1) {
			return SslHandShakeMatch.NO_SSL;
		}
		// SSL 3
		if (version_major != 3) {
			return SslHandShakeMatch.NO_SSL;
		}
		// the cipher length is always divisible by 3
		if (cipher_spec_length % 3 != 0) {
			return SslHandShakeMatch.NO_SSL;
		}
		// session id is either of length 0 or of length 16
		if (session_id_length != 16 && session_id_length != 0) {
			return SslHandShakeMatch.NO_SSL;
		}
		// challenge length is always 32
		if (challenge_length != 32) {
			return SslHandShakeMatch.NO_SSL;
		}
		return SslHandShakeMatch.SSL_CLIENT_CERT;
	}

	/*
	 * no need to reallocate this stuff, we are throwing it away anyway
	 */
	private static byte[] OUTPUT_BYTES = new byte[SSL_NET_BUFFER_SIZE];

	private static SslHandShakeMatch isBrowserSslClientHello(byte[] data) {
		if (!COConfigurationManager.getBooleanParameter("OSGWTUI.RemoteAccess")) {
			return SslHandShakeMatch.NO_SSL;
		}
		if (data.length < SSL_HEADER_MIN_LENGTH) {
			return SslHandShakeMatch.NOT_ENOUGH_BYTES;
		}
		/*
		 * check if this is an ssl connection by feeding the data to an ssl engine and check if it complains
		 */
		try {
			ByteBuffer sslTestIn = ByteBuffer.allocate(SSL_NET_BUFFER_SIZE);
			ByteBuffer sslTestOut = ByteBuffer.wrap(OUTPUT_BYTES);
			//long time = System.currentTimeMillis();
			SSLEngine sslEngine = OneSwarmSslKeyManager.getInstance().getSSLContext().createSSLEngine();
			sslEngine.setUseClientMode(false);
			sslEngine.setNeedClientAuth(false);
			sslEngine.beginHandshake();
			sslTestIn.put(data);
			sslTestIn.flip();
			SSLEngineResult result = sslEngine.unwrap(sslTestIn, sslTestOut);
			logger.finer("got result from ssl engine: " + result.toString());
			return SslHandShakeMatch.SSL_NO_CLIENT_CERT;
		} catch (Throwable e) {
			logger.finer("got error from ssl engine: " + e.getMessage());
			return SslHandShakeMatch.NO_SSL;
		}
	}

	//	private void log(String text) {
	//		if (DEBUG_LOGGING) {
	//			text = getName() + ": '" + text + "'";
	//			if (Logger.isEnabled()) {
	//				Logger.log(new LogEvent(LogIDs.OSF2F, LogEvent.LT_INFORMATION, text));
	//			}
	//
	//			System.out.println(text);
	//
	//		}
	//	}

}
