package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERTaggedObject;

public class AttCertIssuer
    implements DEREncodable
{
    DEREncodable    obj;
    DERObject       choiceObj;

    public AttCertIssuer(
        V2Form  v2Form)
    {
        obj = v2Form;
        choiceObj = new DERTaggedObject(true, 0, obj);
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     *  AttCertIssuer ::= CHOICE {
     *       v1Form   GeneralNames,  -- MUST NOT be used in this
     *                               -- profile
     *       v2Form   [0] V2Form     -- v2 only
     *  }
     * </pre>
     */
    public DERObject getDERObject()
    {
        return choiceObj;
    }
}
