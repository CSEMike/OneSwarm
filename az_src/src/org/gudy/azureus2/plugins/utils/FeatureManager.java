/*
 * Created on Feb 4, 2010
 * Created by Paul Gardner
 * 
 * Copyright 2010 Vuze, Inc.  All rights reserved.
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


package org.gudy.azureus2.plugins.utils;

import org.gudy.azureus2.plugins.PluginException;


public interface 
FeatureManager 
{
	public Licence[]
	getLicences();
	
	public Licence[]
	createLicences(
		String[]				feature_ids )
	
		throws PluginException;
	
	public Licence
	addLicence(
		String		licence_key )
	
		throws PluginException;
	
	public FeatureDetails[]
	getFeatureDetails(
		String					feature_id );
	
		// feature present and not expired
	
	public boolean
	isFeatureInstalled(
		String					feature_id );
	
	public void
	refreshLicences();
	
	public void
	registerFeatureEnabler(
		FeatureEnabler	enabler );
	
	public void
	unregisterFeatureEnabler(
		FeatureEnabler	enabler );
	
	public void
	addListener(
		FeatureManagerListener		listener );
	
	public void
	removeListener(
		FeatureManagerListener		listener );

	
	public interface
	Licence
	{
		public final int LS_PENDING_AUTHENTICATION	= 1;
		public final int LS_AUTHENTICATED			= 2;
		public final int LS_INVALID_KEY				= 3;
		public final int LS_CANCELLED				= 4;
		public final int LS_REVOKED					= 5;
		public final int LS_ACTIVATION_DENIED		= 6;
		
		public int
		getState();
		
		public String
		getKey();
		
		public String
		getShortID();
		
		public FeatureDetails[]
		getFeatures();
		
		public boolean
		isFullyInstalled();
		
		public void
		retryInstallation();
		
		public void
		addInstallationListener(
			LicenceInstallationListener	listener );
		
		public void
		removeInstallationListener(
			LicenceInstallationListener	listener );
		
		public void
		remove();
		
		public interface
		LicenceInstallationListener
		{
			public void
			start(
				String		licence_key );
			
			public void
			reportActivity(
				String		licence_key,
				String		install,
				String		activity );
			
			public void
			reportProgress(
				String		licence_key,
				String		install,
				int			percent );
			
			public void
			complete(
				String		licence_key );
			
			public void
			failed(
				String				licence_key,
				PluginException		error );
		}
	}
	
	public interface
	FeatureEnabler
	{
		public Licence[]
       	getLicences();
		
		public Licence[]
		createLicences(
			String[]				feature_ids )
		
			throws PluginException;
		
       	public Licence
       	addLicence(
       		String		licence_key );
       	
       	public void
       	refreshLicences();
       	
    	public void
    	addListener(
    		FeatureManagerListener		listener );
    	
    	public void
    	removeListener(
    		FeatureManagerListener		listener );
	}
	
	public interface
	FeatureDetails
	{
		public String	PR_PUBLIC_KEY				= "PublicKey";				// String
		public String	PR_VALID_UNTIL				= "ValidUntil";				// Long
		public String	PR_OFFLINE_VALID_UNTIL		= "OfflineValidUntil";		// Long
		public String	PR_IS_INSTALL_TIME			= "IsInstallTime";			// Long (0=false)
		public String	PR_IS_TRIAL					= "IsTrial";				// Long (0=false)
		public String	PR_TRIAL_USES_LIMIT			= "TrialUsesLimit";			// Long
		public String	PR_TRIAL_USES_FAIL_COUNT	= "TrialUsesFailCount";		// Long
		public String	PR_TRIAL_USES_REMAINING		= "TrialUsesRemaining";		// Long
		public String	PR_REQUIRED_PLUGINS			= "Plugins";				// String: comma separated plugin ids
		public String	PR_FINGERPRINT				= "Fingerprint";			// String
		public String	PR_RENEWAL_KEY				= "RenewalKey";				// String
		
		public Licence
		getLicence();
		
		public String
		getID();
		
			/**
			 * Returns true if offline expired or overall expired. 
			 * NOT to be used by verified plugins, they must do the check explicitly using the
			 * signed properties
			 * @return
			 */
		
		public boolean
		hasExpired();
		
		public byte[]
		getEncodedProperties();
		
		public byte[]
		getSignature();
		
		public Object
		getProperty(
			String		propery_name );
		
		public void
		setProperty(
			String		property_name,
			Object		property_value );
	}
	
	public interface
	FeatureManagerListener
	{
		public void
		licenceAdded(
			Licence	licence );
		
		public void
		licenceChanged(
			Licence	licence );
		
		public void
		licenceRemoved(
			Licence	licence );
	}
}
