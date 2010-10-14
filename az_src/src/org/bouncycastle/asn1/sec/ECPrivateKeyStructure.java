package org.bouncycastle.asn1.sec;

import java.math.BigInteger;
import org.bouncycastle.asn1.*;

/**
 * the elliptic curve private key object from SEC 1
 */
public class ECPrivateKeyStructure
implements DEREncodable
{
    private ASN1Sequence  seq;

    public ECPrivateKeyStructure(
        ASN1Sequence  seq)
    {
        this.seq = seq;
    }

    public ECPrivateKeyStructure(
        BigInteger  key)
    {
        byte[]  bytes = key.toByteArray();

        if (bytes[0] == 0)
        {
            byte[]  tmp = new byte[bytes.length - 1];

            System.arraycopy(bytes, 1, tmp, 0, tmp.length);
            bytes = tmp;
        }

        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(new DERInteger(1));
        v.add(new DEROctetString(bytes));

        seq = new DERSequence(v);
    }

    public BigInteger getKey()
    {
        ASN1OctetString  octs = (ASN1OctetString)seq.getObjectAt(1);

        BigInteger  k = new BigInteger(1, octs.getOctets());

        return k;
    }

    public DERObject getDERObject()
    {
        return seq;
    }
}
