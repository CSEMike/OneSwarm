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

package com.google.gwt.gen2.commonwidget.client;

import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;

/**
 * A popup panel with some extra functionality specialized for use as a drop
 * down.
 */
public class DropDownPanel extends PopupPanel {

  // Set of open panels so we can close them on window resize, because resizing
  // the window is equivalent to the user clicking outside the widget.
  private static ArrayList<DropDownPanel> openPanels;
  private static ResizeHandler resizeHandler = new ResizeHandler() {

    public void onResize(ResizeEvent event) { 
      if (openPanels != null) {
        ArrayList<DropDownPanel> old = openPanels;
        openPanels = null;
        for (DropDownPanel panel : old) {
          assert (panel.isShowing());
          if (panel.currentAnchor != null) {
            panel.showRelativeTo(panel.currentAnchor);
          }
        }
        old.clear();
        openPanels = old;
      }
    }
  };

  private Widget currentAnchor;

  /**
   * Creates a new drop down panel.
   */
  public DropDownPanel() {
    super(true);
    setStyleName("gwt-DropDownPanel");
    setPreviewingAllNativeEvents(true);
  }

  @Override
  public final void hide() {
    hide(false);
  }

  @Override
  public void hide(boolean autohide) {
    if (!isShowing()) {
      return;
    }
    super.hide(autohide);

    // Removes this from the list of open panels.
    if (openPanels != null) {
      openPanels.remove(this);
    }
  }

  @Override
  public void show() {
    if (isShowing()) {
      return;
    }
    // Add this to the set of open panels.
    if (openPanels == null) {
      openPanels = new ArrayList<DropDownPanel>();
      Window.addResizeHandler(resizeHandler);
    }
    openPanels.add(this);
    super.show();
  }

  public void showRelativeTo(Widget anchor) {
    setCurrentAnchor(anchor);
    super.showRelativeTo(anchor);
  }

  private void setCurrentAnchor(Widget anchor) {
    if (currentAnchor != null) {
      this.removeAutoHidePartner(currentAnchor.getElement());
    }
    if (anchor != null) {
      this.addAutoHidePartner(anchor.getElement());
    }
    currentAnchor = anchor;
  }

}
