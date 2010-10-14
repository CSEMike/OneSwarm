package org.bouncycastle.asn1.pkcs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

public class PrivateKeyInfo
    implements PKCSObjectIdentifiers, DEREncodable
{
    private DERObject               privKey;
    private AlgorithmIdentifier     algId;

    public PrivateKeyInfo(
        AlgorithmIdentifier algId,
        DERObject           privateKey)
    {
        this.privKey = privateKey;
        this.algId = algId;
    }

    public PrivateKeyInfo(
        ASN1Sequence  seq)
    {
        Enumeration e = seq.getObjects();

        BigInteger  version = ((DERInteger)e.nextElement()).getValue();
        if (version.intValue() != 0)
        {
            throw new IllegalArgumentException("wrong version for private key info");
        }

        algId = new AlgorithmIdentifier((ASN1Sequence)e.nextElement());

        try
        {
            ByteArrayInputStream    bIn = new ByteArrayInputStream(((ASN1OctetString)e.nextElement()).getOctets());
            DERInputStream          dIn = new DERInputStream(bIn);

            privKey = dIn.readObject();
        }
        catch (IOException ex)
        {
            throw new IllegalArgumentException("Error recoverying private key from sequence");
        }
    }

    public AlgorithmIdentifier getAlgorithmId()
    {
        return algId;
    }

    public DERObject getPrivateKey()
    {
        return privKey;
    }

    /**
     * write out an RSA private key with it's asscociated information
     * as described in PKCS8.
     * <pre>
     *      PrivateKeyInfo ::= SEQUENCE {
     *                              version Version,
     *                              privateKeyAlgorithm AlgorithmIdentifier {{PrivateKeyAlgorithms}},
     *                              privateKey PrivateKey,
     *                              attributes [0] IMPLICIT Attributes OPTIONAL 
     *                          }
     *      Version ::= INTEGER {v1(0)} (v1,...)
     *
     *      PrivateKey ::= OCTET STRING
     *
     *      Attributes ::= SET OF Attribute
     * </pre>
     */
    public DERObject getDERObject()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(new DERInteger(0));
        v.add(algId);
        v.add(new DEROctetString(privKey));

        return new DERSequence(v);
    }
}
