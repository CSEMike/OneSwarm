package edu.washington.cs.oneswarm.f2f.messaging;


/**
 * Messages implementing this interface support prioritization via computational
 * puzzles.
 */
public interface OSF2FPuzzleSupportingMessage extends OSF2FMessage {

    /**
     * Returns the immutable part of this message that serves as the basis for
     * the puzzle. If forwarded, this data must not change per-hop.
     */
    byte[] getPuzzleMaterial();

    /**
     * State carried by puzzle supporting messages that's used and/or filled in
     * at various stages of processing.
     * 
     * If this is a message the is forwarded, the puzzle wrapper is the original
     * one received with the message. If the message is generated locally, a
     * puzzle wrapper is created to hold the puzzle solution without an
     * encapsulated message.
     */
    void setPuzzleWrapper(OSF2FPuzzleWrappedMessage wrapper);
    OSF2FPuzzleWrappedMessage getPuzzleWrapper();
}
