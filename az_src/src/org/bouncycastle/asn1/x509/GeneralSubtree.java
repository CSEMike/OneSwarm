package org.bouncycastle.asn1.x509;

import java.math.BigInteger;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

public class GeneralSubtree
    implements DEREncodable
{
	private GeneralName  base;
	private DERInteger minimum;
	private DERInteger maximum;

	public GeneralSubtree(
        ASN1Sequence seq)
    {
		base = GeneralName.getInstance(seq.getObjectAt(0));
		
		switch (seq.size())
        {
        case 1:
            break;
        case 2:
            ASN1TaggedObject o = (ASN1TaggedObject)seq.getObjectAt(1);
            switch (o.getTagNo())
            {
            case 0 :
                minimum = DERInteger.getInstance(o, false);
                break;
            case 1 :
                maximum = DERInteger.getInstance(o, false);
                break;
            default:
                throw new IllegalArgumentException("Bad tag number: " + o.getTagNo());
            }
            break;
        case 3 :
            minimum = DERInteger.getInstance((ASN1TaggedObject)seq.getObjectAt(1), false);
            maximum = DERInteger.getInstance((ASN1TaggedObject)seq.getObjectAt(2), false);
            break;
        default:
            throw new IllegalArgumentException("Bad sequence size: " + seq.size());
		}
	}

	public static GeneralSubtree getInstance(
        ASN1TaggedObject    o,
        boolean             explicit)
    {
		return new GeneralSubtree(ASN1Sequence.getInstance(o, explicit));
	}

	public static GeneralSubtree getInstance(
        Object obj)
    {
		if (obj == null)
        {
			return null;
		}

		if (obj instanceof GeneralSubtree)
        {
			return (GeneralSubtree)obj;
		}

		return new GeneralSubtree(ASN1Sequence.getInstance(obj));
	}

	public GeneralName getBase()
    {
		return base;
	}
	
	public BigInteger getMinimum()
    {
		if (minimum == null)
        {
			return BigInteger.valueOf(0);
		}
		
		return minimum.getValue();
	}
	
	public BigInteger getMaximum()
    {
        if (maximum == null)
        {
            return null;
        }

		return maximum.getValue();
	}
	
    /* 
     * GeneralSubtree ::= SEQUENCE {
     *      base                    GeneralName,
     *      minimum         [0]     BaseDistance DEFAULT 0,
     *      maximum         [1]     BaseDistance OPTIONAL }
     */ 
	public DERObject getDERObject()
    {
		ASN1EncodableVector v = new ASN1EncodableVector();

		v.add(base);
		
		if (minimum != null)
        {
			v.add(new DERTaggedObject(false, 0, minimum));
		}
		
		if (maximum != null)
        {
			v.add(new DERTaggedObject(false, 1, maximum));
		}

		return new DERSequence(v);
	}
}
