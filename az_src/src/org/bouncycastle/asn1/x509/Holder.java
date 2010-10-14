package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

/**
 * The Holder object.
 * <pre>
 *  Holder ::= SEQUENCE {
 *        baseCertificateID   [0] IssuerSerial OPTIONAL,
 *                 -- the issuer and serial number of
 *                 -- the holder's Public Key Certificate
 *        entityName          [1] GeneralNames OPTIONAL,
 *                 -- the name of the claimant or role
 *        objectDigestInfo    [2] ObjectDigestInfo OPTIONAL
 *                 -- used to directly authenticate the holder,
 *                 -- for example, an executable
 *  }
 * </pre>
 */
public class Holder
    implements DEREncodable
{
    IssuerSerial        baseCertificateID;
    GeneralNames        entityName;
    ObjectDigestInfo    objectDigestInfo;

    public DERObject getDERObject()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        if (baseCertificateID != null)
        {
            v.add(new DERTaggedObject(false, 0, baseCertificateID));
        }

        if (entityName != null)
        {
            v.add(new DERTaggedObject(false, 1, entityName));
        }

        if (objectDigestInfo != null)
        {
            v.add(new DERTaggedObject(false, 2, objectDigestInfo));
        }

        return new DERSequence(v);
    }
}
