package org.bouncycastle.asn1;

import java.io.IOException;

public class BERSetParser
    implements ASN1SetParser
{
    private ASN1ObjectParser _parser;

    BERSetParser(ASN1ObjectParser parser)
    {
        this._parser = parser;
    }

    public DEREncodable readObject()
        throws IOException
    {
        return _parser.readObject();
    }

    public DERObject getDERObject()
    {
        return new BERSet(_parser.readVector());
    }
}
