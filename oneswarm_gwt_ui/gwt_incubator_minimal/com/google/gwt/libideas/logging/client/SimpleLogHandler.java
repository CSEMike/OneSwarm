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

import com.google.gwt.libideas.logging.shared.Level;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A very simple log handler meant to display simple text messages.
 * 
 * @deprecated use the com.google.gwt.gen2.logging classes instead
 */
@Deprecated
public class SimpleLogHandler extends PopupWidgetLogHandler<VerticalPanel> {
  private VerticalPanel contents;

  /**
   * Constructor.
   * 
   * @param autoHide automatically close the popup
   */
  public SimpleLogHandler(boolean autoHide) {
    super(autoHide, new VerticalPanel());
    ScrollPanel scroller = new ScrollPanel();
    contents = new VerticalPanel();
    scroller.add(contents);
    VerticalPanel p = this.getWidget();
    p.add(new Button("clear log", new ClickListener() {
      public void onClick(Widget sender) {
        contents.clear();
      }
    }));
    p.add(scroller);
    getWidget().setStyleName("gwt-SimpleLogHandler");
  }

  @Override
  public void publish(String message, Level level, String category, Throwable e) {
    String output = category == null ? message : category + ": " + message;
    Label l = new Label(output);
    l.setStyleName("." + level.toString().toLowerCase());
    l.setTitle("level: " + level);
    contents.add(l);
    if (e != null) {
      Label eLabel = new Label("&nbsp;&nbsp;&nbsp;&nbsp;" + e.getMessage());
      contents.add(eLabel);
    }
    showLog();
  }
}
