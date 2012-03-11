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

import com.aelitis.azureus.core.util.GeneralUtils;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import java.util.ArrayList;

/**
 * @author TuxPaper
 */
public class LogAlert implements org.gudy.azureus2.plugins.logging.LogAlert {
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

		// -1 -> default
	public int	timeoutSecs	= -1;
	
	public String details;
	
	public boolean forceNotify;
	
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

	/**
	 * @param type
	 * @param text
	 * @param repeatable
	 * @param timeoutSecs  -1 -> use defaults 0 -> no timeout
	 */
	public LogAlert(boolean repeatable, int type, String text, int timeoutSecs) {
		entryType = type;
		this.text = text;
		this.repeatable = repeatable;
		this.timeoutSecs = timeoutSecs;
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
	
	public LogAlert(boolean repeatable, int type, String text, Throwable err) {
		this(repeatable, type, text);
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
	
	// Plugin methods.
	public int getGivenTimeoutSecs() {return timeoutSecs;}
	public String getText() {return text;}
	public Throwable getError() {return err;}
	public int getType() {
		switch (entryType) {
			case AT_INFORMATION:
				return LT_INFORMATION;
			case AT_ERROR:
				return LT_ERROR;
			case AT_WARNING:
				return LT_WARNING;
			default:
				return LT_INFORMATION;
		}
	}
	
	public Object[] getContext() {
		if (this.relatedTo == null) {return null;}
		ArrayList l = new ArrayList();
		for (int i=0; i<this.relatedTo.length; i++) {
			l.add(PluginCoreUtils.convert(this.relatedTo[i], false));
		}
		return l.toArray();
	}
	
	public int getTimeoutSecs() {
		if (this.timeoutSecs != -1) {return this.timeoutSecs;}
		return COConfigurationManager.getIntParameter("Message Popup Autoclose in Seconds");
	}
	
	public String getPlainText() {
		return GeneralUtils.stripOutHyperlinks(text);
	}
	
}
