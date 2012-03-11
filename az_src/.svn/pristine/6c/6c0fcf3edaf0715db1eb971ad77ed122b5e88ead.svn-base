/*
 * Created on 9 juil. 2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.ui.swt.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.BufferedLabel;

/**
 * @author parg
 * 
 */
public class 
InfoParameter 
	extends Parameter
{
	private String 				name;
	private BufferedLabel	 	label;

	public InfoParameter(Composite composite,String name) {
		this(composite, name, COConfigurationManager.getStringParameter(name));
	}

	public InfoParameter(Composite composite,final String name, String defaultValue ) {
		super(name);
		this.name = name;
		this.label = new BufferedLabel(composite, SWT.NULL);
		String value = COConfigurationManager.getStringParameter(name, defaultValue);
		label.setText(value);
	}


	public void setValue(final String value) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (label == null || label.isDisposed()
						|| label.getText().equals(value)) {
					return;
				}
				label.setText(value);
			}
		});

		if (!COConfigurationManager.getStringParameter(name).equals(value)) {
			COConfigurationManager.setParameter(name, value);
		}
	}

	public void setLayoutData(Object layoutData) {
		label.setLayoutData(layoutData);
	}
	public String getValue() {
		return label.getText();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.IParameter#getControl()
	 */
	public Control getControl() {
		return label.getControl();
	}

	public void setValue(Object value) {
		if (value instanceof String) {
			setValue((String)value);
		}
	}

	public Object getValueObject() {
		return COConfigurationManager.getStringParameter(name);
	}
}
