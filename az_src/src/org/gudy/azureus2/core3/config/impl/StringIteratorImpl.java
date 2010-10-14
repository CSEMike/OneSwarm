/*
 * Created on 24 sept. 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.core3.config.impl;

import java.util.Iterator;

import org.gudy.azureus2.core3.config.StringIterator;

/**
 * @author Olivier Chalouhi
 *
 */
public class StringIteratorImpl implements StringIterator {

	Iterator iterator;
	
	public StringIteratorImpl(Iterator _iterator) {
		iterator = _iterator;
	}
	
	public boolean hasNext() {
		return iterator.hasNext();
	}
	
	public String next() {
		return (String) iterator.next();
	}
	
	public void remove() {
		iterator.remove();		
	}
}
