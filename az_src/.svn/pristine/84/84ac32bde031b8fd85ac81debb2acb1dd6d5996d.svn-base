/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.shells;

import java.util.Iterator;
import java.util.LinkedHashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * Cheap ugly slider shell
 * 
 * @author TuxPaper
 * @created Jul 5, 2007
 *
 */
public class SpeedScaleShell
{
	private static final boolean MOUSE_ONLY_UP_EXITS = true;

	private static final int OPTION_HEIGHT = 15;

	private static final int TEXT_HEIGHT = 32;

	private static final int SCALER_HEIGHT = 20;

	private int HEIGHT = TEXT_HEIGHT + SCALER_HEIGHT;

	private static final int WIDTH = 120;

	private static final int PADDING_X0 = 10;

	private static final int PADDING_X1 = 10;

	private static final int WIDTH_NO_PADDING = WIDTH - PADDING_X0 - PADDING_X1;

	private static final int TYPED_TEXT_ALPHA = 80;

	private static final long CLOSE_DELAY = 600;

	private int value;

	private boolean cancelled;

	private int minValue;

	private int maxValue;

	private int maxTextValue;

	private int pageIncrement;

	private int bigPageIncrement;

	private Shell shell;

	private LinkedHashMap mapOptions = new LinkedHashMap();

	private String sValue = "";

	private Composite composite;

	private boolean menuChosen;

	protected boolean lastMoveHadMouseDown;

	private boolean assumeInitiallyDown;

	private TimerEventPerformer cursorBlinkPerformer = null;

	private TimerEvent cursorBlinkEvent = null;

	public static void main(String[] args) {
		SpeedScaleShell speedScaleWidget = new SpeedScaleShell() {
			public String getStringValue() {
				return getValue() + "b/s";
			}
		};
		speedScaleWidget.setMaxValue(10000);
		speedScaleWidget.setMaxTextValue(15000);
		speedScaleWidget.addOption("AutoSpeed", -1);
		speedScaleWidget.addOption("Preset: 10b/s", 10);
		speedScaleWidget.addOption("Preset: 20b/s", 20);
		speedScaleWidget.addOption("Preset: 1b/s", 1);
		speedScaleWidget.addOption("Preset: 1000b/s", 1000);
		speedScaleWidget.addOption("Preset: A really long preset", 2000);
		System.out.println("returns "
				+ speedScaleWidget.open(1000, Constants.isWindows) + " w/"
				+ speedScaleWidget.getValue());
	}

	public SpeedScaleShell() {
		minValue = 0;
		maxValue = -1;
		maxTextValue = -1;
		pageIncrement = 10;
		bigPageIncrement = 100;
		cancelled = true;
		menuChosen = false;
	}

