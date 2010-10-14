package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERTaggedObject;

/**
 * The DistributionPointName object.
 * <pre>
 * DistributionPointName ::= CHOICE {
 *     fullName                 [0] GeneralNames,
 *     nameRelativeToCRLIssuer  [1] RelativeDistinguishedName
 * }
 * </pre>
 */
public class DistributionPointName
    extends ASN1Encodable
{
    DEREncodable        name;
    int                 type;

    public static final int FULL_NAME = 0;
    public static final int NAME_RELATIVE_TO_CRL_ISSUER = 1;

	public static DistributionPointName getInstance(
		ASN1TaggedObject obj,
		boolean          explicit)
	{
		return getInstance(ASN1TaggedObject.getInstance(obj, explicit));
	}

	public static DistributionPointName getInstance(
		Object  obj)
	{
		if (obj == null || obj instanceof DistributionPointName)
		{
			return (DistributionPointName)obj;
		}
		else if (obj instanceof ASN1TaggedObject)
		{
			return new DistributionPointName((ASN1TaggedObject)obj);
		}

		throw new IllegalArgumentException("unknown object in factory");
	}

    public DistributionPointName(
        int             		type,
        DEREncodable    name)
    {
        this.type = type;
        this.name = name;
    }

	public DistributionPointName(
		ASN1TaggedObject	obj)
	{
		this.type = obj.getTagNo();
		
		if (type == 0)
		{
			this.name = ASN1Sequence.getInstance(obj, false);
		}
		else
		{
			this.name = ASN1Set.getInstance(obj, false);
		}
	}
	
    public DERObject toASN1Object()
    {
        return new DERTaggedObject(false, type, name);
    }
}
