/**
 * 
 */
package org.gudy.azureus2.ui.swt.shells;

/**
 * A simple class to declare a docking position and an offset; currently only used by <code>ShellDocker</code> 
 * @author khai
 *
 */
public class DockPosition
{
	public static final int TOP_LEFT = 1;

	public static final int BOTTOM_LEFT = 2;

	public static final int TOP_RIGHT = 3;

	public static final int BOTTOM_RIGHT = 4;

	private int position = TOP_LEFT;

	private Offset offset = new Offset(0, 0);

	public DockPosition() {
		this(TOP_LEFT, null);
	}

	public DockPosition(int position, Offset offset) {
		if (position == TOP_LEFT || position == TOP_RIGHT
				|| position == BOTTOM_LEFT || position == BOTTOM_RIGHT) {
			this.position = position;
		} else {
			this.position = TOP_LEFT;
		}
		if (null != offset) {
			this.offset = offset;
		}
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public Offset getOffset() {
		return offset;
	}

	public void setOffset(Offset offset) {
		if (null != offset) {
			this.offset = offset;
		}
	}
}