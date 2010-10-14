package org.bouncycastle.math.ec;

import java.math.BigInteger;

/**
 * base class for an elliptic curve
 */
public abstract class ECCurve
{
	BigInteger q;
	ECFieldElement a, b;

	public ECCurve(BigInteger q, BigInteger a, BigInteger b)
	{
		this.q = q;
		this.a = fromBigInteger(a);
		this.b = fromBigInteger(b);
	}

	public abstract ECFieldElement fromBigInteger(BigInteger x);

	public abstract ECPoint decodePoint(byte[] encoded);

    public ECFieldElement getA()
    {
        return a;
    }

    public ECFieldElement getB()
    {
        return b;
    }

    /**
     * Elliptic curve over Fp
     */
    public static class Fp extends ECCurve
    {
        public Fp(BigInteger q, BigInteger a, BigInteger b)
        {
            super(q, a, b);
        }

        public BigInteger getQ()
        {
            return q;
        }

        public ECFieldElement fromBigInteger(BigInteger x)
        {
            return new ECFieldElement.Fp(this.q, x);
        }

        /**
         * decode a point on this curve which has been encoded using
         * point compression (X9.62 s 4.2.1 pg 17) returning the point.
         */
        public ECPoint decodePoint(byte[] encoded)
        {
            ECPoint p = null;

            switch (encoded[0])
            {
				// compressed
			case 0x02:
			case 0x03:
                int ytilde = encoded[0] & 1;
                byte[]  i = new byte[encoded.length - 1];

                System.arraycopy(encoded, 1, i, 0, i.length);

                ECFieldElement x = new ECFieldElement.Fp(this.q, new BigInteger(1, i));
                ECFieldElement alpha = x.multiply(x.square()).add(x.multiply(a).add(b));
                ECFieldElement beta = alpha.sqrt();

                //
                // if we can't find a sqrt we haven't got a point on the
                // curve - run!
                //
                if (beta == null)
                {
                    throw new RuntimeException("Invalid point compression");
                }

                int bit0 = (beta.toBigInteger().testBit(0) ? 0 : 1);

                if ( bit0 == ytilde )
                {
                    p = new ECPoint.Fp(this, x, beta);
                }
                else
                {
                    p = new ECPoint.Fp(this, x,
                        new ECFieldElement.Fp(this.q, q.subtract(beta.toBigInteger())));
                }
                break;
            case 0x04:
                byte[]  xEnc = new byte[(encoded.length - 1) / 2];
                byte[]  yEnc = new byte[(encoded.length - 1) / 2];

                System.arraycopy(encoded, 1, xEnc, 0, xEnc.length);
                System.arraycopy(encoded, xEnc.length + 1, yEnc, 0, yEnc.length);

                p = new ECPoint.Fp(this,
                        new ECFieldElement.Fp(this.q, new BigInteger(1, xEnc)),
                        new ECFieldElement.Fp(this.q, new BigInteger(1, yEnc)));
                break;
            default:
                throw new RuntimeException("Invalid point encoding 0x" + Integer.toString(encoded[0], 16));
            }

            return p;
        }
    }
}
