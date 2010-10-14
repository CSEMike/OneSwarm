/*
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
 */

package org.gudy.azureus2.plugins.ui.SWT;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.graphics.Image;

import org.gudy.azureus2.plugins.PluginView;

/** Evil SWT Specific stuff that plugins may need access to
 *
 * @author TuxPaper
 * @deprecated use {@link org.gudy.azureus2.ui.swt.plugins.UISWTInstance}
 */
public interface SWTManager
{
  /** Retrieve the SWT Display object that Azureus uses (when in SWT mode).
   * If you have a thread that does some periodic/asynchronous stuff, Azureus 
   * will crashes with and 'InvalidThreadAccess' exception unless you
   * embed your calls in a Runnable, and use getDisplay().aSyncExec(Runnable r);
   *
   * @return SWT Display object that Azureus uses
   *
   * @since 2.1.0.0
   */
  public Display getDisplay();

  /** Creates an UIImageSWT object with the supplied SWT Image
   *
   * @param img Image to assign to the object
   * @return a new UIImagetSWT object
   *
   * @since 2.1.0.0
   */
  public GraphicSWT createGraphic(Image img);

  /**
   * A Plugin might call this method to add a View to Azureus's views
   * The View will be accessible from View > Plugins > View name
   * @param view The PluginView to be added
   *
   * @since 2.1.0.2
   */
  public void addView(PluginView view);

  /**
   * A Plugin might call this method to add a View to Azureus's views
   * The View will be accessible from View > Plugins > View name
   * @param view The PluginView to be added
   * @param autoOpen Whether the plugin should auto-open at startup
   *
   * @since 2.1.0.2
   */
  public void addView(PluginView view, boolean autoOpen);
  
  /**
   * A Plugin might call this method to load an image from
   * a resource (eg: "org/my_name/my_plugin/images/te_image.gif"
   * @see getImage(String name)
   * @param resource the resource path to the image
   * @param name the name used for the image, please use names starting with your plugin name.
   * @return true is the image was correctly loaded
   * @since 2.1.0.6
   */
  //public boolean loadImage(String resource,String name);
  
  /**
   * Once an image is loaded (@see loadImage(String resource,String name) )
   * a plugin can retrieve it by calling this method.
   * 
   * @param name the name used in loadImage to identify the image.
   * @return the image
   *
   */
  //public Image getImage(String name);
  
}
