package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;

public class AttributeCertificateInfo
    implements DEREncodable
{
    DERInteger              attCertVersion;
    Holder                  holder;
    AttCertIssuer           issuer;
    AlgorithmIdentifier     signature;
    DERInteger              serialNumber;
    AttCertValidityPeriod   attrCertValidityPeriod;
    ASN1Sequence            attributes;
    DERBitString            issuerUniqueID;
    X509Extensions          extensions;

    public DERInteger getAttCertVersion()
    {
        return attCertVersion;
    }

    public Holder getHolder()
    {
        return holder;
    }

    public AttCertIssuer getIssuer()
    {
        return issuer;
    }

    public AlgorithmIdentifier getSignature()
    {
        return signature;
    }

    public DERInteger getSerialNumber()
    {
        return serialNumber;
    }

    public AttCertValidityPeriod getAttrCertValidityPeriod()
    {
        return attrCertValidityPeriod;
    }

    public ASN1Sequence getAttributes()
    {
        return attributes;
    }

    public DERBitString getIssuerUniqueID()
    {
        return issuerUniqueID;
    }

    public X509Extensions getExtensions()
    {
        return extensions;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     *  AttributeCertificateInfo ::= SEQUENCE {
     *       version              AttCertVersion -- version is v2,
     *       holder               Holder,
     *       issuer               AttCertIssuer,
     *       signature            AlgorithmIdentifier,
     *       serialNumber         CertificateSerialNumber,
     *       attrCertValidityPeriod   AttCertValidityPeriod,
     *       attributes           SEQUENCE OF Attribute,
     *       issuerUniqueID       UniqueIdentifier OPTIONAL,
     *       extensions           Extensions OPTIONAL
     *  }
     *
     *  AttCertVersion ::= INTEGER { v2(1) }
     * </pre>
     */
    public DERObject getDERObject()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        return new DERSequence(v);
    }
}
