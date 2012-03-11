/*
 * Created on 13-Nov-2006
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
package org.gudy.azureus2.ui.swt.pluginsimpl;

import org.gudy.azureus2.pluginsimpl.local.ui.AbstractUIInputReceiver;
import org.gudy.azureus2.ui.swt.plugins.UISWTInputReceiver;

/**
 * Extended abstract class which implements the bulk of logic required for the
 * UISWTInputReceiver interface.
 */
public abstract class AbstractUISWTInputReceiver extends AbstractUIInputReceiver 
	implements UISWTInputReceiver {

	protected boolean select_preentered_text = true;
	public void selectPreenteredText(boolean select) {
		this.assertPrePrompt();
		this.select_preentered_text = select;
	}
	
	protected int line_height = -1;
	public void setLineHeight(int line_height) {
		this.assertPrePrompt();
		this.line_height = line_height;
	}

	protected int width_hint = -1;
	public void setWidthHint(int width) {
		this.assertPrePrompt();
		this.width_hint = width;
	}

	protected String[] choices = null;
	protected boolean choices_allow_edit = true;
	protected int choices_default = -1;
	
	public void setPreenteredText(String text, boolean as_suggested) {
		if (!this.choices_allow_edit) {
			throw new RuntimeException("cannot set pre-entered text if you have chosen to use non editable selected items");
		}
		super.setPreenteredText(text, as_suggested);
	}
	
	public void setSelectableItems(String[] choices, int default_choice, boolean allow_edit) {
		this.assertPrePrompt();
		if (!allow_edit && this.preentered_text != null) {
			throw new RuntimeException("cannot set allow_edit to false if you have already set pre-entered text");
		} 
		
		// Don't resort to combo mode if there's no choices - it seems to break things.
		if (choices.length == 0) {return;}
		
		this.choices = choices;
		this.choices_allow_edit = allow_edit;
		this.choices_default = default_choice;
		
    	if (default_choice >= 0 && default_choice < choices.length) {
    		this.preentered_text = choices[default_choice];
    	}
	}
	
}