	/**
	 * Borks with 0 or -1 maxValue
	 * 
	 * @param startValue
	 * @param assumeInitiallyDown 
	 * @return
	 *
	 * @since 3.0.1.7
	 */
	public boolean open(final int startValue, boolean _assumeInitiallyDown) {
		value = startValue;
		this.assumeInitiallyDown = _assumeInitiallyDown;
		if (assumeInitiallyDown) {
			lastMoveHadMouseDown = true;
		}
		cancelled = true;

		shell = new Shell(Utils.findAnyShell(), SWT.DOUBLE_BUFFERED | SWT.ON_TOP);
		shell.setLayout(new FillLayout());
		final Display display = shell.getDisplay();

		composite = new Composite(shell, SWT.DOUBLE_BUFFERED);

		final Point firstMousePos = display.getCursorLocation();

		composite.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					setCancelled(true);
					shell.dispose();
				} else if (e.detail == SWT.TRAVERSE_ARROW_NEXT) {
					setValue(value + 1);
				} else if (e.detail == SWT.TRAVERSE_ARROW_PREVIOUS) {
					setValue(value - 1);
				} else if (e.detail == SWT.TRAVERSE_PAGE_NEXT) {
					setValue(value + bigPageIncrement);
				} else if (e.detail == SWT.TRAVERSE_PAGE_PREVIOUS) {
					setValue(value - bigPageIncrement);
				} else if (e.detail == SWT.TRAVERSE_RETURN) {
					setCancelled(false);
					shell.dispose();
				}
			}
		});

		composite.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.PAGE_DOWN && e.stateMask == 0) {
					setValue(value + pageIncrement);
				} else if (e.keyCode == SWT.PAGE_UP && e.stateMask == 0) {
					setValue(value - pageIncrement);
				} else if (e.keyCode == SWT.HOME) {
					setValue(minValue);
				} else if (e.keyCode == SWT.END) {
					if (maxValue != -1) {
						setValue(maxValue);
					}
				}
			}
		});

		composite.addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent e) {
				lastMoveHadMouseDown = false;
				boolean hasButtonDown = (e.stateMask & SWT.BUTTON_MASK) > 0
						|| assumeInitiallyDown;
				if (hasButtonDown) {
					if (e.y > HEIGHT - SCALER_HEIGHT) {
						lastMoveHadMouseDown = true;
						setValue(getValueFromMousePos(e.x));
					}
					composite.redraw();
				} else {
					composite.redraw();
				}
			}
		});

		composite.addMouseTrackListener(new MouseTrackListener() {
			boolean mouseIsOut = false;

			private boolean exitCancelled = false;

			public void mouseHover(MouseEvent e) {
			}

			public void mouseExit(MouseEvent e) {
				mouseIsOut = true;
				SimpleTimer.addEvent("close scaler",
						SystemTime.getOffsetTime(CLOSE_DELAY), new TimerEventPerformer() {
							public void perform(TimerEvent event) {
								Utils.execSWTThread(new AERunnable() {
									public void runSupport() {
										if (!exitCancelled) {
											shell.dispose();
										} else {
											exitCancelled = false;
										}
									}
								});
							}
						});
			}

			public void mouseEnter(MouseEvent e) {
				if (mouseIsOut) {
					exitCancelled = true;
				}
				mouseIsOut = false;
			}
		});

		composite.addMouseListener(new MouseListener() {
			boolean bMouseDown = false;

			public void mouseUp(MouseEvent e) {
				if (assumeInitiallyDown) {
					//System.out.println("assumed down");
					assumeInitiallyDown = false;
				}
				if (MOUSE_ONLY_UP_EXITS) {
					//System.out.println("last move had mouse down: " + lastMoveHadMouseDown);
					if (lastMoveHadMouseDown) {
						Point mousePos = display.getCursorLocation();
						//System.out.println("first=" + firstMousePos + ";mouse= " + mousePos);
						if (mousePos.equals(firstMousePos)) {
							lastMoveHadMouseDown = false;
							return;
						}
					}
					bMouseDown = true;
				}
				if (bMouseDown) {
					if (e.y > HEIGHT - SCALER_HEIGHT) {
						setValue(getValueFromMousePos(e.x));
						setCancelled(false);
						if (lastMoveHadMouseDown) {
							shell.dispose();
						}
					} else if (e.y > TEXT_HEIGHT) {
						int idx = (e.y - TEXT_HEIGHT) / OPTION_HEIGHT;
						Iterator iterator = mapOptions.keySet().iterator();
						int newValue;
						do {
							newValue = ((Integer) iterator.next()).intValue();
							idx--;
						} while (idx >= 0);
						value = newValue; // ignore min/max
						setCancelled(false);
						setMenuChosen(true);
						shell.dispose();
					}
				}
			}

			public void mouseDown(MouseEvent e) {
				if (e.count > 1) {
					lastMoveHadMouseDown = true;
					return;
				}
				Point mousePos = display.getCursorLocation();
				if (e.y > HEIGHT - SCALER_HEIGHT) {
					bMouseDown = true;
					setValue(getValueFromMousePos(e.x));
				}
			}

			public void mouseDoubleClick(MouseEvent e) {
			}

		});

		composite.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				int x = WIDTH_NO_PADDING * value / maxValue;
				if (x < 0) {
					x = 0;
				} else if (x > WIDTH_NO_PADDING) {
					x = WIDTH_NO_PADDING;
				}
				int startX = WIDTH_NO_PADDING * startValue / maxValue;
				if (startX < 0) {
					startX = 0;
				} else if (startX > WIDTH_NO_PADDING) {
					startX = WIDTH_NO_PADDING;
				}
				int baseLinePos = getBaselinePos();

				try {
					e.gc.setAdvanced(true);
					e.gc.setAntialias(SWT.ON);
				} catch (Exception ex) {
					// aw
				}

				e.gc.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
				// left
				e.gc.drawLine(PADDING_X0, baseLinePos - 6, PADDING_X0, baseLinePos + 6);
				// right
				e.gc.drawLine(PADDING_X0 + WIDTH_NO_PADDING, baseLinePos - 6,
						PADDING_X0 + WIDTH_NO_PADDING, baseLinePos + 6);
				// baseline
				e.gc.drawLine(PADDING_X0, baseLinePos, PADDING_X0 + WIDTH_NO_PADDING,
						baseLinePos);

				e.gc.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
				e.gc.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
				// start value marker
				e.gc.drawLine(PADDING_X0 + startX, baseLinePos - 5,
						PADDING_X0 + startX, baseLinePos + 5);
				// current value marker
				e.gc.fillRoundRectangle(PADDING_X0 + x - 2, baseLinePos - 5, 5, 10, 10,
						10);

				// Current Value Text
				e.gc.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
				e.gc.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));

				e.gc.fillRectangle(0, 0, WIDTH, TEXT_HEIGHT);

				GCStringPrinter.printString(e.gc, _getStringValue(), new Rectangle(0,
						0, WIDTH, HEIGHT), true, false, SWT.CENTER | SWT.TOP | SWT.WRAP);

				e.gc.drawLine(0, TEXT_HEIGHT - 1, WIDTH, TEXT_HEIGHT - 1);

				// options list
				int y = TEXT_HEIGHT;
				Point mousePos = composite.toControl(display.getCursorLocation());
				for (Iterator iter = mapOptions.keySet().iterator(); iter.hasNext();) {
					Integer value = (Integer) iter.next();
					String text = (String) mapOptions.get(value);

					Rectangle area = new Rectangle(0, y, WIDTH, OPTION_HEIGHT);
					Color bg;
					if (area.contains(mousePos)) {
						bg = display.getSystemColor(SWT.COLOR_LIST_SELECTION);
						e.gc.setBackground(bg);
						e.gc.setForeground(display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
						e.gc.fillRectangle(area);
					} else {
						bg = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
						e.gc.setBackground(bg);
						e.gc.setForeground(display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));
					}

					int ovalSize = OPTION_HEIGHT - 6;
					if (getValue() == value.intValue()) {
						Color saveColor = e.gc.getBackground();
						e.gc.setBackground(e.gc.getForeground());
						e.gc.fillOval(4, y + 5, ovalSize - 3, ovalSize - 3);
						e.gc.setBackground(saveColor);
					}
					if (Constants.isLinux) {
						// Hack: on linux, drawing oval seems to draw a line from last pos
						// to start of oval.. drawing a point (anywhere) seems to clear the
						// path
						Color saveColor = e.gc.getForeground();
						e.gc.setForeground(bg);
						e.gc.drawPoint(2, y + 3);
						e.gc.setForeground(saveColor);
					}
					e.gc.drawOval(2, y + 3, ovalSize, ovalSize);

					GCStringPrinter.printString(e.gc, text, new Rectangle(OPTION_HEIGHT,
							y, WIDTH - OPTION_HEIGHT, OPTION_HEIGHT), true, false, SWT.LEFT);
					y += OPTION_HEIGHT;
				}

				// typed value
				if (sValue.length() > 0) {
					Point extent = e.gc.textExtent(sValue);
					if (extent.x > WIDTH - 10) {
						extent.x = WIDTH - 10;
					}
					Rectangle rect = new Rectangle(WIDTH - 8 - extent.x, 14,
							extent.x + 5, extent.y + 4 + 14 > TEXT_HEIGHT ? TEXT_HEIGHT - 15
									: extent.y + 4);
					e.gc.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
					e.gc.fillRectangle(rect);

					try {
						e.gc.setAlpha(TYPED_TEXT_ALPHA);
					} catch (Exception ex) {
					}
					e.gc.setBackground(display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
					e.gc.setForeground(display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));
					//e.gc.drawRectangle(rect);

					GCStringPrinter.printString(e.gc, sValue, new Rectangle(rect.x + 2,
							rect.y + 2, WIDTH - 5, OPTION_HEIGHT), true, false, SWT.LEFT
							| SWT.BOTTOM);
				}
			}
		});

		// blinking cursor so people know they can type
		final AERunnable cursorBlinkRunnable = new AERunnable() {
			boolean on = false;

			public void runSupport() {
				if (composite.isDisposed()) {
					return;
				}

				on = !on;

				GC gc = new GC(composite);
				try {
					gc.setLineWidth(2);
					if (!on) {
						gc.setForeground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
					} else {
						try {
							gc.setAlpha(TYPED_TEXT_ALPHA);
						} catch (Exception e) {
						}
					}
					int y = 15;
					gc.drawLine(WIDTH - 5, y + 1, WIDTH - 5, y + OPTION_HEIGHT);
				} finally {
					gc.dispose();
				}
				if (cursorBlinkPerformer != null) {
					cursorBlinkEvent = SimpleTimer.addEvent("BlinkingCursor",
							SystemTime.getOffsetTime(500), cursorBlinkPerformer);
				}
			}
		};
		cursorBlinkPerformer = new TimerEventPerformer() {
			public void perform(final TimerEvent event) {
				Utils.execSWTThread(cursorBlinkRunnable);
			}
		};
		cursorBlinkEvent = SimpleTimer.addEvent("BlinkingCursor",
				SystemTime.getOffsetTime(500), cursorBlinkPerformer);

		composite.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
				if (Character.isDigit(e.character)) {
					sValue += e.character;
				} else if (e.keyCode == SWT.BS && sValue.length() > 0) {
					sValue = sValue.substring(0, sValue.length() - 1);
				} else {
					return;
				}
				try {
					int newValue = Integer.parseInt(sValue);
					if (maxTextValue == -1) {
						setValue(newValue);
					} else {
						if (minValue > 0 && newValue < minValue) {
							newValue = minValue;
						}
						if (newValue > maxTextValue) {
							newValue = maxTextValue;
						}
						value = newValue;
						composite.redraw();
					}
				} catch (Exception ex) {
					setValue(startValue);
				}
			}
		});

		Point location = display.getCursorLocation();

		location.y -= getBaselinePos();
		int x = (int) (WIDTH_NO_PADDING * (value > maxValue ? 1 : (double) value
				/ maxValue));
		location.x -= PADDING_X0 + x;

		Rectangle bounds = new Rectangle(location.x, location.y, WIDTH, HEIGHT);
		Monitor mouseMonitor = shell.getMonitor();
		Monitor[] monitors = display.getMonitors();
		for (int i = 0; i < monitors.length; i++) {
			Monitor monitor = monitors[i];
			if (monitor.getBounds().contains(location)) {
				mouseMonitor = monitor;
				break;
			}
		}
		Rectangle monitorBounds = mouseMonitor.getBounds();
		Rectangle intersection = monitorBounds.intersection(bounds);
		if (intersection.width != bounds.width) {
			bounds.x = monitorBounds.x + monitorBounds.width - WIDTH;
			bounds.width = WIDTH;
		}
		if (intersection.height != bounds.height) {
			bounds.y = monitorBounds.y + monitorBounds.height - HEIGHT;
			bounds.height = HEIGHT;
		}

		shell.setBounds(bounds);
		if (!bounds.contains(firstMousePos)) {
			// should never happen, which means it probably will, so handle it badly
			shell.setLocation(firstMousePos.x - (bounds.width / 2), firstMousePos.y
					- bounds.height + 2);
		}

		shell.open();
		// must be after, for OSX
		composite.setFocus();

		try {
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		} catch (Throwable t) {
			Debug.out(t);
		}

		if (cursorBlinkEvent != null) {
			cursorBlinkEvent.cancel();
			cursorBlinkEvent = null;
		}

		return !cancelled;
	}

	/**
	 * @param x
	 * @return
	 *
	 * @since 3.0.1.7
	 */
	protected int getValueFromMousePos(int x) {
		int x0 = x + 1;
		if (x < PADDING_X0) {
			x0 = PADDING_X0;
		} else if (x > PADDING_X0 + WIDTH_NO_PADDING) {
			x0 = PADDING_X0 + WIDTH_NO_PADDING;
		}

		return (x0 - PADDING_X0) * maxValue / WIDTH_NO_PADDING;
	}

	public int getValue() {
		return value;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public int getMinValue() {
		return minValue;
	}

	public void setMinValue(int minValue) {
		this.minValue = minValue;
	}

	public int getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(int maxValue) {
		this.maxValue = maxValue;
	}

	public void setValue(int value) {
		//System.out.println("sv " + value + ";" + Debug.getCompressedStackTrace());
		if (value > maxValue) {
			value = maxValue;
		} else if (value < minValue) {
			value = minValue;
		}
		this.value = value;
		if (composite != null && !composite.isDisposed()) {
			composite.redraw();
		}
	}

	public String _getStringValue() {
		String name = (String) mapOptions.get(new Integer(value));
		return getStringValue(value, name);
	}

	public String getStringValue(int value, String sValue) {
		if (sValue != null) {
			return sValue;
		}
		return "" + value;
	}

	private int getBaselinePos() {
		return HEIGHT - (SCALER_HEIGHT / 2);
	}

	public void addOption(String id, int value) {
		mapOptions.put(new Integer(value), id);
		HEIGHT += OPTION_HEIGHT;
	}

	public int getMaxTextValue() {
		return maxTextValue;
	}

	public void setMaxTextValue(int maxTextValue) {
		this.maxTextValue = maxTextValue;
	}

	public boolean wasMenuChosen() {
		return menuChosen;
	}

	public void setMenuChosen(boolean menuChosen) {
		this.menuChosen = menuChosen;
	}
}
