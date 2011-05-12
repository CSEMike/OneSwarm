package edu.washington.cs.oneswarm.f2f.messaging;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

/**
 * Messages implementing this interface support prioritization via computational
 * puzzles.
 */
public interface OSF2FPuzzleSupportingMessage extends OSF2FMessage {

    /**
     * Returns the immutable part of this message that serves as the basis for
     * the puzzle. If forwarded, this data must not change per-hop.
     */
	DirectByteBuffer getPuzzleMaterial();
}
