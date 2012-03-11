package org.gudy.azureus2.core3.util;

import java.io.File;

public class ShellUtilityFinder {
	
	
	public static String getChMod() {
		return findCommand("chmod");
	}
	
	public static String getNice() {
		return findCommand("nice");
	}
	
	public static String
	findCommand(
	  String name )
	{
	  final String[] locations = { "/bin", "/usr/bin" };
	  for ( String s: locations ){
	      File f = new File( s, name );
	      if ( f.exists() && f.canRead()){
	          return( f.getAbsolutePath());
	      }
	  }
	  return( name );
	} 

}
