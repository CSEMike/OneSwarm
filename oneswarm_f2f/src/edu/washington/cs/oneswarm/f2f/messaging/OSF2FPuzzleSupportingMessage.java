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
     */
    void setPuzzleWrapper(OSF2FPuzzleWrappedMessage wrapper);
    OSF2FPuzzleWrappedMessage getPuzzleWrapper();
}
