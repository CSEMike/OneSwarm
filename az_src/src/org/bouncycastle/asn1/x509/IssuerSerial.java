package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;

public class IssuerSerial
    implements DEREncodable
{
    GeneralNames            issuer;
    DERInteger              serial;
    DERBitString            issuerUID;

    public IssuerSerial(
        ASN1Sequence    seq)
    {
        issuer = GeneralNames.getInstance(seq.getObjectAt(0));
        serial = (DERInteger)seq.getObjectAt(1);

        if (seq.size() == 3)
        {
            issuerUID = (DERBitString)seq.getObjectAt(2);
        }
    }

    public GeneralNames getIssuer()
    {
        return issuer;
    }

    public DERInteger getSerial()
    {
        return serial;
    }

    public DERBitString getIssuerUID()
    {
        return issuerUID;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     *  IssuerSerial  ::=  SEQUENCE {
     *       issuer         GeneralNames,
     *       serial         CertificateSerialNumber,
     *       issuerUID      UniqueIdentifier OPTIONAL
     *  }
     * </pre>
     */
    public DERObject getDERObject()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(issuer);
        v.add(serial);

        if (issuerUID != null)
        {
            v.add(issuerUID);
        }

        return new DERSequence(v);
    }
}
