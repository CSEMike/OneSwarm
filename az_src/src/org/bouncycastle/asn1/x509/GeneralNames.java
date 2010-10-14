package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObject;

public class GeneralNames
    implements DEREncodable
{
    ASN1Sequence            seq;
    boolean                 isInsideImplicit = false;

    public static GeneralNames getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof GeneralNames)
        {
            return (GeneralNames)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new GeneralNames((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    public static GeneralNames getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public GeneralNames(
        ASN1Sequence  seq)
    {
        this.seq = seq;
    }

    /*
     * this is a hack! But it will have to do until the ambiguity rules
     * get sorted out for implicit/explicit tagging...
     * @deprecated
     */
    public void markInsideImplicit(
        boolean    isInsideImplicit)
    {
        this.isInsideImplicit = isInsideImplicit;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * GeneralNames ::= SEQUENCE SIZE {1..MAX} OF GeneralName
     * </pre>
     */
    public DERObject getDERObject()
    {
        return seq;
    }
}
