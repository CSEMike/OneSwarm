/*
 * Created on 29 juin 2003
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
package org.gudy.azureus2.ui.swt.views;

import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.ui.swt.IconBarEnabler;
import org.gudy.azureus2.ui.swt.mainwindow.Refreshable;

/**
 * @author Olivier
 */

// XXX This class is used by plugins.  Don't remove any functions from it!
public interface IView extends IconBarEnabler, Refreshable {
  /**
   * This method is called when the view is instanciated, it should initialize all GUI
   * components. Must NOT be blocking, or it'll freeze the whole GUI.
   * Caller is the GUI Thread.
   * 
   * @param composite the parent composite. Each view should create a child 
   *         composite, and then use this child composite to add all elements
   *         to.
   *         
   * @note It's possible that the view may be created, but never initialize'd.
   *        In these cases, delete will still be called.
   */
  public void initialize(Composite composite);
  
  /**
   * This method is called after initialize so that the Tab is set its control
   * Caller is the GUI Thread.
   * @return the Composite that should be set as the control for the Tab item
   */
  public Composite getComposite();
  
  /**
   * This method is caled when the view is destroyed.
   * Each color instanciated, images and such things should be disposed.
   * The caller is the GUI thread.
   *
   */
  public void delete();
  
  /**
   * Data 'could' store a key to a language file, in order to support multi-language titles
   * @return a String which is the key of this view title.
   */
  public String getData();
  
  /**
   * Called in order to set / update the short title of this view.  When the 
   * view is being displayed in a tab, the short title is used for the tab's
   * text
   * 
   * @return A short title for the view
   */
  public String getShortTitle();
  
  /**
   * Called in order to set / update the title of this View.  When the view
   * is being displayed in a tab, the full title is used for the tooltip.
   * 
   * @return the full title for the view
   */
  public String getFullTitle();
  
  /**
   * Called when the language needs updating
   *
   */
  public void updateLanguage();
  
  
  /**
   * Called when Azureus generates Diagnostics.
   * Write any diagnostic information you want to the writer. 
   * 
   * @param writer
   * @since 2.3.0.4
   */
  // XXX Introduced IndentWriter to plugins..
  public void
  generateDiagnostics(
		IndentWriter	writer );
  

  /**
   * Called when the selected dataSource has changed.
   * If this view is dependent upon a selected datasource, implement this 
   * function and update your view.
   * 
   * @param newDataSource null if no datasource is selected.  May be an array
   *                       of Object[] if multiple dataSources are selected
   * @since 2.3.0.7
   */
  public void dataSourceChanged(Object newDataSource);
}
