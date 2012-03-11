package org.gudy.azureus2.plugins.ui.toolbar;

public interface UIToolBarManager
{
	public final static String GROUP_BIG = "big";
	public final static String GROUP_MAIN = "main";

	/**
	 * Create a new {@link UIToolBarItem}.  You will still need to add it
	 * via {@link #addToolBarItem(UIToolBarItem)}, after setting the item's
	 * properties
	 * 
	 * @param id unique id
	 * @return newly created toolbar
	 */
	public UIToolBarItem createToolBarItem(String id);
	
	/**
	 * Adds a {@link UIToolBarItem} to the UI.  Make sure you at least set the
	 * icon before adding
	 * 
	 * @param item
	 */
	public void addToolBarItem(UIToolBarItem item);

	public UIToolBarItem getToolBarItem(String id);
	public UIToolBarItem[] getAllToolBarItems();
	public void removeToolBarItem(String id);
}
