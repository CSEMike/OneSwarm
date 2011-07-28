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

package com.google.gwt.gen2.widgetbase.client;

import com.google.gwt.libideas.resources.client.CssResource;

/**
 * Returns all the css style names used by a widget. Each widget supporting this
 * interface should provide its own custom sub-interface and a concrete standard
 * implementation of that interface. <p/>
 * 
 * By default all style names should be baseName + "-" + name of method. So, for
 * instance, if a widget had the base name of "gwt-DatePicker" and a css method
 * "listItem", then the default css class name corresponding to that method is
 * "gwt-DatePicker-listItem". <br>
 * <h2>Important!</h2> {@link WidgetCss} interfaces can and will have additional
 * methods added over time. Therefore do not extend any of these interfaces if
 * you are not prepared to have your build broken until you can add the
 * additional css style names.
 * 
 * @deprecated this solution was too confusing for our users, so is being removed from consideration
 */
@Deprecated
public interface WidgetCss extends CssResource {

}
