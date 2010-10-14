package org.bouncycastle.asn1.x509;

import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

public class NameConstraints
    implements DEREncodable
{
    ASN1Sequence    permitted, excluded;

    public NameConstraints(
        ASN1Sequence    seq)
    {
        Enumeration e = seq.getObjects();
        while (e.hasMoreElements())
        {
            ASN1TaggedObject    o = (ASN1TaggedObject)e.nextElement();
            switch (o.getTagNo())
            {
            case 0:
                permitted = ASN1Sequence.getInstance(o, false);
                break;
            case 1:
                excluded = ASN1Sequence.getInstance(o, false);
                break;
            }
        }
    }

    public ASN1Sequence getPermittedSubtrees()
    {
        return permitted;
    }

    public ASN1Sequence getExcludedSubtrees()
    {
        return excluded;
    }

    /*
     * NameConstraints ::= SEQUENCE {
     *      permittedSubtrees       [0]     GeneralSubtrees OPTIONAL,
     *      excludedSubtrees        [1]     GeneralSubtrees OPTIONAL }
     */
    public DERObject getDERObject()
    {
        ASN1EncodableVector   v = new ASN1EncodableVector();

        if (permitted != null)
        {
            v.add(new DERTaggedObject(false, 0, permitted));
        }

        if (excluded != null)
        {
            v.add(new DERTaggedObject(false, 1, excluded));
        }

        return new DERSequence(v);
    }
}
