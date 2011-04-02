package edu.washington.cs.oneswarm.f2f.dht;

import java.io.IOException;

import edu.washington.cs.oneswarm.f2f.dht.CHTClientUDP.CHTCallback;

public interface CHTClientInterface {

	public void put(byte[] key, byte[] value) throws IOException;

	public void get(final byte[] key, final CHTCallback callback);

}
