/*
 * File    : SimpleXMLParserDocumentImpl.java
 * Created : 5 Oct. 2003
 * By      : Parg 
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
 
package org.gudy.azureus2.pluginsimpl.local.utils.xml.simpleparser;

import javax.xml.parsers.*;
import org.xml.sax.*;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocument;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentAttribute;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentException;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;
import org.w3c.dom.*;

import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;

public class 
SimpleXMLParserDocumentImpl
	implements SimpleXMLParserDocument
{
	private static DocumentBuilderFactory 		dbf_singleton;

	protected Document							document;
	protected SimpleXMLParserDocumentNodeImpl	root_node;
	
	
	public
	SimpleXMLParserDocumentImpl(
		File		file )
		
		throws SimpleXMLParserDocumentException
	{
		try{
			
			create( new FileInputStream( file ));
			
		}catch( Throwable e ){
			
			throw( new SimpleXMLParserDocumentException( e ));
		}
	}
	
	public
	SimpleXMLParserDocumentImpl(
		String		data )
		
		throws SimpleXMLParserDocumentException
	{
		try{
			create( new ByteArrayInputStream( data.getBytes( Constants.DEFAULT_ENCODING )));
			
		}catch( UnsupportedEncodingException e ){
			
		}
	}

	public
	SimpleXMLParserDocumentImpl(
		InputStream		input_stream )
		
		throws SimpleXMLParserDocumentException
	{
		create( input_stream );
	}
	
	protected static synchronized DocumentBuilderFactory
	getDBF()
	{
			// getting the factory involves a fait bit of work - cache it
		
		if ( dbf_singleton == null ){
		
			dbf_singleton = DocumentBuilderFactory.newInstance();

			// Set namespaceAware to true to get a DOM Level 2 tree with nodes
			// containing namesapce information.  This is necessary because the
			// default value from JAXP 1.0 was defined to be false.
						
			dbf_singleton.setNamespaceAware(true);
	
			// Set the validation mode to either: no validation, DTD
			// validation, or XSD validation
					
			dbf_singleton.setValidating( false );
					
			// Optional: set various configuration options
					
			dbf_singleton.setIgnoringComments(true);
			dbf_singleton.setIgnoringElementContentWhitespace(true);
			dbf_singleton.setCoalescing(true);
					
			// The opposite of creating entity ref nodes is expanding them inline
			// NOTE that usage of, e.g. "&amp;" in text results in an entity ref. e.g.
			//	if ("BUY".equals (type) "
			//		ENT_REF: nodeName="amp"
			//		TEXT: nodeName="#text" nodeValue="&"
			
			dbf_singleton.setExpandEntityReferences(true);
		}
		
		return( dbf_singleton );
	}
	
	protected void
	create(
		InputStream		input_stream )
		
		throws SimpleXMLParserDocumentException
	{
		try{
			DocumentBuilderFactory dbf = getDBF();

			// Step 2: create a DocumentBuilder that satisfies the constraints
			// specified by the DocumentBuilderFactory
					
			DocumentBuilder db = dbf.newDocumentBuilder();

			// Set an ErrorHandler before parsing
					
			OutputStreamWriter errorWriter = new OutputStreamWriter(System.err);
					
			MyErrorHandler error_handler = new MyErrorHandler(new PrintWriter(errorWriter, true));

			db.setErrorHandler( error_handler );

			db.setEntityResolver(
				new EntityResolver()
				{
					public InputSource 
					resolveEntity(
						String publicId, String systemId )
					{
						// System.out.println( publicId + ", " + systemId );
						
						// handle bad DTD external refs
						
						try{
							String host = new URL( systemId ).getHost();
							
							InetAddress.getByName( host );
							
							return( null );
							
						}catch( UnknownHostException e ){
							
							return new InputSource(	new ByteArrayInputStream("<?xml version='1.0' encoding='UTF-8'?>".getBytes()));
							
						}catch( Throwable e ){
							
							return( null );
						}
					}
				});
	
			// Step 3: parse the input file
					
			document = db.parse( input_stream );
						
			SimpleXMLParserDocumentNodeImpl[] root_nodes = parseNode( document, false );
	
			int	root_node_count	= 0;
			
				// remove any processing instructions such as <?xml-stylesheet
			
			for (int i=0;i<root_nodes.length;i++){
				
				SimpleXMLParserDocumentNodeImpl	node = root_nodes[i];
				
				if ( node.getNode().getNodeType() != Node.PROCESSING_INSTRUCTION_NODE ){
					
					root_node	= node;
					
					root_node_count++;
				}
			}
			
			if ( root_node_count != 1 ){
								
				throw( new SimpleXMLParserDocumentException( "invalid document - " + root_nodes.length + " root elements" ));
			}
						
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			throw( new SimpleXMLParserDocumentException( e ));
		}
	}
	
	public String
	getName()
	{
		return( root_node.getName());
	}
	
	public String
	getFullName()
	{
		return( root_node.getFullName());
	}
	
	public String
	getNameSpaceURI()
	{
		return( root_node.getNameSpaceURI());
	}
	
	public String
	getValue()
	{
		return( root_node.getValue());
	}
	
	public SimpleXMLParserDocumentNode[]
	getChildren()
	{
		return( root_node.getChildren());
	}
	public SimpleXMLParserDocumentNode
	getChild(
		String	name )
	{
		return( root_node.getChild(name));
	}
	
	public SimpleXMLParserDocumentAttribute[]
	getAttributes()
	{
		return( root_node.getAttributes());
	}
	public SimpleXMLParserDocumentAttribute
	getAttribute(
		String		name )
	{
		return( root_node.getAttribute(name));
	}

	public void
	print()
	{
		PrintWriter	pw = new PrintWriter( System.out );
		
		print( pw );
		
		pw.flush();
	}
	
	public void
	print(
		PrintWriter	pw )
	{
		root_node.print( pw, "" );
	}
	
		// idea is to flatten out any unwanted structure. We just want the resultant
		// tree to have nodes for each nesting element and leaves denoting name/value bits
	
	protected SimpleXMLParserDocumentNodeImpl[]
	parseNode(
		Node		node,
		boolean		skip_this_node )
	{
        int type = node.getNodeType();
		
		if ( (	type == Node.ELEMENT_NODE ||
				type == Node.PROCESSING_INSTRUCTION_NODE )&& !skip_this_node ){
			
			return( new SimpleXMLParserDocumentNodeImpl[]{ new SimpleXMLParserDocumentNodeImpl( this, node )});
		}

		Vector	v = new Vector();
		
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()){
			
			SimpleXMLParserDocumentNodeImpl[] kids = parseNode( child, false );
			
			for (int i=0;i<kids.length;i++){
				
				v.addElement(kids[i]);
			}
        }
		
		SimpleXMLParserDocumentNodeImpl[]	res = new SimpleXMLParserDocumentNodeImpl[v.size()];
		
		v.copyInto( res );
		
		return( res );
	}
	
    private static class MyErrorHandler implements ErrorHandler {
        /** Error handler output goes here */
        //private PrintWriter out;

        MyErrorHandler(PrintWriter out) {
            //this.out = out;
        }

        /**
         * Returns a string describing parse exception details
         */
        private String getParseExceptionInfo(SAXParseException spe) {
            String systemId = spe.getSystemId();
            if (systemId == null) {
                systemId = "null";
            }
            String info = "URI=" + systemId +
                " Line=" + spe.getLineNumber() +
                ": " + spe.getMessage();
            return info;
        }

        // The following methods are standard SAX ErrorHandler methods.
        // See SAX documentation for more info.

        public void 
		warning(
			SAXParseException spe ) 
			
			throws SAXException 
		{
            // out.println("Warning: " + getParseExceptionInfo(spe));
        }
        
        public void 
		error(
			SAXParseException spe )
			
			throws SAXException 
		{
            String message = "Error: " + getParseExceptionInfo(spe);
			
            throw new SAXException(message);
        }

        public void 
		fatalError(
			SAXParseException spe ) 
			
			throws SAXException 
		{
            String message = "Fatal Error: " + getParseExceptionInfo(spe);
			
            throw new SAXException(message,spe);
        }
    }
    
    public static void
    main(
    	String[]	args )
    {
    	
    	try{
			StringBuffer	data = new StringBuffer(1024);
			
			FileInputStream is = new FileInputStream( "C:\\temp\\upnp_trace3.log" );
			
			LineNumberReader	lnr = new LineNumberReader( new InputStreamReader( is, "UTF-8" ));
			
			while( true ){
				
				String	line = lnr.readLine();
				
				if ( line == null ){
					
					break;
				}
				
				for (int i=0;i<line.length();i++){
					char	c = line.charAt(i);
				
					if ( c < 0x20 ){
						data.append( ' ' );
					}else{
						data.append( c );
					}
				}
				
				data.append( "\n" );	
			}
				
			String	data_str = data.toString();
			
	  		new SimpleXMLParserDocumentImpl( data_str ).print();
	  		
	  		//new SimpleXMLParserDocumentImpl(new File( "C:\\temp\\upnp_trace3.log")).print();
	  		    		
    	}catch( Throwable e ){
    		
    		e.printStackTrace();
    	}
    }
}
