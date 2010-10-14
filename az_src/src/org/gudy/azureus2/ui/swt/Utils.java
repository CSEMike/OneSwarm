/*
 * File    : Utils.java
 * Created : 25 sept. 2003 16:15:07
 * By      : Olivier 
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

package org.gudy.azureus2.ui.swt;

import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.utils.VerticalAligner;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

/**
 * @author Olivier
 * 
 */
public class Utils
{
	private static final String GOOD_STRING = "(/|,jI~`gy";

	public static final boolean isGTK = SWT.getPlatform().equals("gtk");

	/** Some platforms expand the last column to fit the remaining width of
	 * the table.
	 */
	public static final boolean LAST_TABLECOLUMN_EXPANDS = isGTK;

	/** GTK already handles alternating background for tables */
	public static final boolean TABLE_GRIDLINE_IS_ALTERNATING_COLOR = isGTK;

	private static final boolean DIRECT_SETCHECKED = !Constants.isOSX
			|| SWT.getVersion() >= 3212;

	public static final boolean SWT32_TABLEPAINT = false; //SWT.getVersion() >= 3200;

	/**
	 * Debug/Diagnose SWT exec calls.  Provides usefull information like how
	 * many we are queuing up, and how long each call takes.  Good to turn on
	 * occassionally to see if we coded something stupid.
	 */
	private static final boolean DEBUG_SWTEXEC = System.getProperty(
			"debug.swtexec", "0").equals("1");

	private static ArrayList queue;

	private static AEDiagnosticsLogger diag_logger;

	static {
		if (DEBUG_SWTEXEC) {
			queue = new ArrayList();
			diag_logger = AEDiagnostics.getLogger("swt");
			diag_logger.log("\n\nSWT Logging Starts");
		} else {
			queue = null;
			diag_logger = null;
		}
	}

	public static boolean isAZ2UI() {
		String ui_type = COConfigurationManager.getStringParameter("ui");

		return (ui_type.equals("az2"));
	}

	public static void disposeComposite(Composite composite, boolean disposeSelf) {
		if (composite == null || composite.isDisposed())
			return;
		Control[] controls = composite.getChildren();
		for (int i = 0; i < controls.length; i++) {
			Control control = controls[i];
			if (control != null && !control.isDisposed()) {
				if (control instanceof Composite) {
					disposeComposite((Composite) control, true);
				}
				try {
					control.dispose();
				} catch (SWTException e) {
					Debug.printStackTrace(e);
				}
			}
		}
		// It's possible that the composite was destroyed by the child
		if (!composite.isDisposed() && disposeSelf)
			try {
				composite.dispose();
			} catch (SWTException e) {
				Debug.printStackTrace(e);
			}
	}

	public static void disposeComposite(Composite composite) {
		disposeComposite(composite, true);
	}

	/**
	 * Dispose of a list of SWT objects
	 * 
	 * @param disposeList
	 */
	public static void disposeSWTObjects(List disposeList) {
		disposeSWTObjects(disposeList.toArray());
		disposeList.clear();
	}

	public static void disposeSWTObjects(Object[] disposeList) {
		boolean bResourceObjectExists = SWT.getVersion() >= 3129;

		for (int i = 0; i < disposeList.length; i++) {
			Object o = disposeList[i];
			if (o instanceof Widget && !((Widget) o).isDisposed())
				((Widget) o).dispose();
			else if (bResourceObjectExists && (o instanceof Resource)
					&& !((Resource) o).isDisposed())
				((Resource) o).dispose();
			else {
				try {
					// For Pre-SWT 3.1
					if ((o instanceof Cursor) && !((Cursor) o).isDisposed()) {
						((Cursor) o).dispose();
					} else if ((o instanceof Font) && !((Font) o).isDisposed()) {
						((Font) o).dispose();
					} else if ((o instanceof GC) && !((GC) o).isDisposed()) {
						((GC) o).dispose();
					} else if ((o instanceof Image) && !((Image) o).isDisposed()) {
						((Image) o).dispose();
					} else if ((o instanceof Region) && !((Region) o).isDisposed()) {
						((Region) o).dispose(); // 3.0
					} else if ((o instanceof TextLayout)
							&& !((TextLayout) o).isDisposed()) {
						((TextLayout) o).dispose(); // 3.0
					}
				} catch (NoClassDefFoundError e) {
					// ignore
				}
				// Path, Pattern, Transform are all 3.1, which will be instances of 
				// Resource
			}
		}
	}

	/**
	 * Initializes the URL dialog with http://
	 * If a valid link is found in the clipboard, it will be inserted
	 * and the size (and location) of the dialog is adjusted.
	 * @param shell to set the dialog location if needed
	 * @param url the URL text control
	 * @param accept_magnets 
	 *
	 * @author Rene Leonhardt
	 */
	public static void setTextLinkFromClipboard(final Shell shell,
			final Text url, boolean accept_magnets) {
		String link = getLinkFromClipboard(shell.getDisplay(), accept_magnets);
		if (link != null)
			url.setText(link);
	}

	/**
	 * <p>Gets an URL from the clipboard if a valid URL for downloading has been copied.</p>
	 * <p>The supported protocols currently are http, https, and magnet.</p>
	 * @param display
	 * @param accept_magnets 
	 * @return first valid link from clipboard, else "http://"
	 */
	public static String getLinkFromClipboard(Display display,
			boolean accept_magnets) {
		final Clipboard cb = new Clipboard(display);
		final TextTransfer transfer = TextTransfer.getInstance();

		String data = (String) cb.getContents(transfer);

		String text = UrlUtils.parseTextForURL(data, accept_magnets);
		if (text == null) {
			return "http://";
		}

		return text;
	}

	public static void centreWindow(Shell shell) {
		Rectangle displayArea; // area to center in
		try {
			displayArea = shell.getMonitor().getClientArea();
		} catch (NoSuchMethodError e) {
			displayArea = shell.getDisplay().getClientArea();
		}

		Rectangle shellRect = shell.getBounds();

		if (shellRect.height > displayArea.height) {
			shellRect.height = displayArea.height;
		}
		if (shellRect.width > displayArea.width - 50) {
			shellRect.width = displayArea.width;
		}

		shellRect.x = displayArea.x + (displayArea.width - shellRect.width) / 2;
		shellRect.y = displayArea.y + (displayArea.height - shellRect.height) / 2;

		shell.setBounds(shellRect);
	}

	/**
	 * Centers a window relative to a control. That is to say, the window will be located at the center of the control.
	 * @param window
	 * @param control
	 */
	public static void centerWindowRelativeTo(final Shell window,
			final Control control) {
		final Rectangle bounds = control.getBounds();
		final Point shellSize = window.getSize();
		window.setLocation(bounds.x + (bounds.width / 2) - shellSize.x / 2,
				bounds.y + (bounds.height / 2) - shellSize.y / 2);
	}

