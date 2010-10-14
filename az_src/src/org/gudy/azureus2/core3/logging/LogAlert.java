/*
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
 * 
 */
package org.gudy.azureus2.core3.logging;


/**
 * @author TuxPaper
 */
public class LogAlert {
	// log types
	public static final int AT_INFORMATION = LogEvent.LT_INFORMATION;

	public static final int AT_WARNING = LogEvent.LT_WARNING;

	public static final int AT_ERROR = LogEvent.LT_ERROR;

	public static final boolean REPEATABLE = true;

	public static final boolean UNREPEATABLE = false;

	public int entryType;

	public Throwable err = null;

	public boolean repeatable;

	public String text;

	/** A list of events that this entry is related to */
	public Object[] relatedTo;

	/**
	 * @param type
	 * @param text
	 * @param repeatable
	 */
	public LogAlert(boolean repeatable, int type, String text) {
		entryType = type;
		this.text = text;
		this.repeatable = repeatable;
	}

	public LogAlert(Object[] relatedTo, boolean repeatable, int type, String text) {
		this(repeatable, type, text);
		this.relatedTo = relatedTo;
	}

	public LogAlert(Object relatedTo, boolean repeatable, int type, String text) {
		this(repeatable, type, text);
		this.relatedTo = new Object[] { relatedTo };
	}

	public LogAlert(boolean repeatable, String text, Throwable err) {
		this(repeatable, AT_ERROR, text);
		this.err = err;
	}

	/**
	 * @param downloadManagerImpl
	 * @param b
	 * @param string
	 * @param e
	 */
	public LogAlert(Object relatedTo, boolean repeatable,
			String text, Throwable err) {
		this(repeatable, text, err);
		this.relatedTo = new Object[] { relatedTo };
	}
}
