package org.gudy.azureus2.ui.swt.shells;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

/**
 * A utility class that helps in docking a given <code>Shell</code> to a given <code>Control</code>
 * @author khai
 *
 */
public class ShellDocker
{

	private DockPosition anchorControlPosition = new DockPosition();

	private boolean isDocked = true;

	private boolean moveWithShell = true;

	private boolean resizeWithShell = false;

	private Listener dockingEnabler = null;

	private Control anchorControl = null;

	private Shell dockedShell = null;

	private Shell mainShell = null;

	public ShellDocker(Control anchorControl, Shell dockedShell) {
		if (null == anchorControl || true == anchorControl.isDisposed()) {
			throw new NullPointerException("anchorControl cannot be null or disposed");
		}
		if (null == dockedShell || true == dockedShell.isDisposed()) {
			throw new NullPointerException("dockedShell cannot be null or disposed");
		}

		this.anchorControl = anchorControl;
		this.dockedShell = dockedShell;
		mainShell = anchorControl.getShell();
	}

	/**
	 * Opens the shell 
	 */
	public void openShell() {
		openShell(isDocked(), false);
	}

	/**
	 * Opens the shell as docked
	 * @param isDocked
	 */
	public void openShell(boolean isDocked) {
		openShell(isDocked, false);
	}

	/**
	 * Opens the shell as docked and animated
	 * @param isDocked
	 * @param isAnimated
	 */
	public void openShell(boolean isDocked, boolean isAnimated) {
		setDocked(isDocked);

		if (false == isDocked) {
			/*
			 * Centers the window by default
			 */

			UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
			if (null == uiFunctions) {
				/*
				 * Centers on the active monitor
				 */
				Utils.centreWindow(dockedShell);
			} else {
				/*
				 * Centers on the main application window
				 */
				Utils.centerWindowRelativeTo(dockedShell, uiFunctions.getMainShell());
			}
		}

		if (false == isAnimated) {
			dockedShell.open();
		} else {
			//TODO: implement any fancy animation FX here; this is currently just opening the shell normally
			dockedShell.open();
		}

	}

	public boolean isDocked() {
		return isDocked;
	}

	public void setDocked(boolean isDocked) {
		this.isDocked = isDocked;

		if (true == isDocked) {

			performDocking();

			if (null == dockingEnabler) {
				dockingEnabler = new Listener() {

					public void handleEvent(Event event) {
						if (event.type == SWT.Resize) {

							if (true == isResizeWithShell()) {
								System.out.println("resizing");//KN: sysout
							} else {
								performDocking();
							}
						} else if (event.type == SWT.Move) {
							performDocking();
						}

					}
				};
			}

			if (null != mainShell && false == mainShell.isDisposed()) {
				if (true == isMoveWithShell()) {
					mainShell.addListener(SWT.Move, dockingEnabler);
				}
				if (true == isResizeWithShell()) {
					mainShell.addListener(SWT.Resize, dockingEnabler);
				}
				anchorControl.addListener(SWT.Move, dockingEnabler);
				anchorControl.addListener(SWT.Resize, dockingEnabler);
			}

			anchorControl.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					setDocked(false);
				}
			});

			dockedShell.addListener(SWT.Close, new Listener() {
				public void handleEvent(Event e) {
					setDocked(false);

				}
			});

		} else {
			if (null != mainShell && false == mainShell.isDisposed()) {
				if (null != dockingEnabler) {
					mainShell.removeListener(SWT.Move, dockingEnabler);
					mainShell.removeListener(SWT.Resize, dockingEnabler);
				}
			}
			if (null != anchorControl && false == anchorControl.isDisposed()) {
				if (null != dockingEnabler) {
					anchorControl.removeListener(SWT.Move, dockingEnabler);
					anchorControl.removeListener(SWT.Resize, dockingEnabler);
				}
			}
		}

	}

	private void performDocking() {
		if (true == isAlive()) {
			switch (anchorControlPosition.getPosition()) {
				case DockPosition.TOP_LEFT:
					dockedShell.setLocation(mainShell.toDisplay(anchorControl.getLocation()));
					break;
				case DockPosition.TOP_RIGHT:
					break;

				case DockPosition.BOTTOM_LEFT: {
					Point p = mainShell.toDisplay(anchorControl.getLocation());
					p.x += anchorControlPosition.getOffset().xOffset;
					p.y += anchorControlPosition.getOffset().yOffset;
					p.y += anchorControl.getSize().y;
					dockedShell.setLocation(p);
				}
					break;

				case DockPosition.BOTTOM_RIGHT: {
					Point p = mainShell.toDisplay(anchorControl.getLocation());
					p.x += anchorControlPosition.getOffset().xOffset;
					p.y += anchorControlPosition.getOffset().yOffset;

					p.x += anchorControl.getSize().x;
					p.y += anchorControl.getSize().y;
					dockedShell.setLocation(p);
				}
					break;

				default:
					break;
			}

		}
	}

	private boolean isAlive() {
		if (null == mainShell || true == mainShell.isDisposed()) {
			System.err.println("\tmainshell is disposed?");//KN: sysout
			return false;
		}
		if (null == dockedShell || true == dockedShell.isDisposed()) {
			System.err.println("\tdockedShell is disposed?");//KN: sysout
			return false;
		}

		if (null == anchorControl || true == anchorControl.isDisposed()) {
			System.err.println("\tanchorControl is disposed?");//KN: sysout
			return false;
		}

		return true;
	}

	public boolean isMoveWithShell() {
		return moveWithShell;
	}

	public void setMoveWithShell(boolean moveWithShell) {
		this.moveWithShell = moveWithShell;
	}

	public boolean isResizeWithShell() {
		return resizeWithShell;
	}

	public void setResizeWithShell(boolean resizeWithShell) {
		this.resizeWithShell = resizeWithShell;
	}

	public DockPosition getAnchorControlPosition() {
		return anchorControlPosition;
	}

	public void setAnchorControlPosition(DockPosition anchorControlPosition) {
		this.anchorControlPosition = anchorControlPosition;
	}
}
