package com.aelitis.azureus.core.dht.router;

import com.aelitis.azureus.core.dht.router.DHTRouterContact;

/**
 * Observer interface to allow monitoring of contacts in the routing table.
 * 
 * @author Michael Parker
 */
public interface DHTRouterObserver {
	/**
	 * Observer method invoked when a contact is added to the routing table.
	 * 
	 * @param contact
	 * the added contact
	 */
	public void added(DHTRouterContact contact);
	
	/**
	 * Observer method invoked when a contact is removed from the routing table.
	 * 
	 * @param contact
	 * the removed contact
	 */
	public void removed(DHTRouterContact contact);
	
	/**
	 * Observer method invoked when a contact changes between a bucket entry and a
	 * replacement in the routing table.
	 * 
	 * @param contact
	 * the contact that changed location
	 */
	public void locationChanged(DHTRouterContact contact);
	
	/**
	 * Observer method invoked when a contact is found to be alive.
	 * 
	 * @param contact
	 * the contact now alive
	 */
	public void nowAlive(DHTRouterContact contact);
	
	/**
	 * Observer method invoked when a contact is found to be failing.
	 * 
	 * @param contact
	 * the contact now failing
	 */
	public void nowFailing(DHTRouterContact contact);
	
	/**
	 * Router is not longer in use
	 * @param router
	 */
	
	public void
	destroyed(
		DHTRouter	router );
}
