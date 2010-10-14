package edu.washington.cs.oneswarm.f2f.messaging;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.networkmanager.Transport;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.MessageManager;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamDecoder;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessage;

public class OSF2FMessageDecoder implements MessageStreamDecoder {

	private static final int MIN_MESSAGE_LENGTH = 1; // for type id

	// should never be > 16KB+9B, as we never request chunks > 16KB
	// private static final int MAX_MESSAGE_LENGTH = 16393;
	// EDIT: actually, file lists and torrents can be in the MB
	private static final int MAX_MESSAGE_LENGTH = 1024 * 1024;// 16401;

	// (byte)19 +"Bit" readInt() value of header
	// private static final int HANDSHAKE_FAKE_LENGTH = 323119476;
	// EDIT: (byte) 12 + "One" readInt() value of header
	private static final int HANDSHAKE_FAKE_LENGTH = 206532197;

	private static final byte SS = DirectByteBuffer.SS_MSG;

	private DirectByteBuffer payload_buffer = null;
	private final DirectByteBuffer length_buffer = DirectByteBufferPool
			.getBuffer(DirectByteBuffer.AL_MSG, 4);
	private final ByteBuffer[] decode_array = new ByteBuffer[] { null,
			length_buffer.getBuffer(SS) };

	private boolean reading_length_mode = true;
	private boolean reading_handshake_message = false;

	private int message_length;
	private int pre_read_start_buffer;
	private int pre_read_start_position;

	private boolean last_received_was_keepalive = false;

	private volatile boolean destroyed = false;
	private volatile boolean is_paused = false;

	private ArrayList<Message> messages_last_read = new ArrayList<Message>();
	private int protocol_bytes_last_read = 0;
	private int data_bytes_last_read = 0;
	private int percent_complete = -1;

	public OSF2FMessageDecoder() {
		/* nothing */
	}

	public int performStreamDecode(Transport transport, int max_bytes)
			throws IOException {

		protocol_bytes_last_read = 0;
		data_bytes_last_read = 0;

		int bytes_remaining = max_bytes;

		while (bytes_remaining > 0) {

			if (destroyed) {

				// destruction currently isn't thread safe so one thread can
				// destroy the decoder (e.g. when closing a connection)
				// while the read-controller is still actively processing the us
				// throw( new IOException( "BTMessageDecoder already destroyed"
				// ));
				break;
			}

			if (is_paused) {
				break;
			}

			int bytes_possible = preReadProcess(bytes_remaining);

			if (bytes_possible < 1) {
				Debug.out("ERROR OS: bytes_possible < 1");
				break;
			}

			if (reading_length_mode) {
				transport.read(decode_array, 1, 1); // only read into length
				// buffer
			} else {
				transport.read(decode_array, 0, 2); // read into payload buffer,
				// and possibly next message
				// length
			}

			int bytes_read = postReadProcess();

			bytes_remaining -= bytes_read;

			if (bytes_read < bytes_possible) {
				break;
			}

			if (reading_length_mode && last_received_was_keepalive) {
				// hack to stop a 0-byte-read after receiving a keep-alive
				// message
				// otherwise we won't realize there's nothing left on the line
				// until trying to read again
				last_received_was_keepalive = false;
				break;
			}
		}

		return max_bytes - bytes_remaining;
	}

	public int getPercentDoneOfCurrentMessage() {
		return percent_complete;
	}

	public Message[] removeDecodedMessages() {
		if (messages_last_read.isEmpty())
			return null;

		Message[] msgs = (Message[]) messages_last_read
				.toArray(new Message[messages_last_read.size()]);

		messages_last_read.clear();

		return msgs;
	}

	public int getProtocolBytesDecoded() {
		return protocol_bytes_last_read;
	}

	public int getDataBytesDecoded() {
		return data_bytes_last_read;
	}

	public ByteBuffer destroy() {
		is_paused = true;
		destroyed = true;

		int lbuff_read = 0;
		int pbuff_read = 0;
		if (length_buffer != null) {
			length_buffer.limit(SS, 4);

			if (reading_length_mode) {
				lbuff_read = length_buffer.position(SS);
			} else { // reading payload
				length_buffer.position(SS, 4);
				lbuff_read = 4;
				pbuff_read = payload_buffer == null ? 0 : payload_buffer
						.position(SS);
			}

			ByteBuffer unused = ByteBuffer.allocate(lbuff_read + pbuff_read); // TODO
			// convert
			// to
			// direct?

			length_buffer.flip(SS);
			unused.put(length_buffer.getBuffer(SS));

			if (payload_buffer != null) {
				payload_buffer.flip(SS);
				unused.put(payload_buffer.getBuffer(SS));
			}

			unused.flip();

			length_buffer.returnToPool();

			if (payload_buffer != null) {
				payload_buffer.returnToPool();
				payload_buffer = null;
			}

			for (int i = 0; i < messages_last_read.size(); i++) {
				Message msg = (Message) messages_last_read.get(i);
				msg.destroy();
			}
			messages_last_read.clear();

			return unused;
		}
		return ByteBuffer.allocate(0);
	}

