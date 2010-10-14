package org.bouncycastle.asn1.x509;

import java.util.Enumeration;

import org.bouncycastle.asn1.*;

/**
 * The AuthorityInformationAccess object.
 * <pre>
 * id-pe-authorityInfoAccess OBJECT IDENTIFIER ::= { id-pe 1 }
 *
 * AuthorityInfoAccessSyntax  ::=
 *      SEQUENCE SIZE (1..MAX) OF AccessDescription
 * AccessDescription  ::=  SEQUENCE {
 *       accessMethod          OBJECT IDENTIFIER,
 *       accessLocation        GeneralName  }
 *
 * id-ad OBJECT IDENTIFIER ::= { id-pkix 48 }
 * id-ad-caIssuers OBJECT IDENTIFIER ::= { id-ad 2 }
 * id-ad-ocsp OBJECT IDENTIFIER ::= { id-ad 1 }
 * </pre>
 */
public class AuthorityInformationAccess
    extends ASN1Encodable
{
    DERObjectIdentifier accessMethod=null;
    GeneralName accessLocation=null;

    public AuthorityInformationAccess getInstance(
        Object  obj)
    {
        if (obj instanceof AuthorityInformationAccess)
        {
            return (AuthorityInformationAccess)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new AuthorityInformationAccess((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory");
    }
 
    public AuthorityInformationAccess(
        ASN1Sequence   seq)
    {
        Enumeration     e = seq.getObjects();

        if (e.hasMoreElements())
        {
            DERSequence vec= (DERSequence)e.nextElement();
            if (vec.size() != 2) 
            {
                throw new IllegalArgumentException("wrong number of elements in inner sequence");
            }
            accessMethod = (DERObjectIdentifier)vec.getObjectAt(0);
            accessLocation = (GeneralName)vec.getObjectAt(1);
        }
    }

    /**
     * create an AuthorityInformationAccess with the oid and location provided.
     */
    public AuthorityInformationAccess(
        DERObjectIdentifier oid,
        GeneralName location)
    {
        accessMethod = oid;
        accessLocation = location;
    }

    public DERObject toASN1Object()
    {
        ASN1EncodableVector accessDescription  = new ASN1EncodableVector();
        accessDescription.add(accessMethod);
        accessDescription.add(accessLocation);
        ASN1EncodableVector vec = new ASN1EncodableVector();
        vec.add(new DERSequence(accessDescription));
        return new DERSequence(vec);
    }

    public String toString()
    {
        return ("AuthorityInformationAccess: Oid(" + this.accessMethod.getId() + ")");
    }
}
