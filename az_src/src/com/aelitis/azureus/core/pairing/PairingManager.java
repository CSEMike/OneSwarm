/*
 * Created on Oct 5, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.pairing;

import java.util.List;

public interface 
PairingManager 
{
	public boolean
	isEnabled();
	
	public void
	setGroup(
		String		group );
	
	public String
	getGroup();
	
	public List<PairedNode>
	listGroup()
	
		throws PairingException;
	
	public String
	getAccessCode()
	
		throws PairingException;
	
	public String
	peekAccessCode();
	
	public String
	getReplacementAccessCode()
	
		throws PairingException;
	
	public PairedService
	addService(
		String		sid );
	
	public PairedService
	getService(
		String		sid );
	
	public void 
	setEnabled(
		boolean enabled );

	public String
	getStatus();

	public String
	getLastServerError();

	public boolean
	hasActionOutstanding();
	
	public PairingTest
	testService(
		String					sid,
		PairingTestListener		listener )
	
		throws PairingException;
	
	public void
	addListener(
		PairingManagerListener	l );
	
	public void
	removeListener(
		PairingManagerListener	l );

}
