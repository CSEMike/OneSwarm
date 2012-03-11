/*
 * Created on Feb 5, 2008
 * Created by Olivier Chalouhi
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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

package com.aelitis.azureus.core.util.png;

import java.nio.ByteBuffer;

public class IHDRChunk extends CRCedChunk {
	
	private static final byte[] type = {(byte) 73, (byte)  72, (byte)  68, (byte)  82};
	
	private int width;
	private int height;
	
	public IHDRChunk(int width, int height) {
		super(type);
		this.width = width;
		this.height = height;
	}
	
	public byte[] getContentPayload() {
		ByteBuffer buffer = ByteBuffer.allocate(13);
		
		//width : 4 bytes
		buffer.putInt(width);
		
		//height : 4 bytes
		buffer.putInt(height);
		
		//color depth : 1 byte
		buffer.put((byte)8);
		
		//color type : 1 byte
		buffer.put((byte)0);
		
		//compression method : 1 byte
		buffer.put((byte)0);
		
		//filter method : 1 byte
		buffer.put((byte)0);
		
		//interlace method : 1 byte
		buffer.put((byte)0);
		
		return buffer.array();
		
	}

}
