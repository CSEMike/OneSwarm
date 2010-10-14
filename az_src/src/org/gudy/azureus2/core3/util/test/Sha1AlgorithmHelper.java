/*
 * File    : Sha1AlgorithmHelper.java
 * Created : 12 mars 2004
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
 * @author Olivier
 * 
 */
public class Sha1AlgorithmHelper {
  
  public static void main(String args[]) {
    algorithm2NoShift();
  }    
  
  //SHA1 Algorithm v2 using only 16 ints and changing variable meaning
  //Over a period of 5 iterations.
  
  // A SHA-1 Basic operation can be discribed as:
  // temp = F(a,b,c,d,e);
  // e = d ; d = c ; c = G(b) ; b = a ; a = temp;
  // The Goal of changing the variable meaning is to remove the allocations
  // made.
  // In order to do, let's replace the role of :
  //
  // e by d,
  // d by c,
  // c by b
  // b by a
  // a by e
  //
  // If we rewrite the 1st equation, we can see that :
  // e = F(a,b,c,d,e); and b = G(b);
  // However, next line will be :
  // d = F(b,c,d,e,a); and a = G(a);
  // Next will be :
  // c = F(c,d,e,a,b); and e = G(e);
  // b = F(d,e,a,b,c); and d = G(d);
  // a = F(e,a,b,c,d); and c = G(c);
  // And we loop to first 'kind' where :
  // e = F(a,b,c,d,e), and b = G(b);
  
  // On 80th iteration (that is for t=79), we've looped 80 = 5 * 16,
  // so we're back to the classic representation of variables a,b,c,d,e
  // and we can directly use them to increment the h0,h1,h2,h3,h4 variables.
  
  public static void algorithm2NoShift() {
    String variables = "abcde";
    int mask = 0x0000000F;
    for(int t = 0 ; t <= 79 ; t++) {
      String a = "" + variables.charAt((85-t)%5);
      String b = "" + variables.charAt((85-t+1)%5);
      String c = "" + variables.charAt((85-t+2)%5);
      String d = "" + variables.charAt((85-t+3)%5);
      String e = "" + variables.charAt((85-t+4)%5);
      
      int s = t & mask;
      if(t >= 16) {
        System.out.println("w" + s + " = w" + ((s+13) & mask) + " ^ w" + ((s+8) & mask) + " ^ w" + ((s+2) & mask) + " ^ w" + s + "; w" + s + " = (w" + s + " << 1) | (w" + s + " >>> 31) ;");        
      }
      System.out.print(e + " += ((" + a + " << 5) | ( " + a + " >>> 27)) + w" + s + " + ");
      int ft = t / 20;
      if(ft == 0) {
        System.out.println("((" + b +" & " + c + ") | ((~" + b +" ) & " + d + ")) + 0x5A827999 ;");
      }
      if(ft == 1) {
        System.out.println("(" + b +" ^ " + c + " ^ " + d + ") + 0x6ED9EBA1 ;");
      }
      if(ft == 2) {
        System.out.println("((" + b +" & " + c + ") | (" + b + " & " + d + ") | (" + c + " & " + d + ")) + 0x8F1BBCDC ;");
      }
      if(ft == 3) {
        System.out.println("(" + b + " ^ " + c + " ^ " + d + ") + 0xCA62C1D6 ;");
      }
      System.out.println(b + " = (" + b + " << 30) | (" + b + " >>> 2) ;");
    }
  }
  
  
  //SHA1 Algorithm v2 using only 16 ints (+ a,b,c,d,e and temp)
  public static void algorithm2() {
    int mask = 0x0000000F;
  	for(int t = 0 ; t <= 79 ; t++) {
  		int s = t & mask;
      if(t >= 16) {
        System.out.println("w" + s + " = w" + ((s+13) & mask) + " ^ w" + ((s+8) & mask) + " ^ w" + ((s+2) & mask) + " ^ w" + s + "; w" + s + " = (w" + s + " << 1) | (w" + s + " >>> 31) ;");        
      }
      System.out.print("temp = ((a << 5) | (a >>> 27)) + e + w" + s + " + ");
      int ft = t / 20;
      if(ft == 0) {
        System.out.println("((b & c) | ((~b) & d)) + 0x5A827999 ;");
      }
      if(ft == 1) {
        System.out.println("(b ^ c ^ d) + 0x6ED9EBA1 ;");
      }
      if(ft == 2) {
        System.out.println("((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;");
      }
      if(ft == 3) {
        System.out.println("(b ^ c ^ d) + 0xCA62C1D6 ;");
      }
      System.out.println("e=d ; d=c ; c = (b << 30) | (b >>> 2) ; b=a ; a=temp;");
  	}
  }
  
  public static void part1() {
    for(int t = 16 ; t <= 79 ; t++) {
    	System.out.println( "w" + t + " = w" + (t-3) + " ^ w" + (t-8) + " ^ w" + (t-14) + " ^ w" + (t-16) + ";");
    	System.out.println( "w" + t + " = (w" + t + " << 1) | (w" + t + " >>> 31);");
    }
  }
  
  public static void part2() {
  	for(int t=0; t<= 79 ; t++) {
      int fn = t / 20;      
      System.out.print("temp = ((a << 5) | (a >>> 27)) + e + w" + t + " + ");
      if(fn == 0) {
        System.out.println("((b & c) | ((~b) & d)) + 0x5A827999 ;");
      }
      if(fn == 1) {
        System.out.println("(b ^ c ^ d) + 0x6ED9EBA1 ;");
      }
      if(fn == 2) {
        System.out.println("((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;");
      }
      if(fn == 3) {
        System.out.println("(b ^ c ^ d) + 0xCA62C1D6 ;");
      }
      System.out.println("e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;");
  	}
  }

}
