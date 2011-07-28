// CHECKSTYLE_OFF
/*
 * Copyright 2008 Fred Sauer
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Modified by Google
 */
package com.google.gwt.libideas.logging.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.libideas.logging.shared.LogHandler;
import com.google.gwt.libideas.logging.shared.Level;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowResizeListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Logger which outputs to a draggable floating <code>DIV</code>.
 * 
 * @deprecated use the com.google.gwt.gen2.logging classes instead
 */
@Deprecated
public class DivLogHandler extends LogHandler {
  // CHECKSTYLE_JAVADOC_OFF

  private static final String STACKTRACE_ELEMENT_PREFIX = "&nbsp;&nbsp;&nbsp;&nbsp;at&nbsp;";
  private static final String STYLE_LOG_HEADER = "log-header";
  private static final String STYLE_LOG_PANEL = "log-panel";
  private static final String STYLE_LOG_SCROLL_PANEL = "log-scroll-panel";
  private static final String STYLE_LOG_TEXT_AREA = "log-text-area";
  private static final int UPDATE_INTERVAL_MILLIS = 500;

  private FlexTable debugTable = new FlexTable() {
    private WindowResizeListener windowResizeListener = new WindowResizeListener() {
      public void onWindowResized(int width, int height) {
        scrollPanel.setPixelSize(Math.max(300,
            (int) (Window.getClientWidth() * .8)), Math.max(100,
            (int) (Window.getClientHeight() * .2)));
      }
    };

    protected void onLoad() {
      super.onLoad();
      windowResizeListener.onWindowResized(Window.getClientWidth(),
          Window.getClientHeight());
      Window.addWindowResizeListener(windowResizeListener);
    }

    protected void onUnload() {
      super.onUnload();
      Window.removeWindowResizeListener(windowResizeListener);
    }
  };

  private boolean dirty = false;
  private String logText = "";
  private HTML logTextArea = new HTML();
  private ScrollPanel scrollPanel = new ScrollPanel();
  private Timer timer;

  /**
   * Default constructor.
   */
  public DivLogHandler() {
    debugTable.addStyleName(STYLE_LOG_PANEL);
    logTextArea.addStyleName(STYLE_LOG_TEXT_AREA);
    scrollPanel.addStyleName(STYLE_LOG_SCROLL_PANEL);

    final Label header = new Label("LOG PANEL");
    header.addStyleName(STYLE_LOG_HEADER);
    debugTable.setWidget(0, 0, header);
    debugTable.setWidget(1, 0, scrollPanel);
    debugTable.getCellFormatter().setHorizontalAlignment(0, 0,
        HasHorizontalAlignment.ALIGN_CENTER);

    scrollPanel.setWidget(logTextArea);

    header.addMouseListener(new MouseListenerAdapter() {
      private boolean dragging = false;
      private int dragStartX;
      private int dragStartY;

      public void onMouseDown(Widget sender, int x, int y) {
        dragging = true;
        DOM.setCapture(header.getElement());
        dragStartX = x;
        dragStartY = y;
      }

      public void onMouseMove(Widget sender, int x, int y) {
        if (dragging) {
          int absX = x + debugTable.getAbsoluteLeft();
          int absY = y + debugTable.getAbsoluteTop();
          RootPanel.get().setWidgetPosition(debugTable, absX - dragStartX,
              absY - dragStartY);
        }
      }

      public void onMouseUp(Widget sender, int x, int y) {
        dragging = false;
        DOM.releaseCapture(header.getElement());
      }
    });

    debugTable.setVisible(false);
    RootPanel.get().add(debugTable, 0, 0);

    timer = new Timer() {
      public void run() {
        dirty = false;
        logTextArea.setHTML(logTextArea.getHTML() + logText);
        logText = "";
        DeferredCommand.addCommand(new Command() {
          public void execute() {
            scrollPanel.setScrollPosition(Integer.MAX_VALUE);
          }
        });
      }
    };
  }

  public final void clear() {
    logTextArea.setHTML("");
  }

  public final Widget getWidget() {
    return debugTable;
  }

  public final boolean isSupported() {
    return true;
  }

  public final boolean isVisible() {
    return debugTable.isAttached() && debugTable.isVisible();
  }

  public final void moveTo(int x, int y) {
    RootPanel.get().add(debugTable, x, y);
  }

  public final void setPixelSize(int width, int height) {
    logTextArea.setPixelSize(width, height);
  }

  public final void setSize(String width, String height) {
    logTextArea.setSize(width, height);
  }

  public final void publish(String message, Level level, String category,
      Throwable throwable) {
    logText = formatMessage(message, level, throwable);

    if (!dirty) {
      dirty = true;
      timer.schedule(UPDATE_INTERVAL_MILLIS);
    }
    debugTable.setVisible(true);
  }

  public static String formatMessage(String message, Level level,
      Throwable throwable) {
    String text = message;
    String title = makeTitle(message, throwable);
    if (throwable != null) {
      text += "\n";
      while (throwable != null) {
        text += GWT.getTypeName(throwable) + ":<br><b>"
            + throwable.getMessage() + "</b>";
        StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        if (stackTraceElements.length > 0) {
          text += "<div class='log-stacktrace'>";
          for (int i = 0; i < stackTraceElements.length; i++) {
            text += STACKTRACE_ELEMENT_PREFIX + stackTraceElements[i] + "<br>";
          }
          text += "</div>";
        }
        throwable = throwable.getCause();
        if (throwable != null) {
          text += "Caused by: ";
        }
      }
    }
    text = text.replaceAll("\r\n|\r|\n", "<BR>");

    return "<div class='log-message' onmouseover='className+=\" log-message-hover\"' "
        + "onmouseout='className=className.replace(/ log-message-hover/g,\"\")' class='"
        + level.getName().toLowerCase() + "'" + title + "'>" + text + "</div>";
  }

  public static String makeTitle(String message, Throwable throwable) {
    if (throwable != null) {
      if (throwable.getMessage() == null) {
        message = GWT.getTypeName(throwable);
      } else {
        message = throwable.getMessage().replaceAll(
            GWT.getTypeName(throwable).replaceAll("^(.+\\.).+$", "$1"), "");
      }
    }
    return com.google.gwt.libideas.logging.client.util.DOMUtil.adjustTitleLineBreaks(
        message).replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll(
        "'", "\"");
  }

  public void hideHandler() {
    debugTable.setVisible(false);
  }
}