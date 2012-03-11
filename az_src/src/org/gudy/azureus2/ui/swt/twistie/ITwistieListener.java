package org.gudy.azureus2.ui.swt.twistie;

/**
 * A convenience listener that will be notified whenever the control changes state between being
 * collapse and being expanded
 * @author knguyen
 *
 */
public interface ITwistieListener
{
	/**
	 * <code>true</code> is the control is in a collapsed state; <code>false otherwise</code>
	 * @param value
	 */
	public void isCollapsed(boolean value);
}