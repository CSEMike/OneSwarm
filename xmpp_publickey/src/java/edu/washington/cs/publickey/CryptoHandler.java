/**
 * 
 */
package edu.washington.cs.publickey;

import java.security.PublicKey;

import javax.net.ssl.SSLContext;

/**
 * @author isdal
 * 
 */
public interface CryptoHandler {

    /**
     * Sign the date using a RSA public key
     * 
     * @param data
     * @return the signature
     * @throws Exception
     */
    public byte[] sign(byte[] data) throws Exception;

    public SSLContext getSSLContext() throws Exception;

    public PublicKey getPublicKey();
}
