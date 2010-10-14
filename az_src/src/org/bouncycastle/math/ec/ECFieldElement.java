package org.bouncycastle.math.ec;

import java.math.BigInteger;

public abstract class ECFieldElement
    implements ECConstants
{
	BigInteger x;
	BigInteger p;

	protected ECFieldElement(BigInteger q, BigInteger x)
	{
        if (x.compareTo(q) >= 0)
        {
            throw new IllegalArgumentException("x value too large in field element");
        }

		this.x = x;
		this.p = q; // curve.getQ();
	}

	public BigInteger toBigInteger()
	{
		return x;
	}

	public boolean equals(Object other)
	{
		if ( other == this )
			return true;

		if ( !(other instanceof ECFieldElement) )
			return false;

		ECFieldElement o = (ECFieldElement)other;
		return p.equals(o.p) && x.equals(o.x);
	}

	public abstract String         getFieldName();
	public abstract ECFieldElement add(ECFieldElement b);
	public abstract ECFieldElement subtract(ECFieldElement b);
	public abstract ECFieldElement multiply(ECFieldElement b);
	public abstract ECFieldElement divide(ECFieldElement b);
	public abstract ECFieldElement negate();
	public abstract ECFieldElement square();
	public abstract ECFieldElement invert();
	public abstract ECFieldElement sqrt();

    public static class Fp extends ECFieldElement
    {
        /**
         * return the field name for this field.
         *
         * @return the string "Fp".
         */
        public String getFieldName()
        {
            return "Fp";
        }

        public Fp(BigInteger q, BigInteger x)
        {
            super(q, x);
        }

        public ECFieldElement add(ECFieldElement b)
        {
            return new Fp(p, x.add(b.x).mod(p));
        }

        public ECFieldElement subtract(ECFieldElement b)
        {
            return new Fp(p, x.subtract(b.x).mod(p));
        }

        public ECFieldElement multiply(ECFieldElement b)
        {
            return new Fp(p, x.multiply(b.x).mod(p));
        }

        public ECFieldElement divide(ECFieldElement b)
        {
            return new Fp(p, x.multiply(b.x.modInverse(p)).mod(p));
        }

        public ECFieldElement negate()
        {
            return new Fp(p, x.negate().mod(p));
        }

        public ECFieldElement square()
        {
            return new Fp(p, x.multiply(x).mod(p));
        }

        public ECFieldElement invert()
        {
            return new Fp(p, x.modInverse(p));
        }

        // D.1.4 91
        /**
         * return a sqrt root - the routine verifies that the calculation
         * returns the right value - if none exists it returns null.
         */
        public ECFieldElement sqrt()
        {
            // p mod 4 == 3
            if ( p.testBit(1) )
            {
                // z = g^(u+1) + p, p = 4u + 3
                ECFieldElement z = new Fp(p, x.modPow(p.shiftRight(2).add(ONE), p));

                return z.square().equals(this) ? z : null;
            }

            throw new RuntimeException("not done yet");
        }
    }
}
