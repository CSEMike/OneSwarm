
package org.bouncycastle.jce.provider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CRLException;
import java.security.cert.X509CRLEntry;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.x509.TBSCertList;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;

/**
 * The following extensions are listed in RFC 2459 as relevant to CRL Entries
 *
 * ReasonCode
 * Hode Instruction Code
 * Invalidity Date
 * Certificate Issuer (critical)
 */
public class X509CRLEntryObject extends X509CRLEntry
{
	private TBSCertList.CRLEntry c;

	public X509CRLEntryObject(
		TBSCertList.CRLEntry c)
	{
		this.c = c;
	}

	/**
	 * Will return true if any extensions are present and marked
	 * as critical as we currently dont handle any extensions!
	 */
    public boolean hasUnsupportedCriticalExtension()
	{
		Set extns = getCriticalExtensionOIDs();
		if ( extns != null && !extns.isEmpty() )
		{
			return true;
		}

		return false;
	}

	private Set getExtensionOIDs(boolean critical)
	{
		X509Extensions extensions = c.getExtensions();

		if ( extensions != null )
		{
			HashSet			set = new HashSet();
			Enumeration		e = extensions.oids();

			while (e.hasMoreElements())
			{
				DERObjectIdentifier	oid = (DERObjectIdentifier)e.nextElement();
				X509Extension		ext = extensions.getExtension(oid);

				if (critical == ext.isCritical())
				{
					set.add(oid.getId());
				}
			}

			return set;
		}

		return null;
	}

    public Set getCriticalExtensionOIDs()
	{
		return getExtensionOIDs(true);
	}

    public Set getNonCriticalExtensionOIDs()
	{
		return getExtensionOIDs(false);
	}

    public byte[] getExtensionValue(String oid)
	{
		X509Extensions exts = c.getExtensions();

		if (exts != null)
		{
			X509Extension ext = exts.getExtension(new DERObjectIdentifier(oid));

			if (ext != null)
			{
				return ext.getValue().getOctets();
			}
		}

		return null;
	}

    public byte[] getEncoded()
		throws CRLException
	{
		ByteArrayOutputStream	bOut = new ByteArrayOutputStream();
		DEROutputStream			dOut = new DEROutputStream(bOut);

		try
		{
			dOut.writeObject(c);

			return bOut.toByteArray();
		}
		catch (IOException e)
		{
			throw new CRLException(e.toString());
		}
	}

    public BigInteger getSerialNumber()
	{
		return c.getUserCertificate().getValue();
	}

    public Date getRevocationDate()
	{
		return c.getRevocationDate().getDate();
	}

    public boolean hasExtensions()
	{
		return c.getExtensions() != null;
	}

    public String toString()
	{
		StringBuffer buf = new StringBuffer();
		String nl = System.getProperty("line.separator");

		buf.append("      userCertificate: " + this.getSerialNumber() + nl);
		buf.append("       revocationDate: " + this.getRevocationDate() + nl);


		X509Extensions extensions = c.getExtensions();

		if ( extensions != null )
		{
			Enumeration e = extensions.oids();
			if ( e.hasMoreElements() )
			{
				buf.append("   crlEntryExtensions:" + nl);

				while ( e.hasMoreElements() )
				{
					DERObjectIdentifier oid = (DERObjectIdentifier)e.nextElement();
					X509Extension ext = extensions.getExtension(oid);
					buf.append(ext);
				}
			}
		}

		return buf.toString();
	}
}
