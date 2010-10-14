/*
 * Created on 16 Jan 2008
 * Created by Allan Crooks
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 */
package org.gudy.azureus2.pluginsimpl.local.dht.mainline;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.MessageManager;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTDHTPort;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTHandshake;

import org.gudy.azureus2.plugins.dht.mainline.MainlineDHTManager;
import org.gudy.azureus2.plugins.dht.mainline.MainlineDHTProvider;

import org.gudy.azureus2.core3.util.Debug;

/**
 * @author Allan Crooks
 *
 */
public class MainlineDHTManagerImpl implements MainlineDHTManager {
	
	private AzureusCore core;
	
	public MainlineDHTManagerImpl(AzureusCore core) {
		this.core = core;
	}

	public void setProvider(MainlineDHTProvider provider) {
		MainlineDHTProvider old_provider = core.getGlobalManager().getMainlineDHTProvider();
		core.getGlobalManager().setMainlineDHTProvider(provider);
		
		// Registering new provider, so enable global DHT support.
		if (old_provider == null && provider != null) {
			BTHandshake.setMainlineDHTEnabled(true);
			try {MessageManager.getSingleton().registerMessageType(new BTDHTPort(-1));}
			catch (MessageException me) {Debug.printStackTrace(me);}
		}
		
		// Deregistering existing provider, so disable global DHT support.
		else if (old_provider != null && provider == null) {
			BTHandshake.setMainlineDHTEnabled(false);
			MessageManager.getSingleton().deregisterMessageType(new BTDHTPort(-1));
		}
		
	}

	public MainlineDHTProvider getProvider() {
		return core.getGlobalManager().getMainlineDHTProvider();
	}

}
