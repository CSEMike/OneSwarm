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

package com.google.gwt.user.client.ui;

/**
 * Implementation class (i.e. DO NOT USE) meant to allow incubator base widget
 * classes to access widget package protected fields.
 */
public class WidgetAdaptorImpl {

  public static void onAttach(Widget widget) {
    widget.onAttach();
  }

  public static void onDetach(Widget widget) {
    widget.onDetach();
  }

  public static void setParent(Widget widget, Widget parent) {
    widget.setParent(parent);
  }
}
