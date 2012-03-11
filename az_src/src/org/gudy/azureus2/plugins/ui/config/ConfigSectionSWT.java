/*
 * File    : ConfigSection.java
 * Created : 23 jan. 2004
 * By      : Paper
 *
 * Azureus - a Java Bittorrent client
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
 */

package org.gudy.azureus2.plugins.ui.config;

import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;

/**
 * Extend this class to add a new configuration panel to the SWT config view.
 * 
 * @deprecated use {@link org.gudy.azureus2.ui.swt.plugins.UISWTInstance}
 */

public interface ConfigSectionSWT extends ConfigSection {
  /**
   * Create your own configuration panel here.  It can be anything that inherits
   * from SWT's Composite class.
   * Please be mindfull of small screen resolutions
   *
   * @param parent The parent of your configuration panel
   * @return your configuration panel
   */
  public Composite configSectionCreate(Composite parent);
}