	public static void createTorrentDropTarget(Composite composite,
			boolean bAllowShareAdd) {
		try {
			createDropTarget(composite, bAllowShareAdd, null);
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	/**
	 * @param control the control (usually a Shell) to add the DropTarget
	 * @param url the Text control where to set the link text
	 *
	 * @author Rene Leonhardt
	 */
	public static void createURLDropTarget(Composite composite, Text url) {
		try {
			createDropTarget(composite, false, url);
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	private static void createDropTarget(Composite composite,
			final boolean bAllowShareAdd, final Text url,
			DropTargetListener dropTargetListener) {

		Transfer[] transferList;
		if (SWT.getVersion() >= 3107) {
			transferList = new Transfer[] {
				HTMLTransfer.getInstance(),
				URLTransfer.getInstance(),
				FileTransfer.getInstance(),
				TextTransfer.getInstance()
			};
		} else {
			transferList = new Transfer[] {
				URLTransfer.getInstance(),
				FileTransfer.getInstance(),
				TextTransfer.getInstance()
			};
		}

		final DropTarget dropTarget = new DropTarget(composite, DND.DROP_DEFAULT
				| DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK | DND.DROP_TARGET_MOVE);
		dropTarget.setTransfer(transferList);
		dropTarget.addDropListener(dropTargetListener);
		// Note: DropTarget will dipose when the parent it's on diposes

		// On Windows, dropping on children moves up to parent
		// On OSX, each child needs it's own drop.
		if (Constants.isWindows)
			return;

		Control[] children = composite.getChildren();
		for (int i = 0; i < children.length; i++) {
			Control control = children[i];
			if (!control.isDisposed()) {
				if (control instanceof Composite) {
					createDropTarget((Composite) control, bAllowShareAdd, url,
							dropTargetListener);
				} else {
					final DropTarget dropTarget2 = new DropTarget(control,
							DND.DROP_DEFAULT | DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK
									| DND.DROP_TARGET_MOVE);
					dropTarget2.setTransfer(transferList);
					dropTarget2.addDropListener(dropTargetListener);
				}
			}
		}
	}

	private static void createDropTarget(Composite composite,
			boolean bAllowShareAdd, Text url) {

		URLDropTarget target = new URLDropTarget(url, bAllowShareAdd);
		createDropTarget(composite, bAllowShareAdd, url, target);
	}

	private static class URLDropTarget
		extends DropTargetAdapter
	{
		private final Text url;

		private final boolean bAllowShareAdd;

		public URLDropTarget(Text url, boolean bAllowShareAdd) {
			this.url = url;
			this.bAllowShareAdd = bAllowShareAdd;
		}

		public void dropAccept(DropTargetEvent event) {
			event.currentDataType = URLTransfer.pickBestType(event.dataTypes,
					event.currentDataType);
		}

		public void dragOver(DropTargetEvent event) {
			// skip setting detail if user is forcing a drop type (ex. via the
			// ctrl key), providing that the operation is valid
			if (event.detail != DND.DROP_DEFAULT
					&& ((event.operations & event.detail) > 0))
				return;

			if ((event.operations & DND.DROP_LINK) > 0)
				event.detail = DND.DROP_LINK;
			else if ((event.operations & DND.DROP_DEFAULT) > 0)
				event.detail = DND.DROP_DEFAULT;
			else if ((event.operations & DND.DROP_COPY) > 0)
				event.detail = DND.DROP_COPY;
		}

		public void drop(DropTargetEvent event) {
			if (url == null || url.isDisposed()) {
				TorrentOpener.openDroppedTorrents(AzureusCoreImpl.getSingleton(),
						event, bAllowShareAdd);
			} else {
				if (event.data instanceof URLTransfer.URLType) {
					if (((URLTransfer.URLType) event.data).linkURL != null)
						url.setText(((URLTransfer.URLType) event.data).linkURL);
				} else if (event.data instanceof String) {
					String sURL = UrlUtils.parseTextForURL((String) event.data, true);
					if (sURL != null) {
						url.setText(sURL);
					}
				}
			}
		}
	}

	/**
	 * Force label to use more vertical space if wrapped and in a GridLayout
	 * Place this listener on the _parent_ of the label
	 * See Eclipse SWT Bug #9866 (GridLayout does not handle wrapped Label properly)
	 * This workaround only works for labels who:
	 *   - horizontally span their whole parent 
	 *     (ie. the parent has 3 columns, the label must span 3 columns)
	 *   - GridData style has GridData.FILL_HORIZONTAL
	 *   - Label style has SWT.WRAP
	 *
	 * @author TuxPaper
	 * @note Bug 9866 fixed in 3105 and later
	 */
	public static class LabelWrapControlListener
		extends ControlAdapter
	{
		public void controlResized(ControlEvent e) {
			if (SWT.getVersion() >= 3105)
				return;
			Composite parent = (Composite) e.widget;
			Control children[] = parent.getChildren();

			if (children.length > 0) {
				GridLayout parentLayout = (GridLayout) parent.getLayout();
				if (parentLayout != null) {
					Point size;
					int marginWidth = parentLayout.marginWidth;

					Composite grandParent = parent.getParent();
					if (grandParent instanceof ScrolledComposite) {
						Composite greatGP = grandParent.getParent();
						if (greatGP != null) {
							size = greatGP.getSize();

							if (greatGP.getLayout() instanceof GridLayout) {
								marginWidth += ((GridLayout) greatGP.getLayout()).marginWidth;
							}
						} else {
							// not tested
							size = grandParent.getSize();
						}

						if (grandParent.getLayout() instanceof GridLayout) {
							marginWidth += ((GridLayout) grandParent.getLayout()).marginWidth;
						}

						ScrollBar sb = grandParent.getVerticalBar();
						if (sb != null) {
							// I don't know why, but we have to remove one
							size.x -= sb.getSize().x + 1;
						}
					} else
						size = parent.getSize();

					boolean oneChanged = false;
					for (int i = 0; i < children.length; i++) {
						if ((children[i] instanceof Label)
								&& (children[i].getStyle() & SWT.WRAP) == SWT.WRAP) {
							GridData gd = (GridData) children[i].getLayoutData();
							if (gd != null && gd.horizontalAlignment == GridData.FILL) {
								if (gd.horizontalSpan == parentLayout.numColumns) {
									gd.widthHint = size.x - 2 * marginWidth;
									oneChanged = true;
								} else {
									Point pt = children[i].getLocation();
									gd.widthHint = size.x - pt.x - (2 * marginWidth);
									oneChanged = true;
								}
							}
						}
					}
					if (oneChanged) {
						parent.layout(true);
						if (grandParent instanceof ScrolledComposite) {
							((ScrolledComposite) grandParent).setMinSize(parent.computeSize(
									SWT.DEFAULT, SWT.DEFAULT, true));
						}
					}
				}
			} // size
		} // controlResized
	} // class

	public static void alternateRowBackground(TableItem item) {
		if (Utils.TABLE_GRIDLINE_IS_ALTERNATING_COLOR) {
			if (!item.getParent().getLinesVisible())
				item.getParent().setLinesVisible(true);
			return;
		}

		if (item == null || item.isDisposed())
			return;
		Color[] colors = {
			item.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND),
			Colors.colorAltRow
		};
		Color newColor = colors[item.getParent().indexOf(item) % colors.length];
		if (!item.getBackground().equals(newColor)) {
			item.setBackground(newColor);
		}
	}

	public static void alternateTableBackground(Table table) {
		if (table == null || table.isDisposed())
			return;

		if (Utils.TABLE_GRIDLINE_IS_ALTERNATING_COLOR) {
			if (!table.getLinesVisible())
				table.setLinesVisible(true);
			return;
		}

		int iTopIndex = table.getTopIndex();
		int iBottomIndex = getTableBottomIndex(table, iTopIndex);

		Color[] colors = {
			table.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND),
			Colors.colorAltRow
		};
		int iFixedIndex = iTopIndex;
		for (int i = iTopIndex; i <= iBottomIndex; i++) {
			TableItem row = table.getItem(i);
			// Rows can be disposed!
			if (!row.isDisposed()) {
				Color newColor = colors[iFixedIndex % colors.length];
				iFixedIndex++;
				if (!row.getBackground().equals(newColor)) {
					//        System.out.println("setting "+rows[i].getBackground() +" to " + newColor);
					row.setBackground(newColor);
				}
			}
		}
	}

	/**
	 * <p>
	 * Set a MenuItem's image with the given ImageRepository key. In compliance with platform
	 * human interface guidelines, the images are not set under Mac OS X.
	 * </p>
	 * @param item SWT MenuItem
	 * @param repoKey ImageRepository image key
	 * @see <a href="http://developer.apple.com/documentation/UserExperience/Conceptual/OSXHIGuidelines/XHIGMenus/chapter_7_section_3.html#//apple_ref/doc/uid/TP30000356/TPXREF116">Apple HIG</a>
	 */
	public static void setMenuItemImage(final MenuItem item, final String repoKey) {
		if (!Constants.isOSX)
			item.setImage(ImageRepository.getImage(repoKey));
	}

	public static void setMenuItemImage(final MenuItem item, final Image image) {
		if (!Constants.isOSX)
			item.setImage(image);
	}

	/**
	 * Sets the shell's Icon(s) to the default Azureus icon.  OSX doesn't require
	 * an icon, so they are skipped
	 * 
	 * @param shell
	 */
	public static void setShellIcon(Shell shell) {
		final String[] sImageNames = {
			"azureus",
			"azureus32",
			"azureus64",
			"azureus128"
		};

		if (Constants.isOSX)
			return;

		try {
			ArrayList list = new ArrayList();
			Image[] images = new Image[] {
				ImageRepository.getImage("azureus"),
				ImageRepository.getImage("azureus32"),
				ImageRepository.getImage("azureus64"),
				ImageRepository.getImage("azureus128")
			};

			for (int i = 0; i < images.length; i++) {
				Image image = ImageRepository.getImage(sImageNames[i]);
				if (image != null)
					list.add(image);
			}

			if (list.size() == 0)
				return;

			shell.setImages((Image[]) list.toArray(new Image[0]));
		} catch (NoSuchMethodError e) {
			// SWT < 3.0
			Image image = ImageRepository.getImage(sImageNames[0]);
			if (image != null)
				shell.setImage(image);
		}
	}

	private static Display getDisplay() {
		SWTThread swt = SWTThread.getInstance();

		Display display;
		if (swt == null) {
			display = Display.getDefault();
			if (display == null) {
				System.err.println("SWT Thread not started yet!");
				return null;
			}
		} else {
			if (swt.isTerminated()) {
				return null;
			}
			display = swt.getDisplay();
		}

		if (display == null || display.isDisposed()) {
			return null;
		}
		return display;
	}

	/**
	 * Execute code in the Runnable object using SWT's thread.  If current
	 * thread it already SWT's thread, the code will run immediately.  If the
	 * current thread is not SWT's, code will be run either synchronously or 
	 * asynchronously on SWT's thread at the next reasonable opportunity.
	 * 
	 * This method does not catch any exceptions.
	 * 
	 * @param code code to run
	 * @param async true if SWT asyncExec, false if SWT syncExec
	 * @return success
	 */
	public static boolean execSWTThread(final Runnable code, boolean async) {
		return execSWTThread(code, async ? -1 : -2);
	}

	/**
	 * Schedule execution of the code in the Runnable object using SWT's thread.
	 * Even if the current thread is the SWT Thread, the code will be scheduled.
	 * <p>
	 * Much like Display.asyncExec, except getting the display is handled for you,
	 * and provides the ability to diagnose and monitor scheduled code run.
	 * 
	 * @param msLater time to wait before running code on SWT thread.  0 does not
	 *                mean immediate, but as soon as possible.
	 * @param code Code to run
	 * @return sucess
	 *
	 * @since 3.0.4.3
	 */
	public static boolean execSWTThreadLater(int msLater, final Runnable code) {
		return execSWTThread(code, msLater);
	}

	/**
	 * 
	 * @param code
	 * @param msLater -2: sync<BR>
	 *                -1: sync if on SWT thread, async otherwise<BR>
	 *                 0: async<BR>
	 *                >0: timerExec
	 * @return
	 *
	 * @since 3.0.4.3
	 */
	private static boolean execSWTThread(final Runnable code, final int msLater) {
		Display display = getDisplay();
		if (display == null || code == null) {
			return false;
		}

		if (msLater < 0 && display.getThread() == Thread.currentThread()) {
			if (queue == null) {
				code.run();
			} else {
				long lStartTimeRun = SystemTime.getCurrentTime();

				code.run();

				long wait = SystemTime.getCurrentTime() - lStartTimeRun;
				if (wait > 700) {
					diag_logger.log(SystemTime.getCurrentTime() + "] took " + wait
							+ "ms to run " + Debug.getCompressedStackTrace());
				}
			}
		} else if (msLater >= -1) {
			try {
				if (queue == null) {
					if (msLater <= 0) {
						display.asyncExec(code);
					} else {
						display.timerExec(msLater, code);
					}
				} else {
					queue.add(code);

					diag_logger.log(SystemTime.getCurrentTime() + "] + QUEUE. size= "
							+ queue.size() + "; add " + code + " via "
							+ Debug.getCompressedStackTrace());
					final long lStart = SystemTime.getCurrentTime();

					final Display fDisplay = display;
					AERunnable runnableWrapper = new AERunnable() {
						public void runSupport() {
							long wait = SystemTime.getCurrentTime() - lStart - msLater;
							if (wait > 700) {
								diag_logger.log(SystemTime.getCurrentTime() + "] took " + wait
										+ "ms before SWT ran async code " + code);
							}
							long lStartTimeRun = SystemTime.getCurrentTime();

							try {
								if (fDisplay.isDisposed()) {
									Debug.out("Display disposed while trying to execSWTThread "
											+ code);
									// run anayway, except trap SWT error
									try {
										code.run();
									} catch (SWTException e) {
										Debug.out("Error while execSWTThread w/disposed Display", e);
									}
								} else {
									code.run();
								}
							} finally {
								wait = SystemTime.getCurrentTime() - lStartTimeRun;
								if (wait > 500) {
									diag_logger.log(SystemTime.getCurrentTime() + "] took "
											+ wait + "ms to run " + code);
								}

								diag_logger.log(SystemTime.getCurrentTime()
										+ "] - QUEUE. size=" + queue.size());
								queue.remove(code);
							}
						}
					};
					if (msLater <= 0) {
						display.asyncExec(runnableWrapper);
					} else {
						display.timerExec(msLater, runnableWrapper);
					}
				}
			} catch (NullPointerException e) {
				// If the display is being disposed of, asyncExec may give a null
				// pointer error
				return false;
			}
		} else {
			display.syncExec(code);
		}

		return true;
	}

	/**
	 * Execute code in the Runnable object using SWT's thread.  If current
	 * thread it already SWT's thread, the code will run immediately.  If the
	 * current thread is not SWT's, code will be run asynchronously on SWT's 
	 * thread at the next reasonable opportunity.
	 * 
	 * This method does not catch any exceptions.
	 * 
	 * @param code code to run
	 * @return success
	 */
	public static boolean execSWTThread(Runnable code) {
		return execSWTThread(code, -1);
	}

	public static boolean isThisThreadSWT() {
		SWTThread swt = SWTThread.getInstance();

		if (swt == null) {
			System.err.println("WARNING: SWT Thread not started yet");
		}

		Display display = (swt == null) ? Display.getCurrent() : swt.getDisplay();

		if (display == null) {
			return false;
		}

		// This will throw if we are disposed or on the wrong thread
		// Much better that display.getThread() as that one locks Device.class
		// and may end up causing sync lock when disposing
		try {
			display.getWarnings();
		} catch (SWTException e) {
			return false;
		}

		return (display.getThread() == Thread.currentThread());
	}

	/** Open a messagebox using resource keys for title/text
	 * 
	 * @param parent Parent shell for messagebox
	 * @param style SWT styles for messagebox
	 * @param keyPrefix message bundle key prefix used to get title and text.  
	 *         Title will be keyPrefix + ".title", and text will be set to
	 *         keyPrefix + ".text"
	 * @param textParams any parameters for text
	 * 
	 * @return what the messagebox returns
	 */
	public static int openMessageBox(Shell parent, int style, String keyPrefix,
			String[] textParams) {
		if (style == SWT.OK) {
			int ret = new MessageBoxShell(parent, MessageText.getString(keyPrefix
					+ ".title"), MessageText.getString(keyPrefix + ".text", textParams),
					new String[] {
						MessageText.getString("Button.ok")
					}, 0).open();

			return (ret == 0) ? SWT.OK : SWT.CANCEL;
		} else {
			MessageBox mb = new MessageBox(parent, style);
			mb.setMessage(MessageText.getString(keyPrefix + ".text", textParams));
			mb.setText(MessageText.getString(keyPrefix + ".title"));
			return mb.open();
		}
	}

	/** Open a messagebox with actual title and text
	 * 
	 * @param parent
	 * @param style
	 * @param title
	 * @param text
	 * @return
	 */
	public static int openMessageBox(Shell parent, int style, String title,
			String text) {
		MessageBox mb = new MessageBox(parent == null ? findAnyShell() : parent,
				style);
		mb.setMessage(text);
		mb.setText(title);
		return mb.open();
	}

	/**
	 * Bottom Index may be negative
	 */
	public static int getTableBottomIndex(Table table, int iTopIndex) {
		// on Linux, getItemHeight is slow AND WRONG. so is getItem(x).getBounds().y 
		// getItem(Point) is slow on OSX

		int itemCount = table.getItemCount();
		if (!table.isVisible() || iTopIndex >= itemCount)
			return -1;

		if (Constants.isOSX) {
			try {
				TableItem item = table.getItem(iTopIndex);
				Rectangle bounds = item.getBounds();
				Rectangle clientArea = table.getClientArea();

				int itemHeight = table.getItemHeight();
				int iBottomIndex = Math.min(iTopIndex
						+ (clientArea.height + clientArea.y - bounds.y - 1) / itemHeight,
						itemCount - 1);

				//			System.out.println(bounds + ";" + clientArea + ";" + itemHeight + ";bi="
				//					+ iBottomIndex + ";ti=" + iTopIndex + ";"
				//					+ (clientArea.height + clientArea.y - bounds.y - 1));
				return iBottomIndex;
			} catch (NoSuchMethodError e) {
				// item.getBounds is 3.2
				return Math.min(
						iTopIndex
								+ ((table.getClientArea().height - table.getHeaderHeight() - 1) / table.getItemHeight())
								+ 1, table.getItemCount() - 1);
			}
		}

		// getItem will return null if clientArea's height is smaller than
		// header height.
		int areaHeight = table.getClientArea().height;
		if (areaHeight <= table.getHeaderHeight())
			return -1;

		// 2 offset to be on the safe side
		TableItem bottomItem = table.getItem(new Point(2,
				table.getClientArea().height - 1));
		int iBottomIndex = (bottomItem != null) ? table.indexOf(bottomItem)
				: itemCount - 1;
		return iBottomIndex;
	}

	public static void launch(String sFile) {
		if (sFile == null) {
			return;
		}

		if (SWT.getVersion() >= 3315 || SWT.getVersion() < 3300
				|| UrlUtils.isURL(sFile) || sFile.startsWith("mailto:")) {
			boolean launched = Program.launch(sFile);
			if (!launched && Constants.isUnix
					&& (UrlUtils.isURL(sFile) || sFile.startsWith("mailto:"))) {
				if (!Program.launch("xdg-open " + sFile)) {
						Program.launch("htmlview " + sFile);
				}
			}
		} else {
			if (Constants.isOSX) {
				Program.launch("file://" + sFile.replaceAll(" ", "%20"));
			} else {
				Program.launch(sFile);
			}
		}
	}

	/**
	 * Sets the checkbox in a Virtual Table while inside a SWT.SetData listener
	 * trigger.  SWT 3.1 has an OSX bug that needs working around.
	 * 
	 * @param item
	 * @param checked
	 */
	public static void setCheckedInSetData(final TableItem item,
			final boolean checked) {
		if (DIRECT_SETCHECKED) {
			item.setChecked(checked);
		} else {
			item.setChecked(!checked);
			item.getDisplay().asyncExec(new AERunnable() {
				public void runSupport() {
					item.setChecked(checked);
				}
			});
		}

		if (Constants.isWindowsXP || isGTK) {
			Rectangle r = item.getBounds(0);
			Table table = item.getParent();
			Rectangle rTable = table.getClientArea();

			r.y += VerticalAligner.getTableAdjustVerticalBy(table);
			table.redraw(0, r.y, rTable.width, r.height, true);
		}
	}

	public static boolean linkShellMetricsToConfig(final Shell shell,
			final String sConfigPrefix) {
		String windowRectangle = COConfigurationManager.getStringParameter(
				sConfigPrefix + ".rectangle", null);
		boolean bDidResize = false;
		if (null != windowRectangle) {
			int i = 0;
			int[] values = new int[4];
			StringTokenizer st = new StringTokenizer(windowRectangle, ",");
			try {
				while (st.hasMoreTokens() && i < 4) {
					values[i++] = Integer.valueOf(st.nextToken()).intValue();
				}
				if (i == 4) {
					Rectangle shellBounds = new Rectangle(values[0], values[1],
							values[2], values[3]);
					shell.setBounds(shellBounds);
					verifyShellRect(shell, true);
					bDidResize = true;
				}
			} catch (Exception e) {
			}
		}

		boolean isMaximized = COConfigurationManager.getBooleanParameter(sConfigPrefix
				+ ".maximized")
				&& !Constants.isOSX;
		shell.setMaximized(isMaximized);

		new ShellMetricsResizeListener(shell, sConfigPrefix);

		return bDidResize;
	}

	private static class ShellMetricsResizeListener
		implements Listener
	{
		private int state = -1;

		private String sConfigPrefix;

		private Rectangle bounds = null;

		ShellMetricsResizeListener(Shell shell, String sConfigPrefix) {
			this.sConfigPrefix = sConfigPrefix;
			state = calcState(shell);
			if (state == SWT.NONE)
				bounds = shell.getBounds();

			shell.addListener(SWT.Resize, this);
			shell.addListener(SWT.Move, this);
			shell.addListener(SWT.Dispose, this);
		}

		private int calcState(Shell shell) {
			return shell.getMinimized() ? SWT.MIN : shell.getMaximized()
					&& !Constants.isOSX ? SWT.MAX : SWT.NONE;
		}

		private void saveMetrics() {
			COConfigurationManager.setParameter(sConfigPrefix + ".maximized",
					state == SWT.MAX);

			if (bounds == null)
				return;

			COConfigurationManager.setParameter(sConfigPrefix + ".rectangle",
					bounds.x + "," + bounds.y + "," + bounds.width + "," + bounds.height);
		}

		public void handleEvent(Event event) {
			Shell shell = (Shell) event.widget;
			state = calcState(shell);

			if (event.type != SWT.Dispose && state == SWT.NONE)
				bounds = shell.getBounds();

			if (event.type == SWT.Dispose)
				saveMetrics();
		}
	}

	public static GridData setGridData(Composite composite, int gridStyle,
			Control ctrlBestSize, int maxHeight) {
		GridData gridData = new GridData(gridStyle);
		gridData.heightHint = ctrlBestSize.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		if (gridData.heightHint > maxHeight && maxHeight > 0)
			gridData.heightHint = maxHeight;
		composite.setLayoutData(gridData);

		return gridData;
	}

	public static FormData getFilledFormData() {
		FormData formData = new FormData();
		formData.top = new FormAttachment(0, 0);
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.bottom = new FormAttachment(100, 0);

		return formData;
	}

	public static int pixelsToPoint(int pixels, int dpi) {
		int ret = (int) Math.round((pixels * 72.0) / dpi);
		return ret;
	}

	public static int pixelsToPoint(double pixels, int dpi) {
		int ret = (int) Math.round((pixels * 72.0) / dpi);
		return ret;
	}

	public static boolean drawImage(GC gc, Image image, Rectangle dstRect,
			Rectangle clipping, int hOffset, int vOffset, boolean clearArea) {
		return drawImage(gc, image, new Point(0, 0), dstRect, clipping, hOffset,
				vOffset, clearArea);
	}

	public static boolean drawImage(GC gc, Image image, Rectangle dstRect,
			Rectangle clipping, int hOffset, int vOffset) {
		return drawImage(gc, image, new Point(0, 0), dstRect, clipping, hOffset,
				vOffset, false);
	}

	public static boolean drawImage(GC gc, Image image, Point srcStart,
			Rectangle dstRect, Rectangle clipping, int hOffset, int vOffset,
			boolean clearArea) {
		Rectangle srcRect;
		Point dstAdj;

		if (clipping == null) {
			dstAdj = new Point(0, 0);
			srcRect = new Rectangle(srcStart.x, srcStart.y, dstRect.width,
					dstRect.height);
		} else {
			if (!dstRect.intersects(clipping)) {
				return false;
			}

			dstAdj = new Point(Math.max(0, clipping.x - dstRect.x), Math.max(0,
					clipping.y - dstRect.y));

			srcRect = new Rectangle(0, 0, 0, 0);
			srcRect.x = srcStart.x + dstAdj.x;
			srcRect.y = srcStart.y + dstAdj.y;
			srcRect.width = Math.min(dstRect.width - dstAdj.x, clipping.x
					+ clipping.width - dstRect.x);
			srcRect.height = Math.min(dstRect.height - dstAdj.y, clipping.y
					+ clipping.height - dstRect.y);
		}

		if (!srcRect.isEmpty()) {
			try {
				if (clearArea) {
					gc.fillRectangle(dstRect.x + dstAdj.x + hOffset, dstRect.y + dstAdj.y
							+ vOffset, srcRect.width, srcRect.height);
				}
				gc.drawImage(image, srcRect.x, srcRect.y, srcRect.width,
						srcRect.height, dstRect.x + dstAdj.x + hOffset, dstRect.y
								+ dstAdj.y + vOffset, srcRect.width, srcRect.height);
			} catch (Exception e) {
				System.out.println("drawImage: " + e.getMessage() + ": " + image + ", "
						+ srcRect + ", " + (dstRect.x + dstAdj.y + hOffset) + ","
						+ (dstRect.y + dstAdj.y + vOffset) + "," + srcRect.width + ","
						+ srcRect.height + "; imageBounds = " + image.getBounds());
			}
		}

		return true;
	}

	/**
	 * @param area
	 * @param event id
	 * @param listener
	 */
	public static void addListenerAndChildren(Composite area, int event,
			Listener listener) {
		area.addListener(event, listener);
		Control[] children = area.getChildren();
		for (int i = 0; i < children.length; i++) {
			Control child = children[i];
			if (child instanceof Composite) {
				addListenerAndChildren((Composite) child, event, listener);
			} else {
				child.addListener(event, listener);
			}
		}
	}

	public static Shell findAnyShell() {
		// Pick the main shell if we can
		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (uiFunctions != null) {
			Shell shell = uiFunctions.getMainShell();
			if (shell != null && !shell.isDisposed()) {
				return shell;
			}
		}

		// Get active shell from current display if we can
		Display current = Display.getCurrent();
		if (current == null) {
			return null;
		}
		Shell shell = current.getActiveShell();
		if (shell != null && !shell.isDisposed()) {
			return shell;
		}

		// Get first shell of current display if we can
		Shell[] shells = current.getShells();
		if (shells.length == 0) {
			return null;
		}

		if (shells[0] != null && !shells[0].isDisposed()) {
			return shells[0];
		}

		return null;
	}

	/**
	 * @param listener
	 */
	public static boolean verifyShellRect(Shell shell, boolean bAdjustIfInvalid) {
		boolean bMetricsOk;
		try {
			bMetricsOk = false;
			Point ptTopLeft = shell.getLocation();

			Monitor[] monitors = shell.getDisplay().getMonitors();
			for (int j = 0; j < monitors.length && !bMetricsOk; j++) {
				Rectangle bounds = monitors[j].getBounds();
				bMetricsOk = bounds.contains(ptTopLeft);
			}
		} catch (NoSuchMethodError e) {
			Rectangle bounds = shell.getDisplay().getBounds();
			bMetricsOk = shell.getBounds().intersects(bounds);
		}
		if (!bMetricsOk && bAdjustIfInvalid) {
			centreWindow(shell);
		}
		return bMetricsOk;
	}

	/**
	 * Relayout all composites up from control until there's enough room for the
	 * control to fit
	 * 
	 * @param control Control that had it's sized changed and needs more room
	 */
	public static void relayout(Control control) {
		relayout(control, false);
	}

	/**
	 * Relayout all composites up from control until there's enough room for the
	 * control to fit
	 * 
	 * @param control Control that had it's sized changed and needs more room
	 */
	public static void relayout(Control control, boolean expandOnly) {
		if (control == null || control.isDisposed()) {
			return;
		}

		Composite parent = control.getParent();
		Point targetSize = control.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		Point size = control.getSize();
		if (size.y == targetSize.y && size.x == targetSize.x) {
			return;
		}

		Object layoutData = control.getLayoutData();
		if (layoutData instanceof FormData) {
			FormData fd = (FormData) layoutData;
			if (fd.width != SWT.DEFAULT && fd.height != SWT.DEFAULT) {
				parent.layout();
				return;
			}
		}

		if (expandOnly && size.y >= targetSize.y && size.x >= targetSize.x) {
			parent.layout();
			return;
		}

		while (parent != null) {
			parent.layout(true, true);
			parent = parent.getParent();

			Point newSize = control.getSize();

			//System.out.println("new=" + newSize + ";target=" + targetSize);

			if (newSize.y >= targetSize.y && newSize.x >= targetSize.x) {
				break;
			}
		}

		if (parent != null) {
			parent.layout();
		}
	}

	/**
	 * 
	 */
	public static void beep() {
		execSWTThread(new AERunnable() {
			public void runSupport() {
				Display display = Display.getDefault();
				if (display != null) {
					display.beep();
				}
			}
		});
	}

	/**
	 * 
	 * @param baseFont
	 * @param gc Can be null
	 * @param heightInPixels
	 * @return
	 *
	 * @since 3.0.0.7
	 */
	public static int getFontHeightFromPX(Font baseFont, GC gc, int heightInPixels) {
		Font font = null;
		Device device = baseFont.getDevice();

		// hack..
		heightInPixels++;

		// This isn't accurate, but gets us close
		int size = Utils.pixelsToPoint(heightInPixels, device.getDPI().y) + 1;
		if (size <= 0) {
			return 0;
		}

		boolean bOurGC = gc == null || gc.isDisposed();
		try {
			if (bOurGC) {
				gc = new GC(device);
			}
			FontData[] fontData = baseFont.getFontData();

			do {
				if (font != null) {
					size--;
					font.dispose();
				}
				fontData[0].setHeight(size);

				font = new Font(device, fontData);

				gc.setFont(font);

			} while (font != null && gc.textExtent(GOOD_STRING).y > heightInPixels
					&& size > 1);

		} finally {
			if (bOurGC) {
				gc.dispose();
			}
			if (font != null && !font.isDisposed()) {
				font.dispose();
			}
		}
		return size;
	}

	public static int getFontHeightFromPX(Device device, FontData[] fontData,
			GC gc, int heightInPixels) {
		Font font = null;

		// hack..
		heightInPixels++;

		// This isn't accurate, but gets us close
		int size = Utils.pixelsToPoint(heightInPixels, device.getDPI().y) + 1;
		if (size <= 0) {
			return 0;
		}

		boolean bOurGC = gc == null || gc.isDisposed();
		try {
			if (bOurGC) {
				gc = new GC(device);
			}

			do {
				if (font != null) {
					size--;
					font.dispose();
				}
				fontData[0].setHeight(size);

				font = new Font(device, fontData);

				gc.setFont(font);

			} while (font != null && gc.textExtent(GOOD_STRING).y > heightInPixels
					&& size > 1);

		} finally {
			if (bOurGC) {
				gc.dispose();
			}
			if (font != null && !font.isDisposed()) {
				font.dispose();
			}
		}
		return size;
	}

	public static Font getFontWithHeight(Font baseFont, GC gc, int heightInPixels) {
		Font font = null;
		Device device = baseFont.getDevice();

		// hack..
		heightInPixels++;

		// This isn't accurate, but gets us close
		int size = Utils.pixelsToPoint(heightInPixels, device.getDPI().y) + 1;
		if (size <= 0) {
			size = 2;
		}

		boolean bOurGC = gc == null || gc.isDisposed();
		try {
			if (bOurGC) {
				gc = new GC(device);
			}
			FontData[] fontData = baseFont.getFontData();

			do {
				if (font != null) {
					size--;
					font.dispose();
				}
				fontData[0].setHeight(size);

				font = new Font(device, fontData);

				gc.setFont(font);

			} while (font != null && gc.textExtent(GOOD_STRING).y > heightInPixels
					&& size > 1);

		} finally {
			if (bOurGC) {
				gc.dispose();
			}
		}

		return font;
	}

	/**
	 * @deprecated Use {@link #execSWTThread(AERunnableWithCallback)} to avoid
	 *             thread locking issues
	 */
	public static boolean execSWTThreadWithBool(String ID, AERunnableBoolean code) {
		return execSWTThreadWithBool(ID, code, 0);
	}

	/**
	 * Runs code within the SWT thread, waits for code to complete executing,
	 * (using a sempaphore), and then returns a value.
	 * 
	 * @note USE WITH CAUTION.  If the calling function synchronizes, and the
	 *       runnable code ends up synchronizing on the same object, an indefinite
	 *       thread lock or an unexpected timeout may occur (if one of the threads
	 *       is the SWT thread).<p>
	 *  ex - Thread1 calls c.foo(), which synchronized(this).
	 *     - Thread2 is the SWT Thread.  Thread2 calls c.foo(), which waits on 
	 *       Thread1 to complete.
	 *   	 - c.foo() from Thread1 calls execSWTThreadWithBoolean(.., swtcode, ..),
	 *       which waits for the SWT Thread to return run the swtcode.
	 *     - Deadlock, or Timoeout which returns a false (and no code ran)
	 *
	 * @param ID id for debug
	 * @param code code to run
	 * @param millis ms to timeout in
	 * @return
	 */
	public static boolean execSWTThreadWithBool(String ID,
			AERunnableBoolean code, long millis) {
		if (code == null) {
			return false;
		}

		boolean[] returnValueObject = {
			false
		};

		Display display = getDisplay();

		AESemaphore sem = null;
		if (display == null || display.getThread() != Thread.currentThread()) {
			sem = new AESemaphore(ID);
		}

		try {
			code.setupReturn(ID, returnValueObject, sem);

			if (!execSWTThread(code)) {
				// code never got run
				// XXX: throw instead?
				return false;
			}
		} catch (Throwable e) {
			if (sem != null) {
				sem.release();
			}
			Debug.out(ID, e);
		}
		if (sem != null) {
			sem.reserve(millis);
		}

		return returnValueObject[0];
	}

	/**
	 * @deprecated Use {@link #execSWTThread(AERunnableWithCallback)} to avoid
	 *             thread locking issues
	 */
	public static Object execSWTThreadWithObject(String ID, AERunnableObject code) {
		return execSWTThreadWithObject(ID, code, 0);
	}

	/**
	 * Runs code within the SWT thread, waits for code to complete executing,
	 * (using a sempaphore), and then returns a value.
	 * 
	 * @note USE WITH CAUTION.  If the calling function synchronizes, and the
	 *       runnable code ends up synchronizing on the same object, an indefinite
	 *       thread lock or an unexpected timeout may occur (if one of the threads
	 *       is the SWT thread).<p>
	 *  ex - Thread1 calls c.foo(), which synchronized(this).
	 *     - Thread2 is the SWT Thread.  Thread2 calls c.foo(), which waits on 
	 *       Thread1 to complete.
	 *   	 - c.foo() from Thread1 calls execSWTThreadWithObject(.., swtcode, ..),
	 *       which waits for the SWT Thread to return run the swtcode.
	 *     - Deadlock, or Timoeout which returns a null (and no code ran)
	 *
	 * @param ID id for debug
	 * @param code code to run
	 * @param millis ms to timeout in
	 * @return
	 */
	public static Object execSWTThreadWithObject(String ID,
			AERunnableObject code, long millis) {
		if (code == null) {
			return null;
		}

		Object[] returnValueObject = {
			null
		};

		Display display = getDisplay();

		AESemaphore sem = null;
		if (display == null || display.getThread() != Thread.currentThread()) {
			sem = new AESemaphore(ID);
		}

		try {
			code.setupReturn(ID, returnValueObject, sem);

			if (!execSWTThread(code)) {
				// XXX: throw instead?
				return null;
			}
		} catch (Throwable e) {
			if (sem != null) {
				sem.release();
			}
			Debug.out(ID, e);
		}
		if (sem != null) {
			sem.reserve(millis);
		}

		return returnValueObject[0];
	}

	/**
	 * Waits until modal dialogs are disposed.  Assumes we are on SWT thread
	 *
	 * @since 3.0.1.3
	 */
	public static void waitForModals() {
		SWTThread swt = SWTThread.getInstance();

		Display display;
		if (swt == null) {
			display = Display.getDefault();
			if (display == null) {
				System.err.println("SWT Thread not started yet!");
				return;
			}
		} else {
			if (swt.isTerminated()) {
				return;
			}
			display = swt.getDisplay();
		}

		if (display == null || display.isDisposed()) {
			return;
		}

		Shell[] shells = display.getShells();
		Shell modalShell = null;
		for (int i = 0; i < shells.length; i++) {
			Shell shell = shells[i];
			if ((shell.getStyle() & SWT.APPLICATION_MODAL) > 0) {
				modalShell = shell;
				break;
			}
		}

		if (modalShell != null) {
			while (!modalShell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		}
	}

	public static GridData getWrappableLabelGridData(int hspan, int styles) {
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | styles);
		gridData.horizontalSpan = hspan;
		gridData.widthHint = 0;
		return gridData;
	}

	public static Image createAlphaImage(Device device, int width, int height) {
		return createAlphaImage(device, width, height, (byte) 0);
	}

	public static Image createAlphaImage(Device device, int width, int height,
			byte defaultAlpha) {
		byte[] alphaData = new byte[width * height];
		Arrays.fill(alphaData, 0, alphaData.length, (byte) defaultAlpha);

		ImageData imageData = new ImageData(width, height, 24, new PaletteData(
				0xFF, 0xFF00, 0xFF0000));
		Arrays.fill(imageData.data, 0, imageData.data.length, (byte) 0);
		imageData.alphaData = alphaData;
		Image image = new Image(device, imageData);
		return image;
	}

	public static Image blitImage(Device device, Image srcImage,
			Rectangle srcArea, Image dstImage, Point dstPos) {
		if (srcArea == null) {
			srcArea = srcImage.getBounds();
		}
		Rectangle dstBounds = dstImage.getBounds();
		if (dstPos == null) {
			dstPos = new Point(dstBounds.x, dstBounds.y);
		} else {
			dstBounds.x = dstPos.x;
			dstBounds.y = dstPos.y;
		}
		srcArea.intersect(dstBounds);

		// draw the image with no mask! :(
		GC gc = new GC(dstImage);
		try {
			gc.drawImage(srcImage, srcArea.x, srcArea.y, srcArea.width,
					srcArea.height, dstPos.x, dstPos.y, srcArea.width, srcArea.height);

		} finally {
			gc.dispose();
		}

		ImageData dstImageData = dstImage.getImageData();
		ImageData srcImageData = srcImage.getImageData();
		int yPos = dstPos.y;
		for (int y = 0; y < srcArea.height; y++) {
			int xPos = dstPos.x;
			for (int x = 0; x < srcArea.width; x++) {
				dstImageData.setAlpha(xPos, yPos, srcImageData.getAlpha(x + srcArea.x,
						y + srcArea.y));
				xPos++;
			}
			yPos++;
		}

		return new Image(device, dstImageData);
	}

	/**
	 * 
	 * @param display
	 * @param background
	 * @param foreground
	 * @param foregroundOffsetOnBg
	 * @param modifyForegroundAlpha 0 (fully transparent) to 255 (retain current alpha) 
	 * @return
	 */
	public static Image renderTransparency(Display display, Image background,
			Image foreground, Point foregroundOffsetOnBg, int modifyForegroundAlpha) {
		//Checks
		if (display == null || display.isDisposed() || background == null
				|| background.isDisposed() || foreground == null
				|| foreground.isDisposed())
			return null;
		Rectangle backgroundArea = background.getBounds();
		Rectangle foregroundDrawArea = foreground.getBounds();

		foregroundDrawArea = new Rectangle(foregroundDrawArea.x
				+ foregroundOffsetOnBg.x,
				foregroundDrawArea.y + foregroundOffsetOnBg.y,
				foregroundDrawArea.width, foregroundDrawArea.height);
		foregroundDrawArea.intersect(backgroundArea);

		if (foregroundDrawArea.isEmpty())
			return null;

		Image image = new Image(display, backgroundArea);

		ImageData backData = background.getImageData();
		ImageData foreData = foreground.getImageData();
		ImageData imgData = image.getImageData();

		PaletteData backPalette = backData.palette;
		ImageData backMask = backData.getTransparencyType() != SWT.TRANSPARENCY_ALPHA
				? backData.getTransparencyMask() : null;
		PaletteData forePalette = foreData.palette;
		ImageData foreMask = foreData.getTransparencyType() != SWT.TRANSPARENCY_ALPHA
				? foreData.getTransparencyMask() : null;
		PaletteData imgPalette = imgData.palette;
		image.dispose();

		for (int x = 0; x < backgroundArea.width; x++) {
			for (int y = 0; y < backgroundArea.height; y++) {
				RGB cBack = backPalette.getRGB(backData.getPixel(x, y));
				int aBack = backData.getAlpha(x, y);
				if (backMask != null && backMask.getPixel(x, y) == 0)
					aBack = 0; // special treatment for icons with transparency masks

				int aFore = 0;

				if (foregroundDrawArea.contains(x, y)) {
					final int fx = x - foregroundDrawArea.x;
					final int fy = y - foregroundDrawArea.y;
					RGB cFore = forePalette.getRGB(foreData.getPixel(fx, fy));
					aFore = foreData.getAlpha(fx, fy);
					if (foreMask != null && foreMask.getPixel(fx, fy) == 0)
						aFore = 0; // special treatment for icons with transparency masks
					aFore = aFore * modifyForegroundAlpha / 255;
					cBack.red *= aBack * (255 - aFore);
					cBack.red /= 255;
					cBack.red += aFore * cFore.red;
					cBack.red /= 255;
					cBack.green *= aBack * (255 - aFore);
					cBack.green /= 255;
					cBack.green += aFore * cFore.green;
					cBack.green /= 255;
					cBack.blue *= aBack * (255 - aFore);
					cBack.blue /= 255;
					cBack.blue += aFore * cFore.blue;
					cBack.blue /= 255;
				}
				imgData.setAlpha(x, y, aFore + aBack * (255 - aFore) / 255);
				imgData.setPixel(x, y, imgPalette.getPixel(cBack));
			}
		}
		return new Image(display, imgData);
	}

	public static Control findBackgroundImageControl(Control control) {
		Image image = control.getBackgroundImage();
		if (image == null) {
			return control;
		}

		Composite parent = control.getParent();
		Composite lastParent = parent;
		while (parent != null) {
			Image parentImage = parent.getBackgroundImage();
			if (!image.equals(parentImage)) {
				return lastParent;
			}
			lastParent = parent;
			parent = parent.getParent();
		}

		return control;
	}

	/**
	 * @return
	 *
	 * @since 3.0.3.5
	 */
	public static boolean anyShellHaveStyle(int styles) {
		Display display = Display.getCurrent();
		if (display != null) {
			Shell[] shells = display.getShells();
			for (int i = 0; i < shells.length; i++) {
				Shell shell = shells[i];
				int style = shell.getStyle();
				if ((style & styles) == styles) {
					return true;
				}
			}
		}
		return false;
	}

	public static int[] colorToIntArray(Color color) {
		if (color == null || color.isDisposed()) {
			return null;
		}
		return new int[] {
			color.getRed(),
			color.getGreen(),
			color.getBlue()
		};
	}

	/**
	 * Centers the target <code>Rectangle</code> relative to the reference Rectangle
	 * @param target
	 * @param reference
	 */
	public static void centerRelativeTo(Rectangle target, Rectangle reference) {
		target.x = reference.x + (reference.width / 2) - target.width / 2;
		target.y = reference.y + (reference.height / 2) - target.height / 2;
	}

	/**
	 * Ensure that the given <code>Rectangle</code> is fully visible on the monitor that the cursor
	 * is currently in.  This method does not resize the given Rectangle; it merely reposition it
	 * if appropriate.  If the given Rectangle is taller or wider than the current monitor then
	 * it may not fit 'fully' in the monitor.
	 * <P>
	 * We use a best-effort approach with an emphasis to have at least the top-left of the Rectangle
	 * be visible.  If the given Rectangle does not fit entirely in the monitor then portion
	 * of the right and/or left may be off-screen.
	 * 
	 * <P>
	 * This method does honor global screen elements when possible.  Screen elements include the TaskBar on Windows
	 * and the Application menu on OSX, and possibly others.  The re-positioned Rectangle returned will fit on the
	 * screen without overlapping (or sliding under) these screen elements.
	 * @param rect 
	 * @return
	 */
	public static void makeVisibleOnCursor(Rectangle rect) {

		if (null == rect) {
			return;
		}

		Display display = Display.getCurrent();
		if (null == display) {
			Debug.out("No current display detected.  This method [Utils.makeVisibleOnCursor()] must be called from a display thread.");
			return;
		}

		try {

			/*
			 * Get cursor location
			 */
			Point cursorLocation = display.getCursorLocation();

			/*
			 * Find the monitor that this cursorLocation resides in
			 */
			Monitor[] monitors = display.getMonitors();
			Rectangle monitorBounds = null;
			for (int i = 0; i < monitors.length; i++) {
				monitorBounds = monitors[i].getClientArea();
				if (true == monitorBounds.contains(cursorLocation)) {
					break;
				}
			}

			if (null == monitorBounds) {
				return;
			}

			/*
			 * Make sure the bottom is fully visible on the monitor
			 */

			int bottomDiff = (monitorBounds.y + monitorBounds.height)
					- (rect.y + rect.height);
			if (bottomDiff < 0) {
				rect.y += bottomDiff;
			}

			/*
			 * Make sure the right is fully visible on the monitor
			 */

			int rightDiff = (monitorBounds.x + monitorBounds.width)
					- (rect.x + rect.width);
			if (rightDiff < 0) {
				rect.x += rightDiff;
			}

			/*
			 * Make sure the left is fully visible on the monitor
			 */
			if (rect.x < monitorBounds.x) {
				rect.x = monitorBounds.x;
			}

			/*
			 * Make sure the top is fully visible on the monitor
			 */
			if (rect.y < monitorBounds.y) {
				rect.y = monitorBounds.y;
			}

		} catch (NoSuchMethodError e) {
			//Do nothing
		}

	}
}
