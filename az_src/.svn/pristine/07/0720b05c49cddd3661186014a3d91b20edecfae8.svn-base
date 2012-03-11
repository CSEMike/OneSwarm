/*
 * Created on Jan 5, 2011
 * Created by Paul Gardner
 * 
 * Copyright 2011 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package org.gudy.azureus2.core3.xml.util;

import java.io.PrintWriter;

public class 
XMLEscapeWriter
	extends PrintWriter
{
	private boolean	enabled = true;
	
	public
	XMLEscapeWriter(
		PrintWriter	pw )
	{
		super( pw );
	}
	
	public void
	print(
		String	str )
	{
		if ( enabled ){
		
			super.print( XUXmlWriter.escapeXML( str ));
			
		}else{
			
			super.print( str );
		}
	}
	
	public void
	setEnabled(
		boolean		b )
	{
		enabled	= b;
	}
}
