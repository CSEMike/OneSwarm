/*
 * File    : UISWTViewEvent.java
 * Created : Oct 14, 2005
 * By      : TuxPaper
 *
 * Copyright (C) 2005, 2006 Aelitis SAS, All rights Reserved
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

package org.gudy.azureus2.ui.swt.plugins;

/**
 * A UI SWT View Event triggered by the UISWTViewEventListener
 * 
 * @see org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener
 * @see org.gudy.azureus2.ui.swt.plugins.UISWTInstance#addView(String, String, UISWTViewEventListener)
 * 
 * @author TuxPaper
 */
public interface UISWTViewEvent {
	/**
	 * Triggered before view is initialize in order to allow any set up before
	 * initialization
	 * <p>
	 * This is the only time that setting {@link UISWTView#setControlType(int)}
	 * has any effect.
	 * <p>
	 * return true from {@link UISWTViewEventListener#eventOccurred(UISWTViewEvent)}
	 * if creation was successfull.  If you want only one instance of your view,
	 * or if there's any reason you can't create, return false, and an existing
	 * view will be used, if one is present.
	 * 
	 * @since 2.3.0.6
	 */
	public static final int TYPE_CREATE = 0;

	/**
	 * Triggered when the datasource related to this view change.
	 * <p>
	 * Usually called after TYPE_CREATE, but before TYPE_INITIALIZE
	 * <p>
	 * getData() will return an Object[] array, or null
	 * 
	 * @since 2.3.0.6
	 */
	public static final int TYPE_DATASOURCE_CHANGED = 1;

	/**
	 * Initialize your view.
	 * <p>
	 * getData() will return a SWT Composite or AWT Container for you to place
	 * object in.
	 * 
	 * @since 2.3.0.6
	 */
	public static final int TYPE_INITIALIZE = 2;

	/** 
	 * Focus Gained
	 * 
	 * @since 2.3.0.6
	 */
	public static final int TYPE_FOCUSGAINED = 3;

	/**
	 * Focus Lost
	 * <p>
	 * TYPE_FOCUSLOST may not be called before TYPE_DESTROY
	 * 
	 * @since 2.3.0.6
	 */
	public static final int TYPE_FOCUSLOST = 4;

	/** Triggered on user-specified intervals.  Plugins should update any
	 * live information at this time.
	 * <p>
	 * Caller is the GUI thread
	 * 
	 * @since 2.3.0.6
	 */
	public static final int TYPE_REFRESH = 5;

	/** Language has changed.  Plugins should update their text to the new
	 * language.  To determine the new language, use Locale.getDefault()
	 * 
	 * @since 2.3.0.6
	 */
	public static final int TYPE_LANGUAGEUPDATE = 6;

	/**
	 * Triggered when the parent view is about to be destroyed
	 * <p>
	 * TYPE_FOCUSLOST may not be called before TYPE_DESTROY
	 * 
	 * @since 2.3.0.6
	 */
	public static final int TYPE_DESTROY = 7;

	/**
	 * Triggered when the parent view is about to be closed
	 * <p>
	 * Return false to abort close
	 * 
	 * @since 2.5.0.1
	 */
	public static final int TYPE_CLOSE = 8;

	/**
	 * Get the type.
	 * 
	 * @return The TYPE_* constant for this event
	 * 
	 * @since 2.3.0.6
	 */
	public int getType();

	/**
	 * Get the data
	 * 
	 * @return Any data for this event
	 * 
	 * @since 2.3.0.6
	 */
	public Object getData();

	/**
	 * Get the View
	 * 
	 * @return Information and control over the view
	 * 
	 * @since 2.3.0.6
	 */
	public UISWTView getView();
}
