package edu.washington.cs.oneswarm.f2f.dht;

public interface CHTCallback {
    public void valueReceived(byte[] key, byte[] value);

    public void errorReceived(Throwable cause);
}