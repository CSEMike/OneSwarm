package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERGeneralizedTime;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;

public class AttCertValidityPeriod
    implements DEREncodable
{
    DERGeneralizedTime  notBeforeTime;
    DERGeneralizedTime  notAfterTime;

    public AttCertValidityPeriod(
        ASN1Sequence    seq)
    {
        notBeforeTime = (DERGeneralizedTime)seq.getObjectAt(0);
        notAfterTime = (DERGeneralizedTime)seq.getObjectAt(1);
    }

    public DERGeneralizedTime getNotBeforeTime()
    {
        return notBeforeTime;
    }

    public DERGeneralizedTime getNotAfterTime()
    {
        return notAfterTime;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     *  AttCertValidityPeriod  ::= SEQUENCE {
     *       notBeforeTime  GeneralizedTime,
     *       notAfterTime   GeneralizedTime
     *  } 
     * </pre>
     */
    public DERObject getDERObject()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(notBeforeTime);
        v.add(notAfterTime);

        return new DERSequence(v);
    }
}