	private int preReadProcess(int allowed) {
		if (allowed < 1) {
			Debug.out("allowed < 1");
		}

		decode_array[0] = payload_buffer == null ? null : payload_buffer
				.getBuffer(SS); // ensure the decode array has the latest
		// payload pointer

		int bytes_available = 0;
		boolean shrink_remaining_buffers = false;
		int start_buff = reading_length_mode ? 1 : 0;
		boolean marked = false;

		for (int i = start_buff; i < 2; i++) { // set buffer limits according
			// to bytes allowed
			ByteBuffer bb = decode_array[i];

			if (bb == null) {
				Debug.out("preReadProcess:: bb[" + i
						+ "] == null, decoder destroyed=" + destroyed);
			}

			if (shrink_remaining_buffers) {
				bb.limit(0); // ensure no read into this next buffer is
				// possible
			} else {
				int remaining = bb.remaining();

				if (remaining < 1)
					continue; // skip full buffer

				if (!marked) {
					pre_read_start_buffer = i;
					pre_read_start_position = bb.position();
					marked = true;
				}

				if (remaining > allowed) { // read only part of this buffer
					bb.limit(bb.position() + allowed); // limit current buffer
					bytes_available += bb.remaining();
					shrink_remaining_buffers = true; // shrink any tail
					// buffers
				} else { // full buffer is allowed to be read
					bytes_available += remaining;
					allowed -= remaining; // count this buffer toward allowed
					// and move on to the next
				}
			}
		}

		return bytes_available;
	}

	private int postReadProcess() throws IOException {
		int prot_bytes_read = 0;
		int data_bytes_read = 0;

		if (!reading_length_mode && !destroyed) { // reading payload data mode
			// ensure-restore proper buffer limits
			payload_buffer.limit(SS, message_length);
			length_buffer.limit(SS, 4);

			int read = payload_buffer.position(SS) - pre_read_start_position;

			if (payload_buffer.position(SS) > 0) { // need to have read the
				// message id first byte
				if (OSF2FMessageFactory.getMessageType(payload_buffer) == Message.TYPE_DATA_PAYLOAD) {
					data_bytes_read += read;
				} else {
					prot_bytes_read += read;
				}
			}

			if (!payload_buffer.hasRemaining(SS) && !is_paused) { // full
				// message received!
				payload_buffer.position(SS, 0);

				DirectByteBuffer ref_buff = payload_buffer;
				payload_buffer = null;

				if (reading_handshake_message) { // decode handshake
					reading_handshake_message = false;

					DirectByteBuffer handshake_data = DirectByteBufferPool
							.getBuffer(DirectByteBuffer.AL_MSG,
									OSF2FHandshake.MESSAGE_LENGTH);
					handshake_data.putInt(SS, HANDSHAKE_FAKE_LENGTH);
					handshake_data.put(SS, ref_buff);
					handshake_data.flip(SS);

					ref_buff.returnToPool();

					try {
						Message handshake = MessageManager.getSingleton()
								.createMessage(
										OSF2FMessage.ID_OS_HANDSHAKE_BYTES,
										handshake_data, (byte) 1);
						messages_last_read.add(handshake);
					} catch (MessageException me) {
						handshake_data.returnToPool();
						throw new IOException("OSF2F message decode failed: "
								+ me.getMessage());
					}

				} else { // decode normal message
					try {
						Message msg = OSF2FMessageFactory
								.createOSF2FMessage(ref_buff);
						messages_last_read.add(msg);
					} catch (Throwable e) {
						ref_buff.returnToPoolIfNotFree();

						// maintain unexpected erorrs as such so they get logged
						// later

						if (e instanceof RuntimeException) {

							throw ((RuntimeException) e);
						}

						throw new IOException("OSF2F message decode failed: "
								+ e.getMessage());
					}
				}

				reading_length_mode = true; // see if we've already read the
				// next message's length
				percent_complete = -1; // reset receive percentage
			} else { // only partial received so far
				percent_complete = (payload_buffer.position(SS) * 100)
						/ message_length; // compute receive percentage
			}
		}

		if (reading_length_mode && !destroyed) {
			length_buffer.limit(SS, 4); // ensure proper buffer limit

			prot_bytes_read += (pre_read_start_buffer == 1) ? length_buffer
					.position(SS)
					- pre_read_start_position : length_buffer.position(SS);

			if (!length_buffer.hasRemaining(SS)) { // done reading the length
				reading_length_mode = false;

				length_buffer.position(SS, 0);
				message_length = length_buffer.getInt(SS);

				length_buffer.position(SS, 0); // reset it for next length read
				// System.out.println("decoded length: " + message_length);
				if (message_length == HANDSHAKE_FAKE_LENGTH) { // handshake
					// message
					reading_handshake_message = true;
					message_length = OSF2FHandshake.MESSAGE_LENGTH - 4; // restore
					// 'real'
					// length
					payload_buffer = DirectByteBufferPool.getBuffer(
							DirectByteBuffer.AL_MSG, message_length);

				} else if (message_length == 0) { // keep-alive message
					reading_length_mode = true;
					last_received_was_keepalive = true;

					try {
						Message keep_alive = MessageManager.getSingleton()
								.createMessage(
										BTMessage.ID_BT_KEEP_ALIVE_BYTES, null,
										(byte) 1);
						messages_last_read.add(keep_alive);
					} catch (MessageException me) {
						throw new IOException("BT message decode failed: "
								+ me.getMessage());
					}
				} else if (message_length < MIN_MESSAGE_LENGTH
						|| message_length > MAX_MESSAGE_LENGTH) {
					throw new IOException(
							"Invalid message length given for OS message decode: "
									+ message_length);
				} else { // normal message
					payload_buffer = DirectByteBufferPool.getBuffer(
							DirectByteBuffer.AL_MSG_BT_PAYLOAD, message_length);
				}
			}
		}

		protocol_bytes_last_read += prot_bytes_read;
		data_bytes_last_read += data_bytes_read;

		return prot_bytes_read + data_bytes_read;
	}

	public void pauseDecoding() {
		is_paused = true;
	}

	public void resumeDecoding() {
		is_paused = false;
	}

}
