/*
 * Created on Mar 7, 2006 10:42:32 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
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
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.shells;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;

import com.aelitis.azureus.ui.swt.*;

/**
 * 
 * +=====================================+
 * | +----+                              |
 * | |Icon| Big Bold Title               |
 * | +----+                              |
 * | Wrapping message text               |
 * | with optional URL links             |
 * | +-----+                             |
 * | |BGImg|           XX more slideys.. |
 * | | Icon|          Closing in XX secs |
 * | +-----+  [HideAll] [Details] [Hide] |
 * +=====================================+ 
 * 
 * @author TuxPaper
 * @created Mar 7, 2006
 *
 */
public class MessageSlideShell
{
	private static boolean USE_SWT32_BG_SET = true;

	private static final boolean DEBUG = false;

	private final static String REGEX_URLHTML = "<A HREF=\"(.+?)\">(.+?)</A>";

	/** Slide until there's this much gap between shell and edge of screen */
	private final static int EDGE_GAP = 0;

	/** Width used when BG image can't be loaded */
	private final static int SHELL_DEF_WIDTH = 280;

	/** Standard height of the shell.  Shell may grow depending on text */
	private final static int SHELL_MIN_HEIGHT = 150;

	/** Maximimum height of popup.  If text is too long, the full text will be
	 * put into details.
	 */
	private final static int SHELL_MAX_HEIGHT = 330;

	/** Width of the details shell */
	private final static int DETAILS_WIDTH = 550;

	/** Height of the details shell */
	private final static int DETAILS_HEIGHT = 180;

	/** Synchronization for popupList */
	private final static AEMonitor monitor = new AEMonitor("slidey_mon");

	/** List of all popups ever created */
	private static ArrayList historyList = new ArrayList();

	/** Current popup being displayed */
	private static int currentPopupIndex = -1;

	/** Index of first message which the user has not seen (index) - set to -1 if we don't care. :) **/
	private static int firstUnreadMessage = -1;

	/** Shell for popup */
	private Shell shell;

	/** Composite in shell */
	private Composite cShell;

	/** popup could and closing in xx seconds label */
	private Label lblCloseIn;

	/** Button that hides all slideys in the popupList.  Visible only when there's
	 * more than 1 slidey
	 */
	private Button btnHideAll;

	/** Button to move to next message.  Text changes from "Hide" to "Next"
	 * appropriately.
	 */
	private Button btnNext;

	/** paused state of auto-close delay */
	private boolean bDelayPaused = false;

	/** List of SWT objects needing disposal */
	private ArrayList disposeList = new ArrayList();

	/** Text to put into details popup */
	private String sDetails;

	/** Position this popup is in the history list */
	private int idxHistory;

	private Image imgPopup;

	/** Forces the timer feature to be active; overriding the default behavior */
	private boolean forceTimer = true;

	/** Open a popup using resource keys for title/text
	 * 
	 * @param display Display to create the shell on
	 * @param iconID SWT.ICON_* constant for icon in top left
	 * @param keyPrefix message bundle key prefix used to get title and text.  
	 *         Title will be keyPrefix + ".title", and text will be set to
	 *         keyPrefix + ".text"
	 * @param details actual text for details (not a key)
	 * @param textParams any parameters for text
	 * 
	 * @note Display moved to end to remove conflict in constructors
	 */
	public MessageSlideShell(Display display, int iconID, String keyPrefix,
			String details, String[] textParams) {
		this(display, iconID, MessageText.getString(keyPrefix + ".title"),
				MessageText.getString(keyPrefix + ".text", textParams), details);
	}

	public MessageSlideShell(Display display, int iconID, String keyPrefix,
			String details, String[] textParams, Object[] relatedObjects) {
		this(display, iconID, MessageText.getString(keyPrefix + ".title"),
				MessageText.getString(keyPrefix + ".text", textParams), details,
				relatedObjects, false);
	}

	/**
	 * 
	 * @param display
	 * @param iconID
	 * @param keyPrefix
	 * @param details
	 * @param textParams
	 * @param relatedObjects
	 * @param forceTimer Forces the timer feature to be active; overriding the default logic
	 */
	public MessageSlideShell(Display display, int iconID, String keyPrefix,
			String details, String[] textParams, Object[] relatedObjects,
			boolean forceTimer) {
		this(display, iconID, MessageText.getString(keyPrefix + ".title"),
				MessageText.getString(keyPrefix + ".text", textParams), details,
				relatedObjects, forceTimer);
	}

	public MessageSlideShell(Display display, int iconID, String title,
			String text, String details) {
		this(display, iconID, title, text, details, null, false);
	}

