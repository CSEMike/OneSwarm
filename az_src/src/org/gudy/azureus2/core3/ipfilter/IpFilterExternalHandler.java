package org.gudy.azureus2.core3.ipfilter;

import java.net.InetAddress;

public interface 
IpFilterExternalHandler 
{
	public boolean
	isBlocked(
		byte[]			torrent_hash,
		String			ip );
	
	public boolean
	isBlocked(
		byte[]			torrent_hash,
		InetAddress		ip );
}
