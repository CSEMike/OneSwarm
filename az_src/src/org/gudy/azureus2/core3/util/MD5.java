/*
 * Created on 16 avr. 2004
 * Created by Olivier Chalouhi
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 */
package org.gudy.azureus2.core3.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;

/**
 * @author Olivier Chalouhi
 *
 */
public class MD5 {
  
  private int h0,h1,h2,h3;
  private int length;
  private ByteBuffer finalBuffer;
  
  public MD5() {
    finalBuffer = ByteBuffer.allocate(64);
    finalBuffer.position(0);
    finalBuffer.limit(64);
    
    reset();
  }
  
  public void transform(ByteBuffer M) {    
    int x0 , x1 , x2 , x3 ,  x4 , x5 , x6 , x7 , x8 , x9 ,
    x10, x11, x12, x13, x14, x15;
    
    int a,b,c,d;
    
    /*
     * Crazy byte order for MD5 ... took me hours to find out
     * where the problem was .... words (32 bits) must be read starting
     * with the least significant byte !
     */
    ByteOrder order = M.order();
    M.order(ByteOrder.LITTLE_ENDIAN);
    
    x0 = M.getInt();
    x1 = M.getInt();
    x2 = M.getInt();
    x3 = M.getInt();
    x4 = M.getInt();
    x5 = M.getInt();
    x6 = M.getInt();
    x7 = M.getInt();
    x8 = M.getInt();
    x9 = M.getInt();
    x10 = M.getInt();
    x11 = M.getInt();
    x12 = M.getInt();
    x13 = M.getInt();
    x14 = M.getInt();
    x15 = M.getInt();
    
    M.order(order);
    
    a = h0 ; b = h1 ; c = h2 ; d = h3 ;
    
    
    a += ((b & c) | ( ~b & d)) + x0 + 0xd76aa478;
    a = b + ((a << 7) | (a >>> 25));
    d += ((a & b) | ( ~a & c)) + x1 + 0xe8c7b756;
    d = a + ((d << 12) | (d >>> 20));
    c += ((d & a) | ( ~d & b)) + x2 + 0x242070db;
    c = d + ((c << 17) | (c >>> 15));
    b += ((c & d) | ( ~c & a)) + x3 + 0xc1bdceee;
    b = c + ((b << 22) | (b >>> 10));
    a += ((b & c) | ( ~b & d)) + x4 + 0xf57c0faf;
    a = b + ((a << 7) | (a >>> 25));
    d += ((a & b) | ( ~a & c)) + x5 + 0x4787c62a;
    d = a + ((d << 12) | (d >>> 20));
    c += ((d & a) | ( ~d & b)) + x6 + 0xa8304613;
    c = d + ((c << 17) | (c >>> 15));
    b += ((c & d) | ( ~c & a)) + x7 + 0xfd469501;
    b = c + ((b << 22) | (b >>> 10));
    a += ((b & c) | ( ~b & d)) + x8 + 0x698098d8;
    a = b + ((a << 7) | (a >>> 25));
    d += ((a & b) | ( ~a & c)) + x9 + 0x8b44f7af;
    d = a + ((d << 12) | (d >>> 20));
    c += ((d & a) | ( ~d & b)) + x10 + 0xffff5bb1;
    c = d + ((c << 17) | (c >>> 15));
    b += ((c & d) | ( ~c & a)) + x11 + 0x895cd7be;
    b = c + ((b << 22) | (b >>> 10));
    a += ((b & c) | ( ~b & d)) + x12 + 0x6b901122;
    a = b + ((a << 7) | (a >>> 25));
    d += ((a & b) | ( ~a & c)) + x13 + 0xfd987193;
    d = a + ((d << 12) | (d >>> 20));
    c += ((d & a) | ( ~d & b)) + x14 + 0xa679438e;
    c = d + ((c << 17) | (c >>> 15));
    b += ((c & d) | ( ~c & a)) + x15 + 0x49b40821;
    b = c + ((b << 22) | (b >>> 10));
    
    a += ((b & d) | (c & ~d)) + x1 + 0xf61e2562;
    a = b + ((a << 5) | (a >>> 27));
    d += ((a & c) | (b & ~c)) + x6 + 0xc040b340;
    d = a + ((d << 9) | (d >>> 23));
    c += ((d & b) | (a & ~b)) + x11 + 0x265e5a51;
    c = d + ((c << 14) | (c >>> 18));
    b += ((c & a) | (d & ~a)) + x0 + 0xe9b6c7aa;
    b = c + ((b << 20) | (b >>> 12));
    a += ((b & d) | (c & ~d)) + x5 + 0xd62f105d;
    a = b + ((a << 5) | (a >>> 27));
    d += ((a & c) | (b & ~c)) + x10 + 0x2441453;
    d = a + ((d << 9) | (d >>> 23));
    c += ((d & b) | (a & ~b)) + x15 + 0xd8a1e681;
    c = d + ((c << 14) | (c >>> 18));
    b += ((c & a) | (d & ~a)) + x4 + 0xe7d3fbc8;
    b = c + ((b << 20) | (b >>> 12));
    a += ((b & d) | (c & ~d)) + x9 + 0x21e1cde6;
    a = b + ((a << 5) | (a >>> 27));
    d += ((a & c) | (b & ~c)) + x14 + 0xc33707d6;
    d = a + ((d << 9) | (d >>> 23));
    c += ((d & b) | (a & ~b)) + x3 + 0xf4d50d87;
    c = d + ((c << 14) | (c >>> 18));
    b += ((c & a) | (d & ~a)) + x8 + 0x455a14ed;
    b = c + ((b << 20) | (b >>> 12));
    a += ((b & d) | (c & ~d)) + x13 + 0xa9e3e905;
    a = b + ((a << 5) | (a >>> 27));
    d += ((a & c) | (b & ~c)) + x2 + 0xfcefa3f8;
    d = a + ((d << 9) | (d >>> 23));
    c += ((d & b) | (a & ~b)) + x7 + 0x676f02d9;
    c = d + ((c << 14) | (c >>> 18));
    b += ((c & a) | (d & ~a)) + x12 + 0x8d2a4c8a;
    b = c + ((b << 20) | (b >>> 12));
    a += (b ^ c ^ d) + x5 + 0xfffa3942;
    a = b + ((a << 4) | (a >>> 28));
    d += (a ^ b ^ c) + x8 + 0x8771f681;
    d = a + ((d << 11) | (d >>> 21));
    c += (d ^ a ^ b) + x11 + 0x6d9d6122;
    c = d + ((c << 16) | (c >>> 16));
    b += (c ^ d ^ a) + x14 + 0xfde5380c;
    b = c + ((b << 23) | (b >>> 9));
    a += (b ^ c ^ d) + x1 + 0xa4beea44;
    a = b + ((a << 4) | (a >>> 28));
    d += (a ^ b ^ c) + x4 + 0x4bdecfa9;
    d = a + ((d << 11) | (d >>> 21));
    c += (d ^ a ^ b) + x7 + 0xf6bb4b60;
    c = d + ((c << 16) | (c >>> 16));
    b += (c ^ d ^ a) + x10 + 0xbebfbc70;
    b = c + ((b << 23) | (b >>> 9));
    a += (b ^ c ^ d) + x13 + 0x289b7ec6;
    a = b + ((a << 4) | (a >>> 28));
    d += (a ^ b ^ c) + x0 + 0xeaa127fa;
    d = a + ((d << 11) | (d >>> 21));
    c += (d ^ a ^ b) + x3 + 0xd4ef3085;
    c = d + ((c << 16) | (c >>> 16));
    b += (c ^ d ^ a) + x6 + 0x4881d05;
    b = c + ((b << 23) | (b >>> 9));
    a += (b ^ c ^ d) + x9 + 0xd9d4d039;
    a = b + ((a << 4) | (a >>> 28));
    d += (a ^ b ^ c) + x12 + 0xe6db99e5;
    d = a + ((d << 11) | (d >>> 21));
    c += (d ^ a ^ b) + x15 + 0x1fa27cf8;
    c = d + ((c << 16) | (c >>> 16));
    b += (c ^ d ^ a) + x2 + 0xc4ac5665;
    b = c + ((b << 23) | (b >>> 9));
    a += (c ^ (b  | ~d)) + x0 + 0xf4292244;
    a = b + ((a << 6) | (a >>> 26));
    d += (b ^ (a  | ~c)) + x7 + 0x432aff97;
    d = a + ((d << 10) | (d >>> 22));
    c += (a ^ (d  | ~b)) + x14 + 0xab9423a7;
    c = d + ((c << 15) | (c >>> 17));
    b += (d ^ (c  | ~a)) + x5 + 0xfc93a039;
    b = c + ((b << 21) | (b >>> 11));
    a += (c ^ (b  | ~d)) + x12 + 0x655b59c3;
    a = b + ((a << 6) | (a >>> 26));
    d += (b ^ (a  | ~c)) + x3 + 0x8f0ccc92;
    d = a + ((d << 10) | (d >>> 22));
    c += (a ^ (d  | ~b)) + x10 + 0xffeff47d;
    c = d + ((c << 15) | (c >>> 17));
    b += (d ^ (c  | ~a)) + x1 + 0x85845dd1;
    b = c + ((b << 21) | (b >>> 11));
    a += (c ^ (b  | ~d)) + x8 + 0x6fa87e4f;
    a = b + ((a << 6) | (a >>> 26));
    d += (b ^ (a  | ~c)) + x15 + 0xfe2ce6e0;
    d = a + ((d << 10) | (d >>> 22));
    c += (a ^ (d  | ~b)) + x6 + 0xa3014314;
    c = d + ((c << 15) | (c >>> 17));
    b += (d ^ (c  | ~a)) + x13 + 0x4e0811a1;
    b = c + ((b << 21) | (b >>> 11));
    a += (c ^ (b  | ~d)) + x4 + 0xf7537e82;
    a = b + ((a << 6) | (a >>> 26));
    d += (b ^ (a  | ~c)) + x11 + 0xbd3af235;
    d = a + ((d << 10) | (d >>> 22));
    c += (a ^ (d  | ~b)) + x2 + 0x2ad7d2bb;
    c = d + ((c << 15) | (c >>> 17));
    b += (d ^ (c  | ~a)) + x9 + 0xeb86d391;
    b = c + ((b << 21) | (b >>> 11));

    
    h0 += a;
    h1 += b;
    h2 += c;
    h3 += d;    
  }
  
