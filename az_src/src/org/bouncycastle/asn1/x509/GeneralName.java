package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERTaggedObject;

/**
 * The GeneralName object.
 * <pre>
 * GeneralName ::= CHOICE {
 *      otherName                       [0]     OtherName,
 *      rfc822Name                      [1]     IA5String,
 *      dNSName                         [2]     IA5String,
 *      x400Address                     [3]     ORAddress,
 *      directoryName                   [4]     Name,
 *      ediPartyName                    [5]     EDIPartyName,
 *      uniformResourceIdentifier       [6]     IA5String,
 *      iPAddress                       [7]     OCTET STRING,
 *      registeredID                    [8]     OBJECT IDENTIFIER}
 *
 * OtherName ::= SEQUENCE {
 *      type-id    OBJECT IDENTIFIER,
 *      value      [0] EXPLICIT ANY DEFINED BY type-id }
 *
 * EDIPartyName ::= SEQUENCE {
 *      nameAssigner            [0]     DirectoryString OPTIONAL,
 *      partyName               [1]     DirectoryString }
 * </pre>
 */
public class GeneralName
    implements DEREncodable
{
    DEREncodable  	obj;
    int           	tag;
	boolean			isInsideImplicit = false;		// if we are in an implicitly tagged object

    public GeneralName(
        X509Name  directoryName)
    {
        this.obj = directoryName;
        this.tag = 4;
    }

    /**
     * When the subjectAltName extension contains an Internet mail address,
     * the address MUST be included as an rfc822Name. The format of an
     * rfc822Name is an "addr-spec" as defined in RFC 822 [RFC 822].
     *
     * When the subjectAltName extension contains a domain name service
     * label, the domain name MUST be stored in the dNSName (an IA5String).
     * The name MUST be in the "preferred name syntax," as specified by RFC
     * 1034 [RFC 1034].
     *
     * When the subjectAltName extension contains a URI, the name MUST be
     * stored in the uniformResourceIdentifier (an IA5String). The name MUST
     * be a non-relative URL, and MUST follow the URL syntax and encoding
     * rules specified in [RFC 1738].  The name must include both a scheme
     * (e.g., "http" or "ftp") and a scheme-specific-part.  The scheme-
     * specific-part must include a fully qualified domain name or IP
     * address as the host.
     *
     * When the subjectAltName extension contains a iPAddress, the address
     * MUST be stored in the octet string in "network byte order," as
     * specified in RFC 791 [RFC 791]. The least significant bit (LSB) of
     * each octet is the LSB of the corresponding byte in the network
     * address. For IP Version 4, as specified in RFC 791, the octet string
     * MUST contain exactly four octets.  For IP Version 6, as specified in
     * RFC 1883, the octet string MUST contain exactly sixteen octets [RFC
     * 1883].
     */
    public GeneralName(
        DERObject name, int tag)
    {
        this.obj = name;
        this.tag = tag;
    }

    public static GeneralName getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof GeneralName)
        {
            return (GeneralName)obj;
        }

        if (obj instanceof ASN1TaggedObject)
        {
            ASN1TaggedObject    tagObj = (ASN1TaggedObject)obj;
            int tag = tagObj.getTagNo();

            switch (tag)
            {
            case 0:
                return new GeneralName(tagObj.getObject(), tag);
            case 1:
                return new GeneralName(DERIA5String.getInstance(tagObj, false), tag);
            case 2:
                return new GeneralName(DERIA5String.getInstance(tagObj, false), tag);
            case 3:
                throw new IllegalArgumentException("unknown tag: " + tag);
            case 4:
                return new GeneralName(tagObj.getObject(), tag);
            case 5:
                return new GeneralName(tagObj.getObject(), tag);
            case 6:
                return new GeneralName(DERIA5String.getInstance(tagObj, false), tag);
            case 7:
                return new GeneralName(ASN1OctetString.getInstance(tagObj, false), tag);
            case 8:
                return new GeneralName(DERObjectIdentifier.getInstance(tagObj, false), tag);
            }
        }

        throw new IllegalArgumentException("unknown object in getInstance");
    }

    public static GeneralName getInstance(
        ASN1TaggedObject tagObj,
        boolean          explicit)
    {
        return GeneralName.getInstance(ASN1TaggedObject.getInstance(tagObj, explicit));
    }

    /**
     * mark whether or not we are contained inside an implicitly tagged
     * object.
     * @deprecated
     */
	public void markInsideImplicit(
		boolean		isInsideImplicit)
	{
		this.isInsideImplicit = isInsideImplicit;
	}

    public int getTagNo()
    {
        return tag;
    }

    public DEREncodable getName()
    {
        return obj;
    }

    public DERObject getDERObject()
    {
        if (tag == 4)       // directoryName is explicitly tagged!
        {
            return new DERTaggedObject(true, tag, obj);
        }
        else
        {
            return new DERTaggedObject(false, tag, obj);
        }
    }
}
