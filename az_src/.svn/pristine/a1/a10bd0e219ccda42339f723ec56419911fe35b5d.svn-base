/*
 * Created on 11-Nov-2006
 * Created by Allan Crooks
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.plugins.ui;

/**
 * Interface class which provides a way to validate or reject input provided
 * by a user. This is mainly to be used with {@link UIInputReceiver}.
 * 
 * <p><b>Note:</b> This interface is intended to be implemented by plugins.</p>
 */
public interface UIInputValidator {

	/**
	 * Validates a input string from the user. This function has to return
	 * a localised string message indicating why the given string is not
	 * valid. If the string is valid, the function should return <tt>null</tt>.
	 * 
	 * <p>
	 * 
	 * Note - the returned string in the case of invalid input <b>must</b> be
	 * localised - no translating of the string returned will take place.
	 */
	public String validate(String input);
	
}
