/*
 * Created on Apr 4, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 * 
 */
package org.gudy.azureus2.core3.util.test;


import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SHA1;
import java.security.MessageDigest;

/**
 * 
 */
public class SHA1Verification {
  
  public static final String dirname = "D:" + System.getProperty("file.separator") + "testdir";
  
  public static void main(String[] args) {
    if (! new File( dirname ).exists())  createTestFiles();
    runTests();
  }

  public static void createTestFiles() {
    try {
      System.out.println("Creating test files ... ");
      Random rand = new Random();
      String rootname = "f-";
      
      long[] sizes = { 0, 1, 50000000 };
    
      File testdir = new File( dirname );
      FileUtil.mkdirs(testdir);
   

      
      for (int i=0; i < sizes.length; i++) {
        long size = sizes[i];
        File file = new File( testdir, rootname + String.valueOf( size ));
        System.out.println( file.getName() + "...");
        FileChannel fc = new RandomAccessFile( file, "rw" ).getChannel();
        
        long position = 0;
        while ( position < size ) {
          long remaining = size - position;
          if ( remaining > 1024000 ) remaining = 1024000;
          byte[] buffer = new byte[ new Long(remaining).intValue() ];
          rand.nextBytes( buffer );
          ByteBuffer bb = ByteBuffer.wrap( buffer );
          position += fc.write( bb );
        }
        
        fc.close();
      }
      System.out.println("DONE\n");
    }
    catch (Exception e) { Debug.printStackTrace( e ); }
  }
  
  
	public static void runTests() {
    try {
    
      //SHA1 sha1Jmule = new SHA1();
      MessageDigest sha1Sun = MessageDigest.getInstance("SHA-1");
      SHA1 sha1Gudy = new SHA1();
      //SHA1Az shaGudyResume = new SHA1Az();
    
      ByteBuffer buffer = ByteBuffer.allocate( 1024 * 1024 );
    
      File dir = new File( dirname );
      File[] files = dir.listFiles();

      for (int i=0; i < files.length; i++) {
        FileChannel fc = new RandomAccessFile( files[i], "r" ).getChannel();
        
        System.out.println("Testing " + files[i].getName() + " ...");
        
        while( fc.position() < fc.size() ) {
         fc.read( buffer );
         buffer.flip();
         
         byte[] raw = new byte[ buffer.limit() ];
         System.arraycopy( buffer.array(), 0, raw, 0, raw.length );

         sha1Gudy.update( buffer );
         sha1Gudy.saveState();
         ByteBuffer bb = ByteBuffer.wrap( new byte[56081] );
         sha1Gudy.digest( bb );
         sha1Gudy.restoreState();
         
         sha1Sun.update( raw );
         
         buffer.clear();
        }
        
        byte[] sun = sha1Sun.digest();
        sha1Sun.reset();
        
        byte[] gudy = sha1Gudy.digest();
        sha1Gudy.reset();
        
        if ( Arrays.equals( sun, gudy ) ) {
          System.out.println("  SHA1-Gudy: OK");
        }
        else {
          System.out.println("  SHA1-Gudy: FAILED");
        }
        
        buffer.clear();
        fc.close();
        System.out.println();
      }
    
    }
    catch (Throwable e) { Debug.printStackTrace( e );}
  }
	

}
