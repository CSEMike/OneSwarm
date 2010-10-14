package edu.uw.cse.netlab.reputation.storage;

import java.security.PublicKey;

public interface SoftStateListener
{
	public void refresh_complete( PublicKey inID );
}