  /**
   * Resets the MD5 to initial state for a new message digest calculation.
   * Must be called before starting a new hash calculation.
   */
  public void reset() {
    h0 = 0x67452301;
    h1 = 0xEFCDAB89;
    h2 = 0x98BADCFE;
    h3 = 0x10325476;   
    
    length = 0;
    
    finalBuffer.clear();
  }
  
  private void completeFinalBuffer(ByteBuffer buffer) {
    if(finalBuffer.position() == 0) 
      return;
    
    while(buffer.remaining() > 0 && finalBuffer.remaining() > 0) {
      finalBuffer.put(buffer.get());
    }
    
    if(finalBuffer.remaining() == 0) {
      finalBuffer.position(0);
      transform(finalBuffer);
      finalBuffer.position(0);
    }
  }
  
  
  /**
   * Starts or continues a MD5 message digest calculation.
   * Only the remaining bytes of the given ByteBuffer are used.
   * @param buffer input data
   */
  public void update(ByteBuffer buffer) {
    length += buffer.remaining();
    //Save current position to leave given buffer unchanged
    int position = buffer.position();
    
    //Complete the final buffer if needed
    completeFinalBuffer(buffer);
    
    while(buffer.remaining() >= 64) {
      transform(buffer);
    }
    
    if(buffer.remaining() != 0) {
      finalBuffer.put(buffer);
    }
    
    buffer.position(position);
  }
  
  
  /**
   * Finishes the MD5-1 message digest calculation.
   * @return 16-byte hash result
   */
  public byte[] digest() {
    byte[] result = new byte[16];
    
    finalBuffer.put((byte)0x80);
    if(finalBuffer.remaining() < 8) {
      while(finalBuffer.remaining() > 0) {
        finalBuffer.put((byte)0);
      }
      finalBuffer.position(0);
      transform(finalBuffer);
      finalBuffer.position(0);
    }
    
    while(finalBuffer.remaining() > 8) {
      finalBuffer.put((byte)0);
    }
    
    finalBuffer.putLong(length << 3);
    finalBuffer.position(0);
    transform(finalBuffer);
    
    finalBuffer.position(0);
    finalBuffer.putInt(h3);
    finalBuffer.putInt(h2);
    finalBuffer.putInt(h1);
    finalBuffer.putInt(h0);    
    finalBuffer.position(0);
    
    for(int i  = 0 ; i < 16 ; i++) {
     result[15-i] = finalBuffer.get(); 
    }
    
    return result;
  }
  
  
  /**
   * Finishes the MD5 message digest calculation, by first performing a final update
   * from the given input buffer, then completing the calculation as with digest().
   * @param buffer input data
   * @return 16-byte hash result
   */
  public byte[] digest(ByteBuffer buffer) {
    update( buffer );
    return digest();
  }
  