	/**
	 * Open Mr Slidey
	 * 
	 * @param display Display to create the shell on
	 * @param iconID SWT.ICON_* constant for icon in top left
	 * @param title Text to put in the title
	 * @param text Text to put in the body
	 * @param details Text displayed when the Details button is pressed.  Null
	 *                 for disabled Details button.
	 * @param forceTimer Forces the timer feature to be active; overriding the default logic
	 */
	public MessageSlideShell(Display display, int iconID, String title,
			String text, String details, Object[] relatedObjects, boolean forceTimer) {
		try {
			monitor.enter();

			this.forceTimer = forceTimer;

			PopupParams popupParams = new PopupParams(iconID, title, text, details,
					relatedObjects);
			historyList.add(popupParams);
			if (currentPopupIndex < 0) {
				create(display, popupParams, true);
			}
		} catch (Exception e) {
			Logger.log(new LogEvent(LogIDs.GUI, "Mr. Slidey Init", e));
			disposeShell(shell);
			Utils.disposeSWTObjects(disposeList);
		} finally {
			monitor.exit();
		}
	}

	private MessageSlideShell(Display display, PopupParams popupParams,
			boolean bSlide) {
		create(display, popupParams, bSlide);
	}

	public static void displayLastMessage(final Display display,
			final boolean last_unread) {
		display.asyncExec(new AERunnable() {
			public void runSupport() {
				if (historyList.isEmpty()) {
					return;
				}
				if (currentPopupIndex >= 0) {
					return;
				} // Already being displayed.
				int msg_index = firstUnreadMessage;
				if (!last_unread || msg_index == -1) {
					msg_index = historyList.size() - 1;
				}
				new MessageSlideShell(display,
						(PopupParams) historyList.get(msg_index), true);
			}
		});
	}

	/**
	 * Adds this message to the slide shell without forcing it to be displayed.
	 * @param relatedTo 
	 */
	public static void recordMessage(int iconID, String title, String text,
			String details, Object[] relatedTo) {
		try {
			monitor.enter();
			historyList.add(new PopupParams(iconID, title, text, details, relatedTo));
			if (firstUnreadMessage == -1) {
				firstUnreadMessage = historyList.size() - 1;
			}
		} finally {
			monitor.exit();
		}
	}

