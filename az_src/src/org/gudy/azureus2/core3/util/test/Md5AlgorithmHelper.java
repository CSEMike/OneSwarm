/*
 * File    : Md5AlgorithmHelper.java
 * Created : 16 avr. 2004
 * By      : Olivier
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
 */
package org.gudy.azureus2.core3.util.test;

/**
 * @author Olivier Chalouhi
 *
 */
public class Md5AlgorithmHelper {
  
  public static final int S11 = 7;
  public static final int S12 = 12;
  public static final int S13 = 17;
  public static final int S14 = 22;
  public static final int S21 = 5;
  public static final int S22 = 9;
  public static final int S23 = 14;
  public static final int S24 = 20;
  public static final int S31 = 4;
  public static final int S32 = 11;
  public static final int S33 = 16;
  public static final int S34 = 23;
  public static final int S41 = 6;
  public static final int S42 = 10;
  public static final int S43 = 15;
  public static final int S44 = 21;
  
  public static void main(String args[]) {    
    /* Round 1 */
    String a = "a";
    String b = "b";
    String c = "c";
    String d = "d";
    
    pFF (a, b, c, d, 0, S11, "0xd76aa478"); /* 1 */
    pFF (d, a, b, c, 1, S12, "0xe8c7b756"); /* 2 */
    pFF (c, d, a, b, 2, S13, "0x242070db"); /* 3 */
    pFF (b, c, d, a, 3, S14, "0xc1bdceee"); /* 4 */
    pFF (a, b, c, d, 4, S11, "0xf57c0faf"); /* 5 */
    pFF (d, a, b, c, 5, S12, "0x4787c62a"); /* 6 */
    pFF (c, d, a, b, 6, S13, "0xa8304613"); /* 7 */
    pFF (b, c, d, a, 7, S14, "0xfd469501"); /* 8 */
    pFF (a, b, c, d, 8, S11, "0x698098d8"); /* 9 */
    pFF (d, a, b, c, 9, S12, "0x8b44f7af"); /* 10 */
    pFF (c, d, a, b, 10, S13,"0xffff5bb1"); /* 11 */
    pFF (b, c, d, a, 11, S14,"0x895cd7be"); /* 12 */
    pFF (a, b, c, d, 12, S11,"0x6b901122"); /* 13 */
    pFF (d, a, b, c, 13, S12,"0xfd987193"); /* 14 */
    pFF (c, d, a, b, 14, S13,"0xa679438e"); /* 15 */
    pFF (b, c, d, a, 15, S14,"0x49b40821"); /* 16 */

   /* Round 2 */
    pGG (a, b, c, d, 1, S21, "0xf61e2562"); /* 17 */
    pGG (d, a, b, c, 6, S22, "0xc040b340"); /* 18 */
    pGG (c, d, a, b, 11, S23,"0x265e5a51"); /* 19 */
    pGG (b, c, d, a, 0, S24, "0xe9b6c7aa"); /* 20 */
    pGG (a, b, c, d, 5, S21, "0xd62f105d"); /* 21 */
    pGG (d, a, b, c, 10, S22,"0x02441453"); /* 22 */
    pGG (c, d, a, b, 15, S23,"0xd8a1e681"); /* 23 */
    pGG (b, c, d, a, 4, S24, "0xe7d3fbc8"); /* 24 */
    pGG (a, b, c, d, 9, S21, "0x21e1cde6"); /* 25 */
    pGG (d, a, b, c, 14, S22,"0xc33707d6"); /* 26 */
    pGG (c, d, a, b, 3, S23, "0xf4d50d87"); /* 27 */
    pGG (b, c, d, a, 8, S24, "0x455a14ed"); /* 28 */
    pGG (a, b, c, d, 13, S21,"0xa9e3e905"); /* 29 */
    pGG (d, a, b, c, 2, S22, "0xfcefa3f8"); /* 30 */
    pGG (c, d, a, b, 7, S23, "0x676f02d9"); /* 31 */
    pGG (b, c, d, a, 12, S24,"0x8d2a4c8a"); /* 32 */

    /* Round 3 */
    pHH (a, b, c, d, 5, S31, "0xfffa3942"); /* 33 */
    pHH (d, a, b, c, 8, S32, "0x8771f681"); /* 34 */
    pHH (c, d, a, b, 11, S33,"0x6d9d6122"); /* 35 */
    pHH (b, c, d, a, 14, S34,"0xfde5380c"); /* 36 */
    pHH (a, b, c, d, 1, S31, "0xa4beea44"); /* 37 */
    pHH (d, a, b, c, 4, S32, "0x4bdecfa9"); /* 38 */
    pHH (c, d, a, b, 7, S33, "0xf6bb4b60"); /* 39 */
    pHH (b, c, d, a, 10, S34,"0xbebfbc70"); /* 40 */
    pHH (a, b, c, d, 13, S31,"0x289b7ec6"); /* 41 */
    pHH (d, a, b, c, 0, S32, "0xeaa127fa"); /* 42 */
    pHH (c, d, a, b, 3, S33, "0xd4ef3085"); /* 43 */
    pHH (b, c, d, a, 6, S34, "0x04881d05"); /* 44 */
    pHH (a, b, c, d, 9, S31, "0xd9d4d039"); /* 45 */
    pHH (d, a, b, c, 12, S32,"0xe6db99e5"); /* 46 */
    pHH (c, d, a, b, 15, S33,"0x1fa27cf8"); /* 47 */
    pHH (b, c, d, a, 2, S34, "0xc4ac5665"); /* 48 */

    /* Round 4 */
    pII (a, b, c, d, 0, S41, "0xf4292244"); /* 49 */
    pII (d, a, b, c, 7, S42, "0x432aff97"); /* 50 */
    pII (c, d, a, b, 14, S43,"0xab9423a7"); /* 51 */
    pII (b, c, d, a, 5, S44, "0xfc93a039"); /* 52 */
    pII (a, b, c, d, 12, S41,"0x655b59c3"); /* 53 */
    pII (d, a, b, c, 3, S42, "0x8f0ccc92"); /* 54 */
    pII (c, d, a, b, 10, S43,"0xffeff47d"); /* 55 */
    pII (b, c, d, a, 1, S44, "0x85845dd1"); /* 56 */
    pII (a, b, c, d, 8, S41, "0x6fa87e4f"); /* 57 */
    pII (d, a, b, c, 15, S42,"0xfe2ce6e0"); /* 58 */
    pII (c, d, a, b, 6, S43, "0xa3014314"); /* 59 */
    pII (b, c, d, a, 13, S44,"0x4e0811a1"); /* 60 */
    pII (a, b, c, d, 4, S41, "0xf7537e82"); /* 61 */
    pII (d, a, b, c, 11, S42,"0xbd3af235"); /* 62 */
    pII (c, d, a, b, 2, S43, "0x2ad7d2bb"); /* 63 */
    pII (b, c, d, a, 9, S44, "0xeb86d391"); /* 64 */
  }
  
