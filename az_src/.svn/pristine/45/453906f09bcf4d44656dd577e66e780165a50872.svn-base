package org.gudy.azureus2.plugins.ui.toolbar;

public interface UIToolBarItem
{
	public final static long STATE_ENABLED = 0x1;

	public static final long STATE_DOWN = 0x2;

	/**
	 * Retrieve the ID of the toolbar item
	 * 
	 * @since 4.6.0.5
	 */
	public String getID();

	/**
	 * Return the message bundle ID for the button text
	 * 
	 * @since 4.6.0.5
	 */
	public String getTextID();

	/**
	 * Sets the button's text to a messagebundle value looked up using the id
	 * 
	 * @param id
	 * @since 4.6.0.5
	 */
	public void setTextID(String id);

	/**
	 * Get the ID of the image used
	 *
	 * @since 4.6.0.5
	 */
	public String getImageID();

	/**
	 * Sets the toolbar item to use the specified image
	 *
	 * @since 4.6.0.5
	 */
	public void setImageID(String id);

	/**
	 * Returns if the toolbar item is always available (enabled)
	 *
	 * @since 4.6.0.5
	 */
	public boolean isAlwaysAvailable();

	public long getState();
	
	public void setState(long state);

	public boolean triggerToolBarItem(long activationType, Object datasource);

	public void setDefaultActivationListener(
			UIToolBarActivationListener defaultActivation);
}
