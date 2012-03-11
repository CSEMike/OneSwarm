/*
 * Created on 14-Dec-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.lws;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.disk.DiskManagerReadRequest;
import org.gudy.azureus2.core3.disk.DiskManagerReadRequestListener;
import org.gudy.azureus2.core3.logging.LogRelation;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManagerAdapter;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.peermanager.PeerManagerRegistration;
import com.aelitis.azureus.core.peermanager.peerdb.PeerItemFactory;


public class 
LWSPeerManagerAdapter
	extends 	LogRelation
	implements 	PEPeerManagerAdapter
{
	private LightWeightSeed			lws;
	
	private PeerManagerRegistration	peer_manager_registration;
	
	public
	LWSPeerManagerAdapter(
		LightWeightSeed				_lws,
		PeerManagerRegistration		_peer_manager_registration )
	{
		lws		= _lws;
		
		peer_manager_registration = _peer_manager_registration;
	}
	
	public String
	getDisplayName()
	{
		return( lws.getName());
	}
	
	public PeerManagerRegistration
	getPeerManagerRegistration()
	{
		return( peer_manager_registration );
	}
	
	public int
	getUploadRateLimitBytesPerSecond()
	{
		return( 0 );
	}
	
	public int
	getDownloadRateLimitBytesPerSecond()
	{
		return( 0 );
	}
	
	public int 
	getPermittedBytesToReceive()
	{
		return( Integer.MAX_VALUE );
	}
	
	public void 
	permittedReceiveBytesUsed( 
		int bytes )
	{
	}
	
	public int 
	getPermittedBytesToSend()
	{
		return( Integer.MAX_VALUE );
	}
	
	public void	
	permittedSendBytesUsed(	
		int bytes )
	{
	}
	
	public int
	getMaxUploads()
	{
		return( 4 );
	}
	
	public int
	getMaxConnections()
	{
		return( 0 );
	}
	
	public int
	getMaxSeedConnections()
	{
		return( 0 );
	}
	
	public boolean
	isExtendedMessagingEnabled()
	{
		return( true );
	}
	
	public boolean
	isPeerExchangeEnabled()
	{
		return( true );
	}
	
	public int 
	getCryptoLevel() 
	{
		return( NetworkManager.CRYPTO_OVERRIDE_NONE );
	}
	
	public long 
	getRandomSeed() 
	{
		return 0;
	}
	
	public boolean
	isPeriodicRescanEnabled()
	{
		return( false );
	}
	
	public void
	setStateFinishing()
	{
	}
	
	public void
	setStateSeeding(
		boolean	never_downloaded )
	{
	}
	
	public void
	restartDownload(
		boolean	recheck )
	{
		Debug.out( "restartDownload called for " + getDisplayName());
	}
	
	public TRTrackerScraperResponse
	getTrackerScrapeResponse()
	{
		return( null );
	}
	
	public String
	getTrackerClientExtensions()
	{
		return( null );
	}
	
	public void
	setTrackerRefreshDelayOverrides(
		int	percent )
	{
		
	}
	
	public int 
	getTorrentInfoDictSize() 
	{
		return( 0 );
	}
	
	public byte[]
	getTorrentInfoDict(
		PEPeer	peer )
	{
		return( null );
	}
	
	public boolean
	isNATHealthy()
	{
		return( true );
	}
	
	public void
	addPeer(
		PEPeer	peer )
	{
	}
	
	public void
	removePeer(
		PEPeer	peer )
	{
	}
	
	public void
	addPiece(
		PEPiece	piece )
	{	
	}
	
	public void
	removePiece(
		PEPiece	piece )
	{	
	}
	
	public void
	discarded(
		PEPeer		peer,
		int			bytes )
	{	
	}
	
	public void
	protocolBytesReceived(
		PEPeer		peer,
		int			bytes )
	{
	}
	
	public void
	dataBytesReceived(
		PEPeer		peer,
		int			bytes )
	{	
	}
	
	public void
	protocolBytesSent(
		PEPeer		peer,
		int			bytes )
	{
	}
	
	public void
	dataBytesSent(
		PEPeer		peer,
		int			bytes )
	{
	}
	
	public void 
	statsRequest(
		PEPeer 	originator, 
		Map 	request,
		Map		reply )
	{		
	}
	
	public void
	addHTTPSeed(
		String	address,
		int		port )
	{	
	}
	
	public byte[][] 
	getSecrets(
		int crypto_level )
	{
		return( lws.getSecrets());
	}
	
	public void 
	enqueueReadRequest( 
		PEPeer							peer,
		DiskManagerReadRequest 			request, 
		DiskManagerReadRequestListener 	listener )
	{
		lws.enqueueReadRequest( peer, request, listener );
	}
	
	public int getPosition() 
	{
		return( Integer.MAX_VALUE );
	}
	
	public boolean 
	isPeerSourceEnabled(
		String peer_source ) 
	{
		return( true );
	}
	

	public boolean 
	hasPriorityConnection() 
	{
		return( false );
	}
	
	public void 
	priorityConnectionChanged(
		boolean added )
	{
	}
	
	public LogRelation
	getLogRelation()
	{
		return( this );
	}
	
	public String
	getRelationText() 
	{
		return( lws.getRelationText());
	}

	public Object[] 
	getQueryableInterfaces() 
	{
		List	interfaces = new ArrayList();
		
		Object[]	intf = lws.getQueryableInterfaces();
		
		for (int i=0;i<intf.length;i++){
			
			if( intf[i] != null ){
				
				interfaces.add( intf[i] );
			}
		}
		
		interfaces.add( lws.getRelation());
		
		return( interfaces.toArray());
	}
}