  public static String F(String x,String y,String z) {
    return "((" + x + " & " + y + ") | ( ~" + x + " & " + z + "))";
  }
  
  public static String G(String x,String y,String z) {
    return "((" + x + " & " + z + ") | (" + y + " & ~" + z + "))";
  }
  
  public static String H(String x,String y,String z) {
    return "(" + x + " ^ " + y +  " ^ " + z + ")";
  }
  
  public static String I(String x,String y,String z) {
    return "(" + y + " ^ (" + x + "  | ~" + z + "))";
  }
  
  public static String rotateLeft(String x,int n) {
    return "((" + x + " << " + n + ") | (" + x + " >>> " + (32-n) + "))"; 
  }
  
  public static int T(int i) {
    return (int) (4294967296.0 * Math.abs(Math.sin(i)));
  }
  
  public static String pFF(String a,String b,String c,String d,int x, int s, String Ti) {
    String result =  a + " += " + F(b,c,d) + " + x" + x + " + " + Ti + ";";
    result += "\n";
    result += a + " = " + b + " + " + rotateLeft(a,s) + ";";
    System.out.println(result);
    return result;
  }
  
  public static String pGG(String a,String b,String c,String d,int x, int s, String Ti) {
    String result =  a + " += " + G(b,c,d) + " + x" + x + " + " + Ti + ";";
    result += "\n";
    result += a + " = " + b + " + " + rotateLeft(a,s) + ";";
    System.out.println(result);
    return result;
  }
  
  public static String pHH(String a,String b,String c,String d,int x, int s, String Ti) {
    String result =  a + " += " + H(b,c,d) + " + x" + x + " + " + Ti + ";";
    result += "\n";
    result += a + " = " + b + " + " + rotateLeft(a,s) + ";";
    System.out.println(result);
    return result;
  }
  
  public static String pII(String a,String b,String c,String d,int x, int s, String Ti) {
    String result =  a + " += " + I(b,c,d) + " + x" + x + " + " + Ti + ";";
    result += "\n";
    result += a + " = " + b + " + " + rotateLeft(a,s) + ";";
    System.out.println(result);
    return result;
  }
  
}
