package edu.uw.cse.netlab.utils;

import java.io.Serializable;

/**
 * This class compensates for some apparent bugs in the serialization of Java's built-in BitSet object (either on Linux or OS X)
 */
public class BitArray implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	int size;	
    int[] back;
	
	public BitArray( int n ) {
        size = n;
        back = new int[(size+31)/32];
    }
	
    public final int length() {
        return size;
    }
    
    public int hashCode() 
    {
    	long h = 1234;
    	for (int i = back.length; --i >= 0; )
                h ^= back[i] * (i + 1);

    	return (int)((h >> 32) ^ h);
    }
    
    public boolean equals( Object in )
    {
    	if( in instanceof BitArray == false ) {
    		return false;
    	}
    	BitArray rhs = (BitArray)in;
    	
    	if( rhs.back.length != back.length )
    		return false;
    	
    	for( int i=0; i<back.length; i++ )
    		if( rhs.back[i] != back[i] )
    			return false;
    	
    	return true;
    }
    
    public boolean get( int bit ) {
        return 0 != ( back[bit/32] & ( 1<<(bit%32) ) );
    }

    public final void set( int bit ) {
        back[bit/32] |= 1<<(bit%32);
    }

    public final void clear( int bit ) {
    	back[bit/32] &= ~(1<<(bit%32));
    }
    
    public final void clear() {
        for ( int i=0; i<back.length; i++ )
            back[i] = 0;
    }

    public final void flip( int bit ) {
    	back[bit/32] ^= 1<<(bit%32);
    }
    
	/**
	 * @param args
	 */
	public static void main( String[] args ) 
	{
		
	}

}
