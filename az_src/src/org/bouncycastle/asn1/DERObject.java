package org.bouncycastle.asn1;

import java.io.IOException;

public abstract class DERObject
    implements DERTags, DEREncodable
{
    public DERObject getDERObject()
    {
        return this;
    }

    abstract void encode(DEROutputStream out)
        throws IOException;
}
