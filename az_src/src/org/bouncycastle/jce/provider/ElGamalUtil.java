package org.bouncycastle.jce.provider;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ElGamalParameters;
import org.bouncycastle.crypto.params.ElGamalPrivateKeyParameters;
import org.bouncycastle.crypto.params.ElGamalPublicKeyParameters;
import org.bouncycastle.jce.interfaces.ElGamalPrivateKey;
import org.bouncycastle.jce.interfaces.ElGamalPublicKey;

/**
 * utility class for converting jce/jca ElGamal objects
 * objects into their org.bouncycastle.crypto counterparts.
 */
public class ElGamalUtil
{
    static public AsymmetricKeyParameter generatePublicKeyParameter(
        PublicKey    key)
        throws InvalidKeyException
    {
        if (key instanceof ElGamalPublicKey)
        {
            ElGamalPublicKey    k = (ElGamalPublicKey)key;

            return new ElGamalPublicKeyParameters(k.getY(),
                new ElGamalParameters(k.getParams().getP(), k.getParams().getG()));
        }

        throw new InvalidKeyException("can't identify ElGamal public key.");
    }

    static public AsymmetricKeyParameter generatePrivateKeyParameter(
        PrivateKey    key)
        throws InvalidKeyException
    {
        if (key instanceof ElGamalPrivateKey)
        {
            ElGamalPrivateKey    k = (ElGamalPrivateKey)key;

            return new ElGamalPrivateKeyParameters(k.getX(),
                new ElGamalParameters(k.getParams().getP(), k.getParams().getG()));
        }
                        
        throw new InvalidKeyException("can't identify ElGamal private key.");
    }
}