  public static void main(String args[]) throws Exception {
    MD5 md5Gudy = new MD5();
    BrokenMd5Hasher md5Jmule = new BrokenMd5Hasher();
    MessageDigest md5Sun = MessageDigest.getInstance("MD5");
    
    ByteBuffer bhashJ = ByteBuffer.allocate(16);
    
    
    System.out.println("Gudy : " + ByteFormatter.nicePrint(md5Gudy.digest()));
    md5Gudy.reset();
    md5Jmule.finalDigest(bhashJ);
    bhashJ.rewind();      
    byte hashJ[] = bhashJ.array();
    System.out.println("Jmule: " + ByteFormatter.nicePrint(hashJ));
    System.out.println("Sun: " + ByteFormatter.nicePrint(md5Sun.digest()));
    
    for(int i = 0 ; i < 1 ; i++) {
      ByteBuffer test = ByteBuffer.allocate(i);
      while(test.remaining() > 0) {
        test.put((byte)(Math.random() * 256));
      }
      test.rewind();
      byte hashG[] = md5Gudy.digest(test);
      md5Gudy.reset();
      
      md5Jmule.update(test);      
      bhashJ.rewind();
      md5Jmule.finalDigest(bhashJ);
      bhashJ.rewind();      
      hashJ = bhashJ.array();
      test.rewind();
      
      md5Sun.update(test.array());
      byte hashS[] = md5Sun.digest();
      
      System.out.println("Gudy : " + ByteFormatter.nicePrint(hashG));
      System.out.println("Jmule: " + ByteFormatter.nicePrint(hashJ));
      System.out.println("Sun: " + ByteFormatter.nicePrint(hashS));
      //boolean same = true;

      //System.out.println(i + " : " + same);      
    }
  }
  
}