	private void create(final Display display, final PopupParams popupParams,
			boolean bSlide) {

		firstUnreadMessage = -1; // Reset the last read message counter.

		GridData gridData;
		int shellWidth;
		int style = SWT.ON_TOP;

		boolean bDisableSliding = COConfigurationManager.getBooleanParameter("GUI_SWT_DisableAlertSliding");
		if (bDisableSliding) {
			bSlide = false;
			style = SWT.NONE;
		}

		if (DEBUG)
			System.out.println("create " + (bSlide ? "SlideIn" : "") + ";"
					+ historyList.indexOf(popupParams) + ";");

		idxHistory = historyList.indexOf(popupParams);

		// 2 Assertions
		if (idxHistory < 0) {
			System.err.println("Not in popup history list");
			return;
		}

		if (currentPopupIndex == idxHistory) {
			System.err.println("Trying to open already opened!! " + idxHistory);
			return;
		}

		try {
			monitor.enter();
			currentPopupIndex = idxHistory;
		} finally {
			monitor.exit();
		}

		if (DEBUG)
			System.out.println("set currIdx = " + idxHistory);

		sDetails = popupParams.details;

		// Load Images
		// Disable BG Image on OSX
		if (imgPopup == null) {
			if (Constants.isOSX && (SWT.getVersion() < 3221 || !USE_SWT32_BG_SET)) {
				USE_SWT32_BG_SET = false;
				imgPopup = null;
			} else {
				imgPopup = ImageRepository.getImage("popup");
			}
		}
		Rectangle imgPopupBounds;
		if (imgPopup != null) {
			shellWidth = imgPopup.getBounds().width;
			imgPopupBounds = imgPopup.getBounds();
		} else {
			shellWidth = SHELL_DEF_WIDTH;
			imgPopupBounds = null;
		}
		Image imgIcon = null;
		switch (popupParams.iconID) {
			case SWT.ICON_ERROR:
				imgIcon = ImageRepository.getImage("error");
				break;

			case SWT.ICON_WARNING:
				imgIcon = ImageRepository.getImage("warning");
				break;

			case SWT.ICON_INFORMATION:
				imgIcon = ImageRepository.getImage("info");
				break;

			default:
				imgIcon = null;
				break;
		}

		/*
		 * If forceTimer is true then we always show the counter for auto-closing the shell;
		 * otherwise proceed to the more fine-grained logic
		 */
		if (true == forceTimer) {
			bDelayPaused = false;
		} else {
			// if there's a link, or the info is non-information,
			// disable timer and mouse watching
			bDelayPaused = UrlUtils.parseHTMLforURL(popupParams.text) != null
					|| popupParams.iconID != SWT.ICON_INFORMATION || !bSlide;
		}
		// Pause the auto-close delay when mouse is over slidey
		// This will be applies to every control
		final MouseTrackAdapter mouseAdapter = bDelayPaused ? null
				: new MouseTrackAdapter() {
					public void mouseEnter(MouseEvent e) {
						bDelayPaused = true;
					}

					public void mouseExit(MouseEvent e) {
						bDelayPaused = false;
					}
				};

		// Create shell & widgets
		if (bDisableSliding) {
			UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
			if (uiFunctions != null) {
				Shell mainShell = uiFunctions.getMainShell();
				if (mainShell != null) {
					shell = new Shell(mainShell, style);
				}
			}
		}
		if (shell == null) {
			shell = new Shell(display, style);
		}
		if (USE_SWT32_BG_SET) {
			try {
				shell.setBackgroundMode(SWT.INHERIT_DEFAULT);
			} catch (NoSuchMethodError e) {
				// Ignore
			} catch (NoSuchFieldError e2) {
				// ignore
			}
		}
		Utils.setShellIcon(shell);
		shell.setText(popupParams.title);

		UISkinnableSWTListener[] listeners = UISkinnableManagerSWT.getInstance().getSkinnableListeners(
				MessageSlideShell.class.toString());
		for (int i = 0; i < listeners.length; i++) {
			try {
				listeners[i].skinBeforeComponents(shell, this, popupParams.relatedTo);
			} catch (Exception e) {
				Debug.out(e);
			}
		}

		FormLayout shellLayout = new FormLayout();
		shell.setLayout(shellLayout);

		cShell = new Composite(shell, SWT.NULL);
		GridLayout layout = new GridLayout(3, false);
		cShell.setLayout(layout);

		FormData formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		cShell.setLayoutData(formData);

		Label lblIcon = new Label(cShell, SWT.NONE);
		lblIcon.setImage(imgIcon);
		lblIcon.setLayoutData(new GridData());

		Label lblTitle = new Label(cShell, SWT.getVersion() < 3100 ? SWT.NONE
				: SWT.WRAP);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		if (SWT.getVersion() < 3100)
			gridData.widthHint = 140;
		lblTitle.setLayoutData(gridData);
		lblTitle.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
		lblTitle.setText(popupParams.title);
		FontData[] fontData = lblTitle.getFont().getFontData();
		fontData[0].setStyle(SWT.BOLD);
		fontData[0].setHeight((int) (fontData[0].getHeight() * 1.5));
		Font boldFont = new Font(display, fontData);
		disposeList.add(boldFont);
		lblTitle.setFont(boldFont);

		final Button btnDetails = new Button(cShell, SWT.TOGGLE);
		btnDetails.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
		Messages.setLanguageText(btnDetails, "popup.error.details");
		gridData = new GridData();
		btnDetails.setLayoutData(gridData);
		btnDetails.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event arg0) {
				try {
					boolean bShow = btnDetails.getSelection();
					if (bShow) {
						Shell detailsShell = new Shell(display, SWT.BORDER | SWT.ON_TOP);
						Utils.setShellIcon(detailsShell);
						detailsShell.setLayout(new FillLayout());
						StyledText textDetails = new StyledText(detailsShell, SWT.READ_ONLY
								| SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
						textDetails.setBackground(display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
						textDetails.setForeground(display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));
						textDetails.setWordWrap(true);
						textDetails.setText(sDetails);
						detailsShell.layout();
						Rectangle shellBounds = shell.getBounds();
						detailsShell.setBounds(shellBounds.x + shellBounds.width
								- DETAILS_WIDTH, shellBounds.y - DETAILS_HEIGHT, DETAILS_WIDTH,
								DETAILS_HEIGHT);
						detailsShell.open();
						shell.setData("detailsShell", detailsShell);
						shell.addDisposeListener(new DisposeListener() {
							public void widgetDisposed(DisposeEvent e) {
								Shell detailsShell = (Shell) shell.getData("detailsShell");
								if (detailsShell != null && !detailsShell.isDisposed()) {
									detailsShell.dispose();
								}
							}
						});

						// disable auto-close on opening of details
						bDelayPaused = true;
						removeMouseTrackListener(shell, mouseAdapter);
					} else {
						Shell detailsShell = (Shell) shell.getData("detailsShell");
						if (detailsShell != null && !detailsShell.isDisposed()) {
							detailsShell.dispose();
						}
					}
				} catch (Exception e) {
					Logger.log(new LogEvent(LogIDs.GUI, "Mr. Slidey DetailsButton", e));
				}
			}
		});

		createLinkLabel(cShell, true, popupParams);

		lblCloseIn = new Label(cShell, SWT.TRAIL);
		lblCloseIn.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
		// Ensure computeSize computes for 2 lined label
		lblCloseIn.setText("\n");
		gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
		gridData.horizontalSpan = 3;
		lblCloseIn.setLayoutData(gridData);

		final Composite cButtons = new Composite(cShell, SWT.NULL);
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		gridLayout.verticalSpacing = 0;
		if (Constants.isOSX)
			gridLayout.horizontalSpacing = 0;
		gridLayout.numColumns = (idxHistory > 0) ? 3 : 2;
		cButtons.setLayout(gridLayout);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END
				| GridData.VERTICAL_ALIGN_CENTER);
		gridData.horizontalSpan = 3;
		cButtons.setLayoutData(gridData);

		btnHideAll = new Button(cButtons, SWT.PUSH);
		Messages.setLanguageText(btnHideAll, "popup.error.hideall");
		btnHideAll.setVisible(false);
		btnHideAll.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
		// XXX SWT.Selection doesn't work on latest GTK (2.8.17) & SWT3.2 for ON_TOP
		btnHideAll.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event arg0) {
				cButtons.setEnabled(false);

				shell.dispose();
			}
		});

		if (idxHistory > 0) {
			final Button btnPrev = new Button(cButtons, SWT.PUSH);
			btnPrev.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
			btnPrev.setText(MessageText.getString("popup.previous", new String[] {
				"" + idxHistory
			}));
			btnPrev.addListener(SWT.MouseUp, new Listener() {
				public void handleEvent(Event arg0) {
					disposeShell(shell);
					int idx = historyList.indexOf(popupParams) - 1;
					if (idx >= 0) {
						PopupParams item = (PopupParams) historyList.get(idx);
						showPopup(display, item, false);
						disposeShell(shell);
					}
				}
			});
		}

		btnNext = new Button(cButtons, SWT.PUSH);
		btnNext.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
		int numAfter = historyList.size() - idxHistory - 1;
		setButtonNextText(numAfter);

		btnNext.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event arg0) {
				if (DEBUG)
					System.out.println("Next Pressed");

				if (idxHistory + 1 < historyList.size()) {
					showPopup(display, (PopupParams) historyList.get(idxHistory + 1),
							false);
				}

				disposeShell(shell);
			}
		});

		// Image has gap for text at the top (with image at bottom left)
		// trim top to height of shell 
		Point bestSize = cShell.computeSize(shellWidth, SWT.DEFAULT);
		if (bestSize.y < SHELL_MIN_HEIGHT)
			bestSize.y = SHELL_MIN_HEIGHT;
		else if (bestSize.y > SHELL_MAX_HEIGHT) {
			bestSize.y = SHELL_MAX_HEIGHT;
			if (sDetails == null) {
				sDetails = popupParams.text;
			} else {
				sDetails = popupParams.text + "\n===============\n" + sDetails;
			}
		}

		if (imgPopup != null) {
			// no text on the frog in the bottom left
			int bottomHeight = cButtons.computeSize(SWT.DEFAULT, SWT.DEFAULT).y
					+ lblCloseIn.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
			if (bottomHeight < 50)
				bestSize.y += 50 - bottomHeight;

			final Image imgBackground = new Image(display, bestSize.x, bestSize.y);

			disposeList.add(imgBackground);
			GC gc = new GC(imgBackground);
			int dstY = imgPopupBounds.height - bestSize.y;
			if (dstY < 0)
				dstY = 0;
			gc.drawImage(imgPopup, 0, dstY, imgPopupBounds.width,
					imgPopupBounds.height - dstY, 0, 0, bestSize.x, bestSize.y);
			gc.dispose();

			boolean bAlternateDrawing = true;
			if (USE_SWT32_BG_SET) {
				try {
					shell.setBackgroundImage(imgBackground);
					bAlternateDrawing = false;
				} catch (NoSuchMethodError e) {
				}
			}

			if (bAlternateDrawing) {
				// Drawing of BG Image for pre SWT 3.2

				cShell.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent e) {
						e.gc.drawImage(imgBackground, e.x, e.y, e.width, e.height, e.x,
								e.y, e.width, e.height);
					}
				});

				Color colorBG = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
				final RGB bgRGB = colorBG.getRGB();

				PaintListener paintListener = new PaintListener() {
					// OSX: copyArea() causes a paint event, resulting in recursion
					boolean alreadyPainting = false;

					public void paintControl(PaintEvent e) {
						if (alreadyPainting || e.width <= 0 || e.height <= 0) {
							return;
						}

						alreadyPainting = true;

						try {
							Rectangle bounds = ((Control) e.widget).getBounds();

							Image img = new Image(display, e.width, e.height);
							e.gc.copyArea(img, e.x, e.y);

							e.gc.drawImage(imgBackground, -bounds.x, -bounds.y);

							// Set the background color to invisible.  img.setBackground
							// doesn't work, so change transparentPixel directly and roll
							// a new image
							ImageData data = img.getImageData();
							data.transparentPixel = data.palette.getPixel(bgRGB);
							Image imgTransparent = new Image(display, data);

							// This is an alternative way of setting the transparency.
							// Probably much slower

							//int bgIndex = data.palette.getPixel(bgRGB);
							//ImageData transparencyMask = data.getTransparencyMask();
							//for (int y = 0; y < data.height; y++) {
							//	for (int x = 0; x < data.width; x++) {
							//		if (bgIndex == data.getPixel(x, y))
							//			transparencyMask.setPixel(x, y, 0);
							//	}
							//}
							//
							//Image imgTransparent = new Image(display, data, transparencyMask);

							e.gc.drawImage(imgTransparent, 0, 0, e.width, e.height, e.x, e.y,
									e.width, e.height);

							img.dispose();
							imgTransparent.dispose();
						} finally {
							alreadyPainting = false;
						}
					}
				};

				shell.setBackground(colorBG);
				cShell.setBackground(colorBG);
				addPaintListener(cShell, paintListener, colorBG, true);
			}
		}

		Rectangle bounds = null;
		try {
			UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
			if (uiFunctions != null) {
				Shell mainShell = uiFunctions.getMainShell();
				if (mainShell != null) {
					bounds = mainShell.getMonitor().getClientArea();
				}
			} else {
				Shell shell = display.getActiveShell();
				if (shell != null) {
					bounds = shell.getMonitor().getClientArea();
				}
			}
			if (bounds == null) {
				bounds = shell.getMonitor().getClientArea();
			}
		} catch (Exception e) {
		}
		if (bounds == null) {
			bounds = display.getClientArea();
		}

		Rectangle endBounds;
		if (bDisableSliding) {
			endBounds = new Rectangle(((bounds.x + bounds.width) / 2)
					- (bestSize.x / 2), ((bounds.y + bounds.height) / 2)
					- (bestSize.y / 2), bestSize.x, bestSize.y);
		} else {
			int boundsX2 = bounds.x + bounds.width;
			int boundsY2 = bounds.y + bounds.height;
			endBounds = shell.computeTrim(boundsX2 - bestSize.x, boundsY2
					- bestSize.y, bestSize.x, bestSize.y);

			// bottom and right trim will be off the edge, calulate this trim
			// and adjust it up and left (trim may not be the same size on all sides)
			int diff = (endBounds.x + endBounds.width) - boundsX2;
			if (diff >= 0)
				endBounds.x -= diff + EDGE_GAP;
			diff = (endBounds.y + endBounds.height) - boundsY2;
			if (diff >= 0) {
				endBounds.y -= diff + EDGE_GAP;
			}
			//System.out.println("best" + bestSize + ";mon" + bounds + ";end" + endBounds);
		}

		FormData data = new FormData(bestSize.x, bestSize.y);
		cShell.setLayoutData(data);

		btnDetails.setVisible(sDetails != null);
		if (sDetails == null) {
			gridData = new GridData();
			gridData.widthHint = 0;
			btnDetails.setLayoutData(gridData);
		}
		shell.layout();

		btnNext.setFocus();
		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				Utils.disposeSWTObjects(disposeList);

				if (currentPopupIndex == idxHistory) {
					if (DEBUG)
						System.out.println("Clear #" + currentPopupIndex + "/" + idxHistory);
					try {
						monitor.enter();
						currentPopupIndex = -1;
					} finally {
						monitor.exit();
					}
				}
			}
		});

		shell.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event event) {
				if (event.detail == SWT.TRAVERSE_ESCAPE) {
					disposeShell(shell);
					event.doit = false;
				}
			}
		});

		if (mouseAdapter != null)
			addMouseTrackListener(shell, mouseAdapter);

		for (int i = 0; i < listeners.length; i++) {
			try {
				listeners[i].skinAfterComponents(shell, this, popupParams.relatedTo);
			} catch (Exception e) {
				Debug.out(e);
			}
		}

		runPopup(endBounds, idxHistory, bSlide);
	}

	/**
	 * @param shell2
	 * @param b
	 *
	 * @since 3.0.0.9
	 */
	private void createLinkLabel(Composite shell, boolean tryLinkIfURLs,
			PopupParams popupParams) {

		Matcher matcher = Pattern.compile(REGEX_URLHTML, Pattern.CASE_INSENSITIVE).matcher(
				popupParams.text);
		boolean hasHTML = matcher.find();
		if (tryLinkIfURLs && hasHTML) {
			try {
				Link linkLabel = new Link(cShell, SWT.WRAP);
				GridData gridData = new GridData(GridData.FILL_BOTH);
				gridData.horizontalSpan = 3;
				linkLabel.setLayoutData(gridData);
				linkLabel.setForeground(shell.getDisplay().getSystemColor(
						SWT.COLOR_BLACK));
				linkLabel.setText(popupParams.text);
				linkLabel.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						if (e.text.startsWith(":")) {
							return;
						}
						if (e.text.endsWith(".torrent"))
							TorrentOpener.openTorrent(e.text);
						else
							Utils.launch(e.text);
					}
				});

				String tooltip = null;
				matcher.reset();
				while (matcher.find()) {
					if (tooltip == null)
						tooltip = "";
					else
						tooltip += "\n";
					String url = matcher.group(1);
					if (url != null && url.startsWith(":")) {
						url = url.substring(1);
					}
					tooltip += matcher.group(2) + ": " + url;
				}
				linkLabel.setToolTipText(tooltip);
			} catch (Throwable t) {
				createLinkLabel(shell, false, popupParams);
			}
		} else {
			// 3.0
			Label linkLabel = new Label(cShell, SWT.WRAP);
			GridData gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 3;
			linkLabel.setLayoutData(gridData);

			//<a href="http://atorre.s">test</A> and <a href="http://atorre.s">test2</A>

			if (hasHTML) {
				matcher.reset();
				popupParams.text = matcher.replaceAll("$2 ($1)");

				if (sDetails == null) {
					sDetails = popupParams.text;
				} else {
					sDetails = popupParams.text + "\n---------\n" + sDetails;
				}
			}

			linkLabel.setForeground(shell.getDisplay().getSystemColor(SWT.COLOR_BLACK));
			linkLabel.setText(popupParams.text);
		}
	}

	/**
	 * @param numAfter
	 */
	private void setButtonNextText(int numAfter) {
		if (numAfter <= 0)
			Messages.setLanguageText(btnNext, "popup.error.hide");
		else
			Messages.setLanguageText(btnNext, "popup.next", new String[] {
				"" + numAfter
			});
		cShell.layout(true);
	}

	/**
	 * Show the popup with the specified parameters.
	 * 
	 * @param display Display to show on 
	 * @param item popup to display.  Must already exist in historyList
	 * @param bSlide Whether to slide in or show immediately 
	 */
	private void showPopup(final Display display, final PopupParams item,
			final boolean bSlide) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				new MessageSlideShell(display, item, bSlide);
			}
		});
	}

	/**
	 * Adds mousetracklistener to composite and all it's children
	 * 
	 * @param parent Composite to start at
	 * @param listener Listener to add
	 */
	private void addMouseTrackListener(Composite parent,
			MouseTrackListener listener) {
		if (parent == null || listener == null || parent.isDisposed())
			return;

		parent.addMouseTrackListener(listener);
		Control[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			Control control = children[i];
			if (control instanceof Composite)
				addMouseTrackListener((Composite) control, listener);
			else
				control.addMouseTrackListener(listener);
		}
	}

	private void addPaintListener(Composite parent, PaintListener listener,
			Color colorBG, boolean childrenOnly) {
		if (parent == null || listener == null || parent.isDisposed())
			return;

		if (!childrenOnly) {
			parent.addPaintListener(listener);
			parent.setBackground(colorBG);
		}

		Control[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			Control control = children[i];

			control.addPaintListener(listener);
			control.setBackground(colorBG);

			if (control instanceof Composite)
				addPaintListener((Composite) control, listener, colorBG, true);
		}
	}

	/**
	 * removes mousetracklistener from composite and all it's children
	 * 
	 * @param parent Composite to start at
	 * @param listener Listener to remove
	 */
	private void removeMouseTrackListener(Composite parent,
			MouseTrackListener listener) {
		if (parent == null || listener == null || parent.isDisposed())
			return;

		Control[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			Control control = children[i];
			control.removeMouseTrackListener(listener);
			if (control instanceof Composite)
				removeMouseTrackListener((Composite) control, listener);
		}
	}

	/**
	 * Start the slid in, wait specified time while notifying user of impending
	 * auto-close, then slide out.  Run on separate thread, so this method
	 * returns immediately
	 * 
	 * @param endBounds end location and size wanted
	 * @param idx Index in historyList of popup (Used to calculate # prev, next)
	 * @param bSlide Whether to slide in, or show immediately
	 */
	private void runPopup(final Rectangle endBounds, final int idx,
			final boolean bSlide) {
		if (shell == null || shell.isDisposed())
			return;

		final Display display = shell.getDisplay();

		if (DEBUG)
			System.out.println("runPopup " + idx + ((bSlide) ? " Slide" : " Instant"));

		AEThread thread = new AEThread("Slidey", true) {
			private final static int PAUSE = 500;

			public void runSupport() {
				if (shell == null || shell.isDisposed())
					return;

				if (bSlide) {
					new SlideShell(shell, SWT.UP, endBounds).run();
				} else {
					Utils.execSWTThread(new AERunnable() {

						public void runSupport() {
							shell.setBounds(endBounds);
							shell.open();
						}
					});
				}

				int delayLeft = COConfigurationManager.getIntParameter("Message Popup Autoclose in Seconds") * 1000;
				final boolean autohide = (delayLeft != 0);

				long lastDelaySecs = 0;
				int lastNumPopups = -1;
				while ((!autohide || bDelayPaused || delayLeft > 0)
						&& !shell.isDisposed()) {
					int delayPausedOfs = (bDelayPaused ? 1 : 0);
					final long delaySecs = Math.round(delayLeft / 1000.0)
							+ delayPausedOfs;
					final int numPopups = historyList.size();
					if (lastDelaySecs != delaySecs || lastNumPopups != numPopups) {
						lastDelaySecs = delaySecs;
						lastNumPopups = numPopups;
						shell.getDisplay().asyncExec(new AERunnable() {
							public void runSupport() {
								String sText = "";

								if (lblCloseIn == null || lblCloseIn.isDisposed())
									return;

								lblCloseIn.setRedraw(false);
								if (!bDelayPaused && autohide)
									sText += MessageText.getString("popup.closing.in",
											new String[] {
												String.valueOf(delaySecs)
											});

								int numPopupsAfterUs = numPopups - idx - 1;
								boolean bHasMany = numPopupsAfterUs > 0;
								if (bHasMany) {
									sText += "\n";
									sText += MessageText.getString("popup.more.waiting",
											new String[] {
												String.valueOf(numPopupsAfterUs)
											});
								}

								lblCloseIn.setText(sText);

								if (btnHideAll.getVisible() != bHasMany) {
									cShell.setRedraw(false);
									btnHideAll.setVisible(bHasMany);
									lblCloseIn.getParent().layout(true);
									cShell.setRedraw(true);
								}

								setButtonNextText(numPopupsAfterUs);

								// Need to redraw to cause a paint
								lblCloseIn.setRedraw(true);
							}
						});
					}

					if (!bDelayPaused)
						delayLeft -= PAUSE;
					try {
						Thread.sleep(PAUSE);
					} catch (InterruptedException e) {
						delayLeft = 0;
					}
				}

				if (this.isInterrupted()) {
					// App closedown likely, boot out ASAP
					disposeShell(shell);
					return;
				}

				// Assume that if the shell was disposed during loop, it's on purpose
				// and that it has handled whether to show the next popup or not
				if (shell != null && !shell.isDisposed()) {
					if (idx + 1 < historyList.size()) {
						showPopup(display, (PopupParams) historyList.get(idx + 1), true);
					}

					// slide out current popup
					if (bSlide)
						new SlideShell(shell, SWT.RIGHT).run();

					disposeShell(shell);
				}
			}
		};
		thread.start();
	}

	private void disposeShell(final Shell shell) {
		if (shell == null || shell.isDisposed())
			return;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				shell.dispose();
			}
		});
	}

	/**
	 * Waits until all slideys are closed before returning to caller.
	 */
	public static void waitUntilClosed() {
		if (currentPopupIndex < 0)
			return;

		Display display = Display.getCurrent();
		while (currentPopupIndex >= 0) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

	public static String stripOutHyperlinks(String message) {
		return Pattern.compile(REGEX_URLHTML, Pattern.CASE_INSENSITIVE).matcher(
				message).replaceAll("$2");
	}

	/**
	 * XXX This could/should be its own class 
	 */
	private class SlideShell
	{
		private int STEP = 8;

		private int PAUSE = 30;

		private Shell shell;

		private Rectangle shellBounds = null;

		private Rectangle endBounds;

		private final int direction;

		private final boolean slideIn;

		/**
		 * Slide In
		 * 
		 * @param shell
		 * @param direction 
		 * @param endBounds 
		 */
		public SlideShell(final Shell shell, int direction,
				final Rectangle endBounds) {
			this.shell = shell;
			this.endBounds = endBounds;
			this.slideIn = true;
			this.direction = direction;

			if (shell == null || shell.isDisposed())
				return;

			Display display = shell.getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					if (shell == null || shell.isDisposed())
						return;

					switch (SlideShell.this.direction) {
						case SWT.UP:
						default:
							shell.setLocation(endBounds.x, endBounds.y);
							Rectangle displayBounds = null;
							try {
								boolean ok = false;
								Monitor[] monitors = shell.getDisplay().getMonitors();
								for (int i = 0; i < monitors.length; i++) {
									Monitor monitor = monitors[i];
									displayBounds = monitor.getBounds();
									if (displayBounds.contains(endBounds.x, endBounds.y)) {
										ok = true;
										break;
									}
								}
								if (!ok) {
									displayBounds = shell.getMonitor().getBounds();
								}
							} catch (Throwable t) {
								displayBounds = shell.getDisplay().getBounds();
							}

							shellBounds = new Rectangle(endBounds.x, displayBounds.y
									+ displayBounds.height, endBounds.width, 0);
							break;
					}
					shell.setBounds(shellBounds);
					shell.setVisible(true);

					if (DEBUG)
						System.out.println("Slide In: " + shell.getText());
				}
			});
		}

		/**
		 * Slide Out
		 * 
		 * @param shell
		 * @param direction
		 */
		public SlideShell(final Shell shell, int direction) {
			this.shell = shell;
			this.slideIn = false;
			this.direction = direction;
			if (DEBUG && canContinue())
				shell.getDisplay().syncExec(new Runnable() {
					public void run() {
						System.out.println("Slide Out: " + shell.getText());
					}
				});
		}

		private boolean canContinue() {
			if (shell == null || shell.isDisposed())
				return false;

			if (shellBounds == null)
				return true;

			//System.out.println((slideIn ? "In" : "Out") + ";" + direction + ";S:" + shellBounds + ";" + endBounds);
			if (slideIn) {
				if (direction == SWT.UP) {
					return shellBounds.y > endBounds.y;
				}
				// TODO: Other directions
			} else {
				if (direction == SWT.RIGHT) {
					// stop early, because some OSes have trim, and won't allow the window
					// to go smaller than it.
					return shellBounds.width > 10;
				}
			}
			return false;
		}

		public void run() {

			while (canContinue()) {
				long lStartedAt = System.currentTimeMillis();

				shell.getDisplay().syncExec(new AERunnable() {
					public void runSupport() {
						if (shell == null || shell.isDisposed()) {
							return;
						}

						if (shellBounds == null) {
							shellBounds = shell.getBounds();
						}

						int delta;
						if (slideIn) {
							switch (direction) {
								case SWT.UP:
									delta = Math.min(endBounds.height - shellBounds.height, STEP);
									shellBounds.height += delta;
									delta = Math.min(shellBounds.y - endBounds.y, STEP);
									shellBounds.y -= delta;
									break;

								default:
									break;
							}
						} else {
							switch (direction) {
								case SWT.RIGHT:
									delta = Math.min(shellBounds.width, STEP);
									shellBounds.width -= delta;
									shellBounds.x += delta;

									if (shellBounds.width == 0) {
										shell.dispose();
										return;
									}
									break;

								default:
									break;
							}
						}

						shell.setBounds(shellBounds);
						shell.update();
					}
				});

				try {
					long lDrawTime = System.currentTimeMillis() - lStartedAt;
					long lSleepTime = PAUSE - lDrawTime;
					if (lSleepTime < 15) {
						double d = (lDrawTime + 15.0) / PAUSE;
						PAUSE *= d;
						STEP *= d;
						lSleepTime = 15;
					}
					Thread.sleep(lSleepTime);
				} catch (Exception e) {
				}
			}
		}
	}

	private static class PopupParams
	{
		int iconID;

		String title;

		String text;

		String details;

		long addedOn;

		Object[] relatedTo;

		/**
		 * @param iconID
		 * @param title
		 * @param text
		 * @param details
		 */
		public PopupParams(int iconID, String title, String text, String details) {
			this.iconID = iconID;
			this.title = title;
			this.text = text;
			this.details = details;
			addedOn = System.currentTimeMillis();
		}

		/**
		 * @param iconID2
		 * @param title2
		 * @param text2
		 * @param details2
		 * @param relatedTo
		 */
		public PopupParams(int iconID, String title, String text, String details,
				Object[] relatedTo) {
			this(iconID, title, text, details);
			this.relatedTo = relatedTo;
		}
	}

	/**
	 * Test
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		final Display display = Display.getDefault();

		Shell shell = new Shell(display, SWT.DIALOG_TRIM);
		shell.setLayout(new FillLayout());
		Button btn = new Button(shell, SWT.PUSH);
		btn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				test(display);
			}
		});
		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	public static void test(Display display) {

		ImageRepository.loadImages(display);

		String title = "This is the title that never ends, never ends!";
		String text = "This is a very long message with lots of information and "
				+ "stuff you really should read.  Are you still reading? Good, because "
				+ "reading <a href=\"http://moo.com\">stimulates</a> the mind and grows "
				+ "hair on your chest.\n\n  Unless you are a girl, then it makes you want "
				+ "to read more.  It's an endless cycle of reading that will never "
				+ "end.  Cursed is the long text that is in this test and may it fill"
				+ "every last line of the shell until there is no more.";

		// delay before running, to give eclipse time to finish up it's work
		// Otherwise, Mr Slidey is jumpy
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//		MessagePopupShell shell = new MessagePopupShell(display,
		//				MessagePopupShell.ICON_INFO, "Title", text, "Details");

		new MessageSlideShell(display, SWT.ICON_INFORMATION,
				"Simple. . . . . . . . . . . . . . . . . . .", "Simple", (String) null);

		new MessageSlideShell(display, SWT.ICON_INFORMATION, title + "1", text,
				"Details: " + text);

		new MessageSlideShell(display, SWT.ICON_INFORMATION, "ShortTitle2",
				"ShortText", "Details");
		MessageSlideShell.waitUntilClosed();

		new MessageSlideShell(display, SWT.ICON_INFORMATION, "ShortTitle3",
				"ShortText", (String) null);
		for (int x = 0; x < 10; x++)
			text += "\n\n\n\n\n\n\n\nWow";
		new MessageSlideShell(display, SWT.ICON_INFORMATION, title + "4", text,
				"Details");

		new MessageSlideShell(display, SWT.ICON_ERROR, title + "5", text,
				(String) null);

		MessageSlideShell.waitUntilClosed();
	}

	/**
	 * @return the imgPopup
	 */
	public Image getImgPopup() {
		return imgPopup;
	}

	/**
	 * @param imgPopup the imgPopup to set
	 */
	public void setImgPopup(Image imgPopup) {
		this.imgPopup = imgPopup;
	}
}
