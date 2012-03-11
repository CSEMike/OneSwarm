/**
 * Created on Jan 4, 2010
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package org.gudy.azureus2.ui.swt.mainwindow;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Alerts;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter.URLInfo;

import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

/**
 * @author TuxPaper
 * @created Jan 4, 2010
 *
 */
public class SystemWarningWindow
{
	private final static int WIDTH = 230;

	private final static int BORDER_X = 12;

	private final static int BORDER_Y0 = 10;

	private final static int BORDER_Y1 = 6;

	private final static int GAP_Y = 5;

	private final static int GAP_BUTTON_Y = 20;

	private final static int GAP_Y_TITLE_COUNT = 3;

	private final LogAlert logAlert;

	private final Point ptBottomRight;

	private final Shell parent;

	private Shell shell;

	private Image imgClose;

	private Rectangle boundsClose;

	private GCStringPrinter spText;

	private GCStringPrinter spTitle;

	private GCStringPrinter spCount;

	private Point sizeTitle;

	private Point sizeText;

	private Point sizeCount;

	private Font fontTitle;

	private Font fontCount;

	private int height;

	private Rectangle rectX;

	private int historyPosition;

	private String title;

	private String text;
	
	public static int numWarningWindowsOpen = 0;

	public SystemWarningWindow(LogAlert logAlert, Point ptBottomRight,
			Shell parent, int historyPosition) {
		this.logAlert = logAlert;
		this.ptBottomRight = ptBottomRight;
		this.parent = parent;
		this.historyPosition = historyPosition;

		String amb_key_suffix;
		switch (logAlert.entryType) {
			case LogAlert.AT_ERROR:
				amb_key_suffix = "error";
				break;
			case LogAlert.AT_INFORMATION:
				amb_key_suffix = "information";
				break;
			case LogAlert.AT_WARNING:
				amb_key_suffix = "warning";
				break;
			default:
				amb_key_suffix = null;
				break;
		}
		title = amb_key_suffix == null ? Constants.APP_NAME
				: MessageText.getString("AlertMessageBox." + amb_key_suffix);

		if (logAlert.text.startsWith("{")) {
			text = MessageText.expandValue(logAlert.text);
		} else {
			text = logAlert.text;
		}
		
		if (logAlert.err != null) {
			text += "\n" + Debug.getExceptionMessage(logAlert.err);
		}

		if (logAlert.details != null) {
			text += "\n<A HREF=\"details\">" + MessageText.getString("v3.MainWindow.button.viewdetails") + "</A>";
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				openWindow();
			}
		});
	}

	protected void openWindow() {
		Display display = parent.getDisplay();
		//shell = new Shell(parent, SWT.TOOL | SWT.TITLE | SWT.CLOSE);
		//shell.setText("Warning (X of X)");
		shell = new Shell(parent, SWT.TOOL);
		shell.setLayout(new FormLayout());
		shell.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		shell.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		
		Menu menu = new Menu(shell);
		MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(menuItem, "MyTorrentsView.menu.thisColumn.toClipboard");
		menuItem.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				ClipboardCopy.copyToClipBoard(logAlert.text
						+ (logAlert.details == null ? "" : "\n" + logAlert.details));
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		shell.setMenu(menu);


		ImageLoader imageLoader = ImageLoader.getInstance();
		imgClose = imageLoader.getImage("image.systemwarning.closeitem");
		boundsClose = imgClose.getBounds();

		GC gc = new GC(shell);

		FontData[] fontdata = gc.getFont().getFontData();
		fontdata[0].setHeight(fontdata[0].getHeight() + 1);
		fontdata[0].setStyle(SWT.BOLD);
		fontTitle = new Font(display, fontdata);
		
		fontdata = gc.getFont().getFontData();
		fontdata[0].setHeight(fontdata[0].getHeight() - 1);
		fontCount = new Font(display, fontdata);

		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				Utils.disposeSWTObjects(new Object[] {
					fontTitle,
					fontCount,
				});
				numWarningWindowsOpen--;
			}
		});

		Rectangle printArea = new Rectangle(BORDER_X, 0, WIDTH - (BORDER_X * 2),
				5000);
		spText = new GCStringPrinter(gc, text, printArea, true, false, SWT.WRAP);
		spText.setUrlColor(Colors.blues[Colors.FADED_DARKEST]);
		spText.calculateMetrics();

		gc.setFont(fontCount);
		String sCount = MessageText.getString("OpenTorrentWindow.xOfTotal",
				new String[] {
					"" + historyPosition + 1,
					"" + getWarningCount()
				});
		spCount = new GCStringPrinter(gc, sCount, printArea, true, false, SWT.WRAP);
		spCount.calculateMetrics();

		gc.setFont(fontTitle);
		spTitle = new GCStringPrinter(gc, title, printArea, true, false, SWT.WRAP);
		spTitle.calculateMetrics();

		gc.dispose();
		sizeText = spText.getCalculatedSize();
		sizeTitle = spTitle.getCalculatedSize();
		sizeCount = spCount.getCalculatedSize();

		FormData fd;

		Button btnDismiss = new Button(shell, SWT.PUSH);
		Messages.setLanguageText(btnDismiss, "Button.dismiss");
		final int btnHeight = btnDismiss.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;

		Button btnPrev = new Button(shell, SWT.PUSH);
		btnPrev.setText("<");

		Button btnNext = new Button(shell, SWT.PUSH);
		btnNext.setText(">");

		fd = new FormData();
		fd.bottom = new FormAttachment(100, -BORDER_Y1);
		fd.right = new FormAttachment(100, -BORDER_X);
		btnNext.setLayoutData(fd);

		fd = new FormData();
		fd.bottom = new FormAttachment(100, -BORDER_Y1);
		fd.right = new FormAttachment(btnNext, -BORDER_X);
		btnPrev.setLayoutData(fd);

		fd = new FormData();
		fd.bottom = new FormAttachment(100, -BORDER_Y1);
		fd.right = new FormAttachment(btnPrev, -BORDER_X);
		btnDismiss.setLayoutData(fd);

		height = BORDER_Y0 + sizeTitle.y + GAP_Y + sizeText.y + GAP_Y_TITLE_COUNT
				+ sizeCount.y + GAP_BUTTON_Y + btnHeight + BORDER_Y1;

		Rectangle area = shell.computeTrim(ptBottomRight.x - WIDTH, ptBottomRight.y
				- height, WIDTH, height);
		shell.setBounds(area);
		shell.setLocation(ptBottomRight.x - area.width, ptBottomRight.y
				- area.height - 2);

		rectX = new Rectangle(area.width - BORDER_X - boundsClose.width, BORDER_Y0,
				boundsClose.width, boundsClose.height);

		shell.addMouseMoveListener(new MouseMoveListener() {
			int lastCursor = SWT.CURSOR_ARROW;

			public void mouseMove(MouseEvent e) {
				if (shell == null || shell.isDisposed()) {
					return;
				}
				URLInfo hitUrl = spText.getHitUrl(e.x, e.y);

				int cursor = (rectX.contains(e.x, e.y)) || hitUrl != null
						? SWT.CURSOR_HAND : SWT.CURSOR_ARROW;
				if (cursor != lastCursor) {
					lastCursor = cursor;
					shell.setCursor(e.display.getSystemCursor(cursor));
				}
			}
		});

		shell.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent e) {
				if (shell == null || shell.isDisposed()) {
					return;
				}
				if (rectX.contains(e.x, e.y)) {
					shell.dispose();
				}
				URLInfo hitUrl = spText.getHitUrl(e.x, e.y);
				if (hitUrl != null) {
					if (hitUrl.url.equals("details")) {
						MessageBoxShell mb = new MessageBoxShell(Constants.APP_NAME,
								logAlert.details, new String[] {
									MessageText.getString("Button.ok")
								}, 0);
						mb.setUseTextBox(true);
						mb.setParent(Utils.findAnyShell());
						mb.open(null);
					} else {
						Utils.launch(hitUrl.url);
					}
				}
			}

			public void mouseDown(MouseEvent e) {
			}

			public void mouseDoubleClick(MouseEvent e) {
			}
		});

		shell.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				e.gc.drawImage(imgClose, WIDTH - BORDER_X - boundsClose.width,
						BORDER_Y0);

				Rectangle printArea;
				printArea = new Rectangle(BORDER_X, BORDER_Y0 + sizeTitle.y + GAP_Y_TITLE_COUNT,
						WIDTH, 100);
				String sCount = MessageText.getString("OpenTorrentWindow.xOfTotal",
						new String[] {
							"" + (historyPosition + 1),
							"" + getWarningCount()
						});
				e.gc.setAlpha(180);
				Font lastFont = e.gc.getFont();
				e.gc.setFont(fontCount);
				spCount = new GCStringPrinter(e.gc, sCount, printArea, true, false,
						SWT.WRAP | SWT.TOP);
				spCount.printString();
				e.gc.setAlpha(255);
				sizeCount = spCount.getCalculatedSize();

				e.gc.setFont(lastFont);
				spText.printString(e.gc, new Rectangle(BORDER_X, BORDER_Y0
						+ sizeTitle.y + GAP_Y_TITLE_COUNT + sizeCount.y + GAP_Y, WIDTH - BORDER_X
						- BORDER_X, 5000), SWT.WRAP | SWT.TOP);

				e.gc.setFont(fontTitle);

				e.gc.setForeground(ColorCache.getColor(e.gc.getDevice(), "#54728c"));
				spTitle.printString(e.gc, new Rectangle(BORDER_X, BORDER_Y0, WIDTH
						- BORDER_X - BORDER_X, 5000), SWT.WRAP | SWT.TOP);

				e.gc.setLineStyle(SWT.LINE_DOT);
				e.gc.setLineWidth(1);
				e.gc.setAlpha(180);
				e.gc.drawLine(BORDER_X, height - btnHeight - (GAP_BUTTON_Y / 2)
						- BORDER_Y1, WIDTH - BORDER_X, height - btnHeight
						- (GAP_BUTTON_Y / 2) - BORDER_Y1);

			}
		});

		shell.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					shell.dispose();
					return;
				}
			}
		});

		btnPrev.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				ArrayList<LogAlert> alerts = Alerts.getUnviewedLogAlerts();
				int pos = historyPosition - 1;
				if (pos < 0 || pos >= alerts.size()) {
					return;
				}

				new SystemWarningWindow(alerts.get(pos), ptBottomRight, parent, pos);
				shell.dispose();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		btnPrev.setEnabled(historyPosition > 0); 

		btnNext.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				ArrayList<LogAlert> alerts = Alerts.getUnviewedLogAlerts();
				int pos = historyPosition + 1;
				if (pos >= alerts.size()) {
					return;
				}

				new SystemWarningWindow(alerts.get(pos), ptBottomRight, parent, pos);
				shell.dispose();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		ArrayList<LogAlert> alerts = Alerts.getUnviewedLogAlerts();
		btnNext.setEnabled(alerts.size() != historyPosition + 1); 

		btnDismiss.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				ArrayList<LogAlert> alerts = Alerts.getUnviewedLogAlerts();
				for (int i = 0; i < alerts.size() && i <= historyPosition; i++) {
					Alerts.markAlertAsViewed(alerts.get(i));
				}
				shell.dispose();
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		shell.open();
		numWarningWindowsOpen++;
	}

	private int getWarningCount() {
		ArrayList<LogAlert> historyList = Alerts.getUnviewedLogAlerts();
		return historyList.size();
	}
}
