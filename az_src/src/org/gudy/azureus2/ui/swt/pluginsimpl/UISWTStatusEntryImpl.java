/**
 * 
 */
package org.gudy.azureus2.ui.swt.pluginsimpl;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.pluginsimpl.local.ui.menus.MenuContextImpl;
import org.gudy.azureus2.ui.common.util.MenuItemManager;
import org.gudy.azureus2.ui.swt.MenuBuildUtils;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.MainStatusBar;
import org.gudy.azureus2.ui.swt.mainwindow.MainStatusBar.CLabelPadding;
import org.gudy.azureus2.ui.swt.plugins.UISWTStatusEntry;
import org.gudy.azureus2.ui.swt.plugins.UISWTStatusEntryListener;

import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

import org.gudy.azureus2.plugins.ui.menus.MenuContext;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;

/**
 * @author Allan Crooks
 *
 */
public class UISWTStatusEntryImpl implements UISWTStatusEntry, MainStatusBar.CLabelUpdater {
	
	private AEMonitor this_mon = new AEMonitor("UISWTStatusEntryImpl@" + Integer.toHexString(this.hashCode()));
	
	private UISWTStatusEntryListener listener = null;
	private MenuContextImpl menu_context = MenuContextImpl.create("status_entry");
	
	// Used by "update".
	private boolean needs_update = false;
	private boolean needs_layout = false;
	private String text = null;
	private String tooltip = null;
	private boolean image_enabled = false;
	private Image image = null;
	private boolean is_visible = false;
	private boolean needs_disposing = false;
	private boolean is_destroyed = false;
	
	private Menu menu;

	private CopyOnWriteArrayList<String> imageIDstoDispose = new CopyOnWriteArrayList<String>();
	private String imageID = null;
	
	private void checkDestroyed() {
		if (is_destroyed) {throw new RuntimeException("object is destroyed, cannot be reused");}
	}
	
	public MenuContext getMenuContext() {
		return this.menu_context;
	}
	
	public boolean update(CLabelPadding label) {
		if (needs_disposing && !label.isDisposed()) {
			if (menu != null && !menu.isDisposed()) {
				menu.dispose();
				menu = null;
			}
			label.dispose();

			if (imageID != null) {
				imageIDstoDispose.add(imageID);
			}
			releaseOldImages();
			
			return( true );
		}
		
		boolean do_layout = needs_layout;
		
		needs_layout = false;
		
		if (menu_context.is_dirty) {needs_update = true; menu_context.is_dirty = false;} 
		if (!needs_update) {return do_layout;}
		
		// This is where we do a big update.
		try {
			this_mon.enter();
			update0(label);
		}
		finally {
			this_mon.exit();
		}
		
		return do_layout;
	}
	
	/**
	 * 
	 *
	 * @since 4.0.0.5
	 */
	private void releaseOldImages() {
		if (imageIDstoDispose.size() > 0) {
			ImageLoader imageLoader = ImageLoader.getInstance();

			for (Iterator iter = imageIDstoDispose.iterator(); iter.hasNext();) {
				String id = (String) iter.next();
				imageLoader.releaseImage(id);
				iter.remove();
			}
		}
	}

	private void update0(final CLabelPadding label) {
		label.setText(text);
		label.setToolTipText(tooltip);
		label.setImage(image_enabled ? image : null);
		label.setVisible(this.is_visible);
		
		releaseOldImages();
		
		MenuItem[] items = MenuItemManager.getInstance().getAllAsArray(menu_context.context);
		if (items.length > 0 & menu == null) {
			menu = new Menu(label);
			label.setMenu(menu);
				
			MenuBuildUtils.addMaintenanceListenerForMenu(menu,
			    new MenuBuildUtils.MenuBuilder() {
					public void buildMenu(Menu menu, MenuEvent menuEvent) {
						MenuItem[] items = MenuItemManager.getInstance().getAllAsArray(menu_context.context);
						MenuBuildUtils.addPluginMenuItems(label, items, menu, true, true, 
							MenuBuildUtils.BASIC_MENU_ITEM_CONTROLLER);
					}
				}
			);
		}
		else if (menu != null && items.length == 0) {
			label.setMenu(null);
			if (!menu.isDisposed()) {menu.dispose();}
			this.menu = null;
		}
		
		this.needs_update = false;
	}
	
	void onClick() {
		UISWTStatusEntryListener listener0 = listener; // Avoid race conditions.
		if (listener0 != null) {listener.entryClicked(this);}
	}

	public void destroy() {
		try {
			this_mon.enter();
			this.is_visible = false;
			this.listener = null;
			this.image = null;
			this.needs_disposing = true;
			this.is_destroyed = true;
			
			// Remove any existing menu items.
			MenuItemManager.getInstance().removeAllMenuItems(this.menu_context.context);
		}
		finally {
			this_mon.exit();
		}
	}

	public void setImage(int image_id) {
		// we can't release the old image here because the label is still using it
		// Put it into a list until the label is updated with the new image, then
		// release the old
		if (imageID != null) {
			imageIDstoDispose.add(imageID);
		}

		switch (image_id) {
			case IMAGE_LED_GREEN:
				imageID = "greenled";
				break;
			case IMAGE_LED_RED:
				imageID = "redled";
				break;
			case IMAGE_LED_YELLOW:
				imageID = "yellowled";
				break;
			default:
				imageID = "grayled";
				break;
		}
		ImageLoader imageLoader = ImageLoader.getInstance();
		this.setImage(imageLoader.getImage(imageID));
	}

	public void setImage(Image image) {
		checkDestroyed();
		this_mon.enter();
		if( image != this.image ){
			needs_layout = true;
		}
		this.image = image;
		this.needs_update = true;
		this_mon.exit();
	}

	public void setImageEnabled(boolean enabled) {
		checkDestroyed();
		this_mon.enter();
		if ( enabled != image_enabled ){
			needs_layout = true;
		}
		this.image_enabled = enabled;
		this.needs_update = true;
		this_mon.exit();
	}

	public void setListener(UISWTStatusEntryListener listener) {
		checkDestroyed();
		this.listener = listener;
	}

	public void setText(String text) {
		checkDestroyed();
		this_mon.enter();
		this.text = text;
		this.needs_update = true;
		this_mon.exit();
	}

	public void setTooltipText(String text) {
		checkDestroyed();
		this_mon.enter();
		this.tooltip = text;
		this.needs_update = true;
		this_mon.exit();
	}

	public void setVisible(boolean visible) {
		checkDestroyed();
		this_mon.enter();
		this.is_visible = visible;
		this.needs_update = true;
		this_mon.exit();
	}

	public void created(final MainStatusBar.CLabelPadding label) {
		final Listener click_listener = new Listener() {
			public void handleEvent(Event e) {
				onClick();
			}
		};

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				label.addListener(SWT.MouseDoubleClick, click_listener);
			}
		}, true);
	}
}
