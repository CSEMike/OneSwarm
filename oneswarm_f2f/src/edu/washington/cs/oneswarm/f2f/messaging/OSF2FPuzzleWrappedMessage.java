package edu.washington.cs.oneswarm.f2f.messaging;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

/**
 * A message that wraps another message with a solution to a computational puzzle.
 * 
 * (The puzzle is to generate a SHA-1 hash collision in N bits with a given immutable part of the
 * contained message, obtained via {@code getPuzzleMaterial()}.)
 */
public class OSF2FPuzzleWrappedMessage implements OSF2FMessage {

	@Override
	public String getID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] getIDBytes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getFeatureID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getFeatureSubID() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public byte getVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getType() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DirectByteBuffer[] getData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public int getMessageSize() {
		// TODO Auto-generated method stub
		return 0;
	}

}
