package org.bouncycastle.asn1.x509;

import java.math.BigInteger;
import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;

public class RSAPublicKeyStructure
    implements DEREncodable
{
    private BigInteger  modulus;
    private BigInteger  publicExponent;

    public static RSAPublicKeyStructure getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static RSAPublicKeyStructure getInstance(
        Object obj)
    {
        if(obj == null || obj instanceof RSAPublicKeyStructure) 
        {
            return (RSAPublicKeyStructure)obj;
        }
        
        if(obj instanceof ASN1Sequence) 
        {
            return new RSAPublicKeyStructure((ASN1Sequence)obj);
        }
        
        throw new IllegalArgumentException("Invalid RSAPublicKeyStructure: " + obj.getClass().getName());
    }
    
    public RSAPublicKeyStructure(
        BigInteger  modulus,
        BigInteger  publicExponent)
    {
        this.modulus = modulus;
        this.publicExponent = publicExponent;
    }

    public RSAPublicKeyStructure(
        ASN1Sequence  seq)
    {
        Enumeration e = seq.getObjects();

        modulus = ((DERInteger)e.nextElement()).getPositiveValue();
        publicExponent = ((DERInteger)e.nextElement()).getPositiveValue();
    }

    public BigInteger getModulus()
    {
        return modulus;
    }

    public BigInteger getPublicExponent()
    {
        return publicExponent;
    }

    /**
     * This outputs the key in PKCS1v2 format.
     * <pre>
     *      RSAPublicKey ::= SEQUENCE {
     *                          modulus INTEGER, -- n
     *                          publicExponent INTEGER, -- e
     *                      }
     * </pre>
     * <p>
     */
    public DERObject getDERObject()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(new DERInteger(getModulus()));
        v.add(new DERInteger(getPublicExponent()));

        return new DERSequence(v);
    }
}
