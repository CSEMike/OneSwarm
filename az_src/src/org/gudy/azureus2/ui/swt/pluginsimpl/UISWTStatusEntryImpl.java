/**
 * 
 */
package org.gudy.azureus2.ui.swt.pluginsimpl;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.custom.CLabel;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.mainwindow.MainStatusBar;
import org.gudy.azureus2.ui.swt.plugins.UISWTStatusEntry;
import org.gudy.azureus2.ui.swt.plugins.UISWTStatusEntryListener;

/**
 * @author Allan Crooks
 *
 */
public class UISWTStatusEntryImpl implements UISWTStatusEntry, MainStatusBar.CLabelUpdater {
	
	private AEMonitor this_mon = new AEMonitor("UISWTStatusEntryImpl@" + Integer.toHexString(this.hashCode()));
	
	private UISWTStatusEntryListener listener = null;
	
	// Used by "update".
	private boolean needs_update = false;
	private String text = null;
	private String tooltip = null;
	private boolean image_enabled = false;
	private Image image = null;
	private boolean is_visible = false;
	private boolean needs_disposing = false;
	private boolean is_destroyed = false;
	
	private void checkDestroyed() {
		if (is_destroyed) {throw new RuntimeException("object is destroyed, cannot be reused");}
	}
	
	public void update(CLabel label) {
		if (needs_disposing && !label.isDisposed()) {label.dispose(); return;}
		if (!needs_update) {return;}
		
		// This is where we do a big update.
		try {
			this_mon.enter();
			update0(label);
		}
		finally {
			this_mon.exit();
		}
	}
	
	private void update0(CLabel label) {
		label.setText(text);
		label.setToolTipText(tooltip);
		label.setImage(image_enabled ? image : null);
		label.setVisible(this.is_visible);
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
		}
		finally {
			this_mon.exit();
		}
	}

	public void setImage(int image_id) {
		String img_name;
		switch (image_id) {
			case IMAGE_LED_GREEN:
				img_name = "greenled";
				break;
			case IMAGE_LED_RED:
				img_name = "redled";
				break;
			case IMAGE_LED_YELLOW:
				img_name = "yellowled";
				break;
			default:
				img_name = "grayled";
				break;
		}
		this.setImage(ImageRepository.getImage(img_name));
	}

	public void setImage(Image image) {
		checkDestroyed();
		this_mon.enter();
		this.image = image;
		this.needs_update = true;
		this_mon.exit();
	}

	public void setImageEnabled(boolean enabled) {
		checkDestroyed();
		this_mon.enter();
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

}
