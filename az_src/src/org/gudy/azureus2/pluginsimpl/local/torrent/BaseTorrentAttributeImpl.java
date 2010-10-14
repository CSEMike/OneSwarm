/**
 * Created on 19-Jul-2006
 * Created by Allan Crooks
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.pluginsimpl.local.torrent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.torrent.TorrentAttributeEvent;
import org.gudy.azureus2.plugins.torrent.TorrentAttributeListener;

abstract class BaseTorrentAttributeImpl implements TorrentAttribute {

		private List listeners;
		
		protected BaseTorrentAttributeImpl() {
			listeners = new ArrayList();
		}
		
		public abstract String getName();
		public String[] getDefinedValues() {
			return new String[0];
		}
		
		public void addDefinedValue(String name) {
			throw new RuntimeException("not supported");
		}
		
		public void	removeDefinedValue(String name) {
			throw new RuntimeException("not supported");
		}
		
		public void	addTorrentAttributeListener(TorrentAttributeListener l) {
			this.listeners.add(l);
		}
		
		public void removeTorrentAttributeListener(TorrentAttributeListener	l) {
			this.listeners.remove(l);
		}
		
		protected List getTorrentAttributeListeners() {
			return this.listeners;
		}
		
		protected void notifyListeners(TorrentAttributeEvent ev) {
			Iterator itr = this.listeners.iterator();
			while (itr.hasNext()) {
				try {
					((TorrentAttributeListener)itr.next()).event(ev);
				}
				catch (Throwable t) { // Does it need to be Throwable?
					Debug.printStackTrace(t);
				}
			}
		}

}
 