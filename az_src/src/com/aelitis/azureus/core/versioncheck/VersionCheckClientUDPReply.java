/*
 * Created on 1 Nov 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.versioncheck;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;



import com.aelitis.net.udp.uc.PRUDPPacketReply;

public class 
VersionCheckClientUDPReply 
	extends PRUDPPacketReply
{
	private Map	payload;
	
	public
	VersionCheckClientUDPReply(
		int			trans_id )
	{
		super( VersionCheckClientUDPCodecs.ACT_VERSION_REPLY, trans_id );
	}
	
	protected
	VersionCheckClientUDPReply(
		DataInputStream		is,
		int					trans_id )
	
		throws IOException
	{
		super( VersionCheckClientUDPCodecs.ACT_VERSION_REPLY, trans_id );
		
		short	len = is.readShort();
		
		if ( len <= 0 ){
			
			throw( new IOException( "invalid length" ));
		}
		
		byte[]	bytes = new byte[len];
		
		is.read( bytes );
		
		payload = BDecoder.decode( bytes );
	}
	
	public void
	serialise(
		DataOutputStream	os )
	
		throws IOException
	{
		super.serialise(os);
		
		byte[]	bytes = BEncoder.encode( payload );
		
		os.writeShort( (short)bytes.length );
		
		os.write( bytes );
	}
	
	public Map
	getPayload()
	{
		return( payload );
	}
	
	public void
	setPayload(
		Map		_payload )
	{
		payload	= _payload;
	}
	
	public String
	getString()
	{
		return( super.getString());
	}
}
