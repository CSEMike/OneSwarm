/*
 * Copyright 2008 Google Inc.
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
 */

package com.google.gwt.libideas.logging.client;

import com.google.gwt.libideas.logging.shared.LogHandler;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Shows a widget handler in a popup.
 * 
 * @param <T> type of popup contents
 * @deprecated use the com.google.gwt.gen2.logging classes instead
 */
@Deprecated
public abstract class PopupWidgetLogHandler<T extends Widget> extends
    LogHandler {

  private final PopupPanel popup;
  private final boolean autoShow;
  private final T widget;

  PopupWidgetLogHandler(boolean autoShow, T widget) {
    this.popup = new PopupPanel(autoShow);
    getPopup().setStyleName("gwt-PopupWidgetHandler");
    this.autoShow = autoShow;
    this.widget = widget;
    getPopup().setWidget(widget);
  }

  public T getWidget() {
    return widget;
  }

  public void hideHandler() {
    getPopup().hide();
  }

  public void setPopupPosition(int left, int top) {
    getPopup().setPopupPosition(left, top);
  }

  public void showLog() {

    if (getPopup().equals(getWidget().getParent())) {
      getPopup().show();
    }
  }

  protected PopupPanel getPopup() {
    return popup;
  }

  protected boolean isAutoShow() {
    return autoShow;
  }

}
