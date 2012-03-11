/*
 * Created on 07-Jun-2004
 * Created by Paul Gardner
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

package org.gudy.azureus2.core3.util.jar;

/**
 * @author parg
 *
 */

public class 
AEJarSigner 
{
}

/*
import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import java.util.jar.*;

import org.gudy.azureus2.core3.util.Debug;

import sun.misc.BASE64Encoder;

import sun.security.util.ManifestDigester;

import sun.security.util.SignatureFile;


public class 
WUJarSigner 
{
	protected static final String	MANIFEST_NAME	= "META-INF/MANIFEST.MF";
	
	protected String alias;
	protected PrivateKey privateKey;
	protected X509Certificate[] certChain;

	public 
	WUJarSigner( 
		String 					_alias, 
		PrivateKey 				_privateKey, 
		X509Certificate[] 		_certChain ) 
	{
	    alias 			= _alias;
	    privateKey 		= _privateKey;
	    certChain 		= _certChain;
	}
	

	protected Manifest 
	getManifestFile( 
		JarFile jarFile )
	
		throws IOException 
	{
		Manifest manifest = new Manifest();
		
		JarEntry entry = jarFile.getJarEntry( MANIFEST_NAME );
		
		if ( entry != null ){
			
			manifest.read( jarFile.getInputStream( entry ) );
		}
		
		return manifest;
	}


	protected Map 
	removeMissingEntriesFromManifest( 
		Manifest 		manifest, 
		JarFile 		jarFile )
	
		throws IOException 
	{
		Map map = manifest.getEntries();
		
		Iterator elements = map.keySet().iterator();
		
		while( elements.hasNext() ){
			
			String element = (String)elements.next();
			
			if ( jarFile.getEntry( element ) == null ){
				
				elements.remove();
			}
		}
	
		return( map );
	}

	private Map 
	createEntries( 
		Manifest	manifest, 
		JarFile 	jarFile )
	
		throws IOException 
	{
		Map entries = null;
		
		if( manifest.getEntries().size() > 0 ){
			
				// already existing manifest, just remove any extra entries
			
			entries = removeMissingEntriesFromManifest( manifest, jarFile );
			
		}else{

				// new manifest, stick in default values
			
			Attributes attributes = manifest.getMainAttributes();
			
			attributes.putValue( Attributes.Name.MANIFEST_VERSION.toString(), "1.0" );
			
			attributes.putValue( "Created-By", System.getProperty( "java.version" ) + " (" + System.getProperty( "java.vendor" ) + ")" );
			
			entries = manifest.getEntries();
		}
	
		return entries;
	}
	
	protected String 
	updateDigest( 
		MessageDigest 		digest, 
		InputStream 		inputStream )
	
		throws IOException 
	{
		byte[] buffer = new byte[8192];
	  	  
		while( true ){
	  	
			int len	= inputStream.read( buffer);
	  	
			if ( len <= 0 ){
	  		
				break;
			}
	  
			digest.update( buffer, 0, len );
		}
	  
		inputStream.close();

		return( new BASE64Encoder().encode( digest.digest()));
	}


	protected Map 
	updateManifestEntries( 
		Manifest 		manifest, 
		JarFile 		jar_file, 
		MessageDigest 	message_digest, 
		Map 			entries )
	
		throws IOException 
	{
		Enumeration jar_entries = jar_file.entries();
		
		while ( jar_entries.hasMoreElements()){
			
			JarEntry entry = (JarEntry)jar_entries.nextElement();
			
			if ( entry.getName().startsWith( "META-INF" ) ){
			
				continue;
				
			}else if ( manifest.getAttributes( entry.getName() ) != null ){
				
				Attributes attributes = manifest.getAttributes( entry.getName() );
				
				attributes.putValue( 
						"SHA1-Digest", 
						updateDigest( message_digest, jar_file.getInputStream( entry ) ));

			}else if ( !entry.isDirectory()){

				Attributes attributes = new Attributes();
				
				attributes.putValue( 
						"SHA1-Digest", 
						updateDigest( message_digest, jar_file.getInputStream( entry ) ));
				
				entries.put( entry.getName(), attributes );
			}
		}
		
		return( entries );
	}
	
	protected byte[] 
	serialiseManifest( 
		Manifest manifest )
	
		throws IOException 
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		manifest.write( baos );
		
		baos.flush();
		
		baos.close();
		
		return( baos.toByteArray());
	}

	protected SignatureFile 
	createSignatureFile( 
		Manifest 		manifest, 
		MessageDigest 	messageDigest )
	
		throws IOException 
	{
		ManifestDigester manifestDigester = new ManifestDigester( serialiseManifest( manifest ) );
		
		return( new SignatureFile( 
					new MessageDigest[]{ messageDigest }, 
					manifest, 
					manifestDigester, 
					alias, true ));

	}

	protected void 
	writeJarEntry( 
		JarEntry 		entry, 
		JarFile 		jar_file, 
		JarOutputStream jos )
	
		throws IOException 
	{
		jos.putNextEntry( new JarEntry( entry.getName()));
		
		byte[] buffer = new byte[8192];
				
		InputStream is = jar_file.getInputStream( entry );
		
		while( true ){
			
			int	len = is.read( buffer );
		
			if ( len <= 0 ){
				
				break;
			}
			
			jos.write( buffer, 0, len );
		}
		
		jos.closeEntry();
	}

	public void 
	signJarFile( 
		JarFile 			jar_file, 
		OutputStream 		output_stream )
	
		throws 	NoSuchAlgorithmException, InvalidKeyException, CertificateException, 
				SignatureException, IOException 
	{
		Manifest manifest = getManifestFile( jar_file );
		
		Map entries = createEntries( manifest, jar_file );

		MessageDigest messageDigest = MessageDigest.getInstance( "SHA1" );
		
		updateManifestEntries( manifest, jar_file, messageDigest, entries );

		SignatureFile signatureFile = createSignatureFile( manifest, messageDigest );
		
		SignatureFile.Block block = signatureFile.generateBlock( privateKey, certChain, true );

		String manifestFileName = MANIFEST_NAME;
		
		JarOutputStream jos = 
			output_stream instanceof JarOutputStream?(JarOutputStream)output_stream:new JarOutputStream( output_stream );
		
		JarEntry manifestFile = new JarEntry( manifestFileName );
		
		jos.putNextEntry( manifestFile );
		
		byte[]	manifest_bytes = serialiseManifest( manifest );
		
		jos.write( manifest_bytes  );
		
		jos.closeEntry();

		String signatureFileName = signatureFile.getMetaName();
		
		JarEntry signatureFileEntry = new JarEntry( signatureFileName );
		
		jos.putNextEntry( signatureFileEntry );
		
		signatureFile.write( jos );
		
		jos.closeEntry();
		
		String signatureBlockName = block.getMetaName();
		
		JarEntry signatureBlockEntry = new JarEntry( signatureBlockName );
		
		jos.putNextEntry( signatureBlockEntry );
		
		block.write( jos );
		
		jos.closeEntry();

		Enumeration metaEntries = jar_file.entries();
		
		while( metaEntries.hasMoreElements() ){
			
			JarEntry metaEntry = (JarEntry)metaEntries.nextElement();
			
			if ( 	metaEntry.getName().startsWith( "META-INF" ) &&
					!( 	manifestFileName.equalsIgnoreCase( metaEntry.getName()) ||
						signatureFileName.equalsIgnoreCase( metaEntry.getName()) ||
						signatureBlockName.equalsIgnoreCase( metaEntry.getName()))){
				
				writeJarEntry( metaEntry, jar_file, jos );
			}
		}

		Enumeration allEntries = jar_file.entries();
		
		while( allEntries.hasMoreElements() ){
			
			JarEntry entry = (JarEntry)allEntries.nextElement();
			
			if( !entry.getName().startsWith( "META-INF" )){
				
				writeJarEntry( entry, jar_file, jos );
			}
		}

		jos.flush();
		
		jos.finish();

		jar_file.close();
	}
	
	public void
	signJarStream(
		InputStream		is,
		OutputStream	os )
		
		throws 	NoSuchAlgorithmException, InvalidKeyException, CertificateException, 
				SignatureException, IOException
	{
		File	temp_file = File.createTempFile("AZU", null );
		
		FileOutputStream	fos = null;
		
		try{
			
			byte[]	buffer = new byte[8192];
		
			fos = new FileOutputStream( temp_file );
			
			while(true){
				
				int	len = is.read( buffer );
				
				if ( len <= 0 ){
					
					break;
				}
				
				fos.write( buffer, 0, len );
			}
			
			fos.close();
			
			fos	= null;
			
			signJarFile( new JarFile(temp_file), os );
			
		}finally{
			
			try{
				is.close();
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
			
			if ( fos != null ){
				
				try{
					fos.close();
					
				}catch( Throwable e ){
					
					Debug.printStackTrace( e );
				}
			}
			temp_file.delete();
			
		}
	}
}
*/
