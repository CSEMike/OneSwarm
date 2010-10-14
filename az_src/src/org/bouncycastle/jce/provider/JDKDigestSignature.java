package org.bouncycastle.jce.provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.teletrust.TeleTrusTObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.MD2Digest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.RIPEMD128Digest;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.crypto.digests.RIPEMD256Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;

public class JDKDigestSignature
    extends Signature implements PKCSObjectIdentifiers, X509ObjectIdentifiers
{
    private Digest                  digest;
    private AsymmetricBlockCipher   cipher;
    private AlgorithmIdentifier     algId;

    protected JDKDigestSignature(
        String                  name,
        DERObjectIdentifier     objId,
        Digest                  digest,
        AsymmetricBlockCipher   cipher)
    {
        super(name);

        this.digest = digest;
        this.cipher = cipher;
        this.algId = new AlgorithmIdentifier(objId, null);
    }

    protected void engineInitVerify(
        PublicKey   publicKey)
        throws InvalidKeyException
    {
		if ( !(publicKey instanceof RSAPublicKey) )
		{
			throw new InvalidKeyException("Supplied key is not a RSAPublicKey instance");
		}

        CipherParameters    param = RSAUtil.generatePublicKeyParameter((RSAPublicKey)publicKey);

        digest.reset();
        cipher.init(false, param);
    }

    protected void engineInitSign(
        PrivateKey  privateKey)
        throws InvalidKeyException
    {
		if ( !(privateKey instanceof RSAPrivateKey) )
		{
			throw new InvalidKeyException("Supplied key is not a RSAPrivateKey instance");
		}

        CipherParameters    param = RSAUtil.generatePrivateKeyParameter((RSAPrivateKey)privateKey);

        digest.reset();

        cipher.init(true, param);
    }

    protected void engineUpdate(
        byte    b)
        throws SignatureException
    {
        digest.update(b);
    }

    protected void engineUpdate(
        byte[]  b,
        int     off,
        int     len) 
        throws SignatureException
    {
        digest.update(b, off, len);
    }

    protected byte[] engineSign()
        throws SignatureException
    {
        byte[]  hash = new byte[digest.getDigestSize()];

        digest.doFinal(hash, 0);

        try
        {
            byte[]  bytes = derEncode(hash);

            return cipher.processBlock(bytes, 0, bytes.length);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            throw new SignatureException("key too small for signature type");
        }
        catch (Exception e)
        {
            throw new SignatureException(e.toString());
        }
    }

    protected boolean engineVerify(
        byte[]  sigBytes) 
        throws SignatureException
    {
        byte[]  hash = new byte[digest.getDigestSize()];

        digest.doFinal(hash, 0);

        DigestInfo  digInfo;
        byte[]      sig;

        try
        {
            sig = cipher.processBlock(sigBytes, 0, sigBytes.length);
            digInfo = derDecode(sig);
        }
        catch (Exception e)
        {
            return false;
        }

        if (!digInfo.getAlgorithmId().equals(algId))
        {
            return false;
        }

        byte[]  sigHash = digInfo.getDigest();

        if (hash.length != sigHash.length)
        {
            return false;
        }

        for (int i = 0; i < hash.length; i++)
        {
            if (sigHash[i] != hash[i])
            {
                return false;
            }
        }

        return true;
    }

    protected void engineSetParameter(
        AlgorithmParameterSpec params)
    {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }

    /**
     * @deprecated replaced with <a href = "#engineSetParameter(java.security.spec.AlgorithmParameterSpec)">
     */
    protected void engineSetParameter(
        String  param,
        Object  value)
    {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }

    /**
     * @deprecated
     */
    protected Object engineGetParameter(
        String      param)
    {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }

    private byte[] derEncode(
        byte[]  hash)
        throws IOException
    {
        ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
        DEROutputStream         dOut = new DEROutputStream(bOut);
        DigestInfo              dInfo = new DigestInfo(algId, hash);

        dOut.writeObject(dInfo);

        return bOut.toByteArray();
    }

    private DigestInfo derDecode(
        byte[]  encoding)
        throws IOException
    {
        ByteArrayInputStream    bIn = new ByteArrayInputStream(encoding);
        DERInputStream          dIn = new DERInputStream(bIn);

        return new DigestInfo((ASN1Sequence)dIn.readObject());
    }

    static public class SHA1WithRSAEncryption
        extends JDKDigestSignature
    {
        public SHA1WithRSAEncryption()
        {
            super("SHA1withRSA", id_SHA1, new SHA1Digest(), new PKCS1Encoding(new RSAEngine()));
        }
    }

    static public class MD2WithRSAEncryption
        extends JDKDigestSignature
    {
        public MD2WithRSAEncryption()
        {
            super("MD2withRSA", md2, new MD2Digest(), new PKCS1Encoding(new RSAEngine()));
        }
    }

    static public class MD5WithRSAEncryption
        extends JDKDigestSignature
    {
        public MD5WithRSAEncryption()
        {
            super("MD5withRSA", md5, new MD5Digest(), new PKCS1Encoding(new RSAEngine()));
        }
    }

    static public class RIPEMD160WithRSAEncryption
        extends JDKDigestSignature
    {
        public RIPEMD160WithRSAEncryption()
        {
            super("RIPEMD160withRSA", TeleTrusTObjectIdentifiers.ripemd160, new RIPEMD160Digest(), new PKCS1Encoding(new RSAEngine()));
        }
    }
    
	static public class RIPEMD128WithRSAEncryption
		extends JDKDigestSignature
	{
		public RIPEMD128WithRSAEncryption()
		{
			super("RIPEMD128withRSA", TeleTrusTObjectIdentifiers.ripemd128, new RIPEMD128Digest(), new PKCS1Encoding(new RSAEngine()));
		}
	}
	
	static public class RIPEMD256WithRSAEncryption
		extends JDKDigestSignature
	{
		public RIPEMD256WithRSAEncryption()
		{
			super("RIPEMD256withRSA", TeleTrusTObjectIdentifiers.ripemd256, new RIPEMD256Digest(), new PKCS1Encoding(new RSAEngine()));
		}
	}
}
