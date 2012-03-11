/*
 * Created on 15 Jun 2006
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.security;

public interface 
CryptoManager 
{
		// don't even THINK about changing this!!!!

	public static final String	CRYPTO_CONFIG_PREFIX = "core.crypto.";
	
	public static final int HANDLER_ECC	= 1;
	
	public static final int[] HANDLERS = { HANDLER_ECC };
	
	public byte[]
	getSecureID();
	
	public CryptoHandler
	getECCHandler();
	
	public byte[]
	obfuscate(
		byte[]		data );
	
	public byte[]
	deobfuscate(
		byte[]		data );
	
	public void
	clearPasswords();
	
	public void
	clearPasswords(
		int	password_handler_type );

	public void
	addPasswordHandler(
		CryptoManagerPasswordHandler		handler );
	
	public void
	removePasswordHandler(
		CryptoManagerPasswordHandler		handler );
	
	public void
	addKeyListener(
		CryptoManagerKeyListener		listener );
	
	public void
	removeKeyListener(
		CryptoManagerKeyListener		listener );
}
