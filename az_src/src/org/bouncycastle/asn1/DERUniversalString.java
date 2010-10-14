package org.bouncycastle.asn1;

import java.io.IOException;

/**
 * DER UniversalString object.
 */
public class DERUniversalString
    extends DERObject
    implements DERString
{
    byte[]  string;
    char[]  table = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
     * return a Universal String from the passed in object.
     *
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static DERUniversalString getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof DERUniversalString)
        {
            return (DERUniversalString)obj;
        }

        if (obj instanceof ASN1OctetString)
        {
            return new DERUniversalString(((ASN1OctetString)obj).getOctets());
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    /**
     * return a Universal String from a tagged object.
     *
     * @param obj the tagged object holding the object we want
     * @param explicit true if the object is meant to be explicitly
     *              tagged false otherwise.
     * @exception IllegalArgumentException if the tagged object cannot
     *               be converted.
     */
    public static DERUniversalString getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(obj.getObject());
    }

    /**
     * basic constructor - byte encoded string.
     */
    public DERUniversalString(
        byte[]   string)
    {
        this.string = string;
    }

    /**
     * UniversalStrings have characters which are 4 bytes long - for the
     * moment we just return them in Hex...
     */
    public String getString()
    {
        StringBuffer    buf = new StringBuffer();

        for (int i = 0; i != string.length; i++)
        {
            buf.append(table[(string[i] >>> 4) % 0xf]);
            buf.append(table[string[i] & 0xf]);
        }

        return buf.toString();
    }

    public byte[] getOctets()
    {
        return string;
    }

    void encode(
        DEROutputStream  out)
        throws IOException
    {
        out.writeEncoded(UNIVERSAL_STRING, this.getOctets());
    }
    
    public boolean equals(
        Object  o)
    {
        if ((o == null) || !(o instanceof DERUniversalString))
        {
            return false;
        }

        return this.getString().equals(((DERUniversalString)o).getString());
    }
}
